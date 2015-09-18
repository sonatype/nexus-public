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

import java.util.List;

import javax.inject.Named;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskDescriptor;
import org.sonatype.nexus.scheduling.TaskSupport;
import org.sonatype.nexus.scheduling.internal.Tasks.TaskWithDescriptor;
import org.sonatype.nexus.scheduling.internal.Tasks.TaskWithDescriptorDescriptor;
import org.sonatype.nexus.scheduling.internal.Tasks.TaskWithoutDescriptor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.eclipse.sisu.BeanEntry;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.fail;

public class DefaultTaskFactoryTest
    extends TestSupport
{
  private DefaultTaskFactory nexusTaskFactory;

  @Before
  public void prepare() {
    final BeanEntry<Named, Task> be1 = Tasks.beanEntry(TaskWithDescriptor.class);
    final BeanEntry<Named, Task> be2 = Tasks.beanEntry(TaskWithoutDescriptor.class);
    nexusTaskFactory = new DefaultTaskFactory(ImmutableList.of(be1, be2),
        Lists.<TaskDescriptor<?>>newArrayList(new TaskWithDescriptorDescriptor()));
  }

  @Test
  public void listTaskDescriptors() {
    final List<TaskDescriptor<?>> descriptorList = nexusTaskFactory.listTaskDescriptors();
    assertThat(descriptorList, hasSize(2));
  }

  @Test
  public void resolveTaskDescriptorByTypeId() {
    assertThat(nexusTaskFactory.resolveTaskDescriptorByTypeId(TaskWithDescriptor.class.getName()), is(notNullValue()));
    assertThat(nexusTaskFactory.resolveTaskDescriptorByTypeId(TaskWithoutDescriptor.class.getName()),
        is(notNullValue()));
    assertThat(nexusTaskFactory.resolveTaskDescriptorByTypeId(String.class.getName()), is(nullValue()));
    assertThat(nexusTaskFactory.resolveTaskDescriptorByTypeId("foobar"), is(nullValue()));
  }

  @Test
  public void createTaskInstance() {
    final TaskConfiguration c1 = new TaskConfiguration();
    c1.setId("id");
    c1.setTypeId(nexusTaskFactory.resolveTaskDescriptorByTypeId(TaskWithDescriptor.class.getName()).getId());
    final TaskSupport task1 = nexusTaskFactory.createTaskInstance(c1);
    assertThat(task1, is(instanceOf(TaskWithDescriptor.class)));
    assertThat(task1.taskConfiguration().getTypeId(), equalTo(new TaskWithDescriptorDescriptor().getId()));

    final TaskConfiguration c2 = new TaskConfiguration();
    c2.setId("id");
    c2.setTypeId(nexusTaskFactory.resolveTaskDescriptorByTypeId(TaskWithoutDescriptor.class.getName()).getId());
    final Task task2 = nexusTaskFactory.createTaskInstance(c2);
    assertThat(task2, is(instanceOf(TaskWithoutDescriptor.class)));

    final TaskConfiguration c3 = new TaskConfiguration();
    c3.setId("id");
    c2.setTypeId("foobar");
    try {
      final Task task3 = nexusTaskFactory.createTaskInstance(c2);
      fail("This should not return");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("foobar"));
    }
  }
}
