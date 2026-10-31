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
package org.sonatype.nexus.bootstrap.siesta;

import org.sonatype.nexus.security.FilterChain;
import org.sonatype.nexus.security.anonymous.AnonymousFilter;
import org.sonatype.nexus.security.authc.AntiCsrfFilter;
import org.sonatype.nexus.security.authc.NexusAuthenticationFilter;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

class SiestaConfigurationTest
{
  private final SiestaConfiguration underTest = new SiestaConfiguration();

  @Test
  void testStatusWritableFilterChain_hasCorrectPath() {
    FilterChain chain = underTest.statusWritableFilterChain();

    assertThat(chain.getPathPattern(), is("/service/rest/v1/status/writable"));
  }

  @Test
  void testStatusWritableFilterChain_excludesNxAuthc() {
    FilterChain chain = underTest.statusWritableFilterChain();

    // nx-authc triggers bcrypt + DB lookup on every probe — must not be in this chain
    assertThat(chain.getFilterExpression(), not(containsString(NexusAuthenticationFilter.NAME)));
  }

  @Test
  void testStatusWritableFilterChain_includesAnonymousAndAntiCsrf() {
    FilterChain chain = underTest.statusWritableFilterChain();

    assertThat(chain.getFilterExpression(), containsString(AnonymousFilter.NAME));
    assertThat(chain.getFilterExpression(), containsString(AntiCsrfFilter.NAME));
  }
}
