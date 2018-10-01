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
package org.sonatype.nexus.repository.storage;

import java.util.Objects;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.entity.ConflictState;
import org.sonatype.nexus.orient.entity.DeconflictStepSupport;

import com.orientechnologies.orient.core.record.impl.ODocument;

import static org.sonatype.nexus.orient.entity.ConflictState.ALLOW;
import static org.sonatype.nexus.orient.entity.ConflictState.DENY;
import static org.sonatype.nexus.orient.entity.ConflictState.IGNORE;
import static org.sonatype.nexus.orient.entity.ConflictState.MERGE;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_LAST_DOWNLOADED;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_LAST_UPDATED;

/**
 * Deconflicts asset metadata:
 *
 * "last_updated"          - book-keeping attribute, we're only interested in the latest time
 * "last_downloaded"       - book-keeping attribute, we're only interested in the latest time
 * "cache.last_verified"   - book-keeping attribute, we're only interested in the latest time
 * "cache.cache_token"     - for invalidation: "invalidated" always wins, non-null wins over null
 * "content.last_modified" - book-keeping attribute, we're only interested in the latest time
 *
 * @since 3.14
 */
@Named
@Singleton
public class DeconflictAssetMetadata
    extends DeconflictStepSupport<Asset>
{
  private static final String CACHE_ATTRIBUTES = P_ATTRIBUTES + ".cache";

  private static final String CACHE_TOKEN_ATTRIBUTE = CACHE_ATTRIBUTES + ".cache_token";

  private static final String CACHE_TOKEN_INVALIDATED = "invalidated";

  private static final String LAST_VERIFIED_ATTRIBUTE = CACHE_ATTRIBUTES + ".last_verified";

  private static final String CONTENT_ATTRIBUTES = P_ATTRIBUTES + ".content";

  private static final String LAST_MODIFIED_ATTRIBUTE = CONTENT_ATTRIBUTES + ".last_modified";

  @Override
  public ConflictState deconflict(final ODocument storedRecord, final ODocument changeRecord) {
    return resolveCache(storedRecord, changeRecord)
        .andThen(() -> resolveContent(storedRecord, changeRecord)
        .andThen(() -> pickLatest(storedRecord, changeRecord, P_LAST_DOWNLOADED)
        .andThen(() -> pickLatest(storedRecord, changeRecord, P_LAST_UPDATED))));
  }

  /**
   * Resolves minor book-keeping differences in cache information.
   */
  public static ConflictState resolveCache(final ODocument storedRecord, final ODocument changeRecord) {
    Object storedCache = storedRecord.rawField(CACHE_ATTRIBUTES);
    Object changeCache = changeRecord.rawField(CACHE_ATTRIBUTES);
    if (storedCache != null && changeCache != null) {
      return resolveCacheToken(storedRecord, changeRecord)
          .andThen(() -> pickLatest(storedRecord, changeRecord, LAST_VERIFIED_ATTRIBUTE));
    }
    else if (changeCache != null) {
      storedRecord.field(CACHE_ATTRIBUTES, changeCache);
      return ALLOW;
    }
    else if (storedCache != null) {
      changeRecord.field(CACHE_ATTRIBUTES, storedCache);
      return MERGE;
    }
    return IGNORE;
  }

  /**
   * Resolves differences in cache tokens: "invalidated" always wins, non-null tokens win over null tokens.
   */
  private static ConflictState resolveCacheToken(final ODocument storedRecord, final ODocument changeRecord) {
    Object storedToken = storedRecord.rawField(CACHE_TOKEN_ATTRIBUTE);
    Object changeToken = changeRecord.rawField(CACHE_TOKEN_ATTRIBUTE);
    if (Objects.equals(storedToken, changeToken)) {
      return IGNORE;
    }
    else if (storedToken == null || CACHE_TOKEN_INVALIDATED.equals(changeToken)) {
      storedRecord.field(CACHE_TOKEN_ATTRIBUTE, changeToken);
      return ALLOW;
    }
    else if (changeToken == null || CACHE_TOKEN_INVALIDATED.equals(storedToken)) {
      changeRecord.field(CACHE_TOKEN_ATTRIBUTE, storedToken);
      return MERGE;
    }
    return DENY;
  }

  /**
   * Resolves minor book-keeping differences in content information.
   */
  public static ConflictState resolveContent(final ODocument storedRecord, final ODocument changeRecord) {
    Object storedContent = storedRecord.rawField(CONTENT_ATTRIBUTES);
    Object changeContent = changeRecord.rawField(CONTENT_ATTRIBUTES);
    if (storedContent != null && changeContent != null) {
      return pickLatest(storedRecord, changeRecord, LAST_MODIFIED_ATTRIBUTE);
    }
    else if (changeContent != null) {
      storedRecord.field(CONTENT_ATTRIBUTES, changeContent);
      return ALLOW;
    }
    else if (storedContent != null) {
      changeRecord.field(CONTENT_ATTRIBUTES, storedContent);
      return MERGE;
    }
    return IGNORE;
  }
}
