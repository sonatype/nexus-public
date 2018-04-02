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
package org.sonatype.nexus.repository.npm.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;

public class ArchiveUtils
{
  public static InputStream pack(final File tempFile, final File srcFile, final String archiveFilename) {
    try (TarArchiveOutputStream out = new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(tempFile)));
        InputStream in = new FileInputStream(srcFile)) {
        out.putArchiveEntry(out.createArchiveEntry(srcFile, archiveFilename));
        IOUtils.copy(in, out);
        out.closeArchiveEntry();
        out.finish();
      return new FileInputStream(tempFile);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
