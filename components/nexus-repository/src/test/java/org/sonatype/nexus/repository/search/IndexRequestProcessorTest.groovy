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
import org.sonatype.nexus.common.event.EventManager
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
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import org.mockito.Mock

import static org.mockito.Mockito.any
import static org.mockito.Mockito.anySet
import static org.mockito.Mockito.doThrow
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.verifyNoMoreInteractions
import static org.mockito.Mockito.verifyZeroInteractions
import static org.mockito.Mockito.when
import static org.sonatype.nexus.repository.FacetSupport.State.DELETED
import static org.sonatype.nexus.repository.FacetSupport.State.DESTROYED
import static org.sonatype.nexus.repository.FacetSupport.State.FAILED
import static org.sonatype.nexus.repository.FacetSupport.State.STARTED
import static org.sonatype.nexus.repository.FacetSupport.State.STOPPED

@RunWith(Parameterized.class)
class IndexRequestProcessorTest
    extends TestSupport
{

  @Parameters
  public static Collection<Boolean> data() {
    return [false, true]
  }

  @Parameter
  public boolean bulkProcessing

  EntityId alphaComponentId = new DetachedEntityId('#3:1')

  EntityId betaComponentId = new DetachedEntityId('#4:2')

  EntityBatchEvent emptyBatchEvent = new EntityBatchEvent([])

  EntityBatchEvent simpleBatchEvent = new EntityBatchEvent([
    mockEntityEvent(ComponentCreatedEvent, alphaComponentId)
  ])

  @Mock
  RepositoryManager repositoryManager

  @Mock
  EventManager eventManager

  @Mock
  SearchService searchService

  @Mock
  Repository repository

  @Mock
  SearchFacet searchFacet

  @Mock
  StorageFacet storageFacet

  @Mock
  StorageTx storageTx

  IndexRequestProcessor indexRequestProcessor

  @Before
  public void setup() {
    indexRequestProcessor = new IndexRequestProcessor(repositoryManager, eventManager, searchService, bulkProcessing)
    when(repositoryManager.get('testRepo')).thenReturn(repository)
    when(repository.optionalFacet(SearchFacet)).thenReturn(Optional.of(searchFacet))
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

    indexRequestProcessor.on(new EntityBatchEvent([e1, e2, e3, e4, e5, e6, e7]))

    // only alpha component should be deleted, beta events should be coalesced into a single put

    if (bulkProcessing) {
      verify(searchFacet).bulkDelete([alphaComponentId] as Set)
      verify(searchFacet).bulkPut([betaComponentId] as Set)
    }
    else {
      verify(searchFacet).delete(alphaComponentId)
      verify(searchFacet).put(betaComponentId)
    }

    verifyNoMoreInteractions(searchFacet)
  }

  @Test
  public void entityEventsDoNotTriggerSearchUpdatesWhenDisabled() throws Exception {
    indexRequestProcessor.processEvents = false

    // mix of events; simulates creating new entities, plus updating and/or deleting old ones
    ComponentCreatedEvent e1 = mockEntityEvent(ComponentCreatedEvent, alphaComponentId)
    AssetCreatedEvent e2 = mockEntityEvent(AssetCreatedEvent, betaComponentId)
    AssetCreatedEvent e3 = mockEntityEvent(AssetCreatedEvent, alphaComponentId)
    ComponentUpdatedEvent e4 = mockEntityEvent(ComponentUpdatedEvent, betaComponentId)
    AssetUpdatedEvent e5 = mockEntityEvent(AssetUpdatedEvent, alphaComponentId)
    AssetDeletedEvent e6 = mockEntityEvent(AssetDeletedEvent, betaComponentId)
    ComponentDeletedEvent e7 = mockEntityEvent(ComponentDeletedEvent, alphaComponentId)

    indexRequestProcessor.on(new EntityBatchEvent([e1, e2, e3, e4, e5, e6, e7]))

    verifyZeroInteractions(searchFacet)
  }

  @Test
  public void noEntityEventsTriggerNoSearchUpdates() throws Exception {
    indexRequestProcessor.on(emptyBatchEvent)

    verifyNoMoreInteractions(searchFacet)
  }

  @Test
  public void entityEventsOnStoppedRepositoryDoesNotThrowException() throws Exception {
    throwOnPut(new InvalidStateException(STOPPED, STARTED))

    indexRequestProcessor.on(simpleBatchEvent)
  }

  @Test
  public void entityEventsOnDeletedRepositoryDoesNotThrowException() throws Exception {
    throwOnPut(new InvalidStateException(DELETED, STARTED))

    indexRequestProcessor.on(simpleBatchEvent)
  }

  @Test
  public void entityEventsOnDestroyedRepositoryDoesNotThrowException() throws Exception {
    throwOnPut(new InvalidStateException(DESTROYED, STARTED))

    indexRequestProcessor.on(simpleBatchEvent)
  }

  @Test(expected = InvalidStateException)
  public void entityEventsOnFailedRepositoryThrowsException() throws Exception {
    throwOnPut(new InvalidStateException(FAILED, STARTED))

    indexRequestProcessor.on(simpleBatchEvent)
  }

  void throwOnPut(final Throwable throwable) {
    if (bulkProcessing) {
      doThrow(throwable).when(searchFacet).bulkPut(anySet())
    }
    else {
      doThrow(throwable).when(searchFacet).put(any())
    }
  }
}
