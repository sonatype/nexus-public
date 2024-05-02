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
package org.sonatype.nexus.bootstrap.osgi;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;

public class NexusEditionPropertiesConfigurerTest
{

  private final NexusEditionPropertiesConfigurer underTest = new NexusEditionPropertiesConfigurer();

  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Before
  public void setUp() {
    System.clearProperty("nexus.clustered");
  }

  @Test
  public void testThrowsExceptionIfHACIsEnabledInSystemProperties() {
    System.setProperty("nexus.clustered", "true");
    IllegalStateException expected =
        assertThrows(IllegalStateException.class, underTest::ensureHACIsDisabled);
    assertThat(expected.getMessage(),
        containsString("High Availability Clustering (HA-C) is a legacy feature and is no longer supported"));
  }

  @Test(expected = Test.None.class)
  public void testDoNotThrowExceptionIfHACIsDisabledInSystemProperties() {
    System.setProperty("nexus.clustered", "false");
    underTest.ensureHACIsDisabled();
  }

  @Test
  public void testThrowsExceptionIfHACIsEnabledInEnvVariables() {
    environmentVariables.set("NEXUS_CLUSTERED", "true");
    IllegalStateException expected =
        assertThrows(IllegalStateException.class, underTest::ensureHACIsDisabled);
    assertThat(expected.getMessage(),
        containsString("High Availability Clustering (HA-C) is a legacy feature and is no longer supported"));
  }

  @Test(expected = Test.None.class)
  public void testDoNotThrowExceptionIfHACIsDisabledInEnvVariables() {
    environmentVariables.set("NEXUS_CLUSTERED", "false");
    underTest.ensureHACIsDisabled();
  }

  @Test(expected = Test.None.class)
  public void testOrientDbWithAllowedVersion() {
    System.setProperty("java.version", "11.0.0");
    underTest.ensureOrientRunningWithCorrectJavaRuntime();
  }

  @Test
  public void testOrientDbWithNotAllowedVersion() {
    System.setProperty("java.version", "17.0.0");
    IllegalStateException expected = assertThrows(IllegalStateException.class,
        underTest::ensureOrientRunningWithCorrectJavaRuntime
    );
    assertThat(expected.getMessage(), containsString("The maximum Java version for OrientDb is Java 11"));
  }

}