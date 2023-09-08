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
package org.sonatype.nexus.repository.content.blobstore.metrics.migration.internal;

import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.AccumulatingBlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.common.property.PropertiesFile;
import org.sonatype.nexus.repository.content.blobstore.metrics.migration.BlobStoreMetricsReader;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Long.parseLong;
import static java.util.stream.Collectors.toList;
import static org.sonatype.nexus.blobstore.api.OperationType.DOWNLOAD;
import static org.sonatype.nexus.blobstore.api.OperationType.UPLOAD;

/**
 * Read {@link BlobStore} metrics from external file (-metrics.properties)
 */
@Named
@Singleton
public class FileBasedBlobStoreMetricsReader
    implements BlobStoreMetricsReader
{
  private static final String METRICS_FILENAME = "metrics.properties";

  @Override
  @Nullable
  public BlobStoreMetrics readMetrics(final BlobStore blobStore) throws Exception {
    if (blobStore instanceof FileBlobStore) {
      Path storageDir = ((FileBlobStore) blobStore).getAbsoluteBlobDir();
      PropertiesFile propertiesFile = loadProperties(storageDir);
      if (propertiesFile == null) {
        return null;
      }

      long blobCount = parseLong(propertiesFile.getProperty("blobCount", "0"));
      long totalSize = parseLong(propertiesFile.getProperty("totalSize", "0"));

      FileStore fileStore = Files.getFileStore(storageDir);
      ImmutableMap<String, Long> availableSpace = ImmutableMap
          .of("fileStore:" + fileStore.name(), fileStore.getUsableSpace());

      return new AccumulatingBlobStoreMetrics(blobCount, totalSize, availableSpace, false);
    }
    return null;
  }

  @Override
  @Nullable
  public Map<OperationType, OperationMetrics> readOperationMetrics(final BlobStore blobStore) throws Exception {
    if (blobStore instanceof FileBlobStore) {
      Path storageDir = ((FileBlobStore) blobStore).getAbsoluteBlobDir();
      PropertiesFile propertiesFile = loadProperties(storageDir);
      if (propertiesFile == null) {
        return null;
      }

      long downloadTotalErrorRequests = parseLong(propertiesFile.getProperty("DOWNLOAD_totalErrorRequests", "0"));
      long downloadTotalSizeOnRequests = parseLong(propertiesFile.getProperty("DOWNLOAD_totalSizeOnRequests", "0"));
      long downloadTotalSuccessfulRequests =
          parseLong(propertiesFile.getProperty("DOWNLOAD_totalSuccessfulRequests", "0"));
      long downloadTotalTimeOnRequests = parseLong(propertiesFile.getProperty("DOWNLOAD_totalTimeOnRequests", "0"));

      OperationMetrics downloadMetrics = new OperationMetrics();
      downloadMetrics.setBlobSize(downloadTotalSizeOnRequests);
      downloadMetrics.setErrorRequests(downloadTotalErrorRequests);
      downloadMetrics.setSuccessfulRequests(downloadTotalSuccessfulRequests);
      downloadMetrics.setTimeOnRequests(downloadTotalTimeOnRequests);

      long uploadTotalSuccessfulRequests = parseLong(propertiesFile.getProperty("UPLOAD_totalSuccessfulRequests", "0"));
      long uploadTotalSizeOnRequests = parseLong(propertiesFile.getProperty("UPLOAD_totalSizeOnRequests", "0"));
      long uploadTotalTimeOnRequests = parseLong(propertiesFile.getProperty("UPLOAD_totalTimeOnRequests", "0"));
      long uploadTotalErrorRequests = parseLong(propertiesFile.getProperty("UPLOAD_totalErrorRequests", "0"));

      OperationMetrics uploadMetrics = new OperationMetrics();
      uploadMetrics.setBlobSize(uploadTotalSizeOnRequests);
      uploadMetrics.setErrorRequests(uploadTotalErrorRequests);
      uploadMetrics.setSuccessfulRequests(uploadTotalSuccessfulRequests);
      uploadMetrics.setTimeOnRequests(uploadTotalTimeOnRequests);

      return ImmutableMap.of(
          DOWNLOAD, downloadMetrics,
          UPLOAD, uploadMetrics);
    }
    return null;
  }

  @Nullable
  private PropertiesFile loadProperties(final Path storageDir) throws Exception {
    checkNotNull(storageDir);

    try (DirectoryStream<Path> files = Files.newDirectoryStream(storageDir,
        path -> path.toString().endsWith(METRICS_FILENAME))) {
      Optional<PropertiesFile> propertiesFile =
          StreamSupport.stream(files.spliterator(), false).collect(toList()).stream()
              .map(Path::toFile)
              .map(PropertiesFile::new)
              .findFirst();

      if (!propertiesFile.isPresent()) {
        return null;
      }
      propertiesFile.get().load();
      return propertiesFile.get();
    }
  }
}
