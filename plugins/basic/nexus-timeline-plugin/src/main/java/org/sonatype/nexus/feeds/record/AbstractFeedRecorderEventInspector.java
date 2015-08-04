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

import javax.inject.Inject;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.feeds.FeedRecorder;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author cstamas
 */
public abstract class AbstractFeedRecorderEventInspector
    extends ComponentSupport
{
  private FeedRecorder feedRecorder;

  private ApplicationStatusSource applicationStatusSource;

  public AbstractFeedRecorderEventInspector() {
    // empty
  }

  // for now used just for UTs
  AbstractFeedRecorderEventInspector(final FeedRecorder feedRecorder,
                                     final ApplicationStatusSource applicationStatusSource)
  {
    this.feedRecorder = checkNotNull(feedRecorder);
    this.applicationStatusSource = checkNotNull(applicationStatusSource);
  }

  @Inject
  public void setFeedRecorder(final FeedRecorder feedRecorder) {
    this.feedRecorder = feedRecorder;
  }

  protected FeedRecorder getFeedRecorder() {
    return feedRecorder;
  }

  @Inject
  public void setApplicationStatusSource(final ApplicationStatusSource applicationStatusSource) {
    this.applicationStatusSource = applicationStatusSource;
  }

  protected ApplicationStatusSource getApplicationStatusSource() {
    return applicationStatusSource;
  }

  protected boolean isNexusStarted() {
    return getApplicationStatusSource().getSystemStatus().isNexusStarted();
  }
}
