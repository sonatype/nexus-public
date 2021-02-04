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

import java.util.HashMap;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.security.RepositorySelector;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class PreviewAssetsSqlBuilderTest
    extends TestSupport
{
  static final String CONTENT_EXPRESSION = "contentExpression(@this, :jexlExpression, :repositorySelector, " +
      ":repoToContainedGroupMap) == true";

  static final String FILTER_EXPRESSION = "name LIKE :nameFilter";

  @Mock
  RepositorySelector repositorySelector;

  @Mock
  QueryOptions queryOptions;

  @Mock
  Repository repository;

  PreviewAssetsSqlBuilder underTest;

  @Before
  public void setup() throws Exception {
    underTest = new PreviewAssetsSqlBuilder(repositorySelector,
        "",
        queryOptions,
        new HashMap<>());
  }

  @Test
  public void whereWithContentExpression() throws Exception {
    String whereClause = underTest.buildWhereClause();
    assertThat(whereClause, is(equalTo(CONTENT_EXPRESSION)));
  }

  @Test
  public void whereWithFilter() throws Exception {
    when(queryOptions.getFilter()).thenReturn("filter");
    String whereClause = underTest.buildWhereClause();
    assertThat(whereClause, is(equalTo(CONTENT_EXPRESSION + " AND " + FILTER_EXPRESSION)));
  }
}
