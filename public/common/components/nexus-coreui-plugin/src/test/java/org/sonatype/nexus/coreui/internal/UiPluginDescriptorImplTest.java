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
package org.sonatype.nexus.coreui.internal;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for {@link UiPluginDescriptorImpl}.
 */
public class UiPluginDescriptorImplTest
    extends TestSupport
{
  private UiPluginDescriptorImpl underTest;

  @Before
  public void setUp() {
    underTest = new UiPluginDescriptorImpl();
  }

  @Test
  public void testGetPluginId() {
    assertThat(underTest.getPluginId(), is("nexus-coreui-plugin"));
  }

  @Test
  public void testGetNamespace() {
    assertThat(underTest.getNamespace(), is("NX.coreui"));
  }

  @Test
  public void testGetConfigClassName() {
    assertThat(underTest.getConfigClassName(), is("NX.coreui.app.PluginConfig"));
  }

  @Test
  public void testHasStyle() {
    assertThat(underTest.hasStyle(), is(true));
  }

  @Test
  public void testHasScript() {
    assertThat(underTest.hasScript(), is(true));
  }

  @Test
  public void testGetScripts_returnsEmptyList() {
    assertThat(underTest.getScripts(false), is(notNullValue()));
    assertThat(underTest.getScripts(false).isEmpty(), is(true));
    assertThat(underTest.getScripts(true), is(notNullValue()));
    assertThat(underTest.getScripts(true).isEmpty(), is(true));
  }
}
