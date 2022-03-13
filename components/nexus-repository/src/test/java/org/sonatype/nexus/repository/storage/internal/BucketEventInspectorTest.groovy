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
package org.sonatype.nexus.repository.storage.internal

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.entity.EntityMetadata
import org.sonatype.nexus.common.event.EventManager
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.manager.RepositoryMetadataUpdatedEvent

import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.instanceOf
import static org.mockito.ArgumentMatchers.isA
import static org.mockito.Mockito.atLeastOnce
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

class BucketEventInspectorTest
    extends TestSupport
{
  static final String MAVEN_CENTRAL_NAME = 'maven-central'

  @Mock
  RepositoryManager repositoryManager

  @Mock
  Repository mavenCentralRepository

  @Mock
  EventManager eventManager

  @Mock
  EntityMetadata entityMetadata

  BucketEventInspector underTest

  @Before
  void setup() {
    underTest = new BucketEventInspector(repositoryManager, eventManager)
  }

  @Test
  void 'post metadata-updated-event when bucket of existing repository is updated'() {
    when(repositoryManager.get(MAVEN_CENTRAL_NAME)).thenReturn(mavenCentralRepository)
    BucketUpdatedEvent bucketEvent = new BucketUpdatedEvent(entityMetadata, MAVEN_CENTRAL_NAME)
    underTest.onBucketUpdated(bucketEvent)
    ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object)
    verify(eventManager, atLeastOnce()).post(eventCaptor.capture())
    Object repoEvent = eventCaptor.allValues.last()
    assertThat(repoEvent, is(instanceOf(RepositoryMetadataUpdatedEvent)))
    assertThat(repoEvent.repository, is(mavenCentralRepository))
  }

  @Test
  void 'do not post metadata-updated-event when bucket of deleted repository is updated'() {
    BucketUpdatedEvent bucketEvent = new BucketUpdatedEvent(entityMetadata, 'some-deleted-repo$uuid')
    underTest.onBucketUpdated(bucketEvent)
    verify(eventManager, never()).post(isA(RepositoryMetadataUpdatedEvent))
  }
}
