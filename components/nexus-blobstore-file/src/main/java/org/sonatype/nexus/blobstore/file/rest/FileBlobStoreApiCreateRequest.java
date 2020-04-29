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

/**
 * @since 3.19
 */
public class FileBlobStoreApiCreateRequest
    extends FileBlobStoreApiModel
{
  @NotEmpty(message = "Name is required")
  private String name;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  @Override
  public BlobStoreConfiguration toBlobStoreConfiguration(final BlobStoreConfiguration oldConfig) {
    BlobStoreConfiguration newConfig = super.toBlobStoreConfiguration(oldConfig);
    newConfig.setName(name);
    return newConfig;
  }
}
