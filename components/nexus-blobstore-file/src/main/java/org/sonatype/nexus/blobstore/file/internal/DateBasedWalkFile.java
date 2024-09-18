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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.time.UTC;

import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.api.BlobRef.DATE_TIME_PATH_FORMATTER;

/**
 * Walks the blob storage to find all files that have been created since a given duration.
 */
public class DateBasedWalkFile
    extends ComponentSupport
{
  private static final String PROPS_EXT = ".properties";

  private static final String BYTES_EXT = ".bytes";

  private final String contentDir;

  private final Duration duration;

  public DateBasedWalkFile(final String contentDir, final Duration duration) {
    checkNotNull(contentDir);
    this.contentDir = StringUtils.appendIfMissing(contentDir, File.separator);
    this.duration = checkNotNull(duration);
  }

  public Map<String, OffsetDateTime> getBlobIdToDateRef() {
    OffsetDateTime now = UTC.now();
    OffsetDateTime fromDateTime = now.minusSeconds(duration.getSeconds());
    String datePathPrefix = contentDir + buildDatePathPrefix(fromDateTime, now);

    try {
      return getAllFiles(datePathPrefix, fromDateTime);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String buildDatePathPrefix(final OffsetDateTime fromDateTime, final OffsetDateTime toDateTime)
  {
    StringBuilder datePathPrefix = new StringBuilder();
    if (fromDateTime.getYear() == toDateTime.getYear()) {
      datePathPrefix.append("yyyy").append("/");
      if (fromDateTime.getMonth().getValue() == toDateTime.getMonth().getValue()) {
        datePathPrefix.append("MM").append("/");
        if (fromDateTime.getDayOfMonth() == toDateTime.getDayOfMonth()) {
          datePathPrefix.append("dd").append("/");
          if (fromDateTime.getHour() == toDateTime.getHour()) {
            datePathPrefix.append("HH").append("/");
            if (fromDateTime.getMinute() == toDateTime.getMinute()) {
              datePathPrefix.append("mm").append("/");
            }
          }
        }
      }
    }
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(datePathPrefix.toString());

    return toDateTime.format(dateTimeFormatter);
  }

  private Map<String, OffsetDateTime> getAllFiles(final String startDir, final OffsetDateTime fromDateTime)
      throws IOException
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
          OffsetDateTime blobCreated = parseDate(contentDir, file.toString());
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

  private OffsetDateTime parseDate(final String contentDir, final String path) {
    // Extract the date-time part from the path "2024/01/01/13/10/" (from year to minutes)
    String dateTimeString = path.split(contentDir)[1].substring(0, 16);
    try {
      LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, DATE_TIME_PATH_FORMATTER);
      return localDateTime.atOffset(ZoneOffset.UTC);
    }
    catch (DateTimeParseException e) {
      // we don't care about the files that are not in the expected format
      log.debug("Incorrect date format in path: {}", path);
      return null;
    }
  }
}
