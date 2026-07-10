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
package org.sonatype.nexus.api.rest.selfhosted.status;

import jakarta.ws.rs.GET;

import com.codahale.metrics.health.HealthCheck.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.SortedMap;

@Tag(name = "Status")
public interface StatusCheckResourceDoc
{
  @GET
  @Operation(summary = "Health check endpoint that returns the results of the system status checks")
  // Response schema (Map<String, Result>) is inferred from the return type — the original
  // Swagger 1.x 'response = Result.class, responseContainer = "Map"' has no clean OAS 3
  // annotation form, so we let swagger-jaxrs2 derive 'additionalProperties: $ref: Result'
  // from the SortedMap<String, Result> signature below.
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "The system status check results")
  })
  SortedMap<String, Result> getSystemStatusChecks();
}
