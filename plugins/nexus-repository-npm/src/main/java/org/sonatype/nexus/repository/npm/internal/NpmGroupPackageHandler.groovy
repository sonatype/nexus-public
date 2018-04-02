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
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.common.collect.NestedAttributesMap
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.group.GroupFacet
import org.sonatype.nexus.repository.group.GroupHandler
import org.sonatype.nexus.repository.http.HttpStatus
import org.sonatype.nexus.repository.view.Content
import org.sonatype.nexus.repository.view.ContentTypes
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Response
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher
import org.sonatype.nexus.repository.view.payloads.BytesPayload

import org.joda.time.DateTime

import static org.sonatype.nexus.repository.http.HttpConditions.makeConditional
import static org.sonatype.nexus.repository.http.HttpConditions.makeUnconditional

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
  
  private final boolean mergeMetadata;

  /**
   * @param mergeMetadata whether or not metadata should be merged across all group members (defaults to true)
   */
  @Inject
  NpmGroupPackageHandler(@Named('${nexus.npm.mergeGroupMetadata:-true}') final boolean mergeMetadata) {
    this.mergeMetadata = mergeMetadata;
  }

  @Override
  protected Response doGet(@Nonnull final Context context,
                           @Nonnull final GroupHandler.DispatchedRepositories dispatched)
      throws Exception
  {
    TokenMatcher.State state = context.attributes.require(TokenMatcher.State)
    Repository repository = context.repository
    GroupFacet groupFacet = repository.facet(GroupFacet)

    log.debug '[getPackage] group repository: {} tokens: {}', repository.name, state.tokens

    // Remove conditional headers before making "internal" requests: https://issues.sonatype.org/browse/NEXUS-13915
    makeUnconditional(context.getRequest())

    LinkedHashMap<Repository, Response> responses = null
    try {
      // get all and filter for HTTP OK responses
      responses = getAll(context, groupFacet.members(), dispatched)
              .findAll { k, v -> v.status.code == HttpStatus.OK }
    }
    finally {
      makeConditional(context.getRequest())
    }

    if (responses == null || responses.isEmpty()) {
      return NpmResponses.notFound("Not found")
    }

    // unroll the actual package metadata from content attributes
    final List<NestedAttributesMap> packages = responses
        .collect { k, v -> ((Content) v.payload).attributes.get(NestedAttributesMap) }

    def result
    if (shouldServeFirstResult(packages, NpmHandlers.packageId(state))) {
      result = packages[0]
    }
    else {
      log.debug("Merging results from {} repositories", responses.size())
      result = NpmMetadataUtils.merge(packages[0].key, packages.reverse())
    }

    NpmMetadataUtils.rewriteTarballUrl(repository.name, result)

    Content content = new Content(new BytesPayload(NpmJsonUtils.bytes(result), ContentTypes.APPLICATION_JSON))
    content.attributes.set(Content.CONTENT_LAST_MODIFIED, DateTime.now())
    content.attributes.set(NestedAttributesMap, result)

    return NpmResponses.ok(content)
  }

  /**
   * @param packages
   * @param packageId
   * @return  True if we only have one result or if we have been asked not to merge metadata for non-scoped packages,
   * false otherwise.
   */
  boolean shouldServeFirstResult(List<NestedAttributesMap> packages, NpmPackageId packageId) {
    return packages.size() == 1 || (!mergeMetadata && !packageId.scope())
  }
}
