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
package org.sonatype.nexus.repository.view;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.Nullable;

import com.google.common.io.ByteStreams;

/**
 * Payload.
 *
 * @since 3.0
 */
public interface Payload
    extends Closeable
{
  long UNKNOWN_SIZE = -1;

  InputStream openInputStream() throws IOException;

  long getSize();

  @Nullable
  String getContentType();

  /**
   * Closes this payload, relinquishing any underlying resources. Streams previously handed out by
   * {@link #openInputStream()} may not be affected by this method and should be closed separately.
   *
   * @since 3.1
   */
  @Override
  default void close() throws IOException {
    // no underlying resources to clean-up by default
  }

  /**
   * Provide a handle on the copying of the {@link InputStream} to the given {@link OutputStream}.
   * Callers are required to handle the flushing and closing of the streams. By default we
   * do a {@link ByteStreams#copy(InputStream, OutputStream)}.
   *
   * @param input  {@link InputStream}
   * @param output {@link OutputStream}
   */
  default void copy(final InputStream input, final OutputStream output) throws IOException {
    ByteStreams.copy(input, output);
  }
}
