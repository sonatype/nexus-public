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
package org.sonatype.nexus.httpclient;
import org.sonatype.nexus.httpclient.HttpClientPlan;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link HttpClientPlan}.
 */
public class HttpClientPlanTest
{

  private HttpClientPlan underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new HttpClientPlan();
  }

  @Test
  public void testGetUserAgentNull() {
    testGetUserAgent(null, null);
  }

  @Test
  public void testGetUserAgentNoSuffix() {
    testGetUserAgent("Agent of Nexus", null);
  }

  @Test
  public void testGetUserAgentWithSuffix() {
    testGetUserAgent("Agent of Nexus", "Some suffix");
  }

  private void testGetUserAgent(String base, String suffix) {
    underTest.setUserAgentBase(base);
    String expected = base;
    if (suffix != null) {
      underTest.setUserAgentSuffix(suffix);
      expected = base + " " + suffix;
    }

    // Execute
    String returned = underTest.getUserAgent();

    // Verify
    assertEquals(expected, returned);
  }

}
