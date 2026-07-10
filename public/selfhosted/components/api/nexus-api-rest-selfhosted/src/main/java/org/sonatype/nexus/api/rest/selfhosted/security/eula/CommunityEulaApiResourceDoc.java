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
package org.sonatype.nexus.api.rest.selfhosted.security.eula;

import org.sonatype.nexus.api.rest.selfhosted.security.eula.model.EulaStatus;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Swagger documentation for {@link CommunityEulaApiResource}.
 */
@Tag(name = "Community Edition Eula")
public interface CommunityEulaApiResourceDoc
{
  @Operation(summary = "Get the current Community Eula status.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successful response")
  // NEXUS-46395 TODO: examples= attribute dropped during sweep; restore via
  // @Content(examples = @ExampleObject(value = "{ ... }"))
  })
  EulaStatus getCommunityEulaStatus();

  @Operation(summary = "Set the Community Eula status.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "EULA status set successfully"),
      @ApiResponse(responseCode = "500", description = "Incorrect EULA Status")
  })
  void setEulaAcceptedCE(
      // NEXUS-46395 TODO: examples on @Parameter moves to a nested @Schema or to @Content;
      // for the spike we accept the loss of the inline example. Restore via
      // @Parameter(content = @Content(examples = @ExampleObject(value = "{...}"))) on the
      // body parameter, or via @RequestBody at the operation level.
      EulaStatus eulaStatus);
}
