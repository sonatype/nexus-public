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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_BLOB_REF;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_CONTENT_TYPE;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_SIZE;

/**
 * Metadata about a file, which may or may not belong to a component.
 *
 * @since 3.0
 */
public class Asset
    extends AbstractMetadataNode<Asset>
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

  private DateTime lastDownloaded;

  private DateTime blobCreated;

  private DateTime blobUpdated;

  private String createdBy;

  private String createdByIp;

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
  public Asset componentId(final EntityId componentId) {
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
   * Gets the user used to create this
   *
   * @since 3.6.1
   */
  @Nullable
  public String createdBy() {
    return createdBy;
  }

  /**
   * Gets the ip address used to create this
   *
   * @since 3.6.1
   */
  @Nullable
  public String createdByIp() {
    return createdByIp;
  }

  /**
   * Sets the user used to create this
   *
   * @since 3.6.1
   */
  public Asset createdBy(@Nullable final String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * Sets the IP address used to create this
   *
   * @since 3.6.1
   */
  public Asset createdByIp(@Nullable final String createdByIp) {
    this.createdByIp = createdByIp;
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
  public Asset blobRef(@Nullable final BlobRef blobRef) {
    this.blobRef = blobRef;
    return this;
  }

  /**
   * Gets the last downloaded timestamp or {@code null} if undefined. Note that this can also reflect the timestamp for
   * initial upload if the artifact has not yet been downloaded.
   */
  @Nullable
  public DateTime lastDownloaded() {
    return lastDownloaded;
  }

  /**
   * Sets the last downloaded timestamp.
   */
  @VisibleForTesting
  public Asset lastDownloaded(final DateTime lastDownloaded) {
    this.lastDownloaded = lastDownloaded;
    return this;
  }

  /**
   * Sets the last downloaded timestamp to now, if it has been longer than {@param lastDownloadedInterval}
   *
   * @return {@code true} if the timestamp was changed, otherwise {@code false}
   */
  public boolean markAsDownloaded(final Duration lastDownloadedInterval) {
    DateTime now = DateTime.now();
    if (lastDownloaded == null || lastDownloaded.isBefore(now.minus(lastDownloadedInterval))) {
      lastDownloaded(now);
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

  /**
   * Gets the blob_created timestamp or null if undefined.
   *
   * @since 3.3
   */
  @Nullable
  public DateTime blobCreated() {
    return blobCreated;
  }

  /**
   * Sets the blob_created timestamp.
   *
   * @since 3.3
   */
  public Asset blobCreated(@Nullable final DateTime blobCreated) {
    this.blobCreated = blobCreated;
    return this;
  }

  /**
   * Gets the blob_updated timestamp or null if undefined.
   *
   * @since 3.3
   */
  @Nullable
  public DateTime blobUpdated() {
    return blobUpdated;
  }

  /**
   * Sets the blob_updated timestamp.
   *
   * @since 3.3
   */
  public Asset blobUpdated(@Nullable final DateTime blobUpdated) {
    this.blobUpdated = blobUpdated;
    return this;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "metadata=" + getEntityMetadata() +
        ", name=" + name() +
        '}';
  }
}
