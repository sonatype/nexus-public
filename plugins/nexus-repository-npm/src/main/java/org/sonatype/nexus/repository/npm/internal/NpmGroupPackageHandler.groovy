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
package org.sonatype.nexus.repository.npm.internal

import javax.annotation.Nonnull
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.group.GroupFacet
import org.sonatype.nexus.repository.group.GroupHandler
import org.sonatype.nexus.repository.view.Content
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Response
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State

import static java.util.Objects.isNull
import static org.sonatype.nexus.repository.http.HttpConditions.makeConditional
import static org.sonatype.nexus.repository.http.HttpConditions.makeUnconditional
import static org.sonatype.nexus.repository.http.HttpStatus.OK
import static org.sonatype.nexus.repository.npm.internal.NpmResponses.notFound
import static org.sonatype.nexus.repository.npm.internal.NpmResponses.ok
import static org.sonatype.nexus.repository.group.GroupHandler.DispatchedRepositories

/**
 * Merge metadata results from all member repositories.
 *
 * @since 3.0
 */
@Named
@Singleton
class NpmGroupPackageHandler
    extends GroupHandler
{
  @Override
  protected Response doGet(@Nonnull final Context context,
                           @Nonnull final DispatchedRepositories dispatched)
      throws Exception
  {
    log.debug '[getPackage] group repository: {} tokens: {}',
        context.repository.name,
        context.attributes.require(State.class).tokens

    return buildMergedPackageRoot(context, dispatched)
  }

  private Response buildMergedPackageRoot(final Context context,
                                          final DispatchedRepositories dispatched)
  {
    NpmGroupFacet groupFacet = getGroupFacet(context)

    Content content = groupFacet.getFromCache(context)

    // first check cached content against itself only
    if (isNull(content)) {

      Map responses = getResponses(context, dispatched, groupFacet)
      if (isNull(responses) || responses.isEmpty()) {
        return notFound("Not found")
      }

      return ok(groupFacet.buildPackageRoot(responses, context))
    }

    return ok(content)
  }

  private NpmGroupFacet getGroupFacet(final Context context) {
    return context.getRepository().facet(GroupFacet.class) as NpmGroupFacet
  }

  private Map<Repository, Response> getResponses(final Context context,
                                                 final DispatchedRepositories dispatched,
                                                 final NpmGroupFacet groupFacet)
  {
    // Remove conditional headers before making "internal" requests: https://issues.sonatype.org/browse/NEXUS-13915
    makeUnconditional(context.getRequest())

    try {
      // get all and filter for HTTP OK responses
      return getAll(context, groupFacet.members(), dispatched).findAll { k, v -> v.status.code == OK }
    }
    finally {
      makeConditional(context.getRequest())
    }
  }
}
