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
package org.sonatype.nexus.repository.maven.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.selector.VariableSource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class MavenVariableResolverAdapterTest
    extends TestSupport
{
  @Mock
  Request request;

  @Mock
  Repository repository;

  MavenVariableResolverAdapter mavenVariableResolverAdapter = new MavenVariableResolverAdapter(
      new Maven2MavenPathParser());

  @Before
  public void setup() throws Exception {
    when(repository.getName()).thenReturn("MavenVariableResolverAdapterTest");
    when(repository.getFormat()).thenReturn(new Format(Maven2Format.NAME) { });
  }

  @Test
  public void testFromRequest() throws Exception {
    when(request.getPath()).thenReturn("/mygroupid/myartifactid/1.0/myartifactid-1.0.jar");

    VariableSource source = mavenVariableResolverAdapter.fromRequest(request, repository);

    assertThat(source.getVariableSet(),
        containsInAnyOrder("format", "path", "coordinate.groupId", "coordinate.artifactId", "coordinate.version",
            "coordinate.extension", "coordinate.classifier"));
    assertThat(source.get("format").get(), is(Maven2Format.NAME));
    assertThat(source.get("path").get(), is("/mygroupid/myartifactid/1.0/myartifactid-1.0.jar"));
    assertThat(source.get("coordinate.groupId").get(), is("mygroupid"));
    assertThat(source.get("coordinate.artifactId").get(), is("myartifactid"));
    assertThat(source.get("coordinate.version").get(), is("1.0"));
    assertThat(source.get("coordinate.extension").get(), is("jar"));
    assertThat(source.get("coordinate.classifier").get(), is(""));
  }

  @Test
  public void testFromRequest_snapshot() throws Exception {
    when(request.getPath()).thenReturn("/mygroupid/myartifactid/1.0-SNAPSHOT/myartifactid-1.0-20160414.160310-3.jar");

    VariableSource source = mavenVariableResolverAdapter.fromRequest(request, repository);

    assertThat(source.getVariableSet(),
        containsInAnyOrder("format", "path", "coordinate.groupId", "coordinate.artifactId", "coordinate.version",
            "coordinate.extension", "coordinate.classifier"));
    assertThat(source.get("format").get(), is(Maven2Format.NAME));
    assertThat(source.get("path").get(),
        is("/mygroupid/myartifactid/1.0-SNAPSHOT/myartifactid-1.0-20160414.160310-3.jar"));
    assertThat(source.get("coordinate.groupId").get(), is("mygroupid"));
    assertThat(source.get("coordinate.artifactId").get(), is("myartifactid"));
    assertThat(source.get("coordinate.version").get(), is("1.0-SNAPSHOT"));
    assertThat(source.get("coordinate.extension").get(), is("jar"));
    assertThat(source.get("coordinate.classifier").get(), is(""));
  }

  @Test
  public void testFromRequest_withClassifier() throws Exception {
    when(request.getPath()).thenReturn("/mygroupid/myartifactid/1.0/myartifactid-1.0-sources.jar");

    VariableSource source = mavenVariableResolverAdapter.fromRequest(request, repository);

    assertThat(source.getVariableSet(),
        containsInAnyOrder("format", "path", "coordinate.groupId", "coordinate.artifactId", "coordinate.version",
            "coordinate.extension", "coordinate.classifier"));
    assertThat(source.get("format").get(), is(Maven2Format.NAME));
    assertThat(source.get("path").get(), is("/mygroupid/myartifactid/1.0/myartifactid-1.0-sources.jar"));
    assertThat(source.get("coordinate.groupId").get(), is("mygroupid"));
    assertThat(source.get("coordinate.artifactId").get(), is("myartifactid"));
    assertThat(source.get("coordinate.version").get(), is("1.0"));
    assertThat(source.get("coordinate.extension").get(), is("jar"));
    assertThat(source.get("coordinate.classifier").get(), is("sources"));
  }
}
