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
package org.sonatype.nexus.blobstore.file.internal;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.DateBasedHelper;
import org.sonatype.nexus.common.time.UTC;

import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.BlobStoreSupport.CONTENT_PREFIX;
import static org.sonatype.nexus.blobstore.api.BlobRef.DATE_TIME_PATH_FORMATTER;

/**
 * Walks the blob storage to find all files that have been created since a given duration.
 */
public class DateBasedWalkFile
    extends ComponentSupport
{
  // to support Unix and Windows file separators
  private static final String FILE_SEPARATOR = "[\\\\/]";

  // to match date, for example "2024/01/01/13/10"
  private static final String DATE_BASED_MATCHER =
      "(\\d{4}" + FILE_SEPARATOR + "\\d{2}" + FILE_SEPARATOR + "\\d{2}" + FILE_SEPARATOR + "\\d{2}" + FILE_SEPARATOR
          + "\\d{2})";

  private static final Pattern DATE_BASED_PATTERN = Pattern.compile(
      ".*" + CONTENT_PREFIX + FILE_SEPARATOR + DATE_BASED_MATCHER + FILE_SEPARATOR + ".*$", Pattern.CASE_INSENSITIVE);

  private static final String PROPS_EXT = ".properties";

  private static final String BYTES_EXT = ".bytes";

  private final String contentDir;

  private final Duration duration;

  private final OffsetDateTime fromDateTime;

  public DateBasedWalkFile(final String contentDir, final Duration duration) {
    this.fromDateTime = null;
    checkNotNull(contentDir);
    this.contentDir = StringUtils.appendIfMissing(contentDir, File.separator);
    this.duration = checkNotNull(duration);
  }

  public DateBasedWalkFile(final String contentDir, final OffsetDateTime fromDateTime) {
    this.duration = null;
    this.fromDateTime = checkNotNull(fromDateTime);
    checkNotNull(contentDir);
    this.contentDir = StringUtils.appendIfMissing(contentDir, File.separator);
  }

  public Map<String, OffsetDateTime> getBlobIdToDateRef() {
    OffsetDateTime now = UTC.now();
    OffsetDateTime from;

    if (duration != null) {
      from = now.minusSeconds(duration.getSeconds());
    }
    else {
      from = this.fromDateTime;
    }
    String datePathPrefix = contentDir + DateBasedHelper.getDatePathPrefix(from, now);

    try {
      return getAllFiles(datePathPrefix, from);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Map<String, OffsetDateTime> getBlobIdToDateRef(String datePathPrefix) {
    try {
      return getAllFiles(datePathPrefix, this.fromDateTime);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Map<String, OffsetDateTime> getAllFiles(
      final String startDir,
      final OffsetDateTime fromDateTime) throws IOException
  {
    Map<String, OffsetDateTime> blobIds = new HashMap<>();
    Path startPath = Paths.get(startDir);

    if (!Files.exists(startPath)) {
      return blobIds;
    }

    Files.walkFileTree(startPath, new SimpleFileVisitor<Path>()
    {
      @Override
      public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
        if (file.toString().endsWith(BYTES_EXT) || file.toString().endsWith(PROPS_EXT)) {
          OffsetDateTime blobCreated = parseDate(file);
          if (blobCreated == null) {
            return FileVisitResult.CONTINUE;
          }

          if (blobCreated.isAfter(fromDateTime)) {
            String fileName = file.getFileName().toString();
            // remove file extension
            int dotIndex = fileName.lastIndexOf('.');
            String id = fileName.substring(0, dotIndex);
            blobIds.put(id, blobCreated);
          }
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(final Path file, final IOException e) {
        log.error("Failed to access file: {} Error: {}", file, e.getMessage());
        return FileVisitResult.CONTINUE;
      }
    });

    return blobIds;
  }

  private OffsetDateTime parseDate(final Path path) {
    try {
      // Extract the date-time part from the path "2024/01/01/13/10/" (from year to minutes)
      Matcher matcher = DATE_BASED_PATTERN.matcher(path.toString());
      if (matcher.find()) {
        LocalDateTime localDateTime = LocalDateTime.parse(matcher.group(1), DATE_TIME_PATH_FORMATTER);
        return localDateTime.atOffset(ZoneOffset.UTC);
      }
    }
    catch (Exception e) {
      // we don't care about the files that are not in the expected format
      log.debug("Incorrect date format in path: {}", path);
      return null;
    }

    return null;
  }
}
