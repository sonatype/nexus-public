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
package org.sonatype.nexus.blobstore.s3.rest.internal.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

public class S3BlobStoreApiFailoverBucket
{
  @NotNull
  @ApiModelProperty("The region containing the bucket")
  private final String region;

  @NotNull
  @ApiModelProperty("The name of the bucket in the region")
  private final String bucketName;

  public S3BlobStoreApiFailoverBucket(
      @JsonProperty("region") final String region,
      @JsonProperty("bucketName") final String bucketName)
  {
    this.region = region;
    this.bucketName = bucketName;
  }

  public String getRegion() {
    return region;
  }

  public String getBucketName() {
    return bucketName;
  }
}
