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
package org.sonatype.nexus.testsuite.index.nexus637;

import java.io.File;
import java.util.Arrays;

import org.sonatype.nexus.index.tasks.descriptors.PublishIndexesTaskDescriptor;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;

/**
 * Test task Publish Indexes is working.
 *
 * @author marvin
 */
public class Nexus637PublishIndexIT
    extends AbstractNexusIntegrationTest
{

  public Nexus637PublishIndexIT() {
    super("nexus-test-harness-repo");
  }

  @BeforeClass
  public static void clean()
      throws Exception
  {
    cleanWorkDir();
  }

  @Test
  public void publishIndex()
      throws Exception
  {
    File repositoryPath = new File(nexusWorkDir, "storage/nexus-test-harness-repo");
    File index = new File(repositoryPath, ".index");

    if (index.exists()) {
      // can't contain the OSS index
      assertThat(
          Arrays.asList(index.list()),
          not(hasItems("nexus-maven-repository-index.gz", "nexus-maven-repository-index.gz.md5",
              "nexus-maven-repository-index.gz.sha1", "nexus-maven-repository-index.properties",
              "nexus-maven-repository-index.properties.md5", "nexus-maven-repository-index.properties.sha1",
              "nexus-maven-repository-index.zip", "nexus-maven-repository-index.zip.md5",
              "nexus-maven-repository-index.zip.sha1")));
    }

    ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
    prop.setKey("repositoryId");
    prop.setValue("nexus-test-harness-repo");

    TaskScheduleUtil.runTask(PublishIndexesTaskDescriptor.ID, prop);

    Assert.assertTrue(".index should exists after publish index task was run.", index.exists());
  }
}
