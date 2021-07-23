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
package org.sonatype.nexus.blobstore.group;

import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.group.BlobStoreGroup;

/**
 * Chooses a group member to store a new blob.
 *
 * @since 3.14
 */
public interface FillPolicy
{
  /**
   * Validate the {@link BlobStoreGroup} is correctly configured for this {@link FillPolicy}.
   */
  default void validateBlobStoreGroup(BlobStoreGroup blobStoreGroup) {
    //default to a valid blob store group
  }

  String getName();

  /**
   * Choose the blob store group member to write a new temp blob to.
   */
  @Nullable
  BlobStore chooseBlobStoreForCreate(BlobStoreGroup blobStoreGroup, Map<String, String> headers);

  /**
   * Choose the blob store group member to copy a blob to, when making temp blob a permanent one.
   */
  @Nullable
  BlobStore chooseBlobStoreForCopy(BlobStoreGroup blobStoreGroup, BlobStore sourceBlobStore, Map<String, String> headers);
}
