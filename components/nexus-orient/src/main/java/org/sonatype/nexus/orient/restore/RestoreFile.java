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
package org.sonatype.nexus.orient.restore;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a file that can be used to restore a prior state of an OrientDB database.
 *
 * Constructor intentionally private, use {@link RestoreFile#newInstance(Path)}.
 *
 * Restore files have a specific filename format:
 * <pre>
 *   database_name-YYYY-MM-DD-HH-MM-SS[-nxrm_version].bak
 * </pre>
 * Examples:
 * <pre>
 *   component-2017-07-06-11-16-49-3.4.1.bak
 *   component-2017-07-06-11-16-49.bak
 * </pre>
 *
 * The nxrm_version will not be present for files generated pre-3.4.1.
 *
 * @since 3.5
 */
public class RestoreFile
{

  private static final String TIMESTAMP_FORMAT = "%1$tY-%1$tm-%1$td-%1$tH-%1$tM-%1$tS";

  private static final Pattern FILENAME_PATTERN = Pattern.compile(
    "(.*)-(\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2})(?:-(.*))?.bak");

  private final String timestamp;

  private final String databaseName;

  private final String version;

  private RestoreFile(final String timestamp, final String databaseName, @Nullable final String version) {
    this.timestamp = checkNotNull(timestamp);
    this.databaseName = checkNotNull(databaseName);
    this.version = version;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public String getDatabaseName() {
    return databaseName;
  }

  @Nullable
  public String getVersion() {
    return version;
  }

  /**
   * @param databaseName the name of the database (e.g. 'config')
   * @param timestamp the timestamp
   * @param version the nexus repository manager (e.g. '3.4.1')
   * @return a string that represents the valid {@link Path#getFileName()}
   */
  public static String formatFilename(String databaseName, LocalDateTime timestamp, String version) {
    return new StringBuilder()
        .append(databaseName).append("-")
        .append(String.format(TIMESTAMP_FORMAT, timestamp)).append("-")
        .append(version.replace("-SNAPSHOT", "")).append(".bak")
        .toString();
  }

  /**
   * @param path {@link Path} to the restore file
   * @return a new {@link RestoreFile} instance
   * @throws IllegalArgumentException if the filename does not match {@link #TIMESTAMP_FORMAT}
   */
  public static RestoreFile newInstance(Path path) {
    if (path != null) {
      Path basepath = path.getFileName();
      if (basepath != null) {
        String basename = basepath.toString();

        Matcher matcher = FILENAME_PATTERN.matcher(basename);
        if (matcher.matches()) {
          String databaseName = matcher.group(1);
          String timestamp = matcher.group(2);
          String version = matcher.group(3);

          return new RestoreFile(timestamp, databaseName, version);
        }
      }
    }
    throw new IllegalArgumentException(path + " is not a valid restore file");
  }

  @Override
  public String toString() {
    return "{" +
        "databaseName=" + databaseName +
        ", timestamp=" + timestamp +
        ", version=" + version +
        "}";
  }
}
