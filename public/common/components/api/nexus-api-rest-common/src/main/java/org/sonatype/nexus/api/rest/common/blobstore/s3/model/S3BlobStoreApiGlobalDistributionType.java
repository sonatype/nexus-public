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
package org.sonatype.nexus.api.rest.common.blobstore.s3.model;

import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public enum S3BlobStoreApiGlobalDistributionType
{
  TRANSFER_ACCELERATION,
  CLOUDFRONT;

  public interface S3BlobStoreApiGlobalDistribution
  {
    @Schema(requiredMode = REQUIRED, allowableValues = {STR_CLOUDFRONT, STR_TRANSFER_ACCELERATION})
    public String type();
  }

  public static final String STR_TRANSFER_ACCELERATION = "TRANSFER_ACCELERATION";

  public static final String STR_CLOUDFRONT = "CLOUDFRONT";
}
