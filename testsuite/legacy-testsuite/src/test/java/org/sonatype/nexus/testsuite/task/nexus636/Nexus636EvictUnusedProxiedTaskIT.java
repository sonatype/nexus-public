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
package org.sonatype.nexus.testsuite.task.nexus636;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.proxy.attributes.Attributes;
import org.sonatype.nexus.proxy.attributes.JacksonJSONMarshaller;
import org.sonatype.nexus.proxy.attributes.Marshaller;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.EvictUnusedItemsTaskDescriptor;
import org.sonatype.nexus.tasks.descriptors.RebuildAttributesTaskDescriptor;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests evict task.
 */
public class Nexus636EvictUnusedProxiedTaskIT
    extends AbstractNexusIntegrationTest
{

  private File repositoryPath;

  private File attributesPath;

  public Nexus636EvictUnusedProxiedTaskIT() {
    super(REPO_RELEASE_PROXY_REPO1);
  }

  @Before
  public void deployOldArtifacts()
      throws Exception
  {

    repositoryPath = new File(nexusWorkDir, "storage/" + REPO_RELEASE_PROXY_REPO1);
    attributesPath = new File(nexusWorkDir, "storage/" + REPO_RELEASE_PROXY_REPO1 + "/.nexus/attributes");

    File repo = getTestFile("repo");

    FileUtils.copyDirectory(repo, repositoryPath);

    // overwrite attributes
    // FileUtils.copyDirectory( getTestFile( "attributes" ), attributesPath );

    // rebuild attributes
    ScheduledServicePropertyResource prop = new ScheduledServicePropertyResource();
    prop.setKey("repositoryId");
    prop.setValue(this.getTestRepositoryId());
    TaskScheduleUtil.runTask(RebuildAttributesTaskDescriptor.ID, prop);

  }

  @Test
  public void clearProxy()
      throws Exception
  {
    executeTask("clearProxy", "release-proxy-repo-1", 0);

    File[] files = repositoryPath.listFiles();

    if (files != null && files.length != 0) {
      // not true anymore, all "." (dot files) hidden files should be left in there
      // Assert.assertEquals( "All files should be delete from repository except the index:\n"
      // + Arrays.asList( files ), 1, files.length );
      // Assert.assertTrue( "The only file left should be the index.\n" + Arrays.asList( files ),
      // files[0].getAbsolutePath().endsWith( ".index" ) );

      boolean isAllDotFiles = true;

      List<String> paths = new ArrayList<String>();

      for (File file : files) {
        paths.add(file.getPath());
        isAllDotFiles = isAllDotFiles && file.getName().startsWith(".");
      }

      Assert.assertTrue("The only files left should be \"dotted\" files! We have: " + paths, isAllDotFiles);
    }
  }

  @Test
  public void keepTestDeployedFiles()
      throws Exception
  {
    executeTask("keepTestDeployedFiles", "release-proxy-repo-1", 2);

    File artifact = new File(repositoryPath, "nexus636/artifact-new/1.0/artifact-new-1.0.jar");
    Assert.assertTrue("The files deployed by this test should be young enough to be kept", artifact.exists());

  }

  @Test
  public void doNotDeleteEverythingTest()
      throws Exception
  {

    executeTask("doNotDeleteEverythingTest-1", "release-proxy-repo-1", 2);
    // expect 3 files in repo
    File groupDirectory = new File(repositoryPath, this.getTestId());
    File[] files = groupDirectory.listFiles();
    Assert.assertEquals("Expected 3 artifacts in repo:\n" + Arrays.asList(files), files.length, 3);

    // edit dates on files
    File oldJar = new File(this.attributesPath, "nexus636/artifact-old/2.1/artifact-old-2.1.jar");
    File oldPom = new File(this.attributesPath, "nexus636/artifact-old/2.1/artifact-old-2.1.pom");

    // set date to 3 days ago
    this.changeProxyAttributeDate(oldJar, -3);
    this.changeProxyAttributeDate(oldPom, -3);

    // run task
    executeTask("doNotDeleteEverythingTest-2", "release-proxy-repo-1", 2);

    // check file list
    files = groupDirectory.listFiles();
    Assert.assertEquals("Expected 2 artifacts in repo:\n" + Arrays.asList(files), files.length, 2);
  }

  private void executeTask(String taskName, String repository, int cacheAge)
      throws Exception
  {
    ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
    repo.setKey("repositoryId");
    repo.setValue(repository);
    ScheduledServicePropertyResource age = new ScheduledServicePropertyResource();
    age.setKey("evictOlderCacheItemsThen");
    age.setValue(String.valueOf(cacheAge));

    // clean unused
    TaskScheduleUtil.runTask(taskName, EvictUnusedItemsTaskDescriptor.ID, repo, age);
  }

  private Marshaller getMarshaller() {
    return new JacksonJSONMarshaller();
  }

  private void changeProxyAttributeDate(File attributeFile, int daysFromToday)
      throws IOException
  {
    // load file
    Attributes attributes;
    FileInputStream fis = new FileInputStream(attributeFile);
    try {
      attributes = getMarshaller().unmarshal(fis);
    }
    finally {
      fis.close();
    }

    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.DATE, daysFromToday);

    // edit object
    attributes.incrementGeneration();
    attributes.setLastRequested(cal.getTime().getTime());
    attributes.setCheckedRemotely(cal.getTime().getTime());

    // save file
    FileOutputStream fos = new FileOutputStream(attributeFile);
    try {
      getMarshaller().marshal(attributes, fos);
    }
    finally {
      fos.close();
    }
  }

}
