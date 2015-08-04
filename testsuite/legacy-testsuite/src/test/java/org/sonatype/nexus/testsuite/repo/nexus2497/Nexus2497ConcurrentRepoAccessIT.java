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
package org.sonatype.nexus.testsuite.repo.nexus2497;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.maven.tasks.RebuildMavenMetadataTask;
import org.sonatype.nexus.maven.tasks.descriptors.RebuildMavenMetadataTaskDescriptor;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.GavUtil;
import org.sonatype.nexus.test.utils.MavenDeployer;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.maven.it.Verifier;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.sonatype.nexus.test.utils.FileTestingUtils.populate;

public class Nexus2497ConcurrentRepoAccessIT
    extends AbstractNexusIntegrationTest
{

  private static final String TASK_NAME = "RebuildMavenMetadata-Nexus2497";

  private static File[] files;

  @BeforeClass
  public static void createFiles()
      throws Exception
  {
    files = new File[5];
    files[0] = populate(new File("./target/downloads/nexus2497", "file1.jar"), 3);
    files[1] = populate(new File("./target/downloads/nexus2497", "file2.jar"), 3);
    files[2] = populate(new File("./target/downloads/nexus2497", "file3.jar"), 3);
    files[3] = populate(new File("./target/downloads/nexus2497", "file4.jar"), 3);
    files[4] = populate(new File("./target/downloads/nexus2497", "file5.jar"), 3);
    // files[5] = populate( new File( "./target/downloads/nexus2497", "file6.jar" ) );
    // files[6] = populate( new File( "./target/downloads/nexus2497", "file7.jar" ) );
    // files[7] = populate( new File( "./target/downloads/nexus2497", "file8.jar" ) );
    // files[8] = populate( new File( "./target/downloads/nexus2497", "file9.jar" ) );
    // files[9] = populate( new File( "./target/downloads/nexus2497", "file0.jar" ) );
  }

  @Test
  @Ignore("Test is unstable - needs to be rewritten or replaced. See NEXUS-4606")
  public void doConcurrence()
      throws Exception
  {
    List<Thread> threads = new ArrayList<Thread>();
    final Map<Thread, Throwable> errors = new LinkedHashMap<Thread, Throwable>();
    for (final File f : files) {
      Thread t = new Thread(new Runnable()
      {

        public void run() {
          try {
            Verifier v =
                MavenDeployer.deployAndGetVerifier(GavUtil.newGav("nexus2497", "concurrence",
                    "1.0-SNAPSHOT"),
                    getRepositoryUrl(REPO_TEST_HARNESS_SNAPSHOT_REPO), f,
                    getOverridableFile("settings.xml"));

            v.verifyErrorFreeLog();
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      });
      t.setUncaughtExceptionHandler(new UncaughtExceptionHandler()
      {
        public void uncaughtException(Thread t, Throwable e) {
          errors.put(t, e);
        }
      });

      threads.add(t);
    }

    ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
    repo.setKey("repositoryId");
    repo.setValue(REPO_TEST_HARNESS_SNAPSHOT_REPO);
    TaskScheduleUtil.runTask(TASK_NAME, RebuildMavenMetadataTaskDescriptor.ID, repo);

    // uploads while rebuilding
    for (Thread thread : threads) {
      thread.start();
      Thread.yield();
    }

    // w8 for uploads
    for (Thread thread : threads) {
      thread.join();
    }

    TaskScheduleUtil.waitForAllTasksToStop(RebuildMavenMetadataTask.class);

    if (!errors.isEmpty()) {
      Set<Entry<Thread, Throwable>> entries = errors.entrySet();
      ByteArrayOutputStream str = new ByteArrayOutputStream();
      PrintStream s = new PrintStream(str);
      for (Entry<Thread, Throwable> entry : entries) {
        s.append(entry.getKey().getName());
        s.append("\n");
        entry.getValue().printStackTrace(s);
        s.append("\n");
        s.append("\n");
      }

      Assert.fail("Found some errors deploying:\n" + str.toString());
    }
    Assert.assertEquals(TaskScheduleUtil.getStatus(TASK_NAME), "Ok");
  }
}
