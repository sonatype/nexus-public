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
package org.sonatype.nexus.blobstore.file;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.RawObjectAccess;

import org.apache.commons.io.FileUtils;

import static java.nio.file.Files.delete;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link RawObjectAccess} for the {@link FileBlobStore}.
 *
 * @since 3.31
 */
public class FileRawObjectAccess
    extends ComponentSupport
    implements RawObjectAccess
{
  private final Path storageDir;

  public FileRawObjectAccess(final Path storageDir) {
    this.storageDir = requireNonNull(storageDir);
  }

  @Override
  public Stream<String> listRawObjects(@Nullable final Path path) {
    try {
      Path dir = path != null ? storageDir.resolve(path) : storageDir;
      return listFiles(dir, File::isFile).map(File::getName);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  @Nullable
  public InputStream getRawObject(final Path path) {
    try {
      File rawObjectFile = storageDir.resolve(path).toFile();
      if (rawObjectFile.exists()) {
        return new FileInputStream(rawObjectFile);
      }
      else {
        return null;
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void putRawObject(final Path path, final InputStream input) {
    try (InputStream in = input) {
      File rawObjectFile = storageDir.resolve(path).toFile();
      FileUtils.copyToFile(in, rawObjectFile);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Stream<File> listFiles(Path dirPath, FileFilter fileFilter) throws IOException {
    File dir = dirPath.toFile();
    if (dir.isDirectory()) {
      File[] files = dir.listFiles(fileFilter);
      if (files != null) {
        return stream(files).sorted();
      }
      else {
        throw new IOException("Unexpected exception reading directory " + dir);
      }
    }
    else {
      log.debug("Path {} is not a directory", dirPath);
      return Stream.empty();
    }
  }

  @Override
  public void deleteRawObject(final Path path) {
    try {
      File rawObjectFile = storageDir.resolve(path).toFile();
      if (rawObjectFile.exists()) {
        delete(rawObjectFile.toPath());
        deleteEmptyParentDirectories(rawObjectFile.getParentFile());
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void deleteRawObjectsInPath(final Path path) {
    try {
      File rawObjectDir = storageDir.resolve(path).toFile();
      if (rawObjectDir.exists()) {
        File[] files = rawObjectDir.listFiles();
        if (files != null) {
          for (File file : files) {
            if (file.isFile()) {
              delete(file.toPath());
            }
          }
        }

        deleteEmptyParentDirectories(rawObjectDir);
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public boolean hasRawObject(final Path path) {
    File rawObjectFile = storageDir.resolve(path).toFile();
    return rawObjectFile.exists();
  }

  private void deleteEmptyParentDirectories(final File dir) throws IOException {
    File parent = dir.getParentFile();
    String[] children = dir.list();
    if (children == null || children.length == 0) {
      delete(dir.toPath());
      deleteEmptyParentDirectories(parent);
    }
  }
}
