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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.ComponentSet;
import org.sonatype.nexus.repository.content.SqlGenerator;
import org.sonatype.nexus.repository.content.SqlQueryParameters;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentQuery;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ComponentSetData;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FluentComponentsImplTest
    extends TestSupport
{
  @Mock
  private ContentFacetSupport facet;

  @Mock
  private ComponentStore<?> componentStore;

  @Mock
  private Repository repository;

  private FluentComponentsImpl underTest;

  @Before
  public void setUp() {
    Format format = new Format("maven2")
    {
    };
    when(repository.getFormat()).thenReturn(format);
    when(repository.getType()).thenReturn(new HostedType());
    when(facet.repository()).thenReturn(repository);
    when(facet.contentRepositoryId()).thenReturn(1);

    underTest = new FluentComponentsImpl(facet, componentStore);
  }

  @Test
  public void testName() {
    assertThat(underTest.name("test-component"), is(notNullValue()));
  }

  @Test
  public void testWithComponent() {
    ComponentData component = new ComponentData();
    component.setRepositoryId(1);
    component.setComponentId(1);
    component.setNamespace("ns");
    component.setName("name");
    component.setVersion("1.0");

    FluentComponent result = underTest.with(component);
    assertThat(result, is(notNullValue()));
    assertThat(result.name(), is("name"));
  }

  @Test
  public void testWithFluentComponentReturnsSameInstance() {
    FluentComponent fluentComponent = mock(FluentComponent.class);
    FluentComponent result = underTest.with(fluentComponent);
    assertThat(result, is(fluentComponent));
  }

  @Test
  public void testWithComponentAndNullAssets() {
    ComponentData component = new ComponentData();
    component.setRepositoryId(1);
    component.setComponentId(1);
    component.setNamespace("ns");
    component.setName("name");
    component.setVersion("1.0");

    FluentComponent result = underTest.with(component, null);
    assertThat(result, is(notNullValue()));
  }

  @Test
  public void testCount() {
    when(componentStore.countComponents(eq(1), isNull(), isNull(), isNull())).thenReturn(42);
    assertThat(underTest.count(), is(42));
  }

  @Test
  public void testBrowse() {
    Continuation<Component> continuation = mock(Continuation.class);
    when(componentStore.browseComponents(anyInt(), anyInt(), anyString(), isNull(), isNull(), isNull()))
        .thenReturn(continuation);
    when(continuation.isEmpty()).thenReturn(true);

    Continuation<FluentComponent> result = underTest.browse(10, "token");
    assertThat(result, is(notNullValue()));
  }

  @Test
  public void testNamespaces() {
    Collection<String> expected = Arrays.asList("org.example", "com.test");
    when(componentStore.browseNamespaces(1)).thenReturn(expected);

    Collection<String> result = underTest.namespaces();
    assertThat(result, is(expected));
  }

  @Test
  public void testNames() {
    Collection<String> expected = Arrays.asList("artifact-a", "artifact-b");
    when(componentStore.browseNames(1, "org.example")).thenReturn(expected);

    Collection<String> result = underTest.names("org.example");
    assertThat(result, is(expected));
  }

  @Test
  public void testVersions() {
    Collection<String> expected = Arrays.asList("1.0", "2.0", "3.0");
    when(componentStore.browseVersions(1, "org.example", "artifact")).thenReturn(expected);

    Collection<String> result = underTest.versions("org.example", "artifact");
    assertThat(result, is(expected));
  }

  @Test
  public void testFind() {
    ComponentData component = new ComponentData();
    component.setRepositoryId(1);
    component.setComponentId(5);
    component.setNamespace("ns");
    component.setName("name");
    component.setVersion("1.0");

    when(componentStore.readComponent(anyInt())).thenReturn(Optional.of(component));

    Optional<FluentComponent> result = underTest.find(new DetachedEntityId("5"));
    assertThat(result.isPresent(), is(true));
  }

  @Test
  public void testFindNotFound() {
    when(componentStore.readComponent(anyInt())).thenReturn(Optional.empty());

    Optional<FluentComponent> result = underTest.find(new DetachedEntityId("999"));
    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void testByKind() {
    FluentQuery<FluentComponent> query = underTest.byKind("DOCKER_MANIFEST");
    assertThat(query, is(notNullValue()));
  }

  @Test
  public void testByFilter() {
    FluentQuery<FluentComponent> query = underTest.byFilter("name = :name", Collections.singletonMap("name", "test"));
    assertThat(query, is(notNullValue()));
  }

  @Test
  public void testWithGroupMemberContent() {
    FluentQuery<FluentComponent> query = underTest.withGroupMemberContent();
    assertThat(query, is(notNullValue()));
  }

  @Test
  public void testWithOnlyGroupMemberContent() {
    FluentQuery<FluentComponent> query = underTest.withOnlyGroupMemberContent();
    assertThat(query, is(notNullValue()));
  }

  @Test
  public void testSets() {
    Continuation<ComponentSetData> continuation = mock(Continuation.class);
    when(componentStore.browseSets(1, 25, "token")).thenReturn(continuation);

    Continuation<ComponentSetData> result = underTest.sets(25, "token");
    assertThat(result, is(notNullValue()));
    verify(componentStore).browseSets(1, 25, "token");
  }

  @Test
  public void testWithComponentAndAssetsList() {
    ComponentData component = new ComponentData();
    component.setRepositoryId(1);
    component.setComponentId(1);
    component.setNamespace("ns");
    component.setName("name");
    component.setVersion("1.0");

    Asset asset = mock(Asset.class);
    FluentAssets fluentAssets = mock(FluentAssets.class);
    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(facet.assets()).thenReturn(fluentAssets);
    when(fluentAssets.with(asset)).thenReturn(fluentAsset);

    FluentComponent result = underTest.with(component, List.of(asset));
    assertThat(result, is(notNullValue()));
    verify(fluentAssets).with(asset);
  }

  @Test
  public void testMemberVersionsThrowsForNonGroupRepository() {
    // Repository is already HostedType from setUp
    assertThrows(IllegalArgumentException.class,
        () -> underTest.memberVersions("org.example", "artifact"));
  }

  @Test
  public void testFindComponentInDifferentRepositoryReturnsEmpty() {
    ComponentData component = new ComponentData();
    component.setRepositoryId(999); // different from facet's contentRepositoryId (1)
    component.setComponentId(5);
    component.setNamespace("ns");
    component.setName("name");
    component.setVersion("1.0");

    when(componentStore.readComponent(anyInt())).thenReturn(Optional.of(component));

    Optional<FluentComponent> result = underTest.find(new DetachedEntityId("5"));
    assertThat(result.isPresent(), is(false));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCountUsesNugetV2PathWhenApplicable() {
    Format nugetFormat = new Format("nuget")
    {
    };
    when(repository.getFormat()).thenReturn(nugetFormat);
    when(repository.getType()).thenReturn(new ProxyType());

    Configuration config = mock(Configuration.class);
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> nugetProxy = new HashMap<>();
    nugetProxy.put("nugetVersion", "V2");
    attributes.put("nugetProxy", nugetProxy);
    when(config.getAttributes()).thenReturn(attributes);
    when(repository.getConfiguration()).thenReturn(config);

    when(componentStore.countComponentsWithAssetsBlobs(eq(1), isNull(), isNull(), isNull())).thenReturn(10);

    assertThat(underTest.count(), is(10));
    verify(componentStore).countComponentsWithAssetsBlobs(eq(1), isNull(), isNull(), isNull());
  }

  @Test
  public void testCountUsesRegularPathForNonNuget() {
    when(componentStore.countComponents(eq(1), isNull(), isNull(), isNull())).thenReturn(5);

    assertThat(underTest.count(), is(5));
    verify(componentStore).countComponents(eq(1), isNull(), isNull(), isNull());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCountUsesRegularPathForNugetV3() {
    Format nugetFormat = new Format("nuget")
    {
    };
    when(repository.getFormat()).thenReturn(nugetFormat);
    when(repository.getType()).thenReturn(new ProxyType());

    Configuration config = mock(Configuration.class);
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    Map<String, Object> nugetProxy = new HashMap<>();
    nugetProxy.put("nugetVersion", "V3");
    attributes.put("nugetProxy", nugetProxy);
    when(config.getAttributes()).thenReturn(attributes);
    when(repository.getConfiguration()).thenReturn(config);

    when(componentStore.countComponents(eq(1), isNull(), isNull(), isNull())).thenReturn(3);

    assertThat(underTest.count(), is(3));
    verify(componentStore).countComponents(eq(1), isNull(), isNull(), isNull());
  }

  @Test
  public void testBrowseEager() {
    Continuation<ComponentData> continuation = mock(Continuation.class);
    when(continuation.isEmpty()).thenReturn(true);
    when(componentStore.browseComponentsEager(
        org.mockito.ArgumentMatchers.anySet(), anyInt(), anyString(), isNull(), isNull(), isNull()))
            .thenReturn(continuation);

    Continuation<FluentComponent> result = underTest.browseEager(10, "token");
    assertThat(result, is(notNullValue()));
  }

  @Test
  public void testBrowseWithAssets() {
    Continuation<Component> continuation = mock(Continuation.class);
    when(continuation.isEmpty()).thenReturn(true);
    when(componentStore.browseComponentsWithAssets(eq(1), anyInt(), anyString()))
        .thenReturn(continuation);

    Continuation<FluentComponent> result = underTest.browseWithAssets(10, "token");
    assertThat(result, is(notNullValue()));
  }

  @Test
  public void testFindComponentInGroupMemberRepository() {
    when(repository.getType()).thenReturn(new GroupType());

    ComponentData component = new ComponentData();
    component.setRepositoryId(42);
    component.setComponentId(5);
    component.setNamespace("ns");
    component.setName("name");
    component.setVersion("1.0");

    when(componentStore.readComponent(anyInt())).thenReturn(Optional.of(component));

    // Set up member repository with matching content repo id
    Repository memberRepo = mock(Repository.class);
    ContentFacet memberFacet = mock(ContentFacet.class);
    when(memberFacet.contentRepositoryId()).thenReturn(42);
    when(memberRepo.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(memberFacet));

    GroupFacet groupFacet = mock(GroupFacet.class);
    when(groupFacet.allMembers()).thenReturn(List.of(memberRepo));
    when(repository.facet(GroupFacet.class)).thenReturn(groupFacet);

    Optional<FluentComponent> result = underTest.find(new DetachedEntityId("5"));
    assertThat(result.isPresent(), is(true));
  }

  @Test
  public void testFindComponentNotInGroupMembersReturnsEmpty() {
    when(repository.getType()).thenReturn(new GroupType());

    ComponentData component = new ComponentData();
    component.setRepositoryId(999);
    component.setComponentId(5);
    component.setNamespace("ns");
    component.setName("name");
    component.setVersion("1.0");

    when(componentStore.readComponent(anyInt())).thenReturn(Optional.of(component));

    Repository memberRepo = mock(Repository.class);
    ContentFacet memberFacet = mock(ContentFacet.class);
    when(memberFacet.contentRepositoryId()).thenReturn(42);
    when(memberRepo.optionalFacet(ContentFacet.class)).thenReturn(Optional.of(memberFacet));

    GroupFacet groupFacet = mock(GroupFacet.class);
    when(groupFacet.allMembers()).thenReturn(List.of(memberRepo));
    when(repository.facet(GroupFacet.class)).thenReturn(groupFacet);

    Optional<FluentComponent> result = underTest.find(new DetachedEntityId("5"));
    assertThat(result.isPresent(), is(false));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testMemberVersionsForGroupRepository() {
    when(repository.getType()).thenReturn(new GroupType());

    Repository memberRepo = mock(Repository.class);
    ContentFacet memberFacet = mock(ContentFacet.class);
    when(memberFacet.contentRepositoryId()).thenReturn(2);
    when(memberRepo.facet(ContentFacet.class)).thenReturn(memberFacet);

    GroupFacet groupFacet = mock(GroupFacet.class);
    when(groupFacet.allMembers()).thenReturn(List.of(memberRepo));
    when(repository.facet(GroupFacet.class)).thenReturn(groupFacet);

    Collection<String> expected = Arrays.asList("1.0", "2.0");
    when(componentStore.browseVersionsByRepoIds(eq("org.example"), eq("artifact"),
        org.mockito.ArgumentMatchers.anySet()))
            .thenReturn(expected);

    Collection<String> result = underTest.memberVersions("org.example", "artifact");
    assertThat(result, is(expected));
  }

  @Test
  public void testBySet() {
    ComponentSet componentSet = mock(ComponentSet.class);
    Continuation<Component> continuation = mock(Continuation.class);
    when(componentStore.browseComponentsBySet(eq(1), eq(componentSet), eq(25), eq("token")))
        .thenReturn(continuation);

    Continuation<FluentComponent> result = underTest.bySet(componentSet, 25, "token");
    assertThat(result, is(notNullValue()));
    verify(componentStore).browseComponentsBySet(1, componentSet, 25, "token");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSelectComponents() {
    SqlGenerator<SqlQueryParameters> generator = mock(SqlGenerator.class);
    SqlQueryParameters params = mock(SqlQueryParameters.class);
    Continuation<Component> continuation = mock(Continuation.class);
    when(componentStore.selectComponents(generator, params)).thenReturn(continuation);

    Continuation<FluentComponent> result = underTest.selectComponents(generator, params);
    assertThat(result, is(notNullValue()));
    verify(componentStore).selectComponents(generator, params);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSelectComponentsWithAssets() {
    SqlGenerator<SqlQueryParameters> generator = mock(SqlGenerator.class);
    SqlQueryParameters params = mock(SqlQueryParameters.class);
    Continuation<Component> continuation = mock(Continuation.class);
    when(componentStore.selectComponentsWithAssets(generator, params)).thenReturn(continuation);

    Continuation<FluentComponent> result = underTest.selectComponentsWithAssets(generator, params);
    assertThat(result, is(notNullValue()));
    verify(componentStore).selectComponentsWithAssets(generator, params);
  }

  @Test
  public void testBrowseForGroupRepositoryAddsLocalConstraint() {
    when(repository.getType()).thenReturn(new GroupType());
    when(repository.facet(ContentFacet.class)).thenReturn(facet);

    // LOCAL constraint returns single repo ID, so doBrowse uses the single-repo overload
    Continuation<Component> continuation = mock(Continuation.class);
    when(continuation.isEmpty()).thenReturn(true);
    when(componentStore.browseComponents(anyInt(), anyInt(), anyString(), isNull(), isNull(), isNull()))
        .thenReturn(continuation);

    Continuation<FluentComponent> result = underTest.browse(10, "token");
    assertThat(result, is(notNullValue()));
  }

  @Test
  public void testCountNugetV2NullConfiguration() {
    Format nugetFormat = new Format("nuget")
    {
    };
    when(repository.getFormat()).thenReturn(nugetFormat);
    when(repository.getType()).thenReturn(new ProxyType());
    when(repository.getConfiguration()).thenReturn(null);

    when(componentStore.countComponents(eq(1), isNull(), isNull(), isNull())).thenReturn(7);

    assertThat(underTest.count(), is(7));
    verify(componentStore).countComponents(eq(1), isNull(), isNull(), isNull());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCountNugetV2NullAttributes() {
    Format nugetFormat = new Format("nuget")
    {
    };
    when(repository.getFormat()).thenReturn(nugetFormat);
    when(repository.getType()).thenReturn(new ProxyType());

    Configuration config = mock(Configuration.class);
    when(config.getAttributes()).thenReturn(null);
    when(repository.getConfiguration()).thenReturn(config);

    when(componentStore.countComponents(eq(1), isNull(), isNull(), isNull())).thenReturn(8);

    assertThat(underTest.count(), is(8));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCountNugetV2MissingNugetProxyAttribute() {
    Format nugetFormat = new Format("nuget")
    {
    };
    when(repository.getFormat()).thenReturn(nugetFormat);
    when(repository.getType()).thenReturn(new ProxyType());

    Configuration config = mock(Configuration.class);
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    when(config.getAttributes()).thenReturn(attributes);
    when(repository.getConfiguration()).thenReturn(config);

    when(componentStore.countComponents(eq(1), isNull(), isNull(), isNull())).thenReturn(9);

    assertThat(underTest.count(), is(9));
  }

  @Test
  public void testWithFluentComponentAndAssetsReturnsSameInstance() {
    FluentComponent fluentComponent = mock(FluentComponent.class);
    FluentComponent result = underTest.with(fluentComponent, List.of(mock(Asset.class)));
    assertThat(result, is(fluentComponent));
  }
}
