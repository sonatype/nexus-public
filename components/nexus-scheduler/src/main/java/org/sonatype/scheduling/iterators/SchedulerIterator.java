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

public interface SchedulerIterator
{
  /**
   * Returns, or "peek"s the next run without updating internal state of iterator. Calling this method simultaneously
   * will always return the same date of "next" run.
   */
  Date peekNext();

  /**
   * Returns the date of next run and updates internal state of the iterator: "steps" over just like Iterator.next().
   * Calling this method simultaneously will always return new and new (different) dates of next run until it's
   * depleted.
   */
  Date next();

  /**
   * Returns true when iterator is depleted, no more runs needed.
   */
  boolean isFinished();

  /**
   * Resets the scheduler internal state to start with from date passed in as parameter.
   */
  void resetFrom(Date from);
}
