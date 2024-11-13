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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.sonatype.nexus.blobstore.rest.BlobStoreApiSoftQuota;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.TYPE;

/**
 * Encapsulates the API payload for creating, reading and updating an S3 blob store.
 *
 * @since 3.20
 */
public class S3BlobStoreApiModel
{
  @NotNull
  @ApiModelProperty(value = "The name of the S3 blob store.", example = "s3", required = true)
  private String name;

  @ApiModelProperty("Settings to control the soft quota.")
  private final BlobStoreApiSoftQuota softQuota;

  @Valid
  @NotNull
  @ApiModelProperty("The S3 specific configuration details for the S3 object that'll contain the blob store.")
  private final S3BlobStoreApiBucketConfiguration bucketConfiguration;

  @JsonCreator
  public S3BlobStoreApiModel(
      @JsonProperty("name") final String name,
      @JsonProperty("softQuota") final BlobStoreApiSoftQuota softQuota,
      @JsonProperty("bucketConfiguration")
      final S3BlobStoreApiBucketConfiguration bucketConfiguration)
  {
    this.name = name;
    this.softQuota = softQuota;
    this.bucketConfiguration = bucketConfiguration;
  }

  public String getName() {
    return name;
  }

  public BlobStoreApiSoftQuota getSoftQuota() {
    return softQuota;
  }

  public S3BlobStoreApiBucketConfiguration getBucketConfiguration() {
    return bucketConfiguration;
  }

  @ApiModelProperty(value = "The blob store type.", readOnly = true, example = TYPE)
  public String getType() {return TYPE;}
}
