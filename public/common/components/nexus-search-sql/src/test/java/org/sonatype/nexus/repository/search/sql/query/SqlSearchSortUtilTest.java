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
package org.sonatype.nexus.repository.search.sql.query;

import java.util.Arrays;

import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.sql.query.h2.H2SearchDB;

import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings.GROUP_RAW;
import static org.sonatype.nexus.repository.search.index.SearchConstants.GROUP;

public class SqlSearchSortUtilTest
{
  private SqlSearchSortUtil underTest;

  @Before
  public void setup() {
    SearchMappings mappings = searchMappings();
    underTest = new SqlSearchSortUtil(new H2SearchDB(), singletonList(mappings));
  }

  @Test
  public void shouldBeEmpty() {
    assertThat(underTest.getSortExpression(null), is(empty()));
    assertThat(underTest.getSortExpression(""), is(empty()));
    assertThat(underTest.getSortExpression("unknownAttribute"), is(empty()));
  }

  @Test
  public void shouldBeColumnName() {
    assertThat(underTest.getSortExpression(GROUP), is(of("cs.namespace")));
  }

  @Test
  public void shouldNotBeJsonKeyForH2() {
    // H2 uses format_field_values columns instead of attributes JSON column
    assertThat(underTest.getSortExpression("assets.attributes.maven2.extension"),
        is(of("cs.format_field_values_2")));

    assertThat(underTest.getSortExpression("attributes.nuget.id"),
        is(of("cs.format_field_values_1")));

    assertThat(underTest.getSortExpression("assets.attributes.docker.content_digest"),
        is(of("cs.format_field_values_2")));

    assertThat(underTest.getSortExpression("attributes.docker.layerAncestry"),
        is(of("cs.format_field_values_1")));
  }

  @Test
  public void shouldMapLastUpdatedToLastModifiedColumn() {
    assertThat(underTest.getSortExpression("last_updated"), is(of("cs.last_modified")));
  }

  @Test
  public void shouldMapLastBlobUpdatedToLastModifiedColumn() {
    assertThat(underTest.getSortExpression("lastBlobUpdated"), is(of("cs.last_modified")));
  }

  private SearchMappings searchMappings() {
    return () -> Arrays.asList(new SearchMapping("group", GROUP_RAW, "Component group", SearchField.NAMESPACE),
        new SearchMapping("last_updated", "last_modified", "Asset last modified time", SearchField.LAST_MODIFIED),
        new SearchMapping("lastBlobUpdated", "last_modified", "Asset last modified time (UI)",
            SearchField.LAST_MODIFIED),
        new SearchMapping("maven.extension", "assets.attributes.maven2.extension",
            "Maven extension of component's asset", SearchField.FORMAT_FIELD_2),
        new SearchMapping("nuget.id", "attributes.nuget.id", "NuGet id", SearchField.FORMAT_FIELD_1),
        new SearchMapping("docker.contentDigest", "assets.attributes.docker.content_digest", "Docker content digest",
            SearchField.FORMAT_FIELD_2),
        new SearchMapping("docker.layerId", "attributes.docker.layerAncestry", "Docker layer ID",
            SearchField.FORMAT_FIELD_1));
  }
}
