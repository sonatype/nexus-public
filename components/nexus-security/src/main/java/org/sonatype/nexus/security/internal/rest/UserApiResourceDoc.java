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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Swagger documentation for {@link UserApiResource}
 *
 * @since 3.next
 */
@Api(value = "Security Management: Users")
public interface UserApiResourceDoc
{
  String USER_ID_DESCRIPTION = "The userid the request should apply to.";

  String PASSWORD_DESCRIPTION = "The new password to use.";

  String PASSWORD_REQUIRED = "Password was not supplied in the body of the request";

  @ApiOperation(
      value = "Retrieve a list of users. Note if the source is not 'default' the response is limited to 100 users.",
      hidden = true)
  @ApiResponses(value = {@ApiResponse(code = 400, message = PASSWORD_REQUIRED),
      @ApiResponse(code = 403, message = NexusSecurityApiConstants.INVALID_PERMISSIONS)})
  Collection<ApiUser> getUsers(
      @ApiParam("An optional term to search userids for.") String userId,
      @ApiParam("An optional user source to restrict the search to.") String source);

  @ApiOperation(value = "Create a new user in the default source.", hidden = true)
  @ApiResponses(value = {@ApiResponse(code = 400, message = PASSWORD_REQUIRED),
      @ApiResponse(code = 403, message = NexusSecurityApiConstants.INVALID_PERMISSIONS)})
  ApiUser createUser(@ApiParam("A representation of the user to create.") @NotNull @Valid ApiCreateUser user);

  @ApiOperation(value = "Update an existing user.", hidden = true)
  @ApiResponses(value = {@ApiResponse(code = 400, message = PASSWORD_REQUIRED),
      @ApiResponse(code = 403, message = NexusSecurityApiConstants.INVALID_PERMISSIONS),
      @ApiResponse(code = 404, message = NexusSecurityApiConstants.USER_OR_SOURCE_NOT_FOUND)})
  void updateUser(
      @ApiParam(value = USER_ID_DESCRIPTION) String userId,
      @ApiParam("A representation of the user to update.") @NotNull @Valid ApiUser user);

  @ApiOperation(value = "Delete a user.", hidden = true)
  @ApiResponses(value = {@ApiResponse(code = 400, message = PASSWORD_REQUIRED),
      @ApiResponse(code = 403, message = NexusSecurityApiConstants.INVALID_PERMISSIONS),
      @ApiResponse(code = 404, message = NexusSecurityApiConstants.USER_OR_SOURCE_NOT_FOUND)})
  void deleteUser(@ApiParam(value = USER_ID_DESCRIPTION) String userId);

  @ApiOperation("Change a user's password.")
  @ApiResponses(value = {@ApiResponse(code = 400, message = PASSWORD_REQUIRED),
      @ApiResponse(code = 403, message = NexusSecurityApiConstants.INVALID_PERMISSIONS),
      @ApiResponse(code = 404, message = NexusSecurityApiConstants.USER_NOT_FOUND)})
  void changePassword(
      @ApiParam(value = USER_ID_DESCRIPTION) String userId,
      @ApiParam(value = PASSWORD_DESCRIPTION) @NotNull String password);
}
