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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.feeds.FeedRecorder;
import org.sonatype.nexus.feeds.SystemEvent;

/**
 * The system changes feed.
 *
 * @author cstamas
 */
@Named(SystemRepositoryStatusChangesFeedSource.CHANNEL_KEY)
@Singleton
public class SystemRepositoryStatusChangesFeedSource
    extends AbstractSystemFeedSource
{
  public static final String CHANNEL_KEY = "systemRepositoryStatusChanges";

  public List<SystemEvent> getEventList(Integer from, Integer count, Map<String, String> params) {
    return getFeedRecorder().getSystemEvents(
        new HashSet<String>(Arrays.asList(FeedRecorder.SYSTEM_REPO_LSTATUS_CHANGES_ACTION,
            FeedRecorder.SYSTEM_REPO_PSTATUS_CHANGES_ACTION, FeedRecorder.SYSTEM_REPO_PSTATUS_AUTO_CHANGES_ACTION)),
        from, count, null);
  }

  public String getFeedKey() {
    return CHANNEL_KEY;
  }

  public String getFeedName() {
    return getDescription();
  }

  @Override
  public String getDescription() {
    return "Repository Status Changes in Nexus (user interventions and automatic).";
  }

  @Override
  public String getTitle() {
    return "Repository Status Changes";
  }

}
