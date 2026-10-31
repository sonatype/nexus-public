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
package org.sonatype.nexus.supportzip.datastore;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.sonatype.nexus.supportzip.ImportData;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SupportRestorer}
 */
public class SupportRestorerTest
{
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private RestoreHelper restoreHelper;

  @Before
  public void setUp() {
    restoreHelper = mock(RestoreHelper.class);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRequiresRestoreHelper() {
    new SupportRestorer(null, Collections.emptyList());
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRequiresImportDataList() {
    new SupportRestorer(restoreHelper, null);
  }

  @Test
  public void testRestoresWhenFileExists() throws Exception {
    // QualifierUtil.buildQualifierBeanMap derives the bean key from the Mockito mock name via reflection,
    // so a mock named 'config' resolves to the file 'config.json'
    ImportData importer = mock(ImportData.class, "config");
    File configFile = folder.newFile("config" + RestoreHelper.FILE_SUFFIX);
    assertTrue(configFile.exists());

    when(restoreHelper.getDbPath()).thenReturn(folder.getRoot().toPath());

    SupportRestorer restorer = new SupportRestorer(restoreHelper, Collections.singletonList(importer));
    restorer.doStart();

    ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
    verify(importer).restore(captor.capture());
    File restored = captor.getValue();
    // the resolved file must be exactly the 'config.json' inside the temp db dir, not merely a file of that name
    assertThat(restored, is(configFile));
    assertThat(restored.getName(), is("config.json"));
    assertThat(restored.getParentFile(), is(folder.getRoot()));

    // the file is deleted after a successful restore
    assertFalse(restored.exists());
    assertFalse(configFile.exists());
  }

  @Test
  public void testDoesNotRestoreWhenFileDoesNotExist() throws Exception {
    // 'missing.json' is never created in the temp dir, so restore must not be invoked
    ImportData importer = mock(ImportData.class, "missing");

    when(restoreHelper.getDbPath()).thenReturn(folder.getRoot().toPath());

    SupportRestorer restorer = new SupportRestorer(restoreHelper, Collections.singletonList(importer));
    restorer.doStart();

    verify(importer, never()).restore(any(File.class));
    // maybeRestore still ran (it resolved the db path) but skipped the missing-file branch
    verify(restoreHelper).getDbPath();
  }

  @Test
  public void testDoStartWithNoImportersDoesNothing() throws Exception {
    when(restoreHelper.getDbPath()).thenReturn(folder.getRoot().toPath());

    SupportRestorer restorer = new SupportRestorer(restoreHelper, Collections.<ImportData>emptyList());
    restorer.doStart();

    verify(restoreHelper).getDbPath();
  }

  @Test
  public void testRestoresOnlyImportersWithExistingFiles() throws Exception {
    ImportData present = mock(ImportData.class, "present");
    ImportData absent = mock(ImportData.class, "absent");
    File presentFile = folder.newFile("present" + RestoreHelper.FILE_SUFFIX);
    assertTrue(presentFile.exists());

    when(restoreHelper.getDbPath()).thenReturn(folder.getRoot().toPath());

    List<ImportData> importers = Arrays.asList(present, absent);
    SupportRestorer restorer = new SupportRestorer(restoreHelper, importers);
    restorer.doStart();

    ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
    verify(present).restore(captor.capture());
    File restored = captor.getValue();
    assertThat(restored, is(presentFile));
    assertThat(restored.getName(), is("present" + RestoreHelper.FILE_SUFFIX));
    assertThat(restored.getParentFile(), is(folder.getRoot()));
    verify(absent, never()).restore(any(File.class));
    assertFalse(presentFile.exists());
  }

  @Test
  public void testDoesNotDeleteFileWhenRestoreFails() throws Exception {
    // QualifierUtil.buildQualifierBeanMap derives the bean key from the Mockito mock name via reflection,
    // so a mock named 'config' resolves to the file 'config.json'
    ImportData importer = mock(ImportData.class, "config");
    File configFile = folder.newFile("config" + RestoreHelper.FILE_SUFFIX);
    assertTrue(configFile.exists());

    when(restoreHelper.getDbPath()).thenReturn(folder.getRoot().toPath());
    doThrow(new IOException("restore failed")).when(importer).restore(any(File.class));

    SupportRestorer restorer = new SupportRestorer(restoreHelper, Collections.singletonList(importer));

    // the IOException from restore propagates out of doStart
    IOException thrown = assertThrows(IOException.class, restorer::doStart);
    assertThat(thrown.getMessage(), is("restore failed"));

    verify(importer).restore(any(File.class));
    // deleteQuietly only runs after a successful restore, so the file is left in place
    assertTrue(configFile.exists());
  }
}
