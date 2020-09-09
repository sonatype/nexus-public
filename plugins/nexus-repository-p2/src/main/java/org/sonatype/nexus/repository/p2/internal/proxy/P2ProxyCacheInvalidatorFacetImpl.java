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
package org.sonatype.nexus.repository.p2.internal.proxy;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Named;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.scheduling.CancelableHelper;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.collect.Streams.stream;

/**
 * Handles invalidating p2 proxy cache when the URL for the repository changes.
 *
 * @since 1.1
 */
@Named
@Facet.Exposed
public class P2ProxyCacheInvalidatorFacetImpl
    extends FacetSupport
{
  @Subscribe
  @AllowConcurrentEvents
  protected void on(final RepositoryUpdatedEvent event) {
    final Repository repository = event.getRepository();

    if (!repository.getName().equals(this.getRepository().getName())) {
      return;
    }

    if (!Objects.equals(getRemoteUrl(repository.getConfiguration()), getRemoteUrl(event.getOldConfiguration()))) {
      log.info("URL changed for p2 repository: {}", getRepository().getName());
      deleteAssets();
    }
  }

  public void deleteAssets() {
    Repository repository = getRepository();
    log.info("Removing cached assets in p2 proxy repository: {}", repository);
    StorageFacet storageFacet = repository.facet(StorageFacet.class);

    try (StorageTx tx = storageFacet.txSupplier().get()) {
      tx.begin();
      Bucket bucket = tx.findBucket(repository);
      stream(tx.browseAssets(bucket))
          .peek(a -> log.debug("Evaluating asset: {}", a.name()))
          .forEach(a -> {
            CancelableHelper.checkCancellation();
            log.debug("Deleting asset: {}", a.name());
            tx.deleteAsset(a);
          });
      stream(tx.browseComponents(bucket))
          .peek(c -> log.debug("Evaluating asset: {}", c.name()))
          .forEach(c -> {
             CancelableHelper.checkCancellation();
             log.debug("Deleting component: {}", c.name());
             tx.deleteComponent(c);
          });
      tx.commit();
    }
  }

  private static Object getRemoteUrl(final Configuration configuration) {
    return Optional.ofNullable(configuration.getAttributes().get("proxy")).map(proxy -> proxy.get("remoteUrl")).orElse(null);
  }
}
