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
package org.sonatype.nexus.testsuite.feed.nexus779;

import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractPrivilegeTest;
import org.sonatype.nexus.test.utils.FeedUtil;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

public abstract class AbstractRssIT
    extends AbstractPrivilegeTest
{

  private static final String RECENTLY_DEPLOYED = "recentlyDeployedArtifacts";

  private List<SyndEntry> entries;

  public AbstractRssIT(String testRepositoryId) {
    super(testRepositoryId);
  }

  public AbstractRssIT() {
    super();
  }

  protected String entriesToString()
      throws Exception
  {
    if (entries == null) {
      return "No entries";
    }

    StringBuilder buffer = new StringBuilder();

    for (SyndEntry syndEntry : entries) {
      buffer.append("\n").append(syndEntry.getTitle());
    }

    return buffer.toString();
  }

  @SuppressWarnings("unchecked")
  protected boolean feedListContainsArtifact(String groupId, String artifactId, String version)
      throws Exception
  {
    SyndFeed feed = FeedUtil.getFeed(RECENTLY_DEPLOYED);
    entries = feed.getEntries();

    for (SyndEntry entry : entries) {
      if (entry.getTitle().contains(groupId) && entry.getTitle().contains(artifactId)
          && entry.getTitle().contains(version)) {
        return true;
      }
    }
    return false;
  }

}