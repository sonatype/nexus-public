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
package org.sonatype.nexus.blobstore.file.rest;

import javax.validation.constraints.NotEmpty;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.blobstore.file.internal.FileBlobStoreDescriptor;
import org.sonatype.nexus.blobstore.rest.BlobStoreApiModel;

import io.swagger.annotations.ApiModelProperty;

public class FileBlobStoreApiModel
    extends BlobStoreApiModel
{
  @ApiModelProperty(FileBlobStoreDescriptor.PATH_HELP_TEXT)
  @NotEmpty(message = "Path is required")
  private String path;

  public FileBlobStoreApiModel() {
    super();
  }

  public FileBlobStoreApiModel(final BlobStoreConfiguration configuration) {
    super(configuration);
    this.path = configuration.attributes(FileBlobStore.CONFIG_KEY).get(FileBlobStore.PATH_KEY, String.class);
  }

  public String getPath() {
    return path;
  }

  public void setPath(final String path) {
    this.path = path;
  }

  @Override
  public BlobStoreConfiguration toBlobStoreConfiguration(final BlobStoreConfiguration configuration) {
    BlobStoreConfiguration newConfig = super.toBlobStoreConfiguration(configuration);
    newConfig.setType(FileBlobStore.TYPE);
    newConfig.attributes(FileBlobStore.CONFIG_KEY).set(FileBlobStore.PATH_KEY, path);
    return newConfig;
  }
}
