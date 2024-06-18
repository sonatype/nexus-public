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

import org.sonatype.nexus.blobstore.BlobStoreMetricsPropertiesReaderSupport;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.common.property.PropertiesFile;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * Read {@link BlobStore} metrics from external file (-metrics.properties)
 */
@Named("File")
@Singleton
public class FileBlobStoreMetricsPropertiesReader
    extends BlobStoreMetricsPropertiesReaderSupport<PropertiesFile>
{
  private Path storageDir;

  @Override
  public void initWithBlobStore(final BlobStore blobStore) throws Exception {
    if (!(blobStore instanceof FileBlobStore)) {
      throw new IllegalArgumentException("BlobStore must be of type FileBlobStore");
    }
    this.storageDir = ((FileBlobStore) blobStore).getAbsoluteBlobDir();
  }

  @Override
  protected PropertiesFile getProperties() throws Exception {
    return loadProperties(storageDir);
  }

  @Override
  protected Map<String, Long> getAvailableSpace() throws Exception {
    FileStore fileStore = Files.getFileStore(storageDir);
    return ImmutableMap.of("fileStore:" + fileStore.name(), fileStore.getUsableSpace());
  }

  @Nullable
  private PropertiesFile loadProperties(final Path storageDir) throws Exception {
    checkNotNull(storageDir);

    try (DirectoryStream<Path> files = Files.newDirectoryStream(storageDir,
        path -> path.toString().endsWith(metricsFilename()))) {
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
