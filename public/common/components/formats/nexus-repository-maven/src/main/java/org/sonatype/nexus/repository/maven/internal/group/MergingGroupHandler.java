/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.maven.internal.group;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.cooperation2.Cooperation2;
import org.sonatype.nexus.common.cooperation2.IOCall;
import org.sonatype.nexus.common.cooperation2.datastore.DefaultCooperation2Factory;
import org.sonatype.nexus.repository.HasFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.transaction.RetryDeniedException;

import com.google.common.base.Predicate;
import org.springframework.beans.factory.annotation.Value;

import static com.google.common.base.Predicates.or;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import org.springframework.stereotype.Component;

/**
 * Maven2 specific group handler: calls into {@link MavenGroupFacet} to get some content from members, cache it, and
 * serve it up. Handles merging of repository metadata and archetype catalog.
 *
 * @since 3.0
 */
@Singleton
@Component
public class MergingGroupHandler
    extends GroupHandler
{
  private static final Predicate<Repository> PROXY_OR_GROUP =
      or(new HasFacet(ProxyFacet.class), new HasFacet(GroupFacet.class));

  private Cooperation2 metadataCooperation;

  @Inject
  public MergingGroupHandler(
      @Value("${nexus.maven.group.cooperation.enabled:true}") final boolean cooperationEnabled,
      @Value("${nexus.maven.group.cooperation.majorTimeout:0s}") final Duration majorTimeout,
      @Value("${nexus.maven.group.cooperation.minorTimeout:30s}") final Duration minorTimeout,
      @Value("${nexus.maven.group.cooperation.threadsPerKey:100}") final int threadsPerKey)
  {
    this.metadataCooperation = new DefaultCooperation2Factory().configure()
        .majorTimeout(majorTimeout)
        .minorTimeout(minorTimeout)
        .threadsPerKey(threadsPerKey)
        .enabled(cooperationEnabled)
        .build(getClass());
  }

  protected Response doGetHash(@Nonnull final Context context) throws Exception {
    final MavenPath mavenPath = context.getAttributes().require(MavenPath.class);
    final MavenGroupFacet groupFacet = context.getRepository().facet(MavenGroupFacet.class);
    final Repository repository = context.getRepository();
    log.trace("Incoming request for {} : {}", context.getRepository().getName(), mavenPath.getPath());

    // hashes need the parent asset(s) loaded into cache, they are calculated as a side effect of that
    final MavenPath parentPath = mavenPath.subordinateOf();

    // Create a new context to request the parent path and prime the caches with the subordinates
    final Context copyContext = context.copy(
        oldAttributes -> {
          AttributesMap newAttributes = new AttributesMap();
          oldAttributes.backing().forEach(newAttributes::set);
          newAttributes.set(MavenPath.class, parentPath);
          return newAttributes;
        },
        requestBuilder -> {
          requestBuilder.path(parentPath.getPath());

          AttributesMap oldAttributes = requestBuilder.attributes();
          AttributesMap newAttributes = new AttributesMap();
          oldAttributes.backing().forEach(newAttributes::set);
          newAttributes.set(MavenPath.class, parentPath);

          requestBuilder.attributes(newAttributes);
          return requestBuilder;
        });

    doGet(copyContext, new DispatchedRepositories());

    Optional<Content> cachedContent = checkCache(groupFacet, mavenPath, repository);
    if (cachedContent.isPresent()) {
      log.trace("Serving cached content {} : {}", repository.getName(), mavenPath.getPath());
      return HttpResponses.ok(cachedContent.get());
    }
    else {
      // hash should be available if corresponding content fetched. out of bound request?
      log.trace("Outbound request for hash {} : {}", repository.getName(), mavenPath.getPath());
      return HttpResponses.notFound();
    }
  }

  private Response doGetContent(
      @Nonnull final Context context,
      @Nonnull final DispatchedRepositories dispatched) throws Exception
  {
    final MavenPath mavenPath = context.getAttributes().require(MavenPath.class);
    final MavenGroupFacet groupFacet = context.getRepository().facet(MavenGroupFacet.class);
    final Repository repository = context.getRepository();
    final List<Repository> members = groupFacet.members();

    log.trace("Incoming request for {} : {}", context.getRepository().getName(), mavenPath.getPath());

    List<Repository> proxiesOrGroups =
        members.stream().filter(PROXY_OR_GROUP::apply).collect(toList());

    Content content = maybeCooperate(repository, mavenPath, () -> {
      try {

        Optional<Content> cached = checkCache(groupFacet, mavenPath, repository);

        boolean isStale = !cached.isPresent();

        if (!isStale) {
          log.trace("Serving cached content {} : {}", repository.getName(), mavenPath.getPath());
          return cached.get();
        }

        // Check members to prime
        Map<Repository, Response> passThroughResponses = getAll(context, proxiesOrGroups, dispatched, isStale);

        // Content is stale or missing - rebuild from members
        log.trace("Parent group {} is stale or has no cached content, rebuilding from members",
            repository.getName());

        // this will fetch the remaining responses, thanks to the 'dispatched' tracking
        Map<Repository, Response> remainingResponses = getAll(context, members, dispatched, isStale);

        // merge the two sets of responses according to member order
        LinkedHashMap<Repository, Response> responses = new LinkedHashMap<>();
        for (Repository member : members) {
          Response response = passThroughResponses.get(member);
          if (response == null) {
            response = remainingResponses.get(member);
          }
          if (response != null) {
            responses.put(member, response);
          }
        }

        // merge the individual responses and cache the result
        Content mergedContent = groupFacet.mergeAndCache(mavenPath, responses);
        if (mergedContent != null) {
          log.trace("Responses merged {} : {}", context.getRepository().getName(), mavenPath.getPath());
        }
        return mergedContent;
      }
      catch (Exception e) {
        throw new IOException(e);
      }
    });

    if (content != null) {
      return HttpResponses.ok(content);
    }
    else {
      log.trace("Not found response to merge {} : {}", repository.getName(), mavenPath.getPath());
      return HttpResponses.notFound();
    }
  }

  private Optional<Content> checkCache(
      @Nonnull final MavenGroupFacet groupFacet,
      @Nonnull final MavenPath mavenPath,
      @Nonnull final Repository repository) throws IOException
  {
    try {
      // check group-level cache to see if it's been invalidated by any updates
      return ofNullable(groupFacet.getCached(mavenPath));
    }
    catch (RetryDeniedException e) {
      log.debug("Conflict fetching cached content {} : {}", repository.getName(), mavenPath.getPath(), e);
    }
    return empty();
  }

  @Override
  protected Response doGet(
      @Nonnull final Context context,
      @Nonnull final DispatchedRepositories dispatched) throws Exception
  {
    final MavenPath mavenPath = context.getAttributes().require(MavenPath.class);

    if (mavenPath.isHash()) {
      return doGetHash(context);
    }
    else {
      return doGetContent(context, dispatched);
    }
  }

  /*
   * Invoke the call with co-operation if available, if not invoke the call directly.
   */
  private <T> T maybeCooperate(
      final Repository repository,
      final MavenPath path,
      final IOCall<T> call) throws IOException
  {
    return metadataCooperation.on(call)
        .cooperate(repository.getName(), path.toString());
  }
}
