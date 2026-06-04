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
package org.sonatype.nexus.api.rest.selfhosted.blobstore.s3;

import org.springframework.beans.factory.annotation.Autowired;
import javax.ws.rs.Path;

import org.sonatype.nexus.api.rest.common.blobstore.s3.S3BlobStoreApiUpdateValidation;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.crypto.secrets.SecretsFactory;

import io.swagger.annotations.Api;

import static org.sonatype.nexus.api.rest.selfhosted.blobstore.s3.S3BlobStoreApiResourceBeta.RESOURCE_URI;
import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;
import org.springframework.stereotype.Component;

/**
 * beta endpoint for S3 BlobStore REST API
 *
 * @since 3.24
 * @deprecated moving to {@link S3BlobStoreApiResourceV1}
 */
@Api(hidden = true)
@Component
@Path(RESOURCE_URI)
@Deprecated
public class S3BlobStoreApiResourceBeta
    extends S3BlobStoreApiResource
{
  static final String RESOURCE_URI = BETA_API_PREFIX + "/blobstores/s3";

  @Autowired
  public S3BlobStoreApiResourceBeta(
      final BlobStoreManager blobStoreManager,
      final S3BlobStoreApiUpdateValidation validation,
      final SecretsFactory secretsFactory,
      final ApplicationVersion applicationVersion)
  {
    super(blobStoreManager, validation, secretsFactory, applicationVersion);
  }
}
