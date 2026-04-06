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
package org.sonatype.nexus.repository.content.search;

import java.util.Optional;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponentBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultComponentFinderTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private ContentFacet contentFacet;

  @Mock
  private FluentComponents components;

  @Mock
  private FluentComponent fluentComponent;

  @Mock
  private FluentComponentBuilder builder;

  private DefaultComponentFinder underTest;

  @Before
  public void setUp() {
    when(repository.facet(ContentFacet.class)).thenReturn(contentFacet);
    when(contentFacet.components()).thenReturn(components);
    when(components.name(anyString())).thenReturn(builder);
    when(builder.namespace(anyString())).thenReturn(builder);
    when(builder.version(anyString())).thenReturn(builder);

    underTest = new DefaultComponentFinder();
  }

  @Test
  public void testFindByNameNamespaceVersion() {
    when(builder.find()).thenReturn(Optional.of(fluentComponent));

    Stream<FluentComponent> result = underTest.findComponentsByModel(
        repository, null, "org.example", "my-artifact", "1.0");

    assertThat(result.count(), is(1L));
    verify(components).name("my-artifact");
    verify(builder).namespace("org.example");
    verify(builder).version("1.0");
  }

  @Test
  public void testFindByIdFirst() {
    when(components.find(any())).thenReturn(Optional.of(fluentComponent));

    Stream<FluentComponent> result = underTest.findComponentsByModel(
        repository, "123", "org.example", "my-artifact", "1.0");

    assertThat(result.count(), is(1L));
    verify(components).find(any());
    verify(components, never()).name(anyString());
  }

  @Test
  public void testFallsBackToNameWhenIdNotFound() {
    when(components.find(any())).thenReturn(Optional.empty());
    when(builder.find()).thenReturn(Optional.of(fluentComponent));

    Stream<FluentComponent> result = underTest.findComponentsByModel(
        repository, "999", "org.example", "my-artifact", "1.0");

    assertThat(result.count(), is(1L));
    verify(components).find(any());
    verify(components).name("my-artifact");
  }

  @Test
  public void testReturnsEmptyStreamForNullRepository() {
    Stream<FluentComponent> result = underTest.findComponentsByModel(
        null, null, "org.example", "my-artifact", "1.0");

    assertThat(result.count(), is(0L));
  }

  @Test
  public void testReturnsEmptyStreamWhenNotFound() {
    when(builder.find()).thenReturn(Optional.empty());

    Stream<FluentComponent> result = underTest.findComponentsByModel(
        repository, null, "org.example", "my-artifact", "1.0");

    assertThat(result.count(), is(0L));
  }

  @Test
  public void testHandlesNullNamespace() {
    when(builder.find()).thenReturn(Optional.of(fluentComponent));

    Stream<FluentComponent> result = underTest.findComponentsByModel(
        repository, null, null, "my-artifact", "1.0");

    assertThat(result.count(), is(1L));
    verify(builder).namespace("");
  }

  @Test
  public void testHandlesNullVersion() {
    when(builder.find()).thenReturn(Optional.of(fluentComponent));

    Stream<FluentComponent> result = underTest.findComponentsByModel(
        repository, null, "ns", "name", null);

    assertThat(result.count(), is(1L));
    verify(builder).version("");
  }
}
