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
package org.sonatype.nexus.repository.r.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.sonatype.nexus.common.io.InputStreamSupplier;
import org.sonatype.nexus.repository.r.internal.RException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.compress.archivers.ArchiveStreamFactory.TAR;
import static org.apache.commons.compress.archivers.ArchiveStreamFactory.ZIP;
import static org.apache.commons.compress.compressors.CompressorStreamFactory.GZIP;
import static org.sonatype.nexus.repository.r.internal.util.RMetadataUtils.parseDescriptionFile;

/**
 * Utility methods for working with R metadata.
 *
 * @see <a href="https://cran.r-project.org/doc/manuals/r-release/R-exts.html#The-DESCRIPTION-file">DESCRIPTION</a>
 *
 * @since 3.28
 */
public final class RDescriptionUtils
{
  private static final Pattern DESCRIPTION_FILE_PATTERN = Pattern.compile("^[^/]*/DESCRIPTION$");

  /**
   * Extracts the DESCRIPTION contents from the tgz or zip.
   */
  public static Map<String, String> extractDescriptionFromArchive(
      final String filename,
      final InputStreamSupplier inputStreamSupplier)
  {
    try (InputStream is = inputStreamSupplier.get()) {
      checkNotNull(filename);
      checkNotNull(is);
      final String lowerCaseFilename = filename.toLowerCase();
      if (lowerCaseFilename.endsWith(".tar.gz") || lowerCaseFilename.endsWith(".tgz")) {
        return extractMetadataFromTgz(is);
      }
      else if (lowerCaseFilename.endsWith(".gz")) {
        return extractMetadataFromGz(is);
      }
      else if (lowerCaseFilename.endsWith(".zip")) {
        return extractMetadataFromZip(is);
      }
      throw new IllegalStateException("Unexpected file extension for file: " + filename);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Extracts the DESCRIPTION contents from the tgz or zip.
   *
   * @deprecated Use {@link #extractDescriptionFromArchive(String, InputStreamSupplier)}
   */
  @Deprecated
  public static Map<String, String> extractDescriptionFromArchive(final String filename, InputStream is) {
    return extractDescriptionFromArchive(filename, () -> is);
  }

  private static Map<String, String> extractMetadataFromTgz(final InputStream is) {
    checkNotNull(is);
    try {
      final CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory();
      try (InputStream cis = compressorStreamFactory.createCompressorInputStream(GZIP, is)) {
        return extractMetadataFromArchive(TAR, cis);
      }
    }
    catch (CompressorException | IOException e) {
      throw new RException(null, e);
    }
  }

  private static Map<String, String> extractMetadataFromGz(final InputStream in) {
    try {
      try (GZIPInputStream gz = new GZIPInputStream(in)) {
        return parseDescriptionFile(gz);
      }
    }
    catch ( IOException e) {
      throw new RException(null, e);
    }
  }

  private static Map<String, String> extractMetadataFromZip(final InputStream is) {
    checkNotNull(is);
    return extractMetadataFromArchive(ZIP, is);
  }

  private static Map<String, String> extractMetadataFromArchive(final String archiveType, final InputStream is) {
    final ArchiveStreamFactory archiveFactory = new ArchiveStreamFactory();
    try (ArchiveInputStream ais = archiveFactory.createArchiveInputStream(archiveType, is)) {
      ArchiveEntry entry = ais.getNextEntry();
      while (entry != null) {
        if (!entry.isDirectory() && DESCRIPTION_FILE_PATTERN.matcher(entry.getName()).matches()) {
          return parseDescriptionFile(ais);
        }
        entry = ais.getNextEntry();
      }
    }
    catch (ArchiveException | IOException e) {
      throw new RException(null, e);
    }
    throw new IllegalStateException("No metadata file found");
  }

  private RDescriptionUtils() {
    // empty
  }
}
