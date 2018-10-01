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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rest.Page;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.api.TaskXO;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.rest.APIConstants.BETA_API_PREFIX;

/**
 * @deprecated since 3.14, use {@link TasksResource} instead.
 */
@Deprecated
@Named
@Singleton
@Path(TasksResourceBeta.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class TasksResourceBeta
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_URI = BETA_API_PREFIX + "/tasks";

  private final TasksResource delegate;

  @Inject
  public TasksResourceBeta(final TaskScheduler taskScheduler) {
    delegate = new TasksResource(taskScheduler);
  }

  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:tasks:read")
  public Page<TaskXO> getTasks(@QueryParam("type") final String type) {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, TasksResource.RESOURCE_URI);
    return delegate.getTasks(type);
  }

  @GET
  @Path("/{id}")
  @RequiresAuthentication
  @RequiresPermissions("nexus:tasks:read")
  public TaskXO getTaskById(@PathParam("id") final String id) {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, TasksResource.RESOURCE_URI);
    return delegate.getTaskById(id);
  }

  @POST
  @Path("/{id}/run")
  @RequiresAuthentication
  @RequiresPermissions("nexus:tasks:start")
  public void run(@PathParam("id") final String id) {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, TasksResource.RESOURCE_URI);
    delegate.run(id);
  }

  @POST
  @Path("/{id}/stop")
  @RequiresAuthentication
  @RequiresPermissions("nexus:tasks:stop")
  public void stop(@PathParam("id") final String id) {
    log.warn("Deprecated endpoint: {}, please use: {}", RESOURCE_URI, TasksResource.RESOURCE_URI);
    delegate.stop(id);
  }
}
