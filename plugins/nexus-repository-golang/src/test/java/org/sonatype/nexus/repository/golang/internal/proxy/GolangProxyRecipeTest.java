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
package org.sonatype.nexus.repository.golang.internal.proxy;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.golang.GolangFormat;
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

public class GolangProxyRecipeTest extends TestSupport
{
  @Mock
  private HighAvailabilitySupportChecker highAvailabilitySupportChecker;

  private String GO_NAME = "go";

  private GolangProxyRecipe proxyUnderTest;

  @Before
  public void setUp() throws Exception {
    proxyUnderTest = new GolangProxyRecipe(highAvailabilitySupportChecker, new ProxyType(), new GolangFormat());
  }

  @Test
  public void enabledByDefault_GoProxyRepository() {
    when(highAvailabilitySupportChecker.isSupported(GO_NAME)).thenReturn(true);
    assertThat(proxyUnderTest.isFeatureEnabled(), is(equalTo(true)));
    verify(highAvailabilitySupportChecker).isSupported(GO_NAME);
  }

  @Test
  public void disabledIfNexusIsClusteredAndGoNotCluster_GoProxyRepository() {
    when(highAvailabilitySupportChecker.isSupported(GO_NAME)).thenReturn(false);
    assertThat(proxyUnderTest.isFeatureEnabled(), is(equalTo(false)));
    verify(highAvailabilitySupportChecker).isSupported(GO_NAME);
  }
}
