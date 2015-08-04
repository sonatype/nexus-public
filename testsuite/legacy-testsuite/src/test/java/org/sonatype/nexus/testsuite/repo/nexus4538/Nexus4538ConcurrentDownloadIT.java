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
package org.sonatype.nexus.testsuite.repo.nexus4538;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.Thread.State;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.tasks.descriptors.RebuildAttributesTaskDescriptor;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.maven.index.artifact.Gav;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.sonatype.nexus.integrationtests.RequestFacade.clockUrlDownload;
import static org.sonatype.nexus.test.utils.FileTestingUtils.populate;

/**
 * Test which makes sure that simultaneous requests for the same artifact are not serialized.
 */
public class Nexus4538ConcurrentDownloadIT
    extends AbstractNexusIntegrationTest
{

  private Gav gav;

  @Before
  public void createFiles()
      throws Exception
  {
    gav = GavUtil.newGav("nexus4538", "artifact", "1.0");
    File f = new File(nexusWorkDir + "/storage/" + REPO_TEST_HARNESS_REPO, getRelitiveArtifactPath(gav));
    populate(f, 5);

    TaskScheduleUtil.runTask(RebuildAttributesTaskDescriptor.ID);
  }

  @Test
  public void makeSureConcurrentDownloadisNotSerialized()
      throws Exception
  {
    String baseUrl =
        AbstractNexusIntegrationTest.nexusBaseUrl + REPOSITORY_RELATIVE_URL + REPO_TEST_HARNESS_REPO + "/";
    String path = getRelitiveArtifactPath(gav);
    final URL url = new URL(baseUrl + path);

    // same trick as with the throttling tests to counter load spikes on CI:
    // get an already throttled baseline and test that it does not get worse.
    // This download will take about 3 seconds now.
    final long op = clockUrlDownload(url, 2000);

    final Long[] time = new Long[1];
    final Throwable[] errors = new Throwable[1];
    Thread t = new Thread(new Runnable()
    {
      public void run() {
        try {
          // limit speed to make sure it is gonna lock nexus for quite some time
          time[0] = clockUrlDownload(url, 10);
        }
        catch (Exception e) {
          errors[0] = e;
          time[0] = -1L;
        }
      }
    });
    t.setUncaughtExceptionHandler(new UncaughtExceptionHandler()
    {
      public void uncaughtException(Thread t, Throwable e) {
        errors[0] = e;
      }
    });

    // let java kill it if VM wanna quit
    t.setDaemon(true);

    // start download in background
    t.start();
    for (int i = 0; i < 10; i++) {
      Thread.yield();
      Thread.sleep(1);
    }

    // while download is happening let's check if nexus still responsive
    final long ping = clockUrlDownload(url);

    // check if ping was not blocked by download
    assertThat("Ping took " + ping + " original ping " + op, ping, lessThan(op * 2));

    if (time[0] != null) {
      assertThat("Ping took " + ping + " dl time: " + time[0], ping, lessThan(time[0]));
    }
    assertThat(t.getState(), not(equalTo(State.TERMINATED)));

    // check if it is error free
    if (errors[0] != null) {
      ByteArrayOutputStream str = new ByteArrayOutputStream();
      PrintStream s = new PrintStream(str);
      for (Throwable e : errors) {
        e.printStackTrace(s);
        s.append("\n");
        s.append("\n");
      }

      assertThat("Found some errors downloading:\n" + str.toString(), false);
    }

    stop(t);
  }

  @SuppressWarnings("deprecation")
  private void stop(Thread t) {
    // I know, I know, shouldn't be doing this
    t.stop();
  }

}
