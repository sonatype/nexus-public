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
package org.sonatype.nexus.repository.r.datastore.internal.hosted;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.event.asset.AssetEvent;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.r.RFormat;
import org.sonatype.nexus.repository.r.RPackagesBuilderFacet;
import org.sonatype.nexus.repository.r.datastore.RContentFacet;
import org.sonatype.nexus.repository.r.AssetKind;
import org.sonatype.nexus.repository.r.internal.hosted.RMetadataInvalidationEvent;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.r.internal.util.RPathUtils.PACKAGES_GZ_FILENAME;
import static org.sonatype.nexus.repository.r.internal.util.RPathUtils.buildPath;
import static org.sonatype.nexus.repository.r.internal.util.RPathUtils.getBasePath;

/**
 * R hosted metadata facet
 *
 * @since 3.next
 */
@Exposed
@Named
public class RHostedMetadataFacet
    extends FacetSupport
    implements RPackagesBuilderFacet, Asynchronous
{
  /**
   * The interval in milliseconds to wait between rebuilds.
   */
  private final long interval;

  /**
   * The flag indicating if we are currently waiting to rebuild.
   */
  private final AtomicBoolean waiting = new AtomicBoolean(false);

  @Inject
  public RHostedMetadataFacet(@Named("${nexus.r.packagesBuilder.interval:-1000}") final long interval) {
    this.interval = interval;
  }

  /**
   * Handles {@link AssetEvent} events concurrently, requesting a metadata invalidation and rebuild if warranted by the
   * event contents.
   *
   * @param event - The event to handle.
   */
  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(final AssetEvent event) {
    if (shouldInvalidate(event)) {
      String basePath = getBasePath(event.getAsset().path());
      invalidateMetadata(basePath);
    }
  }

  /**
   * Returns whether or not an asset event should result in an invalidation request. In order for the event to mandate
   * that we rebuild metadata, it must both refer to this particular repository, and it also must refer to an uploaded
   * archive and not to one of the metadata files itself. This helps ensure that we don't end up responding to metadata
   * changes when rebuilding metadata and end up in a loop.
   *
   * @param event - The asset event to process.
   * @return true if an archive, false if a packages file
   */
  private boolean shouldInvalidate(final AssetEvent event) {
    Optional<Repository> eventRepositoryOptional = event.getRepository();
    if (!eventRepositoryOptional.isPresent()) {
      return false;
    }

    Repository eventRepository = eventRepositoryOptional.get();
    return getRepository().getName().equals(eventRepository.getName())
        && event.getAsset().kind().equals(AssetKind.ARCHIVE.name());
  }

  /**
   * Handles metadata invalidation for this repository given the specified base path. Internally this is handled by
   * posting a {@code RMetadataInvalidationEvent} to the event manager which then triggers the actual metadata
   * regeneration. Note that the regeneration may not be instantaneous (and that the waiting period is configurable).
   *
   * @param basePath - The base path of the PACKAGES.gz file to invalidate.
   */
  @Override
  public void invalidateMetadata(final String basePath) {
    if (!waiting.get()) {
      getEventManager()
          .post(new RMetadataInvalidationEvent(getRepository().getName(), basePath));
    }
  }

  /**
   * Listen for invalidation of the metadata, wait a configured time and then rebuild. The waiting allows throwing away
   * of subsequent events to reduce the number of rebuilds if multiple archives are being uploaded. This method must NOT
   * be allowed to process concurrent events as race conditions may result when rebuilding the data.
   */
  @Subscribe
  public void on(final RMetadataInvalidationEvent event) throws IOException {
    if (getRepository().getName().equals(event.getRepositoryName())) {
      try {
        waiting.set(true);
        Thread.sleep(interval);
      }
      catch (InterruptedException e) {
        log.warn("R invalidation thread interrupted on repository {}, proceeding with invalidation",
            getRepository().getName());
        Thread.currentThread().interrupt();
      } finally {
        waiting.set(false);
      }

      log.info("Rebuilding R PACKAGES.gz metadata for repository {}", getRepository().getName());

      recalculatePackagesGz(event.getBasePath());
    }
  }

  private FluentAsset recalculatePackagesGz(final String basePath) throws IOException {
    RContentFacet contentFacet = facet(RContentFacet.class);

    RPackagesBuilder builder = new RPackagesBuilder();

    for (FluentAsset fluentAsset : contentFacet.getAssetsByKind(AssetKind.ARCHIVE.name())) {
      String assetBasePath = getBasePath(fluentAsset.path());
      if (!assetBasePath.equals(basePath)) {
        continue;
      }

      NestedAttributesMap formatAttributes = fluentAsset.attributes(RFormat.NAME);
      builder.append(formatAttributes);
    }

    byte[] bytes = builder.buildPackagesGz();

    Payload payload = new BytesPayload(bytes, null);
    String assetPath = buildPath(basePath, PACKAGES_GZ_FILENAME);
    return contentFacet.putMetadata(payload, assetPath, AssetKind.PACKAGES);
  }

  public FluentAsset getOrCreatePackagesGz(final String assetPath) throws IOException {
    Optional<FluentAsset> asset = facet(RContentFacet.class)
        .getAsset(assetPath);
    if (asset.isPresent()) {
      return asset.get();
    }

    String basePath = getBasePath(assetPath);
    return recalculatePackagesGz(basePath);
  }
}
