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

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Priority;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * UT for {@link GeneratedContentSourceSupport}.
 */
public class GeneratedContentSourceSupportTest
{
  private static final byte[] CONTENT = "generated-content".getBytes(StandardCharsets.UTF_8);

  @Test
  public void constructorWithoutPriorityUsesDefaultPriority() {
    TestGeneratedContentSource source = new TestGeneratedContentSource(Type.CONFIG, "some/path");

    assertThat(source.getType(), is(Type.CONFIG));
    assertThat(source.getPath(), is("some/path"));
    assertThat(source.getPriority(), is(Priority.DEFAULT));
  }

  @Test
  public void constructorWithPriorityUsesProvidedPriority() {
    TestGeneratedContentSource source = new TestGeneratedContentSource(Type.CONFIG, "some/path", Priority.HIGH);

    assertThat(source.getType(), is(Type.CONFIG));
    assertThat(source.getPath(), is("some/path"));
    assertThat(source.getPriority(), is(Priority.HIGH));
  }

  @Test
  public void prepareCreatesFileAndInvokesGenerate() throws Exception {
    TestGeneratedContentSource source = new TestGeneratedContentSource(Type.CONFIG, "some/path");
    try {
      source.prepare();

      assertThat(source.generateCount, is(1));
      assertThat(source.getFile(), is(notNullValue()));
      assertTrue(source.getFile().exists());
      assertThat(source.getFile().getName(), startsWith("some-path-"));
      assertThat(source.getFile().getName(), endsWith(".tmp"));
      assertThat(source.getSize(), is((long) CONTENT.length));
    }
    finally {
      source.cleanup();
    }
  }

  @Test
  public void prepareFailsOnSecondInvocation() throws Exception {
    TestGeneratedContentSource source = new TestGeneratedContentSource(Type.CONFIG, "some/path");
    try {
      source.prepare();

      try {
        source.prepare();
        fail("Expected IllegalStateException on second prepare()");
      }
      catch (IllegalStateException e) {
        // expected: checkState(file == null) fails because file is already set
      }

      // generate was invoked only once; second prepare bailed before calling it
      assertThat(source.generateCount, is(1));
    }
    finally {
      source.cleanup();
    }
  }

  @Test
  public void getSizeReturnsGeneratedByteCount() throws Exception {
    TestGeneratedContentSource source = new TestGeneratedContentSource(Type.CONFIG, "some/path");
    try {
      source.prepare();

      assertThat(source.getSize(), is((long) CONTENT.length));
    }
    finally {
      source.cleanup();
    }
  }

  @Test
  public void getSizeFailsWhenFileMissing() throws Exception {
    TestGeneratedContentSource source = new TestGeneratedContentSource(Type.CONFIG, "some/path");
    source.prepare();
    // delete the underlying file but leave the field reference intact
    Files.delete(source.getFile().toPath());

    try {
      source.getSize();
      fail("Expected IllegalStateException when backing file does not exist");
    }
    catch (IllegalStateException e) {
      // expected: checkState(file.exists()) fails
    }
  }

  @Test
  public void getContentReturnsGeneratedBytes() throws Exception {
    TestGeneratedContentSource source = new TestGeneratedContentSource(Type.CONFIG, "some/path");
    try {
      source.prepare();

      try (InputStream in = source.getContent()) {
        assertThat(in, is(notNullValue()));
        assertArrayEquals(CONTENT, in.readAllBytes());
      }
    }
    finally {
      source.cleanup();
    }
  }

  @Test
  public void getContentFailsWhenFileMissing() throws Exception {
    TestGeneratedContentSource source = new TestGeneratedContentSource(Type.CONFIG, "some/path");
    source.prepare();
    // delete the underlying file but leave the field reference intact
    Files.delete(source.getFile().toPath());

    try {
      source.getContent();
      fail("Expected IllegalStateException when backing file does not exist");
    }
    catch (IllegalStateException e) {
      // expected: checkState(file.exists()) fails
    }
  }

  @Test
  public void cleanupDeletesFileAndIsIdempotent() throws Exception {
    TestGeneratedContentSource source = new TestGeneratedContentSource(Type.CONFIG, "some/path");
    source.prepare();

    File file = source.getFile();
    assertTrue(file.exists());

    // file != null branch: deletes the file and clears the reference
    source.cleanup();
    assertFalse(file.exists());
    assertThat(source.getFile(), is(nullValue()));

    // file == null branch: second cleanup is a no-op
    source.cleanup();
    assertThat(source.getFile(), is(nullValue()));
  }

  @Test
  public void cleanupBeforePrepareIsNoOp() throws Exception {
    TestGeneratedContentSource source = new TestGeneratedContentSource(Type.CONFIG, "some/path");

    // file == null branch: cleanup does nothing when nothing was prepared
    source.cleanup();
    assertThat(source.getFile(), is(nullValue()));
  }

  @Test
  public void prepareSucceedsAgainAfterCleanup() throws Exception {
    TestGeneratedContentSource source = new TestGeneratedContentSource(Type.CONFIG, "some/path");
    try {
      source.prepare();
      File first = source.getFile();

      // cleanup clears the file reference, so the source can be prepared again
      source.cleanup();
      assertFalse(first.exists());
      assertThat(source.getFile(), is(nullValue()));

      source.prepare();
      assertThat(source.generateCount, is(2));
      assertThat(source.getFile(), is(notNullValue()));
      assertTrue(source.getFile().exists());
    }
    finally {
      source.cleanup();
    }
  }

  @Test
  public void getContentCanBeReadMultipleTimes() throws Exception {
    TestGeneratedContentSource source = new TestGeneratedContentSource(Type.CONFIG, "some/path");
    try {
      source.prepare();

      // each call opens a fresh stream over the backing file
      try (InputStream in = source.getContent()) {
        assertArrayEquals(CONTENT, in.readAllBytes());
      }
      try (InputStream in = source.getContent()) {
        assertArrayEquals(CONTENT, in.readAllBytes());
      }
    }
    finally {
      source.cleanup();
    }
  }

  @Test
  public void getSizeAndContentForEmptyGeneratedFile() throws Exception {
    TestGeneratedContentSource source = new TestGeneratedContentSource(Type.CONFIG, "some/path");
    source.content = new byte[0];
    try {
      source.prepare();

      // empty generated content still produces an existing, zero-length file
      assertTrue(source.getFile().exists());
      assertThat(source.getSize(), is(0L));
      try (InputStream in = source.getContent()) {
        assertArrayEquals(new byte[0], in.readAllBytes());
      }
    }
    finally {
      source.cleanup();
    }
  }

  /**
   * Concrete {@link GeneratedContentSourceSupport} that writes known bytes when asked to generate content.
   */
  private static class TestGeneratedContentSource
      extends GeneratedContentSourceSupport
  {
    int generateCount;

    byte[] content = CONTENT;

    TestGeneratedContentSource(final Type type, final String path) {
      super(type, path);
    }

    TestGeneratedContentSource(final Type type, final String path, final Priority priority) {
      super(type, path, priority);
    }

    @Override
    protected void generate(final File file) throws Exception {
      generateCount++;
      Files.write(file.toPath(), content);
    }

    File getFile() {
      return file;
    }
  }
}
