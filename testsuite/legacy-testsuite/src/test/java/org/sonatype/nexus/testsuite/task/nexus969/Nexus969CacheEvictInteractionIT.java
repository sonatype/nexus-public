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
package org.sonatype.nexus.testsuite.task.nexus969;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.ScheduledServiceBaseResource;
import org.sonatype.nexus.rest.model.ScheduledServiceListResource;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.EvictUnusedItemsTaskDescriptor;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.Status;

public class Nexus969CacheEvictInteractionIT
    extends AbstractNexusIntegrationTest
{

  private static final String CACHE_EVICT = "cache-evict";

  @Test
  public void testCacheAndEvict()
      throws Exception
  {
    if (true) {
      printKnownErrorButDoNotFail(getClass(), "Can't be kept active, is breaking all other tests");
      return;
    }
    String id1 = createEvictTask(CACHE_EVICT).getId();
    String id2 = createEvictTask(CACHE_EVICT + "2").getId();
    Assert.assertFalse(id1.equals(id2));
    restartNexus();
    String id3 = createEvictTask(CACHE_EVICT + "3").getId();
    Assert.assertFalse("The new task ID should be different both are : " + id3, id1.equals(id3));
    Assert.assertFalse("The new task ID should be different both are: " + id3, id2.equals(id3));
  }

  private ScheduledServiceListResource createEvictTask(String taskName)
      throws Exception
  {
    ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
    repo.setKey("repositoryId");
    repo.setValue("all_repo");
    ScheduledServicePropertyResource age = new ScheduledServicePropertyResource();
    age.setKey("evictOlderCacheItemsThen");
    age.setValue(String.valueOf(0));
    ScheduledServiceBaseResource scheduledTask = new ScheduledServiceBaseResource();
    scheduledTask.setEnabled(true);
    scheduledTask.setId(null);
    scheduledTask.setName(taskName);
    scheduledTask.setTypeId(EvictUnusedItemsTaskDescriptor.ID);
    scheduledTask.setSchedule("manual");
    scheduledTask.addProperty(age);
    scheduledTask.addProperty(repo);

    Status status = TaskScheduleUtil.create(scheduledTask);
    Assert.assertTrue("Unable to create task: " + status.getDescription(), status.isSuccess());

    return TaskScheduleUtil.getTask(taskName);
  }
}
