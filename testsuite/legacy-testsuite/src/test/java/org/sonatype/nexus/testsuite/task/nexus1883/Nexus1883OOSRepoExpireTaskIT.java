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
package org.sonatype.nexus.testsuite.task.nexus1883;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.ExpireCacheTaskDescriptor;
import org.sonatype.nexus.test.utils.RepositoryStatusMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.junit.Before;
import org.junit.Test;

public class Nexus1883OOSRepoExpireTaskIT
    extends AbstractNexusIntegrationTest
{

  @Before
  public void putOutOfService()
      throws Exception
  {
    RepositoryStatusMessageUtil.putOutOfService(REPO_TEST_HARNESS_SHADOW, "hosted");
  }

  @Test
  public void expireAllRepos()
      throws Exception
  {
    ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
    prop.setKey("repositoryId");
    prop.setValue(REPO_TEST_HARNESS_SHADOW);

    TaskScheduleUtil.runTask(ExpireCacheTaskDescriptor.ID, prop);
  }
}
