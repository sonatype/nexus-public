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
package org.sonatype.nexus.testsuite;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.sonatype.nexus.client.core.subsystem.content.Content;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;

import static org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy.Strategy.EACH_TEST;

/**
 * Support for Core integration tests.
 *
 * @since 2.4
 */
@NexusStartAndStopStrategy(EACH_TEST)
public abstract class NexusCoreITSupport
    extends NexusRunningParametrizedITSupport
{

  protected NexusCoreITSupport(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  /**
   * Creates unique name with given prefix.
   *
   * @return a unique name.
   */
  public static String uniqueName(final String prefix) {
    return prefix + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
  }

  /**
   * Returns {@link Repositories} client subsystem.
   *
   * @return client for repositories.
   */
  public Repositories repositories() {
    return client().getSubsystem(Repositories.class);
  }

  /**
   * Returns {@link Content} client subsystem.
   *
   * @return client for content.
   */
  public Content content() {
    return client().getSubsystem(Content.class);
  }

}
