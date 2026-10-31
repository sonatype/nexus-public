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
import java.util.List;

import org.sonatype.nexus.supportzip.ImportTaskData;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TaskRestorer}
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskRestorerTest
{
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock
  private RestoreHelper restoreHelper;

  @Test
  public void doStartRestoresAndDeletesExistingFile() throws Exception {
    File taskFile = tempFolder.newFile("task" + RestoreHelper.FILE_SUFFIX);
    when(restoreHelper.getDbPath()).thenReturn(tempFolder.getRoot().toPath());

    // QualifierUtil.buildQualifierBeanMap derives the bean key from the Mockito mock name via reflection,
    // so a mock named 'task' resolves to the file 'task.json'
    ImportTaskData importer = mock(ImportTaskData.class, "task");
    TaskRestorer underTest = new TaskRestorer(restoreHelper, List.of(importer));

    assertThat(taskFile.exists(), is(true));

    underTest.doStart();

    ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
    verify(importer).restore(fileCaptor.capture());
    File restoredFile = fileCaptor.getValue();
    assertThat(restoredFile, is(taskFile));
    assertThat(restoredFile.getName(), is("task" + RestoreHelper.FILE_SUFFIX));
    assertThat(taskFile.exists(), is(false));
  }

  @Test
  public void doStartRestoresOnlyImportersWhoseFilesExistUsingQualifierDerivedNames() throws Exception {
    // file name is derived from the importer's @Qualifier value (the mock name), not a fixed 'task.json'
    File presentFile = tempFolder.newFile("present" + RestoreHelper.FILE_SUFFIX);
    when(restoreHelper.getDbPath()).thenReturn(tempFolder.getRoot().toPath());

    ImportTaskData present = mock(ImportTaskData.class, "present");
    ImportTaskData missing = mock(ImportTaskData.class, "missing");
    TaskRestorer underTest = new TaskRestorer(restoreHelper, List.of(present, missing));

    assertThat(presentFile.exists(), is(true));

    underTest.doStart();

    ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
    verify(present).restore(fileCaptor.capture());
    File restoredFile = fileCaptor.getValue();
    assertThat(restoredFile, is(presentFile));
    assertThat(restoredFile.getName(), is("present" + RestoreHelper.FILE_SUFFIX));
    // file restored from an existing path is removed afterwards
    assertThat(presentFile.exists(), is(false));

    // the importer whose 'missing.json' does not exist is never restored
    verify(missing, never()).restore(any(File.class));
  }

  @Test
  public void doStartSkipsRestoreWhenFileMissing() throws Exception {
    when(restoreHelper.getDbPath()).thenReturn(tempFolder.getRoot().toPath());

    ImportTaskData importer = mock(ImportTaskData.class, "task");
    TaskRestorer underTest = new TaskRestorer(restoreHelper, List.of(importer));

    underTest.doStart();

    verify(importer, never()).restore(any(File.class));
  }

  @Test
  public void doStartWithNoImportersOnlyResolvesDbPath() throws Exception {
    when(restoreHelper.getDbPath()).thenReturn(tempFolder.getRoot().toPath());

    TaskRestorer underTest = new TaskRestorer(restoreHelper, List.of());

    underTest.doStart();

    verify(restoreHelper).getDbPath();
  }

  @Test
  public void testDoesNotDeleteFileWhenRestoreFails() throws Exception {
    File taskFile = tempFolder.newFile("task" + RestoreHelper.FILE_SUFFIX);
    when(restoreHelper.getDbPath()).thenReturn(tempFolder.getRoot().toPath());

    // QualifierUtil.buildQualifierBeanMap derives the bean key from the Mockito mock name via reflection,
    // so a mock named 'task' resolves to the file 'task.json'
    ImportTaskData importer = mock(ImportTaskData.class, "task");
    TaskRestorer underTest = new TaskRestorer(restoreHelper, List.of(importer));

    assertThat(taskFile.exists(), is(true));
    doThrow(new IOException("restore failed")).when(importer).restore(any(File.class));

    // the IOException from restore propagates out of doStart
    IOException thrown = assertThrows(IOException.class, underTest::doStart);
    assertThat(thrown.getMessage(), is("restore failed"));

    verify(importer).restore(any(File.class));
    // deleteQuietly only runs after a successful restore, so the file is left in place
    assertThat(taskFile.exists(), is(true));
  }

  @Test
  public void constructorRejectsNullRestoreHelper() {
    assertThrows(NullPointerException.class, () -> new TaskRestorer(null, List.of()));
  }

  @Test
  public void constructorRejectsNullImporterList() {
    assertThrows(NullPointerException.class, () -> new TaskRestorer(restoreHelper, null));
  }
}
