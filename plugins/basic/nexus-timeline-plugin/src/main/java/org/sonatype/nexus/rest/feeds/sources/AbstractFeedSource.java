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

import javax.inject.Inject;

import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.feeds.FeedRecorder;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.sisu.goodies.common.ComponentSupport;

/**
 * And abstract class for NexusArtifactEvent based feeds. This class implements all needed to create a feed,
 * implementors needs only to implement 3 abtract classes.
 *
 * @author cstamas
 */
public abstract class AbstractFeedSource
    extends ComponentSupport
    implements FeedSource
{
  private ApplicationStatusSource applicationStatusSource;

  private RepositoryRegistry repositoryRegistry;

  private FeedRecorder feedRecorder;

  @Inject
  public void setFeedRecorder(final FeedRecorder feedRecorder) {
    this.feedRecorder = feedRecorder;
  }

  protected FeedRecorder getFeedRecorder() {
    return feedRecorder;
  }

  @Inject
  public void setRepositoryRegistry(final RepositoryRegistry repositoryRegistry) {
    this.repositoryRegistry = repositoryRegistry;
  }

  protected RepositoryRegistry getRepositoryRegistry() {
    return repositoryRegistry;
  }

  @Inject
  public void setApplicationStatusSource(final ApplicationStatusSource applicationStatusSource) {
    this.applicationStatusSource = applicationStatusSource;
  }

  protected ApplicationStatusSource getApplicationStatusSource() {
    return applicationStatusSource;
  }

  public abstract String getTitle();

  public abstract String getDescription();
}
