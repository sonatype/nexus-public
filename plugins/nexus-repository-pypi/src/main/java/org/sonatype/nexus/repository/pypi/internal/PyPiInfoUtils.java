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
package org.sonatype.nexus.repository.pypi.internal;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.mail.Header;
import javax.mail.internet.InternetHeaders;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_ARCHIVE_TYPE;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_CLASSIFIERS;
import static org.sonatype.nexus.repository.pypi.internal.PyPiConstants.DIST_INFO_SUFFIX;
import static org.sonatype.nexus.repository.pypi.internal.PyPiConstants.EGG_INFO_FILENAME;
import static org.sonatype.nexus.repository.pypi.internal.PyPiConstants.EGG_INFO_SUFFIX;
import static org.sonatype.nexus.repository.pypi.internal.PyPiConstants.METADATA_FILENAME;
import static org.sonatype.nexus.repository.pypi.internal.PyPiConstants.PKG_INFO_FILENAME;

/**
 * Utility methods for working with PyPI package info following the PEP 241 and PEP 314 specifications.
 *
 * @since 3.1
 */
public final class PyPiInfoUtils
{

  private static final Logger log = LoggerFactory.getLogger(PyPiInfoUtils.class);

  /**
   * Names of attributes stored as RFC 822 headers don't always match what the attributes are called by the client on
   * upload. This map is a nice place to put the ones that have to be replaced for consistency.
   */
  private static final Map<String, String> NAME_SUBSTITUTIONS = new ImmutableMap.Builder<String, String>()
      .put("classifier", P_CLASSIFIERS)
      .build();

  /**
   * Extracts metadata from archive files, returning an empty map on failure.
   */
  static Map<String, String> extractMetadata(InputStream is) {
    checkNotNull(is);
    Map<String, Exception> exceptions = new HashMap<>();

    try {
      String compressionType = CompressorStreamFactory.detect(is);
      try (CompressorInputStream cis = CompressorStreamFactory.getSingleton()
          .createCompressorInputStream(compressionType, is)) {
        InputStream decompressedInput = new BufferedInputStream(cis);
        String archiveType = ArchiveStreamFactory.detect(decompressedInput);
        Map<String, String> metadata = extractMetadataFromArchive(archiveType, decompressedInput);
        metadata.put(P_ARCHIVE_TYPE, archiveType + '.' + compressionType);
        return metadata;
      }
    }
    catch (Exception e) {
      exceptions.put("Unable to decompress PyPI archive", e);
    }

    try {
      String archiveType = ArchiveStreamFactory.detect(is);
      Map<String, String> metadata = extractMetadataFromArchive(archiveType, is);
      metadata.put(P_ARCHIVE_TYPE, archiveType);
      return metadata;
    }
    catch (Exception e) {
      exceptions.put("Unable to extract PyPI archive", e);
    }

    for (Map.Entry<String, Exception> entry : exceptions.entrySet()) {
      log.error(entry.getKey(), entry.getValue());
    }
    return new LinkedHashMap<>();
  }

  /**
   * Extracts metadata from an archive directly (such as for tar and zip formats).
   */
  private static Map<String, String> extractMetadataFromArchive(final String archiveType,
                                                                final InputStream is)
      throws Exception
  {
    checkNotNull(archiveType);
    checkNotNull(is);

    final ArchiveStreamFactory archiveFactory = new ArchiveStreamFactory();
    try (ArchiveInputStream ais = archiveFactory.createArchiveInputStream(archiveType, is)) {
      return processArchiveEntries(ais);
    }
  }

  /**
   * Processes the entries in an archive, attempting to extract metadata. The first possible metadata file found wins.
   */
  private static Map<String, String> processArchiveEntries(final ArchiveInputStream ais) throws Exception {
    checkNotNull(ais);
    ArchiveEntry entry = ais.getNextEntry();
    while (entry != null) {
      if (isMetadataFile(entry)) {
        Map<String, String> results = new LinkedHashMap<>();
        for (Entry<String, List<String>> attribute : parsePackageInfo(ais).entrySet()) {
          results.put(attribute.getKey(), String.join("\n", attribute.getValue()));
        }
        return results;
      }
      entry = ais.getNextEntry();
    }
    return new LinkedHashMap<>();
  }

  /**
   * Parses the PKG-INFO content as RFC 822 headers (per PEPs 241 and 314). (Yes, Python PKG-INFO information is
   * essentially stored as a file of email headers.)
   */
  @VisibleForTesting
  static Map<String, List<String>> parsePackageInfo(InputStream in) throws Exception {
    checkNotNull(in);
    LinkedHashMap<String, List<String>> results = new LinkedHashMap<>();

    // All package info or metadata file types have their metadata stored in the same manner as email headers
    InternetHeaders headers = new InternetHeaders(in);
    Enumeration headerEnumeration = headers.getAllHeaders();
    while (headerEnumeration.hasMoreElements()) {
      Header header = (Header) headerEnumeration.nextElement();
      String underscoreName = header.getName().toLowerCase().replace('-', '_');
      String name = NAME_SUBSTITUTIONS.getOrDefault(underscoreName, underscoreName);
      String value = convertHeaderValue(header.getValue());
      if (!results.containsKey(name)) {
        results.put(name, new ArrayList<>());
      }
      results.get(name).add(value);
    }

    // Wheel metadata can also be stored in the payload section (description only, so far as I'm aware)
    if (!results.containsKey(PyPiAttributes.P_DESCRIPTION)) {
      String text =  Strings.nullToEmpty(CharStreams.toString(new InputStreamReader(in, StandardCharsets.UTF_8))).trim();
      if (!text.isEmpty()) {
        List<String> description = new ArrayList<>();
        description.add(text.replace("\r\n", "\n").replaceAll("[ ]*\\n[ ]*", "\n") + "\n");
        results.put(PyPiAttributes.P_DESCRIPTION, description);
      }
    }

    return results;
  }

  /**
   * PyPI permits UTF-8 characters in the PKG-INFO header values, while Java does not expect to see these. Also, we
   * need to normalize the various spaces and CR-LF sequences to something a bit more sane. This method should handle
   * both of these issues.
   */
  private static String convertHeaderValue(final String value) {
    if (value == null || value.trim().isEmpty()) {
      return "";
    }
    return new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)
        .replace("\r\n", "\n")
        .replaceAll("[ ]*\\n[ ]*", "\n");
  }

  /**
   * Determines if an {@link ArchiveEntry} is a metadata file.
   */
  private static boolean isMetadataFile(final ArchiveEntry entry) {
    checkNotNull(entry);
    if (entry.isDirectory()) {
      return false;
    }
    String[] parts = entry.getName().split("/");
    return isEggInfoPath(parts) || isPackageInfoFilePath(parts) || isMetadataFilePath(parts);
  }

  /**
   * Returns a boolean indicating if the path parts represent an egg-info metadata file.
   */
  private static boolean isEggInfoPath(final String[] parts) {
    checkNotNull(parts);
    if (parts.length == 2 && EGG_INFO_FILENAME.equals(parts[1])) {
      return true;
    }
    if (parts.length == 1) {
      return EGG_INFO_FILENAME.equals(parts[0]) || parts[0].endsWith(EGG_INFO_SUFFIX);
    }
    return false;
  }

  /**
   * Returns a boolean indicating if the path parts represent a metadata file path.
   */
  private static boolean isMetadataFilePath(final String[] parts) {
    checkNotNull(parts);
    if (parts.length == 2 && parts[0].endsWith(DIST_INFO_SUFFIX) && METADATA_FILENAME.equals(parts[1])) {
      return true;
    }
    return false;
  }

  /**
   * Returns a boolean indicating if the path parts represent a pkg-info file path.
   */
  private static boolean isPackageInfoFilePath(final String[] parts) {
    checkNotNull(parts);
    if (parts.length == 3 && parts[1].endsWith(EGG_INFO_SUFFIX) && PKG_INFO_FILENAME.equals(parts[2])) {
      return true;
    }
    if (parts.length == 2 && EGG_INFO_FILENAME.equals(parts[0]) && PKG_INFO_FILENAME.equals(parts[1])) {
      return true;
    }
    if (parts.length == 2 && PKG_INFO_FILENAME.equals(parts[1])) {
      return true;
    }
    return false;
  }

  private PyPiInfoUtils() {
    // empty
  }
}
