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
package org.sonatype.nexus.repository.r.orient.internal.hosted;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.r.orient.OrientRHostedFacet;
import org.sonatype.nexus.repository.r.RPackagesBuilderFacet;
import org.sonatype.nexus.repository.r.internal.hosted.RMetadataInvalidationEvent;
import org.sonatype.nexus.repository.r.orient.util.OrientRFacetUtils;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEvent;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.r.AssetKind.ARCHIVE;
import static org.sonatype.nexus.repository.r.internal.util.RPathUtils.getBasePath;

/**
 * Implementation of {@link RPackagesBuilderFacet} targeted for use with hosted repositories. Uses event dispatching to
 * ensure that we do not have race conditions when processing and rebuilding metadata, and also imposes a waiting period
 * to batch metadata updates.
 *
 * @since 3.28
 */
@Named
public class OrientRPackagesBuilderFacetImpl
    extends FacetSupport
    implements RPackagesBuilderFacet, Asynchronous
{
  /**
   * The event manager to use when posting new events.
   */
  private final EventManager eventManager;

  /**
   * The interval in milliseconds to wait between rebuilds.
   */
  private final long interval;

  /**
   * The flag indicating if we are currently waiting to rebuild.
   */
  private boolean waiting;

  /**
   * Constructor.
   *
   * @param eventManager The event manager to use when posting new events.
   * @param interval     The interval in milliseconds to wait between rebuilds.
   */
  @Inject
  public OrientRPackagesBuilderFacetImpl(
      final EventManager eventManager,
      @Named("${nexus.r.packagesBuilder.interval:-1000}") final long interval)
  {
    this.eventManager = checkNotNull(eventManager);
    this.interval = interval;
  }

  /**
   * Handles {@link AssetEvent} events concurrently, requesting a metadata invalidation and rebuild if warranted
   * by the event contents.
   *
   * @param event The event to handle.
   */
  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(final AssetEvent event) {
    if (shouldInvalidate(event)) {
      invalidateMetadata(getBasePath(event.getAsset().name()));
    }
  }

  /**
   * Handles metadata invalidation for this repository given the specified base path. Internally this is handled by
   * posting a {@code RMetadataInvalidationEvent} to the event manager which then triggers the actual metadata
   * regeneration. Note that the regeneration may not be instantaneous (and that the waiting period is configurable).
   *
   * @param basePath The base path of the PACKAGES.gz file to invalidate.
   */
  @Override
  public void invalidateMetadata(final String basePath) {
    if (!waiting) {
      eventManager.post(new RMetadataInvalidationEvent(getRepository().getName(), basePath));
    }
  }

  /**
   * Listen for invalidation of the metadata, wait a configured time and then rebuild. The waiting allows throwing away
   * of subsequent events to reduce the number of rebuilds if multiple archives are being uploaded. This method must NOT
   * be allowed to process concurrent events as race conditions may result when rebuilding the data.
   */
  @Subscribe
  public void on(final RMetadataInvalidationEvent event) {
    if (getRepository().getName().equals(event.getRepositoryName())) {
      try {
        waiting = true;
        Thread.sleep(interval);
      }
      catch (InterruptedException e) {
        log.warn("R invalidation thread interrupted on repository {}, proceeding with invalidation",
            getRepository().getName());
        Thread.currentThread().interrupt();
      }
      waiting = false;
      log.info("Rebuilding R PACKAGES.gz metadata for repository {}", getRepository().getName());
      UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
      try {
        OrientRHostedFacet hostedFacet = getRepository().facet(OrientRHostedFacet.class);
        hostedFacet.buildAndPutPackagesGz(event.getBasePath());
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      finally {
        UnitOfWork.end();
      }
    }
  }

  /**
   * Returns whether or not an asset event should result in an invalidation request. In order for the event to mandate
   * that we rebuild metadata, it must both refer to this particular repository, and it also must refer to an uploaded
   * archive and not to one of the metadata files itself. This helps ensure that we don't end up responding to metadata
   * changes when rebuilding metadata and end up in a loop.
   *
   * @param assetEvent The asset event to process.
   * @return true if an archive, false if a packages file
   */
  private boolean shouldInvalidate(final AssetEvent assetEvent) {
    return assetEvent.isLocal() &&
        getRepository().getName().equals(assetEvent.getRepositoryName()) &&
        isArchiveAssetKind(assetEvent.getAsset());
  }

  /**
   * Extracts asset kind from asset and compares to archive.
   *
   * @return true is asset kind is archive.
   */
  private boolean isArchiveAssetKind(final Asset asset) {
    return ARCHIVE == OrientRFacetUtils.extractAssetKind(asset);
  }
}
