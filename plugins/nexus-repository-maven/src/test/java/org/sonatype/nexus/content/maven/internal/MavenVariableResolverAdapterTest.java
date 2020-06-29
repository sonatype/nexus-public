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
package org.sonatype.nexus.content.maven.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.selector.PropertiesResolver;
import org.sonatype.nexus.selector.VariableResolver;
import org.sonatype.nexus.selector.VariableSourceBuilder;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.maven.MavenPath.SignatureType.GPG;

public class MavenVariableResolverAdapterTest
    extends TestSupport
{
  private static final String ARTIFACT_PATH = "group/artifact/version/artifact-version.jar";

  @Mock
  private MavenPathParser mavenPathParser;

  @Mock
  private VariableSourceBuilder builder;

  @Mock
  private Request request;

  @Mock
  private MavenPath mavenPath;

  @Captor
  private ArgumentCaptor<PropertiesResolver<String>> propertiesResolverCaptor;

  private final ImmutableMap<String, Object> asset = of("name", ARTIFACT_PATH);

  @InjectMocks
  private MavenVariableResolverAdapter mavenVariableResolverAdapter;

  @Before
  public void setup() {
    Coordinates coordinates = new Coordinates(false, "org.mockito", "mockito-core",
        "3.24", 3600L, 100, "3.24", "test", ".jar", GPG);
    mockPathParsing(coordinates);
  }

  @Test
  public void addFromRequestShouldAddCoordinatesToVariableSourceBuilder() {
    mavenVariableResolverAdapter.addFromRequest(builder, request);

    verifyCoordinatesSet();
  }

  @Test
  public void addFromSourceLookupShouldAddCoordinatesToVariableSourceBuilder() {
    mavenVariableResolverAdapter.addFromSourceLookup(builder, null, asset);

    verifyCoordinatesSet();
  }

  @Test
  public void addFromRequestShouldNotAddNullCoordinates() {
    when(mavenPath.getCoordinates()).thenReturn(null);

    mavenVariableResolverAdapter.addFromRequest(builder, request);

    verify(builder, never()).addResolver(any(VariableResolver.class));
  }

  @Test
  public void addFromSourceLookupShouldNotAddNullCoordinates() {
    when(mavenPath.getCoordinates()).thenReturn(null);

    mavenVariableResolverAdapter.addFromSourceLookup(builder, null, asset);

    verify(builder, never()).addResolver(any(VariableResolver.class));
  }

  private void mockPathParsing(final Coordinates coordinates) {
    when(request.getPath()).thenReturn(ARTIFACT_PATH);
    when(mavenPathParser.parsePath(ARTIFACT_PATH)).thenReturn(mavenPath);
    when(mavenPath.getCoordinates()).thenReturn(coordinates);
  }

  private void verifyCoordinatesSet() {
    verify(builder).addResolver(propertiesResolverCaptor.capture());
    PropertiesResolver<String> propertiesResolver = propertiesResolverCaptor.getValue();

    assertTrue(propertiesResolver.resolve("coordinate.groupId").isPresent());
    assertThat(propertiesResolver.resolve("coordinate.groupId").get(), is("org.mockito"));

    assertTrue(propertiesResolver.resolve("coordinate.artifactId").isPresent());
    assertThat(propertiesResolver.resolve("coordinate.artifactId").get(), is("mockito-core"));

    assertTrue(propertiesResolver.resolve("coordinate.version").isPresent());
    assertThat(propertiesResolver.resolve("coordinate.version").get(), is("3.24"));

    assertTrue(propertiesResolver.resolve("coordinate.classifier").isPresent());
    assertThat(propertiesResolver.resolve("coordinate.classifier").get(), is("test"));

    assertTrue(propertiesResolver.resolve("coordinate.extension").isPresent());
    assertThat(propertiesResolver.resolve("coordinate.extension").get(), is(".jar"));
  }
}
