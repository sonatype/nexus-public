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
package org.sonatype.nexus.repository.view.handlers;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.node.NodeAccess;

import org.fest.util.Strings;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import org.mockito.Mock;

public class HighAvailabilitySupportCheckerTest
    extends TestSupport
{
  private static final String FORMAT_NAME = "dummyFormat";

  private static final String HA_SUPPORTED_PROPERTY = String.format("nexus.%s.ha.supported", FORMAT_NAME);

  private static String haSupportedPropertyInitValue;

  private HighAvailabilitySupportChecker highAvailabilitySupportChecker;

  @Mock
  private NodeAccess nodeAccess;

  @BeforeClass
  public static void init() {
    haSupportedPropertyInitValue = System.getProperty(HA_SUPPORTED_PROPERTY);
  }

  @AfterClass
  public static void tearDown() {
    if (!Strings.isNullOrEmpty(haSupportedPropertyInitValue)) {
      System.setProperty(HA_SUPPORTED_PROPERTY, haSupportedPropertyInitValue);
    }
    else {
      System.clearProperty(HA_SUPPORTED_PROPERTY);
    }
  }

  @Test
  public void returnTrue_IfNexusHAIsFalseAndFormatHAIsFalse() {
    when(nodeAccess.isClustered()).thenReturn(false);
    highAvailabilitySupportChecker = new HighAvailabilitySupportChecker(nodeAccess);
    System.setProperty(HA_SUPPORTED_PROPERTY, "false");
    assertThat(highAvailabilitySupportChecker.isSupported(FORMAT_NAME), is(true));
  }

  @Test
  public void returnFalse_IfNexusHAIsTrueAndFormatHAIsFalse() {
    when(nodeAccess.isClustered()).thenReturn(true);
    highAvailabilitySupportChecker = new HighAvailabilitySupportChecker(nodeAccess);
    System.setProperty(HA_SUPPORTED_PROPERTY, "false");
    assertThat(highAvailabilitySupportChecker.isSupported(FORMAT_NAME), is(false));
  }

  @Test
  public void returnTrue_IfNexusHAIsTrueAndFormatHAIsTrue() {
    when(nodeAccess.isClustered()).thenReturn(true);
    highAvailabilitySupportChecker = new HighAvailabilitySupportChecker(nodeAccess);
    System.setProperty(HA_SUPPORTED_PROPERTY, "true");
    assertThat(highAvailabilitySupportChecker.isSupported(FORMAT_NAME), is(true));
  }

  @Test
  public void returnTrue_IfNexusHAIsFalseAndFormatHAIsTrue() {
    when(nodeAccess.isClustered()).thenReturn(false);
    highAvailabilitySupportChecker = new HighAvailabilitySupportChecker(nodeAccess);
    System.setProperty(HA_SUPPORTED_PROPERTY, "true");
    assertThat(highAvailabilitySupportChecker.isSupported(FORMAT_NAME), is(true));
  }
}
