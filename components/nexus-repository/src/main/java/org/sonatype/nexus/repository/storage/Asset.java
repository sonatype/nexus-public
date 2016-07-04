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

import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.hash.HashAlgorithm;

import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import org.joda.time.DateTime;

import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_BLOB_REF;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_CONTENT_TYPE;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_SIZE;

/**
 * Metadata about a file, which may or may not belong to a component.
 *
 * @since 3.0
 */
public class Asset
    extends MetadataNode<Asset>
{
  /**
   * Key of {@link Asset} nested map of blob content hashes (if asset has backing content).
   *
   * @see StorageTx#attachBlob(Asset, AssetBlob)
   */
  public static final String CHECKSUM = "checksum";

  /**
   * Key of {@link Asset} nested map containing information regarding the provenance of the blob.
   *
   * @see StorageTx#attachBlob(Asset, AssetBlob)
   * @since 3.1
   */
  public static final String PROVENANCE = "provenance";

  /**
   * Key of {@link Asset} under {@link Asset#PROVENANCE} indicating if the hashes were not verified.
   *
   * @since 3.1
   */
  public static final String HASHES_NOT_VERIFIED = "hashes_not_verified";

  private EntityId componentId;

  private Long size;

  private String contentType;

  private BlobRef blobRef;

  private DateTime lastAccessed;

  /**
   * Gets the component's {@link EntityId} this asset is part of, or {@code null} if it's standalone.
   */
  @Nullable
  public EntityId componentId() {
    return componentId;
  }

  /**
   * Sets the component id this asset is part of.
   */
  Asset componentId(final EntityId componentId) {
    this.componentId = componentId;
    return this;
  }

  /**
   * Gets the size of the file in bytes or {@code null} if undefined.
   */
  @Nullable
  public Long size() {
    return size;
  }

  /**
   * Gets the size of the file in bytes or throws a runtime exception if undefined.
   */
  public Long requireSize() {
    return require(size, P_SIZE);
  }

  /**
   * Sets the size to the given value, or {@code null} to un-define it.
   */
  public Asset size(@Nullable final Long size) {
    this.size = size;
    return this;
  }

  /**
   * Gets the content type or {@code null} if undefined.
   */
  @Nullable
  public String contentType() {
    return contentType;
  }

  /**
   * Gets the content type or throws a runtime exception if undefined.
   */
  public String requireContentType() {
    return require(contentType, P_CONTENT_TYPE);
  }

  /**
   * Sets the content type to the given value, or {@code null} to un-define it.
   */
  public Asset contentType(@Nullable final String contentType) {
    this.contentType = contentType;
    return this;
  }

  /**
   * Gets the blobRef or {@code null} if undefined.
   */
  @Nullable
  public BlobRef blobRef() {
    return blobRef;
  }

  /**
   * Gets the blobRef or throws a runtime exception if undefined.
   */
  public BlobRef requireBlobRef() {
    return require(blobRef, P_BLOB_REF);
  }

  /**
   * Sets the blobRef to the given value, or {@code null} to un-define it.
   */
  Asset blobRef(@Nullable final BlobRef blobRef) {
    this.blobRef = blobRef;
    return this;
  }

  /**
   * Gets the last accessed timestamp or {@code null} if undefined.
   */
  @Nullable
  public DateTime lastAccessed() {
    return lastAccessed;
  }

  /**
   * Sets the last accessed timestamp.
   */
  Asset lastAccessed(final DateTime lastAccessed) {
    this.lastAccessed = lastAccessed;
    return this;
  }

  /**
   * Sets the last accessed timestamp to now, if it has been more than a minute.
   *
   * @return {@code true} if the timestamp was changed, otherwise {@code false}
   */
  public boolean markAsAccessed() {
    DateTime now = DateTime.now();
    if (lastAccessed == null || lastAccessed.isBefore(now.minusMinutes(1))) {
      lastAccessed(now);
      return true;
    }
    return false;
  }

  /**
   * Extract checksums of asset blob if checksums are present in asset attributes.
   */
  public Map<HashAlgorithm, HashCode> getChecksums(final Iterable<HashAlgorithm> hashAlgorithms)
  {
    final NestedAttributesMap checksumAttributes = attributes().child(CHECKSUM);
    final Map<HashAlgorithm, HashCode> hashCodes = Maps.newHashMap();
    for (HashAlgorithm algorithm : hashAlgorithms) {
      final HashCode hashCode = HashCode.fromString(checksumAttributes.require(algorithm.name(), String.class));
      hashCodes.put(algorithm, hashCode);
    }
    return hashCodes;
  }

  /**
   * Extract checksum of asset blob if checksum is present in asset attributes.
   */
  @Nullable
  public HashCode getChecksum(final HashAlgorithm hashAlgorithm)
  {
    String hashCode = attributes().child(CHECKSUM).get(hashAlgorithm.name(), String.class);
    if (hashCode != null) {
      return HashCode.fromString(hashCode);
    }
    return null;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "metadata=" + getEntityMetadata() +
        ", name=" + name() +
        '}';
  }
}
