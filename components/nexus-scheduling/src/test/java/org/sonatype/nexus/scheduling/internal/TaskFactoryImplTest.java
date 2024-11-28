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
package org.sonatype.nexus.scheduling.internal;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskDescriptor;
import org.sonatype.nexus.scheduling.TaskInfo;

import com.google.inject.Key;
import com.google.inject.util.Providers;
import org.eclipse.sisu.BeanEntry;
import org.eclipse.sisu.inject.BeanLocator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TaskFactoryImpl}.
 */
public class TaskFactoryImplTest
    extends TestSupport
{
  @Mock
  private BeanLocator beanLocator;

  @Mock
  private TaskInfo taskInfo;

  @Mock
  private DatabaseCheck databaseCheck;

  private TaskFactoryImpl underTest;

  @Before
  public void setUp() {
    BeanEntry<Annotation, SimpleTask> simpleTaskBeanEntry = mock(BeanEntry.class);
    when(simpleTaskBeanEntry.getImplementationClass()).thenReturn(SimpleTask.class);
    when(simpleTaskBeanEntry.getProvider()).thenReturn(Providers.of(new SimpleTask()));
    when(beanLocator.locate(any(Key.class))).thenReturn(Collections.singletonList(simpleTaskBeanEntry));
    when(databaseCheck.isAllowedByVersion(any())).thenReturn(true);
    underTest = new TaskFactoryImpl(beanLocator, databaseCheck);
  }

  /*
   * register and find descriptor
   */
  @Test
  public void testRegisterAndFindDescriptor() {
    assert underTest.getDescriptors().isEmpty();

    underTest.addDescriptor(new SimpleTaskDescriptor());
    assertThat(underTest.getDescriptors(), hasSize(1));

    assertThat(underTest.findDescriptor(SimpleTaskDescriptor.TYPE_ID), notNullValue());

    assertThat(underTest.findDescriptor("no-such-type-id"), nullValue());
  }

  /*
   * descriptor list is immutable
   */
  @Test
  public void testDescriptorListIsImmutable() {
    SimpleTaskDescriptor descriptor = new SimpleTaskDescriptor();
    List<TaskDescriptor> descriptors = underTest.getDescriptors();
    // can not add directly to the list
    assertThrows(UnsupportedOperationException.class, () -> descriptors.add(descriptor));

    // can not remove from list
    underTest.addDescriptor(new SimpleTaskDescriptor());

    Iterator<TaskDescriptor> iter = underTest.getDescriptors().iterator();
    assertThrows(UnsupportedOperationException.class, () -> iter.remove());
  }

  /*
   * create missing descriptor
   */
  @Test
  public void testCreateMissingDescriptor() {
    TaskConfiguration config = new TaskConfiguration();
    config.setId(UUID.randomUUID().toString());
    config.setTypeId("no-such-type-id");

    assertThrows(IllegalArgumentException.class, () -> underTest.create(config, taskInfo));
  }

  @Test
  public void testCreateTask() {
    underTest.addDescriptor(new SimpleTaskDescriptor());

    TaskConfiguration config = new TaskConfiguration();
    config.setId(UUID.randomUUID().toString());
    config.setTypeId(SimpleTaskDescriptor.TYPE_ID);
    Task task = underTest.create(config, taskInfo);

    assertThat(task, notNullValue());
    assertThat(task, instanceOf(SimpleTask.class));
    assertThat(task.getTaskInfo(), is(taskInfo));
  }
}
