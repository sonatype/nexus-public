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
package org.sonatype.nexus.rapture.internal;

import java.net.URI;
import java.util.List;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.rapture.internal.state.StateComponent;
import org.sonatype.nexus.ui.UiPluginDescriptorSupport;

import com.google.inject.util.Providers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class RaptureWebResourceBundleTest
    extends TestSupport
{
  @Mock
  private ApplicationVersion applicationVersion;

  @Mock
  private HttpServletRequest httpServletRequest;

  @Mock
  private StateComponent stateComponent;

  @Mock
  private TemplateHelper templateHelper;


  private RaptureWebResourceBundle underTest;

  @Before
  public void setup() {
    BaseUrlHolder.set("http://baseurl/", ".");

    when(httpServletRequest.getParameter("debug")).thenReturn("false");

    underTest =
        new RaptureWebResourceBundle(applicationVersion, Providers.of(httpServletRequest), Providers.of(stateComponent),
            templateHelper, asList(new UiPluginDescriptorImpl()),
            asList(new ExtJsUiPluginDescriptorImpl("test-1"), new ExtJsUiPluginDescriptorImpl("test-2")), null);
  }

  @Test
  public void testGetExtJsPluginConfigs() {
    List<String> configs = underTest.getExtJsPluginConfigs();

    assertThat(configs, is(asList("id-test-1", "id-test-2")));
  }

  @Test
  public void testGetExtJsNamespaces() {
    List<String> namespaces = underTest.getExtJsNamespaces();

    assertThat(namespaces, is(asList("namespace-test-1", "namespace-test-2")));
  }

  @Test
  public void testGetStyles() throws Exception {
    List<URI> uris = underTest.getStyles();

    assertThat(uris, is(asList(new URI("./static/rapture/resources/loading-prod.css"),
        new URI("./static/rapture/resources/baseapp.css"),
        new URI("./static/rapture/resources/test-1-prod.css"),
        new URI("./static/rapture/resources/test-2-prod.css"),
        new URI("./react-style-1-test.css"),
        new URI("./react-style-2-test.css"))));
  }

  @Test
  public void testGetScripts_prod() throws Exception {
    List<URI> uris = underTest.getScripts();

    assertThat(uris, is(asList(new URI("./static/rapture/baseapp-prod.js"),
        new URI("./static/rapture/extdirect-prod.js"),
        new URI("./static/rapture/bootstrap.js"),
        new URI("./extjs-script-1-test-1.js"),
        new URI("./extjs-script-2-test-1.js"),
        new URI("./extjs-script-1-test-2.js"),
        new URI("./extjs-script-2-test-2.js"),
        new URI("./react-script-1-test-prod.js"),
        new URI("./react-script-2-test-prod.js"),
        new URI("./static/rapture/test-1-prod.js"),
        new URI("./static/rapture/test-2-prod.js"),
        new URI("./static/rapture/app.js"))));
  }

  @Test
  public void testGetScripts_debug() throws Exception {
    when(httpServletRequest.getParameter("debug")).thenReturn("true");

    List<URI> uris = underTest.getScripts();

    assertThat(uris, is(asList(new URI("./static/rapture/baseapp-debug.js"),
        new URI("./static/rapture/extdirect-debug.js"),
        new URI("./static/rapture/bootstrap.js"),
        new URI("./extjs-script-1-test-1.js"),
        new URI("./extjs-script-2-test-1.js"),
        new URI("./extjs-script-1-test-2.js"),
        new URI("./extjs-script-2-test-2.js"),
        new URI("./react-script-1-test-debug.js"),
        new URI("./react-script-2-test-debug.js"),
        new URI("./static/rapture/app.js"))));
  }

  private final class UiPluginDescriptorImpl
      extends UiPluginDescriptorSupport
  {
    public UiPluginDescriptorImpl() {
      super("test");
    }

    @Override
    public List<String> getStyles() {
      return asList("/react-style-1-" + getName() + ".css", "/react-style-2-" + getName() + ".css");
    }

    @Override
    public List<String> getScripts(final boolean isDebug) {
      String suffix = getName() + (isDebug ? "-debug" : "-prod") + ".js";
      return asList("/react-script-1-" + suffix, "/react-script-2-" + suffix);
    }
  }

  private final class ExtJsUiPluginDescriptorImpl
      extends org.sonatype.nexus.rapture.UiPluginDescriptorSupport
  {
    public ExtJsUiPluginDescriptorImpl(final String artifactId) {
      super(artifactId);
    }

    @Nullable
    @Override
    public String getConfigClassName() {
      return "id-" + getPluginId();
    }

    @Nullable
    @Override
    public String getNamespace() {
      return "namespace-" + getPluginId();
    }

    @Override
    public boolean hasStyle() {
      return true;
    }

    @Override
    public boolean hasScript() {
      return true;
    }

    @Override
    public List<String> getScripts(final boolean isDebug) {
      return asList("/extjs-script-1-" + getPluginId() + ".js", "/extjs-script-2-" + getPluginId() + ".js");
    }
  }
}
