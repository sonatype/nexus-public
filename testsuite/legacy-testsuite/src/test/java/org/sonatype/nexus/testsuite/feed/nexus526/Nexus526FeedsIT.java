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
package org.sonatype.nexus.testsuite.feed.nexus526;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.test.utils.FeedUtil;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import org.apache.maven.index.artifact.Gav;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for deployment entries in feeds.
 */
public class Nexus526FeedsIT
    extends AbstractNexusIntegrationTest
{

  private Gav gav;

  public Nexus526FeedsIT()
      throws Exception
  {
    super("nexus-test-harness-repo");
    this.gav =
        new Gav(this.getTestId(), "artifact1", "1.0.0", null, "jar", 0, new Date().getTime(), "Artifact 1",
            false, null, false, null);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void recentCachedOrDeployedFileFeedTest()
      throws Exception
  {
    SyndFeed feed = FeedUtil.getFeed("recentlyCachedOrDeployedFiles");
    this.validateLinksInFeeds(feed);

    List<SyndEntry> entries = feed.getEntries();

    Assert.assertTrue("Expected more then 2 entries, but got " + entries.size() + " - " + entries,
        entries.size() >= 2);

    List<SyndEntry> latestEntries = new ArrayList<SyndEntry>(2);

    latestEntries.add(entries.get(0));

    latestEntries.add(entries.get(1));

    validateFileInFeedEntries(latestEntries);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void recentChangedFileFeedTest()
      throws Exception
  {
    SyndFeed feed = FeedUtil.getFeed("recentlyChangedFiles");
    this.validateLinksInFeeds(feed);

    List<SyndEntry> entries = feed.getEntries();

    Assert.assertTrue("Expected more then 2 entries, but got " + entries.size() + " - " + entries,
        entries.size() >= 2);

    List<SyndEntry> latestEntries = new ArrayList<SyndEntry>(2);

    latestEntries.add(entries.get(0));

    latestEntries.add(entries.get(1));

    validateFileInFeedEntries(latestEntries);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void recentDeployedFileFeedTest()
      throws Exception
  {
    SyndFeed feed = FeedUtil.getFeed("recentlyDeployedFiles");
    this.validateLinksInFeeds(feed);

    List<SyndEntry> entries = feed.getEntries();

    Assert.assertTrue("Expected more then 2 entries, but got " + entries.size() + " - " + entries,
        entries.size() >= 2);

    List<SyndEntry> latestEntries = new ArrayList<SyndEntry>(2);

    latestEntries.add(entries.get(0));

    latestEntries.add(entries.get(1));

    validateFileInFeedEntries(latestEntries);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void recentCachedOrDeployedArtifactFeedTest()
      throws Exception
  {
    SyndFeed feed = FeedUtil.getFeed("recentlyCachedOrDeployedArtifacts");
    this.validateLinksInFeeds(feed);

    List<SyndEntry> entries = feed.getEntries();

    Assert.assertTrue("Expected more then 1 entries, but got " + entries.size() + " - " + entries,
        entries.size() >= 1);

    List<SyndEntry> latestEntries = new ArrayList<SyndEntry>(1);

    latestEntries.add(entries.get(0));

    validateArtifactInFeedEntries(latestEntries);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void recentChangedFileArtifactTest()
      throws Exception
  {
    SyndFeed feed = FeedUtil.getFeed("recentlyChangedArtifacts");
    this.validateLinksInFeeds(feed);

    List<SyndEntry> entries = feed.getEntries();

    Assert.assertTrue("Expected more then 1 entries, but got " + entries.size() + " - " + entries,
        entries.size() >= 1);

    List<SyndEntry> latestEntries = new ArrayList<SyndEntry>(1);

    latestEntries.add(entries.get(0));

    validateArtifactInFeedEntries(latestEntries);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void recentDeployedArtifactFeedTest()
      throws Exception
  {
    SyndFeed feed = FeedUtil.getFeed("recentlyDeployedArtifacts");
    this.validateLinksInFeeds(feed);

    List<SyndEntry> entries = feed.getEntries();

    Assert.assertTrue("Expected more then 1 entries, but got " + entries.size() + " - " + entries,
        entries.size() >= 1);

    List<SyndEntry> latestEntries = new ArrayList<SyndEntry>(1);

    latestEntries.add(entries.get(0));

    validateArtifactInFeedEntries(latestEntries);
  }

  private void validateArtifactInFeedEntries(List<SyndEntry> entries)
      throws Exception
  {
    String link =
        getBaseNexusUrl() + "content/repositories/" + getTestRepositoryId() + "/"
            + getRelitiveArtifactPath(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), "pom", null);

    for (SyndEntry entry : entries) {
      // check if the title contains the groupid, artifactid, and version
      String title = entry.getTitle();

      Assert.assertTrue("Feed title does not contain the groupId. Title was: " + title,
          title.contains(gav.getGroupId()));

      Assert.assertTrue("Feed title does not contain the artifactId. Title was: " + title,
          title.contains(gav.getArtifactId()));

      Assert.assertTrue("Feed title does not contain the version. Title was: " + title,
          title.contains(gav.getVersion()));

      Assert.assertEquals(entry.getLink(), link);
    }
  }

  private void validateFileInFeedEntries(List<SyndEntry> entries)
      throws Exception
  {
    String pomName = gav.getArtifactId() + "-" + gav.getVersion() + ".pom";

    String contentName = gav.getArtifactId() + "-" + gav.getVersion() + "." + gav.getExtension();

    for (SyndEntry entry : entries) {
      // check if the title contains the file name (pom or jar)
      String title = entry.getTitle();

      Assert.assertTrue(title.contains(pomName) || title.contains(contentName));
    }
  }

  private void validateLinksInFeeds(SyndFeed feed) {
    Assert.assertTrue("Feed link is wrong", feed.getLink().startsWith(this.getBaseNexusUrl()));

    List<SyndEntry> entries = feed.getEntries();

    for (SyndEntry syndEntry : entries) {
      Assert.assertTrue("Feed item link is wrong, is: " + syndEntry.getLink(),
          syndEntry.getLink().startsWith(this.getBaseNexusUrl()));
    }
  }
}
