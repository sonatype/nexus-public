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
package org.sonatype.nexus.blobstore.file.internal.datastore.metrics;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Named;

import org.sonatype.nexus.blobstore.AccumulatingBlobStoreMetrics;
import org.sonatype.nexus.blobstore.BlobStoreMetricsNotAvailableException;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.blobstore.metrics.BlobStoreMetricsPropertiesReaderSupport;
import org.sonatype.nexus.common.property.PropertiesFile;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * A {@link BlobStoreMetricsPropertiesReaderSupport} implementation that retains blobstore metrics in memory, periodically
 * writing them out to a file.
 *
 * @since 3.0
 * @deprecated legacy method for metrics stored in the blob store
 */
@Deprecated
@Named(FileBlobStore.TYPE)
public class FileBlobStoreMetricsPropertiesReader
    extends BlobStoreMetricsPropertiesReaderSupport<FileBlobStore, PropertiesFile>
{
  private Path storageDirectory;

  @Override
  protected void doInit(final FileBlobStore blobstore) throws IOException {
    Path storageDir = blobStore.getAbsoluteBlobDir();

    checkNotNull(storageDir);
    checkArgument(Files.isDirectory(storageDir));
    this.storageDirectory = storageDir;
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
  protected Stream<PropertiesFile> backingFiles() throws BlobStoreMetricsNotAvailableException {
    if (storageDirectory == null) {
      return Stream.empty();
    }
    try (DirectoryStream<Path> files =
         Files.newDirectoryStream(storageDirectory, path -> path.toString().endsWith(METRICS_FILENAME))) {
      return StreamSupport.stream(files.spliterator(), false)
          .map(Path::toFile)
          .map(PropertiesFile::new)
          // we need a terminal operation since the directory stream will be closed
          .collect(toList())
          .stream();
    }
    catch (IOException | SecurityException e) {
      throw new BlobStoreMetricsNotAvailableException(e);
    }
  }
}
