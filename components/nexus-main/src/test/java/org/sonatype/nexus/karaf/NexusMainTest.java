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
package org.sonatype.nexus.karaf;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.sonatype.nexus.karaf.NexusMain.requireMinimumJavaVersion;

public class NexusMainTest
    extends TestSupport
{
  static final String LOWER = "1.7";

  static final String INVALID = "X.X-internal";

  @Rule
  public final ExpectedSystemExit exit = ExpectedSystemExit.none();

  @Rule
  public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

  @Rule
  public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

  @SuppressWarnings("java:S2699") // sonar wants assertions, but none seem worthwhile here
  @Test
  public void doNotExitWhenGreaterVersion() throws Exception {
    requireMinimumJavaVersion();
  }

  @SuppressWarnings("java:S2699") // sonar wants assertions, but none seem worthwhile here
  @Test
  public void doNotExitWhenExactVersion() throws Exception {
    setVersion(NexusMain.MINIMUM_JAVA_VERSION.toString());
    requireMinimumJavaVersion();
  }

  @Test
  public void exitOnLowerJavaVersion() throws Exception {
    System.setProperty("java.version", "1.7");

    exit.expectSystemExitWithStatus(-1);
    requireMinimumJavaVersion();
  }

  @SuppressWarnings("java:S2699") // sonar wants assertions, but none seem worthwhile here
  @Test
  public void doNotExitWhenWrongVersionButCheckDisabled() throws Exception {
    runDisabledVmCheckWithVersion(LOWER);
  }

  @SuppressWarnings("java:S2699") // sonar wants assertions, but none seem worthwhile here
  @Test
  public void doNotExitWhenVmCheckDisabledAndInvalidVersion() throws Exception {
    runDisabledVmCheckWithVersion(INVALID);
  }

  @Test
  public void logMinimumVersionErrorWhenVmCheckDisabledAndLowerVersion() throws Exception {
    runDisabledVmCheckWithVersion(LOWER);
    assertThat(systemErrRule.getLog(), containsString("Nexus requires minimum java.version: "));
  }

  @Test
  public void logMinimumVersionErrorWhenVmCheckDisabledAndInvalidVersion() throws Exception {
    runDisabledVmCheckWithVersion(INVALID);
    assertThat(systemErrRule.getLog(), containsString("Nexus requires minimum java.version: "));
  }

  @Test
  public void logInvalidVersionErrorWhenVmCheckDisabledAndInvalidVersion() throws Exception {
    runDisabledVmCheckWithVersion(INVALID);
    assertThat(systemErrRule.getLog(), containsString("invalid version \"X.X-internal\": non-numeric \"X\""));
  }

  @Test
  public void logExpectedExitWithNoOverriddenExitCode() throws Exception {
    try (MockedConstruction<NexusMain> ignored =
        Mockito.mockConstruction(NexusMain.class)) {
      exit.expectSystemExitWithStatus(0);
      NexusMain.main(new String[0]);
    }
  }

  @Test
  public void logExpectedExitWithOverriddenExitCode() throws Exception {
    try (MockedConstruction<NexusMain> ignored =
        Mockito.mockConstruction(NexusMain.class)) {
      System.setProperty("nexus.overrideExitCode", "-42");
      exit.expectSystemExitWithStatus(-42);
      NexusMain.main(new String[0]);
      assertThat(systemErrRule.getLog(), containsString("Exited with code: -42"));
      assertThat(systemErrRule.getLog(), containsString("Please check the previous log messages"));
    }
  }

  private void runDisabledVmCheckWithVersion(final String version) {
    disableVmCheck();
    setVersion(version);
    requireMinimumJavaVersion();
  }

  private void disableVmCheck() {
    System.setProperty("nexus.vmCheck", "false");
  }

  private String setVersion(final String version) {
    return System.setProperty("java.version", version);
  }
}
