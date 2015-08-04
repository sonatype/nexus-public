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
package org.sonatype.nexus.testsuite.misc.nexus4301;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.test.utils.FeedUtil;
import org.sonatype.nexus.test.utils.ITHelperLogUtils;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import org.junit.Assert;
import org.junit.Test;

/**
 * See NEXUS-4301: Load test impact of lots of WARN/ERROR logs as being recorded into Nexus feeds.
 *
 * @author adreghiciu@gmail.com
 */
public class Nexus4301WarnErrorLogsLoadTestIT
    extends AbstractNexusIntegrationTest
{

  /**
   * When an ERROR is logged a corresponding feed entry should be created.
   */
  @Test
  public void test()
      throws Exception
  {

    final List<String> messagesError = new ArrayList<String>();
    final List<String> messagesWarn = new ArrayList<String>();

    Thread threadError = new Thread()
    {

      @Override
      public void run() {
        for (int i = 0; i < 500; i++) {
          String message = generateMessage("error");
          try {
            ITHelperLogUtils.error(message);
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
          messagesError.add(message);
          yield();
        }
      }

    };

    Thread threadWarn = new Thread()
    {

      @Override
      public void run() {
        for (int i = 0; i < 500; i++) {
          String message = generateMessage("warn");
          try {
            ITHelperLogUtils.warn(message);
          }
          catch (Exception e) {
            throw new RuntimeException(e);
          }
          messagesError.add(message);
          yield();
        }
      }

    };

    threadError.start();
    threadWarn.start();

    threadError.join();
    threadWarn.join();

    // logging is asynchronous so give it a bit of time
    getEventInspectorsUtil().waitForCalmPeriod();

    final SyndFeed feed = FeedUtil.getFeed("errorWarning", 0, Integer.MAX_VALUE);

    for (String message : messagesError) {
      assertFeedContainsEntryFor(feed, message);
    }

    for (String message : messagesWarn) {
      assertFeedContainsEntryFor(feed, message);
    }
  }

  private String generateMessage(String id) {
    return this.getClass().getName() + "-" + System.currentTimeMillis() + "(" + id + ")";
  }

  private void assertFeedContainsEntryFor(SyndFeed feed, String message)
      throws Exception
  {
    @SuppressWarnings("unchecked")
    List<SyndEntry> entries = feed.getEntries();
    for (SyndEntry entry : entries) {
      SyndContent description = entry.getDescription();
      if (description != null && description.getValue().contains(message)) {
        return;
      }
    }
    Assert.fail("Feed does not contain entry for " + message);
  }

}
