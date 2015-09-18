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
package org.sonatype.nexus.repository.view;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.util.TypeTokens;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.time.DateHelper.toDate;
import static org.sonatype.nexus.common.time.DateHelper.toDateTime;

/**
 * Content, that is wrapped {@link Payload} with {@link Attributes}.
 *
 * @since 3.0
 */
public class Content
    implements Payload
{
  /**
   * Key of content "Last-Modified" attribute, with type {@link DateTime}.
   */
  public static final String CONTENT_LAST_MODIFIED = "lastModified";

  /**
   * Key of content "ETag" attribute, with type of {@link String}.
   */
  public static final String CONTENT_ETAG = "etag";

  /**
   * Key of the "hashCodes" attribute, with type of {@link TypeTokens#HASH_CODES_MAP}. Essentially, this is a map
   * of content hashes required by the format from where this content originates.
   *
   * @see TypeTokens#HASH_CODES_MAP
   */
  public static final String CONTENT_HASH_CODES_MAP = "hashCodesMap";

  private final Payload payload;

  private final AttributesMap attributes;

  public Content(final Payload payload) {
    this.payload = checkNotNull(payload);
    this.attributes = new AttributesMap();
  }

  @Override
  public InputStream openInputStream() throws IOException {
    return payload.openInputStream();
  }

  @Override
  public long getSize() {
    return payload.getSize();
  }

  @Nullable
  @Override
  public String getContentType() {
    return payload.getContentType();
  }

  @Nonnull
  public AttributesMap getAttributes() {
    return attributes;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "payload=" + payload +
        ", attributes='" + attributes + '\'' +
        '}';
  }

  // Persistence of attributes to and from Asset

  /**
   * Key of the nested map on {@link Asset} attributes carrying content related attributes.
   */
  @VisibleForTesting
  static final String P_CONTENT_ATTRIBUTES = "content_attributes";

  /**
   * Key of last modified {@link Date}.
   */
  @VisibleForTesting
  static final String P_LAST_MODIFIED = "last_modified";

  /**
   * Key of etag {@link String}. If extracted from upstream response, it must have quotes removed.
   */
  @VisibleForTesting
  static final String P_ETAG = "etag";

  /**
   * Extracts non-format specific content attributes into the passed in {@link AttributesMap} (usually originating from
   * {@link Content#getAttributes()}) from passed in {@link Asset} and format required hashes.
   */
  public static void extractFromAsset(final Asset asset,
                                      final Iterable<HashAlgorithm> hashAlgorithms,
                                      final AttributesMap contentAttributes)
  {
    checkNotNull(asset);
    checkNotNull(hashAlgorithms);
    final NestedAttributesMap assetAttributes = asset.attributes().child(P_CONTENT_ATTRIBUTES);
    final DateTime lastModified = toDateTime(assetAttributes.get(P_LAST_MODIFIED, Date.class));
    final String etag = assetAttributes.get(P_ETAG, String.class);

    final NestedAttributesMap checksumAttributes = asset.attributes().child(StorageFacet.P_CHECKSUM);
    final Map<HashAlgorithm, HashCode> hashCodes = Maps.newHashMap();
    for (HashAlgorithm algorithm : hashAlgorithms) {
      final HashCode hashCode = HashCode.fromString(checksumAttributes.require(algorithm.name(), String.class));
      hashCodes.put(algorithm, hashCode);
    }

    contentAttributes.set(Asset.class, asset);
    contentAttributes.set(Content.CONTENT_LAST_MODIFIED, lastModified);
    contentAttributes.set(Content.CONTENT_ETAG, etag);
    contentAttributes.set(Content.CONTENT_HASH_CODES_MAP, hashCodes);
    contentAttributes.set(CacheInfo.class, CacheInfo.extractFromAsset(asset));
  }

  /**
   * Applies non-format specific content attributes onto passed in {@link Asset} from passed in {@link AttributesMap}
   * (usually originating from {@link Content#getAttributes()}).
   */
  public static void applyToAsset(final Asset asset, final AttributesMap contentAttributes) {
    checkNotNull(asset);
    checkNotNull(contentAttributes);
    final NestedAttributesMap assetAttributes = asset.attributes().child(P_CONTENT_ATTRIBUTES);
    assetAttributes.set(P_LAST_MODIFIED, toDate(contentAttributes.get(Content.CONTENT_LAST_MODIFIED, DateTime.class)));
    assetAttributes.set(P_ETAG, contentAttributes.get(Content.CONTENT_ETAG, String.class));
    final CacheInfo cacheInfo = contentAttributes.get(CacheInfo.class);
    if (cacheInfo != null) {
      CacheInfo.applyToAsset(asset, cacheInfo);
    }
  }

  /**
   * Finds fresh {@link Asset} instance from passed in TX by entity ID of the {@link Asset} used
   * to create passed in {@link Content} instance. The passed in {@link Content} must have been created with
   * method {@link #extractFromAsset(Asset, Iterable, AttributesMap)} to have proper attributes set for this operation.
   *
   * @see #extractFromAsset(Asset, Iterable, AttributesMap)
   */
  @Nullable
  public static Asset findAsset(final StorageTx tx, Content content) {
    final Asset contentAsset = content.getAttributes().require(Asset.class);
    if (EntityHelper.hasMetadata(contentAsset)) {
      return tx.findAsset(EntityHelper.id(contentAsset), tx.getBucket());
    }
    return null;
  }

  /**
   * Maintains the "last modified" attribute of the content by setting it to "now". It accepts {@code null}s, and the
   * returned instance will have changes applied. Never returns {@code null}.
   */
  @Nonnull
  public static AttributesMap maintainLastModified(final Asset asset,
                                                   @Nullable final AttributesMap contentAttributes)
  {
    AttributesMap ca = contentAttributes;
    if (ca == null) {
      ca = new AttributesMap();
    }
    if (!ca.contains(Content.CONTENT_LAST_MODIFIED)) {
      ca.set(Content.CONTENT_LAST_MODIFIED, DateTime.now());
    }
    return ca;
  }
}
