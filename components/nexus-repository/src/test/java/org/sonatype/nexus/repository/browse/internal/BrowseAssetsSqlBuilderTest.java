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
package org.sonatype.nexus.repository.browse.internal;

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.browse.QueryOptions;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class BrowseAssetsSqlBuilderTest
    extends TestSupport
{
  static final String CONTENT_AUTH_WHERE = "contentAuth(@this, :browsedRepository) == true";

  static final String FILTER_WHERE = "name LIKE :nameFilter";

  static final String LAST_ID_WHERE = "@rid > :rid";

  @Mock
  QueryOptions queryOptions;

  @Mock
  AssetEntityAdapter assetEntityAdapter;

  BrowseAssetsSqlBuilder underTest;

  @Before
  public void setup() throws Exception {
    underTest = new BrowseAssetsSqlBuilder(assetEntityAdapter);
    when(assetEntityAdapter.getTypeName()).thenReturn("asset");
    when(queryOptions.getContentAuth()).thenReturn(true);
  }

  @Test
  public void whereWithAuth() throws Exception {
    String whereClause = underTest.buildWhereClause(emptyList(), queryOptions);
    assertThat(whereClause, is(equalTo(CONTENT_AUTH_WHERE)));
  }

  @Test
  public void whereWithFilter() throws Exception {
    when(queryOptions.getFilter()).thenReturn("filter");
    String whereClause = underTest.buildWhereClause(emptyList(), queryOptions);
    assertThat(whereClause, is(equalTo(CONTENT_AUTH_WHERE + " AND " + FILTER_WHERE)));
  }

  @Test
  public void whereWithLastId() {
    when(queryOptions.getLastId()).thenReturn("#45:1");
    String whereClause = underTest.buildWhereClause(emptyList(), queryOptions);
    assertThat(whereClause, is(equalTo(CONTENT_AUTH_WHERE + " AND " + LAST_ID_WHERE)));
    Map<String, Object> params = underTest.buildSqlParams("repository", queryOptions);
    assertThat(params.get("browsedRepository"), is("repository"));
    assertThat(params.get("rid"), is("#45:1"));
  }

  @Test
  public void whereWithNoContentAuth() {
    when(queryOptions.getContentAuth()).thenReturn(false);
    assertThat(underTest.buildWhereClause(emptyList(), queryOptions), is(nullValue()));
  }

  @Test
  public void whereWithNoConentAuthAndFilter() throws Exception {
    when(queryOptions.getContentAuth()).thenReturn(false);
    when(queryOptions.getFilter()).thenReturn("filter");
    assertThat(underTest.buildWhereClause(emptyList(), queryOptions), is(equalTo(FILTER_WHERE)));
  }

  @Test
  public void whereWithNoConentAuthAndFilterAndLastId() throws Exception {
    when(queryOptions.getContentAuth()).thenReturn(false);
    when(queryOptions.getFilter()).thenReturn("filter");
    when(queryOptions.getLastId()).thenReturn("#45:1");
    assertThat(underTest.buildWhereClause(emptyList(), queryOptions), is(equalTo(FILTER_WHERE + " AND " + LAST_ID_WHERE)));
    Map<String, Object> params = underTest.buildSqlParams("repository", queryOptions);
    assertThat(params.get("browsedRepository"), is(nullValue()));
    assertThat(params.get("rid"), is("#45:1"));
  }
}
