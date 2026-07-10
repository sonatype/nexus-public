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
package org.sonatype.nexus.security.internal.rest;

import java.util.List;

import javax.annotation.Nullable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response for {@code GET /internal/ui/api/permissions}.
 */
public class ApiPermissionsResponse
{
  @Schema(description = "Mapped endpoints", requiredMode = Schema.RequiredMode.REQUIRED)
  private List<ApiEndpointPermission> endpoints;

  @Schema(description = "ISO-8601 timestamp when the registry was built", requiredMode = Schema.RequiredMode.REQUIRED)
  private String generatedAt;

  @Schema(description = "Total endpoints returned before client-side filtering",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private int totalEndpoints;

  @Schema(description = "Count of Swagger operations that had no permission mapping at registry build time")
  private int unmappedEndpoints;

  @Nullable
  @Schema(description = "Present when the registry could not serve data")
  private String error;

  public ApiPermissionsResponse() {
    // Jackson
  }

  public ApiPermissionsResponse(
      final List<ApiEndpointPermission> endpoints,
      final String generatedAt,
      final int totalEndpoints,
      final int unmappedEndpoints,
      @Nullable final String error)
  {
    this.endpoints = endpoints;
    this.generatedAt = generatedAt;
    this.totalEndpoints = totalEndpoints;
    this.unmappedEndpoints = unmappedEndpoints;
    this.error = error;
  }

  public List<ApiEndpointPermission> getEndpoints() {
    return endpoints;
  }

  public void setEndpoints(final List<ApiEndpointPermission> endpoints) {
    this.endpoints = endpoints;
  }

  public String getGeneratedAt() {
    return generatedAt;
  }

  public void setGeneratedAt(final String generatedAt) {
    this.generatedAt = generatedAt;
  }

  public int getTotalEndpoints() {
    return totalEndpoints;
  }

  public void setTotalEndpoints(final int totalEndpoints) {
    this.totalEndpoints = totalEndpoints;
  }

  public int getUnmappedEndpoints() {
    return unmappedEndpoints;
  }

  public void setUnmappedEndpoints(final int unmappedEndpoints) {
    this.unmappedEndpoints = unmappedEndpoints;
  }

  @Nullable
  public String getError() {
    return error;
  }

  public void setError(@Nullable final String error) {
    this.error = error;
  }
}
