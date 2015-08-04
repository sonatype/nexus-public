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

import org.slf4j.MDC;

/**
 * Support for {@link NexusTask} implementations.
 *
 * @since 2.8
 */
public abstract class NexusTaskSupport
    extends AbstractNexusTask<Void>
{
  @Override
  protected String getAction() {
    return getClass().getSimpleName();
  }

  /**
   * Custom logging start message.
   */
  @Override
  protected String getLoggedMessage(final String action) {
    return String.format("%s %s", action.toUpperCase(), getMessage());
  }

  /**
   * Custom logging stopped message.
   */
  @Override
  protected String getLoggedMessage(String action, long started) {
    return String.format("%s %s", action.toUpperCase(), getMessage());
  }

  @Override
  protected Void doRun() throws Exception {
    MDC.put(NexusTaskSupport.class.getSimpleName(), getAction());
    try {
      execute();
    }
    finally {
      MDC.remove(NexusTaskSupport.class.getSimpleName());
    }

    // return value is meaningless in a background task
    return null;
  }

  protected abstract void execute() throws Exception;
}
