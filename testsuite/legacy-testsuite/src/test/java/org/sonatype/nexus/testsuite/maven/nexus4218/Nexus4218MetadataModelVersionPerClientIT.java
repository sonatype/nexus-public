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
package org.sonatype.nexus.testsuite.maven.nexus4218;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.maven.tasks.descriptors.RebuildMavenMetadataTaskDescriptor;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.index.artifact.Gav;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;

public class Nexus4218MetadataModelVersionPerClientIT
    extends AbstractNexusIntegrationTest
{

  public Nexus4218MetadataModelVersionPerClientIT() {
    setTestRepositoryId("fake-central");
  }

  @Override
  protected void runOnce()
      throws Exception
  {
    super.runOnce();

    File repo = getTestFile("repo");
    FileUtils.copyDirectory(repo, new File(nexusWorkDir, "storage/nexus-test-harness-snapshot-repo"));

    ScheduledServicePropertyResource r = new ScheduledServicePropertyResource();
    r.setKey(RebuildMavenMetadataTaskDescriptor.REPO_OR_GROUP_FIELD_ID);
    r.setValue(REPO_TEST_HARNESS_SNAPSHOT_REPO);

    TaskScheduleUtil.runTask("RebuildMavenMetadata-Nexus4218-snapshot", RebuildMavenMetadataTaskDescriptor.ID, r);

    r.setValue("fake-central");
    TaskScheduleUtil.runTask("RebuildMavenMetadata-Nexus4218-central", RebuildMavenMetadataTaskDescriptor.ID, r);
    TaskScheduleUtil.waitForAllTasksToStop();
  }

  @Test
  public void maven3()
      throws Exception
  {
    Gav gav =
        new Gav("org.apache.maven", "apache-maven", "3.0.3", "bin", "tar.gz", null, null, null, false, null,
            false, null);
    File bundle =
        downloadArtifact(gav, "target/downloads/nexus4218/" + gav.getArtifactId() + "-" + gav.getVersion());

    UnArchiver unArchive = lookup(ArchiverManager.class).getUnArchiver(bundle);
    unArchive.setSourceFile(bundle);
    unArchive.setDestDirectory(bundle.getParentFile());
    unArchive.extract();

    final StringBuilder sb = new StringBuilder();
    StreamConsumer out = new StreamConsumer()
    {
      public void consumeLine(String line) {
        sb.append(line).append('\n');
      }
    };

    final File mvnInstallDir = new File(bundle.getParentFile(), "apache-maven-3.0.3");

    Commandline cl = new Commandline();
    cl.addEnvironment("M2_HOME", mvnInstallDir.getCanonicalPath());
    cl.setWorkingDirectory(getTestFile("m3"));
    cl.setExecutable(new File(mvnInstallDir, "bin/mvn").getCanonicalPath());
    cl.createArg().setValue("install");
    cl.createArg().setValue("-s");
    cl.createArg().setValue(getOverridableFile("settings.xml").getCanonicalPath());
    final File repo = new File(getTestFile("m3"), "repo");
    cl.createArg().setValue("-Dmaven.repo.local=" + repo.getCanonicalPath());
    int exit = CommandLineUtils.executeCommandLine(cl, out, out);

    assertEquals(sb.toString(), exit, 0);
    assertThat(sb.toString(), containsString("nexus4218/md-test/0.1-SNAPSHOT/md-test-0.1-20110415.143359-7.pom"));
    assertThat(sb.toString(), containsString("nexus4218/md-test/0.1-SNAPSHOT/md-test-0.1-20110415.143359-7.txt"));
    assertThat(sb.toString(),
        containsString("nexus4218/md-test/0.1-SNAPSHOT/md-test-0.1-20110415.125102-3-r1.txt"));
    assertThat(sb.toString(),
        containsString("nexus4218/md-test/0.1-SNAPSHOT/md-test-0.1-20110415.125107-4-r2.txt"));
    assertThat(sb.toString(),
        containsString("nexus4218/md-test/0.1-SNAPSHOT/md-test-0.1-20110415.125112-5-r3.txt"));

    InputStream in =
        new FileInputStream(new File(repo, "nexus4218/md-test/0.1-SNAPSHOT/maven-metadata-nexus.xml"));
    Metadata md = new MetadataXpp3Reader().read(in);
    in.close();
    assertEquals("1.1.0", md.getModelVersion());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void maven2()
      throws Exception
  {
    Gav gav =
        new Gav("org.apache.maven", "apache-maven", "2.0.6", "bin", "tar.gz", null, null, null, false, null,
            false, null);
    File bundle =
        downloadArtifact(gav, "target/downloads/nexus4218/" + gav.getArtifactId() + "-" + gav.getVersion());

    UnArchiver unArchive = lookup(ArchiverManager.class).getUnArchiver(bundle);
    unArchive.setSourceFile(bundle);
    unArchive.setDestDirectory(bundle.getParentFile());
    unArchive.extract();

    final StringBuilder sb = new StringBuilder();
    StreamConsumer out = new StreamConsumer()
    {
      public void consumeLine(String line) {
        sb.append(line).append('\n');
      }
    };

    File mvnInstallDir = new File(bundle.getParentFile(), "maven-2.0.6");

    Commandline cl = new Commandline();
    cl.addEnvironment("M2_HOME", mvnInstallDir.getCanonicalPath());
    cl.setWorkingDirectory(getTestFile("m2"));
    cl.setExecutable(new File(mvnInstallDir, "bin/mvn").getCanonicalPath());
    cl.createArg().setValue("install");
    cl.createArg().setValue("-s");
    cl.createArg().setValue(getOverridableFile("settings.xml").getCanonicalPath());
    final File repo = new File(getTestFile("m2"), "repo");
    cl.createArg().setValue("-Dmaven.repo.local=" + repo.getCanonicalPath());
    final String m2Home = new File(bundle.getParentFile(), "maven-2.0.6").getAbsolutePath();
    cl.addEnvironment("M2_HOME", m2Home);
    int exit = CommandLineUtils.executeCommandLine(cl, out, out);

    assertEquals(sb.toString(), exit, 0);
    assertThat(sb.toString(), containsString("nexus4218/md-test/0.1-SNAPSHOT/md-test-0.1-20110415.143359-7.pom"));
    assertThat(sb.toString(), containsString("nexus4218/md-test/0.1-SNAPSHOT/md-test-0.1-20110415.143359-7.txt"));

    InputStream in =
        new FileInputStream(new File(repo, "nexus4218/md-test/0.1-SNAPSHOT/maven-metadata-central.xml"));
    Metadata md = new MetadataXpp3Reader().read(in);
    in.close();
    assertThat(md.getModelVersion(), anyOf(nullValue(), equalTo("1.0.0")));
  }

  @Test
  public void ivy()
      throws Exception
  {
    Gav gav =
        new Gav("org.apache.ivy", "apache-ivy", "2.2.0", "bin", "tar.gz", null, null, null, false, null, false,
            null);
    File bundle =
        downloadArtifact(gav, "target/downloads/nexus4218/" + gav.getArtifactId() + "-" + gav.getVersion());

    UnArchiver unArchive = lookup(ArchiverManager.class).getUnArchiver(bundle);
    unArchive.setSourceFile(bundle);
    unArchive.setDestDirectory(bundle.getParentFile());
    unArchive.extract();

    final StringBuilder sb = new StringBuilder();
    StreamConsumer out = new StreamConsumer()
    {
      public void consumeLine(String line) {
        sb.append(line).append('\n');
      }
    };

    Commandline cl = new Commandline();
    cl.setWorkingDirectory(getTestFile("ivy"));
    cl.setExecutable("java");
    cl.createArg().setValue("-jar");
    cl.createArg().setValue(
        new File(bundle.getParentFile(), "apache-ivy-2.2.0/ivy-2.2.0.jar").getCanonicalPath());
    cl.createArg().setValue("-settings");
    cl.createArg().setValue(getOverridableFile("ivysetting.xml").getCanonicalPath());
    final File repo = new File(getTestFile("ivy"), "repo");
    cl.createArg().setValue("-cache");
    cl.createArg().setValue(repo.getCanonicalPath());
    int exit = CommandLineUtils.executeCommandLine(cl, out, out);

    assertEquals(sb.toString(), exit, 0);
    assertThat(sb.toString(), containsString("nexus4218/md-test/0.1-SNAPSHOT/md-test-0.1-20110415.143359-7.txt"));
  }

}
