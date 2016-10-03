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
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.ViewFacet;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;

/**
 * Group handler.
 *
 * @since 3.0
 */
@Named
@Singleton
public class GroupHandler
    extends ComponentSupport
    implements Handler
{
  /**
   * Request-context state container for set of repositories already dispatched to.
   */
  protected static class DispatchedRepositories
  {
    private final Set<String> dispatched = Sets.newHashSet();

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
  }

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    final String method = context.getRequest().getAction();
    switch (method) {
      case GET:
      case HEAD: {
        final DispatchedRepositories dispatched = context.getRequest().getAttributes()
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
  protected Response doGet(@Nonnull final Context context,
                           @Nonnull final DispatchedRepositories dispatched)
      throws Exception
  {
    final GroupFacet groupFacet = context.getRepository().facet(GroupFacet.class);
    return getFirst(context, groupFacet.members(), dispatched);
  }

  /**
   * Returns the first OK response from member repositories or {@link HttpResponses#notFound()} if none of the members
   * responded with OK.
   */
  protected Response getFirst(@Nonnull final Context context,
                              @Nonnull final List<Repository> members,
                              @Nonnull final DispatchedRepositories dispatched)
      throws Exception
  {
    final Request request = context.getRequest();
    for (Repository member : members) {
      log.trace("Trying member: {}", member);
      // track repositories we have dispatched to, prevent circular dispatch for nested groups
      if (dispatched.contains(member)) {
        log.trace("Skipping already dispatched member: {}", member);
        continue;
      }
      dispatched.add(member);

      final ViewFacet view = member.facet(ViewFacet.class);
      final Response response = view.dispatch(request, context);
      log.trace("Member {} response {}", member, response.getStatus());
      if (response.getStatus().isSuccessful()) {
        return response;
      }
    }
    return notFoundResponse(context);
  }

  /**
   * Returns all responses from all members as a linked map, where order is group member order.
   */
  protected LinkedHashMap<Repository, Response> getAll(@Nonnull final Context context,
                                                       @Nonnull final Iterable<Repository> members,
                                                       @Nonnull final DispatchedRepositories dispatched)
      throws Exception
  {
    final Request request = context.getRequest();
    final LinkedHashMap<Repository, Response> responses = Maps.newLinkedHashMap();
    for (Repository member : members) {
      log.trace("Trying member: {}", member);
      // track repositories we have dispatched to, prevent circular dispatch for nested groups
      if (dispatched.contains(member)) {
        log.trace("Skipping already dispatched member: {}", member);
        continue;
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
}
