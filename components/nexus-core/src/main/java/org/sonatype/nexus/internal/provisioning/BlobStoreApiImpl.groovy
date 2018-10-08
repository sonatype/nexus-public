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
package org.sonatype.nexus.internal.provisioning

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import org.sonatype.nexus.BlobStoreApi
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration
import org.sonatype.nexus.blobstore.api.BlobStoreManager

import static com.google.common.base.Preconditions.checkNotNull

/**
 * @since 3.0
 */
@Named
@Singleton
class BlobStoreApiImpl
    implements BlobStoreApi
{
  @Inject
  BlobStoreManager blobStoreManager

  @Override
  BlobStoreConfiguration createFileBlobStore(final String name, final String path) {
    return blobStoreManager.create(new BlobStoreConfiguration(name: checkNotNull(name), type: 'File',
        attributes: [file: [path: checkNotNull(path)]])).blobStoreConfiguration
  }

  @Override
  BlobStoreConfiguration createBlobStoreGroup(final String name, List<String> memberNames, String fillPolicy) {
    return blobStoreManager.create(new BlobStoreConfiguration(name: checkNotNull(name), type: 'Group',
        attributes: [group: [members: memberNames, fillPolicy: fillPolicy]])).blobStoreConfiguration
  }

  @Override
  BlobStoreConfiguration createS3BlobStore(final String name, final Map<String, String> config) {
    return blobStoreManager.create(new BlobStoreConfiguration(name: checkNotNull(name), type: 'S3',
        attributes: [s3: config])).blobStoreConfiguration
  }
}
