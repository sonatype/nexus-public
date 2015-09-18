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

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.entity.EntityId;

import org.joda.time.DateTime;

import static org.sonatype.nexus.repository.storage.StorageFacet.P_BLOB_REF;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_CONTENT_TYPE;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_SIZE;

/**
 * Metadata about a file, which may or may not belong to a component.
 *
 * @since 3.0
 */
public class Asset
    extends MetadataNode<Asset>
{
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
  public Asset size(final @Nullable Long size) {
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
  public Asset contentType(final @Nullable String contentType) {
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
  Asset blobRef(final @Nullable BlobRef blobRef) {
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

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "metadata=" + getEntityMetadata() +
        ", name=" + name() +
        '}';
  }
}
