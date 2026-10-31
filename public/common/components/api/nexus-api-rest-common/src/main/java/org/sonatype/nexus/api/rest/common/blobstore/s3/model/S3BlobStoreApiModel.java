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

import org.sonatype.nexus.api.rest.common.blobstore.model.BlobStoreApiSoftQuota;
import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiGlobalDistributionType.S3BlobStoreApiGlobalDistribution;
import org.sonatype.nexus.swagger.EditionVisibility;
import org.sonatype.nexus.validation.constraint.NamePatternConstants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import static org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiGlobalDistributionType.STR_CLOUDFRONT;
import static org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiGlobalDistributionType.STR_TRANSFER_ACCELERATION;
import static org.sonatype.nexus.blobstore.s3.internal.S3BlobStore.TYPE;

/**
 * Encapsulates the API payload for creating, reading and updating an S3 blob store.
 *
 * @since 3.20
 */
@JsonFilter(EditionVisibility.NAME)
public class S3BlobStoreApiModel
{
  @NotNull
  @Pattern(regexp = NamePatternConstants.REGEX, message = NamePatternConstants.MESSAGE)
  @Schema(description = "The name of the S3 blob store.", example = "s3", requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Schema(description = "Settings to control the soft quota.")
  private final BlobStoreApiSoftQuota softQuota;

  @Valid
  @NotNull
  @Schema(description = "The S3 specific configuration details for the S3 object that'll contain the blob store.")
  private final S3BlobStoreApiBucketConfiguration bucketConfiguration;

  @EditionVisibility(community = false, pro = false, proPlus = false, note = "Enterprise feature")
  @Valid
  @Schema(description = "Global distribution content distribution configuration. (Enterprise only)",
      oneOf = {S3BlobStoreApiCloudFront.class, S3BlobStoreApiTransferAcceleration.class},
      discriminatorProperty = "type",
      discriminatorMapping = {@DiscriminatorMapping(value = STR_CLOUDFRONT, schema = S3BlobStoreApiCloudFront.class),
          @DiscriminatorMapping(value = STR_TRANSFER_ACCELERATION, schema = S3BlobStoreApiTransferAcceleration.class)})
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type",
      visible = true)
  @JsonSubTypes(value = {@JsonSubTypes.Type(value = S3BlobStoreApiCloudFront.class, name = STR_CLOUDFRONT),
      @JsonSubTypes.Type(value = S3BlobStoreApiTransferAcceleration.class, name = STR_TRANSFER_ACCELERATION),})
  private S3BlobStoreApiGlobalDistribution globalDistribution;

  @JsonCreator
  public S3BlobStoreApiModel(
      @JsonProperty("name") final String name,
      @JsonProperty("softQuota") final BlobStoreApiSoftQuota softQuota,
      @JsonProperty("bucketConfiguration") final S3BlobStoreApiBucketConfiguration bucketConfiguration,
      @JsonProperty("globalDistribution") @Nullable final S3BlobStoreApiGlobalDistribution globalDistribution)
  {
    this.name = name;
    this.softQuota = softQuota;
    this.bucketConfiguration = bucketConfiguration;
    this.globalDistribution = globalDistribution;
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

  @Nullable
  public S3BlobStoreApiGlobalDistribution getGlobalDistribution() {
    return globalDistribution;
  }

  public void setGlobalDistribution(@Nullable final S3BlobStoreApiGlobalDistribution globalDistribution) {
    this.globalDistribution = globalDistribution;
  }

  @Schema(description = "The blob store type.", example = TYPE)
  public String getType() {
    return TYPE;
  }
}
