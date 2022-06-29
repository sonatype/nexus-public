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

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.search.table.SearchTableData;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.when;

public class MavenSearchCustomFieldContributorTest
{
  private NestedAttributesMap attributes;

  private Asset asset;

  private NestedAttributesMap childAttributes;

  private MavenSearchCustomFieldContributor underTest;

  @Before
  public void setup() {
    underTest = new MavenSearchCustomFieldContributor();

    asset = Mockito.mock(Asset.class);

    attributes = new NestedAttributesMap();
    childAttributes = attributes.child(Maven2Format.NAME);
  }

  @Test
  public void customSearchFieldsAreSetCorrectly() {
    SearchTableData searchTableData = new SearchTableData();

    childAttributes.set("baseVersion", "1.1.1");
    childAttributes.set("extension", "testExtension");
    childAttributes.set("classifier", "testClassifier");
    when(asset.attributes()).thenReturn(attributes);

    underTest.populateSearchCustomFields(searchTableData, asset);
    assertThat(searchTableData.getFormatField1(), is("1.1.1"));
    assertThat(searchTableData.getFormatField2(), is("testExtension"));
    assertThat(searchTableData.getFormatField3(), is("testClassifier"));
  }

  @Test
  public void operationIsSuccessfulWithNoCustomSearchData() {
    SearchTableData searchTableData = new SearchTableData();

    when(asset.attributes()).thenReturn(attributes);

    underTest.populateSearchCustomFields(searchTableData, asset);

    assertThat(searchTableData.getFormatField1(), is(nullValue()));
    assertThat(searchTableData.getFormatField2(), is(nullValue()));
    assertThat(searchTableData.getFormatField3(), is(nullValue()));
  }
}
