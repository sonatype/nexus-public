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
package org.sonatype.nexus.test.utils;

import java.io.IOException;

import org.sonatype.nexus.integrationtests.NexusRestClient;

import org.restlet.data.Status;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Util class to talk with nexus events
 */
public class EventInspectorsUtil
{

  private final NexusRestClient nexusRestClient;

  public EventInspectorsUtil(final NexusRestClient nexusRestClient) {
    this.nexusRestClient = checkNotNull(nexusRestClient);
  }

  /**
   * Hold execution until asynchronous events at nexus side stop running
   */
  public void waitForCalmPeriod(final long waitMillis)
      throws IOException, InterruptedException
  {
    Thread.yield();
    if (waitMillis > 0) {
      Thread.sleep(waitMillis);
    }

    final Status status =
        nexusRestClient.doGetForStatus("service/local/eventInspectors/isCalmPeriod?waitForCalm=true");

    if (status.getCode() != Status.SUCCESS_OK.getCode()) {
      throw new IOException("The isCalmPeriod REST resource reported an error ("
          + status.toString() + "), bailing out!");
    }
  }

  /**
   * Hold execution until asynchronous events at nexus side stop running
   */
  public void waitForCalmPeriod()
      throws IOException, InterruptedException
  {
    waitForCalmPeriod(0);
  }
}
