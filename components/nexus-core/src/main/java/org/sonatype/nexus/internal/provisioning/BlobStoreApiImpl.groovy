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
    def attributes = [file: [path: checkNotNull(path)]]
    def config = blobStoreManager.newConfiguration()
    config.name = name
    config.setType('File')
    config.attributes = [file: [path: checkNotNull(path)]]
    return blobStoreManager.create(config).blobStoreConfiguration
  }

  @Override
  BlobStoreConfiguration createBlobStoreGroup(final String name, List<String> memberNames, String fillPolicy) {
    def attributes = [group: [members: memberNames, fillPolicy: fillPolicy]]
    def config = blobStoreManager.newConfiguration()
    config.setName(name)
    config.setType('Group')
    config.setAttributes(attributes)
    return blobStoreManager.create(config).blobStoreConfiguration
  }

  @Override
  BlobStoreConfiguration createS3BlobStore(final String name, final Map<String, String> s3Config) {
    def attributes = [s3: s3Config]
    def config = blobStoreManager.newConfiguration()
    config.setName(name)
    config.setType('S3')
    config.setAttributes(attributes)
    return blobStoreManager.create(config).blobStoreConfiguration
  }
}
