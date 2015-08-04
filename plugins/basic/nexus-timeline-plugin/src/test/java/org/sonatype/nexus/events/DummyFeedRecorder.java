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
package org.sonatype.nexus.events;

import java.util.List;
import java.util.Set;

import org.sonatype.nexus.feeds.AuthcAuthzEvent;
import org.sonatype.nexus.feeds.FeedRecorder;
import org.sonatype.nexus.feeds.NexusArtifactEvent;
import org.sonatype.nexus.feeds.SystemEvent;
import org.sonatype.nexus.feeds.SystemProcess;
import org.sonatype.nexus.timeline.Entry;

import com.google.common.base.Predicate;

public class DummyFeedRecorder
    implements FeedRecorder
{
  int receivedEventCount = 0;

  public void shutdown() {
    //
  }

  public int getReceivedEventCount() {
    return receivedEventCount;
  }

  @Override
  public void addNexusArtifactEvent(final NexusArtifactEvent nae) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void addSystemEvent(final String action, final String message) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void addAuthcAuthzEvent(AuthcAuthzEvent evt) {
    receivedEventCount++;
  }

  @Override
  public SystemProcess systemProcessStarted(final String action, final String message) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void systemProcessFinished(final SystemProcess prc, final String finishMessage) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void systemProcessCanceled(final SystemProcess prc, final String cancelMessage) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void systemProcessBroken(final SystemProcess prc, final Throwable e) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public List<NexusArtifactEvent> getNexusArtifectEvents(final Set<String> subtypes, final Integer from,
                                                         final Integer count,
                                                         final Predicate<Entry> filter)
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public List<SystemEvent> getSystemEvents(final Set<String> subtypes, final Integer from, final Integer count,
                                           final Predicate<Entry> filter)
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public List<AuthcAuthzEvent> getAuthcAuthzEvents(final Set<String> subtypes, final Integer from,
                                                   final Integer count,
                                                   final Predicate<Entry> filter)
  {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
}
