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
package org.sonatype.nexus.internal.security.rest;

import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;

/**
 * Swagger documentation for {@link CommunityEulaApiResource}.
 */
@Api(value = "Community Edition Eula")
public interface CommunityEulaApiResourceDoc
{
  @ApiOperation(value = "Get the current Community Eula status.")
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Successful response", examples = @Example(value = {
          @ExampleProperty(mediaType = MediaType.APPLICATION_JSON, value = "{\"accepted\": false}")
      }))
  })
  EulaStatus getCommunityEulaStatus();

  @ApiOperation("Set the Community Eula status.")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "EULA status set successfully")
  })
  void setEulaAcceptedCE(
      @ApiParam(examples = @Example(value = {
          @ExampleProperty(mediaType = MediaType.APPLICATION_JSON, value = "{\"accepted\": true}")
      })) EulaStatus eulaStatus);
}
