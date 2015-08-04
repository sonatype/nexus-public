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
package org.sonatype.nexus.plugins.tasks.api;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.scheduling.ScheduledTask;

import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import static org.sonatype.nexus.plugins.tasks.api.TasksWaitForPlexusResource.getTaskByName;
import static org.sonatype.nexus.plugins.tasks.api.TasksWaitForPlexusResource.isTaskCompleted;
import static org.sonatype.nexus.plugins.tasks.api.TasksWaitForPlexusResource.sleep;

@Singleton
@Named
public class TaskHelperPlexusResource
    extends AbstractPlexusResource
{

  private final NexusScheduler nexusScheduler;

  @Inject
  public TaskHelperPlexusResource(final NexusScheduler nexusScheduler) {
    this.nexusScheduler = nexusScheduler;
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "anon");
  }

  @Override
  public String getResourceUri() {
    return "/taskhelper";
  }

  @Override
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    Form form = request.getResourceRef().getQueryAsForm();
    String name = form.getFirstValue("name");
    String taskType = form.getFirstValue("taskType");
    String attemptsParam = form.getFirstValue("attempts");
    int attempts = 300;

    if (attemptsParam != null) {
      try {
        attempts = Integer.parseInt(attemptsParam);
      }
      catch (NumberFormatException e) {
        // ignore, will use default of 300
      }
    }

    final ScheduledTask<?> namedTask = getTaskByName(nexusScheduler, name);

    if (name != null && namedTask == null) {
      // task wasn't found, so bounce on outta here
      response.setStatus(Status.SUCCESS_OK);
      return "OK";
    }

    for (int i = 0; i < attempts; i++) {
      sleep();

      if (isTaskCompleted(nexusScheduler, taskType, namedTask)) {
        response.setStatus(Status.SUCCESS_OK);
        return "OK";
      }
    }

    response.setStatus(Status.SUCCESS_NO_CONTENT);
    return "Tasks Not Finished";
  }

}
