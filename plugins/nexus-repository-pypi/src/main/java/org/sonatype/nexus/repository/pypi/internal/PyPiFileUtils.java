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

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.pypi.internal.PyPiArchiveType.TAR;
import static org.sonatype.nexus.repository.pypi.internal.PyPiArchiveType.TAR_BZ2;
import static org.sonatype.nexus.repository.pypi.internal.PyPiArchiveType.TAR_GZ;
import static org.sonatype.nexus.repository.pypi.internal.PyPiArchiveType.TAR_LZ;
import static org.sonatype.nexus.repository.pypi.internal.PyPiArchiveType.TAR_XZ;
import static org.sonatype.nexus.repository.pypi.internal.PyPiArchiveType.TAR_Z;
import static org.sonatype.nexus.repository.pypi.internal.PyPiArchiveType.ZIP;

/**
 * Utility methods for working with PyPI filenames.
 *
 * @since 3.1
 */
public final class PyPiFileUtils
{
  static final Pattern VERSION_NUMBER_PATTERN = Pattern.compile("\\-\\d");

  // The various supported extensions are taken from https://github.com/pypa/pip/blob/develop/pip/utils/__init__.py
  static final Map<String, PyPiArchiveType> SUPPORTED_EXTENSIONS = new ImmutableMap.Builder<String, PyPiArchiveType>()
      .put(".tar.bz2", TAR_BZ2)
      .put(".tbz", TAR_BZ2)
      .put(".tar.gz", TAR_GZ)
      .put(".tgz", TAR_GZ)
      .put(".tlz", TAR_LZ)
      .put(".tar.lz", TAR_LZ)
      .put(".tar.lzma", TAR_LZ)
      .put(".tar.xz", TAR_XZ)
      .put(".txz", TAR_XZ)
      .put(".tar.z", TAR_Z)
      .put(".tz", TAR_Z)
      .put(".taz", TAR_Z)
      .put(".tar", TAR)
      .put(".zip", ZIP)
      .put(".whl", ZIP)
      .put(".egg", ZIP)
      .build();

  /**
   * Returns the filename portion of the path.
   */
  public static String extractFilenameFromPath(final String path) {
    checkNotNull(path);
    return path.substring(path.lastIndexOf('/') + 1);
  }

  /**
   * Extracts the name of the component from the filename. This is a best guess and should only be used when no other
   * information (such as metadata) is available.
   */
  static String extractNameFromFilename(final String filename) {
    checkNotNull(filename);
    return filename.substring(0, getFilenameVersionStart(filename));
  }

  /**
   * Extracts a package version from a filename based on the extension. For .whl files, the version number should always
   * be the second element delimited by dashes. For .egg files, the version number will either be the last element
   * delimited by dashes, or will be the element before the python version number. Other file extensions are treated
   * the same as the .egg format as it should hopefully degrade gracefully for the majority of cases.
   */
  public static String extractVersionFromFilename(final String filename) {
    checkNotNull(filename);
    String base = removeExtension(filename);
    int begin;
    int end;
    if (filename.endsWith(".whl")) {
      begin = base.indexOf('-') + 1;
      end = base.indexOf('-', begin);
    }
    else {
      begin = getFilenameVersionStart(filename) + 1;
      end = base.indexOf('-', begin);
      if (end == -1) {
        end = base.length();
      }
    }
    return base.substring(begin, end);
  }

  /**
   * Returns the best guess as to the start of the version in an archive filename.
   */
  private static int getFilenameVersionStart(final String filename) {
    Matcher matcher = VERSION_NUMBER_PATTERN.matcher(filename);
    if (matcher.find()) {
      return matcher.start();
    }
    if (filename.contains("-")) {
      return filename.indexOf('-');
    }
    return 0;
  }

  /**
   * Extracts the filename without extension. If the filename cannot be matched to one of the known extensions, then
   * the portion of the filename through the last dot is used. If no dot is found, the entire filename is used.
   */
  static String removeExtension(final String filename) {
    checkNotNull(filename);
    final String lowercaseFilename = filename.toLowerCase(Locale.ENGLISH);
    for (String extension : SUPPORTED_EXTENSIONS.keySet()) {
      if (lowercaseFilename.endsWith(extension)) {
        return filename.substring(0, lowercaseFilename.lastIndexOf(extension));
      }
    }
    if (filename.contains(".")) {
      return filename.substring(0, filename.lastIndexOf('.'));
    }
    return filename;
  }

  /**
   * Extracts the extension from the filename. If the filename cannot be matched to one of the known extensions, then
   * the portion of the filename after the last dot is used. If no dot is found, an empty string is returned.
   */
  static String extractExtension(final String filename) {
    checkNotNull(filename);
    final String lowercaseFilename = filename.toLowerCase(Locale.ENGLISH);
    for (String extension : SUPPORTED_EXTENSIONS.keySet()) {
      if (lowercaseFilename.endsWith(extension)) {
        return extension;
      }
    }
    if (filename.contains(".")) {
      return lowercaseFilename.substring(lowercaseFilename.lastIndexOf('.'));
    }
    return "";
  }

  private PyPiFileUtils() {
    // empty
  }
}
