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

import java.util.StringJoiner;

import javax.annotation.Nullable;

/**
 * @since 3.0
 */
public class BlobStoreException
    extends RuntimeException
{
  private static final long serialVersionUID = 1L;

  private final BlobId blobId;

  // transient: defaults to false on deserialization, which is the safe fallback
  private final transient boolean causeEmbeddedInMessage;

  public BlobStoreException(final String message, @Nullable final BlobId blobId) {
    super(message);
    this.blobId = blobId;
    this.causeEmbeddedInMessage = false;
  }

  public BlobStoreException(final String message, final Throwable cause, @Nullable final BlobId blobId) {
    super(message, cause);
    this.blobId = blobId;
    this.causeEmbeddedInMessage = false;
  }

  public BlobStoreException(final Throwable cause, @Nullable final BlobId blobId) {
    super(cause);
    this.blobId = blobId;
    this.causeEmbeddedInMessage = true;
  }

  /**
   * The BlobId of the blob related to this exception, or {@code null} if there is none.
   */
  @Nullable
  public BlobId getBlobId() {
    return blobId;
  }

  @Override
  public String getMessage() {
    final StringJoiner joiner = new StringJoiner(", ");

    if (blobId != null) {
      joiner.add("BlobId: " + blobId);
    }

    String msg = super.getMessage();
    if (msg != null) {
      joiner.add(msg);
    }

    if (!causeEmbeddedInMessage && getCause() != null && getCause().getMessage() != null) {
      joiner.add("Cause: " + getCause().getMessage());
    }

    String result = joiner.toString();
    return result.isEmpty() ? null : result;
  }
}
