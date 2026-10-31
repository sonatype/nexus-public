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

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.sonatype.nexus.scheduling.api.TaskXO;
import org.sonatype.nexus.rest.Page;

/**
 * Swagger documentation for {@link TasksResource}
 *
 * @since 3.6
 */
@Tag(name = "Tasks")
public interface TasksApiResourceDoc
{
  @Operation(summary = "List tasks")
  Page<TaskXO> getTasks(@Parameter(description = "Type of the tasks to get") final String type);

  @Operation(summary = "Get a single task by id")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Task returned",
          content = @Content(schema = @Schema(implementation = TaskXO.class))),
      @ApiResponse(responseCode = "404", description = "Task not found")
  })
  TaskXO getTaskById(@Parameter(description = "Id of the task to get") final String id);

  @Operation(summary = "Run task")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Task was run"),
      @ApiResponse(responseCode = "404", description = "Task not found"),
      @ApiResponse(responseCode = "405", description = "Task is disabled")
  })
  void run(@Parameter(description = "Id of the task to run") final String id);

  @Operation(summary = "Stop task")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Task was stopped"),
      @ApiResponse(responseCode = "409", description = "Unable to stop task"),
      @ApiResponse(responseCode = "404", description = "Task not found")
  })
  void stop(@Parameter(description = "Id of the task to stop") final String id);
}
