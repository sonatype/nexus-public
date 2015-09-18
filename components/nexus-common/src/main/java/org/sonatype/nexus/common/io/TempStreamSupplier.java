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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

/**
 * Renders an InputStream re-readable by saving it to a temporary file that is removed when the supplier is closed.
 *
 * @since 3.0
 */
public class TempStreamSupplier
    extends ComponentSupport
    implements AutoCloseable, Supplier<InputStream>
{
  private final Path tempFile;

  public TempStreamSupplier(final InputStream inputStream) throws IOException {
    tempFile = Files.createTempFile("", "");
    try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
      ByteStreams.copy(inputStream, outputStream);
    }
    finally {
      Closeables.closeQuietly(inputStream);
    }
  }

  @Override
  public void close() {
    try {
      Files.delete(tempFile);
    }
    catch (IOException e) {
      log.warn("Unable to delete temp file {}", tempFile, e);
    }
  }

  @Override
  public InputStream get() {
    try {
      return Files.newInputStream(tempFile);
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}
