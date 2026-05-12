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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MediaType;

import org.sonatype.nexus.api.rest.selfhosted.security.ssrf.model.SsrfProtectionConfigurationXO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;

/**
 * Swagger documentation for the SSRF Protection API.
 */
@Api(value = "Security Management: SSRF Protection")
public interface SsrfProtectionApiResourceDoc
{
  String EXAMPLE_BODY =
      "{\"enabled\": true, \"allowedIPs\": [\"10.0.0.50\", \"192.168.1.100\"], \"allowedDomains\": [\"internal.corp.com\", \"registry.local\"]}";

  @ApiOperation("Get SSRF protection settings")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "SSRF protection settings returned", examples = @Example(value = {
          @ExampleProperty(mediaType = MediaType.APPLICATION_JSON, value = EXAMPLE_BODY)
      })),
      @ApiResponse(code = 401, message = "Authentication required"),
      @ApiResponse(code = 403, message = "Insufficient permissions")
  })
  SsrfProtectionConfigurationXO getConfiguration();

  @ApiOperation("Update SSRF protection settings")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "SSRF protection settings updated"),
      @ApiResponse(code = 400, message = "Invalid configuration"),
      @ApiResponse(code = 401, message = "Authentication required"),
      @ApiResponse(code = 403, message = "Insufficient permissions")
  })
  SsrfProtectionConfigurationXO updateConfiguration(@NotNull @Valid SsrfProtectionConfigurationXO configuration);
}
