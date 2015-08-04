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
package org.sonatype.nexus.proxy.maven.routing.discovery;

import java.io.IOException;

import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;

/**
 * Strategy for remote content discovery for prefix list by some means. It is identified by {@link #getId()} and has
 * priority {@link #getPriority()}. Latter is used to sort (using natural order of integers) the instances and try the
 * one by one in sorted order.
 *
 * @author cstamas
 * @since 2.4
 */
public interface RemoteStrategy
    extends Prioritized
{
  /**
   * Returns the unique ID of the strategy, never {@code null}.
   *
   * @return the ID of the strategy.
   */
  String getId();

  /**
   * Discovers the content of the given {@link MavenRepository}.
   *
   * @param mavenRepository to have local content discovered.
   * @return the result with discovered entries.
   * @throws StrategyFailedException if "soft" failure detected.
   * @throws IOException             in case of IO problem.
   */
  StrategyResult discover(MavenProxyRepository mavenRepository)
      throws StrategyFailedException, IOException;
}
