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
package org.sonatype.nexus.repository.content.director;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.WritePolicy;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponentBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ContentDirectorTest
{
  @Mock
  private Repository source;

  @Mock
  private Repository destination;

  @Mock
  private Component component;

  @Mock
  private FluentComponent fluentComponent;

  private ContentDirector underTest;

  @Before
  public void setUp() {
    // Use an anonymous implementation that just uses defaults
    underTest = new ContentDirector()
    {
    };
  }

  @Test
  public void testBeforeMoveReturnsComponent() {
    List<Asset> assets = new ArrayList<>();
    Component result = underTest.beforeMove(component, assets, source, destination);
    assertThat(result, is(component));
  }

  @Test
  public void testAfterMoveReturnsComponent() {
    Component result = underTest.afterMove(component, destination);
    assertThat(result, is(component));
  }

  @Test
  public void testAllowMoveToReturnsFalse() {
    assertThat(underTest.allowMoveTo(destination), is(false));
  }

  @Test
  public void testAllowMoveToComponentReturnsFalse() {
    assertThat(underTest.allowMoveTo(fluentComponent, destination), is(false));
  }

  @Test
  public void testAllowMoveFromReturnsFalse() {
    assertThat(underTest.allowMoveFrom(source), is(false));
  }

  @Test
  public void testAfterMoveListIsNoOp() {
    // Should not throw
    underTest.afterMove(Collections.emptyList(), destination);
  }

  @Test
  public void testRedeployAllowedWhenWritePolicyAllow() {
    Configuration config = mock(Configuration.class);
    org.sonatype.nexus.common.collect.NestedAttributesMap attributes =
        mock(org.sonatype.nexus.common.collect.NestedAttributesMap.class);

    when(destination.getConfiguration()).thenReturn(config);
    when(config.attributes("storage")).thenReturn(attributes);
    when(attributes.get("writePolicy")).thenReturn(WritePolicy.ALLOW.name());

    assertThat(underTest.redeployAllowed(destination, component), is(true));
  }

  @Test
  public void testRedeployNotAllowedWhenWritePolicyDeny() {
    Configuration config = mock(Configuration.class);
    org.sonatype.nexus.common.collect.NestedAttributesMap attributes =
        mock(org.sonatype.nexus.common.collect.NestedAttributesMap.class);

    when(destination.getConfiguration()).thenReturn(config);
    when(config.attributes("storage")).thenReturn(attributes);
    when(attributes.get("writePolicy")).thenReturn("DENY");

    assertThat(underTest.redeployAllowed(destination, component), is(false));
  }

  @Test
  public void testCopyComponentDelegatesToContentFacet() {
    ContentFacet contentFacet = mock(ContentFacet.class);
    FluentComponents components = mock(FluentComponents.class);
    FluentComponentBuilder builder = mock(FluentComponentBuilder.class);
    FluentComponent created = mock(FluentComponent.class);

    when(destination.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.components()).thenReturn(components);
    when(components.name(anyString())).thenReturn(builder);
    when(builder.namespace(anyString())).thenReturn(builder);
    when(builder.version(anyString())).thenReturn(builder);
    when(builder.getOrCreate()).thenReturn(created);

    when(component.name()).thenReturn("artifact");
    when(component.namespace()).thenReturn("org.example");
    when(component.version()).thenReturn("1.0");
    when(component.attributes()).thenReturn(new NestedAttributesMap("attributes", new HashMap<>()));

    FluentComponent result = underTest.copyComponent(component, destination);
    assertThat(result, is(created));

    verify(components).name("artifact");
    verify(builder).namespace("org.example");
    verify(builder).version("1.0");
    verify(builder).getOrCreate();
  }
}
