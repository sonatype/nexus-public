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
package org.sonatype.nexus.repository.apt.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.apt.internal.hosted.AptHostedRecipe;
import org.sonatype.nexus.repository.apt.internal.proxy.AptProxyRecipe;
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

/**
 * @since 3.17
 */
public class AptRecipeTest
    extends TestSupport
{
  @Mock
  AptFormat format;

  @Mock
  HighAvailabilitySupportChecker highAvailabilitySupportChecker;

  AptHostedRecipe hostedRecipe;

  AptProxyRecipe proxyRecipe;

  final String APT_NAME = "apt";

  @Before
  public void setUp() {
    hostedRecipe = new AptHostedRecipe(highAvailabilitySupportChecker, new ProxyType(), format);
    proxyRecipe = new AptProxyRecipe(highAvailabilitySupportChecker, new ProxyType(), format);
    when(format.getValue()).thenReturn(APT_NAME);
  }

  @Test
  public void enabledByDefault_AptHostedRepository() {
    when(highAvailabilitySupportChecker.isSupported(APT_NAME)).thenReturn(true);
    assertThat(hostedRecipe.isFeatureEnabled(), is(equalTo(true)));
    verify(highAvailabilitySupportChecker).isSupported(APT_NAME);
  }

  @Test
  public void disabledIfNexusIsClusteredAndAptNotCluster_AptHostedRepository() {
    when(highAvailabilitySupportChecker.isSupported(APT_NAME)).thenReturn(false);
    assertThat(hostedRecipe.isFeatureEnabled(), is(equalTo(false)));
    verify(highAvailabilitySupportChecker).isSupported(APT_NAME);
  }

  @Test
  public void enabledByDefault_AptProxyRepository() {
    when(highAvailabilitySupportChecker.isSupported(APT_NAME)).thenReturn(true);
    assertThat(proxyRecipe.isFeatureEnabled(), is(equalTo(true)));
    verify(highAvailabilitySupportChecker).isSupported(APT_NAME);
  }

  @Test
  public void disabledIfNexusIsClusteredAndAptNotCluster_AptProxyRepository() {
    when(highAvailabilitySupportChecker.isSupported(APT_NAME)).thenReturn(false);
    assertThat(proxyRecipe.isFeatureEnabled(), is(equalTo(false)));
    verify(highAvailabilitySupportChecker).isSupported(APT_NAME);
  }

}
