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
package org.sonatype.nexus.rest.feeds.sources;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.feeds.FeedRecorder;
import org.sonatype.nexus.feeds.SystemEvent;
import org.sonatype.nexus.maven.tasks.RebuildMavenMetadataTask;
import org.sonatype.nexus.maven.tasks.SnapshotRemoverTask;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.tasks.DeleteRepositoryFoldersTask;
import org.sonatype.nexus.tasks.EmptyTrashTask;
import org.sonatype.nexus.tasks.EvictUnusedProxiedItemsTask;
import org.sonatype.nexus.tasks.ExpireCacheTask;
import org.sonatype.nexus.tasks.RebuildAttributesTask;
import org.sonatype.nexus.tasks.SynchronizeShadowsTask;
import org.sonatype.nexus.timeline.tasks.PurgeTimeline;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import org.restlet.data.MediaType;

/**
 * The system changes feed.
 *
 * @author cstamas
 */
public abstract class AbstractSystemFeedSource
    extends AbstractFeedSource
{
  public abstract List<SystemEvent> getEventList(Integer from, Integer count, Map<String, String> params);

  public SyndFeed getFeed(Integer from, Integer count, Map<String, String> params) {
    List<SystemEvent> items = getEventList(from, count, params);

    SyndFeedImpl feed = new SyndFeedImpl();

    feed.setTitle(getTitle());

    feed.setDescription(getDescription());

    feed.setAuthor("Nexus " + getApplicationStatusSource().getSystemStatus().getVersion());

    feed.setPublishedDate(new Date());

    List<SyndEntry> entries = new ArrayList<SyndEntry>(items.size());

    SyndEntry entry = null;

    SyndContent content = null;

    String username = null;

    String ipAddress = null;

    for (SystemEvent item : items) {
      if (item.getEventContext().containsKey(AccessManager.REQUEST_USER)) {
        username = (String) item.getEventContext().get(AccessManager.REQUEST_USER);
      }
      else {
        username = null;
      }

      if (item.getEventContext().containsKey(AccessManager.REQUEST_REMOTE_ADDRESS)) {
        ipAddress = (String) item.getEventContext().get(AccessManager.REQUEST_REMOTE_ADDRESS);
      }
      else {
        ipAddress = null;
      }

      StringBuilder msg = new StringBuilder(item.getMessage()).append(". ");

      if (username != null) {
        msg.append(" It was initiated by a request from user ").append(username).append(".");
      }

      if (ipAddress != null) {
        msg.append(" The request was originated from IP address ").append(ipAddress).append(".");
      }

      entry = new SyndEntryImpl();

      if (FeedRecorder.SYSTEM_BOOT_ACTION.equals(item.getAction())) {
        entry.setTitle("Booting");
      }
      else if (FeedRecorder.SYSTEM_CONFIG_ACTION.equals(item.getAction())) {
        entry.setTitle("Configuration change");
      }
      else if (FeedRecorder.SYSTEM_REPO_LSTATUS_CHANGES_ACTION.equals(item.getAction())) {
        entry.setTitle("Repository local status change");
      }
      else if (FeedRecorder.SYSTEM_REPO_PSTATUS_CHANGES_ACTION.equals(item.getAction())) {
        entry.setTitle("Repository proxy mode change");
      }
      else if (FeedRecorder.SYSTEM_REPO_PSTATUS_AUTO_CHANGES_ACTION.equals(item.getAction())) {
        entry.setTitle("Repository proxy mode change (user intervention may be needed!)");
      }
      // manually set to not be dependent on it
      else if ("REINDEX".equals(item.getAction())) {
        entry.setTitle("Reindexing");
      }
      else if ("PUBLISHINDEX".equals(item.getAction())) {
        entry.setTitle("Publishing indexes");
      }
      else if ("DOWNLOADINDEX".equals(item.getAction())) {
        entry.setTitle("Downloading Indexes");
      }
      else if ("OPTIMIZE_INDEX".equals(item.getAction())) {
        entry.setTitle("Optimizing Indexes");
      }
      else if ("PUBLISHINDEX".equals(item.getAction())) {
        entry.setTitle("Publishing Indexes");
      }
      else if (RebuildAttributesTask.ACTION.equals(item.getAction())) {
        entry.setTitle("Rebuilding attributes");
      }
      else if (PurgeTimeline.ACTION.equals(item.getAction())) {
        entry.setTitle("Timeline purge");
      }
      else if (ExpireCacheTask.ACTION.equals(item.getAction())) {
        entry.setTitle("Expiring caches");
      }
      else if (EvictUnusedProxiedItemsTask.ACTION.equals(item.getAction())) {
        entry.setTitle("Evicting unused proxied items");
      }
      else if (SnapshotRemoverTask.SYSTEM_REMOVE_SNAPSHOTS_ACTION.equals(item.getAction())) {
        entry.setTitle("Removing snapshots");
      }
      else if (DeleteRepositoryFoldersTask.ACTION.equals(item.getAction())) {
        entry.setTitle("Removing repository folder");
      }
      else if (EmptyTrashTask.ACTION.equals(item.getAction())) {
        entry.setTitle("Emptying Trash");
      }
      else if (SynchronizeShadowsTask.ACTION.equals(item.getAction())) {
        entry.setTitle("Synchronizing Shadow Repository");
      }
      else if (RebuildMavenMetadataTask.REBUILD_MAVEN_METADATA_ACTION.equals(item.getAction())) {
        entry.setTitle("Rebuilding maven metadata files");
      }
      else {
        entry.setTitle(item.getAction());
      }

      content = new SyndContentImpl();

      content.setType(MediaType.TEXT_PLAIN.toString());

      content.setValue(msg.toString());

      entry.setPublishedDate(item.getEventDate());

      entry.setAuthor(feed.getAuthor());

      entry.setLink("/");

      entry.setDescription(content);

      entries.add(entry);
    }

    feed.setEntries(entries);

    return feed;
  }

}
