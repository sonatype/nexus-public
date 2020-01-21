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
package org.sonatype.nexus.datastore.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @since 3.21
 */
public class DataStoreRestorerLocalImplTest
    extends TestSupport
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private ApplicationDirectories directories;

  @Mock
  private DataStoreConfiguration dataStoreConfiguration;

  private DataStoreRestorerLocalImpl underTest;

  private File workDirectory;

  @Before
  public void setup() throws IOException {
    workDirectory = temporaryFolder.newFolder();
    when(directories.getWorkDirectory(any())).thenAnswer(i -> new File(workDirectory, (String) i.getArguments()[0]));
    when(dataStoreConfiguration.getName()).thenReturn("foo");

    underTest = new DataStoreRestorerLocalImpl(directories);
  }

  @Test
  public void testMaybeRestore() throws IOException {
    makeWorkDirectory("restore-from-backup");
    createBackup("foo");

    assertTrue(underTest.maybeRestore(dataStoreConfiguration));

    // check no file was unzipped
    File dbDir = directories.getWorkDirectory("db");
    assertThat(dbDir.list(), arrayContaining("foo.mv.db"));
  }

  @Test
  public void testMaybeRestore_newInstall() {
    when(dataStoreConfiguration.getName()).thenReturn("config");
    assertFalse(underTest.maybeRestore(dataStoreConfiguration));
  }

  @Test
  public void testMaybeRestore_existingDb() throws IOException {
    makeWorkDirectory("db");
    directories.getWorkDirectory("db/foo.mv.db").createNewFile();
    makeWorkDirectory("restore-from-backup");
    createBackup("foo");

    assertFalse(underTest.maybeRestore(dataStoreConfiguration));

    // check no file was unzipped
    File dbDir = directories.getWorkDirectory("db");
    assertThat(dbDir.listFiles(), arrayWithSize(1));
  }

  private File makeWorkDirectory(final String path) throws IOException {
    File dir = directories.getWorkDirectory(path);
    dir.mkdirs();
    return dir;
  }

  private void createBackup(final String name) throws FileNotFoundException, IOException {
    File restoreDirectory = makeWorkDirectory("restore-from-backup");
    restoreDirectory.mkdirs();
    File zip = new File(restoreDirectory, name);
    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
      ZipEntry entry = new ZipEntry(name.concat(".mv.db"));
      out.putNextEntry(entry);
      out.write(name.getBytes());
      out.closeEntry();
    }
  }
}
