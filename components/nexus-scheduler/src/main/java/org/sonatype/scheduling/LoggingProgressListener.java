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
package org.sonatype.scheduling;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple progress listener that logs messages into SLF4J API at Info level.
 *
 * @author cstamas
 */
public class LoggingProgressListener
    implements ProgressListener
{
  private static final Workunit ROOT = new Workunit("root", UNKNOWN_WORKUNITS);

  private final Logger logger;

  private final Deque<Workunit> workunits;

  private volatile boolean canceled = false;

  public LoggingProgressListener(final String name) {
    this(LoggerFactory.getLogger(name));
  }

  public LoggingProgressListener(final Logger logger) {
    if (logger == null) {
      throw new NullPointerException();
    }
    this.logger = logger;
    this.workunits = new ArrayDeque<Workunit>(Arrays.asList(ROOT));
    this.canceled = false;
  }

  public void beginTask(final String name) {
    beginTask(name, UNKNOWN_WORKUNITS);
  }

  public void beginTask(final String name, final int toDo) {
    workunits.push(new Workunit(name, toDo));

    if (logger.isInfoEnabled()) {
      if (UNKNOWN_WORKUNITS != toDo) {
        logger.info("{}: started ({} steps).", getStackedWorkunitNames(), toDo);
      }
      else {
        logger.info("{}: started.", getStackedWorkunitNames());
      }
    }
  }

  public void working(final int workDone) {
    working(null, workDone);
  }

  public void working(final String message) {
    working(message, 0);
  }

  public void working(final String message, final int workDone) {
    final Workunit wu = workunits.peek();

    wu.done(workDone);

    if (logger.isInfoEnabled() && message != null && message.trim().length() > 0) {
      if (UNKNOWN_WORKUNITS != wu.getToDo()) {
        logger.info("{}: {} ({}/{})", getStackedWorkunitNames(), message, wu.getDone(), wu.getToDo());
      }
      else {
        logger.info("{}: {} ({})", getStackedWorkunitNames(), message, wu.getDone());
      }
    }
  }

  public void endTask(final String message) {
    if (logger.isInfoEnabled()) {
      logger.info("{}: finished: {}", getStackedWorkunitNames(), message);
    }

    if (workunits.size() > 1) {
      workunits.pop();
    }
  }

  public boolean isCanceled() {
    return canceled;
  }

  public void cancel() {
    final String wus = getStackedWorkunitNames();

    if (logger.isDebugEnabled()) {
      if (wus != null && wus.trim().length() > 0) {
        logger.info("{}: canceled, bailing out (may take a while).", wus);
      }
      else {
        logger.info("Task canceled, bailing out (may take a while).");
      }
    }

    this.canceled = true;
  }

  // ==

  protected String getStackedWorkunitNames() {
    Iterator<Workunit> wi = workunits.descendingIterator();

    // skip root
    wi.next();

    if (wi.hasNext()) {
      final StringBuilder sb = new StringBuilder(wi.next().getName());

      for (; wi.hasNext(); ) {
        sb.append(" - ").append(wi.next().getName());
      }

      return sb.toString();
    }
    else {
      return "";
    }
  }

  // ==

  public static class Workunit
  {
    private final String name;

    private final int toDo;

    private int done;

    public Workunit(final String name, final int toDo) {
      this.name = name;
      this.toDo = toDo;
      this.done = 0;
    }

    public String getName() {
      return name;
    }

    public int getToDo() {
      return toDo;
    }

    public int getDone() {
      return done;
    }

    public void done(int done) {
      this.done += done;
    }
  }
}
