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

import javax.inject.Named;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskDescriptor;
import org.sonatype.nexus.scheduling.internal.Tasks.TaskWithDescriptor;
import org.sonatype.nexus.scheduling.internal.Tasks.TaskWithDescriptorDescriptor;
import org.sonatype.nexus.scheduling.internal.Tasks.TaskWithoutDescriptor;
import org.sonatype.nexus.scheduling.spi.TaskExecutorSPI;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.util.Providers;
import org.eclipse.sisu.BeanEntry;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class DefaultTaskSchedulerTest
    extends TestSupport
{
  private DefaultTaskScheduler nexusTaskScheduler;

  @Before
  public void prepare() {
    final BeanEntry<Named, Task> be1 = Tasks.beanEntry(TaskWithDescriptor.class);
    final BeanEntry<Named, Task> be2 = Tasks.beanEntry(TaskWithoutDescriptor.class);
    final DefaultTaskFactory nexusTaskFactory = new DefaultTaskFactory(
        ImmutableList.of(be1, be2), Lists.<TaskDescriptor<?>>newArrayList(new TaskWithDescriptorDescriptor()));
    nexusTaskScheduler = new DefaultTaskScheduler(nexusTaskFactory,
        Providers.<TaskExecutorSPI>of(null));
  }

  @Test
  public void createTaskConfigurationInstance() {
    final TaskConfiguration c1 = nexusTaskScheduler.createTaskConfigurationInstance(TaskWithDescriptor.class);
    assertThat(c1.getId(), notNullValue());
    assertThat(c1.getName(), equalTo("Task with descriptor"));
    assertThat(c1.getTypeId(), equalTo(TaskWithDescriptor.class.getSimpleName()));
    assertThat(c1.getTypeName(), equalTo("Task with descriptor"));
    assertThat(c1.isVisible(), is(true));
    assertThat(c1.isEnabled(), is(true));

    final TaskConfiguration c2 = nexusTaskScheduler.createTaskConfigurationInstance(TaskWithoutDescriptor.class);
    assertThat(c2.getId(), notNullValue());
    assertThat(c2.getName(), equalTo(TaskWithoutDescriptor.class.getSimpleName()));
    assertThat(c2.getTypeId(), equalTo(TaskWithoutDescriptor.class.getSimpleName()));
    assertThat(c2.getTypeName(), equalTo(TaskWithoutDescriptor.class.getSimpleName()));
    assertThat(c2.isVisible(), is(false));
    assertThat(c2.isEnabled(), is(true));

    try {
      final TaskConfiguration c3 = nexusTaskScheduler.createTaskConfigurationInstance("foobar");
      fail("This should not return");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("foobar"));
    }
  }
}
