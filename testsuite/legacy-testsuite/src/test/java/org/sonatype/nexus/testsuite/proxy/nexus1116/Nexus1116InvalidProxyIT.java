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
package org.sonatype.nexus.testsuite.proxy.nexus1116;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.integrationtests.ITGroups.PROXY;
import org.sonatype.nexus.test.utils.TestProperties;
import org.sonatype.nexus.testsuite.proxy.AbstractNexusWebProxyIntegrationTest;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class Nexus1116InvalidProxyIT
    extends AbstractNexusWebProxyIntegrationTest
    implements Runnable
{

  @Test
  @Category(PROXY.class)
  public void checkInvalidProxy()
      throws Exception
  {
    if (true) {
      printKnownErrorButDoNotFail(getClass(), "downloadArtifactOverWebProxy");
      return;
    }

    Thread thread = new Thread(this);
    thread.setDaemon(true);// don't stuck VM
    thread.start();
    for (int i = 0; i < 100; i++) {
      String status = this.status;

      if (status.startsWith("fail")) {
        Assert.fail("Verifier fail: " + status);
      }
      else if (status.equals("executed")) {
        // finished ok
        return;
      }

      Thread.yield();
      Thread.sleep(200);
    }

    Assert.fail("Verifier didn't runn after 20 seconds: " + this.status);
  }

  private String status = "notStarted";

  public void run() {
    status = "started";
    File mavenProject = getTestFile("pom.xml").getParentFile();

    System.setProperty("maven.home", TestProperties.getString("maven.instance"));
    Verifier verifier;
    try {
      verifier = new Verifier(mavenProject.getAbsolutePath(), false);
      status = "verifierCreated";
    }
    catch (VerificationException e) {
      status = "failCreation" + e.getMessage();
      return;
    }

    File mavenRepository = new File(TestProperties.getString("maven.local.repo"));
    verifier.setLocalRepo(mavenRepository.getAbsolutePath());

    verifier.resetStreams();

    List<String> options = new ArrayList<String>();
    options.add("-X");
    options.add("-Dmaven.repo.local=" + mavenRepository.getAbsolutePath());
    options.add("-s " + getOverridableFile("settings.xml"));
    verifier.setCliOptions(options);

    status = "pre-execute";
    try {
      verifier.executeGoal("dependency:resolve");
      status = "executed";
    }
    catch (VerificationException e) {
      status = "failExecute" + e.getMessage();
    }
  }

}
