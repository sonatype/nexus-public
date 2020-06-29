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
package org.sonatype.nexus.repository.pypi.tasks.orient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.pypi.internal.PyPiFormat;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.CancelableHelper;
import org.sonatype.nexus.scheduling.TaskSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Streams.stream;
import static org.sonatype.nexus.repository.pypi.upgrade.PyPiUpgrade_1_2.MARKER_FILE;

/**
 * NEXUS-22770: Delete PyPi proxy assets which don't match the standardized path
 * (<code>/packages/{name}/{version}/{name}-{version}.{ext}</code>) as well as package metadata which has been
 * re-written.
 *
 * @since 3.22
 */
@Named
public class OrientPyPiDeleteLegacyProxyAssetsTask
    extends TaskSupport
    implements Cancelable
{
  private final Path markerFile;

  private final RepositoryManager repositoryManager;

  private final Pattern packagePattern =
      Pattern.compile("packages\\/(?<name>[^/]+)\\/(?<version>[^/]+)\\/\\k<name>-\\k<version>\\.[^/]+");

  @Inject
  public OrientPyPiDeleteLegacyProxyAssetsTask(
      final ApplicationDirectories directories,
      final RepositoryManager repositoryManager)
  {
    markerFile = new File(directories.getWorkDirectory("db"), MARKER_FILE).toPath();
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @Override
  protected Object execute() throws Exception {
    stream(repositoryManager.browse())
        .peek(r -> log.debug("Looking at repository: {}", r))
        .filter(r -> r.getFormat() instanceof PyPiFormat)
        .peek(r -> log.debug("Looking at PyPi repository: {}", r))
        .filter(r -> r.getType() instanceof ProxyType)
        .peek(r -> log.debug("Found PyPi proxy repository: {}", r))
        .forEach(this::deleteLegacyAssets);
    if (Files.exists(markerFile)) {
      Files.delete(markerFile);
    }
    return null;
  }

  private void deleteLegacyAssets(final Repository repository) {
    log.info("Deleting legacy assets in PyPi proxy repository: {}", repository);
    StorageFacet storageFacet = repository.facet(StorageFacet.class);

    try (StorageTx tx = storageFacet.txSupplier().get()) {
      tx.begin();
      Bucket bucket = tx.findBucket(repository);
      stream(tx.browseAssets(bucket))
          .peek(a -> log.debug("Evaluating asset: {}", a.name()))
          .filter(this::isInvalidPath)
          .forEach(a -> {
            CancelableHelper.checkCancellation();
            log.info("Deleting asset: {}", a.name());
            tx.deleteAsset(a);
          });
      tx.commit();
    }
  }

  @Override
  public String getMessage() {
    return "Delete legacy PyPi proxy package assets whose names start with 'packages/'";
  }

  private boolean isInvalidPath(final Asset asset) {
    if ("simple/".equals(asset.name())) {
      return false;
    }
    return !packagePattern.matcher(asset.name()).matches();
  }
}
