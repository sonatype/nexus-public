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
import org.sonatype.nexus.proxy.events.RepositoryEventLocalStatusChanged;
import org.sonatype.nexus.proxy.repository.LocalStatus;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * @author Juven Xu
 */
@Named
@Singleton
public class RepositoryEventLocalStatusChangedInspector
    extends AbstractFeedRecorderEventInspector
    implements EventSubscriber, Asynchronous
{
  @Subscribe
  @AllowConcurrentEvents
  public void inspect(final RepositoryEventLocalStatusChanged revt) {
    StringBuilder sb = new StringBuilder("The repository '");

    sb.append(revt.getRepository().getName());

    sb.append("' (ID='").append(revt.getRepository().getId()).append("') was put ");

    LocalStatus newStatus = revt.getNewLocalStatus();

    if (LocalStatus.IN_SERVICE.equals(newStatus)) {
      sb.append("IN SERVICE.");
    }
    else if (LocalStatus.OUT_OF_SERVICE.equals(newStatus)) {
      sb.append("OUT OF SERVICE.");
    }
    else {
      sb.append(revt.getRepository().getLocalStatus().toString()).append(".");
    }

    sb.append(" The previous state was ");

    if (LocalStatus.IN_SERVICE.equals(revt.getOldLocalStatus())) {
      sb.append("IN SERVICE.");
    }
    else if (LocalStatus.OUT_OF_SERVICE.equals(revt.getOldLocalStatus())) {
      sb.append("OUT OF SERVICE.");
    }
    else {
      sb.append(revt.getOldLocalStatus().toString()).append(".");
    }

    getFeedRecorder().addSystemEvent(FeedRecorder.SYSTEM_REPO_LSTATUS_CHANGES_ACTION, sb.toString());
  }

}
