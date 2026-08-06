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
import java.util.Optional;
import java.util.function.Supplier;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponentBuilder;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FluentComponentBuilderImplTest
{
  private static final int REPOSITORY_ID = 42;

  private static final String COMPONENT_NAME = "my-component";

  @Mock
  private ContentFacetSupport facet;

  @Mock
  private ComponentStore<?> componentStore;

  @Mock
  private Repository repository;

  private FluentComponentBuilderImpl underTest;

  @Before
  public void setUp() {
    when(facet.contentRepositoryId()).thenReturn(REPOSITORY_ID);
    when(facet.repository()).thenReturn(repository);

    underTest = new FluentComponentBuilderImpl(facet, componentStore, FluentComponentBuilderImplTest::normalize,
        COMPONENT_NAME);
  }

  @Test
  public void testVersionNormalizedAutomatically() {
    when(componentStore.getOrCreate(any(Supplier.class), any(Supplier.class))).thenAnswer(invocation -> {
      Supplier<Component> create = invocation.getArgument(1);
      return create.get();
    });

    underTest.version("1.0.0");
    FluentComponent component = underTest.getOrCreate();

    assertThat(component.normalizedVersion(), is("normalized-1.0.0"));
  }

  @Test
  public void testNamespaceDefaultsToEmpty() {
    // namespace defaults to "" when not explicitly set; verify builder is returned
    FluentComponentBuilder result = underTest.namespace("org.example");
    assertThat(result, is(notNullValue()));

    // Also verify the default by calling find without setting namespace -
    // readCoordinate should be called with empty string for namespace
    when(componentStore.readCoordinate(REPOSITORY_ID, "", COMPONENT_NAME, "")).thenReturn(Optional.empty());

    FluentComponentBuilderImpl freshBuilder = new FluentComponentBuilderImpl(facet, componentStore,
        FluentComponentBuilderImplTest::normalize, COMPONENT_NAME);
    freshBuilder.find();

    verify(componentStore).readCoordinate(REPOSITORY_ID, "", COMPONENT_NAME, "");
  }

  @Test
  public void testFluentChaining() {
    FluentComponentBuilder result = underTest
        .namespace("org.example")
        .kind("jar")
        .version("1.0.0")
        .normalizedVersion("001.000.000")
        .attributes("format", "maven2");

    assertThat(result, is(notNullValue()));
    assertThat(result, is((FluentComponentBuilder) underTest));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetOrCreateWhenNotExisting() {
    ComponentData existingComponent = createComponentData();

    // Configure getOrCreate to invoke the suppliers: find returns empty, so create is called
    when(componentStore.getOrCreate(any(Supplier.class), any(Supplier.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Component>> find = invocation.getArgument(0);
          Supplier<Component> create = invocation.getArgument(1);
          return find.get().orElseGet(create);
        });

    when(componentStore.readCoordinate(REPOSITORY_ID, "org.example", COMPONENT_NAME, "1.0.0"))
        .thenReturn(Optional.empty());

    underTest.namespace("org.example").version("1.0.0").kind("jar").normalizedVersion("001.000.000");

    FluentComponent result = underTest.getOrCreate();

    assertThat(result, is(notNullValue()));
    verify(componentStore).readCoordinate(REPOSITORY_ID, "org.example", COMPONENT_NAME, "1.0.0");
    verify(componentStore).createComponent(any(ComponentData.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testGetOrCreateWhenExisting() {
    ComponentData existingComponent = createComponentData();

    // Configure getOrCreate to invoke the suppliers: find returns existing, so create is not called
    when(componentStore.getOrCreate(any(Supplier.class), any(Supplier.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Component>> find = invocation.getArgument(0);
          Supplier<Component> create = invocation.getArgument(1);
          return find.get().orElseGet(create);
        });

    when(componentStore.readCoordinate(REPOSITORY_ID, "org.example", COMPONENT_NAME, "1.0.0"))
        .thenReturn(Optional.of(existingComponent));

    underTest.namespace("org.example").version("1.0.0");

    FluentComponent result = underTest.getOrCreate();

    assertThat(result, is(notNullValue()));
    assertThat(result.name(), is(COMPONENT_NAME));
    verify(componentStore).readCoordinate(REPOSITORY_ID, "org.example", COMPONENT_NAME, "1.0.0");
    verify(componentStore, never()).createComponent(any(ComponentData.class));
  }

  @Test
  public void testFind() {
    ComponentData existingComponent = createComponentData();

    when(componentStore.readCoordinate(REPOSITORY_ID, "org.example", COMPONENT_NAME, "1.0.0"))
        .thenReturn(Optional.of(existingComponent));

    underTest.namespace("org.example").version("1.0.0");

    Optional<FluentComponent> result = underTest.find();

    assertThat(result.isPresent(), is(true));
    assertThat(result.get().name(), is(COMPONENT_NAME));
    assertThat(result.get().namespace(), is("org.example"));
    assertThat(result.get().version(), is("1.0.0"));
  }

  @Test
  public void testFindReturnsEmptyWhenNotExisting() {
    when(componentStore.readCoordinate(REPOSITORY_ID, "", COMPONENT_NAME, ""))
        .thenReturn(Optional.empty());

    Optional<FluentComponent> result = underTest.find();

    assertThat(result.isPresent(), is(false));
  }

  @Test
  public void testKindWithOptional() {
    // kind(Optional.of("jar")) should set the kind
    FluentComponentBuilder result = underTest.kind(Optional.of("jar"));
    assertThat(result, is(notNullValue()));

    // kind(Optional.empty()) should leave the kind unchanged
    result = underTest.kind(Optional.empty());
    assertThat(result, is(notNullValue()));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAttributes() {
    when(componentStore.getOrCreate(any(Supplier.class), any(Supplier.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Component>> find = invocation.getArgument(0);
          Supplier<Component> create = invocation.getArgument(1);
          return find.get().orElseGet(create);
        });

    when(componentStore.readCoordinate(REPOSITORY_ID, "", COMPONENT_NAME, ""))
        .thenReturn(Optional.empty());

    underTest
        .attributes("format", "maven2")
        .attributes("classifier", "sources");

    FluentComponent result = underTest.getOrCreate();

    assertThat(result, is(notNullValue()));

    // Verify createComponent was called and capture the ComponentData
    ArgumentCaptor<ComponentData> captor = ArgumentCaptor.forClass(ComponentData.class);
    verify(componentStore).createComponent(captor.capture());

    ComponentData created = captor.getValue();
    assertThat(created.attributes().backing().get("format"), is("maven2"));
    assertThat(created.attributes().backing().get("classifier"), is("sources"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCreateComponentSetsAllFields() {
    when(componentStore.getOrCreate(any(Supplier.class), any(Supplier.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Component>> find = invocation.getArgument(0);
          Supplier<Component> create = invocation.getArgument(1);
          return find.get().orElseGet(create);
        });

    when(componentStore.readCoordinate(REPOSITORY_ID, "org.example", COMPONENT_NAME, "2.0.0"))
        .thenReturn(Optional.empty());

    underTest
        .namespace("org.example")
        .version("2.0.0")
        .kind("jar")
        .normalizedVersion("002.000.000");

    underTest.getOrCreate();

    ArgumentCaptor<ComponentData> captor = ArgumentCaptor.forClass(ComponentData.class);
    verify(componentStore).createComponent(captor.capture());

    ComponentData created = captor.getValue();
    assertThat(created.namespace(), is("org.example"));
    assertThat(created.name(), is(COMPONENT_NAME));
    assertThat(created.kind(), is("jar"));
    assertThat(created.version(), is("2.0.0"));
    assertThat(created.normalizedVersion(), is("002.000.000"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCreateComponentWithoutAttributes() {
    when(componentStore.getOrCreate(any(Supplier.class), any(Supplier.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Component>> find = invocation.getArgument(0);
          Supplier<Component> create = invocation.getArgument(1);
          return find.get().orElseGet(create);
        });

    when(componentStore.readCoordinate(REPOSITORY_ID, "", COMPONENT_NAME, ""))
        .thenReturn(Optional.empty());

    // Do not set any attributes - the attributes map should be null
    underTest.getOrCreate();

    ArgumentCaptor<ComponentData> captor = ArgumentCaptor.forClass(ComponentData.class);
    verify(componentStore).createComponent(captor.capture());

    ComponentData created = captor.getValue();
    // Attributes backing map should be empty since no attributes were set
    assertTrue(created.attributes().backing().isEmpty());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCreateComponentWithDefaultKindNamespaceVersion() {
    when(componentStore.getOrCreate(any(Supplier.class), any(Supplier.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Component>> find = invocation.getArgument(0);
          Supplier<Component> create = invocation.getArgument(1);
          return find.get().orElseGet(create);
        });

    when(componentStore.readCoordinate(REPOSITORY_ID, "", COMPONENT_NAME, ""))
        .thenReturn(Optional.empty());

    // Create without setting any optional fields - defaults should apply
    underTest.getOrCreate();

    ArgumentCaptor<ComponentData> captor = ArgumentCaptor.forClass(ComponentData.class);
    verify(componentStore).createComponent(captor.capture());

    ComponentData created = captor.getValue();
    assertThat(created.kind(), is(""));
    assertThat(created.namespace(), is(""));
    assertThat(created.version(), is(""));
    assertThat(created.normalizedVersion(), is(""));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testKindWithOptionalPresent() {
    when(componentStore.getOrCreate(any(Supplier.class), any(Supplier.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Component>> find = invocation.getArgument(0);
          Supplier<Component> create = invocation.getArgument(1);
          return find.get().orElseGet(create);
        });

    when(componentStore.readCoordinate(REPOSITORY_ID, "", COMPONENT_NAME, ""))
        .thenReturn(Optional.empty());

    underTest.kind(Optional.of("docker"));

    underTest.getOrCreate();

    ArgumentCaptor<ComponentData> captor = ArgumentCaptor.forClass(ComponentData.class);
    verify(componentStore).createComponent(captor.capture());

    assertThat(captor.getValue().kind(), is("docker"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testKindWithOptionalEmptyLeavesDefault() {
    when(componentStore.getOrCreate(any(Supplier.class), any(Supplier.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Component>> find = invocation.getArgument(0);
          Supplier<Component> create = invocation.getArgument(1);
          return find.get().orElseGet(create);
        });

    when(componentStore.readCoordinate(REPOSITORY_ID, "", COMPONENT_NAME, ""))
        .thenReturn(Optional.empty());

    underTest.kind(Optional.empty());

    underTest.getOrCreate();

    ArgumentCaptor<ComponentData> captor = ArgumentCaptor.forClass(ComponentData.class);
    verify(componentStore).createComponent(captor.capture());

    // kind should still be the default empty string
    assertThat(captor.getValue().kind(), is(""));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAttributesCopiedToComponentOnCreate() {
    when(componentStore.getOrCreate(any(Supplier.class), any(Supplier.class)))
        .thenAnswer(invocation -> {
          Supplier<Optional<Component>> find = invocation.getArgument(0);
          Supplier<Component> create = invocation.getArgument(1);
          return find.get().orElseGet(create);
        });

    when(componentStore.readCoordinate(REPOSITORY_ID, "", COMPONENT_NAME, ""))
        .thenReturn(Optional.empty());

    underTest
        .attributes("key1", "value1")
        .attributes("key2", 42)
        .attributes("key3", true);

    underTest.getOrCreate();

    ArgumentCaptor<ComponentData> captor = ArgumentCaptor.forClass(ComponentData.class);
    verify(componentStore).createComponent(captor.capture());

    ComponentData created = captor.getValue();
    assertThat(created.attributes().backing(), hasEntry("key1", "value1"));
    assertThat(created.attributes().backing(), hasEntry("key2", 42));
    assertThat(created.attributes().backing(), hasEntry("key3", true));
  }

  @Test
  public void testFindInMembersWithGroupRepository() {
    // Set up repository as a group type
    when(repository.getType()).thenReturn(new GroupType());

    // Set up group facet with a leaf member
    GroupFacet groupFacet = mockGroupFacetWithMembers(10);

    when(repository.facet(GroupFacet.class)).thenReturn(groupFacet);

    ComponentData memberComponent = createComponentData();
    when(componentStore.readCoordinateInRepoIds(eq("org.example"), eq(COMPONENT_NAME), eq("1.0.0"), anySet()))
        .thenReturn(Optional.of(memberComponent));

    underTest.namespace("org.example").version("1.0.0");

    Optional<FluentComponent> result = underTest.findInMembers();

    assertTrue(result.isPresent());
    assertThat(result.get().name(), is(COMPONENT_NAME));
  }

  @Test
  public void testFindInMembersReturnsEmptyWhenNotFound() {
    when(repository.getType()).thenReturn(new GroupType());

    GroupFacet groupFacet = mockGroupFacetWithMembers(10);
    when(repository.facet(GroupFacet.class)).thenReturn(groupFacet);

    when(componentStore.readCoordinateInRepoIds(eq(""), eq(COMPONENT_NAME), eq(""), anySet()))
        .thenReturn(Optional.empty());

    Optional<FluentComponent> result = underTest.findInMembers();

    assertFalse(result.isPresent());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindInMembersThrowsForNonGroupRepository() {
    when(repository.getType()).thenReturn(new HostedType());

    underTest.findInMembers();
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullFacet() {
    new FluentComponentBuilderImpl(null, componentStore, FluentComponentBuilderImplTest::normalize, COMPONENT_NAME);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullComponentStore() {
    new FluentComponentBuilderImpl(facet, null, FluentComponentBuilderImplTest::normalize, COMPONENT_NAME);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructorRejectsNullName() {
    new FluentComponentBuilderImpl(facet, componentStore, FluentComponentBuilderImplTest::normalize, null);
  }

  @Test(expected = NullPointerException.class)
  public void testNamespaceRejectsNull() {
    underTest.namespace(null);
  }

  @Test(expected = NullPointerException.class)
  public void testKindRejectsNull() {
    underTest.kind((String) null);
  }

  @Test(expected = NullPointerException.class)
  public void testVersionRejectsNull() {
    underTest.version(null);
  }

  @Test(expected = NullPointerException.class)
  public void testNormalizedVersionRejectsNull() {
    underTest.normalizedVersion(null);
  }

  @Test(expected = NullPointerException.class)
  public void testAttributesRejectsNullKey() {
    underTest.attributes(null, "value");
  }

  @Test(expected = NullPointerException.class)
  public void testAttributesRejectsNullValue() {
    underTest.attributes("key", null);
  }

  private static String normalize(final String version) {
    return "normalized-" + version;
  }

  private GroupFacet mockGroupFacetWithMembers(final int memberRepositoryId) {
    GroupFacet groupFacet = org.mockito.Mockito.mock(GroupFacet.class);

    Repository memberRepo = org.mockito.Mockito.mock(Repository.class);
    ContentFacet memberContentFacet = org.mockito.Mockito.mock(ContentFacet.class);
    when(memberRepo.facet(ContentFacet.class)).thenReturn(memberContentFacet);
    when(memberContentFacet.contentRepositoryId()).thenReturn(memberRepositoryId);
    when(groupFacet.leafMembers()).thenReturn(Collections.singletonList(memberRepo));

    return groupFacet;
  }

  private ComponentData createComponentData() {
    ComponentData component = new ComponentData();
    component.setRepositoryId(REPOSITORY_ID);
    component.setComponentId(1);
    component.setNamespace("org.example");
    component.setName(COMPONENT_NAME);
    component.setKind("jar");
    component.setVersion("1.0.0");
    component.setNormalizedVersion("001.000.000");
    return component;
  }
}
