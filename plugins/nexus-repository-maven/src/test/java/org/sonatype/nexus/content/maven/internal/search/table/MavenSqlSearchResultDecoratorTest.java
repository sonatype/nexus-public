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
package org.sonatype.nexus.content.maven.internal.search.table;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.search.sql.SearchResult;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;

public class MavenSqlSearchResultDecoratorTest
    extends TestSupport
{
  private NestedAttributesMap attributes;

  private NestedAttributesMap childAttributes;

  private MavenSqlSearchResultDecorator underTest;

  @Before
  public void setup() {
    underTest = new MavenSqlSearchResultDecorator();

    attributes = new NestedAttributesMap();
    childAttributes = attributes.child(Maven2Format.NAME);
  }

  @Test
  public void shouldAddBaseVersionAnnotationWhenFormatIsMaven() {
    String baseVersion = "1.1.1";
    ComponentSearchResult component = aComponentSearchResult(Maven2Format.NAME);
    SearchResult searchResult = aSearchResult();
    childAttributes.set(P_BASE_VERSION, baseVersion);

    underTest.updateComponent(component, searchResult);

    assertThat(component.getAnnotation(P_BASE_VERSION), is(baseVersion));
  }

  @Test
  public void shouldNotAddBaseVersionAnnotationWhenFormatIsNotMaven() {
    ComponentSearchResult component = aComponentSearchResult("raw");
    SearchResult searchResult = aSearchResult();

    underTest.updateComponent(component, searchResult);

    assertThat(component.getAnnotation(P_BASE_VERSION), is(nullValue()));
  }

  private SearchResult aSearchResult() {
    SearchResult searchResult = mock(SearchResult.class);
    when(searchResult.attributes()).thenReturn(attributes);
    return searchResult;
  }

  private ComponentSearchResult aComponentSearchResult(final String format) {
    ComponentSearchResult component = new ComponentSearchResult();
    component.setFormat(format);
    return component;
  }
}
