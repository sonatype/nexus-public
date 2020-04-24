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
package org.sonatype.nexus.repository.npm.internal.orient

import javax.annotation.Nonnull
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.repository.group.GroupFacetImpl
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Response
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State

import static java.util.Objects.isNull
import static org.sonatype.nexus.repository.npm.internal.NpmResponses.notFound
import static org.sonatype.nexus.repository.group.GroupHandler.DispatchedRepositories

/**
 * Merge dist tag results from all member repositories.
 *
 * @since 3.19
 */
@Named
@Singleton
class OrientNpmGroupDistTagsHandler
    extends OrientNpmGroupHandler
{
  @Override
  protected Response doGet(@Nonnull final Context context,
                           @Nonnull final DispatchedRepositories dispatched)
      throws Exception
  {
    log.debug '[getDistTags] group repository: {} tokens: {}',
        context.repository.name,
        context.attributes.require(State.class).tokens

    GroupFacetImpl groupFacet = getGroupFacet(context)

    // Dispatch requests to members to trigger update events and group cache invalidation when a member has changed
    Map responses = getResponses(context, dispatched, groupFacet)

    if (isNull(responses) || responses.isEmpty()) {
      return notFound("Not found")
    }

    return NpmFacetUtils.mergeDistTagResponse(responses)
  }
}
