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
package org.sonatype.nexus.internal.security.rest;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EulaStatusTest
    extends TestSupport
{
  private final EulaStatus underTest = new EulaStatus();

  @Test
  public void testValidDisclaimer() {
    underTest.setDisclaimer(
        "Use of Sonatype Nexus Repository - Community Edition is governed by the End User License Agreement at https://links.sonatype.com/products/nxrm/ce-eula. By returning the value from ‘accepted:false’ to ‘accepted:true’, you acknowledge that you have read and agree to the End User License Agreement at https://links.sonatype.com/products/nxrm/ce-eula.");
    assertTrue(underTest.hasExpectedDisclaimer());
  }

  @Test
  public void testInvalidDisclaimer() {
    underTest.setDisclaimer("invalid");
    assertFalse(underTest.hasExpectedDisclaimer());
  }

  @Test
  public void testNullDisclaimer() {
    underTest.setDisclaimer(null);
    assertFalse(underTest.hasExpectedDisclaimer());
  }

  @Test
  public void testEmptyDisclaimer() {
    underTest.setDisclaimer("");
    assertFalse(underTest.hasExpectedDisclaimer());
  }
}
