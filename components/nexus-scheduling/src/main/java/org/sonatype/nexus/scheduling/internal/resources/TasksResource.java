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
package org.sonatype.nexus.scheduling.internal.resources;

import java.util.List;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.POST;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.api.TaskXO;
import org.sonatype.nexus.scheduling.internal.resources.doc.TasksResourceDoc;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.sonatype.nexus.rest.APIConstants.V1_API_PREFIX;

/**
 * @since 3.6
 */
@Named
@Singleton
@Path(TasksResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class TasksResource
    extends ComponentSupport
    implements Resource, TasksResourceDoc
{
  public static final String RESOURCE_URI = V1_API_PREFIX + "/tasks";

  private static final String TRIGGER_SOURCE = "REST API";

  private final TaskScheduler taskScheduler;

  @Inject
  public TasksResource(final TaskScheduler taskScheduler) {
    this.taskScheduler = checkNotNull(taskScheduler);
  }

  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:tasks:read")
  public Page<TaskXO> getTasks(@QueryParam("type") final String type) {
    List<TaskXO> taskXOs = taskScheduler.listsTasks().stream()
        .filter(taskInfo -> taskInfo.getConfiguration().isVisible())
        .filter(taskInfo -> typeParameterMatches(type, taskInfo))
        .map(TaskXO::fromTaskInfo)
        .collect(toList());

    return new Page<>(taskXOs, null);
  }

  @GET
  @Path("/{id}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:tasks:read")
  public TaskXO getTaskById(@PathParam("id") final String id) {
    return TaskXO.fromTaskInfo(getTaskInfo(id));
  }

  @POST
  @Path("/{id}/run")
  @RequiresAuthentication
  @RequiresPermissions("nexus:tasks:start")
  public void run(@PathParam("id") final String id) {
    try {
      TaskInfo taskInfo = getTaskInfo(id);

      if (!taskInfo.getConfiguration().isEnabled()) {
        throw new NotAllowedException(format("Task %s is disabled", id));
      }

      taskInfo.runNow(TRIGGER_SOURCE);
    }
    catch (NotFoundException | NotAllowedException e) {
      throw e;
    }
    catch (Exception e) {
      log.error("error running task with id {}", id, e);
      throw new WebApplicationException(format("Error running task %s", id), INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/{id}/stop")
  @RequiresAuthentication
  @RequiresPermissions("nexus:tasks:stop")
  public void stop(@PathParam("id") final String id) {
    try {
      TaskInfo taskInfo = getTaskInfo(id);
      Future<?> taskFuture = taskInfo.getCurrentState().getFuture();
      if (taskFuture == null) {
        throw new WebApplicationException(format("Task %s is not running", id), CONFLICT);
      }
      if (!taskFuture.cancel(false)) {
        throw new WebApplicationException(format("Unable to stop task %s", id), CONFLICT);
      }
    }
    catch (WebApplicationException webApplicationException) {
      throw webApplicationException;
    }
    catch (Exception e) {
      log.error("error stopping task with id {}", id, e);
      throw new WebApplicationException(format("Error running task %s", id), INTERNAL_SERVER_ERROR);
    }
  }

  private TaskInfo getTaskInfo(final String id) {
    return ofNullable(taskScheduler.getTaskById(id))
        .filter(taskInfo -> taskInfo.getConfiguration().isVisible())
        .orElseThrow(() -> new NotFoundException("Unable to locate task with id " + id));
  }

  private static boolean typeParameterMatches(final String type, final TaskInfo taskInfo) {
    return type == null || type.isEmpty() || type.equals(taskInfo.getTypeId());
  }
}
