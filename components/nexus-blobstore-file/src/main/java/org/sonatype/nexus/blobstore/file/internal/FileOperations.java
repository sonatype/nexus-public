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
import java.nio.file.Path;

import org.sonatype.nexus.blobstore.StreamMetrics;

/**
 * A wrapper around file operations to make mocking easier.
 *
 * @since 3.0
 */
public interface FileOperations
{
  /**
   * Creates a file (and its containing directories, if necessary) and populates it from the
   * InputStream, which gets closed.
   *
   * @return Basic metrics about the stream.
   */
  StreamMetrics create(Path path, InputStream data) throws IOException;

  /**
   * Creates a hard link.
   */
  void hardLink(Path source, Path newLink) throws IOException;

  /**
   * Copies a file.
   * 
   * @since 3.2
   */
  void copy(Path source, Path target) throws IOException;

  /**
   * Moves a file without any guarantee of atomicity.
   *
   * @since 3.2
   */
  void move(Path source, Path target) throws IOException;

  /**
   * Moves a file atomically, throwing <code>AtomicMoveNotSupportedException</code> if not supported.
   *
   * @since 3.2
   */
  void moveAtomic(Path source, Path target) throws IOException;

  /**
   * Computes basic metrics about the file.
   */
  StreamMetrics computeMetrics(Path file) throws IOException;

  boolean exists(Path path);

  InputStream openInputStream(Path path) throws IOException;

  /**
   * Returns true if the file existed before deletion, false otherwise.
   */
  boolean delete(Path path) throws IOException;

  /**
   * Recursively deletes all files and subdirectories, then the directory itself.
   */
  void deleteDirectory(Path directory) throws IOException;

  /**
   * Returns true if the directory was empty and could be removed, false otherwise.
   */
  boolean deleteEmptyDirectory(Path directory) throws IOException;
}
