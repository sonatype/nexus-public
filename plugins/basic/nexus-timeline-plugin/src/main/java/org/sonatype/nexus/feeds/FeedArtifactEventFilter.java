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
package org.sonatype.nexus.feeds;

import java.util.List;

/**
 * Filter the Feeds results.  For example if the user didn't have access to an element in the feed it should be
 * filtered
 * so they do not see it.
 */
public interface FeedArtifactEventFilter
{
  /**
   * Filters the list <code>artifactEvents</code>.
   *
   * @param artifactEvents the events to be filtered.
   * @return A subset of the original <code>artifactEvents</code> list.
   */
  List<NexusArtifactEvent> filterArtifactEventList(List<NexusArtifactEvent> artifactEvents);
}
