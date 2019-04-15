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
package org.sonatype.nexus.internal.status;

import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST API for status operations
 *
 * @since 3.15
 */
@Api("status")
public interface StatusResourceDoc
{
  /**
   * @return 200 if the server is available to serve read requests, 503 otherwise
   */
  @GET
  @ApiOperation("Health check endpoint that validates server can respond to read requests")
  @ApiResponses({
      @ApiResponse(code = 200, message = "Available to service requests"),
      @ApiResponse(code = 503, message = "Unavailable to service requests")
  })
  Response isAvailable();

  /**
   * @return 200 if the server is available to serve read and write requests, 503 otherwise
   *
   * @since 3.16
   */
  @GET
  @ApiOperation("Health check endpoint that validates server can respond to read and write requests")
  @ApiResponses({
      @ApiResponse(code = 200, message = "Available to service requests"),
      @ApiResponse(code = 503, message = "Unavailable to service requests")
  })
  Response isWritable();

}
