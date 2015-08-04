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
package org.sonatype.nexus.proxy.maven.routing.events;

import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.maven.routing.PrefixSource;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Event fired when a {@link MavenRepository} publishes it's prefix file. The prefix source (backed by new file) is
 * carried along with this event in form of {@link PrefixSource} but you can also use {@link Manager} to get repository
 * entry source, as it is already available in the moment you process this event.
 *
 * @author cstamas
 * @since 2.4
 */
public class PrefixFilePublishedRepositoryEvent
    extends AbstractRoutingRepositoryEvent
{
  private final PrefixSource prefixSource;

  /**
   * Constructor.
   *
   * @param mavenRepository the repository published it's prefix file.
   * @param prefixSource    the prefix file in form of {@link PrefixSource}.
   */
  public PrefixFilePublishedRepositoryEvent(final MavenRepository mavenRepository, final PrefixSource prefixSource) {
    super(mavenRepository);
    this.prefixSource = checkNotNull(prefixSource);
  }

  /**
   * The {@link PrefixSource} that gives access to published entries.
   *
   * @return the entry source.
   */
  public PrefixSource getPrefixSource() {
    return prefixSource;
  }
}
