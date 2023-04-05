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
package org.sonatype.nexus.repository.view.payloads;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.view.Payload;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link Payload} backed by a {@link Path}
 */
public class PathPayload
    implements Payload
{
  private final Path path;

  private final String contentType;

  public PathPayload(final Path path, @Nullable final String contentType) {
    this.path = checkNotNull(path);
    this.contentType = contentType;
  }

  @Override
  public InputStream openInputStream() throws IOException {
    return new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ));
  }

  @Override
  public long getSize() {
    try {
      return Files.size(path);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public String getContentType() {
    return contentType;
  }
}
