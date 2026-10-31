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

import org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiGlobalDistributionType.S3BlobStoreApiGlobalDistribution;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import static com.google.common.base.Preconditions.checkArgument;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
import static org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiGlobalDistributionType.STR_TRANSFER_ACCELERATION;
import static org.sonatype.nexus.api.rest.common.blobstore.s3.model.S3BlobStoreApiGlobalDistributionType.TRANSFER_ACCELERATION;

@Schema(description = "S3 Transfer Acceleration, uses AWS internal routing to improve performance over distances.")
public record S3BlobStoreApiTransferAcceleration(
    @Schema(requiredMode = REQUIRED, allowableValues = STR_TRANSFER_ACCELERATION,
        defaultValue = STR_TRANSFER_ACCELERATION) @NotNull String type)
    implements S3BlobStoreApiGlobalDistribution
{
  public S3BlobStoreApiTransferAcceleration {
    // Calling this with anything but TRANSFER_ACCELERATION indicates an issue with deserialization
    checkArgument(TRANSFER_ACCELERATION.name().equals(type));
  }

  @JsonIgnore
  public S3BlobStoreApiTransferAcceleration() {
    this(TRANSFER_ACCELERATION.name());
  }
}
