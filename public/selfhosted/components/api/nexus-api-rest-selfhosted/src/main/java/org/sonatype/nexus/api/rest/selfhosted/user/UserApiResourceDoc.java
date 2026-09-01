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
package org.sonatype.nexus.api.rest.selfhosted.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.sonatype.nexus.api.rest.selfhosted.user.model.ApiCreateUser;
import org.sonatype.nexus.security.internal.rest.ApiUser;
import org.sonatype.nexus.security.internal.rest.NexusSecurityApiConstants;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Swagger documentation for {@link UserApiResource}
 *
 * @since 3.17
 */
@Tag(name = "Security management: users")
public interface UserApiResourceDoc
{
  String USER_ID_DESCRIPTION = "The userid the request should apply to.";

  String PASSWORD_DESCRIPTION = "The new password to use.";

  String PASSWORD_REQUIRED = "Password was not supplied in the body of the request";

  @Operation(summary = "Create a new user in the default source.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "User created",
          content = @Content(schema = @Schema(implementation = ApiUser.class))),
      @ApiResponse(responseCode = "400", description = PASSWORD_REQUIRED),
      @ApiResponse(responseCode = "403", description = NexusSecurityApiConstants.INVALID_PERMISSIONS)})
  ApiUser createUser(
      @Parameter(description = "A representation of the user to create.") @NotNull @Valid ApiCreateUser user);

  @Operation(summary = "Update an existing user.")
  @ApiResponses(value = {@ApiResponse(responseCode = "400", description = PASSWORD_REQUIRED),
      @ApiResponse(responseCode = "403", description = NexusSecurityApiConstants.INVALID_PERMISSIONS),
      @ApiResponse(responseCode = "404", description = NexusSecurityApiConstants.USER_OR_SOURCE_NOT_FOUND),
      @ApiResponse(responseCode = "204", description = NexusSecurityApiConstants.SUCCESS)})
  void updateUser(
      @Parameter(description = USER_ID_DESCRIPTION) String userId,
      @Parameter(description = "A representation of the user to update.") @NotNull @Valid ApiUser user);

  @Operation(summary = "Change a user's password.")
  @ApiResponses(value = {@ApiResponse(responseCode = "400", description = PASSWORD_REQUIRED),
      @ApiResponse(responseCode = "403", description = NexusSecurityApiConstants.INVALID_PERMISSIONS),
      @ApiResponse(responseCode = "404", description = NexusSecurityApiConstants.USER_NOT_FOUND),
      @ApiResponse(responseCode = "204", description = NexusSecurityApiConstants.SUCCESS)})
  void changePassword(
      @Parameter(description = USER_ID_DESCRIPTION) String userId,
      @Parameter(description = PASSWORD_DESCRIPTION) @NotNull(message = "Password must be supplied.") String password);
}
