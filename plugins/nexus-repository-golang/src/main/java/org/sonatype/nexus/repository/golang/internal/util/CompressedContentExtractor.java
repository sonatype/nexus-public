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
package org.sonatype.nexus.repository.golang.internal.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.view.Payload;

import org.apache.commons.io.input.NullInputStream;

/**
 * Extracts a file from a zip image
 *
 * @since 3.next
 */
public class CompressedContentExtractor
    extends ComponentSupport
{
  /**
   * Extracts a file from a zip image
   *
   * @param projectAsStream zip file as a stream
   * @param fileName        file to extract
   * @return stream of extracted file
   */
  @Nullable
  public InputStream extractFile(final InputStream projectAsStream, final String fileName) {
    return extract(projectAsStream, fileName, this::fetchFileAsStream);
  }

  /**
   * Checks if a file exists in the zip
   *
   * @param payload  zip file as a payload
   * @param path     path of stream to be extracted
   * @param fileName file to check exists
   * @return true if it exists
   */
  public boolean fileExists(final Payload payload, final String path, final String fileName) {
    try (InputStream projectAsStream = payload.openInputStream()) {
      return extract(projectAsStream, fileName, (ZipInputStream z) -> new NullInputStream(-1)) != null;
    }
    catch (IOException e) {
      log.warn("Unable to open content {}", path, e);
    }
    return false;
  }

  private ByteArrayInputStream fetchFileAsStream(final ZipInputStream zipInputStream) {
    try {
      ByteArrayOutputStream outputStream = extractEntry(zipInputStream);
      return new ByteArrayInputStream(outputStream.toByteArray());
    }
    catch (IOException e) {
      log.warn("Unable to uncompress zip", e);
    }
    return null;
  }

  private ByteArrayOutputStream extractEntry(final ZipInputStream zipInputStream) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] bytes = new byte[1024];
    while (true) {
      int read = zipInputStream.read(bytes);
      if (read < 0) {
        break;
      }
      outputStream.write(bytes, 0, read);
    }
    return outputStream;
  }

  private InputStream extract(final InputStream projectAsStream,
                              final String fileName,
                              final Function<ZipInputStream, InputStream> method)
  {
    try (ZipInputStream zipInputStream = new ZipInputStream(projectAsStream)) {
      ZipEntry nextEntry = zipInputStream.getNextEntry();
      while (nextEntry != null) {
        if (nextEntry.getName().endsWith(fileName)) {
          return method.apply(zipInputStream);
        }
        zipInputStream.closeEntry();
        nextEntry = zipInputStream.getNextEntry();
      }
    }
    catch (IOException e) {
      log.warn("Unable to uncompress zip", e);
    }
    return null;
  }
}
