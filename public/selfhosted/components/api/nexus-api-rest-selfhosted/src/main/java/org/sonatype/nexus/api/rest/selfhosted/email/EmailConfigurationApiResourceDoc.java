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
package org.sonatype.nexus.api.rest.selfhosted.email;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.Response;

import org.sonatype.nexus.api.rest.selfhosted.email.model.ApiEmailConfiguration;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.sonatype.nexus.api.rest.selfhosted.email.model.ApiEmailValidation;

/**
 * Swagger documentation for {@link EmailConfigurationApiResource}
 *
 * @since 3.19
 */
@Tag(name = "Email")
public interface EmailConfigurationApiResourceDoc
{
  @Operation(summary = "Retrieve the current email configuration")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "403", description = "Insufficient permissions to retrieve the email configuration")
  })
  ApiEmailConfiguration getEmailConfiguration();

  @Operation(summary = "Set the current email configuration")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Email configuration was successfully updated"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions to update the email configuration")
  })
  void setEmailConfiguration(@Parameter(required = true) @NotNull @Valid ApiEmailConfiguration emailConfiguration);

  @Operation(summary = "Send a test email to the email address provided in the request body")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Test email was sent successfully",
          content = @Content(schema = @Schema(implementation = ApiEmailValidation.class))),
      @ApiResponse(responseCode = "400", description = "There was a problem sending the test email" /*
                                                                                                     * NEXUS-46395
                                                                                                     * TODO:
                                                                                                     * examples=
                                                                                                     * dropped; use
                                                                                                     * OpenAPI
                                                                                                     * 3 @Content(
                                                                                                     * examples
                                                                                                     * = @ExampleObject
                                                                                                     * (...))
                                                                                                     */,
          content = @Content(schema = @Schema(implementation = ApiEmailValidation.class))),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions to verify the email configuration")
  })
  Response testEmailConfiguration(
      @Parameter(required = true,
          description = "An email address to send a test email to") @NotNull String validationEmail);

  @Operation(summary = "Disable and clear the email configuration")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Email configuration was successfully cleared")
  })
  void deleteEmailConfiguration();
}
