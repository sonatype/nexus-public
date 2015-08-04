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
package org.sonatype.scheduling.iterators;

import java.util.Date;

public abstract class AbstractSchedulerIterator
    implements SchedulerIterator
{
  private final Date startingDate;

  private final Date endingDate;

  public AbstractSchedulerIterator(Date startingDate) {
    this(startingDate, null);
  }

  public AbstractSchedulerIterator(Date startingDate, Date endingDate) {
    super();

    if (startingDate == null) {
      throw new NullPointerException("Starting Date of " + this.getClass().getName() + " cannot be null!");
    }

    this.startingDate = startingDate;

    this.endingDate = endingDate;
  }

  public Date getStartingDate() {
    return startingDate;
  }

  public Date getEndingDate() {
    return endingDate;
  }

  public final Date peekNext() {
    Date current = doPeekNext();

    if (current == null || (getEndingDate() != null && current.after(getEndingDate()))) {
      return null;
    }
    else {
      return current;
    }
  }

  public final Date next() {
    Date result = peekNext();

    Date now = new Date();

    // Blow through all iterations up until we reach some point in the future (even a single millisecond will do)
    while (result != null && result.before(now)) {
      stepNext();

      result = peekNext();
    }

    stepNext();

    return result;
  }

  public boolean isFinished() {
    return peekNext() == null;
  }

  protected abstract Date doPeekNext();

  protected abstract void stepNext();

}
