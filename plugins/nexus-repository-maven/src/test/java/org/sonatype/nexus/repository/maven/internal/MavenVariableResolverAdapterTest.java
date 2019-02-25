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

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.selector.VariableSource;

import com.orientechnologies.orient.core.record.impl.ODocument;
import org.elasticsearch.search.lookup.SourceLookup;
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
  private static final String FORMAT_VARIABLE = "format";

  private static final String PATH_VARIABLE = "path";

  private static final String COORDINATE_GROUP_ID_VARIABLE = "coordinate.groupId";

  private static final String COORDINATE_ARTIFACT_ID_VARIABLE = "coordinate.artifactId";

  private static final String COORDINATE_VERSION_VARIABLE = "coordinate.version";

  private static final String COORDINATE_EXTENSION_VARIABLE = "coordinate.extension";

  private static final String COORDINATE_CLASSIFIER_VARIABLE = "coordinate.classifier";

  @Mock
  Request request;

  @Mock
  ODocument document;

  @Mock
  Asset asset;

  @Mock
  SourceLookup sourceLookup;

  @Mock
  Map<String, Object> sourceLookupAsset;

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
        containsInAnyOrder(FORMAT_VARIABLE, PATH_VARIABLE, COORDINATE_GROUP_ID_VARIABLE,
            COORDINATE_ARTIFACT_ID_VARIABLE, COORDINATE_VERSION_VARIABLE, COORDINATE_EXTENSION_VARIABLE,
            COORDINATE_CLASSIFIER_VARIABLE));
    assertThat(source.get(FORMAT_VARIABLE).get(), is(Maven2Format.NAME));
    assertThat(source.get(PATH_VARIABLE).get(), is("mygroupid/myartifactid/1.0/myartifactid-1.0.jar"));
    assertThat(source.get(COORDINATE_GROUP_ID_VARIABLE).get(), is("mygroupid"));
    assertThat(source.get(COORDINATE_ARTIFACT_ID_VARIABLE).get(), is("myartifactid"));
    assertThat(source.get(COORDINATE_VERSION_VARIABLE).get(), is("1.0"));
    assertThat(source.get(COORDINATE_EXTENSION_VARIABLE).get(), is("jar"));
    assertThat(source.get(COORDINATE_CLASSIFIER_VARIABLE).get(), is(""));
  }

  @Test
  public void testFromRequest_snapshot() throws Exception {
    when(request.getPath()).thenReturn("/mygroupid/myartifactid/1.0-SNAPSHOT/myartifactid-1.0-20160414.160310-3.jar");

    VariableSource source = mavenVariableResolverAdapter.fromRequest(request, repository);

    assertThat(source.getVariableSet(),
        containsInAnyOrder(FORMAT_VARIABLE, PATH_VARIABLE, COORDINATE_GROUP_ID_VARIABLE,
            COORDINATE_ARTIFACT_ID_VARIABLE, COORDINATE_VERSION_VARIABLE, COORDINATE_EXTENSION_VARIABLE,
            COORDINATE_CLASSIFIER_VARIABLE));
    assertThat(source.get(FORMAT_VARIABLE).get(), is(Maven2Format.NAME));
    assertThat(source.get(PATH_VARIABLE).get(),
        is("mygroupid/myartifactid/1.0-SNAPSHOT/myartifactid-1.0-20160414.160310-3.jar"));
    assertThat(source.get(COORDINATE_GROUP_ID_VARIABLE).get(), is("mygroupid"));
    assertThat(source.get(COORDINATE_ARTIFACT_ID_VARIABLE).get(), is("myartifactid"));
    assertThat(source.get(COORDINATE_VERSION_VARIABLE).get(), is("1.0-SNAPSHOT"));
    assertThat(source.get(COORDINATE_EXTENSION_VARIABLE).get(), is("jar"));
    assertThat(source.get(COORDINATE_CLASSIFIER_VARIABLE).get(), is(""));
  }

  @Test
  public void testFromRequest_withClassifier() throws Exception {
    when(request.getPath()).thenReturn("/mygroupid/myartifactid/1.0/myartifactid-1.0-sources.jar");

    VariableSource source = mavenVariableResolverAdapter.fromRequest(request, repository);

    assertThat(source.getVariableSet(),
        containsInAnyOrder(FORMAT_VARIABLE, PATH_VARIABLE, COORDINATE_GROUP_ID_VARIABLE,
            COORDINATE_ARTIFACT_ID_VARIABLE, COORDINATE_VERSION_VARIABLE, COORDINATE_EXTENSION_VARIABLE,
            COORDINATE_CLASSIFIER_VARIABLE));
    assertThat(source.get(FORMAT_VARIABLE).get(), is(Maven2Format.NAME));
    assertThat(source.get(PATH_VARIABLE).get(), is("mygroupid/myartifactid/1.0/myartifactid-1.0-sources.jar"));
    assertThat(source.get(COORDINATE_GROUP_ID_VARIABLE).get(), is("mygroupid"));
    assertThat(source.get(COORDINATE_ARTIFACT_ID_VARIABLE).get(), is("myartifactid"));
    assertThat(source.get(COORDINATE_VERSION_VARIABLE).get(), is("1.0"));
    assertThat(source.get(COORDINATE_EXTENSION_VARIABLE).get(), is("jar"));
    assertThat(source.get(COORDINATE_CLASSIFIER_VARIABLE).get(), is("sources"));
  }

  @Test
  public void testFromDocument() throws Exception {
    when(document.field("name", String.class)).thenReturn("mygroupid/myartifactid/1.0/myartifactid-1.0.jar");
    when(document.field(FORMAT_VARIABLE, String.class)).thenReturn(Maven2Format.NAME);

    VariableSource source = mavenVariableResolverAdapter.fromDocument(document);

    assertThat(source.getVariableSet(),
        containsInAnyOrder(FORMAT_VARIABLE, PATH_VARIABLE, COORDINATE_GROUP_ID_VARIABLE,
            COORDINATE_ARTIFACT_ID_VARIABLE, COORDINATE_VERSION_VARIABLE, COORDINATE_EXTENSION_VARIABLE,
            COORDINATE_CLASSIFIER_VARIABLE));
    assertThat(source.get(FORMAT_VARIABLE).get(), is(Maven2Format.NAME));
    assertThat(source.get(PATH_VARIABLE).get(), is("mygroupid/myartifactid/1.0/myartifactid-1.0.jar"));
    assertThat(source.get(COORDINATE_GROUP_ID_VARIABLE).get(), is("mygroupid"));
    assertThat(source.get(COORDINATE_ARTIFACT_ID_VARIABLE).get(), is("myartifactid"));
    assertThat(source.get(COORDINATE_VERSION_VARIABLE).get(), is("1.0"));
    assertThat(source.get(COORDINATE_EXTENSION_VARIABLE).get(), is("jar"));
    assertThat(source.get(COORDINATE_CLASSIFIER_VARIABLE).get(), is(""));
  }

  @Test
  public void testFromDocument_snapshot() throws Exception {
    when(document.field("name", String.class))
        .thenReturn("mygroupid/myartifactid/1.0-SNAPSHOT/myartifactid-1.0-20160414.160310-3.jar");
    when(document.field(FORMAT_VARIABLE, String.class)).thenReturn(Maven2Format.NAME);

    VariableSource source = mavenVariableResolverAdapter.fromDocument(document);

    assertThat(source.getVariableSet(),
        containsInAnyOrder(FORMAT_VARIABLE, PATH_VARIABLE, COORDINATE_GROUP_ID_VARIABLE,
            COORDINATE_ARTIFACT_ID_VARIABLE, COORDINATE_VERSION_VARIABLE, COORDINATE_EXTENSION_VARIABLE,
            COORDINATE_CLASSIFIER_VARIABLE));
    assertThat(source.get(FORMAT_VARIABLE).get(), is(Maven2Format.NAME));
    assertThat(source.get(PATH_VARIABLE).get(),
        is("mygroupid/myartifactid/1.0-SNAPSHOT/myartifactid-1.0-20160414.160310-3.jar"));
    assertThat(source.get(COORDINATE_GROUP_ID_VARIABLE).get(), is("mygroupid"));
    assertThat(source.get(COORDINATE_ARTIFACT_ID_VARIABLE).get(), is("myartifactid"));
    assertThat(source.get(COORDINATE_VERSION_VARIABLE).get(), is("1.0-SNAPSHOT"));
    assertThat(source.get(COORDINATE_EXTENSION_VARIABLE).get(), is("jar"));
    assertThat(source.get(COORDINATE_CLASSIFIER_VARIABLE).get(), is(""));
  }

  @Test
  public void testFromDocument_withClassifier() throws Exception {
    when(document.field("name", String.class)).thenReturn("mygroupid/myartifactid/1.0/myartifactid-1.0-sources.jar");
    when(document.field(FORMAT_VARIABLE, String.class)).thenReturn(Maven2Format.NAME);

    VariableSource source = mavenVariableResolverAdapter.fromDocument(document);

    assertThat(source.getVariableSet(),
        containsInAnyOrder(FORMAT_VARIABLE, PATH_VARIABLE, COORDINATE_GROUP_ID_VARIABLE,
            COORDINATE_ARTIFACT_ID_VARIABLE, COORDINATE_VERSION_VARIABLE, COORDINATE_EXTENSION_VARIABLE,
            COORDINATE_CLASSIFIER_VARIABLE));
    assertThat(source.get(FORMAT_VARIABLE).get(), is(Maven2Format.NAME));
    assertThat(source.get(PATH_VARIABLE).get(), is("mygroupid/myartifactid/1.0/myartifactid-1.0-sources.jar"));
    assertThat(source.get(COORDINATE_GROUP_ID_VARIABLE).get(), is("mygroupid"));
    assertThat(source.get(COORDINATE_ARTIFACT_ID_VARIABLE).get(), is("myartifactid"));
    assertThat(source.get(COORDINATE_VERSION_VARIABLE).get(), is("1.0"));
    assertThat(source.get(COORDINATE_EXTENSION_VARIABLE).get(), is("jar"));
    assertThat(source.get(COORDINATE_CLASSIFIER_VARIABLE).get(), is("sources"));
  }

  @Test
  public void testFromAsset() throws Exception {
    when(asset.name()).thenReturn("mygroupid/myartifactid/1.0/myartifactid-1.0.jar");
    when(asset.format()).thenReturn(Maven2Format.NAME);

    VariableSource source = mavenVariableResolverAdapter.fromAsset(asset);

    assertThat(source.getVariableSet(),
        containsInAnyOrder(FORMAT_VARIABLE, PATH_VARIABLE, COORDINATE_GROUP_ID_VARIABLE,
            COORDINATE_ARTIFACT_ID_VARIABLE, COORDINATE_VERSION_VARIABLE, COORDINATE_EXTENSION_VARIABLE,
            COORDINATE_CLASSIFIER_VARIABLE));
    assertThat(source.get(FORMAT_VARIABLE).get(), is(Maven2Format.NAME));
    assertThat(source.get(PATH_VARIABLE).get(), is("mygroupid/myartifactid/1.0/myartifactid-1.0.jar"));
    assertThat(source.get(COORDINATE_GROUP_ID_VARIABLE).get(), is("mygroupid"));
    assertThat(source.get(COORDINATE_ARTIFACT_ID_VARIABLE).get(), is("myartifactid"));
    assertThat(source.get(COORDINATE_VERSION_VARIABLE).get(), is("1.0"));
    assertThat(source.get(COORDINATE_EXTENSION_VARIABLE).get(), is("jar"));
    assertThat(source.get(COORDINATE_CLASSIFIER_VARIABLE).get(), is(""));
  }

  @Test
  public void testFromAsset_snapshot() throws Exception {
    when(asset.name()).thenReturn("mygroupid/myartifactid/1.0-SNAPSHOT/myartifactid-1.0-20160414.160310-3.jar");
    when(asset.format()).thenReturn(Maven2Format.NAME);

    VariableSource source = mavenVariableResolverAdapter.fromAsset(asset);

    assertThat(source.getVariableSet(),
        containsInAnyOrder(FORMAT_VARIABLE, PATH_VARIABLE, COORDINATE_GROUP_ID_VARIABLE,
            COORDINATE_ARTIFACT_ID_VARIABLE, COORDINATE_VERSION_VARIABLE, COORDINATE_EXTENSION_VARIABLE,
            COORDINATE_CLASSIFIER_VARIABLE));
    assertThat(source.get(FORMAT_VARIABLE).get(), is(Maven2Format.NAME));
    assertThat(source.get(PATH_VARIABLE).get(),
        is("mygroupid/myartifactid/1.0-SNAPSHOT/myartifactid-1.0-20160414.160310-3.jar"));
    assertThat(source.get(COORDINATE_GROUP_ID_VARIABLE).get(), is("mygroupid"));
    assertThat(source.get(COORDINATE_ARTIFACT_ID_VARIABLE).get(), is("myartifactid"));
    assertThat(source.get(COORDINATE_VERSION_VARIABLE).get(), is("1.0-SNAPSHOT"));
    assertThat(source.get(COORDINATE_EXTENSION_VARIABLE).get(), is("jar"));
    assertThat(source.get(COORDINATE_CLASSIFIER_VARIABLE).get(), is(""));
  }

  @Test
  public void testFromAsset_withClassifier() throws Exception {
    when(asset.name()).thenReturn("mygroupid/myartifactid/1.0/myartifactid-1.0-sources.jar");
    when(asset.format()).thenReturn(Maven2Format.NAME);

    VariableSource source = mavenVariableResolverAdapter.fromAsset(asset);

    assertThat(source.getVariableSet(),
        containsInAnyOrder(FORMAT_VARIABLE, PATH_VARIABLE, COORDINATE_GROUP_ID_VARIABLE,
            COORDINATE_ARTIFACT_ID_VARIABLE, COORDINATE_VERSION_VARIABLE, COORDINATE_EXTENSION_VARIABLE,
            COORDINATE_CLASSIFIER_VARIABLE));
    assertThat(source.get(FORMAT_VARIABLE).get(), is(Maven2Format.NAME));
    assertThat(source.get(PATH_VARIABLE).get(), is("mygroupid/myartifactid/1.0/myartifactid-1.0-sources.jar"));
    assertThat(source.get(COORDINATE_GROUP_ID_VARIABLE).get(), is("mygroupid"));
    assertThat(source.get(COORDINATE_ARTIFACT_ID_VARIABLE).get(), is("myartifactid"));
    assertThat(source.get(COORDINATE_VERSION_VARIABLE).get(), is("1.0"));
    assertThat(source.get(COORDINATE_EXTENSION_VARIABLE).get(), is("jar"));
    assertThat(source.get(COORDINATE_CLASSIFIER_VARIABLE).get(), is("sources"));
  }

  @Test
  public void testFromSourceLookup() throws Exception {
    when(sourceLookup.get("format")).thenReturn("maven2");
    when(sourceLookupAsset.get("name")).thenReturn("mygroupid/myartifactid/1.0/myartifactid-1.0.jar");

    VariableSource source = mavenVariableResolverAdapter.fromSourceLookup(sourceLookup, sourceLookupAsset);

    assertThat(source.getVariableSet(),
        containsInAnyOrder(FORMAT_VARIABLE, PATH_VARIABLE, COORDINATE_GROUP_ID_VARIABLE,
            COORDINATE_ARTIFACT_ID_VARIABLE, COORDINATE_VERSION_VARIABLE, COORDINATE_EXTENSION_VARIABLE,
            COORDINATE_CLASSIFIER_VARIABLE));
    assertThat(source.get(FORMAT_VARIABLE).get(), is(Maven2Format.NAME));
    assertThat(source.get(PATH_VARIABLE).get(), is("mygroupid/myartifactid/1.0/myartifactid-1.0.jar"));
    assertThat(source.get(COORDINATE_GROUP_ID_VARIABLE).get(), is("mygroupid"));
    assertThat(source.get(COORDINATE_ARTIFACT_ID_VARIABLE).get(), is("myartifactid"));
    assertThat(source.get(COORDINATE_VERSION_VARIABLE).get(), is("1.0"));
    assertThat(source.get(COORDINATE_EXTENSION_VARIABLE).get(), is("jar"));
    assertThat(source.get(COORDINATE_CLASSIFIER_VARIABLE).get(), is(""));
  }

  @Test
  public void testFromSourceLookup_snapshot() throws Exception {
    when(sourceLookup.get("format")).thenReturn("maven2");
    when(sourceLookupAsset.get("name"))
        .thenReturn("mygroupid/myartifactid/1.0-SNAPSHOT/myartifactid-1.0-20160414.160310-3.jar");

    VariableSource source = mavenVariableResolverAdapter.fromSourceLookup(sourceLookup, sourceLookupAsset);

    assertThat(source.getVariableSet(),
        containsInAnyOrder(FORMAT_VARIABLE, PATH_VARIABLE, COORDINATE_GROUP_ID_VARIABLE,
            COORDINATE_ARTIFACT_ID_VARIABLE, COORDINATE_VERSION_VARIABLE, COORDINATE_EXTENSION_VARIABLE,
            COORDINATE_CLASSIFIER_VARIABLE));
    assertThat(source.get(FORMAT_VARIABLE).get(), is(Maven2Format.NAME));
    assertThat(source.get(PATH_VARIABLE).get(),
        is("mygroupid/myartifactid/1.0-SNAPSHOT/myartifactid-1.0-20160414.160310-3.jar"));
    assertThat(source.get(COORDINATE_GROUP_ID_VARIABLE).get(), is("mygroupid"));
    assertThat(source.get(COORDINATE_ARTIFACT_ID_VARIABLE).get(), is("myartifactid"));
    assertThat(source.get(COORDINATE_VERSION_VARIABLE).get(), is("1.0-SNAPSHOT"));
    assertThat(source.get(COORDINATE_EXTENSION_VARIABLE).get(), is("jar"));
    assertThat(source.get(COORDINATE_CLASSIFIER_VARIABLE).get(), is(""));
  }

  @Test
  public void testFromSourceLookup_withClassifier() throws Exception {
    when(sourceLookup.get("format")).thenReturn("maven2");
    when(sourceLookupAsset.get("name")).thenReturn("mygroupid/myartifactid/1.0/myartifactid-1.0-sources.jar");

    VariableSource source = mavenVariableResolverAdapter.fromSourceLookup(sourceLookup, sourceLookupAsset);

    assertThat(source.getVariableSet(),
        containsInAnyOrder(FORMAT_VARIABLE, PATH_VARIABLE, COORDINATE_GROUP_ID_VARIABLE,
            COORDINATE_ARTIFACT_ID_VARIABLE, COORDINATE_VERSION_VARIABLE, COORDINATE_EXTENSION_VARIABLE,
            COORDINATE_CLASSIFIER_VARIABLE));
    assertThat(source.get(FORMAT_VARIABLE).get(), is(Maven2Format.NAME));
    assertThat(source.get(PATH_VARIABLE).get(), is("mygroupid/myartifactid/1.0/myartifactid-1.0-sources.jar"));
    assertThat(source.get(COORDINATE_GROUP_ID_VARIABLE).get(), is("mygroupid"));
    assertThat(source.get(COORDINATE_ARTIFACT_ID_VARIABLE).get(), is("myartifactid"));
    assertThat(source.get(COORDINATE_VERSION_VARIABLE).get(), is("1.0"));
    assertThat(source.get(COORDINATE_EXTENSION_VARIABLE).get(), is("jar"));
    assertThat(source.get(COORDINATE_CLASSIFIER_VARIABLE).get(), is("sources"));
  }
}
