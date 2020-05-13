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
package org.sonatype.nexus.internal.rest;

import org.sonatype.nexus.common.app.ReadOnlyState;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * REST facade for {@link org.sonatype.nexus.common.app.FreezeService}.
 *
 * @since 3.6
 */
@Api(value = "Read-only")
public interface FreezeResourceDoc
{
  @ApiOperation("Get read-only state")
  ReadOnlyState get();

  @ApiOperation("Enable read-only")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "System is now read-only"),
      @ApiResponse(code = 403, message = "Authentication required"),
      @ApiResponse(code = 404, message = "No change to read-only state")
  })
  void freeze();

  @ApiOperation(value = "Release read-only",
    notes = "Release administrator initiated read-only status. Will not release read-only caused by system tasks."
  )
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "System is no longer read-only"),
      @ApiResponse(code = 403, message = "Authentication required"),
      @ApiResponse(code = 404, message = "No change to read-only state")
  })
  void release();

  @ApiOperation(value = "Forcibly release read-only",
    notes = "Forcibly release read-only status, including System initiated tasks. Warning: may result in data loss.")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "System is no longer read-only"),
      @ApiResponse(code = 403, message = "Authentication required"),
      @ApiResponse(code = 404, message = "No change to read-only state")
  })
  void forceRelease();
}
