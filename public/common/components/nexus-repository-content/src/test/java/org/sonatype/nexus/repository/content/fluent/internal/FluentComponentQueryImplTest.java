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
package org.sonatype.nexus.repository.content.fluent.internal;

import java.util.Collections;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.types.HostedType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FluentComponentQueryImplTest
{
  @Mock
  private ContentFacetSupport facet;

  @Mock
  private ComponentStore<?> componentStore;

  @Mock
  private Repository repository;

  private FluentComponentsImpl components;

  @Before
  public void setUp() {
    Format format = new Format("maven2")
    {
    };
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(new HostedType());
    when(facet.repository()).thenReturn(repository);
    when(facet.contentRepositoryId()).thenReturn(1);

    components = new FluentComponentsImpl(facet, componentStore);
  }

  @Test
  public void testCountByKind() {
    when(componentStore.countComponents(eq(1), eq("DOCKER"), isNull(), isNull())).thenReturn(5);
    FluentComponentQueryImpl query = new FluentComponentQueryImpl(components, "DOCKER");
    assertThat(query.count(), is(5));
  }

  @Test
  public void testCountByFilter() {
    when(componentStore.countComponents(eq(1), isNull(), eq("name = :name"), any())).thenReturn(3);
    FluentComponentQueryImpl query = new FluentComponentQueryImpl(
        components, "name = :name", Collections.singletonMap("name", "test"));
    assertThat(query.count(), is(3));
  }

  @Test
  public void testBrowseByKind() {
    Continuation continuation = mock(Continuation.class);
    when(componentStore.browseComponents(anyInt(), anyInt(), anyString(), eq("DOCKER"), isNull(), isNull()))
        .thenReturn(continuation);

    FluentComponentQueryImpl query = new FluentComponentQueryImpl(components, "DOCKER");
    Continuation<FluentComponent> result = query.browse(10, "token");
    assertThat(result, is(notNullValue()));
  }

  @Test
  public void testBrowseEager() {
    Continuation continuation = mock(Continuation.class);
    when(componentStore.browseComponentsEager(any(), anyInt(), anyString(), eq("DOCKER"), isNull(), isNull()))
        .thenReturn(continuation);

    FluentComponentQueryImpl query = new FluentComponentQueryImpl(components, "DOCKER");
    Continuation<FluentComponent> result = query.browseEager(10, "token");
    assertThat(result, is(notNullValue()));
  }
}
