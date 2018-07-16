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

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.node.NodeAccess;

import com.google.common.hash.HashCode;

/**
 * Marker class for a pre-fetched asset blob
 * 
 * @since 3.13
 */
public class PrefetchedAssetBlob
  extends AssetBlob
{
  public PrefetchedAssetBlob(final NodeAccess nodeAccess,
                             final BlobStore blobStore,
                             final Blob blob,
                             final String contentType,
                             final Map<HashAlgorithm, HashCode> hashes,
                             final boolean hashesVerified)
  {
    super(nodeAccess, blobStore, store -> blob, contentType, hashes, hashesVerified);
    /*
     * Pre-fetched blobs are stored in the blob-store but when a duplicate is encountered it is not used meaning it needs
     * to be cleaned up. Because it hasn't been ingested the cleanup fails on BlobTx.commit, this method ingests the blob
     * to ensure that it is correctly deleted. We use this class so that we don't eagerly ingest ALL blobs.
     */
    getBlob();
  }
}
