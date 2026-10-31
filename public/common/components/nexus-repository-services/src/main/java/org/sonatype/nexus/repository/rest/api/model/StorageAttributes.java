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

import org.sonatype.nexus.repository.rest.api.validation.NotEmptyOnDistribution;
import org.sonatype.nexus.swagger.DistributionVisibility;
import org.sonatype.nexus.swagger.EditionVisibility;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import static org.sonatype.nexus.bootstrap.entrypoint.distribution.NexusDistributionType.SELF_HOSTED;

/**
 * REST API model describing a repository's storage settings.
 *
 * @since 3.20
 */
@JsonFilter(EditionVisibility.NAME)
public class StorageAttributes
{
  @Schema(description = "Blob store used to store repository contents", example = "default",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @DistributionVisibility(cloud = false,
      note = "Cloud generates a blob store per repository and overwrites this value, so the name is an internal detail")
  @NotEmptyOnDistribution(SELF_HOSTED)
  protected String blobStoreName;

  @Schema(description = "Whether to validate uploaded content's MIME type appropriate for the repository format",
      example = "true")
  @NotNull
  protected Boolean strictContentTypeValidation;

  @EditionVisibility(community = false, pro = false, proPlus = false, note = "Enterprise feature")
  @Schema(
      description = "When true the repository can redirect downloads to global distribution if configured. (Enterprise only)",
      example = "true", defaultValue = "false")
  @Nullable
  protected Boolean globalDistribution = Boolean.FALSE;

  @JsonCreator
  public StorageAttributes(
      @JsonProperty("blobStoreName") final String blobStoreName,
      @JsonProperty("strictContentTypeValidation") final Boolean strictContentTypeValidation,
      @JsonProperty("globalDistribution") @Nullable final Boolean globalDistribution)
  {
    this.blobStoreName = blobStoreName;
    this.strictContentTypeValidation = strictContentTypeValidation;
    this.globalDistribution = globalDistribution;
  }

  public String getBlobStoreName() {
    return blobStoreName;
  }

  public Boolean getStrictContentTypeValidation() {
    return strictContentTypeValidation;
  }

  public Boolean getGlobalDistribution() {
    return globalDistribution;
  }
}
