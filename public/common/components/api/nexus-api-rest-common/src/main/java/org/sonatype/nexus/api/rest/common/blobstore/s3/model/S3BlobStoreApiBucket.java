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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.sonatype.nexus.blobstore.s3.internal.AmazonS3Factory.DEFAULT;

/**
 * Encapsulates the S3 bucket details for an s3 blob store.
 *
 * @since 3.20
 */
@JsonInclude(NON_NULL)
public class S3BlobStoreApiBucket
{
  @Valid
  @NotNull
  @Schema(description = "The AWS region to create a new S3 bucket in or an existing S3 bucket's region",
      example = DEFAULT, requiredMode = Schema.RequiredMode.REQUIRED)
  private final String region;

  @Valid
  @NotNull
  @Schema(description = "The name of the S3 bucket", requiredMode = Schema.RequiredMode.REQUIRED)
  private final String name;

  @Schema(description = "The S3 blob store (i.e S3 object) key prefix")
  private final String prefix;

  @JsonCreator
  public S3BlobStoreApiBucket(
      @JsonProperty("region") final String region,
      @JsonProperty("name") final String name,
      @JsonProperty("prefix") final String prefix)
  {
    this.region = region;
    this.name = name;
    this.prefix = prefix;
  }

  public String getRegion() {
    return region;
  }

  public String getName() {
    return name;
  }

  public String getPrefix() {
    return prefix;
  }
}
