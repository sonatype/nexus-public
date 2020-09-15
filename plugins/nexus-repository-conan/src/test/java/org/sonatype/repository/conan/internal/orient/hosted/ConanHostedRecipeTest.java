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
package org.sonatype.repository.conan.internal.orient.hosted;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.handlers.HighAvailabilitySupportChecker;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;
import org.sonatype.repository.conan.internal.ConanFormat;
import org.sonatype.repository.conan.internal.common.v1.ConanRoutes;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.sonatype.goodies.testsupport.hamcrest.DiffMatchers.equalTo;
import static org.sonatype.nexus.repository.http.HttpMethods.POST;
import static org.sonatype.nexus.repository.http.HttpMethods.PUT;
import static org.sonatype.repository.conan.internal.ConanSystemProperties.HOSTED_ENABLED_PROPERTY;

public class ConanHostedRecipeTest
    extends TestSupport
{
  @Mock
  Request request;

  @Mock
  Context context;

  @Mock
  Handler handler;

  @Mock
  HighAvailabilitySupportChecker highAvailabilitySupportChecker;

  AttributesMap attributesMap;

  @Before
  public void setUp() {
    attributesMap = new AttributesMap();
    when(context.getRequest()).thenReturn(request);
    when(context.getAttributes()).thenReturn(attributesMap);
  }

  @After
  public void tearDown() {
    System.getProperties().remove(HOSTED_ENABLED_PROPERTY);
  }

  @Test
  public void canMatchOnConanUpload_url() {
    when(request.getAction()).thenReturn(POST);
    when(request.getPath()).thenReturn("/v1/conans/project/2.1.1/group/stable/upload_urls");

    assertTrue(ConanRoutes.uploadUrls().handler(handler).create().getMatcher().matches(context));
    TokenMatcher.State matcherState = attributesMap.require(TokenMatcher.State.class);
    assertThat(matcherState.getTokens().get("group"), is(equalTo("group")));
    assertThat(matcherState.getTokens().get("project"), is(equalTo("project")));
    assertThat(matcherState.getTokens().get("version"), is(equalTo("2.1.1")));
  }

  @Test
  public void canMatchOnConanPackage() {
    when(request.getAction()).thenReturn(POST);
    when(request.getPath()).thenReturn("/v1/conans/project/2.1.1/group/stable/packages/5ab84d6acfe1f23c4fae0ab88f26e3a396351ac9/upload_urls");

    assertTrue(ConanRoutes.uploadUrls().handler(handler).create().getMatcher().matches(context));
    TokenMatcher.State matcherState = attributesMap.require(TokenMatcher.State.class);
    assertThat(matcherState.getTokens().get("group"), is(equalTo("group")));
    assertThat(matcherState.getTokens().get("project"), is(equalTo("project")));
    assertThat(matcherState.getTokens().get("version"), is(equalTo("2.1.1")));
    assertThat(matcherState.getTokens().get("sha"), is(equalTo("5ab84d6acfe1f23c4fae0ab88f26e3a396351ac9")));
  }

  @Test
  public void canMatchOnConanUploadManifest() {
    when(request.getAction()).thenReturn(PUT);
    when(request.getPath()).thenReturn("/v1/conans/group/project/2.1.1/stable/conanmanifest.txt");

    assertTrue(ConanRoutes.uploadManifest().handler(handler).create().getMatcher().matches(context));
    TokenMatcher.State matcherState = attributesMap.require(TokenMatcher.State.class);
    assertThat(matcherState.getTokens().get("group"), is(equalTo("group")));
    assertThat(matcherState.getTokens().get("project"), is(equalTo("project")));
    assertThat(matcherState.getTokens().get("version"), is(equalTo("2.1.1")));
  }

  @Test
  public void canMatchOnConanUploadManifestPackage() {
    when(request.getAction()).thenReturn(PUT);
    when(request.getPath()).thenReturn("/v1/conans/group/project/2.1.1/stable/packages/5ab84d6acfe1f23c4fae0ab88f26e3a396351ac9/conanmanifest.txt");

    assertTrue(ConanRoutes.uploadManifest().handler(handler).create().getMatcher().matches(context));
    TokenMatcher.State matcherState = attributesMap.require(TokenMatcher.State.class);
    assertThat(matcherState.getTokens().get("group"), is(equalTo("group")));
    assertThat(matcherState.getTokens().get("project"), is(equalTo("project")));
    assertThat(matcherState.getTokens().get("version"), is(equalTo("2.1.1")));
    assertThat(matcherState.getTokens().get("sha"), is(equalTo("5ab84d6acfe1f23c4fae0ab88f26e3a396351ac9")));
  }

  @Test
  public void canMatchOnConanfileUpload() {
    when(request.getAction()).thenReturn(PUT);
    when(request.getPath()).thenReturn("/v1/conans/group/project/2.1.1/stable/conanfile.py");

    assertTrue(ConanRoutes.uploadConanfile().handler(handler).create().getMatcher().matches(context));
    TokenMatcher.State matcherState = attributesMap.require(TokenMatcher.State.class);
    assertThat(matcherState.getTokens().get("group"), is(equalTo("group")));
    assertThat(matcherState.getTokens().get("project"), is(equalTo("project")));
    assertThat(matcherState.getTokens().get("version"), is(equalTo("2.1.1")));
    assertThat(matcherState.getTokens().get("sha"), is(equalTo(null)));
  }

  @Test
  public void canMatchOnConanfileUploadPackage() {
    when(request.getAction()).thenReturn(PUT);
    when(request.getPath()).thenReturn("/v1/conans/group/project/2.1.1/stable/packages/5ab84d6acfe1f23c4fae0ab88f26e3a396351ac9/conanfile.py");

    assertTrue(ConanRoutes.uploadConanfile().handler(handler).create().getMatcher().matches(context));
    TokenMatcher.State matcherState = attributesMap.require(TokenMatcher.State.class);
    assertThat(matcherState.getTokens().get("group"), is(equalTo("group")));
    assertThat(matcherState.getTokens().get("project"), is(equalTo("project")));
    assertThat(matcherState.getTokens().get("version"), is(equalTo("2.1.1")));
    assertThat(matcherState.getTokens().get("sha"), is(equalTo("5ab84d6acfe1f23c4fae0ab88f26e3a396351ac9")));
  }

  @Test
  public void canMatchOnConaninfoUpload() {
    when(request.getAction()).thenReturn(PUT);
    when(request.getPath()).thenReturn("/v1/conans/group/project/2.1.1/stable/conaninfo.txt");

    assertTrue(ConanRoutes.uploadConanInfo().handler(handler).create().getMatcher().matches(context));
    TokenMatcher.State matcherState = attributesMap.require(TokenMatcher.State.class);
    assertThat(matcherState.getTokens().get("group"), is(equalTo("group")));
    assertThat(matcherState.getTokens().get("project"), is(equalTo("project")));
    assertThat(matcherState.getTokens().get("version"), is(equalTo("2.1.1")));
    assertThat(matcherState.getTokens().get("sha"), is(equalTo(null)));
  }

  @Test
  public void canMatchOnConanPackageZipUploadPackage() {
    when(request.getAction()).thenReturn(PUT);
    when(request.getPath()).thenReturn("/v1/conans/group/project/2.1.1/stable/packages/5ab84d6acfe1f23c4fae0ab88f26e3a396351ac9/conan_package.tgz");

    assertTrue(ConanRoutes.uploadConanPackageZip().handler(handler).create().getMatcher().matches(context));
    TokenMatcher.State matcherState = attributesMap.require(TokenMatcher.State.class);
    assertThat(matcherState.getTokens().get("group"), is(equalTo("group")));
    assertThat(matcherState.getTokens().get("project"), is(equalTo("project")));
    assertThat(matcherState.getTokens().get("version"), is(equalTo("2.1.1")));
    assertThat(matcherState.getTokens().get("sha"), is(equalTo("5ab84d6acfe1f23c4fae0ab88f26e3a396351ac9")));
  }

  @Test
  public void hostedEnabledNexusConanHostedEnabledIsTrueHaSupportIsTrue() {
    System.setProperty(HOSTED_ENABLED_PROPERTY, "true");
    when(highAvailabilitySupportChecker.isSupported(ConanFormat.NAME)).thenReturn(true);
    ConanHostedRecipe conanHostedRecipe = new ConanHostedRecipe(new HostedType(), new ConanFormat(), null);
    conanHostedRecipe.setHighAvailabilitySupportChecker(highAvailabilitySupportChecker);
    assertThat(conanHostedRecipe.isFeatureEnabled(), is(true));
  }

  @Test
  public void hostedDisabledNexusConanHostedEnabledIsFalseHaSupportIsFalse() {
    System.setProperty(HOSTED_ENABLED_PROPERTY, "false");
    when(highAvailabilitySupportChecker.isSupported(ConanFormat.NAME)).thenReturn(false);
    ConanHostedRecipe conanHostedRecipe = new ConanHostedRecipe(new HostedType(), new ConanFormat(), null);
    conanHostedRecipe.setHighAvailabilitySupportChecker(highAvailabilitySupportChecker);
    assertThat(conanHostedRecipe.isFeatureEnabled(), is(false));
  }

  @Test
  public void hostedDisabledNexusConanHostedEnabledIsTrueHaSupportIsFalse() {
    System.setProperty(HOSTED_ENABLED_PROPERTY, "true");
    when(highAvailabilitySupportChecker.isSupported(ConanFormat.NAME)).thenReturn(false);
    ConanHostedRecipe conanHostedRecipe = new ConanHostedRecipe(new HostedType(), new ConanFormat(), null);
    conanHostedRecipe.setHighAvailabilitySupportChecker(highAvailabilitySupportChecker);
    assertThat(conanHostedRecipe.isFeatureEnabled(), is(false));
  }

  @Test
  public void hostedDisabledNexusConanHostedEnabledIsFalseHaSupportIsTrue() {
    System.setProperty(HOSTED_ENABLED_PROPERTY, "false");
    when(highAvailabilitySupportChecker.isSupported(ConanFormat.NAME)).thenReturn(true);
    ConanHostedRecipe conanHostedRecipe = new ConanHostedRecipe(new HostedType(), new ConanFormat(), null);
    conanHostedRecipe.setHighAvailabilitySupportChecker(highAvailabilitySupportChecker);
    assertThat(conanHostedRecipe.isFeatureEnabled(), is(false));
  }

  @Test
  public void hostedDisabledNexusConanHostedEnabledIsNotSet() {
    when(highAvailabilitySupportChecker.isSupported(ConanFormat.NAME)).thenReturn(false);
    ConanHostedRecipe conanHostedRecipe = new ConanHostedRecipe(new HostedType(), new ConanFormat(), null);
    conanHostedRecipe.setHighAvailabilitySupportChecker(highAvailabilitySupportChecker);
    assertThat(conanHostedRecipe.isFeatureEnabled(), is(false));
  }
}
