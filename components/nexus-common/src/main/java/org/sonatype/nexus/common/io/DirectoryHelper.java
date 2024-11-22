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

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Offers static helper methods for common file-system related operations for manipulating directories.
 *
 * Goal of this class is to utilize new Java7 NIO Files and related classes for better error detection.
 *
 * @since 2.7.0
 */
public final class DirectoryHelper
{
  private static final Logger log = LoggerFactory.getLogger(DirectoryHelper.class);

  public static final Set<FileVisitOption> DEFAULT_FILE_VISIT_OPTIONS = EnumSet.of(FileVisitOption.FOLLOW_LINKS);

  private DirectoryHelper() {
    // empty
  }

  // Visitors

  private static class FunctionVisitor
      extends SimpleFileVisitor<Path>
  {
    private final Function<Path, FileVisitResult> func;

    public FunctionVisitor(final Function<Path, FileVisitResult> func) {
      this.func = checkNotNull(func);
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes a) throws IOException {
      return func.apply(file);
    }

    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
      if (e != null) {
        throw e;
      }
      return func.apply(dir);
    }
  }

  private static class FunctionFileVisitor
      extends SimpleFileVisitor<Path>
  {
    private final Function<Path, FileVisitResult> func;

    public FunctionFileVisitor(final Function<Path, FileVisitResult> func) {
      this.func = checkNotNull(func);
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes a) throws IOException {
      return func.apply(file);
    }
  }

  private static class CopyVisitor
      extends SimpleFileVisitor<Path>
  {
    private final Path from;

    private final Path to;

    private final Predicate<Path> excludeFilter;

    public CopyVisitor(final Path from, final Path to, @Nullable final Predicate<Path> excludeFilter) {
      this.from = from;
      this.to = to;
      this.excludeFilter = excludeFilter;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes a) throws IOException {
      if (excludeFilter != null && excludeFilter.apply(dir)) {
        return FileVisitResult.SKIP_SUBTREE;
      }
      final Path targetPath = to.resolve(from.relativize(dir));
      if (!Files.exists(targetPath)) {
        mkdir(targetPath);
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes a) throws IOException {
      Files.copy(file, to.resolve(from.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
      return FileVisitResult.CONTINUE;
    }
  }

  // MKDIR: directory creation resilient to symlinks

  /**
   * Creates a directory. Fails only if directory creation fails, otherwise cleanly returns. If cleanly returns,
   * it is guaranteed that passed in path is created (with all parents as needed) successfully. Unlike Java7
   * {@link Files#createDirectories(Path, FileAttribute[])} method, this method does support paths having last
   * path element a symlink too. In this case, it's verified that symlink points to a directory and is readable.
   */
  public static void mkdir(final Path dir) throws IOException {
    try {
      Files.createDirectories(dir);
    }
    catch (FileAlreadyExistsException e) {
      // this happens when last element of path exists, but is a symlink.
      // A simple test with Files.isDirectory should be able to detect this
      // case as by default, it follows symlinks.
      if (!Files.isDirectory(dir)) {
        throw e;
      }
    }
  }

  /**
   * @since 2.8
   */
  public static void mkdir(final File dir) throws IOException {
    mkdir(dir.toPath());
  }

  /**
   * Given a parent directory, create the child directory using {@link #mkdir(File)}
   * 
   * @since 3.5
   */
  public static File mkdir(final File parent, final String child) throws IOException {
    File dir = new File(parent, child);
    mkdir(dir);
    return dir;
  }

  // CLEAN: remove files recursively of a directory but keeping the directory structure intact

  /**
   * Cleans an existing directory from any (non-directory) files recursively. Accepts only existing
   * directories, and when returns, it's guaranteed that this directory might contain only subdirectories
   * that also might contain only subdirectories (recursively).
   */
  public static void clean(final Path dir) throws IOException {
    validateDirectory(dir);
    Files.walkFileTree(dir, DEFAULT_FILE_VISIT_OPTIONS, Integer.MAX_VALUE,
        new SimpleFileVisitor<Path>()
        {
          @Override
          public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  /**
   * Invokes {@link #clean(Path)} if passed in path exists and is a directory. Also, in that case {@code true} is
   * returned, and in any other case (path does not exists) {@code false} is returned.
   */
  public static boolean cleanIfExists(final Path dir) throws IOException {
    checkNotNull(dir);
    if (Files.exists(dir)) {
      clean(dir);
      return true;
    }
    else {
      return false;
    }
  }

  // EMPTY: removes directory subtree with directory itself left intact

  /**
   * Empties an existing directory, by recursively removing all it's siblings, regular files and directories. Accepts
   * only existing directories, and when returns, it's guaranteed that passed in directory will be emptied (will
   * have no siblings, not counting OS specific ones, will be "empty" as per current OS is empty defined).
   */
  public static void empty(final Path dir) throws IOException {
    validateDirectory(dir);
    Files.walkFileTree(dir, DEFAULT_FILE_VISIT_OPTIONS, Integer.MAX_VALUE,
        new SimpleFileVisitor<Path>()
        {
          @Override
          public FileVisitResult visitFile(final Path f, final BasicFileAttributes attrs) throws IOException {
            Files.deleteIfExists(f);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(final Path d, final IOException exc) throws IOException {
            if (exc != null) {
              throw exc;
            }
            else if (dir != d) {
              Files.deleteIfExists(d);
            }
            return FileVisitResult.CONTINUE;
          }
        });
  }

  /**
   * Will walk a directory structure and prune any empty directories found that have modified timestamps that fall
   * before the provided timestamp value. If null, all empty directories will be pruned
   */
  public static int deleteIfEmptyRecursively(final Path dir, final Long timestamp) throws IOException {
    final AtomicInteger deleteCount = new AtomicInteger(0);

    File rootDir = dir.toFile();

    if (!rootDir.exists()) {
      log.debug("Requested path {} doesn't exist, will not process for empty directories to remove.",
          rootDir.getAbsolutePath());
      return 0;
    }

    Files.walkFileTree(dir, new SimpleFileVisitor<Path>()
    {
      @Override
      public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
        File dirFile = dir.toFile();
        if (!dirFile.exists()) {
          log.debug("Processing directory {} that no longer exists, will ignore and move on", dir.toAbsolutePath());
          return FileVisitResult.CONTINUE;
        }
        if (timestamp != null && dirFile.lastModified() > timestamp) {
          log.debug("Processing directory {} has been modified recently and will not be removed.",
              dir.toAbsolutePath());
          return FileVisitResult.CONTINUE;
        }

        String[] items = dir.toFile().list();
        if (items != null && items.length == 0) {
          try {
            Files.delete(dir);
            deleteCount.incrementAndGet();
          }
          catch (IOException e) {
            log.error("Failed to delete empty directory {} will stop processing.", dir.toAbsolutePath(), e);
            return FileVisitResult.TERMINATE;
          }
        }
        return FileVisitResult.CONTINUE;
      }
    });

    return deleteCount.intValue();
  }

  /**
   * Invokes {@link #empty(Path)} if passed in path exists and is a directory. Also, in that case {@code true} is
   * returned, and in any other case (path does not exists) {@code false} is returned.
   */
  public static boolean emptyIfExists(final Path dir) throws IOException {
    checkNotNull(dir);
    if (Files.exists(dir)) {
      empty(dir);
      return true;
    }
    else {
      return false;
    }
  }

  // DELETE: removes directory subtree with directory itself recursively

  /**
   * Deletes a file or directory recursively. This method accepts paths denoting regular files and directories. In case
   * of directory, this method will recursively delete all of it siblings and the passed in directory too.
   */
  public static void delete(final Path dir) throws IOException {
    delete(dir, null);
  }

  /**
   * Deletes a file or directory recursively. This method accepts paths denoting regular files and directories. In case
   * of directory, this method will recursively delete all of it siblings and the passed in directory too.
   * The passed in filter can leave out a directory and it's complete subtree from operation.
   */
  public static void delete(final Path dir, @Nullable final Predicate<Path> excludeFilter) throws IOException {
    validateDirectoryOrFile(dir);
    if (Files.isDirectory(dir)) {
      Files.walkFileTree(dir, DEFAULT_FILE_VISIT_OPTIONS, Integer.MAX_VALUE,
          new SimpleFileVisitor<Path>()
          {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
              if (excludeFilter != null && excludeFilter.apply(dir)) {
                return FileVisitResult.SKIP_SUBTREE;
              }
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
              Files.delete(file);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
              if (exc != null) {
                throw exc;
              }
              else {
                // Do this costly calculation only if filter is set,
                // as in that case filtered folder and it's parents will
                // not be empty, hence needs no deletion attempt as it would fail.
                boolean needsDelete = true;
                if (excludeFilter != null) {
                  try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
                    if (dirStream.iterator().hasNext()) {
                      needsDelete = false;
                    }
                  }
                }
                if (needsDelete) {
                  Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
              }
            }
          });
    }
    else {
      Files.delete(dir);
    }
  }

  /**
   * Invokes {@link #delete(Path)} if passed in path exists. Also, in that case {@code true} is
   * returned, and in any other case (path does not exists) {@code false} is returned.
   */
  public static boolean deleteIfExists(final Path dir) throws IOException {
    return deleteIfExists(dir, null);
  }

  /**
   * Invokes {@link #delete(Path)} if passed in path exists. Also, in that case {@code true} is
   * returned, and in any other case (path does not exists) {@code false} is returned.
   * The passed in filter can leave out a directory and it's complete subtree from operation.
   */
  public static boolean deleteIfExists(
      final Path dir,
      @Nullable final Predicate<Path> excludeFilter) throws IOException
  {
    checkNotNull(dir);
    if (Files.exists(dir)) {
      delete(dir, excludeFilter);
      return true;
    }
    else {
      return false;
    }
  }

  // COPY: recursive copy of whole directory tree

  /**
   * Copies path "from" to path "to". This method accepts both existing regular files and existing directories. If
   * "from" is a directory, a recursive copy happens of the whole subtree with "from" directory as root. Caller may
   * alter behaviour of Copy operation using copy options, as seen on {@link Files#copy(Path, Path, CopyOption...)}.
   */
  public static void copy(final Path from, final Path to) throws IOException {
    copy(from, to, null);
  }

  /**
   * Copies path "from" to path "to". This method accepts both existing regular files and existing directories. If
   * "from" is a directory, a recursive copy happens of the whole subtree with "from" directory as root. Caller may
   * alter behaviour of Copy operation using copy options, as seen on {@link Files#copy(Path, Path, CopyOption...)}.
   * The passed in filter can leave out a directory and it's complete subtree from operation.
   *
   * @throws IllegalArgumentException if 'from' is a parent directory of the 'to' path, unless an excludeFilter is
   *           provided
   */
  public static void copy(
      final Path from,
      final Path to,
      @Nullable final Predicate<Path> excludeFilter) throws IOException
  {
    validateDirectoryOrFile(from);
    checkNotNull(to);
    if (Files.isDirectory(from)) {
      // Avoiding recursion: unless there's an exclude filter, the 'to' dir must not be inside the 'from' dir
      checkArgument(!isParentOf(from, to) || excludeFilter != null);
      Files.walkFileTree(from, DEFAULT_FILE_VISIT_OPTIONS, Integer.MAX_VALUE,
          new CopyVisitor(from, to, excludeFilter));
    }
    else {
      mkdir(to.getParent());
      Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * Invokes {@link #copy(Path, Path)} if passed in "from" path exists and returns {@code true}. If
   * "from" path does not exists, {@code false} is returned.
   */
  public static boolean copyIfExists(final Path from, final Path to) throws IOException {
    return copyIfExists(from, to, null);
  }

  /**
   * Invokes {@link #copy(Path, Path)} if passed in "from" path exists and returns {@code true}. If
   * "from" path does not exists, {@code false} is returned.
   * The passed in filter can leave out a directory and it's complete subtree from operation.
   */
  public static boolean copyIfExists(
      final Path from,
      final Path to,
      @Nullable final Predicate<Path> excludeFilter) throws IOException
  {
    checkNotNull(from);
    if (Files.exists(from)) {
      copy(from, to, excludeFilter);
      return true;
    }
    else {
      return false;
    }
  }

  // MOVE: recursive copy of whole directory tree and then deleting it

  /**
   * Perform a move of existing directory (empty or not, does not matter) on same FileStore (volume or partition).
   */
  private static void sameFileStoreMove(final Path from, final Path to) throws IOException {
    Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
  }

  /**
   * Perform a pseudo-move of existing directory (empty or not, does not matter) between different FileStores (volume
   * or partition) using {@link #copyDeleteMove(Path, Path, Predicate)} method.
   */
  private static void crossFileStoreMove(final Path from, final Path to) throws IOException {
    copyDeleteMove(from, to, null);
  }

  /**
   * Return {@code true} if paths {@code from} and {@code to} are located on same FileStore (volume or
   * partition). The {@code from} must exists, while {@code to} does not have to.
   */
  private static boolean areOnSameFileStore(final Path from, final Path to) {
    try {
      final FileStore fromStore = Files.getFileStore(from); // from must exist
      Path toExistingParent = to.normalize(); // to usually does not exists, is about to be created as part of move
      while (toExistingParent != null && !Files.exists(toExistingParent)) {
        toExistingParent = toExistingParent.getParent();
      }
      if (toExistingParent != null) {
        final FileStore toStore = Files.getFileStore(toExistingParent);
        return fromStore.equals(toStore);
      }
      else {
        log.warn("No ultimate parent path found for '{}'", to, new RuntimeException("marker")); // record the stack
                                                                                                // trace?
        return false; // no ultimate parent? be on safe side
      }
    }
    catch (IOException e) {
      return false; // be on safe side
    }
  }

  /**
   * Performs a move operation. It will attempt a real move (if source and target are on same file store), but will
   * fallback to a sequence of "copy" and then "delete" (not a real move!). This method accepts
   * existing Paths that might denote a regular file or a directory. It basically delegates to {@link Files#move(Path,
   * Path, CopyOption...)} method with "replace existing" parameter.
   */
  public static void move(final Path from, final Path to) throws IOException {
    if (areOnSameFileStore(from, to)) {
      sameFileStoreMove(from, to);
    }
    else {
      crossFileStoreMove(from, to);
    }
  }

  /**
   * Invokes {@link #move(Path, Path)} if passed in "from" path exists and returns {@code true}. If
   * "from" path does not exists, {@code false} is returned.
   */
  public static boolean moveIfExists(final Path from, final Path to) throws IOException {
    checkNotNull(from);
    if (Files.exists(from)) {
      move(from, to);
      return true;
    }
    else {
      return false;
    }
  }

  /**
   * Performs a pseudo move operation (copy+delete). This method accepts existing Paths that might denote a regular file
   * or a directory. While this method is not a real move (like {@link #move(Path, Path)} is), it is a bit more capable:
   * it can move a complete directory structure to it's one sub-directory.
   */
  public static void copyDeleteMove(
      final Path from,
      final Path to,
      @Nullable final Predicate<Path> excludeFilter) throws IOException
  {
    copy(from, to, excludeFilter);
    delete(from, excludeFilter);
  }

  /**
   * Invokes {@link #copyDeleteMove(Path, Path, Predicate)} if passed in "from" path exists and returns {@code true}. If
   * "from" path does not exists, {@code false} is returned.
   */
  public static boolean copyDeleteMoveIfExists(
      final Path from,
      final Path to,
      @Nullable final Predicate<Path> excludeFilter) throws IOException
  {
    checkNotNull(from);
    if (Files.exists(from)) {
      copyDeleteMove(from, to, excludeFilter);
      return true;
    }
    else {
      return false;
    }
  }

  // APPLY: applies a function to dir tree, the function should not have any IO "side effect"

  /**
   * Traverses the subtree starting with "from" and applies passed in {@link Function} onto files and directories.
   * This method accepts only existing directories.
   */
  public static void apply(final Path from, final Function<Path, FileVisitResult> func) throws IOException {
    validateDirectory(from);
    Files.walkFileTree(from, DEFAULT_FILE_VISIT_OPTIONS, Integer.MAX_VALUE, new FunctionVisitor(func));
  }

  /**
   * Traverses the subtree starting with "from" and applies passed in {@link Function} onto files only.
   * This method accepts only existing directories.
   */
  public static void applyToFiles(final Path from, final Function<Path, FileVisitResult> func) throws IOException {
    validateDirectory(from);
    Files.walkFileTree(from, DEFAULT_FILE_VISIT_OPTIONS, Integer.MAX_VALUE, new FunctionFileVisitor(func));
  }

  // Validation

  /**
   * Enforce all passed in paths are non-null and is existing directory.
   */
  private static void validateDirectory(final Path... paths) {
    for (Path path : paths) {
      checkNotNull(path, "Path must be non-null");
      checkArgument(Files.isDirectory(path), "%s is not a directory", path);
    }
  }

  /**
   * Enforce all passed in paths are non-null and exist.
   */
  private static void validateDirectoryOrFile(final Path... paths) {
    for (Path path : paths) {
      checkNotNull(path, "Path must be non-null");
      checkArgument(Files.exists(path), "%s does not exists", path);
    }
  }

  /**
   * Determine if one path is a child of another.
   */
  private static boolean isParentOf(Path possibleParent, Path possibleChild) {
    return possibleChild.startsWith(possibleParent);
  }
}
