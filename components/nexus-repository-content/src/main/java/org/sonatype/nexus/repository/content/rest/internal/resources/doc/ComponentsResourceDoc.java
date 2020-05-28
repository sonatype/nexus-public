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
package org.sonatype.nexus.repository.content.rest.internal.resources.doc;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

/**
 * Swagger documentation for components API.
 *
 * @since 3.24
 */
@Api(value = "components")
public interface ComponentsResourceDoc
{
  @ApiOperation(value = "Upload a single component")
  @ApiResponses(value = {
      @ApiResponse(code = 403, message = "Insufficient permissions to upload a component"),
      @ApiResponse(code = 422, message = "Parameter 'repository' is required")
  })
  void uploadComponent(
      @ApiParam(value = "Name of the repository to which you would like to upload the component", required = true)
      final String repository,
      @ApiParam(hidden = true) @MultipartForm HttpServletRequest request)
      throws IOException;
}
