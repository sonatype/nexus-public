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
package org.sonatype.nexus.blobstore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.common.app.ApplicationDirectories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.DefaultBlobIdLocationResolver.TEMPORARY_BLOB_ID_PREFIX;

/**
 * Helper class for storing and retrieving reconciliation log for newly created blob store. Configuration is stored in
 * logback.xml. Logback will be responsible for rolling the files, and all we need to do is just call it via this class
 * that sets the context properties, so each blob store has its own reconciliation log file. Reconciliation logs are
 * stored at &lt;blobstore root&gt;/reconciliation/%date.log
 */
@Singleton
public class BlobStoreReconciliationLogger
{
  public static final String BLOBSTORE = "blobstore-reconciliation-path";

  private static final String RECONCILIATION_LOGGER_NAME = "blobstore-reconciliation-log";

  private static final Logger LOGGER = LoggerFactory.getLogger(BlobStoreReconciliationLogger.class);

  private final Logger reconciliationLogger;

  private final ApplicationDirectories applicationDirectories;

  @Inject
  public BlobStoreReconciliationLogger(final ApplicationDirectories applicationDirectories) {
    this.applicationDirectories = checkNotNull(applicationDirectories);
    this.reconciliationLogger = LoggerFactory.getLogger(RECONCILIATION_LOGGER_NAME);
  }

  /**
   * Add new entry in rolling log later used in reconciliation task.
   * 
   * @param reconciliationLogPath The path to the blob store's reconciliation log directory
   * @param blobId id of blob created
   */
  public void logBlobCreated(final Path reconciliationLogPath, final BlobId blobId) {
    if (isNotTemporaryBlob(blobId)) {
      MDC.put(BLOBSTORE, reconciliationLogPath.toString());
      // blobId.getBlobCreatedRef() != null means the blob was stored under the date-based layout
      reconciliationLogger.info("{},{}", blobId.asUniqueString(), blobId.getBlobCreatedRef() != null);
      MDC.remove(BLOBSTORE);
    }
  }

  private boolean isNotTemporaryBlob(final BlobId blobId) {
    return !blobId.asUniqueString().startsWith(TEMPORARY_BLOB_ID_PREFIX);
  }

  /**
   * Stream blob ids of blobs created in a blob store within date range (inclusive).
   *
   * @param reconciliationLogPath The path to the blob store's reconciliation log directory
   * @param fromDate The date from which range starts
   * @param toDate The date from which range ends
   * @param dateBasedBlobIds date-based blob ids
   *
   * @return stream of BlobId
   */
  public Stream<BlobId> getBlobsCreatedSince(
      final Path reconciliationLogPath,
      final LocalDateTime fromDate,
      final LocalDateTime toDate,
      final Map<String, OffsetDateTime> dateBasedBlobIds)
  {
    return getLogFilesToProcess(reconciliationLogPath, fromDate, toDate)
        .flatMap(this::readLines)
        .map(line -> {
          String[] split = line.split(",");
          if (split.length == 2) {
            return split[1];
          }
          else if (split.length == 3) {
            String blobId = split[1];
            if (Boolean.parseBoolean(split[2])) {
              // we already have date-based blob ids, so we can skip them
              return dateBasedBlobIds.get(blobId) != null ? blobId : null;
            }
            else {
              return blobId;
            }
          }
          else {
            LOGGER.info("Cannot find blob id on line, skipping: {}", line);
            return null;
          }
        })
        .filter(Objects::nonNull)
        .map(id -> new BlobId(id, dateBasedBlobIds.get(id) != null ? dateBasedBlobIds.get(id) : null));
  }

  private Stream<String> readLines(final File file) {
    try {
      return Files.lines(file.toPath());
    }
    catch (IOException e) {
      LOGGER.error("Problem when reading file '{}'", file.getName(), e);
      return Stream.empty();
    }
  }

  private Stream<File> getLogFilesToProcess(
      final Path reconciliationLogPath,
      final LocalDateTime fromDate,
      final LocalDateTime toDate)
  {
    File reconciliationLogDirectory = applicationDirectories.getWorkDirectory(reconciliationLogPath.toString());
    File[] logs = reconciliationLogDirectory.listFiles();
    if (Objects.nonNull(logs)) {
      return Stream.of(logs)
          .filter(isFileNameInDateRange(fromDate, toDate))
          .peek(file -> LOGGER.info("Processing file '{}'", file.getName()));
    }
    else {
      LOGGER.info("No files found to process");
      return Stream.empty();
    }
  }

  private Predicate<File> isFileNameInDateRange(final LocalDateTime fromDate, final LocalDateTime toDate) {
    return file -> {
      try {
        LocalDate logFileDate = LocalDate.parse(file.getName());
        return !fromDate.toLocalDate().isAfter(logFileDate) && !toDate.toLocalDate().isBefore(logFileDate);
      }
      catch (DateTimeParseException e) {
        return false;
      }
    };
  }
}
