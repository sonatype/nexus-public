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
package org.sonatype.nexus.testsuite.ruby.trials;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy.Strategy;

import org.junit.Test;

/**
 * Support class for Rubygems "scenarios" executed as "modern ITs", a "trial". This way, the scenario utilizes
 * modern ITs to set up a running NX instance to test against.
 *
 * The IT should have only one test method defined in this class. Subclasses should only be "factories" for scenarios.
 * TODO: add ability to run multiple instances of scenario against same NX instance.
 * TODO: for now this is just POC to run a scenario utilizing all of the modern ITs infrastructure. Later
 * we might introduce separate module for trials (rubygems, npm, yum, etc) that would do something similar
 */
@NexusStartAndStopStrategy(Strategy.EACH_TEST)
public abstract class TrialSupport
    extends NexusRunningParametrizedITSupport
{

  protected TrialSupport(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(final NexusBundleConfiguration configuration) {
    return configuration
        .setLogLevel("org.sonatype.nexus.ruby", "TRACE")
        .setLogLevel("org.sonatype.nexus.plugins.ruby", "TRACE")
        .addPlugins(
            artifactResolver().resolvePluginFromDependencyManagement(
                "org.sonatype.nexus.plugins", "nexus-ruby-plugin"
            )
        );
  }

  @Test
  public void perform() throws Exception {
    final ScenarioSupport scenario = createScenario();
    scenario.configure();
    scenario.perform();
  }

  protected abstract ScenarioSupport createScenario();
}
