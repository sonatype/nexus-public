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
package org.sonatype.nexus.testsuite.proxy.nexus178;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.integrationtests.ITGroups.PROXY;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.FileTestingUtils;

import org.apache.commons.io.FileUtils;
import org.apache.maven.index.artifact.Gav;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Create an http server. Create a proxy repo to http server. Test if connection works. block proxy, change file on
 * http
 * server. test connection. check to make sure file is the one expected. Delete file from nexus.
 */
public class Nexus178BlockProxyDownloadIT
    extends AbstractNexusProxyIntegrationTest
{

  public static final String TEST_RELEASE_REPO = "release-proxy-repo-1";

  public Nexus178BlockProxyDownloadIT() {
    super(TEST_RELEASE_REPO);
  }

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  @Category(PROXY.class)
  public void blockProxy()
      throws Exception
  {

    Gav gav =
        new Gav(this.getTestId(), "block-proxy-download-test", "1.1.a", null, "jar", 0, new Date().getTime(),
            "Simple Test Artifact", false, null, false, null);

    // download file
    File originalFile = this.downloadArtifact(gav, "target/downloads/original");

    // blockProxy
    repositoryUtil.setBlockProxy(TEST_RELEASE_REPO, true);

    // change file on server
    File localFile = this.getLocalFile(TEST_RELEASE_REPO, gav);
    // backup file on server
    this.backupFile(localFile);
    try {
      // we need to edit the file now, (its just a text file)
      this.changeFile(localFile);

      // redownload file
      File newFile = this.downloadArtifact(gav, "target/downloads/new");

      // check to see if file matches original file
      Assert.assertTrue(FileTestingUtils.compareFileSHA1s(originalFile, newFile));

      // check to see if file does match new file.
      Assert.assertFalse(FileTestingUtils.compareFileSHA1s(originalFile, localFile));

      // if we don't unblock the proxy the other tests will be mad
      repositoryUtil.setBlockProxy(TEST_RELEASE_REPO, false);
    }
    finally {
      this.revertFile(localFile);
    }

  }

  private void backupFile(File file)
      throws IOException
  {
    File backupFile = new File(file.getParentFile(), file.getName() + ".backup");

    FileUtils.copyFile(file, backupFile);
  }

  private void revertFile(File file)
      throws IOException
  {
    File backupFile = new File(file.getParentFile(), file.getName() + ".backup");

    FileUtils.copyFile(backupFile, file);
  }

  private void changeFile(File file)
      throws IOException
  {
    PrintWriter printWriter = new PrintWriter(new FileWriter(file));
    printWriter.println("I just changed the content of this file!");
    printWriter.close();
  }

}
