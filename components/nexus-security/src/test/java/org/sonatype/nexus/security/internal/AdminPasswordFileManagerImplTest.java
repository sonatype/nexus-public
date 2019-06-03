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
package org.sonatype.nexus.security.internal;

import java.io.File;
import java.nio.file.Files;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class AdminPasswordFileManagerImplTest
    extends TestSupport
{
  private AdminPasswordFileManagerImpl underTest;

  @Mock
  private ApplicationDirectories applicationDirectories;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setup() throws Exception {
    when(applicationDirectories.getWorkDirectory()).thenReturn(temporaryFolder.newFolder("workdir"));
    underTest = new AdminPasswordFileManagerImpl(applicationDirectories);
  }

  @Test
  public void testExists() throws Exception {
    File passwordFile = new File(applicationDirectories.getWorkDirectory(), "admin.password");
    assertThat(underTest.exists(), is(false));
    Files.write(passwordFile.toPath(), "testpass".getBytes("UTF-8"));
    assertThat(underTest.exists(), is(true));
    passwordFile.delete();
    assertThat(underTest.exists(), is(false));
  }

  @Test
  public void testWriteFile() throws Exception {
    underTest.writeFile("testpass");
    String storedPassword = new String(
        Files.readAllBytes(new File(applicationDirectories.getWorkDirectory(), "admin.password").toPath()));
    assertThat(storedPassword, is("testpass"));
  }

  @Test
  public void testWriteFile_workdirExists() throws Exception {
    File directory = applicationDirectories.getWorkDirectory();
    directory.mkdirs();
    assertThat(directory.isDirectory(), is(true));
    assertThat(directory.exists(), is(true));

    underTest.writeFile("testpass");

    String storedPassword = new String(
        Files.readAllBytes(new File(applicationDirectories.getWorkDirectory(), "admin.password").toPath()));
    assertThat(storedPassword, is("testpass"));
  }

  @Test
  public void testWriteFile_failure() throws Exception {
    File passwordFile = new File(applicationDirectories.getWorkDirectory(), "admin.password");
    Files.write(passwordFile.toPath(), "testpass".getBytes("UTF-8"));
    passwordFile.setWritable(false);

    assertThat(underTest.writeFile("testpass2"), is(false));
    String storedPassword = new String(
        Files.readAllBytes(new File(applicationDirectories.getWorkDirectory(), "admin.password").toPath()));
    assertThat(storedPassword, is("testpass"));
  }

  @Test
  public void testReadFile() throws Exception {
    File passwordFile = new File(applicationDirectories.getWorkDirectory(), "admin.password");
    Files.write(passwordFile.toPath(), "testpass".getBytes("UTF-8"));
    assertThat(underTest.readFile(), is("testpass"));
  }

  @Test
  public void testRemoveFile() throws Exception {
    File passwordFile = new File(applicationDirectories.getWorkDirectory(), "admin.password");
    Files.write(passwordFile.toPath(), "testpass".getBytes("UTF-8"));
    underTest.removeFile();
    assertThat(passwordFile.exists(), is(false));
  }
}
