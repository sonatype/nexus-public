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
package org.sonatype.nexus.repository.apt.rest;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * REST API model for apt-specific proxy attributes.
 *
 * @since 3.20
 */
public class AptProxyRepositoriesAttributes
{
  @Schema(
      description = "Distribution name. When enforceDistribution is false (default), this field is optional and informational only - "
          +
          "proxy repositories forward all distribution requests to upstream transparently. " +
          "When enforceDistribution is true, this field is required and restricts requests to only the specified distribution.",
      example = "bionic",
      required = false)
  private final String distribution;

  @Schema(
      description = "Whether the upstream repository uses a flat structure (without distribution subdirectories). " +
          "Set to true for flat repositories, false for standard hierarchical repositories.",
      example = "false",
      required = true)
  @NotNull
  private final Boolean flat;

  @Schema(description = "Whether to restrict requests to only the specified distribution", example = "false")
  private final Boolean enforceDistribution;

  @JsonCreator
  public AptProxyRepositoriesAttributes(
      @JsonProperty("distribution") final String distribution,
      @JsonProperty("flat") final Boolean flat,
      @JsonProperty("enforceDistribution") final Boolean enforceDistribution)
  {
    this.distribution = distribution;
    this.flat = flat;
    this.enforceDistribution = enforceDistribution;
  }

  public String getDistribution() {
    return distribution;
  }

  public Boolean getFlat() {
    return flat;
  }

  public Boolean getEnforceDistribution() {
    return enforceDistribution;
  }
}
