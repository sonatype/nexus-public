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
package org.sonatype.nexus.internal.provisioning;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.BlobStoreApi;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.0
 */
@Named
@Singleton
public class BlobStoreApiImpl
    implements BlobStoreApi
{
  private final BlobStoreManager blobStoreManager;

  @Inject
  public BlobStoreApiImpl(final BlobStoreManager blobStoreManager) {
    this.blobStoreManager = checkNotNull(blobStoreManager);
  }

  @Override
  public BlobStoreConfiguration createFileBlobStore(final String name, final String path) {
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> fileAttributes = new HashMap<>();
    fileAttributes.put("path", checkNotNull(path));
    attributes.put("file", fileAttributes);
    BlobStoreConfiguration blobStoreConfiguration = blobStoreManager.newConfiguration();
    blobStoreConfiguration.setName(name);
    blobStoreConfiguration.setType("File");
    blobStoreConfiguration.setAttributes(attributes);
    return doCreate(blobStoreConfiguration);
  }

  @Override
  public BlobStoreConfiguration createBlobStoreGroup(
      final String name,
      final List<String> memberNames,
      final String fillPolicy)
  {
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> groupAttributes = new HashMap<>();
    groupAttributes.put("members", memberNames);
    groupAttributes.put("fillPolicy", fillPolicy);
    attributes.put("group", groupAttributes);
    BlobStoreConfiguration blobStoreConfiguration = blobStoreManager.newConfiguration();
    blobStoreConfiguration.setName(name);
    blobStoreConfiguration.setType("Group");
    blobStoreConfiguration.setAttributes(attributes);
    return doCreate(blobStoreConfiguration);
  }

  @Override
  public BlobStoreConfiguration createS3BlobStore(final String name, final Map<String, String> s3Config) {
    // re-collecting to move from <String,String> to <String,Object>
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put("s3", new HashMap<>(s3Config));
    BlobStoreConfiguration blobStoreConfiguration = blobStoreManager.newConfiguration();
    blobStoreConfiguration.setName(name);
    blobStoreConfiguration.setType("S3");
    blobStoreConfiguration.setAttributes(attributes);
    return doCreate(blobStoreConfiguration);
  }

  private BlobStoreConfiguration doCreate(final BlobStoreConfiguration blobStoreConfiguration) {
    try {
      return blobStoreManager.create(blobStoreConfiguration).getBlobStoreConfiguration();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
