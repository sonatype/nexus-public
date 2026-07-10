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
package org.sonatype.nexus.api.rest.selfhosted.blobstore.file;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.ws.rs.Path;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;

import io.swagger.v3.oas.annotations.Hidden;

import static org.sonatype.nexus.api.rest.selfhosted.blobstore.file.FileBlobStoreApiResourceBeta.RESOURCE_URI;
import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;
import org.springframework.stereotype.Component;

/**
 * beta endpoint for File BlobStore REST API
 *
 * @since 3.24
 * @deprecated moving to {@link FileBlobStoreApiResourceV1}
 */
@Hidden
@Component
@Path(RESOURCE_URI)
@Deprecated
public class FileBlobStoreApiResourceBeta
    extends FileBlobStoreResource
{
  static final String RESOURCE_URI = BETA_API_PREFIX + "/blobstores/file";

  @Autowired
  public FileBlobStoreApiResourceBeta(final BlobStoreManager blobStoreManager) {
    super(blobStoreManager);
  }
}
