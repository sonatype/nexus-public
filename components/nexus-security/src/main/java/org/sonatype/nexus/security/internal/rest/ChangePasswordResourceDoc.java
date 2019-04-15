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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Swagger documentation for {@link ChangePasswordResource}
 *
 * @since 3.next
 */
@Api(value = "Users")
public interface ChangePasswordResourceDoc
{
  String USERNAME_DESCRIPTION = "The username you would like to change the password for.";
  String PASSWORD_DESCRIPTION = "The new password to use.";
  String INVALID_PERMISSIONS = "Not privileged to perform operation.";
  String USER_NOT_FOUND = "User not found in the system";
  String PASSWORD_REQUIRED = "Password was not supplied in the body of the request";

  @ApiOperation("Change a user's password")
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = PASSWORD_REQUIRED),
      @ApiResponse(code = 403, message = INVALID_PERMISSIONS),
      @ApiResponse(code = 404, message = USER_NOT_FOUND)
  })
  void changePassword(@ApiParam(value = USERNAME_DESCRIPTION) final String username,
                          @ApiParam(value = PASSWORD_DESCRIPTION) final String password);
}
