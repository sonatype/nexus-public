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
package org.sonatype.nexus.repository.content.search.table;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentKindEvent;
import org.sonatype.nexus.repository.content.event.repository.ContentRepositoryDeletedEvent;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.InternalIds;

import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA512;

/**
 * This class created to support single search table needs.
 */
@Named
@Singleton
public class SearchTableSubscriber
    extends StateGuardLifecycleSupport
    implements EventAware
{
  private final SearchTableStore store;

  @Inject
  public SearchTableSubscriber(final SearchTableStore store) {
    this.store = checkNotNull(store);
  }

  /**
   * Handles an event when need to create a new entry in component_search table
   *
   * @param event represents a created asset {@link AssetCreatedEvent}
   */
  @Subscribe
  public void on(final AssetCreatedEvent event) {
    Repository repository = event.getRepository().orElse(null);
    if (repository == null) {
      log.debug("Unable to determine repository for event {}", event);
      return;
    }
    Asset asset = event.getAsset();
    Component component = asset.component().orElse(null);
    if (component == null) {
      log.debug("Unable to determine component for event {}", event);
      return;
    }
    AssetBlob blob = asset.blob().orElse(null);
    if (blob == null) {
      log.debug("Unable to determine blob for event {}", event);
      return;
    }

    SearchTableData data = new SearchTableData();
    //PK
    data.setRepositoryId(InternalIds.contentRepositoryId(event));
    data.setComponentId(InternalIds.internalComponentId(component));
    data.setAssetId(InternalIds.internalAssetId(asset));
    data.setFormat(event.getFormat());
    //data component
    data.setNamespace(component.namespace());
    data.setComponentName(component.name());
    data.setComponentKind(component.kind());
    data.setVersion(component.version());
    data.setRepositoryName(repository.getName());
    //data asset
    data.setPath(asset.path());
    //data blob
    data.setContentType(blob.contentType());
    data.setMd5(blob.checksums().get(MD5.name()));
    data.setSha1(blob.checksums().get(SHA1.name()));
    data.setSha256(blob.checksums().get(SHA256.name()));
    data.setSha512(blob.checksums().get(SHA512.name()));
    log.trace("Creating a new record into component_search table: {}", data);

    store.create(data);
  }

  /**
   * Handles an event when need to update a component kind
   *
   * @param event represents an updated component kind {@link ComponentKindEvent}
   */
  @Subscribe
  public void on(final ComponentKindEvent event) {
    ComponentData component = (ComponentData) event.getComponent();
    Integer repositoryId = InternalIds.contentRepositoryId(event);
    Integer componentId = InternalIds.internalComponentId(component);
    String format = event.getFormat();
    String componentKind = component.kind();
    log.trace(
        "Updating a component kind in component_search table for repositoryId: {}, componentId: {}, format: {}," +
            " componentKind: {}", repositoryId, componentId, format, componentKind);

    store.updateKind(repositoryId, componentId, format, componentKind);
  }

  /**
   * Handles an event when need to delete existed entry in component_search table
   *
   * @param event represents a deleted asset {@link AssetDeletedEvent}
   */
  @Subscribe
  public void on(final AssetDeletedEvent event) {
    Asset asset = event.getAsset();
    Component component = asset.component().orElse(null);
    if (component == null) {
      log.debug("Unable to determine component for event {}", event);
      return;
    }
    Integer repositoryId = InternalIds.contentRepositoryId(event);
    Integer componentId = InternalIds.internalComponentId(component);
    Integer assetId = InternalIds.internalAssetId(asset);
    String format = event.getFormat();
    log.trace(
        "Deleting a record from component_search table for repositoryId: {}, componentId: {}, assetId: {}, format: {}",
        repositoryId, componentId, assetId, format);

    store.delete(repositoryId, componentId, assetId, format);
  }

  /**
   * Handles an event when need to delete all entries for repository in component_search table
   *
   * @param event the {@link ContentRepositoryDeletedEvent} with a repository identification.
   */
  @Subscribe
  public void on(final ContentRepositoryDeletedEvent event) {
    Integer repositoryId = InternalIds.contentRepositoryId(event);
    String format = event.getFormat();
    log.trace("Deleting all records from component_search table for repository id: {}, format: {}",
        repositoryId, format);

    store.deleteAllForRepository(repositoryId, format);
  }
}
