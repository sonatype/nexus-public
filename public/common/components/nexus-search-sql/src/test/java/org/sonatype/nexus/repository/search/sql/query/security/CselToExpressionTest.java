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

import java.util.Optional;

import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.sql.SearchMappingService;
import org.sonatype.nexus.repository.search.sql.query.syntax.ExactTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.NullTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Operand;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlClause;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlPredicate;
import org.sonatype.nexus.repository.search.sql.query.syntax.Term;
import org.sonatype.nexus.repository.search.sql.query.syntax.WildcardTerm;
import org.sonatype.nexus.selector.JexlEngine;

import org.apache.commons.jexl3.parser.ASTJexlScript;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
abstract class CselToExpressionTest
{
  @Mock
  protected SearchMappingService service;

  protected JexlEngine jexlEngine = new JexlEngine();

  protected SelectorExpressionBuilder builder;

  protected CselToExpression underTest;

  protected abstract CselToExpression createUnderTest();

  @BeforeEach
  void setup() {
    underTest = createUnderTest();
    reset();
  }

  @Test
  void andTest() {
    final ASTJexlScript script = jexlEngine.parseExpression("a==\"woof\" && b==\"meow\"");

    script.childrenAccept(underTest, builder);

    SqlClause clause = (SqlClause) builder.build();
    assertThat(clause.operand(), is(Operand.AND));

    assertThat(clause.expressions(), hasSize(2));

    // a_alias @@ TO_TSQUERY('simple', :param_0)
    assertPredicate((SqlPredicate) clause.expressions().get(0), SearchField.FORMAT_FIELD_1, Operand.EQ,
        new ExactTerm("woof"));

    // b_alias @@ TO_TSQUERY('simple', :param_1)
    assertPredicate((SqlPredicate) clause.expressions().get(1), SearchField.FORMAT_FIELD_2, Operand.EQ,
        new ExactTerm("meow"));
  }

  @Test
  void orTest() {
    final ASTJexlScript script = jexlEngine.parseExpression("a==\"woof\" || b==\"meow\"");

    script.childrenAccept(underTest, builder);

    SqlClause clause = (SqlClause) builder.build();
    assertThat(clause.operand(), is(Operand.OR));

    assertThat(clause.expressions(), hasSize(2));

    // a_alias @@ TO_TSQUERY('simple', :param_0)
    assertPredicate((SqlPredicate) clause.expressions().get(0), SearchField.FORMAT_FIELD_1, Operand.EQ,
        new ExactTerm("woof"));

    // b_alias @@ TO_TSQUERY('simple', :param_1)
    assertPredicate((SqlPredicate) clause.expressions().get(1), SearchField.FORMAT_FIELD_2, Operand.EQ,
        new ExactTerm("meow"));
  }

  @Test
  void prefixTest() {
    final ASTJexlScript script = jexlEngine.parseExpression("a =^ \"woof\"");

    script.childrenAccept(underTest, builder);

    assertPredicate((SqlPredicate) builder.build(), SearchField.FORMAT_FIELD_1, Operand.EQ,
        new WildcardTerm("woof", false));
  }

  @Test
  void notEqualTest() {
    final ASTJexlScript script = jexlEngine.parseExpression("a != \"woof\"");

    script.childrenAccept(underTest, builder);

    SqlClause clause = (SqlClause) builder.build();
    assertThat(clause.operand(), is(Operand.OR));

    assertPredicate((SqlPredicate) clause.expressions().get(0), SearchField.FORMAT_FIELD_1, Operand.EQ,
        NullTerm.INSTANCE);

    assertPredicate((SqlPredicate) clause.expressions().get(1), SearchField.FORMAT_FIELD_1, Operand.NOT_EQ,
        new ExactTerm("woof"));
  }

  @Test
  void parensTest() {
    final ASTJexlScript script = jexlEngine.parseExpression("a==\"woof\" && (b==\"meow\" || b==\"purr\")");

    script.childrenAccept(underTest, builder);

    SqlClause clause = (SqlClause) builder.build();
    assertThat(clause.operand(), is(Operand.AND));

    assertThat(clause.expressions(), hasSize(2));

    // a_alias @@ TO_TSQUERY('simple', :param_0)
    assertPredicate((SqlPredicate) clause.expressions().get(0), SearchField.FORMAT_FIELD_1, Operand.EQ,
        new ExactTerm("woof"));

    // Right side of AND is itself a clause
    assertThat(clause.expressions().get(1), instanceOf(SqlClause.class));

    clause = (SqlClause) clause.expressions().get(1);
    assertThat(clause.operand(), is(Operand.OR));

    // b_alias @@ TO_TSQUERY('simple', :param_1
    assertPredicate((SqlPredicate) clause.expressions().get(0), SearchField.FORMAT_FIELD_2, Operand.EQ,
        new ExactTerm("meow"));

    // b_alias @@ TO_TSQUERY('simple', :param_2)
    assertPredicate((SqlPredicate) clause.expressions().get(1), SearchField.FORMAT_FIELD_2, Operand.EQ,
        new ExactTerm("purr"));
  }

  @Test
  public void publicDocumentationExampleTest() {
    // Test the public documentation example: progressive Maven path access
    // format == "maven2" and (path == "/" or path == "/org/" or path == "/org/apache/" or path =^
    // "/org/apache/commons/")
    lenient().when(service.getSearchField("format")).thenReturn(Optional.of(SearchField.FORMAT_FIELD_1));
    lenient().when(service.getSearchField("path")).thenReturn(Optional.of(SearchField.PATHS));

    final ASTJexlScript script = jexlEngine.parseExpression(
        "format == \"maven2\" and (path == \"/\" or path == \"/org/\" or path == \"/org/apache/\" or path =^ \"/org/apache/commons/\")");

    script.childrenAccept(underTest, builder);

    SqlClause clause = (SqlClause) builder.build();
    assertThat(clause.operand(), is(Operand.AND));
    assertThat(clause.expressions(), hasSize(2));

    // First part: format == "maven2"
    assertPredicate((SqlPredicate) clause.expressions().get(0), SearchField.FORMAT_FIELD_1, Operand.EQ,
        new ExactTerm("maven2"));

    // Second part: OR clause with multiple path conditions
    assertThat(clause.expressions().get(1), instanceOf(SqlClause.class));
    SqlClause pathClause = (SqlClause) clause.expressions().get(1);
    assertThat(pathClause.operand(), is(Operand.OR));
    assertThat(pathClause.expressions(), hasSize(4));

    // path == "/"
    assertPredicate((SqlPredicate) pathClause.expressions().get(0), SearchField.PATHS, Operand.EQ, new ExactTerm("/"));

    // path == "/org/"
    assertPredicate((SqlPredicate) pathClause.expressions().get(1), SearchField.PATHS, Operand.EQ,
        new ExactTerm("/org/"));

    // path == "/org/apache/"
    assertPredicate((SqlPredicate) pathClause.expressions().get(2), SearchField.PATHS, Operand.EQ,
        new ExactTerm("/org/apache/"));

    // path =^ "/org/apache/commons/" (starts-with)
    assertPredicate((SqlPredicate) pathClause.expressions().get(3), SearchField.PATHS, Operand.EQ,
        new WildcardTerm("/org/apache/commons/", false));
  }

  protected void reset() {
    lenient().when(service.getSearchField("a")).thenReturn(Optional.of(SearchField.FORMAT_FIELD_1));
    lenient().when(service.getSearchField("b")).thenReturn(Optional.of(SearchField.FORMAT_FIELD_2));
    builder = new SelectorExpressionBuilder(service);
    builder.propertyAlias("paths", SearchField.PATHS);
  }

  protected static void assertPredicate(
      final SqlPredicate actual,
      final SearchField field,
      final Operand operand,
      final Term term)
  {
    assertThat(actual.getSearchField(), is(field));
    assertThat(actual.operand(), is(operand));
    assertThat(actual.getTerm(), is(term));
  }
}
