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
package org.sonatype.nexus.testsuite.testsupport.system;

import org.sonatype.nexus.common.event.EventManager;

import org.junit.rules.ExternalResource;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * Any TestSystem impl in the new IT framework that needs to cleanup when done should extend this support class
 * to make sure that any async events that may get spawned are left to complete before moving on.  So that we
 * (for example) don't have a cleanuppolicy get removed which kicks off an async event handler that is updating
 * repositories to remove that cleanuppolicy from repo config, while at the same time the teardown process has moved
 * to deleting repositories, which may fail because the repo is also being updated at same time, i.e.
 * https://issues.sonatype.org/browse/NEXUS-27379
 */
public abstract class TestSystemSupport
    extends ExternalResource
{
  private final EventManager eventManager;

  protected TestSystemSupport(final EventManager eventManager) {
    this.eventManager = checkNotNull(eventManager);
  }

  protected void waitForCalmPeriod() {
    await().atMost(5, SECONDS).until(eventManager::isCalmPeriod);
  }

  @Override
  public void before() {
    doBefore();
  }

  protected void doBefore() {
    // Do nothing by default
  }

  protected abstract void doAfter();

  @Override
  public void after() {
    doAfter();
    waitForCalmPeriod();
  }
}
