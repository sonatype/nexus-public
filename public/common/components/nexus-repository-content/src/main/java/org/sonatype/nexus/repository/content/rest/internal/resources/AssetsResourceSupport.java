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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.selector.ContentAuthHelper;
import org.sonatype.nexus.repository.types.GroupType;

import com.google.common.annotations.VisibleForTesting;
import jakarta.ws.rs.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.sonatype.nexus.repository.content.store.InternalIds.toInternalId;

/**
 * Support class for {@link AssetsResource} which fetches and returns only assets that the user is permitted
 * to view according to {@link ContentAuthHelper#checkPathPermissions(String, String, String...)}
 *
 * @since 3.27
 */
public abstract class AssetsResourceSupport
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Limit the number of assets returned per page. This value is aligned with ComponentsResourceSupport.PAGE_SIZE_LIMIT.
   */
  protected static final int PAGE_SIZE_LIMIT = 100;

  protected static final int INCREASED_PAGE_SIZE_LIMIT = 5000;

  private final ContentAuthHelper contentAuthHelper;

  public AssetsResourceSupport(final ContentAuthHelper contentAuthHelper) {
    this.contentAuthHelper = checkNotNull(contentAuthHelper);
  }

  @VisibleForTesting
  List<FluentAsset> browse(final Repository repository, final String continuationToken) {
    List<FluentAsset> permittedAssets = new ArrayList<>();
    String internalToken = toInternalToken(continuationToken);
    Continuation<FluentAsset> assetContinuation = getAssets(repository, internalToken);

    while (permittedAssets.size() < PAGE_SIZE_LIMIT && !assetContinuation.isEmpty()) {
      permittedAssets.addAll(removeAssetsNotPermitted(repository, assetContinuation));
      assetContinuation = getAssets(repository, assetContinuation.nextContinuationToken());
    }
    return trim(permittedAssets, PAGE_SIZE_LIMIT);
  }

  @VisibleForTesting
  List<FluentAsset> browseEager(
      final Repository repository,
      final String continuationToken)
  {
    return browseEager(repository, continuationToken, PAGE_SIZE_LIMIT, null, null);
  }

  protected List<FluentAsset> browseEager(
      final Repository repository,
      final String continuationToken,
      final int pageSize,
      @Nullable final String newerThan,
      @Nullable final String olderThan)
  {

    // Parse timestamps if provided
    OffsetDateTime olderThanTimestamp = parseTimestamp(olderThan);
    OffsetDateTime newerThanTimestamp = parseTimestamp(newerThan);

    List<FluentAsset> permittedAssets = new ArrayList<>();
    // browseEager uses composite continuation tokens (blob_created + asset_id), so pass through directly
    Continuation<FluentAsset> assetContinuation =
        getAssetsEager(repository, continuationToken, PAGE_SIZE_LIMIT, newerThanTimestamp, olderThanTimestamp);

    while (permittedAssets.size() < PAGE_SIZE_LIMIT && !assetContinuation.isEmpty()) {
      permittedAssets.addAll(removeAssetsNotPermitted(repository, assetContinuation));
      assetContinuation = getAssetsEager(repository, assetContinuation.nextContinuationToken(), PAGE_SIZE_LIMIT,
          newerThanTimestamp, olderThanTimestamp);
    }

    return trim(permittedAssets, PAGE_SIZE_LIMIT);
  }

  private Continuation<FluentAsset> getAssets(final Repository repository, final String continuationToken) {
    // helper for users, if they query by group chances are they want the list of member content
    if (GroupType.NAME.equals(repository.getType().getValue())) {
      return repository.facet(ContentFacet.class)
          .assets()
          .withOnlyGroupMemberContent()
          .browse(PAGE_SIZE_LIMIT, continuationToken);
    }
    return repository.facet(ContentFacet.class).assets().browse(PAGE_SIZE_LIMIT, continuationToken);
  }

  private Continuation<FluentAsset> getAssetsEager(
      final Repository repository,
      final String continuationToken,
      final int pageLimit,
      @Nullable final OffsetDateTime newerThan,
      @Nullable final OffsetDateTime olderThan)
  {
    // For now, we only support timestamp filtering on direct repository calls, not group member content
    if (GroupType.NAME.equals(repository.getType().getValue()) || olderThan == null && newerThan == null) {
      if (GroupType.NAME.equals(repository.getType().getValue())) {
        return repository.facet(ContentFacet.class)
            .assets()
            .withOnlyGroupMemberContent()
            .browseEager(pageLimit, continuationToken);
      }
      return repository.facet(ContentFacet.class).assets().browseEager(pageLimit, continuationToken);
    }

    // Use the new method with timestamp filtering
    return ((org.sonatype.nexus.repository.content.fluent.internal.FluentAssetsImpl) repository
        .facet(ContentFacet.class)
        .assets())
            .browseEager(pageLimit, continuationToken, newerThan, olderThan);
  }

  @Nullable
  private OffsetDateTime parseTimestamp(@Nullable final String timestamp) {
    if (timestamp == null || timestamp.trim().isEmpty()) {
      return null;
    }
    try {
      return OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
    catch (Exception e) {
      log.warn("Invalid timestamp format: {}. Expected ISO-8601 format.", timestamp);
      return null;
    }
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
      try {
        return toInternalId(EntityHelper.id(continuationToken)) + EMPTY;
      }
      catch (NumberFormatException e) {
        throw new BadRequestException("Invalid continuation token");
      }
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
