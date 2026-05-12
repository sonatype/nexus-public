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
package org.sonatype.nexus.repository.search.sql.query.security;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings;
import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.sql.SearchMappingService;
import org.sonatype.nexus.repository.search.sql.query.DatabaseTypeDetector;
import org.sonatype.nexus.repository.search.sql.query.syntax.ExactTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;
import org.sonatype.nexus.repository.search.sql.query.syntax.LenientTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Operand;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlClause;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlPredicate;
import org.sonatype.nexus.repository.search.sql.query.syntax.TermCollection;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SearchContentSelectorSqlFilterGeneratorTest
{
  public static final String REPOSITORY_CONDITION_FORMAT = "repositoryConditionFormat";

  public static final String REPOSITORY_NAME_PARAM = "repositoryNameParam";

  public static final String REPOSITORY_NAME_VALUE = "repositoryNameValue";

  @Mock
  private SearchMappingService service;

  @Mock
  private SelectorConfiguration configuration1;

  @Mock
  private SelectorConfiguration configuration2;

  @Mock
  private SelectorManager selectorManager;

  @Mock
  private DatabaseTypeDetector databaseTypeDetector;

  private SearchContentSelectorSqlFilterGenerator underTest;

  @Before
  public void setup() {
    when(databaseTypeDetector.isH2()).thenReturn(false);
    underTest = new SearchContentSelectorSqlFilterGenerator(selectorManager,
        new SearchMappingService(Arrays.asList(new DefaultSearchMappings())), databaseTypeDetector);
  }

  @Test
  public void shouldCreateContentSelectorSqlFilter() throws Exception {
    Set<String> repositories = ImmutableSet.of("repo1", "repo2");
    mockSelectorConfiguration();

    Optional<Expression> filter = underTest.createFilter(configuration1, repositories);

    assertTrue(filter.isPresent());

    Expression condition = filter.get();

    assertThat(condition,
        is(SqlClause.create(Operand.AND, expectedFilterExpression(), expectedRepositoryExpression())));
  }

  private Expression expectedFilterExpression() {
    return new SqlPredicate(Operand.EQ, SearchField.PATHS, new LenientTerm("value"));
  }

  private Expression expectedRepositoryExpression() {
    return SqlClause.create(Operand.AND, new SqlPredicate(Operand.IN, SearchField.REPOSITORY_NAME,
        TermCollection.create(new ExactTerm("repo1"), new ExactTerm("repo2"))));
  }

  private void mockSelectorConfiguration() throws Exception {
    when(configuration1.getType()).thenReturn(CselSelector.TYPE);
    when(configuration2.getType()).thenReturn(CselSelector.TYPE);

    doAnswer(invocationOnMock -> {
      Object[] arguments = invocationOnMock.getArguments();
      SelectorExpressionBuilder selectorSqlBuilder = (SelectorExpressionBuilder) arguments[1];
      selectorSqlBuilder.appendField("path").appendOperand(Operand.EQ).appendTerm(new LenientTerm("value"));
      return invocationOnMock;
    }).when(selectorManager).toSql(any(), any(), any(CselToExpression.class));
  }
}
