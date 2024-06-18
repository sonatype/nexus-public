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

import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreMetrics;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsPropertiesReader;
import org.sonatype.nexus.common.property.ImplicitSourcePropertiesFile;

import com.google.common.collect.ImmutableMap;

import static java.lang.Long.parseLong;
import static org.sonatype.nexus.blobstore.api.OperationType.DOWNLOAD;
import static org.sonatype.nexus.blobstore.api.OperationType.UPLOAD;

/**
 * Read blob store metrics from provided properties file
 */
public abstract class BlobStoreMetricsPropertiesReaderSupport<T extends ImplicitSourcePropertiesFile>
    extends ComponentSupport
    implements BlobStoreMetricsPropertiesReader
{

  protected abstract T getProperties() throws Exception;

  protected abstract Map<String, Long> getAvailableSpace() throws Exception;

  private T loadedProperties;

  @Override
  public BlobStoreMetrics readMetrics() throws Exception {
    T propertiesFile = loadProperties();
    if (propertiesFile == null) {
      return null;
    }
    long blobCount = parseLong(propertiesFile.getProperty("blobCount", "0"));
    long totalSize = parseLong(propertiesFile.getProperty("totalSize", "0"));
    return new AccumulatingBlobStoreMetrics(blobCount, totalSize, getAvailableSpace(), false);
  }

  @Override
  public Map<OperationType, OperationMetrics> readOperationMetrics() throws Exception {
    T propertiesFile = loadProperties();
    if (propertiesFile == null) {
      log.warn("{} not found for blob store", metricsFilename());
      return Collections.emptyMap();
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

  private T loadProperties() throws Exception {
    if (loadedProperties != null) {
      return loadedProperties;
    }

    loadedProperties = getProperties();
    if (loadedProperties.exists()) {
      loadedProperties.load();
    }
    return loadedProperties;
  }
}
