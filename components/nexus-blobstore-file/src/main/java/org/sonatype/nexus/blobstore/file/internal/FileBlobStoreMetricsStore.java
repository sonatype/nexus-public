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
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.AccumulatingBlobStoreMetrics;
import org.sonatype.nexus.blobstore.BlobStoreMetricsStoreSupport;
import org.sonatype.nexus.scheduling.PeriodicJobService;
import org.sonatype.nexus.blobstore.quota.BlobStoreQuotaService;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.property.PropertiesFile;

import com.google.common.collect.ImmutableMap;

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
{
  private Path storageDirectory;

  private final FileOperations fileOperations;

  @Inject
  public FileBlobStoreMetricsStore(final PeriodicJobService jobService,
                                   final NodeAccess nodeAccess,
                                   final BlobStoreQuotaService quotaService,
                                   @Named("${nexus.blobstore.quota.warnIntervalSeconds:-60}")
                                   final int quotaCheckInterval,
                                   final FileOperations fileOperations)
  {
    super(nodeAccess, jobService, quotaService, quotaCheckInterval);
    this.fileOperations = checkNotNull(fileOperations);
  }

  @Override
  protected PropertiesFile getProperties() {
    Path metricsDataFile = storageDirectory.resolve(nodeAccess.getId() + "-" + METRICS_FILENAME);
    return new PropertiesFile(metricsDataFile.toFile());
  }

  @Override
  protected AccumulatingBlobStoreMetrics getAccumulatingBlobStoreMetrics() {
    try {
      FileStore fileStore = Files.getFileStore(storageDirectory);
      ImmutableMap<String, Long> availableSpace = ImmutableMap
          .of("fileStore:" + fileStore.name(), fileStore.getUsableSpace());
      return new AccumulatingBlobStoreMetrics(0, 0, availableSpace, false);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Stream<PropertiesFile> backingFiles() {
    if (storageDirectory == null) {
      return Stream.empty();
    }
    return Optional.ofNullable(storageDirectory.toFile().listFiles((dir, name) -> name.endsWith(METRICS_FILENAME)))
        .map(files -> Arrays.stream(files).filter(Objects::nonNull).map(PropertiesFile::new))
        .orElse(Stream.empty());
  }

  public void setStorageDir(final Path storageDirectory) {
    checkNotNull(storageDirectory);
    checkArgument(Files.isDirectory(storageDirectory));
    this.storageDirectory = storageDirectory;
  }

  @Override
  public void remove() {
    backingFiles()
        .map(PropertiesFile::getFile)
        .map(File::toPath)
        .forEach(fileOperations::deleteQuietly);
  }
}
