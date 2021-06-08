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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Encapsulates S3 endpoint url, signer type and whether path-style access should be enabled for the specified S3
 * endpoint url.
 *
 * @since 3.20
 */
@JsonInclude(NON_NULL)
public class S3BlobStoreApiAdvancedBucketConnection
{
  @ApiModelProperty("A custom endpoint URL for third party object stores using the S3 API.")
  private final String endpoint;

  @ApiModelProperty("An API signature version which may be required for third party object stores using the S3 API.")
  private final String signerType;

  @ApiModelProperty("Setting this flag will result in path-style access being used for all requests.")
  private final Boolean forcePathStyle;

  @ApiModelProperty("Setting this value will override the default connection pool size of Nexus of the s3 client for this blobstore.")
  private final Integer maxConnectionPoolSize;

  @JsonCreator
  public S3BlobStoreApiAdvancedBucketConnection(
      @JsonProperty("endpoint") final String endpoint,
      @JsonProperty("signerType") final String signerType,
      @JsonProperty("forcePathStyle") final Boolean forcePathStyle,
      @JsonProperty("maxConnectionPoolSize") final Integer maxConnectionPoolSize)
  {
    this.endpoint = endpoint;
    this.signerType = signerType;
    this.forcePathStyle = forcePathStyle;
    this.maxConnectionPoolSize = maxConnectionPoolSize;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public String getSignerType() {
    return signerType;
  }

  public Boolean getForcePathStyle() {
    return forcePathStyle;
  }

  public Integer getMaxConnectionPoolSize() {
    return maxConnectionPoolSize;
  }
}
