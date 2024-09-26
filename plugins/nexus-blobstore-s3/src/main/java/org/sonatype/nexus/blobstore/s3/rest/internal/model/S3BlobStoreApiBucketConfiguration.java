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

import java.util.List;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Encapsulates the objects used to specify the configuration for an s3 blob store.
 *
 * @since 3.20
 */
@JsonInclude(NON_NULL)
public class S3BlobStoreApiBucketConfiguration
{
  public static final String FAILOVER_BUCKETS = "failoverBuckets";

  @Valid
  @NotNull
  @ApiModelProperty(value = "Details of the S3 bucket such as name and region", required = true)
  private final S3BlobStoreApiBucket bucket;

  @ApiModelProperty("Security details for granting access the S3 API")
  private final S3BlobStoreApiBucketSecurity bucketSecurity;

  @ApiModelProperty("The type of encryption to use if any")
  private final S3BlobStoreApiEncryption encryption;

  @ApiModelProperty("A custom endpoint URL, signer type and whether path style access is enabled")
  private final S3BlobStoreApiAdvancedBucketConnection advancedBucketConnection;

  @Valid
  @Nullable
  @ApiModelProperty("A list of secondary buckets which have bidrectional replication enabled and should be used when Nexus is running in the region")
  private final List<S3BlobStoreApiFailoverBucket> failoverBuckets;

  public S3BlobStoreApiBucketConfiguration(
      @JsonProperty("bucket") final S3BlobStoreApiBucket bucket,
      @JsonProperty("security") final S3BlobStoreApiBucketSecurity bucketSecurity,
      @JsonProperty("encryption") final S3BlobStoreApiEncryption encryption,
      @JsonProperty("advancedConnection") final S3BlobStoreApiAdvancedBucketConnection advancedBucketConnection,
      @JsonProperty(FAILOVER_BUCKETS) final List<S3BlobStoreApiFailoverBucket> failoverBuckets)
  {
    this.bucket = bucket;
    this.bucketSecurity = bucketSecurity;
    this.encryption = encryption;
    this.advancedBucketConnection = advancedBucketConnection;
    this.failoverBuckets = failoverBuckets;
  }

  public S3BlobStoreApiBucket getBucket() {
    return bucket;
  }

  public S3BlobStoreApiBucketSecurity getBucketSecurity() {
    return bucketSecurity;
  }

  public S3BlobStoreApiEncryption getEncryption() {
    return encryption;
  }

  public S3BlobStoreApiAdvancedBucketConnection getAdvancedBucketConnection() {
    return advancedBucketConnection;
  }

  public List<S3BlobStoreApiFailoverBucket> getFailoverBuckets() {
    return failoverBuckets;
  }
}
