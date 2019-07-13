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
package org.sonatype.nexus.testsuite.testsupport.performance;

import java.util.concurrent.Callable;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;

/**
 * Waits for Nexus to be in a calm state.
 */
public class WaitForCalmPeriod implements Callable<Void>
{
  final EventManager eventManager;

  public WaitForCalmPeriod(final EventManager eventManager) {
    this.eventManager = eventManager;
  }

  @Override
  public Void call() throws Exception {
    NexusPaxExamSupport.waitFor(eventManager::isCalmPeriod);
    return null;
  }
}
