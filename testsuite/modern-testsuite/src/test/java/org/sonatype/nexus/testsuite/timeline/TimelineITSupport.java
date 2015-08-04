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
package org.sonatype.nexus.testsuite.timeline;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.testsuite.client.Scheduler;
import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;

/**
 * Support class for nexus-timeline-plugin related ITs.
 *
 * @since 2.6.1
 */
@NexusStartAndStopStrategy(NexusStartAndStopStrategy.Strategy.EACH_TEST)
public class TimelineITSupport
    extends NexusRunningParametrizedITSupport
{

  public TimelineITSupport(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(final NexusBundleConfiguration configuration) {
    return super.configureNexus(configuration).addPlugins(
        artifactResolver().resolvePluginFromDependencyManagement("org.sonatype.nexus.plugins",
            "nexus-timeline-plugin"));
  }

  protected Scheduler scheduler() {
    return client().getSubsystem(Scheduler.class);
  }
}
