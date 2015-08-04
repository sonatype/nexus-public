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
package org.sonatype.nexus.ruby;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class IOUtil
{

  /**
   * Copy bytes from an <code>InputStream</code> to an <code>OutputStream</code>.
   */
  public static void copy(final InputStream input, final OutputStream output) throws IOException {
    final byte[] buffer = new byte[4096];
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
    }
  }

  public static ByteArrayInputStream toGzipped(final InputStream input) throws IOException {
    ByteArrayOutputStream gzipped = new ByteArrayOutputStream();
    try (GZIPOutputStream out = new GZIPOutputStream(gzipped)) {
      copy(input, out);
    }
    return new ByteArrayInputStream(gzipped.toByteArray());
  }

  public static ByteArrayInputStream toGunzipped(final InputStream input) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    copy(new GZIPInputStream(input), out);
    return new ByteArrayInputStream(out.toByteArray());
  }
}
