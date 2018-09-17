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
package org.sonatype.nexus.repository.httpclient;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.repository.http.HttpStatus.PROXY_AUTHENTICATION_REQUIRED;

public class DefaultAutoBlockConfigurationTest
    extends TestSupport
{
  private DefaultAutoBlockConfiguration underTest;

  @Before
  public void setup() throws Exception {
    underTest = new DefaultAutoBlockConfiguration();
  }

  @Test
  public void blockUnauthorized() {
    assertTrue(underTest.shouldBlock(SC_UNAUTHORIZED));
  }

  @Test
  public void blockProxyAuthenticationRequired() {
    assertTrue(underTest.shouldBlock(PROXY_AUTHENTICATION_REQUIRED));
  }

  @Test
  public void blockAll500Statuses() {
    for (int status = 500; status <= 599; status++) {
      assertTrue(underTest.shouldBlock(status));
    }
  }
}
