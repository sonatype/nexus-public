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
package org.sonatype.nexus.repository.replication;

import java.util.Objects;

/**
 * @since 3.31
 */
public class BlobEvent
{
  private String blobId;

  private String assetPath;

  private String repositoryName;

  private String replicationConnectionId;

  private BlobEventType blobEventType;

  private boolean inUse;

  private int retryCount = 0;

  public void retry() {
    retryCount++;
  }

  public boolean shouldRetry() {
    return retryCount > 0;
  }

  public String getBlobId() {
    return blobId;
  }

  public String getAssetPath() {
    return assetPath;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getReplicationConnectionId() {
    return replicationConnectionId;
  }

  public BlobEventType getBlobEventType() {
    return blobEventType;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public boolean isInUse() {
    return inUse;
  }

  public BlobEvent withBlobId(final String blobId) {
    this.blobId = blobId;
    return this;
  }

  public BlobEvent withAssetPath(final String assetPath) {
    this.assetPath = assetPath;
    return this;
  }

  public BlobEvent withRepositoryName(final String repositoryName) {
    this.repositoryName = repositoryName;
    return this;
  }

  public BlobEvent withReplicationConnectionId(final String replicationConnectionId) {
    this.replicationConnectionId = replicationConnectionId;
    return this;
  }

  public BlobEvent withBlobEventType(final BlobEventType blobEventType) {
    this.blobEventType = blobEventType;
    return this;
  }

  public BlobEvent withInUse(final boolean inUse) {
    this.inUse = inUse;
    return this;
  }

  public BlobEvent withRetryCount(final int retryCount) {
    this.retryCount = retryCount;
    return this;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BlobEvent blobEvent = (BlobEvent) o;
    return inUse == blobEvent.inUse && retryCount == blobEvent.retryCount &&
        Objects.equals(blobId, blobEvent.blobId) &&
        Objects.equals(assetPath, blobEvent.assetPath) &&
        Objects.equals(repositoryName, blobEvent.repositoryName) &&
        Objects.equals(replicationConnectionId, blobEvent.replicationConnectionId) &&
        blobEventType == blobEvent.blobEventType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(blobId, assetPath, repositoryName, replicationConnectionId, blobEventType, inUse, retryCount);
  }

  @Override
  public String toString() {
    return "BlobEvent{" +
        "blobId='" + blobId + '\'' +
        ", assetPath='" + assetPath + '\'' +
        ", repositoryName='" + repositoryName + '\'' +
        ", replicationConnectionId='" + replicationConnectionId + '\'' +
        ", blobEventType=" + blobEventType +
        ", inUse=" + inUse +
        ", retryCount=" + retryCount +
        '}';
  }
}
