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
package org.sonatype.nexus.blobstore.api;

import java.io.Serializable;
import java.time.OffsetDateTime;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A unique identifier for a blob within a specific {@link BlobStore}.
 *
 * @since 3.0
 */
public class BlobId
    implements Serializable, Comparable<BlobId>
{
  private final String id;

  private final OffsetDateTime blobCreatedRef;

  /**
   * @deprecated Use {@link #BlobId(String, OffsetDateTime)} instead.
   */
  @Deprecated
  public BlobId(final String id) {
    this(id, null);
  }

  public BlobId(final String id, @Nullable OffsetDateTime blobCreatedRef) {
    this.id = checkNotNull(id);
    this.blobCreatedRef = blobCreatedRef;
  }

  public String asUniqueString() {
    return id;
  }

  public OffsetDateTime getBlobCreatedRef() {
    return blobCreatedRef;
  }

  @Override
  public String toString() {
    return id;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BlobId blobId = (BlobId) o;

    return id.equals(blobId.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public int compareTo(final BlobId o) {
    return id.compareTo(o.id);
  }
}
