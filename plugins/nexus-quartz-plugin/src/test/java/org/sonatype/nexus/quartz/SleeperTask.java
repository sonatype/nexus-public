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
package org.sonatype.nexus.quartz;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import org.sonatype.nexus.scheduling.TaskSupport;

/**
 * Simple sleeper task that is not cancelable.
 */
@Named
public class SleeperTask
    extends TaskSupport
{
  static final String RESULT_KEY = "result";

  static CountDownLatch meWait;

  static CountDownLatch youWait;

  static Exception exception;

  static void reset() {
    meWait = new CountDownLatch(1);
    youWait = new CountDownLatch(1);
    exception = null;
  }

  @Override
  protected String execute() throws Exception {
    youWait.countDown(); // task signals "started" to test

    while (!meWait.await(1L, TimeUnit.SECONDS)) { // test signals "finish" to this task
      doTheWork();
    }

    if (exception != null) {
      throw exception;
    }

    return getConfiguration().getString(RESULT_KEY);
  }

  @Override
  public String getMessage() {
    return "Message is:" + getConfiguration().getString(RESULT_KEY);
  }

  protected void doTheWork() throws Exception {
    Thread.sleep(10L); // kinda working
  }
}
