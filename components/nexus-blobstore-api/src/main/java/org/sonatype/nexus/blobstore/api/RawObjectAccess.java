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
package org.sonatype.nexus.blobstore.api;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.stream.Stream;

import javax.annotation.Nullable;

/**
 * Interface for accessing raw objects in a blobstore.
 *
 * @since 3.31
 */
public interface RawObjectAccess
{
  /**
   * List raw objects at this path in the blobstore.
   */
  Stream<String> listRawObjects(@Nullable Path path);

  /**
   * Return the content of the raw object at this path.
   */
  @Nullable
  InputStream getRawObject(Path path);

  /**
   * Create or replace the contents of the raw object at this path.
   */
  void putRawObject(Path path, InputStream in);

  boolean hasRawObject(Path path);

  void deleteRawObject(Path path);

  /**
   * Deletes all raw objects directly under this path. Does not recursively delete.
   *
   * @since 3.34
   */
  void deleteRawObjectsInPath(Path path);
}
