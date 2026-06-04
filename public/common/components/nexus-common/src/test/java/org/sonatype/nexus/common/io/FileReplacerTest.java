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
package org.sonatype.nexus.common.io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.sonatype.nexus.common.io.FileReplacer.ContentWriter;

import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.hamcrest.io.FileMatchers.anExistingFile;

/**
 * Tests for {@link FileReplacer}.
 */
public class FileReplacerTest
{
  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  private FileReplacer fileReplacer;

  @Before
  public void setUp() throws Exception {
    File testFile = testFolder.newFile("test.txt");
    Files.append("initial", testFile, Charset.forName("UTF-8"));
    assertThat(readFirstLine(testFile), is("initial"));

    fileReplacer = new FileReplacer(testFile);
    fileReplacer.setDeleteBackupFile(true);

  }

  private String readFirstLine(final File file) throws IOException {
    return Files.readFirstLine(file, Charset.forName("UTF-8"));
  }

  private void assertFileCount(final File dir, final int size) {
    File[] files = dir.listFiles();
    assertThat(files, notNullValue());
    assertThat(files.length, is(size));
  }

  @Test
  public void writeWithSuccessReplacesFile() throws Exception {
    fileReplacer.replace(new ContentWriter()
    {
      @Override
      public void write(final BufferedOutputStream output) throws IOException {
        output.write("hello".getBytes());
      }
    });

    // target file should exist and have the correct content
    assertThat(fileReplacer.getFile(), anExistingFile());
    assertThat(readFirstLine(fileReplacer.getFile()), is("hello"));

    // tmp and backup files should not exist
    assertThat(fileReplacer.getTempFile(), not(anExistingFile()));
    assertThat(fileReplacer.getBackupFile(), not(anExistingFile()));

    // sanity assert we are not leaking files
    assertFileCount(testFolder.getRoot(), 1);
  }

  @Test
  public void writeWithSuccessReplacesFileWithBackup() throws Exception {
    fileReplacer.setDeleteBackupFile(false);

    fileReplacer.replace(new ContentWriter()
    {
      @Override
      public void write(final BufferedOutputStream output) throws IOException {
        output.write("hello".getBytes());
      }
    });

    // target file should exist and have the correct content
    assertThat(fileReplacer.getFile(), anExistingFile());
    assertThat(readFirstLine(fileReplacer.getFile()), is("hello"));

    // tmp file should not exist
    assertThat(fileReplacer.getTempFile(), not(anExistingFile()));

    // backup file should exist with previous content
    assertThat(fileReplacer.getBackupFile(), anExistingFile());
    assertThat(readFirstLine(fileReplacer.getBackupFile()), is("initial"));

    // sanity assert we are not leaking files
    assertFileCount(testFolder.getRoot(), 2);
  }

  @Test
  public void writeWithErrorLeavesFile() throws Exception {
    try {
      fileReplacer.replace(new ContentWriter()
      {
        @Override
        public void write(final BufferedOutputStream output) throws IOException {
          output.write("oops".getBytes());
          output.flush();
          throw new IOException("test failure");
        }
      });

      fail("replace() should have propagated exception");
    }
    catch (IOException e) {
      // expected
    }

    // target file should exist but unchanged
    assertThat(fileReplacer.getFile(), anExistingFile());
    assertThat(readFirstLine(fileReplacer.getFile()), is("initial"));

    // tmp file should exist with the partial written content
    assertThat(fileReplacer.getTempFile(), anExistingFile());
    assertThat(readFirstLine(fileReplacer.getTempFile()), is("oops"));

    // backup file should not exit
    assertThat(fileReplacer.getBackupFile(), not(anExistingFile()));

    // sanity assert we are not leaking files
    assertFileCount(testFolder.getRoot(), 2);
  }
}
