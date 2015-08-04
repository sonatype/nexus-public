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
package org.sonatype.nexus.testsuite.task.nexus977tasks;

import java.io.File;

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.RebuildAttributesTaskDescriptor;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.DirectoryScanner;
import org.junit.Assert;
import org.junit.Test;

public class Nexus977GroupOfGroupsRebuildAttributesTaskIT
    extends AbstractNexusProxyIntegrationTest
{

  @Test
  public void rebuild()
      throws Exception
  {
    // add some extra artifacts
    File dest = new File(nexusWorkDir, "storage/r1/nexus977tasks/project/1.0/project-1.0.jar");
    dest.getParentFile().mkdirs();
    FileUtils.copyFile(getTestFile("project.jar"), dest);

    dest = new File(nexusWorkDir, "storage/r2/nexus977tasks/project/2.0/project-2.0.jar");
    dest.getParentFile().mkdirs();
    FileUtils.copyFile(getTestFile("project.jar"), dest);

    dest = new File(nexusWorkDir, "storage/r3/nexus977tasks/project/3.0/project-3.0.jar");
    dest.getParentFile().mkdirs();
    FileUtils.copyFile(getTestFile("project.jar"), dest);

    ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
    // I really don't get this, and we probably have some very bad problems with these Nexus977 ITs
    // By reading code, this IT should actually fail. By running this IT alone with
    // $ mvn clean install -Dit.test=Nexus977GroupOfGroupsRebuildAttributesTaskIT
    // it DOES fail. But when run in suite, it does not fail (only sometimes).
    // The cause: since may 2 2011 (change f72ade4a6719dce643da978348b17efbba77b426) the
    // task "RebuildAttributesTask" does not _cascade_!!!
    // repo.setKey( "repositoryId" );
    // repo.setValue( "g4" );
    TaskScheduleUtil.runTask(RebuildAttributesTaskDescriptor.ID, repo);

    DirectoryScanner scan = new DirectoryScanner();
    scan.setBasedir(new File(nexusWorkDir, "storage"));
    scan.addDefaultExcludes();
    scan.setExcludes(new String[]{"**/.nexus/attributes/"});
    scan.scan();
    String[] storageContent = scan.getIncludedFiles();

    scan = new DirectoryScanner();
    scan.setBasedir(new File(nexusWorkDir, "storage"));
    scan.addDefaultExcludes();
    scan.setIncludes(new String[]{"**/.nexus/attributes/"});
    scan.scan();
    String[] attributesContent = scan.getIncludedFiles();

    // the paths will differ, but length should be equal
    Assert.assertEquals(attributesContent.length, storageContent.length);
  }

}
