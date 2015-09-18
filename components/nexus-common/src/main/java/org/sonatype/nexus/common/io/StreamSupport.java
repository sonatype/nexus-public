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

import org.sonatype.nexus.common.property.SystemPropertiesHelper;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

// FIXME: Rename to Streams, or StreamHelper

/**
 * Stream related support class. Offers static helper methods for common stream related operations
 * used in Nexus core and plugins for manipulating streams.
 *
 * @author cstamas
 * @since 2.7.0
 */
public final class StreamSupport
{
  private StreamSupport() {
    // no instance
  }

  public static final int BUFFER_SIZE = SystemPropertiesHelper.getInteger(
      StreamSupport.class.getName() + ".BUFFER_SIZE", 8192
  );

  // TODO: Perhaps remove this and simply use Guava ByteStreams?

  /**
   * Copies provided input stream to the provided output stream, probably using buffer of provided size. The buffer
   * size is used only as "hint", as the actual copy might not be done with buffer having the exact size of passed in
   * parameter.
   */
  public static long copy(final InputStream from, final OutputStream to, final int bufferSize) throws IOException {
    checkNotNull(from);
    checkNotNull(to);
    checkArgument(bufferSize > 0);
    final byte[] buf = new byte[bufferSize];
    long count = 0;
    while (true) {
      int r = from.read(buf);
      if (r == -1) {
        break;
      }
      count += r;
      to.write(buf, 0, r);
    }
    return count;
  }
}
