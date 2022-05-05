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
package org.sonatype.nexus.repository.maven.internal.orient;

import java.util.Arrays;
import java.util.stream.StreamSupport;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.maven.OrientMavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.MavenPath.SignatureType;
import org.sonatype.nexus.repository.maven.MavenPathParser;

import com.orientechnologies.orient.core.record.impl.ODocument;
import org.apache.maven.index.reader.Record;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class OrientMavenIndexPublisherTest
    extends TestSupport
{
  @Mock
  private OrientMavenFacet mavenFacet;

  @Mock
  private MavenPathParser mavenPathParser;

  private final String PATH1 = "clover/g/a/01/clover-1.0.0.jar";

  private final String PATH2 = "aopalliance/1.0.0/aopalliance-1.0.0.jar";

  private final String RIGHT_ARTIFACT = "right";

  private OrientMavenIndexPublisher orientMavenIndexPublisher = new OrientMavenIndexPublisher();

  private Coordinates coordinates;

  private MavenPath mavenPath2;

  @Before
  public void setUp() {
    coordinates = mock(Coordinates.class);
    mavenPath2 = spy(new MavenPath(PATH2, coordinates));
  }

  @Test
  public void testFilterAndConvertToRecords() {
    MavenPath mavenPath1 = new MavenPath(PATH1, null);
    doReturn(mavenPath2).when(mavenPath2).locate(any(), any());
    doReturn(mavenPath2).when(mavenPath2).signature(SignatureType.GPG);
    when(mavenFacet.getMavenPathParser()).thenReturn(mavenPathParser);
    when(mavenPathParser.parsePath(any())).thenReturn(mavenPath1).thenReturn(mavenPath2);

    Iterable<Record> records = orientMavenIndexPublisher.filterAndConvertToRecords(createAssets(), mavenFacet);

    assertThat(records, is(notNullValue()));
    assertThat(StreamSupport.stream(records.spliterator(), false).count(), is(1L));
    assertThat(records.iterator().next().get(Record.ARTIFACT_ID), is(RIGHT_ARTIFACT));
  }

  private Iterable<ODocument> createAssets() {
    return Arrays.asList(asset("wrong"), asset(RIGHT_ARTIFACT));
  }

  private ODocument asset(final String artifactId) {
    ODocument asset = new ODocument(new OClassNameBuilder().type("asset").build());
    asset.field("artifactId", artifactId);
    return asset;
  }
}
