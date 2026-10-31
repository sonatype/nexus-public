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
package org.sonatype.nexus.repository.rest.internal.resources.doc;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.sonatype.nexus.repository.rest.api.RoutingRuleXO;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Swagger documentation for {@link org.sonatype.nexus.repository.rest.internal.resources.RoutingRulesApiResource}
 *
 * @since 3.16
 */
@Tag(name = "Routing rules")
public interface RoutingRulesApiResourceDoc
{
  @Operation(summary = "Create a single routing rule")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Routing rule was successfully created"),
      @ApiResponse(responseCode = "400",
          description = "A routing rule with the same name already exists or required parameters missing"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions to create routing rule")
  })
  void createRoutingRule(
      @Parameter(description = "A routing rule configuration", required = true) @NotNull RoutingRuleXO routingRuleXO);

  @Operation(summary = "List routing rules")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Routing rules returned",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = RoutingRuleXO.class)))),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions to read routing rules")
  })
  List<RoutingRuleXO> getRoutingRules();

  @Operation(summary = "Get a single routing rule")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Routing rule returned",
          content = @Content(schema = @Schema(implementation = RoutingRuleXO.class))),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions to read routing rules"),
      @ApiResponse(responseCode = "404", description = "Routing rule not found")
  })
  RoutingRuleXO getRoutingRule(
      @Parameter(description = "The name of the routing rule to get", required = true) final String name);

  @Operation(summary = "Update a single routing rule")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Routing rule was successfully updated"),
      @ApiResponse(responseCode = "400",
          description = "Another routing rule with the same name already exists or required parameters missing"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions to edit routing rules"),
      @ApiResponse(responseCode = "404", description = "Routing rule not found")
  })
  void updateRoutingRule(
      @Parameter(description = "The name of the routing rule to update", required = true) final String name,
      @Parameter(description = "A routing rule configuration",
          required = true) @NotNull final RoutingRuleXO routingRuleXO);

  @Operation(summary = "Delete a single routing rule")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Routing rule was successfully deleted"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions to delete routing rules"),
      @ApiResponse(responseCode = "404", description = "Routing rule not found")
  })
  void deleteRoutingRule(
      @Parameter(description = "The name of the routing rule to delete", required = true) final String name);
}
