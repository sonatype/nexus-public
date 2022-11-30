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
package org.sonatype.nexus.repository.content.search.table;

import java.util.Arrays;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.sql.ComponentSearchField;

import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.sonatype.nexus.repository.content.search.table.SqlSearchSortUtil.JSON_PATH_FORMAT;
import static org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings.GROUP_RAW;
import static org.sonatype.nexus.repository.rest.sql.ComponentSearchField.FORMAT_FIELD_1;
import static org.sonatype.nexus.repository.rest.sql.ComponentSearchField.FORMAT_FIELD_2;
import static org.sonatype.nexus.repository.rest.sql.ComponentSearchField.NAMESPACE;
import static org.sonatype.nexus.repository.search.index.SearchConstants.GROUP;

public class SqlSearchSortUtilTest
    extends TestSupport
{
  private SqlSearchSortUtil underTest;

  @Before
  public void setup() {
    SearchMappings mappings = searchMappings();
    underTest = new SqlSearchSortUtil(singletonList(mappings));
  }

  @Test
  public void shouldBeEmpty() {
    assertThat(underTest.getSortExpression(null), is(empty()));
    assertThat(underTest.getSortExpression(""), is(empty()));
    assertThat(underTest.getSortExpression("unknownAttribute"), is(empty()));
  }

  @Test
  public void shouldBeColumnName() {
    assertThat(underTest.getSortExpression(GROUP), is(of(NAMESPACE.getSortColumnName())));
  }

  @Test
  public void shouldBeJsonKey() {
    assertThat(underTest.getSortExpression("assets.attributes.maven2.extension"),
        is(of(String.format(JSON_PATH_FORMAT, "attributes", "maven2,extension"))));

    assertThat(underTest.getSortExpression("attributes.nuget.id"),
        is(of(String.format(JSON_PATH_FORMAT, "attributes", "nuget,id"))));

    assertThat(underTest.getSortExpression("assets.attributes.docker.content_digest"),
        is(of(String.format(JSON_PATH_FORMAT, "attributes", "docker,content_digest"))));

    assertThat(underTest.getSortExpression("attributes.docker.layerAncestry"),
        is(of(String.format(JSON_PATH_FORMAT, "attributes", "docker,layerAncestry"))));
  }

  private SearchMappings searchMappings() {
    return () -> Arrays.asList(
        new SearchMapping("group", GROUP_RAW, "Component group", NAMESPACE),
        new SearchMapping("maven.extension", "assets.attributes.maven2.extension",
            "Maven extension of component's asset", FORMAT_FIELD_2),
        new SearchMapping("nuget.id", "attributes.nuget.id",
            "NuGet id", FORMAT_FIELD_1),
        new SearchMapping("docker.contentDigest", "assets.attributes.docker.content_digest",
            "Docker content digest", FORMAT_FIELD_2),
        new SearchMapping("docker.layerId", "attributes.docker.layerAncestry", "Docker layer ID",
            ComponentSearchField.FORMAT_FIELD_1)
    );
  }
}
