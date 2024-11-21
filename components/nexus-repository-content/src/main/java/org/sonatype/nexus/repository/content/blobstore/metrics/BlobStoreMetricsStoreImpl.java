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
package org.sonatype.nexus.repository.content.blobstore.metrics;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsEntity;
import org.sonatype.nexus.blobstore.api.metrics.BlobStoreMetricsStore;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.transaction.Transactional;

@Named
@Singleton
public class BlobStoreMetricsStoreImpl
    extends ConfigStoreSupport<BlobStoreMetricsDAO>
    implements BlobStoreMetricsStore
{
  @Inject
  protected BlobStoreMetricsStoreImpl(
      final DataSessionSupplier sessionSupplier)
  {
    super(sessionSupplier, BlobStoreMetricsDAO.class);
  }

  @Override
  @Transactional
  public void updateMetrics(final BlobStoreMetricsEntity blobStoreMetricsEntity) {
    dao().updateMetrics(blobStoreMetricsEntity);
  }

  @Override
  @Transactional
  public BlobStoreMetricsEntity get(final String blobStoreName) {
    return dao().get(blobStoreName);
  }

  @Override
  @Transactional
  public void remove(final String blobStoreName) {
    dao().remove(blobStoreName);
  }

  @Override
  @Transactional
  public void clearOperationMetrics(final String blobStoreName) {
    dao().clearOperationMetrics(blobStoreName);
  }

  @Override
  @Transactional
  public void clearCountMetrics(final String blobStoreName) {
    dao().clearCountMetrics(blobStoreName);
  }

  @Override
  @Transactional
  public void initializeMetrics(String blobStoreName) {
    try {
      if (dao().get(blobStoreName) == null) {
        dao().initializeMetrics(blobStoreName);
      }
    }
    catch (DuplicateKeyException e) {
      log.debug("Failed to initialize blobstore metrics as they are already initialized.",
          e); // this is likely an HA race condition between multiple nodes - this is not a problem
    }
  }
}
