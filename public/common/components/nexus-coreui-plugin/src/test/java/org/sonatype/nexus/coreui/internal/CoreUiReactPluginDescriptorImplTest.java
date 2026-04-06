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

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.ui.UiUtil;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CoreUiReactPluginDescriptorImpl}.
 */
public class CoreUiReactPluginDescriptorImplTest
    extends TestSupport
{
  @Mock
  private UiUtil uiUtil;

  private CoreUiReactPluginDescriptorImpl underTest;

  @Before
  public void setUp() {
    when(uiUtil.getPathForFile("nexus-coreui-bundle.js")).thenReturn("/static/rapture/nexus-coreui-bundle.js");
    when(uiUtil.getPathForFile("nexus-coreui-bundle.debug.js"))
        .thenReturn("/static/rapture/nexus-coreui-bundle.debug.js");
    when(uiUtil.getPathForFile("nexus-coreui-bundle.css")).thenReturn("/static/rapture/nexus-coreui-bundle.css");

    underTest = new CoreUiReactPluginDescriptorImpl(uiUtil);
  }

  @Test
  public void testGetName() {
    assertThat(underTest.getName(), is("nexus-coreui-plugin"));
  }

  @Test
  public void testGetScripts_notDebug() {
    List<String> scripts = underTest.getScripts(false);
    assertThat(scripts, is(notNullValue()));
    assertThat(scripts, contains("/static/rapture/nexus-coreui-bundle.js"));
  }

  @Test
  public void testGetScripts_debug() {
    List<String> scripts = underTest.getScripts(true);
    assertThat(scripts, is(notNullValue()));
    assertThat(scripts, contains("/static/rapture/nexus-coreui-bundle.debug.js"));
  }

  @Test
  public void testGetStyles() {
    List<String> styles = underTest.getStyles();
    assertThat(styles, is(notNullValue()));
    assertThat(styles, contains("/static/rapture/nexus-coreui-bundle.css"));
  }

  @Test
  public void testGetScripts_debugFalseReturnsProdScripts() {
    List<String> debugScripts = underTest.getScripts(true);
    List<String> prodScripts = underTest.getScripts(false);

    assertThat(debugScripts.get(0).contains("debug"), is(true));
    assertThat(prodScripts.get(0).contains("debug"), is(false));
  }
}
