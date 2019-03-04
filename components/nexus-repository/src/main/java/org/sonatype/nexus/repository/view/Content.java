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
import java.io.OutputStream;
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
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageTx;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.HashCode;
import com.google.common.reflect.TypeToken;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.time.DateHelper.toDate;
import static org.sonatype.nexus.common.time.DateHelper.toDateTime;

/**
 * Content, that is wrapped {@link Payload} with {@link AttributesMap}.
 *
 * @since 3.0
 */
public class Content
    implements Payload
{
  /**
   * Key of {@link Asset} nested map of content related properties.
   */
  @VisibleForTesting
  static final String CONTENT = "content";

  /**
   * Key of content "Last-Modified" attribute, with type {@link DateTime}.
   */
  public static final String CONTENT_LAST_MODIFIED = "lastModified";

  /**
   * Key of content "ETag" attribute, with type of {@link String}.
   */
  public static final String CONTENT_ETAG = "etag";

  /**
   * Key of the "hashCodes" attribute, with type of {@link #T_CONTENT_HASH_CODES_MAP}.
   *
   * Essentially, this is a map of content hashes required by the format from where this content originates.
   */
  public static final String CONTENT_HASH_CODES_MAP = "hashCodesMap";

  public static final TypeToken<Map<HashAlgorithm, HashCode>> T_CONTENT_HASH_CODES_MAP =
      new TypeToken<Map<HashAlgorithm, HashCode>>() {};

  private final Payload payload;

  private final AttributesMap attributes;

  public Content(final Payload payload) {
    this.payload = checkNotNull(payload);
    this.attributes = new AttributesMap();
  }

  /**
   * @since 3.4
   */
  protected Content(final Payload payload, final AttributesMap attributes) {
    this.payload = checkNotNull(payload);
    this.attributes = checkNotNull(attributes);
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

  @Override
  public void close() throws IOException {
    payload.close();
  }

  @Override
  public void copy(final InputStream inputStream, final OutputStream outputStream) throws IOException {
    payload.copy(inputStream, outputStream);
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
   * Key of last modified {@link Date}.
   */
  @VisibleForTesting
  static final String P_LAST_MODIFIED = "last_modified";

  /**
   * Key of etag {@link String}. If present, was extracted from upstream response, and it has quotes removed (by RFC
   * {@code ETag} header value is {@code "quoted"}, this attribute stores it <strong>without quotes</strong>).
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
    final NestedAttributesMap assetAttributes = asset.attributes().child(CONTENT);
    final DateTime lastModified = toDateTime(assetAttributes.get(P_LAST_MODIFIED, Date.class));
    final String etag = assetAttributes.get(P_ETAG, String.class);

    final Map<HashAlgorithm, HashCode> checksums = asset.getChecksums(hashAlgorithms);

    contentAttributes.set(Asset.class, asset);
    contentAttributes.set(Content.CONTENT_LAST_MODIFIED, lastModified);
    contentAttributes.set(Content.CONTENT_ETAG, etag);
    contentAttributes.set(Content.CONTENT_HASH_CODES_MAP, checksums);
    contentAttributes.set(CacheInfo.class, CacheInfo.extractFromAsset(asset));
  }

  /**
   * Applies non-format specific content attributes onto passed in {@link Asset} from passed in {@link AttributesMap}
   * (usually originating from {@link Content#getAttributes()}).
   */
  public static void applyToAsset(final Asset asset, final AttributesMap contentAttributes) {
    checkNotNull(asset);
    checkNotNull(contentAttributes);
    final NestedAttributesMap assetAttributes = asset.attributes().child(CONTENT);
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
  public static Asset findAsset(final StorageTx tx, final Bucket bucket, Content content) {
    final Asset contentAsset = content.getAttributes().require(Asset.class);
    if (EntityHelper.hasMetadata(contentAsset)) {
      return tx.findAsset(EntityHelper.id(contentAsset), bucket);
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
