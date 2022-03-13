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
package org.sonatype.nexus.content.maven.internal.recipe;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.content.maven.store.Maven2ComponentData;
import org.sonatype.nexus.content.maven.store.Maven2ComponentStore;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;

import org.apache.maven.model.Model;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.content.AttributeOperation.OVERLAY;
import static org.sonatype.nexus.repository.maven.MavenPath.SignatureType.GPG;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_ARTIFACT_ID;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_CLASSIFIER;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_GROUP_ID;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_PACKAGING;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_POM_DESCRIPTION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_POM_NAME;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Maven2Format.NAME;

public class MavenAttributesHelperTest
    extends TestSupport
{
  private static final String PACKAGING = "PACKAGING";

  private static final String POM_NAME = "aPom";

  private static final String POM_DESCRIPTION = "A Pom Description";

  @Captor
  private ArgumentCaptor<Map<String, String>> attributesValueCaptor;

  @Captor
  private ArgumentCaptor<Maven2ComponentData> componentDataValueCaptor;

  @Mock
  private FluentComponent fluentComponent;

  @Mock
  private FluentAsset fluentAsset;

  @Mock
  private MavenPath mavenPath;

  @Mock
  private Model model;

  @Mock
  private Maven2ComponentStore componentStore;

  private Coordinates coordinates;

  @Before
  public void setup() {
    coordinates = new Coordinates(false, "org.hamcrest", "hamcrest-core",
        "2.2", 3600L, 100, "2.2", "test", ".jar", GPG);
  }

  @Test
  public void shouldPutComponentAttributesInAMap() {
    mockComponent();
    MavenAttributesHelper.setMavenAttributes(componentStore, fluentComponent, coordinates, Optional.empty(), 1);

    verify(fluentComponent).attributes(eq(OVERLAY), eq(NAME), attributesValueCaptor.capture());
    verify(componentStore, only()).updateBaseVersion(componentDataValueCaptor.capture());

    Maven2ComponentData componentData = componentDataValueCaptor.getValue();
    assertEquals(coordinates.getGroupId(), componentData.namespace());
    assertEquals(coordinates.getArtifactId(), componentData.name());
    assertEquals(coordinates.getVersion(), componentData.version());
    assertEquals(coordinates.getBaseVersion(), componentData.getBaseVersion());

    assertGroupArtifactVersionSet(5, attributesValueCaptor.getValue());
  }

  @Test
  public void shouldPutAssetAttributesInAMap() {
    when(mavenPath.getCoordinates()).thenReturn(coordinates);

    MavenAttributesHelper.setMavenAttributes(fluentAsset, mavenPath);

    verify(fluentAsset).attributes(eq(OVERLAY), eq(NAME), attributesValueCaptor.capture());
    Map<String, String> map = attributesValueCaptor.getValue();
    assertGroupArtifactVersionSet(6, map);
    assertThat(map, hasEntry(P_CLASSIFIER, coordinates.getClassifier()));
    assertThat(map, hasEntry(P_VERSION, coordinates.getVersion()));
  }

  @Test
  public void shouldAddPomAttributesToExistingAttributes() {
    when(mavenPath.getCoordinates()).thenReturn(coordinates);
    mockComponent();
    mockModel();
    when(fluentComponent.attributes(NAME)).thenReturn(aNestedAttributesMap());

    MavenAttributesHelper.setMavenAttributes(componentStore, fluentComponent, coordinates, Optional.of(model), 1);

    verify(fluentComponent).attributes(eq(OVERLAY), eq(NAME), attributesValueCaptor.capture());
    Map<String, String> map = attributesValueCaptor.getValue();
    assertThat(map.entrySet(), hasSize(8));
    assertThat(map, hasEntry(P_PACKAGING, PACKAGING));
    assertThat(map, hasEntry(P_POM_NAME, POM_NAME));
    assertThat(map, hasEntry(P_POM_DESCRIPTION, POM_DESCRIPTION));
  }

  @Test
  public void shouldNotSetPomNameAndDescriptionWhenModelIsNull() {
    mockComponent();
    when(mavenPath.getCoordinates()).thenReturn(coordinates);
    when(fluentComponent.attributes(NAME)).thenReturn(aNestedAttributesMap());

    MavenAttributesHelper.setMavenAttributes(componentStore, fluentComponent, coordinates, Optional.of(model), 1);

    verify(fluentComponent).attributes(eq(OVERLAY), eq(NAME), attributesValueCaptor.capture());
    Map<String, String> map = attributesValueCaptor.getValue();
    assertFalse(map.containsKey(P_POM_NAME));
    assertFalse(map.containsKey(P_POM_DESCRIPTION));
  }

  @Test
  public void packagingShouldBeJarWhenModelIsNull() {
    mockComponent();
    when(mavenPath.getCoordinates()).thenReturn(coordinates);
    when(fluentComponent.attributes(NAME)).thenReturn(aNestedAttributesMap());

    MavenAttributesHelper.setMavenAttributes(componentStore, fluentComponent, coordinates, Optional.of(model), 1);

    verify(fluentComponent).attributes(eq(OVERLAY), eq(NAME), attributesValueCaptor.capture());
    Map<String, String> map = attributesValueCaptor.getValue();
    assertThat(map, hasEntry(P_PACKAGING, "jar"));
  }

  private void assertGroupArtifactVersionSet(final int mapSize, final Map<String, String> map) {
    assertThat(map.size(), is(mapSize));
    assertThat(map, hasEntry(P_GROUP_ID, coordinates.getGroupId()));
    assertThat(map, hasEntry(P_ARTIFACT_ID, coordinates.getArtifactId()));
    assertThat(map, hasEntry(P_VERSION, coordinates.getVersion()));
    assertThat(map, hasEntry(P_BASE_VERSION, coordinates.getBaseVersion()));
  }

  private void mockModel() {
    when(model.getPackaging()).thenReturn(PACKAGING);
    when(model.getName()).thenReturn(POM_NAME);
    when(model.getDescription()).thenReturn(POM_DESCRIPTION);
  }

  private NestedAttributesMap aNestedAttributesMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(P_GROUP_ID, coordinates.getGroupId());
    map.put(P_ARTIFACT_ID, coordinates.getArtifactId());
    map.put(P_VERSION, coordinates.getVersion());
    map.put(P_BASE_VERSION, coordinates.getBaseVersion());
    return new NestedAttributesMap(NAME, map);
  }

  private void mockComponent() {
    when(fluentComponent.namespace()).thenReturn(coordinates.getGroupId());
    when(fluentComponent.name()).thenReturn(coordinates.getArtifactId());
    when(fluentComponent.version()).thenReturn(coordinates.getVersion());
  }
}
