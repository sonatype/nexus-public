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
package org.sonatype.nexus.repository.content.store;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.repository.content.AssetBlob;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;

/**
 * {@link AssetBlob} data backed by the content data store.
 *
 * @since 3.20
 */
public class AssetBlobData
    implements AssetBlob
{
  Integer assetBlobId; // NOSONAR: internal id

  private BlobRef blobRef;

  private long blobSize;

  private String contentType;

  private Map<String, String> checksums;

  private LocalDateTime blobCreated;

  @Nullable
  private String createdBy;

  @Nullable
  private String createdByIp;

  // AssetBlob API

  @Override
  public BlobRef blobRef() {
    return blobRef;
  }

  @Override
  public long blobSize() {
    return blobSize;
  }

  @Override
  public String contentType() {
    return contentType;
  }

  @Override
  public Map<String, String> checksums() {
    return checksums;
  }

  @Override
  public LocalDateTime blobCreated() {
    return blobCreated;
  }

  @Override
  public Optional<String> createdBy() {
    return ofNullable(createdBy);
  }

  @Override
  public Optional<String> createdByIp() {
    return ofNullable(createdByIp);
  }

  // MyBatis setters + validation

  /**
   * Sets the internal asset blob id.
   */
  public void setAssetBlobId(final int assetBlobId) {
    this.assetBlobId = assetBlobId;
  }

  /**
   * Sets the reference to the blob.
   */
  public void setBlobRef(final BlobRef blobRef) {
    this.blobRef = checkNotNull(blobRef);
  }

  /**
   * Sets the size of the blob.
   */
  public void setBlobSize(final long blobSize) {
    this.blobSize = blobSize;
  }

  /**
   * Sets the content-type of the blob.
   */
  public void setContentType(final String contentType) {
    this.contentType = checkNotNull(contentType);
  }

  /**
   * Sets the checksums for the blob.
   *
   * @since 3.next
   */
  public void setChecksums(final Map<String, String> checksums) {
    this.checksums = checkNotNull(checksums);
  }

  /**
   * Sets when the blob was created.
   */
  public void setBlobCreated(final LocalDateTime blobCreated) {
    this.blobCreated = checkNotNull(blobCreated);
  }

  /**
   * Sets the user that triggered creation of this blob.
   */
  public void setCreatedBy(@Nullable final String createdBy) {
    this.createdBy = createdBy;
  }

  /**
   * Sets the client IP that triggered creation of this blob.
   */
  public void setCreatedByIp(@Nullable final String createdByIp) {
    this.createdByIp = createdByIp;
  }
}
