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
package org.sonatype.nexus.testsuite.support;

import java.util.Arrays;
import java.util.Collection;

import org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.sonatype.nexus.testsuite.support.ParametersLoaders.firstAvailableTestParameters;
import static org.sonatype.nexus.testsuite.support.ParametersLoaders.systemTestParameters;
import static org.sonatype.nexus.testsuite.support.ParametersLoaders.testParameters;
import static org.sonatype.nexus.testsuite.support.hamcrest.NexusMatchers.doesNotHaveCommonExceptions;
import static org.sonatype.nexus.testsuite.support.hamcrest.NexusMatchers.doesNotHaveFailingPlugins;
import static org.sonatype.nexus.testsuite.support.hamcrest.NexusMatchers.logFile;
import static org.sonatype.sisu.goodies.common.Varargs.$;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.URLMatchers.respondsWithStatus;

/**
 * Test starting and stopping of Nexus using parameters.
 *
 * @since 2.2
 */
public class StartAndStopNexusRunningParametrizedIT
    extends NexusRunningParametrizedITSupport
{

  // Uncomment the following line to suppress default parameters lookup and use test specific ones
  // @Parameterized.Parameters
  public static Collection<Object[]> testSpecificParameters() {
    return firstAvailableTestParameters(
        systemTestParameters(),
        testParameters(StartAndStopNexusRunningParametrizedIT.class)
    ).load();
  }

  // Uncomment the following line to suppress default parameters lookup and use the ones specified by the method
  // @Parameterized.Parameters
  public static Collection<Object[]> hardcodedParameters() {
    return Arrays.asList(new Object[][]{
        {"org.sonatype.nexus.assemblies:nexus-bundle-template:zip:bundle:${project.version}"},
    });
  }

  // Uncomment the following line to suppress default parameters lookup and use test specific ones
  // @Parameterized.Parameters
  public static Collection<Object[]> hardcodedAndSystemProperties() {
    return firstAvailableTestParameters(
        systemTestParameters(),
        testParameters(
            $("org.sonatype.nexus.assemblies:nexus-bundle-template:zip:bundle:${project.version}")
        )
    ).load();
  }

  public StartAndStopNexusRunningParametrizedIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  /**
   * Given a running/started nexus it checks that:<br/>
   * - Nexus instance is set<br/>
   * - Nexus state is set<br/>
   * - Nexus state confirms that is running<br/>
   * - Nexus responds with 200 at provided URL
   *
   * @throws Exception re-thrown
   */
  @Test
  public void startAndStop()
      throws Exception
  {
    assertThat(nexus(), is(notNullValue()));
    assertThat(nexus().isRunning(), is(true));

    assertThat(nexus().getUrl(), respondsWithStatus(200));

    assertThat(nexus().getLauncherLog(), FileMatchers.exists());
    assertThat(nexus().getLauncherLog(), FileMatchers.isFile());

    assertThat(nexus().getNexusLog(), FileMatchers.exists());
    assertThat(nexus().getNexusLog(), FileMatchers.isFile());

    assertThat(nexus().getNexusLog(), doesNotHaveCommonExceptions());
    assertThat(nexus().getNexusLog(), doesNotHaveFailingPlugins());

    assertThat(nexus(), logFile(doesNotHaveCommonExceptions()));
    assertThat(nexus(), logFile(doesNotHaveFailingPlugins()));
  }

}
