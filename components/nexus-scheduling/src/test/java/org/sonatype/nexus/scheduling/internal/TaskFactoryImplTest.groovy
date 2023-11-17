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
package org.sonatype.nexus.scheduling.internal

import java.lang.annotation.Annotation

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.scheduling.TaskConfiguration
import org.sonatype.nexus.scheduling.TaskInfo

import com.google.inject.Key
import com.google.inject.util.Providers
import org.eclipse.sisu.BeanEntry
import org.eclipse.sisu.inject.BeanLocator
import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static org.junit.Assert.fail
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

/**
 * Tests for {@link TaskFactoryImpl}.
 */
class TaskFactoryImplTest
    extends TestSupport
{
  @Mock
  private BeanLocator beanLocator

  @Mock
  private TaskInfo taskInfo

  private TaskFactoryImpl underTest

  @Before
  void setUp() {
    BeanEntry<Annotation, SimpleTask> simpleTaskBeanEntry = mock(BeanEntry.class)
    when(simpleTaskBeanEntry.getImplementationClass()).thenReturn(SimpleTask)
    when(simpleTaskBeanEntry.getProvider()).thenReturn(Providers.of(new SimpleTask()))
    when(beanLocator.locate(any(Key))).thenReturn(Collections.singletonList(simpleTaskBeanEntry))
    underTest = new TaskFactoryImpl(beanLocator)
  }

  @Test
  void 'register and find descriptor'() {
    assert underTest.descriptors.isEmpty()

    underTest.addDescriptor(new SimpleTaskDescriptor())
    assert underTest.descriptors.size() == 1

    def descriptor1 = underTest.findDescriptor(SimpleTaskDescriptor.TYPE_ID)
    assert descriptor1 != null

    def descriptor2 = underTest.findDescriptor('no-such-type-id')
    assert descriptor2 == null
  }

  @Test
  void 'descriptor list is immutable'() {
    // can not add directly to the list
    try {
      underTest.descriptors.add(new SimpleTaskDescriptor())
      fail()
    }
    catch (UnsupportedOperationException e) {
      // expected
    }

    // can not remove from list
    underTest.addDescriptor(new SimpleTaskDescriptor())
    try {
      underTest.descriptors.iterator().remove()
      fail()
    }
    catch (UnsupportedOperationException e) {
      // expected
    }
  }

  @Test
  void 'create missing descriptor'() {
    def config = new TaskConfiguration(
        id: UUID.randomUUID().toString(),
        typeId: 'no-such-type-id'
    )
    try {
      underTest.create(config, taskInfo)
      fail()
    }
    catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  void 'create task'() {
    underTest.addDescriptor(new SimpleTaskDescriptor())

    def config = new TaskConfiguration(
        id: UUID.randomUUID().toString(),
        typeId: SimpleTaskDescriptor.TYPE_ID
    )
    def task = underTest.create(config, taskInfo)

    assert task != null
    assert task instanceof SimpleTask
    assert task.taskInfo == taskInfo
  }
}
