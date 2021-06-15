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
package org.sonatype.nexus.repository.p2.internal.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;

/**
 * @since 3.28
 */
@FunctionalInterface
public interface StreamCopier
{
  void process(StreamTransformer transformer) throws IOException;

  /**
   *
   * @since 3.28
   */
  @FunctionalInterface
  public interface StreamTransformer
  {
    void transform(InputStream in, OutputStream out) throws IOException;
  }

  static StreamCopier zip(final String filename, final InputStream in, final OutputStream out) {
    return transformer -> {
      try (ZipInputStream zipIn = new ZipInputStream(in); ZipOutputStream zipOut = new ZipOutputStream(out)) {
        ZipEntry entry;
        while ((entry = zipIn.getNextEntry()) != null) {
          if (!entry.isDirectory() && filename.equals(entry.getName())) {
            zipOut.putNextEntry(new ZipEntry(filename));
            transformer.transform(zipIn, zipOut);
            return;
          }
        }
        throw new IOException("Unable to locate entry with filename: " + filename);
      }
    };
  }

  static StreamCopier xz(final InputStream in, final OutputStream out) {
    return transformer -> {
      try (XZCompressorInputStream xzIn = new XZCompressorInputStream(in);
          XZCompressorOutputStream xzOut = new XZCompressorOutputStream(out)) {
        transformer.transform(xzIn, xzOut);
      }
    };
  }

  static StreamCopier passthrough(final InputStream in, final OutputStream out) {
    return transformer -> transformer.transform(in, out);
  }

  /**
   * Provide a StreamCopier for the given type.
   *
   * @param mimeType the mimetype of the soruce stream
   * @param filename the desired filename within an archive
   * @param in the source stream
   * @param out the destination stream
   *
   * @throws IOException
   */
  public static StreamCopier copierFor(
      final String mimeType,
      final String filename,
      final InputStream in,
      final OutputStream out) throws IOException
  {
    switch (mimeType) {
      case "text/plain":
      case "application/xml":
      case "text/xml":
        return StreamCopier.passthrough(in, out);
      case "application/java-archive":
      case "application/zip":
        return StreamCopier.zip(filename, in, out);
      case "application/x-xz":
        return StreamCopier.xz(in, out);
      default:
        throw new IOException("Unknown media type " + mimeType + " for metadata " + filename);
    }
  }
}
