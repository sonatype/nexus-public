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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.collect.NestedAttributesMap;

/**
 * Default implementation of {@link BlobMetadataStorage} that doesn't store metadata in the blob store.
 *
 * @since 3.31
 */
@Named
@Singleton
public class DefaultBlobMetadataStorage
    implements BlobMetadataStorage
{
  @Override
  public void attach(
      final BlobStore blobStore,
      final BlobId blobId,
      final NestedAttributesMap componentAttributes,
      final NestedAttributesMap assetAttributes, final Map<String, String> checksums) {
    // no-op
  }
}
