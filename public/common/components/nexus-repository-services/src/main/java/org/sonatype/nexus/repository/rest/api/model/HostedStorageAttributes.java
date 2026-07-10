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
package org.sonatype.nexus.repository.rest.api.model;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.sonatype.nexus.repository.config.WritePolicy;

import java.util.Objects;

/**
 * REST API model for describing storage of hosted repositories.
 *
 * @since 3.20
 */
public class HostedStorageAttributes
    extends StorageAttributes
{
  @Schema(description = "Controls if deployments of and updates to assets are allowed",
      example = "ALLOW_ONCE")
  @NotNull
  protected final String writePolicy;

  @JsonCreator
  public HostedStorageAttributes(
      @JsonProperty("blobStoreName") final String blobStoreName,
      @JsonProperty("strictContentTypeValidation") final Boolean strictContentTypeValidation,
      @JsonProperty("writePolicy") final String writePolicy)
  {
    super(blobStoreName, strictContentTypeValidation);

    Objects.requireNonNull(writePolicy, "writePolicy is required");
    this.writePolicy = WritePolicy.fromString(writePolicy).name();
  }

  public String getWritePolicy() {
    return writePolicy;
  }
}
