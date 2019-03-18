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
package org.sonatype.nexus.internal.orient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.orient.entity.EntityHook;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class DatabaseServerImplTest
  extends TestSupport
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Mock
  public ApplicationDirectories applicationDirectories;

  @Mock
  public NodeAccess nodeAccess;

  @Mock
  public EventManager eventManager;

  private DatabaseServerImpl underTest;

  @Before
  public void setup() {
    when(applicationDirectories.getWorkDirectory("db")).thenReturn(tempDir.getRoot());

    underTest = new DatabaseServerImpl(applicationDirectories, Collections.emptyList(), Collections.emptyList(),
        ClassLoader.getSystemClassLoader(), false, false, false, "2424-2430", "2480-2490", true, nodeAccess,
        new EntityHook(eventManager));
  }

  @Test
  public void testDisableUnusedDatabases_auditDatabase() throws IOException {
    validateDatabaseUnmounted(tempDir.newFolder("audit"));
  }

  @Test
  public void testDisableUnusedDatabases_analyticsDatabase() throws IOException {
    validateDatabaseUnmounted(tempDir.newFolder("analytics"));
  }

  @Test
  public void testDisableUnusedDatabases_ignoredDatabases() throws IOException {
    validateDatabaseIgnored(tempDir.newFolder("component"));
    validateDatabaseIgnored(tempDir.newFolder("config"));
    validateDatabaseIgnored(tempDir.newFolder("security"));
    validateDatabaseIgnored(tempDir.newFolder("accesslog"));
    validateDatabaseIgnored(tempDir.newFolder("OSystem"));
  }

  @Test
  public void testDisableUnusedDatabases_noDatabases() {
    underTest.disableUnusedDatabases();
  }

  @Test
  public void testDisableUnusedDatabases_invalidDatabaseDir() throws IOException {
    when(applicationDirectories.getWorkDirectory("db")).thenReturn(tempDir.newFile());
    underTest.disableUnusedDatabases();
  }

  private void validateDatabaseUnmounted(File dbDir) throws IOException {
    File ocfSourceFile = new File(dbDir, "database.ocf");
    Files.createFile(ocfSourceFile.toPath());
    assertThat(ocfSourceFile.exists(), is(true));

    underTest.disableUnusedDatabases();

    assertThat(ocfSourceFile.exists(), is(false));
    assertThat(new File(dbDir, "database.ocf.bak").exists(), is(true));
  }

  private void validateDatabaseIgnored(File dbDir) throws IOException {
    File ocfSourceFile = new File(dbDir, "database.ocf");
    Files.createFile(ocfSourceFile.toPath());
    assertThat(ocfSourceFile.exists(), is(true));

    underTest.disableUnusedDatabases();

    assertThat(ocfSourceFile.exists(), is(true));
    assertThat(new File(dbDir, "database.ocf.bak").exists(), is(false));
  }
}
