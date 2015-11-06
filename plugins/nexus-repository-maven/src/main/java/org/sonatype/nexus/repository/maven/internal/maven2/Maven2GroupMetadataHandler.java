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
package org.sonatype.nexus.repository.maven.internal.maven2;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.group.GroupHandler;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.util.HasFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Response;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import static com.google.common.base.Predicates.or;

/**
 * Maven2 specific group metadata handler: metadata merge is specific to Maven2 format.
 *
 * @since 3.0
 */
@Singleton
@Named
public class Maven2GroupMetadataHandler
    extends GroupHandler
{
  private static final Predicate<Repository> PROXY_OR_GROUP =
      or(new HasFacet(ProxyFacet.class), new HasFacet(GroupFacet.class));

  @Override
  protected Response doGet(@Nonnull final Context context, @Nonnull final DispatchedRepositories dispatched)
      throws Exception
  {
    final MavenPath mavenPath = context.getAttributes().require(MavenPath.class);
    final Maven2GroupFacet groupFacet = context.getRepository().facet(Maven2GroupFacet.class);
    log.trace("Incoming request for metadata {} : {}", context.getRepository().getName(), mavenPath.getPath());

    final List<Repository> members = groupFacet.members();

    Map<Repository, Response> passThroughResponses = ImmutableMap.of();

    if (!mavenPath.isHash()) {
      // pass request through to proxies/nested-groups before checking our group cache
      Iterable<Repository> proxiesOrGroups = Iterables.filter(members, PROXY_OR_GROUP);
      if (proxiesOrGroups.iterator().hasNext()) {
        passThroughResponses = getAll(context, proxiesOrGroups, dispatched);
      }
    }

    // now check group-level cache to see if it's been invalidated by any updates
    Content content = groupFacet.getCachedMergedMetadata(mavenPath);
    if (content != null) {
      log.trace("Serving cached merged metadata {} : {}", context.getRepository().getName(), mavenPath.getPath());
      return HttpResponses.ok(content);
    }

    if (!mavenPath.isHash()) {

      // this will fetch the remaining responses, thanks to the 'dispatched' tracking
      Map<Repository, Response> remainingResponses = getAll(context, members, dispatched);

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

      // merge the individual metadata responses and cache the result
      content = groupFacet.mergeAndCacheMetadata(mavenPath, responses);
      if (content != null) {
        log.trace("Metadata merged {} : {}", context.getRepository().getName(), mavenPath.getPath());
        return HttpResponses.ok(content);
      }
      log.trace("Not found metadata to merge {} : {}", context.getRepository().getName(), mavenPath.getPath());
      return HttpResponses.notFound();
    }
    else {
      // hash should be available if corresponding metadata fetched. out of bound request?
      log.trace("Outbound request for metadata hash {} : {}", context.getRepository().getName(), mavenPath.getPath());
      return HttpResponses.notFound();
    }
  }
}
