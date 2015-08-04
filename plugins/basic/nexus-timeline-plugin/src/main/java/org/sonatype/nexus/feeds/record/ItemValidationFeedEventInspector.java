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
import org.sonatype.nexus.feeds.NexusArtifactEvent;
import org.sonatype.nexus.proxy.events.RepositoryItemValidationEventFailed;
import org.sonatype.nexus.proxy.events.RepositoryItemValidationEventFailedChecksum;
import org.sonatype.nexus.proxy.events.RepositoryItemValidationEventFailedFileType;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * Event inspector that creates feeds about failed item validations.
 *
 * @author cstamas
 */
@Named
@Singleton
public class ItemValidationFeedEventInspector
    extends AbstractFeedRecorderEventInspector
    implements EventSubscriber, Asynchronous
{
  @Subscribe
  @AllowConcurrentEvents
  public void inspect(final RepositoryItemValidationEventFailed ievt) {
    final NexusItemInfo ai = new NexusItemInfo();
    ai.setRepositoryId(ievt.getItem().getRepositoryId());
    ai.setPath(ievt.getItem().getPath());
    ai.setRemoteUrl(ievt.getItem().getRemoteUrl());

    String action = NexusArtifactEvent.ACTION_BROKEN;

    if (ievt instanceof RepositoryItemValidationEventFailedChecksum) {
      action = NexusArtifactEvent.ACTION_BROKEN_WRONG_REMOTE_CHECKSUM;
    }
    else if (ievt instanceof RepositoryItemValidationEventFailedFileType) {
      action = NexusArtifactEvent.ACTION_BROKEN_INVALID_CONTENT;
    }

    final NexusArtifactEvent nae = new NexusArtifactEvent(ievt.getEventDate(), action, ievt.getMessage(), ai);

    getFeedRecorder().addNexusArtifactEvent(nae);
  }
}
