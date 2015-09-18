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
import java.util.concurrent.CountDownLatch;

import javax.inject.Named;

import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;
import org.sonatype.nexus.scheduling.TaskSupport;

import com.google.common.base.Throwables;
import com.google.inject.Provider;
import org.eclipse.sisu.BeanEntry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Tasks
{
  public static class TaskWithDescriptor
      extends TaskSupport
  {
    @Override
    protected Void execute() throws Exception {
      return null;
    }

    @Override
    public String getMessage() {
      return getClass().getSimpleName();
    }
  }

  public static class TaskWithDescriptorDescriptor
      extends TaskDescriptorSupport
  {
    public TaskWithDescriptorDescriptor() {
      super(TaskWithDescriptor.class, "Task with descriptor");
    }
  }

  public static class TaskWithoutDescriptor
      extends TaskSupport
  {
    @Override
    protected Void execute() throws Exception {
      return null;
    }

    @Override
    public String getMessage() {
      return getClass().getSimpleName();
    }
  }

  public static BeanEntry<Named, Task> beanEntry(final Class<? extends Task> clz) {
    final BeanEntry<Named, Task> beanEntry = mock(BeanEntry.class);
    when(beanEntry.getKey()).thenReturn(new Named()
    {
      @Override
      public Class<? extends Annotation> annotationType() {
        return Named.class;
      }

      @Override
      public String value() {
        return clz.getName();
      }
    });
    when(beanEntry.getProvider()).thenReturn(new Provider<Task>()
    {
      @Override
      public Task get() {
        try {
          return clz.newInstance();
        }
        catch (Exception e) {
          throw Throwables.propagate(e);
        }
      }
    });
    when(beanEntry.getImplementationClass()).thenReturn((Class<Task>) clz);
    return beanEntry;
  }

  // ==

  public static class SleeperTask
      extends TaskSupport
  {
    static final String RESULT_KEY = "result";

    static CountDownLatch meWait;

    @Override
    protected String execute() throws Exception {
      meWait.await();
      return getConfiguration().getString(RESULT_KEY);
    }

    @Override
    public String getMessage() {
      return getClass().getSimpleName();
    }
  }
}
