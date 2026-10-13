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
package org.sonatype.nexus.api.rest.selfhosted.telemetry;

import org.sonatype.nexus.api.rest.selfhosted.telemetry.model.MandatoryTelemetryAcknowledgementStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Swagger documentation for {@link MandatoryTelemetryAcknowledgementApiResource}.
 */
@Tag(name = "Mandatory Telemetry Acknowledgement")
public interface MandatoryTelemetryAcknowledgementApiResourceDoc
{
  @Operation(summary = "Get the mandatory telemetry acknowledgement record")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successful response",
          content = @Content(
              schema = @Schema(implementation = MandatoryTelemetryAcknowledgementStatus.class))),
      @ApiResponse(responseCode = "403",
          description = "Insufficient permissions to get the mandatory telemetry acknowledgement")
  })
  MandatoryTelemetryAcknowledgementStatus getMandatoryTelemetryAcknowledgement();

  @Operation(
      summary = "Record mandatory telemetry acknowledgement",
      description = "Records that an administrator has acknowledged the mandatory telemetry requirement")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Acknowledgement recorded"),
      @ApiResponse(responseCode = "400", description = "Acknowledgement was not true"),
      @ApiResponse(responseCode = "403",
          description = "Insufficient permissions to record the mandatory telemetry acknowledgement")
  })
  void acknowledgeMandatoryTelemetry(MandatoryTelemetryAcknowledgementStatus status);
}
