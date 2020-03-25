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
package org.sonatype.nexus.repository;

import org.sonatype.nexus.blobstore.api.BlobRef;

/**
 * Thrown when attempting to access blob content which is now missing from the blobstore.
 *
 * @since 3.22
 */
public class MissingBlobException
    extends IllegalStateException
{
  private final BlobRef blobRef;

  public MissingBlobException(final BlobRef blobRef) {
    super(String.format("Blob %s exists in metadata, but is missing from the blobstore", blobRef));
    this.blobRef = blobRef;
  }

  public BlobRef getBlobRef() {
    return blobRef;
  }
}
