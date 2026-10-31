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

import com.sonatype.nexus.ssl.plugin.validator.PemPrivateKey;

import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiGlobalDistributionType.S3BlobStoreApiGlobalDistribution;
import org.sonatype.nexus.validation.constraint.UrlString;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiGlobalDistributionType.CLOUDFRONT;
import static org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiGlobalDistributionType.STR_CLOUDFRONT;

@Schema(description = "Configuration for redirecting downloads to CloudFront")
public record S3BlobStoreApiCloudFront(
    @Schema(requiredMode = REQUIRED, allowableValues = STR_CLOUDFRONT,
        defaultValue = STR_CLOUDFRONT) @NotNull String type,

    @Schema(description = "The CloudFront distribution URL",
        defaultValue = "https://something.cloudfront.net/") @NotBlank @NotNull @UrlString String distributionUrl,

    @Schema(description = "The identifier in CloudFront of the provided private-key.") @NotBlank @NotNull String keyId,

    @Schema(description = "The private key used to create pre-signed URLs in a PEM PKCS#8 string.",
        example = "-----BEGIN PRIVATE KEY-----\\nkeycontent\\n-----END PRIVATE KEY-----") @JsonProperty(
            access = JsonProperty.Access.WRITE_ONLY) @PemPrivateKey @NotBlank @NotNull String privateKey,
    @Schema(description = "The private key password", example = "password") @JsonProperty(
        access = JsonProperty.Access.WRITE_ONLY) @Nullable String privateKeyPassword)
    implements S3BlobStoreApiGlobalDistribution
{

  public S3BlobStoreApiCloudFront {
    checkArgument(CLOUDFRONT.name().equals(type));
    requireNonNull(distributionUrl);
    requireNonNull(keyId);
    requireNonNull(privateKey);
  }

  @JsonIgnore
  public S3BlobStoreApiCloudFront(
      final String distributionUrl,
      final String keyId,
      final String privateKey)
  {
    this(CLOUDFRONT.name(), distributionUrl, keyId, privateKey, null);
  }

  @VisibleForTesting
  @JsonIgnore
  public S3BlobStoreApiCloudFront(
      final String distributionUrl,
      final String keyId,
      final String privateKey,
      final String privateKeyPassword)
  {
    this(CLOUDFRONT.name(), distributionUrl, keyId, privateKey, privateKeyPassword);
  }
}
