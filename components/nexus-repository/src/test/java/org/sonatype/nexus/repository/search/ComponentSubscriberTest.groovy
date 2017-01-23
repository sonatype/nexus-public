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
package org.sonatype.nexus.repository.search

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.entity.DetachedEntityId
import org.sonatype.nexus.common.entity.EntityBatchEvent
import org.sonatype.nexus.common.entity.EntityId
import org.sonatype.nexus.common.stateguard.InvalidStateException
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.storage.AssetCreatedEvent
import org.sonatype.nexus.repository.storage.AssetDeletedEvent
import org.sonatype.nexus.repository.storage.AssetUpdatedEvent
import org.sonatype.nexus.repository.storage.ComponentCreatedEvent
import org.sonatype.nexus.repository.storage.ComponentDeletedEvent
import org.sonatype.nexus.repository.storage.ComponentUpdatedEvent
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.StorageTx

import com.google.common.base.Suppliers
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static org.mockito.Mockito.any
import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.verifyNoMoreInteractions
import static org.mockito.Mockito.when
import static org.sonatype.nexus.repository.FacetSupport.State.DELETED
import static org.sonatype.nexus.repository.FacetSupport.State.DESTROYED
import static org.sonatype.nexus.repository.FacetSupport.State.FAILED
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED
import static org.sonatype.nexus.repository.FacetSupport.State.STOPPED

class ComponentSubscriberTest
    extends TestSupport
{

  EntityId alphaComponentId = new DetachedEntityId('#3:1')

  EntityId betaComponentId = new DetachedEntityId('#4:2')

  EntityBatchEvent emptyBatchEvent = new EntityBatchEvent([])

  EntityBatchEvent simpleBatchEvent = new EntityBatchEvent([
    mockEntityEvent(ComponentCreatedEvent, alphaComponentId)
  ])

  @Mock
  RepositoryManager repositoryManager

  @Mock
  Repository repository

  @Mock
  SearchFacet searchFacet

  @Mock
  StorageFacet storageFacet

  @Mock
  StorageTx storageTx

  ComponentSubscriber componentSubscriber

  @Before
  public void setup() {
    componentSubscriber = new ComponentSubscriber(repositoryManager)
    when(repositoryManager.get('testRepo')).thenReturn(repository)
    when(repository.facet(SearchFacet)).thenReturn(searchFacet)
    when(repository.facet(StorageFacet)).thenReturn(storageFacet)
    when(storageFacet.txSupplier()).thenReturn(Suppliers.ofInstance(storageTx))
  }

  def mockEntityEvent(eventType, componentId) {
    def mockedEvent = mock(eventType)
    when(mockedEvent.getRepositoryName()).thenReturn('testRepo')
    when(mockedEvent.getComponentId()).thenReturn(componentId)
    return mockedEvent
  }

  @Test
  public void entityEventsTriggerSearchUpdates() throws Exception {

    // mix of events; simulates creating new entities, plus updating and/or deleting old ones
    ComponentCreatedEvent e1 = mockEntityEvent(ComponentCreatedEvent, alphaComponentId)
    AssetCreatedEvent e2 = mockEntityEvent(AssetCreatedEvent, betaComponentId)
    AssetCreatedEvent e3 = mockEntityEvent(AssetCreatedEvent, alphaComponentId)
    ComponentUpdatedEvent e4 = mockEntityEvent(ComponentUpdatedEvent, betaComponentId)
    AssetUpdatedEvent e5 = mockEntityEvent(AssetUpdatedEvent, alphaComponentId)
    AssetDeletedEvent e6 = mockEntityEvent(AssetDeletedEvent, betaComponentId)
    ComponentDeletedEvent e7 = mockEntityEvent(ComponentDeletedEvent, alphaComponentId)

    componentSubscriber.on(new EntityBatchEvent([e1, e2, e3, e4, e5, e6, e7]))

    // only alpha component should be deleted, beta events should be coalesced into a single put
    verify(searchFacet).delete(alphaComponentId)
    verify(searchFacet).put(betaComponentId)
    verifyNoMoreInteractions(searchFacet)
  }

  @Test
  public void noEntityEventsTriggerNoSearchUpdates() throws Exception {
    componentSubscriber.on(emptyBatchEvent)

    verifyNoMoreInteractions(searchFacet)
  }

  @Test
  public void entityEventsOnStoppedRepositoryDoesNotThrowException() throws Exception {
    doThrow(new InvalidStateException(STOPPED, STARTED)).when(searchFacet).put(any())

    componentSubscriber.on(simpleBatchEvent)
  }

  @Test
  public void entityEventsOnDeletedRepositoryDoesNotThrowException() throws Exception {
    doThrow(new InvalidStateException(DELETED, STARTED)).when(searchFacet).put(any())

    componentSubscriber.on(simpleBatchEvent)
  }

  @Test
  public void entityEventsOnDestroyedRepositoryDoesNotThrowException() throws Exception {
    doThrow(new InvalidStateException(DESTROYED, STARTED)).when(searchFacet).put(any())

    componentSubscriber.on(simpleBatchEvent)
  }

  @Test(expected = InvalidStateException)
  public void entityEventsOnFailedRepositoryThrowsException() throws Exception {
    doThrow(new InvalidStateException(FAILED, STARTED)).when(searchFacet).put(any())

    componentSubscriber.on(simpleBatchEvent)
  }
}
