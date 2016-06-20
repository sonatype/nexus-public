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
package org.sonatype.nexus.repository.search;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityBatchEvent;
import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.common.event.EventAware.Asynchronous;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.AssetEvent;
import org.sonatype.nexus.repository.storage.ComponentDeletedEvent;
import org.sonatype.nexus.repository.storage.ComponentEvent;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Subscriber of batched component/asset events, which are used to trigger search updates.
 *
 * @since 3.0
 */
@Named
@Singleton
public class ComponentSubscriber
    implements EventAware, Asynchronous
{
  private final RepositoryManager repositoryManager;

  @Inject
  public ComponentSubscriber(final RepositoryManager repositoryManager) {
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final EntityBatchEvent batchEvent) {
    final Multimap<String, EntityId> updatedComponents = HashMultimap.create();
    final Set<EntityId> deletedComponents = new HashSet<>();

    // slice events into their respective repositories
    for (final EntityEvent event : batchEvent.getEvents()) {
      if (event instanceof ComponentEvent) {
        final ComponentEvent componentEvent = (ComponentEvent) event;
        updatedComponents.put(componentEvent.getRepositoryName(), componentEvent.getComponentId());
        if (event instanceof ComponentDeletedEvent) {
          deletedComponents.add(componentEvent.getComponentId());
        }
      }
      else if (event instanceof AssetEvent) {
        final AssetEvent assetEvent = (AssetEvent) event;
        if (assetEvent.getComponentId() != null) {
          updatedComponents.put(assetEvent.getRepositoryName(), assetEvent.getComponentId());
        }
      }
    }

    // distribute updates across repositories as necessary
    for (final String repositoryName : updatedComponents.keySet()) {
      final Repository repository = repositoryManager.get(repositoryName);
      if (repository != null) {
        UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
        try {
          final SearchFacet searchFacet = repository.facet(SearchFacet.class);
          for (final EntityId componentId : updatedComponents.get(repositoryName)) {
            if (deletedComponents.contains(componentId)) {
              searchFacet.delete(componentId);
            }
            else {
              searchFacet.put(componentId);
            }
          }
        }
        finally {
          UnitOfWork.end();
        }
      }
    }
  }
}
