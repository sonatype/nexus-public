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
package org.sonatype.nexus.repository.content.rest.internal.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.selector.ContentAuthHelper;
import org.sonatype.nexus.repository.types.GroupType;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.sonatype.nexus.repository.content.rest.internal.resources.AssetsResource.PAGE_SIZE;
import static org.sonatype.nexus.repository.content.store.InternalIds.toInternalId;

/**
 * Support class for {@link AssetsResource} which fetches and returns only assets that the user is permitted
 * to view according to {@link ContentAuthHelper#checkPathPermissions(String, String, String...)}
 *
 * @since 3.27
 */
abstract class AssetsResourceSupport
    extends ComponentSupport
{
  protected static final int LIMIT = 100;

  private final ContentAuthHelper contentAuthHelper;

  AssetsResourceSupport(final ContentAuthHelper contentAuthHelper) {
    this.contentAuthHelper = checkNotNull(contentAuthHelper);
  }

  List<FluentAsset> browse(final Repository repository, final String continuationToken) {
    List<FluentAsset> permittedAssets = new ArrayList<>();
    String internalToken = toInternalToken(continuationToken);
    Continuation<FluentAsset> assetContinuation = getAssets(repository, internalToken);

    while (permittedAssets.size() < PAGE_SIZE && !assetContinuation.isEmpty()) {
      permittedAssets.addAll(removeAssetsNotPermitted(repository, assetContinuation));
      assetContinuation = getAssets(repository, assetContinuation.nextContinuationToken());
    }
    return trim(permittedAssets, PAGE_SIZE);
  }

  private Continuation<FluentAsset> getAssets(Repository repository, final String continuationToken) {
    // helper for users, if they query by group chances are they want the list of member content
    if (GroupType.NAME.equals(repository.getType().getValue())) {
      return repository.facet(ContentFacet.class).assets().withOnlyGroupMemberContent()
          .browse(LIMIT, continuationToken);
    }
    return repository.facet(ContentFacet.class).assets().browse(LIMIT, continuationToken);
  }

  private List<FluentAsset> removeAssetsNotPermitted(
      final Repository repository,
      final Continuation<FluentAsset> assets)
  {
    return assets.stream()
        .filter(assetPermitted(repository.getFormat().getValue(), repository.getName()))
        .collect(toList());
  }

  Predicate<FluentAsset> assetPermitted(final String format, final String... repositoryNames) {
    return asset -> contentAuthHelper.checkPathPermissions(asset.path(), format, repositoryNames);
  }

  static String toInternalToken(final String continuationToken) {
    if (continuationToken != null) {
      return toInternalId(EntityHelper.id(continuationToken)) + EMPTY;
    }
    return null;
  }

  static <T> List<T> trim(List<T> items, final int limit) {
    if (items.size() > limit) {
      items = items.subList(0, limit);
    }
    return items;
  }
}
