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
package org.sonatype.nexus.supportzip;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * UT for {@link FileContentSourceSupport}.
 */
public class FileContentSourceSupportTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static final String CONTENT = "hello support zip\n";

  @Test
  public void testFullConstructorRetainsTypePathAndPriority() throws Exception {
    File file = temporaryFolder.newFile("config.txt");
    FileContentSourceSupport source = new FileContentSourceSupport(Type.CONFIG, "some/path", file, Priority.HIGH);

    assertThat(source.getType(), is(Type.CONFIG));
    assertThat(source.getPath(), is("some/path"));
    assertThat(source.getPriority(), is(Priority.HIGH));
  }

  @Test
  public void testShortConstructorDefaultsToDefaultPriority() throws Exception {
    File file = temporaryFolder.newFile("log.txt");
    FileContentSourceSupport source = new FileContentSourceSupport(Type.LOG, "another/path", file);

    assertThat(source.getType(), is(Type.LOG));
    assertThat(source.getPath(), is("another/path"));
    assertThat(source.getPriority(), is(Priority.DEFAULT));
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullFile() {
    new FileContentSourceSupport(Type.CONFIG, "some/path", null);
  }

  @Test
  public void testPrepareWithExistingFileSucceeds() throws Exception {
    File file = temporaryFolder.newFile("present.txt");
    FileContentSourceSupport source = new FileContentSourceSupport(Type.CONFIG, "some/path", file);

    // should not throw
    source.prepare();
  }

  @Test(expected = IllegalStateException.class)
  public void testPrepareWithMissingFileThrows() throws Exception {
    File missing = new File(temporaryFolder.getRoot(), "missing.txt");
    FileContentSourceSupport source = new FileContentSourceSupport(Type.CONFIG, "some/path", missing);

    source.prepare();
  }

  @Test
  public void testGetSizeReturnsFileLength() throws Exception {
    File file = temporaryFolder.newFile("sized.txt");
    byte[] bytes = CONTENT.getBytes(StandardCharsets.UTF_8);
    Files.write(file.toPath(), bytes);

    FileContentSourceSupport source = new FileContentSourceSupport(Type.CONFIG, "some/path", file);

    assertThat(source.getSize(), is((long) bytes.length));
  }

  @Test(expected = IllegalStateException.class)
  public void testGetSizeWithMissingFileThrows() {
    File missing = new File(temporaryFolder.getRoot(), "missing.txt");
    FileContentSourceSupport source = new FileContentSourceSupport(Type.CONFIG, "some/path", missing);

    source.getSize();
  }

  @Test
  public void testGetContentReturnsFileBytes() throws Exception {
    File file = temporaryFolder.newFile("content.txt");
    byte[] bytes = CONTENT.getBytes(StandardCharsets.UTF_8);
    Files.write(file.toPath(), bytes);

    FileContentSourceSupport source = new FileContentSourceSupport(Type.CONFIG, "some/path", file);

    try (InputStream in = source.getContent()) {
      assertThat(in, instanceOf(BufferedInputStream.class));
      byte[] read = in.readAllBytes();
      assertArrayEquals(bytes, read);
      assertThat(new String(read, StandardCharsets.UTF_8), is(CONTENT));
    }
  }

  @Test
  public void testGetContentWithMissingFileThrows() {
    File missing = new File(temporaryFolder.getRoot(), "missing.txt");
    FileContentSourceSupport source = new FileContentSourceSupport(Type.CONFIG, "some/path", missing);

    assertThrows(IllegalStateException.class, source::getContent);
  }

  @Test
  public void testCleanupIsNoOp() throws Exception {
    File file = temporaryFolder.newFile("cleanup.txt");
    byte[] bytes = CONTENT.getBytes(StandardCharsets.UTF_8);
    Files.write(file.toPath(), bytes);
    FileContentSourceSupport source = new FileContentSourceSupport(Type.CONFIG, "some/path", file);

    // should not throw and must not remove or alter the underlying file
    source.cleanup();

    assertTrue(file.exists());
    assertArrayEquals(bytes, Files.readAllBytes(file.toPath()));
  }

  @Test(expected = NullPointerException.class)
  public void testFullConstructorRejectsNullFile() {
    new FileContentSourceSupport(Type.CONFIG, "some/path", null, Priority.HIGH);
  }

  @Test(expected = NullPointerException.class)
  public void testFullConstructorRejectsNullPriority() throws Exception {
    File file = temporaryFolder.newFile("priority.txt");
    new FileContentSourceSupport(Type.CONFIG, "some/path", file, null);
  }

  @Test
  public void testConstructorNormalizesBackslashesInPath() throws Exception {
    File file = temporaryFolder.newFile("normalize.txt");
    FileContentSourceSupport source = new FileContentSourceSupport(Type.CONFIG, "some\\nested\\path", file);

    assertThat(source.getPath(), is("some/nested/path"));
  }

  @Test
  public void testGetSizeOfEmptyFileIsZero() throws Exception {
    File file = temporaryFolder.newFile("empty.txt");
    FileContentSourceSupport source = new FileContentSourceSupport(Type.CONFIG, "some/path", file);

    assertThat(source.getSize(), is(0L));
  }

  @Test
  public void testGetContentReturnsNewStreamForEachCall() throws Exception {
    File file = temporaryFolder.newFile("repeat.txt");
    Files.write(file.toPath(), CONTENT.getBytes(StandardCharsets.UTF_8));
    FileContentSourceSupport source = new FileContentSourceSupport(Type.CONFIG, "some/path", file);

    try (InputStream first = source.getContent(); InputStream second = source.getContent()) {
      assertNotSame(first, second);
    }
  }
}
