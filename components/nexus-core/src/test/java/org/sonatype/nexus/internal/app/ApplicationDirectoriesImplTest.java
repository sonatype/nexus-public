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
package org.sonatype.nexus.internal.app;

import java.io.File;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.goodies.testsupport.hamcrest.FileMatchers;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for {@link ApplicationDirectoriesImpl}
 */
public class ApplicationDirectoriesImplTest
  extends TestSupport
{
  private File installDir;

  private File appDir;

  private File workDir;

  private ApplicationDirectoriesImpl underTest;

  @Before
  public void setUp() throws Exception {
    installDir = util.createTempDir("install");
    appDir = util.createTempDir("app");
    workDir = util.createTempDir("work");
    underTest = new ApplicationDirectoriesImpl(installDir, appDir, workDir);
    File childAppDir = new File(appDir, "child");
    childAppDir.createNewFile();
    childAppDir.deleteOnExit();
  }

  @Test
  public void ensureTempDir_exists() throws Exception {
    File dir = underTest.getTemporaryDirectory();
    assertThat(dir, notNullValue());
  }

  @Test
  public void ensureAppDir_exists() throws Exception {
    File dir = underTest.getAppDirectory();
    assertThat(dir, notNullValue());
    assertThat(dir, is(appDir));
    assertThat(dir, FileMatchers.exists());
  }

  @Test
  public void ensureAppDir_childExists() throws Exception {
    File dir = underTest.getAppDirectory("child");
    assertThat(dir, notNullValue());
    assertThat(dir, FileMatchers.exists());
  }

  @Test
  public void ensureAppDir_childNotExists() throws Exception {
    File dir = underTest.getAppDirectory("missing");
    assertThat(dir, notNullValue());
    assertThat(dir, not(FileMatchers.exists()));
  }

  @Test
  public void ensureWorkDir_exists() throws Exception {
    File dir = underTest.getWorkDirectory();
    assertThat(dir, notNullValue());
    assertThat(dir, is(workDir));
    assertThat(dir, FileMatchers.exists());
  }

  @Test
  public void ensureWorkDir_childExists() throws Exception {
    File dir = underTest.getWorkDirectory("child");
    assertThat(dir, notNullValue());
    assertThat(dir, FileMatchers.exists());
  }

  @Test
  public void ensureWorkDir_childWithCreateExists() throws Exception {
    File dir = underTest.getWorkDirectory("child", true);
    assertThat(dir, notNullValue());
    assertThat(dir, FileMatchers.exists());
  }

  @Test
  public void ensureWorkDir_childNoCreateNotExists() throws Exception {
    File dir = underTest.getWorkDirectory("child", false);
    assertThat(dir, notNullValue());
    assertThat(dir, not(FileMatchers.exists()));
  }
}
