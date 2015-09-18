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
package org.sonatype.nexus.scheduling;

/**
 * Runtime exception thrown in cases when task is interrupted. Semantically meaning is almost same as
 * {@link InterruptedException} except this one is unchecked exception and may carry some cause.
 *
 * @since 3.0
 */
public class TaskInterruptedException
    extends RuntimeException
{
  private final boolean canceled;

  /**
   * Ctor for "clean interruption" of task, usually by user or some expected condition.
   */
  public TaskInterruptedException(String message, boolean canceled)
  {
    super(message);
    this.canceled = canceled;
  }

  /**
   * Ctor for "erroneus interruption" of task, usually by some other unexpected exception.
   */
  public TaskInterruptedException(Throwable cause)
  {
    super(cause);
    this.canceled = false;
  }

  public boolean isCanceled() {
    return canceled;
  }
}
