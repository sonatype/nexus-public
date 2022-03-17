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
package org.sonatype.nexus.util.file;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import org.sonatype.sisu.goodies.testsupport.TestSupport;
import org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers;

import com.google.common.base.Charsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.core.IsEqual.equalTo;

public class FileSupportTest
    extends TestSupport
{
  private static final byte[] PAYLOAD = "payload".getBytes(Charset.forName("UTF-8"));

  private File root;

  private void createFile(final Path r) throws IOException {
    Files.write(r.resolve("file1.txt"), PAYLOAD);
  }

  @Before
  public void prepare() throws IOException {
    root = util.createTempDir();
    createFile(root.toPath());
  }

  @After
  public void cleanup() throws IOException {
    Files.walkFileTree(root.toPath(), new SimpleFileVisitor<Path>()
    {
      @Override
      public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  @Test
  public void copy() throws IOException {
    final String baz = "BAZ";
    final File target = new File(root, "baz.txt");
    FileSupport.copy(new ByteArrayInputStream(baz.getBytes(Charsets.UTF_8)), target.toPath());
    assertThat(target, FileMatchers.isFile());
    assertThat(target, FileMatchers.containsOnly(baz));
  }

  @Test
  public void copyAndOverwrite() throws IOException {
    final String baz1 = "BAZ1";
    final String baz2 = "BAZ2";
    final File target = new File(root, "baz.txt");
    FileSupport.copy(new ByteArrayInputStream(baz1.getBytes(Charsets.UTF_8)), target.toPath());
    FileSupport.copy(new ByteArrayInputStream(baz2.getBytes(Charsets.UTF_8)), target.toPath());
    assertThat(target, FileMatchers.isFile());
    assertThat(target, FileMatchers.containsOnly(baz2));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void copyMisconfigured() throws IOException {
    final String baz1 = "BAZ1";
    final File target = new File(root, "baz.txt");
    FileSupport
        .copy(new ByteArrayInputStream(baz1.getBytes(Charsets.UTF_8)), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void readFileNonExistent() throws IOException {
    final File target = new File(root, "baz.txt");
    FileSupport.readFile(target.toPath());
  }

  @Test
  public void readFileWriteFileRoundtrip() throws IOException {
    final String baz1 = "BAZ1";
    final String baz2 = "BAZ2";
    final String baz3 = "BAZ3";
    final File target = new File(root, "baz.txt");
    FileSupport.writeFile(target.toPath(), baz1);
    final String readBaz1 = FileSupport.readFile(target.toPath());
    FileSupport.writeFile(target.toPath(), baz2);
    final String readBaz2 = FileSupport.readFile(target.toPath());
    FileSupport.writeFile(target.toPath(), baz3);
    final String readBaz3 = FileSupport.readFile(target.toPath());

    assertThat(readBaz1, equalTo(baz1));
    assertThat(readBaz2, equalTo(baz2));
    assertThat(readBaz3, equalTo(baz3));
  }

  @Test
  public void readFileWriteFileRoundtripMultiline() throws IOException {
    final String baz1 = "first\nsecond\nthird";
    final String baz2 = "erste\nzweite\ndritte";
    final String baz3 = "első\nmásodik\nharmadik";
    final File target = new File(root, "baz.txt");
    FileSupport.writeFile(target.toPath(), baz1);
    final String readBaz1 = FileSupport.readFile(target.toPath());
    FileSupport.writeFile(target.toPath(), baz2);
    final String readBaz2 = FileSupport.readFile(target.toPath());
    FileSupport.writeFile(target.toPath(), baz3);
    final String readBaz3 = FileSupport.readFile(target.toPath());

    assertThat(readBaz1, equalTo(baz1));
    assertThat(readBaz2, equalTo(baz2));
    assertThat(readBaz3, equalTo(baz3));
  }
}
