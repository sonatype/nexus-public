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
package org.sonatype.repository.conan.internal.proxy.matcher;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.repository.conan.internal.common.v1.ConanRoutes;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@Ignore
public class ConanRoutesTest
    extends TestSupport
{
  @Mock
  Request request;

  @Mock
  private Context context;

  @Mock
  Handler handler;

  private AttributesMap attributesMap;

  private ConanRoutes underTest;

  @Before
  public void setUp() {
    attributesMap = new AttributesMap();
    when(context.getRequest()).thenReturn(request);
    when(context.getAttributes()).thenReturn(attributesMap);
    when(request.getAction()).thenReturn(HttpMethods.GET);

    underTest = new ConanRoutes();
  }

  @Test
  public void canMatchOnDownloadUrls() {
    when(request.getPath()).thenReturn("/v1/conans/jsonformoderncpp/2.1.1/vthiery/stable/download_urls");
    assertTrue(underTest.downloadUrls().handler(handler).create().getMatcher().matches(context));
    TokenMatcher.State matcherState = attributesMap.require(TokenMatcher.State.class);
    assertThat(matcherState.getTokens().get("group"), is(equalTo("vthiery")));
    assertThat(matcherState.getTokens().get("project"), is(equalTo("jsonformoderncpp")));
    assertThat(matcherState.getTokens().get("version"), is(equalTo("2.1.1")));
  }

  @Test
  public void canMatchOnPackagesDownloadUrl() {
    when(request.getPath()).thenReturn("/v1/conans/jsonformoderncpp/2.1.1/vthiery/stable/packages/5ab84d6acfe1f23c4fae0ab88f26e3a396351ac9/download_urls");
    assertTrue(underTest.downloadUrls().handler(handler).create().getMatcher().matches(context));
    TokenMatcher.State matcherState = attributesMap.require(TokenMatcher.State.class);
    assertThat(matcherState.getTokens().get("group"), is(equalTo("vthiery")));
    assertThat(matcherState.getTokens().get("project"), is(equalTo("jsonformoderncpp")));
    assertThat(matcherState.getTokens().get("version"), is(equalTo("2.1.1")));
  }

  @Test
  public void canMatchOnConanfile() {
    when(request.getPath()).thenReturn("/vthiery/jsonformoderncpp/2.1.1/stable/conanfile.py");
    assertTrue(underTest.conanFile().handler(handler).create().getMatcher().matches(context));
    TokenMatcher.State matcherState = attributesMap.require(TokenMatcher.State.class);
    assertThat(matcherState.getTokens().get("group"), is(equalTo("vthiery")));
    assertThat(matcherState.getTokens().get("project"), is(equalTo("jsonformoderncpp")));
    assertThat(matcherState.getTokens().get("version"), is(equalTo("2.1.1")));
  }

  @Test
  public void canMatchOnConanManifest() {
    when(request.getPath()).thenReturn("/vthiery/jsonformoderncpp/2.1.1/stable/conanmanifest.txt");
    assertTrue(underTest.conanManifest().handler(handler).create().getMatcher().matches(context));
    TokenMatcher.State matcherState = attributesMap.require(TokenMatcher.State.class);
    assertThat(matcherState.getTokens().get("group"), is(equalTo("vthiery")));
    assertThat(matcherState.getTokens().get("project"), is(equalTo("jsonformoderncpp")));
    assertThat(matcherState.getTokens().get("version"), is(equalTo("2.1.1")));
  }

  @Test
  public void canMatchOnPackagesConanManifest() {
    when(request.getPath()).thenReturn("/vthiery/jsonformoderncpp/2.1.1/stable/packages/5ab84d6acfe1f23c4fae0ab88f26e3a396351ac9/conanmanifest.txt");
    assertTrue(underTest.conanManifest().handler(handler).create().getMatcher().matches(context));
    TokenMatcher.State matcherState = attributesMap.require(TokenMatcher.State.class);
    assertThat(matcherState.getTokens().get("group"), is(equalTo("vthiery")));
    assertThat(matcherState.getTokens().get("project"), is(equalTo("jsonformoderncpp")));
    assertThat(matcherState.getTokens().get("version"), is(equalTo("2.1.1")));
  }

  @Test
  public void canMatchOnConanInfo() {
    when(request.getPath()).thenReturn("/vthiery/jsonformoderncpp/2.1.1/stable/packages/5ab84d6acfe1f23c4fae0ab88f26e3a396351ac9/conaninfo.txt");
    assertTrue(underTest.conanInfo().handler(handler).create().getMatcher().matches(context));
    TokenMatcher.State matcherState = attributesMap.require(TokenMatcher.State.class);
    assertThat(matcherState.getTokens().get("group"), is(equalTo("vthiery")));
    assertThat(matcherState.getTokens().get("project"), is(equalTo("jsonformoderncpp")));
    assertThat(matcherState.getTokens().get("version"), is(equalTo("2.1.1")));
  }

  @Test
  public void canMatchOnConanPackage() {
    when(request.getPath()).thenReturn("/vthiery/jsonformoderncpp/2.1.1/stable/packages/5ab84d6acfe1f23c4fae0ab88f26e3a396351ac9/conan_package.tgz");
    assertTrue(underTest.conanPackage().handler(handler).create().getMatcher().matches(context));
    TokenMatcher.State matcherState = attributesMap.require(TokenMatcher.State.class);
    assertThat(matcherState.getTokens().get("group"), is(equalTo("vthiery")));
    assertThat(matcherState.getTokens().get("project"), is(equalTo("jsonformoderncpp")));
    assertThat(matcherState.getTokens().get("version"), is(equalTo("2.1.1")));
  }
}
