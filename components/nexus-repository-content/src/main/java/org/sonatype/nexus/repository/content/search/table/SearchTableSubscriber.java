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

import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.event.asset.AssetAttributesEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentKindEvent;
import org.sonatype.nexus.repository.content.event.repository.ContentRepositoryDeletedEvent;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.InternalIds;

import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.content.search.table.SearchTableEventProcessor.EventType.ASSET_ATTRIBUTES_UPDATED;
import static org.sonatype.nexus.repository.content.search.table.SearchTableEventProcessor.EventType.ASSET_CREATED;
import static org.sonatype.nexus.repository.content.search.table.SearchTableEventProcessor.EventType.ASSET_DELETED;
import static org.sonatype.nexus.repository.content.search.table.SearchTableEventProcessor.EventType.COMPONENT_KIND_UPDATED;
import static org.sonatype.nexus.repository.content.search.table.SearchTableEventProcessor.EventType.REPOSITORY_DELETED;

/**
 * This class created to support single search table needs.
 */
@Named
@Singleton
public class SearchTableSubscriber
    extends StateGuardLifecycleSupport
    implements EventAware
{
  private final SearchTableEventProcessor eventProcessor;

  private final SearchTableDataMapper searchTableDataMapper;

  @Inject
  public SearchTableSubscriber(
      final SearchTableEventProcessor eventProcessor,
      final SearchTableDataMapper searchTableDataMapper)
  {
    this.eventProcessor = checkNotNull(eventProcessor);
    this.searchTableDataMapper = checkNotNull(searchTableDataMapper);
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

    Optional<SearchTableData> searchData = searchTableDataMapper.convert(event.getAsset(), repository);
    if (!searchData.isPresent()) {
      log.debug("Unable to build the search data based on event: {}", event);
      return;
    }

    eventProcessor.addEvent(ASSET_CREATED, searchData.get());
    eventProcessor.checkAndFlush();
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

    SearchTableData data = new SearchTableData(repositoryId, componentId, format);
    data.setComponentKind(componentKind);
    eventProcessor.addEvent(COMPONENT_KIND_UPDATED, data);
    eventProcessor.checkAndFlush();
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

    eventProcessor.addEvent(ASSET_DELETED, new SearchTableData(repositoryId, componentId, assetId, format));
    eventProcessor.checkAndFlush();
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

    eventProcessor.addEvent(REPOSITORY_DELETED, new SearchTableData(repositoryId, format));
    eventProcessor.checkAndFlush();
  }

  /**
   * Handles an event when need to update a format specific fields
   *
   * @param event represents an updated asset attributes {@link AssetAttributesEvent}
   */
  @Subscribe
  public void on(final AssetAttributesEvent event) {
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

    SearchTableData data = searchTableDataMapper.convert(asset, repository).orElse(new SearchTableData());
    eventProcessor.addEvent(ASSET_ATTRIBUTES_UPDATED, data);
    eventProcessor.checkAndFlush();
  }
}
