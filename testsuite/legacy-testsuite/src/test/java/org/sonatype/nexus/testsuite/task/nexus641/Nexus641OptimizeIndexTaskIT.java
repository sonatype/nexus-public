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
package org.sonatype.nexus.testsuite.task.nexus641;

import java.io.IOException;

import org.sonatype.nexus.index.tasks.descriptors.OptimizeIndexTaskDescriptor;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test task OptimizeIndex Repositories.
 *
 * @author marvin
 */
public class Nexus641OptimizeIndexTaskIT
    extends AbstractNexusIntegrationTest
{
  protected static Logger logger = LoggerFactory.getLogger(Nexus641OptimizeIndexTaskIT.class);

  public Nexus641OptimizeIndexTaskIT()
      throws IOException
  {
    super("nexus641");
  }

  @Test
  public void testIndexOptimizer()
      throws Exception
  {
    // reindex
    ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
    prop.setKey("repositoryId");
    prop.setValue("nexus-test-harness-repo");

    // reindex
    TaskScheduleUtil.runTask(OptimizeIndexTaskDescriptor.ID, prop);

  }

}
