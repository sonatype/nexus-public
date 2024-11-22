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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.file.FileBlobStore;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.rest.ValidationErrorsException;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.annotations.VisibleForTesting;

import static com.codahale.metrics.health.HealthCheck.Result.healthy;
import static com.codahale.metrics.health.HealthCheck.Result.unhealthy;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.BASEDIR;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.CONFIG_KEY;
import static org.sonatype.nexus.blobstore.file.FileBlobStore.PATH_KEY;

/**
 * @since 3.29
 */
@Named("File Blob Stores Path")
@Singleton
public class FileBlobStorePathValidator
    extends HealthCheck
{
  private static final String DUPLICATES_WARN =
      "A file blob store must have a unique path, and may not be contained in a sub directory of another blob store. Blob stores with violations: %s";

  private final Provider<BlobStoreManager> blobStoreManager;

  private final Provider<ApplicationDirectories> applicationDirectories;

  @Inject
  public FileBlobStorePathValidator(
      final Provider<BlobStoreManager> blobStoreManager,
      final Provider<ApplicationDirectories> applicationDirectories)
  {
    this.blobStoreManager = blobStoreManager;
    this.applicationDirectories = applicationDirectories;
  }

  @Override
  @VisibleForTesting
  protected Result check() throws Exception {
    Iterable<BlobStore> blobStores = blobStoreManager.get().browse();
    List<BlobStoreConfiguration> configs = StreamSupport.stream(blobStores.spliterator(), false)
        .map(BlobStore::getBlobStoreConfiguration)
        .filter(config -> config.getType().equals(FileBlobStore.TYPE))
        .collect(Collectors.toList());

    Set<BlobStoreConfiguration> pathDuplicates = extractPathDuplicates(configs);
    if (!pathDuplicates.isEmpty()) {
      return unhealthy(buildWarning(pathDuplicates));
    }

    return healthy();
  }

  public void validatePathUniqueConstraint(final BlobStoreConfiguration configuration) {
    final Path pathToCheck = fromBlobStoreConfiguration(configuration);

    Iterable<BlobStore> blobStores = blobStoreManager.get().browse();

    List<BlobStoreConfiguration> pathDuplicates = StreamSupport.stream(blobStores.spliterator(), false)
        .map(BlobStore::getBlobStoreConfiguration)
        .filter(config -> config.getType().equals(FileBlobStore.TYPE)
            && !config.getName().equals(configuration.getName())
            && pathsViolateUniqueConstraint(fromBlobStoreConfiguration(config), pathToCheck))
        .collect(Collectors.toList());

    if (!pathDuplicates.isEmpty()) {
      throw new ValidationErrorsException(buildWarning(pathDuplicates));
    }
  }

  private Set<BlobStoreConfiguration> extractPathDuplicates(final List<BlobStoreConfiguration> configs) {
    Set<BlobStoreConfiguration> pathDuplicates = new HashSet<>();
    for (int i = 0; i < configs.size(); i++) {
      BlobStoreConfiguration configI = configs.get(i);
      for (int k = i + 1; k < configs.size(); k++) {
        BlobStoreConfiguration configK = configs.get(k);
        if (configsViolateUniquePathConstraint(configI, configK)) {
          pathDuplicates.add(configI);
          pathDuplicates.add(configK);
        }
      }
    }
    return pathDuplicates;
  }

  private Path fromBlobStoreConfiguration(final BlobStoreConfiguration configuration) {
    return Paths.get(configuration.attributes(CONFIG_KEY).get(PATH_KEY, String.class));
  }

  private boolean configsViolateUniquePathConstraint(
      final BlobStoreConfiguration config1,
      final BlobStoreConfiguration config2)
  {
    return pathsViolateUniqueConstraint(fromBlobStoreConfiguration(config1), fromBlobStoreConfiguration(config2));
  }

  private boolean pathsViolateUniqueConstraint(Path path1, Path path2) {
    if (path1 == null && path2 == null) {
      return true;
    }
    if (path1 == null || path2 == null) {
      return false;
    }

    path1 = path1.normalize();
    path2 = path2.normalize();

    if (path1.equals(path2)) {
      return true;
    }

    try {
      path1 = toAbsolutePath(path1);
      path2 = toAbsolutePath(path2);
    }
    catch (IOException e) {
      return false;
    }

    return path1.startsWith(path2) || path2.startsWith(path1);
  }

  private Path toAbsolutePath(Path path) throws IOException {
    if (!path.isAbsolute()) {
      Path baseDir = applicationDirectories.get().getWorkDirectory(BASEDIR).toPath().toRealPath().normalize();
      return baseDir.resolve(path.normalize());
    }
    return path;
  }

  private String buildWarning(final Collection<BlobStoreConfiguration> pathDuplicates) {
    return String.format(DUPLICATES_WARN,
        pathDuplicates
            .stream()
            .map(BlobStoreConfiguration::getName)
            .collect(Collectors.joining(", ")));
  }
}
