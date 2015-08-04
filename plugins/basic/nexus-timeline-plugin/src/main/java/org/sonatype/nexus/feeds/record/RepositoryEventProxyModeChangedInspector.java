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
package org.sonatype.nexus.feeds.record;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.events.Asynchronous;
import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.feeds.FeedRecorder;
import org.sonatype.nexus.proxy.events.RepositoryEventProxyModeChanged;
import org.sonatype.nexus.proxy.repository.ProxyMode;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * @author Juven Xu
 */
@Named
@Singleton
public class RepositoryEventProxyModeChangedInspector
    extends AbstractFeedRecorderEventInspector
    implements EventSubscriber, Asynchronous
{

  @Subscribe
  @AllowConcurrentEvents
  public void inspect(final RepositoryEventProxyModeChanged evt) {
    RepositoryEventProxyModeChanged revt = (RepositoryEventProxyModeChanged) evt;

    StringBuilder sb = new StringBuilder("The proxy mode of repository '");

    sb.append(revt.getRepository().getName());

    sb.append("' (ID='").append(revt.getRepository().getId()).append("') was set to ");

    if (ProxyMode.ALLOW.equals(revt.getNewProxyMode())) {
      sb.append("Allow.");
    }
    else if (ProxyMode.BLOCKED_AUTO.equals(revt.getNewProxyMode())) {
      sb.append("Blocked (auto).");
    }
    else if (ProxyMode.BLOCKED_MANUAL.equals(revt.getNewProxyMode())) {
      sb.append("Blocked (by user).");
    }
    else {
      sb.append(revt.getRepository().getProxyMode().toString()).append(".");
    }

    sb.append(" The previous state was ");

    if (ProxyMode.ALLOW.equals(revt.getOldProxyMode())) {
      sb.append("Allow.");
    }
    else if (ProxyMode.BLOCKED_AUTO.equals(revt.getOldProxyMode())) {
      sb.append("Blocked (auto).");
    }
    else if (ProxyMode.BLOCKED_MANUAL.equals(revt.getOldProxyMode())) {
      sb.append("Blocked (by user).");
    }
    else {
      sb.append(revt.getOldProxyMode().toString()).append(".");
    }

    if (revt.getCause() != null) {
      sb.append(" Last detected transport error: ").append(revt.getCause().getMessage());
    }

    getFeedRecorder().addSystemEvent(FeedRecorder.SYSTEM_REPO_PSTATUS_CHANGES_ACTION, sb.toString());
  }

}
