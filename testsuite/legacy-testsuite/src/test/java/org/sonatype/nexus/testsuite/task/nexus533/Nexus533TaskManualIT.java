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
package org.sonatype.nexus.testsuite.task.nexus533;

import java.util.Calendar;
import java.util.Date;

import org.sonatype.nexus.index.tasks.descriptors.UpdateIndexTaskDescriptor;
import org.sonatype.nexus.rest.model.ScheduledServiceBaseResource;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;

import org.apache.commons.lang.time.DateUtils;

public class Nexus533TaskManualIT
    extends AbstractNexusTasksIntegrationIT<ScheduledServiceBaseResource>
{

  private static ScheduledServiceBaseResource scheduledTask;

  @Override
  public ScheduledServiceBaseResource getTaskScheduled() {
    if (scheduledTask == null) {
      scheduledTask = new ScheduledServiceBaseResource();
      scheduledTask.setEnabled(true);
      scheduledTask.setId(null);
      scheduledTask.setName("taskManual");
      scheduledTask.setSchedule("manual");
      // A future date
      Date startDate = DateUtils.addDays(new Date(), 10);
      startDate = DateUtils.round(startDate, Calendar.DAY_OF_MONTH);

      scheduledTask.setTypeId(UpdateIndexTaskDescriptor.ID);

      ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
      prop.setKey("repositoryId");
      prop.setValue("all_repo");
      scheduledTask.addProperty(prop);
    }
    return scheduledTask;
  }

  @Override
  public void updateTask(ScheduledServiceBaseResource scheduledTask) {
    scheduledTask.getProperties().clear();

    ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
    prop.setKey("repositoryId");
    prop.setValue("nexus-test-harness-repo");
    scheduledTask.addProperty(prop);
  }

}
