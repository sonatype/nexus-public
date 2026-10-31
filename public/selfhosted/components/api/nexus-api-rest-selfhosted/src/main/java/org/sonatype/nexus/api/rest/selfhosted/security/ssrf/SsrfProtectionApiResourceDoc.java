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
package org.sonatype.nexus.api.rest.selfhosted.security.ssrf;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.sonatype.nexus.api.rest.selfhosted.security.ssrf.model.SsrfProtectionConfigurationXO;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Swagger documentation for the SSRF Protection API.
 */
@Tag(name = "Security Management: SSRF Protection")
public interface SsrfProtectionApiResourceDoc
{
  String EXAMPLE_BODY =
      "{\"enabled\": true, \"allowedIPs\": [\"10.0.0.50\", \"192.168.1.100\"], \"allowedDomains\": [\"internal.corp.com\", \"registry.local\"]}";

  @Operation(summary = "Get SSRF protection settings")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "SSRF protection settings returned",
          content = @Content(schema = @Schema(implementation = SsrfProtectionConfigurationXO.class))),
      @ApiResponse(responseCode = "401", description = "Authentication required"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions")
  })
  SsrfProtectionConfigurationXO getConfiguration();

  @Operation(summary = "Update SSRF protection settings")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "SSRF protection settings updated",
          content = @Content(schema = @Schema(implementation = SsrfProtectionConfigurationXO.class))),
      @ApiResponse(responseCode = "400", description = "Invalid configuration"),
      @ApiResponse(responseCode = "401", description = "Authentication required"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions")
  })
  SsrfProtectionConfigurationXO updateConfiguration(@NotNull @Valid SsrfProtectionConfigurationXO configuration);
}
