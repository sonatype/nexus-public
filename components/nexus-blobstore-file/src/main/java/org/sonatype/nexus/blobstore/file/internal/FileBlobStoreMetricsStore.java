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
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.AccumulatingBlobStoreMetrics;
import org.sonatype.nexus.blobstore.BlobStoreMetricsNotAvailableException;
import org.sonatype.nexus.blobstore.BlobStoreMetricsStoreSupport;
import org.sonatype.nexus.blobstore.api.OperationMetrics;
import org.sonatype.nexus.blobstore.api.OperationType;
import org.sonatype.nexus.blobstore.file.FileBlobStoreMetricsService;
import org.sonatype.nexus.scheduling.PeriodicJobService;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.property.PropertiesFile;

import com.google.common.collect.ImmutableMap;

import static java.util.stream.Collectors.toList;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link BlobStoreMetricsStoreSupport} implementation that retains blobstore metrics in memory, periodically
 * writing them out to a file.
 *
 * @since 3.0
 */
@Named
public class FileBlobStoreMetricsStore
    extends BlobStoreMetricsStoreSupport<PropertiesFile>
    implements FileBlobStoreMetricsService
{
  private Path storageDirectory;

  private final FileOperations fileOperations;

  @Inject
  public FileBlobStoreMetricsStore(final PeriodicJobService jobService,
                                   final NodeAccess nodeAccess,
                                   final FileOperations fileOperations)
  {
    super(nodeAccess, jobService);
    this.fileOperations = checkNotNull(fileOperations);
  }

  @Override
  protected PropertiesFile getProperties() {
    Path metricsDataFile = storageDirectory.resolve(nodeAccess.getId() + "-" + METRICS_FILENAME);
    return new PropertiesFile(metricsDataFile.toFile());
  }

  @Override
  protected AccumulatingBlobStoreMetrics getAccumulatingBlobStoreMetrics() throws BlobStoreMetricsNotAvailableException {
    try {
      FileStore fileStore = Files.getFileStore(storageDirectory);
      ImmutableMap<String, Long> availableSpace = ImmutableMap
          .of("fileStore:" + fileStore.name(), fileStore.getUsableSpace());
      return new AccumulatingBlobStoreMetrics(0, 0, availableSpace, false);
    }
    catch (IOException e) {
      throw new BlobStoreMetricsNotAvailableException(e);
    }
  }

  @Override
  public Stream<PropertiesFile> backingFiles() throws BlobStoreMetricsNotAvailableException {
    if (storageDirectory == null) {
      return Stream.empty();
    }
    try (DirectoryStream<Path> files =
         Files.newDirectoryStream(storageDirectory, path -> path.toString().endsWith(METRICS_FILENAME))) {
      return StreamSupport.stream(files.spliterator(), false).collect(toList()).stream()
          .map(Path::toFile)
          .map(PropertiesFile::new);
    }
    catch (IOException | SecurityException e) {
      throw new BlobStoreMetricsNotAvailableException(e);
    }
  }

  public void setStorageDir(final Path storageDirectory) {
    checkNotNull(storageDirectory);
    checkArgument(storageDirectory.toFile().isDirectory());
    this.storageDirectory = storageDirectory;
  }

  @Override
  public Map<OperationType, OperationMetrics> getOperationMetricsDelta() {
    return getOperationMetrics();
  }

  @Override
  public void flush() throws IOException {
    super.flushProperties();
  }

  @Override
  public void remove() {
    try {
      backingFiles()
          .map(PropertiesFile::getFile)
          .map(File::toPath)
          .forEach(fileOperations::deleteQuietly);
    }
    catch (BlobStoreMetricsNotAvailableException e) {
      log.warn("Unable to remove metrics files", e);
    }
  }
}
