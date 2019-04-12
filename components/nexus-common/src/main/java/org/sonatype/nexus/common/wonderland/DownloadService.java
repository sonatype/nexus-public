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
package org.sonatype.nexus.common.wonderland;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

/**
 * Download service.
 *
 * @since 2.8
 */
public interface DownloadService
{

  /**
   * @since 3.13
   */
  class Download {

    private final long length;

    private final InputStream bytes;

    public Download(final long length, final InputStream bytes) {
      this.length = length;
      this.bytes = bytes;
    }

    /**
     * @since 3.16
     */
    public long getLength() {
      return length;
    }

    /**
     * @since 3.16
     */
    public InputStream getBytes() {
      return bytes;
    }
  }

  /**
   * @param fileName of file to be downloaded
   * @param authTicket authentication ticket
   * @return the download, or null if it doesn't exist
   */
  @Nullable
  Download get(String fileName, String authTicket) throws IOException;

  /**
   * Moves specified file to downloads, using specified name.
   *
   * @param source to be moved
   * @param name name of file in downloads dir
   * @return moved filename (residing in downloads)
   */
  String move(File source, String name) throws IOException;

  /**
   * Generate a unique file prefix.
   */
  String uniqueName(String prefix);
}
