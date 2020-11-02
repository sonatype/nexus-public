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
package org.sonatype.nexus.repository.internal.blobstore;

import static java.util.Objects.requireNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.BlobStoreUtil;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import com.google.common.hash.HashCode;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.size;
import static java.util.function.Function.identity;
import static org.apache.commons.io.FilenameUtils.separatorsToSystem;

/**
 * @since 3.15
 */
@Named
@Singleton
public class BlobStoreUtilImpl
    implements BlobStoreUtil
{
  private final RepositoryManager repositoryManager;

  @Inject
  public BlobStoreUtilImpl(final RepositoryManager repositoryManager) {
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @Override
  public int usageCount(final String blobStoreId) {
    return size(repositoryManager.browseForBlobStore(blobStoreId));
  }

  /**
   * @since 3.20
   */
  @Override
  public boolean validateFilePath(final String filePath, final int maxLength) {
    requireNonNull(filePath);
    checkArgument(maxLength > 0, "maxLength should be greater than zero.");

    Path path = Paths.get(separatorsToSystem(filePath)).toAbsolutePath();
    List<String> fileNames = new ArrayList<>();

    Path pathParent = path.getParent();
    while (pathParent != null) {
      if (path.getFileName() != null) {
        fileNames.add(path.getFileName().toString());
      }
      path = pathParent;
      pathParent = path.getParent();
    }
    return !fileNames.stream().anyMatch(fileOrDirectory -> fileOrDirectory.length() > maxLength);
  }

  @Override
  public Map<HashAlgorithm, HashCode> toHashObjects(final Map<String, String> checksums) {
    return checksums.keySet().stream()
        .map(HashAlgorithm::getHashAlgorithm)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toMap(identity(), alg -> HashCode.fromString(checksums.get(alg.name()))));
  }
}
