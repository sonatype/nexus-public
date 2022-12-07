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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.search.SearchEventHandler;
import org.sonatype.nexus.repository.content.store.InternalIds;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.sonatype.nexus.repository.content.store.InternalIds.toInternalId;

/**
 * This class is for incrementally updating the component_search table in response to ContentStoreEvents.
 *
 * It is also used for batch updating of the component_search table via the search rebuild task
 *
 * Re-indexes or deletes a component record in the search table. This class uses cooperation to make sure
 * that only one thread (within a node or across the nodes in a cluster) can re-index a specific component in
 * the search table. Deletes don't need to use cooperation because the re-index SQL always checks the existence
 * of the component before updating.
 *
 * @see SearchEventHandler
 * @see SqlSearchEventHandler
 */
@Named
@Singleton
public class SqlSearchIndexService
    extends ComponentSupport
{
  private final SearchTableDataProducer searchTableDataProducer;

  private final SearchTableStore searchStore;

  @Inject
  public SqlSearchIndexService(
      final SearchTableDataProducer searchTableDataProducer,
      final SearchTableStore searchStore)
  {
    this.searchTableDataProducer = checkNotNull(searchTableDataProducer);
    this.searchStore = checkNotNull(searchStore);
  }

  public void indexBatch(final Collection<FluentComponent> components, final Repository repository) {
    log.debug("Saving batch of components for repository: {}", repository.getName());

    List<SearchTableData> searchData = components.stream()
        .map(component -> searchTableDataProducer.createSearchTableData(component, repository))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());

    //This is a batch update not an incremental update and only
    // one instance of the RebuildIndexTask can be running in a HA cluster.
    try {
      searchStore.saveBatch(searchData);
    }
    catch (Exception batchError) {
      log.debug("Failed batch insertion, retrying components", batchError);

      searchData.forEach(this::saveToStore);
    }
  }

  public void purge(final Collection<EntityId> componentIds, final Repository repository) {
    ContentFacet facet = repository.facet(ContentFacet.class);
    Set<Integer> internalIds = componentIds.stream().map(InternalIds::toInternalId).collect(toSet());
    log.debug("Purging indexes for component ids: {} and repository: {}", componentIds, repository.getName());
    searchStore.deleteComponentIds(facet.contentRepositoryId(), internalIds, repository.getFormat().getValue());
  }

  public void index(final Collection<EntityId> componentIds, final Repository repository) {
    componentIds.forEach(componentId -> indexSearchData(componentId, repository));
  }

  private void indexSearchData(final EntityId componentId, final Repository repository) {
    try {
      log.debug("Indexing component id: {}, repository: {}", componentId, repository.getName());
      refreshComponentData(componentId, repository);
    }
    catch (ComponentNotFoundException ex) {
      log.debug("Skipping refresh because: {}", ex.getMessage());
    }
  }

  private Optional<SearchTableData> refreshComponentData(final EntityId componentId, final Repository repository) {
    FluentComponent componentFromDb = fetchComponentFromDb(componentId, repository);
    return ofNullable(
        searchTableDataProducer.createSearchTableData(componentFromDb, repository)
            .map(this::saveToStore)
            .orElseGet(() -> {
              searchStore.delete(
                  repository.facet(ContentFacet.class).contentRepositoryId(),
                  toInternalId(componentId),
                  repository.getFormat().getValue()
              );
              return null;
            })
    );
  }

  private FluentComponent fetchComponentFromDb(final EntityId componentId, final Repository repository) {
    return repository.facet(ContentFacet.class)
        .components()
        .find(componentId)
        .orElseThrow(() -> new ComponentNotFoundException(
            String.format("Component %d not found for repository %s and format %s",
                toInternalId(componentId), repository.getName(), repository.getFormat().getValue())));
  }

  private SearchTableData saveToStore(final SearchTableData data)
  {
    log.debug("Saving {} to component_search table", data);
    try {
      searchStore.save(data);
    }
    catch (Exception e) {
      if (log.isDebugEnabled()) {
        log.warn("Failed to update search record for {}:{}:{} in {} cause {}", data.getNamespace(),
            data.getComponentName(), data.getVersion(), data.getRepositoryName(), e.getMessage());
      }
      else {
        log.warn("Failed to update search record for {}:{}:{} in {}", data.getNamespace(), data.getComponentName(),
            data.getVersion(), data.getRepositoryName(), e);
      }
    }
    return data;
  }
}
