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
package org.sonatype.nexus.common.net;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.nio.channels.FileLock;

// This class has temporarily been copied into the nxrm codebase for use by the public portion of the codecase, which
// doesn't have access to the library this class came from, as its internal, ultimately this will be put
// into a library that we push up to central for both IQ and NXRM to ingest.

/**
 * Port util class that does the best job possible to make sure you are given an available port that wont be grabbed by
 * others
 *
 * @since 3.31
 */
public class PortAllocator
{
  private static final File NEXT_PORT_FILE = new File(System.getProperty("java.io.tmpdir"), "nx-test-port-allocator");

  private static final int MIN_PORT = 10000;

  private static final int MAX_PORT = 30000;

  // see https://chromium.googlesource.com/chromium/src.git/+/refs/heads/master/net/base/port_util.cc
  private static final int UNSAFE_PORT = 10080;

  /**
   * This does deliberately NOT pick ephemeral ports which are prone to immediate reuse by the OS for a different
   * purpose the moment we release the found port (to be used for the intended purpose).
   * Instead, this manually scans a range which doesn't overlap with ephemeral ports and uses a shared temporary file to
   * collaborate with other/forked JVMs. The key feature of this port allocation is that a found port is not
   * reused/refound until the entire port range is exhausted.
   */
  public static synchronized int nextFreePort() {
    try (RandomAccessFile raf = new RandomAccessFile(NEXT_PORT_FILE, "rw"); FileLock lock = raf.getChannel().lock()) {
      int nextPort = raf.length() < 4 ? MIN_PORT : raf.readInt();
      try {
        while (true) {
          if (nextPort > MAX_PORT) {
            nextPort = MIN_PORT;
          }
          if (UNSAFE_PORT != nextPort) {
            try (ServerSocket socket = new ServerSocket(nextPort++)) {
              return socket.getLocalPort();
            }
            catch (BindException e) {
              // port blocked, try the next one
            }
          }
          else {
            nextPort++;
          }

        }
      }
      finally {
        raf.seek(0);
        raf.writeInt(nextPort);
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
