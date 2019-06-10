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
package org.sonatype.nexus.repository.apt.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFileParser;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.io.input.CloseShieldInputStream;
// NOTE: replace with commons-compress ArArchiveInputStream once fixes to end of stream detection are
// available in a public release.
import org.sonatype.nexus.repository.apt.internal.org.apache.commons.compress.archivers.ar.ArArchiveInputStream;

/**
 * @since 3.next
 */
public class AptPackageParser
{
  private AptPackageParser() {
    throw new IllegalAccessError("Utility class");
  }

  public static ControlFile parsePackage(final Supplier<InputStream> supplier) throws IOException {
    try (ArArchiveInputStream is = new ArArchiveInputStream(supplier.get())) {
      ControlFile control = null;
      ArchiveEntry debEntry;
      while ((debEntry = is.getNextEntry()) != null) {
        InputStream controlStream;
        switch (debEntry.getName()) {
          case "control.tar":
            controlStream = new CloseShieldInputStream(is);
            break;
          case "control.tar.gz":
            controlStream = new GzipCompressorInputStream(new CloseShieldInputStream(is));
            break;
          case "control.tar.xz":
            controlStream = new XZCompressorInputStream(new CloseShieldInputStream(is));
            break;
          default:
            continue;
        }

        try (TarArchiveInputStream controlTarStream = new TarArchiveInputStream(controlStream)) {
          ArchiveEntry tarEntry;
          while ((tarEntry = controlTarStream.getNextEntry()) != null) {
            if ("control".equals(tarEntry.getName()) || "./control".equals(tarEntry.getName())) {
              control = new ControlFileParser().parseControlFile(controlTarStream);
            }
          }
        }
      }
      return control;
    }
  }

  public static ControlFile getDebControlFile(final Blob blob)
      throws IOException
  {
    final ControlFile controlFile = AptPackageParser.parsePackage(() -> blob.getInputStream());
    if (controlFile == null) {
      throw new IOException("Invalid debian package: no control file");
    }
    return controlFile;
  }
}
