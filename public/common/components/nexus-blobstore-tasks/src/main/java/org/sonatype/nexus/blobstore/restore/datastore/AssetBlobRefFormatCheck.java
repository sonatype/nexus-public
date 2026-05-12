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
package org.sonatype.nexus.blobstore.restore.datastore;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.store.AssetBlobStore;
import org.sonatype.nexus.repository.content.store.FormatStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.DATA_STORE_NAME;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;
import org.springframework.stereotype.Component;

/**
 * Utility class which checks whether asset blob refs no longer contain node ids.
 *
 */
@Component
@Singleton
public class AssetBlobRefFormatCheck
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Map<String, FormatStoreManager> formatStoreManagers;

  @Inject
  public AssetBlobRefFormatCheck(final List<FormatStoreManager> formatStoreManagersList) {
    this.formatStoreManagers = QualifierUtil.buildQualifierBeanMap(checkNotNull(formatStoreManagersList));
  }

  /**
   * Returns true if the specified repository contains node id in its asset blob refs,
   * otherwise returns false
   */
  public boolean isAssetBlobRefNotMigrated(final Repository repository) {
    final String dataStoreName = getDataStoreName(repository);
    final String format = repository.getFormat().getValue();
    final boolean notMigrated = ofNullable(formatStoreManagers.get(format))
        .map(storeManager -> storeManager.assetBlobStore(dataStoreName))
        .map(assetBlobStore -> ((AssetBlobStore<?>) assetBlobStore).notMigratedAssetBlobRefsExists())
        .orElseThrow(() -> new RuntimeException("Cannot determine asset blob ref migration status"));

    if (notMigrated) {
      log.warn("Cannot restore {} repository '{}' " + "because legacy blob ref migration is not complete.",
          repository.getFormat().getValue(), repository.getName());
    }

    return notMigrated;
  }

  private String getDataStoreName(final Repository repository) {
    return Optional.of(repository.getConfiguration())
        .map(configuration -> configuration.attributes(STORAGE))
        .map(attr -> attr.get(DATA_STORE_NAME, String.class))
        .orElse(DEFAULT_DATASTORE_NAME);
  }
}
