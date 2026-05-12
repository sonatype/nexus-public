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
package org.sonatype.nexus.repository.group;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import jakarta.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.ViewFacet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.unmodifiableSet;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.proxy.ProxyFacetSupport.BYPASS_HTTP_ERRORS_HEADER_NAME;
import static org.sonatype.nexus.repository.proxy.ProxyFacetSupport.BYPASS_HTTP_ERRORS_HEADER_VALUE;
import static org.sonatype.nexus.repository.proxy.ProxyFacetSupport.PROXY_THROTTLED_ANALYTICS_MARKED;

/**
 * Group handler.
 *
 * @since 3.0
 */
@Primary
@Component
@Qualifier("default")
@Singleton
public class GroupHandler
    implements Handler
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final String USE_DISPATCHED_RESPONSE = "USE_DISPATCHED_RESPONSE";

  public static final String INSUFFICIENT_LICENSE =
      "Deploying to groups is a PRO-licensed feature. See https://links.sonatype.com/product-nexus-repository";

  public static final String IQ_MEMBER_REPO_NAME = "IQ-Member-Repository";

  /**
   * Request-context state container for set of repositories already dispatched to.
   */
  @VisibleForTesting
  public static class DispatchedRepositories
  {
    private final Set<String> dispatched = Sets.newLinkedHashSet();

    public void add(final Repository repository) {
      dispatched.add(repository.getName());
    }

    public boolean contains(final Repository repository) {
      return dispatched.contains(repository.getName());
    }

    @Override
    public String toString() {
      return dispatched.toString();
    }

    /**
     * Get dispatched repositories names
     *
     * @return Unmodifiable {@link Set} of Dispatched repository names.
     */
    public Set<String> getDispatched() {
      return unmodifiableSet(dispatched);
    }
  }

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    final String method = context.getRequest().getAction();
    switch (method) {
      case GET:
      case HEAD: {
        final DispatchedRepositories dispatched = context.getRequest()
            .getAttributes()
            .getOrCreate(DispatchedRepositories.class);
        return doGet(context, dispatched);
      }

      default:
        return HttpResponses.methodNotAllowed(method, GET, HEAD);
    }
  }

  /**
   * Method that actually performs group GET. Override if needed.
   */
  protected Response doGet(
      @Nonnull final Context context,
      @Nonnull final DispatchedRepositories dispatched) throws Exception
  {
    final GroupFacet groupFacet = context.getRepository().facet(GroupFacet.class);
    return getFirst(context, groupFacet.members(), dispatched);
  }

  /**
   * Returns the first OK response from member repositories or {@link HttpResponses#notFound()} if none of the members
   * responded with OK.
   */
  protected Response getFirst(
      @Nonnull final Context context,
      @Nonnull final List<Repository> members,
      @Nonnull final DispatchedRepositories dispatched) throws Exception
  {
    int repositoryProxyCount = 0;
    final Request request = context.getRequest();
    for (Repository member : members) {
      log.trace("Trying member: {}", member);

      // skip offline repositories
      if (member.getConfiguration() != null && !member.getConfiguration().isOnline()) {
        log.trace("Skipping offline member: {}", member);
        continue;
      }

      if (ProxyType.NAME.equals(member.getType().getValue())) {
        repositoryProxyCount++;
        if (repositoryProxyCount == 2) {
          context.setAttribute(PROXY_THROTTLED_ANALYTICS_MARKED, TRUE);
        }
      }

      // track repositories we have dispatched to, prevent circular dispatch for nested groups
      if (dispatched.contains(member)) {
        log.trace("Skipping already dispatched member: {}", member);
        continue;
      }
      dispatched.add(member);

      final ViewFacet view = member.facet(ViewFacet.class);
      final Response response = view.dispatch(request, context);
      log.trace("Member {} response {}", member, response.getStatus());
      if (isValidResponse(response)) {
        if (ProxyType.NAME.equals(member.getType().getValue())) {
          context.setAttribute(IQ_MEMBER_REPO_NAME, member.getName());
        }
        return response;
      }
    }
    return notFoundResponse(context);
  }

  /**
   * Returns all responses from all members as a linked map, where order is group member order.
   */
  protected LinkedHashMap<Repository, Response> getAll(
      @Nonnull final Context context,
      @Nonnull final Iterable<Repository> members,
      @Nonnull final DispatchedRepositories dispatched) throws Exception
  {
    return getAll(context.getRequest(), context, members, dispatched, false);
  }

  /**
   * Returns all responses from all members as a linked map, where order is group member order.
   *
   * @param context the context
   * @param members the members to fetch from
   * @param dispatched the dispatched repositories tracker
   * @param isParentStale whether the parent group's cached metadata is stale and needs rebuilding
   */
  protected LinkedHashMap<Repository, Response> getAll(
      @Nonnull final Context context,
      @Nonnull final Iterable<Repository> members,
      @Nonnull final DispatchedRepositories dispatched,
      final boolean isParentStale) throws Exception
  {
    return getAll(context.getRequest(), context, members, dispatched, isParentStale);
  }

  /**
   * Similar to {@link #getAll(Context, Iterable, DispatchedRepositories)}, but allows for using a
   * different request then provided by the {@link Context#getRequest()} while still using the
   * same {@link Context} to execute the request in.
   *
   * @param request {@link Request} that could be different then the {@link Context#getRequest()}
   * @param context {@link Context}
   * @param members {@link Repository}'s
   * @param dispatched {@link DispatchedRepositories}
   * @return LinkedHashMap of all responses from all members where order is group member order.
   * @throws Exception throw for any issues dispatching the request
   */
  protected LinkedHashMap<Repository, Response> getAll(
      @Nonnull final Request request,
      @Nonnull final Context context,
      @Nonnull final Iterable<Repository> members,
      @Nonnull final DispatchedRepositories dispatched) throws Exception
  {
    return getAll(request, context, members, dispatched, false);
  }

  /**
   * Similar to {@link #getAll(Context, Iterable, DispatchedRepositories)}, but allows for using a
   * different request and provides control over staleness checking behavior.
   *
   * @param request {@link Request} that could be different then the {@link Context#getRequest()}
   * @param context {@link Context}
   * @param members {@link Repository}'s
   * @param dispatched {@link DispatchedRepositories}
   * @param isParentStale whether the parent group's cached metadata is stale and needs rebuilding
   * @return LinkedHashMap of all responses from all members where order is group member order.
   * @throws Exception throw for any issues dispatching the request
   */
  protected LinkedHashMap<Repository, Response> getAll(
      @Nonnull final Request request,
      @Nonnull final Context context,
      @Nonnull final Iterable<Repository> members,
      @Nonnull final DispatchedRepositories dispatched,
      final boolean isParentStale) throws Exception
  {
    final LinkedHashMap<Repository, Response> responses = Maps.newLinkedHashMap();

    for (Repository member : members) {
      log.trace("Trying member: {}", member);

      // skip offline repositories
      if (member.getConfiguration() != null && !member.getConfiguration().isOnline()) {
        log.trace("Skipping offline member: {}", member);
        continue;
      }

      // track repositories we have dispatched to, prevent circular dispatch for nested groups
      if (dispatched.contains(member)) {
        final String currentRepositoryName = context.getRepository().getName();
        // Check if parent is stale - if so, we need to re-fetch from all members to rebuild metadata
        if (!isParentStale) {
          // Parent is fresh, we can skip this already-dispatched member (group or non-group)
          log.trace("Skipping already dispatched member {} (parent {} is fresh)",
              member, currentRepositoryName);
          continue;
        }
        // Parent is stale, re-fetch from member even though it was dispatched
        log.trace("Re-fetching already dispatched member {} (parent {} is stale)",
            member, currentRepositoryName);
      }
      dispatched.add(member);

      final ViewFacet view = member.facet(ViewFacet.class);
      final Response response = view.dispatch(request, context);
      log.trace("Member {} response {}", member, response.getStatus());

      responses.put(member, response);
    }
    return responses;
  }

  /**
   * Returns standard 404 with no message. Override for format specific messaging.
   */
  protected Response notFoundResponse(final Context context) {
    return HttpResponses.notFound();
  }

  protected boolean isValidResponse(final Response response) {
    return response.getStatus().isSuccessful() ||
        response.getAttributes().contains(USE_DISPATCHED_RESPONSE) ||
        BYPASS_HTTP_ERRORS_HEADER_VALUE.equals(response.getHeaders().get(BYPASS_HTTP_ERRORS_HEADER_NAME));
  }
}
