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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.MetricsInputStream;
import org.sonatype.nexus.blobstore.StreamMetrics;
import org.sonatype.nexus.common.io.DirectoryHelper;

import com.google.common.io.ByteStreams;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A simple {@code java.nio} implementation of {@link FileOperations}.
 *
 * @since 3.0
 */
@Named
public class SimpleFileOperations
    extends ComponentSupport
    implements FileOperations
{
  @Override
  public StreamMetrics create(final Path path, final InputStream data) throws IOException {
    checkNotNull(path);
    checkNotNull(data);

    // Ensure path exists for new blob
    Path dir = path.getParent();
    checkNotNull(dir, "Null parent for path: %s", path);
    DirectoryHelper.mkdir(dir);

    final MetricsInputStream input = new MetricsInputStream(data);
    try {
      try (final OutputStream output = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
        ByteStreams.copy(input, output);
      }
    }
    finally {
      // FIXME: Revisit closing stream which is passed in as parameter, this should be the responsibility of the caller
      data.close();
    }

    return input.getMetrics();
  }

  @Override
  public void hardLink(final Path source, final Path newLink) throws IOException {
    DirectoryHelper.mkdir(newLink.getParent());
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
    DirectoryHelper.mkdir(target.getParent());
    Files.copy(source, target);
  }

  @Override
  public void move(final Path source, final Path target) throws IOException {
    checkNotNull(source);
    checkNotNull(target);
    DirectoryHelper.mkdir(target.getParent());
    Files.move(source, target);
  }

  @Override
  public void moveAtomic(final Path source, final Path target) throws IOException {
    checkNotNull(source);
    checkNotNull(target);
    DirectoryHelper.mkdir(target.getParent());
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
    DirectoryHelper.mkdir(target.getParent());
    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
  }

  @Override
  public void overwriteAtomic(final Path source, final Path target) throws IOException {
    checkNotNull(source);
    checkNotNull(target);
    DirectoryHelper.mkdir(target.getParent());
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
    DirectoryHelper.emptyIfExists(directory);
    Files.deleteIfExists(directory);
  }

  /**
   * Removes the directory if and only if the directory is empty.
   */
  @Override
  public boolean deleteEmptyDirectory(final Path directory) throws IOException {
    try {
      Files.deleteIfExists(directory);
      return true;
    }
    catch (DirectoryNotEmptyException e) {
      log.debug("Cannot remove non-empty directory {}", directory, e);
      return false;
    }
  }
}
