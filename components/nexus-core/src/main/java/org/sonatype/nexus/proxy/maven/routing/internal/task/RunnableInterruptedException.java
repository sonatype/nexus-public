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

/**
 * Runtime exception thrown in cases when runnable task's running thread is interrupted. Semantical meaning is almost
 * same as {@link InterruptedException} meaning, except this one is unchecked exception.
 *
 * @author cstamas
 * @since 2.4
 */
@SuppressWarnings("serial")
public class RunnableInterruptedException
    extends RuntimeException
{
  /**
   * Constructor.
   *
   * @param message the interruption message.
   */
  public RunnableInterruptedException(final String message) {
    super(message);
  }
}
