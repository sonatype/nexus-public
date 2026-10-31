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
package org.sonatype.nexus.security.internal.rest;

import java.util.Collection;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

  String REALM_DESCRIPTION = "The realm the request should apply to.";

  String PASSWORD_REQUIRED = "Password was not supplied in the body of the request";

  @Operation(summary = "Retrieve a list of users. For SAML user sources a limit of 1000 users will be applied.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Users returned",
          content = @Content(array = @ArraySchema(schema = @Schema(implementation = ApiUser.class)))),
      @ApiResponse(responseCode = "400", description = PASSWORD_REQUIRED),
      @ApiResponse(responseCode = "403", description = NexusSecurityApiConstants.INVALID_PERMISSIONS)})
  Collection<ApiUser> getUsers(
      @Parameter(description = "An optional term to search userids for.") String userId,
      @Parameter(description = "An optional user source to restrict the search to.") String source);

  @Operation(summary = "Delete a user.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "400", description = NexusSecurityApiConstants.NON_LOCAL_USER_CANNOT_BE_DELETED),
      @ApiResponse(responseCode = "403", description = NexusSecurityApiConstants.INVALID_PERMISSIONS),
      @ApiResponse(responseCode = "404", description = NexusSecurityApiConstants.USER_OR_SOURCE_NOT_FOUND)})
  void deleteUser(
      @Parameter(description = USER_ID_DESCRIPTION) String userId,
      @Parameter(description = REALM_DESCRIPTION) String realm);
}
