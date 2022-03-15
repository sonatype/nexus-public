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
package org.sonatype.nexus.repository.view.payloads;

import java.util.Map;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.hash.HashAlgorithm;

import com.google.common.hash.HashCode;

/**
 * A {@link TempBlob} which represents an ingested {@link Blob} that as become detached from its asset. When Nexus is
 * using SQL mode there is special handling which can attempt to re-attach the blob rather than copy it.
 */
public class AttachableBlob
    extends TempBlob
{
  private boolean attached = false;

  public AttachableBlob(
      final Blob blob,
      final Map<HashAlgorithm, HashCode> hashes,
      final boolean hashesVerified,
      final BlobStore blobStore)
  {
    super(blob, hashes, hashesVerified, blobStore);
  }

  /**
   * Marks this instances as having been attached.
   */
  public void markAttached() {
    this.attached = true;
  }

  /**
   * Check whether the blob has been attached since this was called.
   */
  public boolean isAttached() {
    return attached;
  }

  @Override
  public void close() {
    // no-op
  }
}
