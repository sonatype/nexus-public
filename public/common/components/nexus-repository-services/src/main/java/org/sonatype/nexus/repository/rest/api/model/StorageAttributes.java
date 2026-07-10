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
import jakarta.validation.constraints.NotEmpty;

/**
 * REST API model describing a repository's storage settings.
 *
 * @since 3.20
 */
public class StorageAttributes
{
  @Schema(description = "Blob store used to store repository contents", example = "default",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotEmpty
  protected String blobStoreName;

  @Schema(description = "Whether to validate uploaded content's MIME type appropriate for the repository format",
      example = "true")
  @NotNull
  protected Boolean strictContentTypeValidation;

  @JsonCreator
  public StorageAttributes(
      @JsonProperty("blobStoreName") final String blobStoreName,
      @JsonProperty("strictContentTypeValidation") final Boolean strictContentTypeValidation)
  {
    this.blobStoreName = blobStoreName;
    this.strictContentTypeValidation = strictContentTypeValidation;
  }

  public String getBlobStoreName() {
    return blobStoreName;
  }

  public Boolean getStrictContentTypeValidation() {
    return strictContentTypeValidation;
  }
}
