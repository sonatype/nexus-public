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
package org.sonatype.nexus.bootstrap.entrypoint.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.io.FileMatchers.anExistingDirectory;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DirectoryHelperTest
{
  @Rule
  public TemporaryFolder temporaryFolder = TemporaryFolder.builder().assureDeletion().build();

  private DirectoryHelper underTest;

  @Before
  public void setUp() {
    underTest = new DirectoryHelper();
  }

  // ===================== mkdir(Path) tests =====================

  @Test
  public void mkdir_createsDirectory() throws IOException {
    Path dir = temporaryFolder.getRoot().toPath().resolve("new-dir");
    underTest.mkdir(dir);
    assertTrue(Files.isDirectory(dir));
  }

  @Test
  public void mkdir_createsParentDirectories() throws IOException {
    Path dir = temporaryFolder.getRoot().toPath().resolve("parent/child/grandchild");
    underTest.mkdir(dir);
    assertTrue(Files.isDirectory(dir));
  }

  @Test
  public void mkdir_existingDirectorySucceeds() throws IOException {
    Path dir = temporaryFolder.newFolder("existing").toPath();
    underTest.mkdir(dir);
    assertTrue(Files.isDirectory(dir));
  }

  @Test
  public void mkdir_symlinkToDirectorySucceeds() throws IOException {
    Path target = temporaryFolder.newFolder("target").toPath();
    Path link = temporaryFolder.getRoot().toPath().resolve("link");
    Files.createSymbolicLink(link, target);
    underTest.mkdir(link);
    assertTrue(Files.isDirectory(link));
  }

  // ===================== mkdir(File) tests =====================

  @Test
  public void mkdir_file_createsDirectory() throws IOException {
    File dir = new File(temporaryFolder.getRoot(), "file-dir");
    underTest.mkdir(dir);
    assertTrue(dir.isDirectory());
  }

  // ===================== mkdir(File, String) tests =====================

  @Test
  public void mkdir_parentChild_createsChildDirectory() throws IOException {
    File parent = temporaryFolder.getRoot();
    File child = underTest.mkdir(parent, "child-dir");
    assertTrue(child.isDirectory());
    assertThat(child.getParentFile(), equalTo(parent));
  }

  @Test
  public void mkdir_parentChild_returnsChildFile() throws IOException {
    File parent = temporaryFolder.getRoot();
    File child = underTest.mkdir(parent, "child-dir");
    assertThat(child.getName(), equalTo("child-dir"));
  }

  // ===================== clean(Path) tests =====================

  @Test
  public void clean_removesFilesButKeepsDirectories() throws IOException {
    Path dir = temporaryFolder.newFolder("clean-test").toPath();
    Files.createFile(dir.resolve("file1.txt"));
    Files.createFile(dir.resolve("file2.txt"));
    Files.createDirectories(dir.resolve("subdir"));
    Files.createFile(dir.resolve("subdir/file3.txt"));

    underTest.clean(dir);

    assertTrue(Files.isDirectory(dir));
    assertTrue(Files.isDirectory(dir.resolve("subdir")));
    assertFalse(Files.exists(dir.resolve("file1.txt")));
    assertFalse(Files.exists(dir.resolve("file2.txt")));
    assertFalse(Files.exists(dir.resolve("subdir/file3.txt")));
  }

  @Test
  public void clean_throwsOnNonExistentPath() {
    Path nonExistent = temporaryFolder.getRoot().toPath().resolve("does-not-exist");
    assertThrows(IllegalArgumentException.class, () -> underTest.clean(nonExistent));
  }

  @Test
  public void clean_throwsOnFile() throws IOException {
    Path file = temporaryFolder.newFile("not-a-dir.txt").toPath();
    assertThrows(IllegalArgumentException.class, () -> underTest.clean(file));
  }

  // ===================== cleanIfExists tests =====================

  @Test
  public void cleanIfExists_returnsTrueWhenPathExists() throws IOException {
    Path dir = temporaryFolder.newFolder("clean-if-exists").toPath();
    Files.createFile(dir.resolve("file.txt"));
    boolean result = underTest.cleanIfExists(dir);
    assertTrue(result);
    assertFalse(Files.exists(dir.resolve("file.txt")));
  }

  @Test
  public void cleanIfExists_returnsFalseWhenPathDoesNotExist() throws IOException {
    Path nonExistent = temporaryFolder.getRoot().toPath().resolve("does-not-exist");
    boolean result = underTest.cleanIfExists(nonExistent);
    assertFalse(result);
  }

  // ===================== empty tests =====================

  @Test
  public void empty_removesAllContentsButKeepsDirectory() throws IOException {
    Path dir = temporaryFolder.newFolder("empty-test").toPath();
    Files.createFile(dir.resolve("file1.txt"));
    Files.createDirectories(dir.resolve("subdir"));
    Files.createFile(dir.resolve("subdir/file2.txt"));

    underTest.empty(dir);

    assertTrue(Files.isDirectory(dir));
    assertFalse(Files.exists(dir.resolve("file1.txt")));
    assertFalse(Files.exists(dir.resolve("subdir")));
  }

  @Test
  public void empty_throwsOnNonExistentPath() {
    Path nonExistent = temporaryFolder.getRoot().toPath().resolve("does-not-exist");
    assertThrows(IllegalArgumentException.class, () -> underTest.empty(nonExistent));
  }

  // ===================== emptyIfExists tests =====================

  @Test
  public void emptyIfExists_returnsTrueWhenPathExists() throws IOException {
    Path dir = temporaryFolder.newFolder("empty-if-exists").toPath();
    Files.createFile(dir.resolve("file.txt"));
    boolean result = underTest.emptyIfExists(dir);
    assertTrue(result);
    assertFalse(Files.exists(dir.resolve("file.txt")));
  }

  @Test
  public void emptyIfExists_returnsFalseWhenPathDoesNotExist() throws IOException {
    Path nonExistent = temporaryFolder.getRoot().toPath().resolve("does-not-exist");
    boolean result = underTest.emptyIfExists(nonExistent);
    assertFalse(result);
  }

  // ===================== delete tests =====================

  @Test
  public void delete_removesDirectoryRecursively() throws IOException {
    Path dir = temporaryFolder.newFolder("delete-test").toPath();
    Files.createFile(dir.resolve("file.txt"));
    Files.createDirectories(dir.resolve("subdir"));
    Files.createFile(dir.resolve("subdir/nested.txt"));

    underTest.delete(dir);

    assertFalse(Files.exists(dir));
  }

  @Test
  public void delete_removesFile() throws IOException {
    Path file = temporaryFolder.newFile("delete-file.txt").toPath();
    underTest.delete(file);
    assertFalse(Files.exists(file));
  }

  @Test
  public void delete_withExcludeFilter_skipsExcluded() throws IOException {
    Path dir = temporaryFolder.newFolder("delete-filter-test").toPath();
    Path excludedDir = Files.createDirectories(dir.resolve("excluded"));
    Files.createFile(excludedDir.resolve("file.txt"));
    Files.createFile(dir.resolve("other.txt"));

    underTest.delete(dir, path -> path.getFileName().toString().equals("excluded"));

    assertFalse(Files.exists(dir.resolve("other.txt")));
    assertTrue(Files.exists(excludedDir.resolve("file.txt")));
  }

  @Test
  public void delete_throwsOnNonExistentPath() {
    Path nonExistent = temporaryFolder.getRoot().toPath().resolve("does-not-exist");
    assertThrows(IllegalArgumentException.class, () -> underTest.delete(nonExistent));
  }

  // ===================== deleteIfExists tests =====================

  @Test
  public void deleteIfExists_returnsTrueWhenPathExists() throws IOException {
    Path dir = temporaryFolder.newFolder("delete-if-exists").toPath();
    boolean result = underTest.deleteIfExists(dir);
    assertTrue(result);
    assertFalse(Files.exists(dir));
  }

  @Test
  public void deleteIfExists_returnsFalseWhenPathDoesNotExist() throws IOException {
    Path nonExistent = temporaryFolder.getRoot().toPath().resolve("does-not-exist");
    boolean result = underTest.deleteIfExists(nonExistent);
    assertFalse(result);
  }

  // ===================== copy tests =====================

  @Test
  public void copy_copiesDirectoryRecursively() throws IOException {
    Path from = temporaryFolder.newFolder("from").toPath();
    Path to = temporaryFolder.getRoot().toPath().resolve("to");
    Files.createFile(from.resolve("file.txt"));
    Files.createDirectories(from.resolve("subdir"));
    Files.createFile(from.resolve("subdir/nested.txt"));

    underTest.copy(from, to);

    assertTrue(Files.exists(to.resolve("file.txt")));
    assertTrue(Files.exists(to.resolve("subdir/nested.txt")));
    assertTrue(Files.exists(from.resolve("file.txt"))); // original still exists
  }

  @Test
  public void copy_copiesSingleFile() throws IOException {
    Path from = temporaryFolder.newFile("source.txt").toPath();
    Path to = temporaryFolder.getRoot().toPath().resolve("dest.txt");
    Files.writeString(from, "content");

    underTest.copy(from, to);

    assertThat(Files.readString(to), equalTo("content"));
  }

  @Test
  public void copy_withExcludeFilter_skipsExcluded() throws IOException {
    Path from = temporaryFolder.newFolder("from-filter").toPath();
    Path to = temporaryFolder.getRoot().toPath().resolve("to-filter");
    Files.createDirectories(from.resolve("excluded"));
    Files.createFile(from.resolve("excluded/file.txt"));
    Files.createFile(from.resolve("other.txt"));

    underTest.copy(from, to, path -> path.getFileName().toString().equals("excluded"));

    assertTrue(Files.exists(to.resolve("other.txt")));
    assertFalse(Files.exists(to.resolve("excluded")));
  }

  @Test
  public void copy_throwsWhenDestinationIsSubdirectoryWithoutFilter() throws IOException {
    Path from = temporaryFolder.newFolder("from-nested").toPath();
    Path to = from.resolve("subdir");
    assertThrows(IllegalArgumentException.class, () -> underTest.copy(from, to));
  }

  @Test
  public void copy_allowsDestinationInsideSourceWithExcludeFilter() throws IOException {
    Path from = temporaryFolder.newFolder("from-nested-filter").toPath();
    Files.createFile(from.resolve("file.txt"));
    Path to = from.resolve("subdir");
    // Exclude the destination directory to avoid recursive copy
    underTest.copy(from, to, path -> path.getFileName() != null && path.getFileName().toString().equals("subdir"));
    assertThat(to.resolve("file.txt").toFile(), anExistingFile());
  }

  @Test
  public void copy_throwsOnNonExistentSource() {
    Path nonExistent = temporaryFolder.getRoot().toPath().resolve("does-not-exist");
    Path to = temporaryFolder.getRoot().toPath().resolve("to");
    assertThrows(IllegalArgumentException.class, () -> underTest.copy(nonExistent, to));
  }

  // ===================== move tests =====================

  @Test
  public void move_movesDirectory() throws IOException {
    Path from = temporaryFolder.newFolder("move-from").toPath();
    Path to = temporaryFolder.getRoot().toPath().resolve("move-to");
    Files.createFile(from.resolve("file.txt"));

    underTest.move(from, to);

    assertTrue(Files.exists(to.resolve("file.txt")));
    assertFalse(Files.exists(from));
  }

  @Test
  public void move_movesFile() throws IOException {
    Path from = temporaryFolder.newFile("move-file.txt").toPath();
    Files.writeString(from, "content");
    Path to = temporaryFolder.getRoot().toPath().resolve("moved.txt");

    underTest.move(from, to);

    assertThat(Files.readString(to), equalTo("content"));
    assertFalse(Files.exists(from));
  }

  // ===================== copyDeleteMove tests =====================

  @Test
  public void copyDeleteMove_movesDirectory() throws IOException {
    Path from = temporaryFolder.newFolder("cdm-from").toPath();
    Path to = temporaryFolder.getRoot().toPath().resolve("cdm-to");
    Files.createFile(from.resolve("file.txt"));

    underTest.copyDeleteMove(from, to, null);

    assertTrue(Files.exists(to.resolve("file.txt")));
    assertFalse(Files.exists(from));
  }

  // ===================== apply tests =====================

  @Test
  public void apply_appliesFunctionToAllFiles() throws IOException {
    Path dir = temporaryFolder.newFolder("apply-test").toPath();
    Files.createFile(dir.resolve("file1.txt"));
    Files.createFile(dir.resolve("file2.txt"));

    int[] count = {0};
    underTest.apply(dir, path -> {
      if (Files.isRegularFile(path)) {
        count[0]++;
      }
      return java.nio.file.FileVisitResult.CONTINUE;
    });

    assertThat(count[0], equalTo(2));
  }

  @Test
  public void apply_throwsOnNonExistentPath() {
    Path nonExistent = temporaryFolder.getRoot().toPath().resolve("does-not-exist");
    assertThrows(IllegalArgumentException.class,
        () -> underTest.apply(nonExistent, path -> java.nio.file.FileVisitResult.CONTINUE));
  }

  // ===================== deleteIfEmptyRecursively tests =====================

  @Test
  public void deleteIfEmptyRecursively_removesEmptyDirectories() throws IOException {
    Path dir = temporaryFolder.newFolder("empty-dirs").toPath();
    Files.createDirectories(dir.resolve("empty1"));
    Files.createDirectories(dir.resolve("empty2"));
    Files.createFile(dir.resolve("file.txt"));

    int count = underTest.deleteIfEmptyRecursively(dir, null);

    assertThat(count, greaterThan(0));
    assertTrue(Files.exists(dir.resolve("file.txt")));
  }

  @Test
  public void deleteIfEmptyRecursively_respectsTimestamp() throws IOException {
    long timestamp = Instant.now().toEpochMilli();

    Path dir = temporaryFolder.newFolder("timestamp-test").toPath();
    Path oldDir = Files.createDirectories(dir.resolve("old"));
    Path notOldDir = Files.createDirectories(dir.resolve("not-old"));
    Files.setLastModifiedTime(oldDir, FileTime.fromMillis(timestamp - 3600));
    Files.setLastModifiedTime(notOldDir, FileTime.fromMillis(timestamp + 3600));

    int count = underTest.deleteIfEmptyRecursively(dir, timestamp);

    assertThat(count, is(1));
    assertThat(oldDir.toFile(), not(anExistingDirectory()));
    assertThat(notOldDir.toFile(), anExistingDirectory());
  }

  @Test
  public void deleteIfEmptyRecursively_returnsZeroForNonExistentPath() throws IOException {
    Path nonExistent = temporaryFolder.getRoot().toPath().resolve("does-not-exist");
    int count = underTest.deleteIfEmptyRecursively(nonExistent, null);
    assertThat(count, equalTo(0));
  }

  @Test
  public void deleteIfEmptyRecursively_keepsNonEmptyDirectories() throws IOException {
    Path dir = temporaryFolder.newFolder("non-empty-test").toPath();
    Path subDir = Files.createDirectories(dir.resolve("notEmpty"));
    Files.createFile(subDir.resolve("file.txt"));

    int count = underTest.deleteIfEmptyRecursively(dir, null);

    assertThat(count, equalTo(0));
    assertTrue(Files.exists(subDir));
  }

  @Test
  public void deleteIfEmptyRecursively_neverDeletesRootDirectory() throws IOException {
    // Create an empty directory with old timestamp
    Path dir = temporaryFolder.newFolder("root-test").toPath();
    // Set the directory's timestamp to 1 hour ago so it would qualify for deletion by timestamp
    Files.setLastModifiedTime(dir, FileTime.from(Instant.now().minusSeconds(3600)));

    // Use a timestamp before which directories should be deleted
    long timestamp = Instant.now().toEpochMilli();
    int count = underTest.deleteIfEmptyRecursively(dir, timestamp);

    // The root directory is explicitly protected - it should never be deleted even if empty/old
    assertThat(dir.toFile(), anExistingDirectory());
    assertThat(count, equalTo(0));
  }

  // ===================== getTemporaryDirectory tests =====================

  @Test
  public void getTemporaryDirectory_returnsValidDirectory() throws IOException {
    File tempDir = underTest.getTemporaryDirectory();
    assertThat(tempDir, notNullValue());
    assertTrue(tempDir.isDirectory());
  }

  @Test
  public void getTemporaryDirectory_isWritable() throws IOException {
    File tempDir = underTest.getTemporaryDirectory();
    File testFile = new File(tempDir, "test-write-" + System.currentTimeMillis());
    assertTrue(testFile.createNewFile());
    assertTrue(testFile.delete());
  }
}
