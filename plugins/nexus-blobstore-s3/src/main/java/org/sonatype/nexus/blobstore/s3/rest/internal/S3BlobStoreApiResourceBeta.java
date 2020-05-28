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
package org.sonatype.nexus.blobstore.s3.rest.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Path;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;

import io.swagger.annotations.Api;

import static org.sonatype.nexus.blobstore.s3.rest.internal.S3BlobStoreApiResourceBeta.RESOURCE_URI;
import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;

/**
 * beta endpoint for S3 BlobStore REST API
 *
 * @since 3.24
 * @deprecated moving to {@link S3BlobStoreApiResourceV1}
 */
@Api(hidden = true)
@Named
@Singleton
@Path(RESOURCE_URI)
@Deprecated
public class S3BlobStoreApiResourceBeta
  extends S3BlobStoreApiResource
{
  static final String RESOURCE_URI = BETA_API_PREFIX + "/blobstores/s3";

  @Inject
  public S3BlobStoreApiResourceBeta(final BlobStoreManager blobStoreManager,
                                    final S3BlobStoreApiUpdateValidation validation)
  {
    super(blobStoreManager, validation);
  }
}
