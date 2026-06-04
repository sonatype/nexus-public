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
package org.sonatype.nexus.blobstore.file.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.sonatype.nexus.blobstore.MetricsInputStream;
import org.sonatype.nexus.blobstore.StreamMetrics;
import org.sonatype.nexus.bootstrap.entrypoint.configuration.DirectoryHelper;

import com.google.common.io.ByteStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A simple {@code java.nio} implementation of {@link FileOperations}.
 *
 * @since 3.0
 */
@Component
public class SimpleFileOperations
    implements FileOperations
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  public static final int MAX_RETRIES = 3;

  private final DirectoryHelper directoryHelper;

  @Autowired
  public SimpleFileOperations(final DirectoryHelper directoryHelper) {
    this.directoryHelper = checkNotNull(directoryHelper);
  }

  @Override
  public StreamMetrics create(final Path path, final InputStream data) throws IOException {
    checkNotNull(path);
    checkNotNull(data);

    // Ensure path exists for new blob
    Path dir = path.getParent();
    checkNotNull(dir, "Null parent for path: %s", path);
    if (!Files.isDirectory(dir)) {
      directoryHelper.mkdir(dir);
    }

    final MetricsInputStream input = new MetricsInputStream(data);
    try (data) {
      try (final OutputStream output = create(path)) {
        ByteStreams.copy(input, output);
      }
    }

    return input.getMetrics();
  }

  /**
   * Creates a new file at the specified path, ensuring that the parent directory exists. Only throws an exception if
   * creation fails after a second attempt.
   */
  public OutputStream create(final Path path) throws IOException {
    IOException lastException = null;

    for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
      try {
        Path parent = path.getParent();
        if (parent != null) {
          directoryHelper.mkdir(parent);
        }
        return Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
      }
      catch (IOException e) {
        // Retry once on transient parent deletion or existence race
        lastException = e;
      }
    }

    // Both attempts failed, rethrow the last exception
    throw lastException;
  }

  @Override
  public void hardLink(final Path source, final Path newLink) throws IOException {
    directoryHelper.mkdir(newLink.getParent());
    try {
      Files.createLink(newLink, source);
    }
    catch (UnsupportedOperationException e) {
      // In some situations, an unsupported file link actually throws FileSystemException rather than
      // UnsupportedException, so wrap USEs here to narrow the range of exceptions that need catching.
      throw new IOException(e);
    }
    log.debug("Hard link created from {} to {}", newLink, source);
  }

  @Override
  public void copy(final Path source, final Path target) throws IOException {
    checkNotNull(source);
    checkNotNull(target);
    directoryHelper.mkdir(target.getParent());
    Files.copy(source, target);
  }

  @Override
  public void move(final Path source, final Path target) throws IOException {
    checkNotNull(source);
    checkNotNull(target);
    directoryHelper.mkdir(target.getParent());
    Files.move(source, target);
  }

  @Override
  public void moveAtomic(final Path source, final Path target) throws IOException {
    checkNotNull(source);
    checkNotNull(target);
    directoryHelper.mkdir(target.getParent());
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    }
    catch (UnsupportedOperationException e) { // NOSONAR
      throw new AtomicMoveNotSupportedException(source.toString(), target.toString(), e.getMessage());
    }
  }

  @Override
  public void overwrite(final Path source, final Path target) throws IOException {
    checkNotNull(source);
    checkNotNull(target);
    directoryHelper.mkdir(target.getParent());
    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
  }

  @Override
  public void overwriteAtomic(final Path source, final Path target) throws IOException {
    checkNotNull(source);
    checkNotNull(target);
    directoryHelper.mkdir(target.getParent());
    try {
      Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
    catch (UnsupportedOperationException e) { // NOSONAR
      throw new AtomicMoveNotSupportedException(source.toString(), target.toString(), e.getMessage());
    }
  }

  @Override
  public void copyIfLocked(final Path source, final Path target, final Mover mover) throws IOException {
    checkNotNull(source);
    checkNotNull(target);
    try {
      mover.accept(source, target);
    }
    catch (AtomicMoveNotSupportedException atomicMoveNotSupported) {
      throw atomicMoveNotSupported;
    }
    catch (FileSystemException e) { // NOSONAR
      // Windows can throw a FileSystemException on move or moveAtomic
      // if the file is in use; in this case, copy and attempt to delete
      // source
      log.warn("Using copy to move {} to {}", source, target);
      copy(source, target);
      try {
        delete(source);
      }
      catch (IOException deleteException) { // NOSONAR
        log.error("Unable to delete {} after move: {}", source, deleteException.getMessage());
      }
    }
  }

  @Override
  public StreamMetrics computeMetrics(final Path file) throws IOException {
    try (InputStream is = Files.newInputStream(file);
        MetricsInputStream mis = new MetricsInputStream(is)) {
      ByteStreams.copy(mis, ByteStreams.nullOutputStream());
      return mis.getMetrics();
    }
  }

  @Override
  public boolean exists(final Path path) {
    checkNotNull(path);
    return Files.exists(path);
  }

  @Override
  public boolean isBlobZeroLength(final Path path) {
    checkNotNull(path);
    try {
      return Files.exists(path) && Files.size(path) == 0L;
    }
    catch (IOException e) {
      return false;
    }
  }

  @Override
  public InputStream openInputStream(final Path path) throws IOException {
    checkNotNull(path);
    return Files.newInputStream(path, StandardOpenOption.READ);
  }

  @Override
  public boolean delete(final Path path) throws IOException {
    checkNotNull(path);
    checkArgument(!Files.isDirectory(path));
    boolean deleted = Files.deleteIfExists(path);

    // complain if request to delete file has failed
    if (!deleted && exists(path)) {
      throw new IOException("File was not successfully deleted: " + path);
    }

    return deleted;
  }

  @Override
  public boolean deleteQuietly(final Path path) {
    try {
      return delete(path);
    }
    catch (Exception e) {
      log.warn("Unable to delete path {}", path, e);
      return false;
    }
  }

  /**
   * Removes the directory and all of its contents.
   */
  @Override
  public void deleteDirectory(final Path directory) throws IOException {
    directoryHelper.emptyIfExists(directory);
    Files.deleteIfExists(directory);
  }

  public boolean isDirectoryEmpty(final Path directory) {
    if (Files.isDirectory(directory)) {
      try (var stream = Files.list(directory)) {
        return stream.findAny().isEmpty();
      }
      catch (IOException e) {
        log.debug("Unable to check if directory is empty {}", directory, e);
      }
    }
    return false;
  }

  /**
   * Removes the directory if and only if the directory is empty.
   */
  @Override
  public synchronized boolean deleteEmptyDirectory(final Path directory) {
    try {
      if (isDirectoryEmpty(directory)) {
        Files.deleteIfExists(directory);
        return true;
      }
    }
    catch (IOException e) {
      log.debug("Unable to remove directory {}", directory, e);
    }
    return false;
  }

  @Override
  public OffsetDateTime lastModified(final Path path) throws IOException {
    FileTime time = Files.getLastModifiedTime(path);
    return OffsetDateTime.ofInstant(time.toInstant(), ZoneOffset.UTC);
  }
}
