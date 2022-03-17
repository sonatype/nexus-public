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
package org.sonatype.nexus.testsuite.feed.nexus3882;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.test.utils.FeedUtil;
import org.sonatype.nexus.test.utils.UserCreationUtil;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.test.utils.NexusRequestMatchers.hasStatusCode;

/**
 * Tests for fail to login entries in feeds.
 */
public class Nexus3882IPAtAthenticationFailureFeedIT
    extends AbstractPrivilegeTest
{

  @SuppressWarnings("unchecked")
  @Test
  public void failAuthentication()
      throws Exception
  {
    TestContainer.getInstance().getTestContext().setUsername("juka");
    TestContainer.getInstance().getTestContext().setPassword("juka");

    assertThat(UserCreationUtil.login(), hasStatusCode(401));

    // NexusAuthenticationEventInspector is asynchronous
    getEventInspectorsUtil().waitForCalmPeriod();

    TestContainer.getInstance().getTestContext().useAdminForRequests();

    SyndFeed feed = FeedUtil.getFeed("authcAuthz");

    List<SyndEntry> entries = feed.getEntries();

    Assert.assertTrue("Expected more then 1 entries, but got " + entries.size() + " - "
        + entries, entries.size() >= 1);

    validateIP(entries);
  }

  private static final Pattern V4 =
      Pattern.compile(
          "(([2]([5][0-5]|[0-4][0-9]))|([1][0-9]{2})|([1-9]?[0-9]))(\\.(([2]([5][0-5]|[0-4][0-9]))|([1][0-9]{2})|([1-9]?[0-9]))){3}");

  private void validateIP(List<SyndEntry> entries)
      throws Exception
  {
    StringBuilder titles = new StringBuilder();

    for (SyndEntry entry : entries) {
      // check if the title contains the file name (pom or jar)
      String title = entry.getDescription().getValue();
      titles.append(title);
      titles.append(',');

      Matcher match = V4.matcher(title);
      if (match.find()) {
        return;
      }
    }

    Assert.fail(titles.toString());
  }

}
