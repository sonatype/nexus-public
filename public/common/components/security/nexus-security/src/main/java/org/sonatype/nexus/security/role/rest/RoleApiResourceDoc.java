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
package org.sonatype.nexus.security.role.rest;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.sonatype.nexus.security.authz.NoSuchAuthorizationManagerException;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotEmpty;

/**
 * Swagger documentation for {@link RoleApiResource}
 *
 * @since 3.19
 */
@Tag(name = "Security management: roles")
public interface RoleApiResourceDoc
{
  @Operation(summary = "List roles")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "400", description = "The specified source does not exist"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions to read roles")})
  List<RoleXOResponse> getRoles(
      @Parameter(
          description = "The id of the user source to filter the roles by, if supplied. Otherwise roles from all user sources will be returned.") final String source);

  @Operation(summary = "Create role")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "403", description = "Insufficient permissions to create role")})
  RoleXOResponse create(
      @Parameter(description = "A role configuration", required = true) @NotNull @Valid final RoleXORequest roleXO

  ) throws NoSuchAuthorizationManagerException;

  @Operation(summary = "Get role")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "400", description = "The specified source does not exist"),
      @ApiResponse(responseCode = "403", description = "Insufficient permissions to read roles"),
      @ApiResponse(responseCode = "404", description = "Role not found")})
  RoleXOResponse getRole(
      @Parameter(
          description = "The id of the user source to filter the roles by. Available sources can be fetched using the 'User Sources' endpoint." /*
                                                                                                                                                 * NEXUS
                                                                                                                                                 * -
                                                                                                                                                 * 46395
                                                                                                                                                 * TODO:
                                                                                                                                                 * defaultValue
                                                                                                                                                 * moved
                                                                                                                                                 * to @Schema
                                                                                                                                                 * in
                                                                                                                                                 * OpenAPI
                                                                                                                                                 * 3
                                                                                                                                                 * .
                                                                                                                                                 * x: @Parameter
                                                                                                                                                 * (
                                                                                                                                                 * schema
                                                                                                                                                 * = @Schema
                                                                                                                                                 * (
                                                                                                                                                 * defaultValue
                                                                                                                                                 * =
                                                                                                                                                 * DEFAULT_SOURCE
                                                                                                                                                 * )
                                                                                                                                                 * )
                                                                                                                                                 */) final String source,
      @Parameter(description = "The id of the role to get", required = true) @NotEmpty final String id);

  @Operation(summary = "Update role")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "403", description = "Insufficient permissions to update role"),
      @ApiResponse(responseCode = "404", description = "Role not found")})
  void update(
      @Parameter(description = "The id of the role to update", required = true) @NotEmpty final String id,
      @Parameter(description = "A role configuration",
          required = true) @NotNull @Valid final RoleXORequest roleXO) throws NoSuchAuthorizationManagerException;

  @Operation(summary = "Delete role")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "403", description = "Insufficient permissions to delete role"),
      @ApiResponse(responseCode = "404", description = "Role not found"),
      @ApiResponse(responseCode = "204", description = "Success")})
  void delete(
      @Parameter(description = "The id of the role to delete",
          required = true) @NotEmpty final String id) throws NoSuchAuthorizationManagerException;
}
