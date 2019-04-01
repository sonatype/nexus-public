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

import javax.validation.constraints.NotNull;

import org.sonatype.nexus.repository.rest.api.RoutingRuleXO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;


/**
 * Swagger documentation for {@link org.sonatype.nexus.repository.rest.internal.resources.RoutingRulesApiResource}
 *
 * @since 3.next
 */
@Api(value = "routing-rules", hidden = true)
public interface RoutingRulesApiResourceDoc
{
  @ApiOperation("Create a single routing rule")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Routing rule was successfully created"),
      @ApiResponse(code = 400, message = "A routing rule with the same name already exists or required parameters missing"),
      @ApiResponse(code = 403, message = "Insufficient permissions to create routing rule")
  })
  void createRoutingRule(
      @ApiParam(value = "A routing rule configuration", required = true)
      @NotNull RoutingRuleXO routingRuleXO
  );

  @ApiOperation("List routing rules")
  @ApiResponses(value = {
      @ApiResponse(code = 403, message = "Insufficient permissions to read routing rules")
  })
  List<RoutingRuleXO> getRoutingRules();

  @ApiOperation("Get a single routing rule")
  @ApiResponses(value = {
      @ApiResponse(code = 403, message = "Insufficient permissions to read routing rules"),
      @ApiResponse(code = 404, message = "Routing rule not found")
  })
  RoutingRuleXO getRoutingRule(
      @ApiParam(value = "The name of the routing rule to get", required = true) final String name
  );

  @ApiOperation("Update a single routing rule")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Routing rule was successfully updated"),
      @ApiResponse(code = 400, message = "Another routing rule with the same name already exists or required parameters missing"),
      @ApiResponse(code = 403, message = "Insufficient permissions to edit routing rules"),
      @ApiResponse(code = 404, message = "Routing rule not found")
  })
  void updateRoutingRule(
      @ApiParam(value = "The name of the routing rule to update", required = true) final String name,
      @ApiParam(value = "A routing rule configuration", required = true)
      @NotNull final RoutingRuleXO routingRuleXO
  );

  @ApiOperation("Delete a single routing rule")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Routing rule was successfully deleted"),
      @ApiResponse(code = 403, message = "Insufficient permissions to delete routing rules"),
      @ApiResponse(code = 404, message = "Routing rule not found")
  })
  void deleteRoutingRule(@ApiParam(value = "The name of the routing rule to delete", required = true) final String name);
}
