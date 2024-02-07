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
package org.sonatype.nexus.scheduling.internal.resources.doc;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.sonatype.nexus.scheduling.api.TaskXO;
import org.sonatype.nexus.rest.Page;

/**
 * Swagger documentation for {@link TasksResource}
 *
 * @since 3.6
 */
@Api(value = "Tasks")
public interface TasksApiResourceDoc
{
  @ApiOperation("List tasks")
  Page<TaskXO> getTasks(@ApiParam(value = "Type of the tasks to get") final String type);

  @ApiOperation("Get a single task by id")
  @ApiResponses(value = {
      @ApiResponse(code = 404, message = "Task not found")
  })
  TaskXO getTaskById(@ApiParam(value = "Id of the task to get") final String id);

  @ApiOperation("Run task")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Task was run"),
      @ApiResponse(code = 404, message = "Task not found"),
      @ApiResponse(code = 405, message = "Task is disabled")
  })
  void run(@ApiParam(value = "Id of the task to run") final String id);

  @ApiOperation("Stop task")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Task was stopped"),
      @ApiResponse(code = 409, message = "Unable to stop task"),
      @ApiResponse(code = 404, message = "Task not found")
  })
  void stop(@ApiParam(value = "Id of the task to stop") final String id);
}
