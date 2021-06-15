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
package org.sonatype.nexus.repository.p2.orient.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.p2.internal.P2Format;
import org.sonatype.nexus.repository.p2.orient.internal.proxy.OrientP2ProxyRecipe;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.handlers.HighAvailabilitySupportChecker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.p2.internal.P2Format.NAME;

public class P2RecipeTest
    extends TestSupport
{
  @Mock
  private P2Format p2Format;

  @Mock
  private HighAvailabilitySupportChecker highAvailabilitySupportChecker;

  private OrientP2ProxyRecipe p2ProxyRecipe;

  @Before
  public void setUp() {
    when(p2Format.getValue()).thenReturn(NAME);
    p2ProxyRecipe = new OrientP2ProxyRecipe(new ProxyType(), p2Format);
    p2ProxyRecipe.setHighAvailabilitySupportChecker(highAvailabilitySupportChecker);
  }

  @Test
  public void haEnabledProxyRepository() {
    when(highAvailabilitySupportChecker.isSupported(NAME)).thenReturn(true);
    assertThat(p2ProxyRecipe.isFeatureEnabled(), is(equalTo(true)));
    verify(highAvailabilitySupportChecker).isSupported(NAME);
  }

  @Test
  public void haDisabledProxyRepository() {
    when(highAvailabilitySupportChecker.isSupported(NAME)).thenReturn(false);
    assertThat(p2ProxyRecipe.isFeatureEnabled(), is(equalTo(false)));
    verify(highAvailabilitySupportChecker).isSupported(NAME);
  }
}
