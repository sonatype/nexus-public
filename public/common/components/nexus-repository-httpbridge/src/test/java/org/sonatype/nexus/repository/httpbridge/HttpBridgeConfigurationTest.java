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
package org.sonatype.nexus.repository.httpbridge;

import org.sonatype.nexus.security.FilterChain;
import org.sonatype.nexus.security.authc.NexusAuthenticationFilter;
import org.sonatype.nexus.security.authc.apikey.ApiKeyAuthenticationFilter;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class HttpBridgeConfigurationTest
{
  @Test
  public void sessionFilterChain_hasNxAuthcBeforeNxApikeyAuthc() {
    FilterChain chain = new HttpBridgeConfiguration().httpBridgeFilterChain();
    assertFilterOrder(chain);
  }

  @Test
  public void jwtFilterChain_hasNxAuthcBeforeNxApikeyAuthc() {
    FilterChain chain = new HttpBridgeConfiguration().httpBridgeFilterChain_jwt();
    assertFilterOrder(chain);
  }

  private static void assertFilterOrder(final FilterChain chain) {
    String expression = chain.getFilterExpression();
    int authcPos = expression.indexOf(NexusAuthenticationFilter.NAME);
    int apikeyPos = expression.indexOf(ApiKeyAuthenticationFilter.NAME);

    assertThat("nx-authc must appear in the filter expression", authcPos, is(not(-1)));
    assertThat("nx-apikey-authc must appear in the filter expression", apikeyPos, is(not(-1)));
    assertThat("nx-authc must precede nx-apikey-authc — reordering breaks service account auth",
        authcPos, is(not(greaterThan(apikeyPos))));
  }
}
