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
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.apt.internal.proxy.AptProxyRecipe;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @since 3.next
 */
public class AptRecipeTest
    extends TestSupport
{
  @Mock
  private Request request;

  @Mock
  private Context context;

  private AttributesMap attributesMap;

  private AptRecipeSupport underTest;

  @Before
  public void setUp() {
    underTest = new AptProxyRecipe(new ProxyType(), new AptFormat());

    attributesMap = new AttributesMap();
    when(context.getRequest()).thenReturn(request);
    when(context.getAttributes()).thenReturn(attributesMap);
  }

  @After
  public void tearDown() {
    System.getProperties().remove("nexus.apt.enabled");
  }

  @Test
  public void disabledByDefault() {
    assertThat(underTest.isFeatureEnabled(), is(equalTo(false)));
  }

  @Test
  public void enableApt() {
    System.setProperty("nexus.apt.enabled", "true");
    assertThat(underTest.isFeatureEnabled(), is(equalTo(true)));
  }
}
