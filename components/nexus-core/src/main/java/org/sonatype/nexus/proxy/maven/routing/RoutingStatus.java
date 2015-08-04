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
package org.sonatype.nexus.proxy.maven.routing;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Routing Status.
 *
 * @author cstamas
 * @since 2.4
 */
public class RoutingStatus
{
  private final PublishingStatus publishingStatus;

  private final DiscoveryStatus discoveryStatus;

  /**
   * Constructor.
   */
  public RoutingStatus(final PublishingStatus publishingStatus, final DiscoveryStatus discoveryStatus) {
    this.publishingStatus = checkNotNull(publishingStatus);
    this.discoveryStatus = checkNotNull(discoveryStatus);
  }

  /**
   * Returns the publishing status.
   *
   * @return the publishing status.
   */
  public PublishingStatus getPublishingStatus() {
    return publishingStatus;
  }

  /**
   * Returns the discovery status.
   *
   * @return the discovery status.
   */
  public DiscoveryStatus getDiscoveryStatus() {
    return discoveryStatus;
  }
}
