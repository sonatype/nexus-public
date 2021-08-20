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
 * Implementation of {@link RawObjectAccess} that returns UnsupportedOperationException for all methods.
 */
public class UnimplementedRawObjectAccess
    implements RawObjectAccess
{
  public static final String RAW_OBJECTS_NOT_SUPPORTED = "BlobStore does not support raw objects";

  @Override
  public Stream<String> listRawObjects(@Nullable final Path path) {
    throw new UnsupportedOperationException(RAW_OBJECTS_NOT_SUPPORTED);
  }

  @Nullable
  @Override
  public InputStream getRawObject(final Path path) {
    throw new UnsupportedOperationException(RAW_OBJECTS_NOT_SUPPORTED);
  }

  @Override
  public void putRawObject(final Path path, final InputStream in) {
    throw new UnsupportedOperationException(RAW_OBJECTS_NOT_SUPPORTED);
  }

  @Override
  public boolean hasRawObject(final Path path) {
    throw new UnsupportedOperationException(RAW_OBJECTS_NOT_SUPPORTED);
  }

  @Override
  public void deleteRawObject(final Path path) {
    throw new UnsupportedOperationException(RAW_OBJECTS_NOT_SUPPORTED);
  }

  @Override
  public void deleteRawObjectsInPath(final Path path) {
    throw new UnsupportedOperationException(RAW_OBJECTS_NOT_SUPPORTED);
  }
}
