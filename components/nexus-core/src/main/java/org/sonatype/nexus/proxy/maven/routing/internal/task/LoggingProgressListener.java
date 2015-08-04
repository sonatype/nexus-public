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
package org.sonatype.nexus.proxy.maven.routing.internal.task;

import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A simple logging {@link ProgressListener} that logs to {@link Logger} at INFO level.
 *
 * @author cstamas
 * @since 2.4
 */
public class LoggingProgressListener
    implements ProgressListener
{
  private final static class TaskWork
  {
    private final String name;

    private int total;

    private int current;

    public TaskWork(final String name, final int total) {
      this.name = checkNotNull(name);
      this.total = total > -1 ? total : 0;
      this.current = 0;
    }

    public String getName() {
      return name;
    }

    public int getTotal() {
      return total;
    }

    public int getCurrent() {
      return current;
    }

    public void addWorkAmountDelta(int delta) {
      if (delta < 1) {
        return;
      }
      current = current + delta;
    }
  }

  private final Logger logger;

  private final Stack<TaskWork> tasksStack;

  /**
   * Constructor.
   */
  public LoggingProgressListener(final Class<?> clazz) {
    this(LoggerFactory.getLogger(clazz));
  }

  /**
   * Constructor.
   */
  public LoggingProgressListener(final Logger logger) {
    this.logger = logger;
    this.tasksStack = new Stack<TaskWork>();
  }

  @Override
  public void beginTask(String name, int workAmount) {
    final TaskWork tw = new TaskWork(name, workAmount);
    tasksStack.push(tw);
    logger.info("Begin task: {}, work to do {}.", tw.getName(), tw.getTotal());
  }

  @Override
  public void working(String message, int workAmountDelta) {
    final TaskWork tw = safePeek();
    tw.addWorkAmountDelta(workAmountDelta);
    logger.info("Working: {}, done {} out of {}.", message, tw.getCurrent(), tw.getTotal());
  }

  @Override
  public void endTask(String message) {
    final TaskWork tw = safePeek();
    logger.info("End task: {}, done {} out of {}.", message, tw.getCurrent(), tw.getTotal());
    tasksStack.pop();
  }

  // ==

  protected TaskWork safePeek() {
    if (tasksStack.isEmpty()) {
      tasksStack.push(new TaskWork("Task", -1));
    }
    return tasksStack.peek();
  }
}
