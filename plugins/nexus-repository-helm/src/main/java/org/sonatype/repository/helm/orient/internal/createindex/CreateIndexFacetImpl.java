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
package org.sonatype.repository.helm.orient.internal.createindex;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetCreatedEvent;
import org.sonatype.nexus.repository.storage.AssetDeletedEvent;
import org.sonatype.nexus.repository.storage.AssetEvent;
import org.sonatype.nexus.repository.storage.AssetUpdatedEvent;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.repository.helm.HelmAttributes;
import org.sonatype.repository.helm.internal.AssetKind;
import org.sonatype.repository.helm.internal.HelmFormat;
import org.sonatype.repository.helm.internal.createindex.CreateIndexFacet;
import org.sonatype.repository.helm.internal.createindex.HelmIndexInvalidationEvent;
import org.sonatype.repository.helm.orient.internal.HelmFacet;
import org.sonatype.repository.helm.orient.internal.hosted.HelmHostedFacet;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.repository.helm.internal.AssetKind.HELM_INDEX;
import static org.sonatype.repository.helm.internal.AssetKind.HELM_PACKAGE;

/**
 * Facet for rebuilding Helm index.yaml files
 *
 * @since 3.28
 */
@Named
public class CreateIndexFacetImpl
    extends FacetSupport
    implements CreateIndexFacet, Asynchronous
{
  private final EventManager eventManager;

  private CreateIndexService createIndexService;

  private final long interval;

  private static final String INDEX_YAML = "index.yaml";

  private static final String TGZ_CONTENT_TYPE = "application/x-tgz";

  private final AtomicBoolean acceptingEvents = new AtomicBoolean(true);

  //Prevents the same event from being fired multiple times
  private final AtomicBoolean eventFired = new AtomicBoolean(false);

  @Inject
  public CreateIndexFacetImpl(final EventManager eventManager,
                              final CreateIndexService createIndexService,
                              @Named("${nexus.helm.createrepo.interval:-1000}") final long interval)
  {
    this.eventManager = checkNotNull(eventManager);
    this.createIndexService = checkNotNull(createIndexService);
    this.interval = interval;
  }

  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(final AssetCreatedEvent created) {
    maybeInvalidateIndex(created);
  }

  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(final AssetDeletedEvent deleted) {
    maybeInvalidateIndex(deleted);
  }

  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(final AssetUpdatedEvent updated) {
    maybeInvalidateIndex(updated);
  }

  @Subscribe
  @Guarded(by = STARTED)
  @AllowConcurrentEvents
  public void on(RepositoryCreatedEvent createdEvent) {
    // at repository creation time, create index.yaml with empty entries
    log.debug("Initializing index.yaml for hosted repository {}", getRepository().getName());
    invalidateIndex();
  }

  private void maybeInvalidateIndex(final AssetEvent event) {
    Asset asset = event.getAsset();
    String formatName = asset.format();
    if (HelmFormat.NAME.equals(formatName)) {
      String assetKindString = (String) asset.formatAttributes().get(P_ASSET_KIND);
      AssetKind assetKind = AssetKind.valueOf(assetKindString);
      if (assetKind == HELM_PACKAGE && matchesRepository(event) && isEventRelevant(event)) {
        invalidateIndex();
      }
    }
  }

  @Subscribe
  public void on(final HelmIndexInvalidationEvent event) {
    if (shouldProcess(event)) {
      acceptingEvents.set(false);
      maybeWait(event);

      log.info("Rebuilding Helm index for repository {}", getRepository().getName());

      UnitOfWork.begin(getRepository().facet(StorageFacet.class).txSupplier());
      try {
        acceptingEvents.set(true);
        eventFired.set(false);

        updateIndexYaml(createIndexService.buildIndexYaml(getRepository()));
      }
      finally {
        log.info("Finished rebuilding Helm index for repository {}", getRepository().getName());

        UnitOfWork.end();
      }
    }
  }

  @TransactionalStoreBlob
  protected void updateIndexYaml(final TempBlob indexYaml) {
    if (indexYaml == null) {
      deleteIndexYaml();
    }
    else {
      createIndexYaml(indexYaml);
    }
  }

  private void createIndexYaml(final TempBlob indexYaml) {
    Repository repository = getRepository();
    HelmFacet helmFacet = repository.facet(HelmFacet.class);
    StorageTx tx = UnitOfWork.currentTx();
    HelmAttributes attributes = new HelmAttributes(Collections.emptyMap());
    Asset asset = helmFacet.findOrCreateAsset(tx, INDEX_YAML, HELM_INDEX, attributes);
    try {
      helmFacet.saveAsset(tx, asset, indexYaml, TGZ_CONTENT_TYPE, null);
    }
    catch (IOException ex) {
      log.warn("Could not set blob {}", ex.getMessage(), ex);
    }
  }

  private void deleteIndexYaml() {
    log.debug("Empty index.yaml returned, proceeding to delete asset");
    HelmHostedFacet hosted = getRepository().facet(HelmHostedFacet.class);
    boolean result = hosted.delete(INDEX_YAML);
    if (result) {
      log.info("Deleted index.yaml because of empty asset list");
    }
    else {
      log.warn("Unable to delete index.yaml asset");
    }
  }

  private boolean shouldProcess(final HelmIndexInvalidationEvent event) {
    return getRepository().getName().equals(event.getRepositoryName());
  }

  private void maybeWait(final HelmIndexInvalidationEvent event) {
    if (event.isWaitBeforeRebuild()) {
      try {
        Thread.sleep(interval);
      }
      catch (InterruptedException e) { // NOSONAR
        log.warn("Helm invalidation thread interrupted, proceeding with invalidation");
      }
    }
  }

  /**
   * This prevents us firing the invalidation event multiple time unnecessarily. If we don't do this check then then
   * created/updated/deleted events will be handled by every instance of this class and each will fire an invalidation
   * event
   */
  private boolean matchesRepository(final AssetEvent assetEvent) {
    return assetEvent.isLocal() && getRepository().getName().equals(assetEvent.getRepositoryName());
  }

  /**
   * If this is a component (chart) then we need to rebuild the metadata.
   */
  private boolean isEventRelevant(final AssetEvent event) {
    return event.getComponentId() != null;
  }

  /**
   * We are accepting that the boolean values can change in the middle of this if statement. For example if
   * acceptingEvents is true and eventFired is false but before we check eventFired acceptingEvents is toggled, we will
   * add one additional process request which will be handled synchronously and not cause any issues.
   */
  @Override
  public synchronized void invalidateIndex() {
    if (acceptingEvents.get() && !eventFired.get()) {
      log.info("Scheduling rebuild of Helm metadata to start in {} seconds", interval / 1000);

      // Prevent another event being fired if one is already queued up
      eventFired.set(true);
      eventManager.post(new HelmIndexInvalidationEvent(getRepository().getName(), true));
    }
  }
}
