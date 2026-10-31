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
  void unparenthesizedAndOrTest() {
    // a and b or c — AND binds tighter; expected: OR( AND(a,b), c )
    final ASTJexlScript script = jexlEngine.parseExpression("a==\"woof\" && b==\"meow\" || b==\"purr\"");

    script.childrenAccept(underTest, builder);

    SqlClause clause = (SqlClause) builder.build();
    assertThat(clause.operand(), is(Operand.OR));
    assertThat(clause.expressions(), hasSize(2));

    // Left of OR is itself an AND clause
    assertThat(clause.expressions().get(0), instanceOf(SqlClause.class));
    SqlClause andClause = (SqlClause) clause.expressions().get(0);
    assertThat(andClause.operand(), is(Operand.AND));
    assertThat(andClause.expressions(), hasSize(2));
    assertPredicate((SqlPredicate) andClause.expressions().get(0), SearchField.FORMAT_FIELD_1, Operand.EQ,
        new ExactTerm("woof"));
    assertPredicate((SqlPredicate) andClause.expressions().get(1), SearchField.FORMAT_FIELD_2, Operand.EQ,
        new ExactTerm("meow"));

    // Right of OR is a single predicate
    assertPredicate((SqlPredicate) clause.expressions().get(1), SearchField.FORMAT_FIELD_2, Operand.EQ,
        new ExactTerm("purr"));
  }

  @Test
  void unparenthesizedOrAndTest() {
    // a or b and c — AND binds tighter; expected: OR( a, AND(b,c) )
    final ASTJexlScript script = jexlEngine.parseExpression("a==\"woof\" || b==\"meow\" && b==\"purr\"");

    script.childrenAccept(underTest, builder);

    SqlClause clause = (SqlClause) builder.build();
    assertThat(clause.operand(), is(Operand.OR));
    assertThat(clause.expressions(), hasSize(2));

    // Left of OR is a single predicate
    assertPredicate((SqlPredicate) clause.expressions().get(0), SearchField.FORMAT_FIELD_1, Operand.EQ,
        new ExactTerm("woof"));

    // Right of OR is itself an AND clause
    assertThat(clause.expressions().get(1), instanceOf(SqlClause.class));
    SqlClause andClause = (SqlClause) clause.expressions().get(1);
    assertThat(andClause.operand(), is(Operand.AND));
    assertThat(andClause.expressions(), hasSize(2));
    assertPredicate((SqlPredicate) andClause.expressions().get(0), SearchField.FORMAT_FIELD_2, Operand.EQ,
        new ExactTerm("meow"));
    assertPredicate((SqlPredicate) andClause.expressions().get(1), SearchField.FORMAT_FIELD_2, Operand.EQ,
        new ExactTerm("purr"));
  }

  @Test
  void multiGroupAndOrTest() {
    // a and b and c or d and e — expected: OR( AND(a,b,c), AND(d,e) )
    final ASTJexlScript script = jexlEngine.parseExpression(
        "a==\"woof\" && b==\"meow\" && b==\"purr\" || a==\"bark\" && b==\"hiss\"");

    script.childrenAccept(underTest, builder);

    SqlClause clause = (SqlClause) builder.build();
    assertThat(clause.operand(), is(Operand.OR));
    assertThat(clause.expressions(), hasSize(2));

    // Left of OR: AND(a, b, b)
    assertThat(clause.expressions().get(0), instanceOf(SqlClause.class));
    SqlClause leftAnd = (SqlClause) clause.expressions().get(0);
    assertThat(leftAnd.operand(), is(Operand.AND));
    assertThat(leftAnd.expressions(), hasSize(3));

    // Right of OR: AND(a, b)
    assertThat(clause.expressions().get(1), instanceOf(SqlClause.class));
    SqlClause rightAnd = (SqlClause) clause.expressions().get(1);
    assertThat(rightAnd.operand(), is(Operand.AND));
    assertThat(rightAnd.expressions(), hasSize(2));
  }

  @Test
  void pureAndThreeTermsTest() {
    // a and b and c — expected: AND(a, b, c)
    final ASTJexlScript script = jexlEngine.parseExpression("a==\"woof\" && b==\"meow\" && b==\"purr\"");
    script.childrenAccept(underTest, builder);

    SqlClause clause = (SqlClause) builder.build();
    assertThat(clause.operand(), is(Operand.AND));
    assertThat(clause.expressions(), hasSize(3));
    assertPredicate((SqlPredicate) clause.expressions().get(0), SearchField.FORMAT_FIELD_1, Operand.EQ,
        new ExactTerm("woof"));
    assertPredicate((SqlPredicate) clause.expressions().get(1), SearchField.FORMAT_FIELD_2, Operand.EQ,
        new ExactTerm("meow"));
    assertPredicate((SqlPredicate) clause.expressions().get(2), SearchField.FORMAT_FIELD_2, Operand.EQ,
        new ExactTerm("purr"));
  }

  @Test
  void pureOrThreeTermsTest() {
    // a or b or c — expected: OR(a, b, c)
    final ASTJexlScript script = jexlEngine.parseExpression("a==\"woof\" || b==\"meow\" || b==\"purr\"");
    script.childrenAccept(underTest, builder);

    SqlClause clause = (SqlClause) builder.build();
    assertThat(clause.operand(), is(Operand.OR));
    assertThat(clause.expressions(), hasSize(3));
    assertPredicate((SqlPredicate) clause.expressions().get(0), SearchField.FORMAT_FIELD_1, Operand.EQ,
        new ExactTerm("woof"));
    assertPredicate((SqlPredicate) clause.expressions().get(1), SearchField.FORMAT_FIELD_2, Operand.EQ,
        new ExactTerm("meow"));
    assertPredicate((SqlPredicate) clause.expressions().get(2), SearchField.FORMAT_FIELD_2, Operand.EQ,
        new ExactTerm("purr"));
  }

  @Test
  void twoAndGroupsSeparatedByOrTest() {
    // a and b or c and d — expected: OR( AND(a,b), AND(c,d) )
    final ASTJexlScript script = jexlEngine.parseExpression(
        "a==\"woof\" && b==\"meow\" || a==\"bark\" && b==\"hiss\"");
    script.childrenAccept(underTest, builder);

    SqlClause clause = (SqlClause) builder.build();
    assertThat(clause.operand(), is(Operand.OR));
    assertThat(clause.expressions(), hasSize(2));

    SqlClause left = (SqlClause) clause.expressions().get(0);
    assertThat(left.operand(), is(Operand.AND));
    assertThat(left.expressions(), hasSize(2));
    assertPredicate((SqlPredicate) left.expressions().get(0), SearchField.FORMAT_FIELD_1, Operand.EQ,
        new ExactTerm("woof"));
    assertPredicate((SqlPredicate) left.expressions().get(1), SearchField.FORMAT_FIELD_2, Operand.EQ,
        new ExactTerm("meow"));

    SqlClause right = (SqlClause) clause.expressions().get(1);
    assertThat(right.operand(), is(Operand.AND));
    assertThat(right.expressions(), hasSize(2));
    assertPredicate((SqlPredicate) right.expressions().get(0), SearchField.FORMAT_FIELD_1, Operand.EQ,
        new ExactTerm("bark"));
    assertPredicate((SqlPredicate) right.expressions().get(1), SearchField.FORMAT_FIELD_2, Operand.EQ,
        new ExactTerm("hiss"));
  }

  @Test
  void orSandwichingAndGroupTest() {
    // a or b and c or d — expected: OR( a, AND(b,c), d )
    final ASTJexlScript script = jexlEngine.parseExpression(
        "a==\"woof\" || b==\"meow\" && b==\"purr\" || a==\"bark\"");
    script.childrenAccept(underTest, builder);

    SqlClause clause = (SqlClause) builder.build();
    assertThat(clause.operand(), is(Operand.OR));
    assertThat(clause.expressions(), hasSize(3));

    assertPredicate((SqlPredicate) clause.expressions().get(0), SearchField.FORMAT_FIELD_1, Operand.EQ,
        new ExactTerm("woof"));

    SqlClause andClause = (SqlClause) clause.expressions().get(1);
    assertThat(andClause.operand(), is(Operand.AND));
    assertThat(andClause.expressions(), hasSize(2));
    assertPredicate((SqlPredicate) andClause.expressions().get(0), SearchField.FORMAT_FIELD_2, Operand.EQ,
        new ExactTerm("meow"));
    assertPredicate((SqlPredicate) andClause.expressions().get(1), SearchField.FORMAT_FIELD_2, Operand.EQ,
        new ExactTerm("purr"));

    assertPredicate((SqlPredicate) clause.expressions().get(2), SearchField.FORMAT_FIELD_1, Operand.EQ,
        new ExactTerm("bark"));
  }

  @Test
  void threeAndGroupsSeparatedByOrTest() {
    // a and b or c and d or e and f — expected: OR( AND(a,b), AND(c,d), AND(e,f) )
    final ASTJexlScript script = jexlEngine.parseExpression(
        "a==\"w1\" && b==\"w2\" || a==\"w3\" && b==\"w4\" || a==\"w5\" && b==\"w6\"");
    script.childrenAccept(underTest, builder);

    SqlClause clause = (SqlClause) builder.build();
    assertThat(clause.operand(), is(Operand.OR));
    assertThat(clause.expressions(), hasSize(3));

    assertThat(clause.expressions().get(0), instanceOf(SqlClause.class));
    assertThat(((SqlClause) clause.expressions().get(0)).operand(), is(Operand.AND));
    assertThat(((SqlClause) clause.expressions().get(0)).expressions(), hasSize(2));

    assertThat(clause.expressions().get(1), instanceOf(SqlClause.class));
    assertThat(((SqlClause) clause.expressions().get(1)).operand(), is(Operand.AND));
    assertThat(((SqlClause) clause.expressions().get(1)).expressions(), hasSize(2));

    assertThat(clause.expressions().get(2), instanceOf(SqlClause.class));
    assertThat(((SqlClause) clause.expressions().get(2)).operand(), is(Operand.AND));
    assertThat(((SqlClause) clause.expressions().get(2)).expressions(), hasSize(2));
  }

  @Test
  void andGroupBetweenTwoOrGroupsTest() {
    // a or b or c and d and e or f or g — expected: OR( a, b, AND(c,d,e), f, g )
    final ASTJexlScript script = jexlEngine.parseExpression(
        "a==\"w1\" || a==\"w2\" || b==\"w3\" && b==\"w4\" && b==\"w5\" || a==\"w6\" || a==\"w7\"");
    script.childrenAccept(underTest, builder);

    SqlClause clause = (SqlClause) builder.build();
    assertThat(clause.operand(), is(Operand.OR));
    assertThat(clause.expressions(), hasSize(5));

    assertThat(clause.expressions().get(0), instanceOf(SqlPredicate.class));
    assertThat(clause.expressions().get(1), instanceOf(SqlPredicate.class));

    SqlClause andGroup = (SqlClause) clause.expressions().get(2);
    assertThat(andGroup.operand(), is(Operand.AND));
    assertThat(andGroup.expressions(), hasSize(3));

    assertThat(clause.expressions().get(3), instanceOf(SqlPredicate.class));
    assertThat(clause.expressions().get(4), instanceOf(SqlPredicate.class));
  }

  @Test
  void nestedParensWithUnparenthesizedOuterTest() {
    // a and (b or c) or d — expected: OR( AND(a, OR(b,c)), d )
    final ASTJexlScript script = jexlEngine.parseExpression(
        "a==\"woof\" && (b==\"meow\" || b==\"purr\") || a==\"bark\"");
    script.childrenAccept(underTest, builder);

    SqlClause outer = (SqlClause) builder.build();
    assertThat(outer.operand(), is(Operand.OR));
    assertThat(outer.expressions(), hasSize(2));

    // Left: AND(a, OR(b,c))
    SqlClause andClause = (SqlClause) outer.expressions().get(0);
    assertThat(andClause.operand(), is(Operand.AND));
    assertThat(andClause.expressions(), hasSize(2));
    assertPredicate((SqlPredicate) andClause.expressions().get(0), SearchField.FORMAT_FIELD_1, Operand.EQ,
        new ExactTerm("woof"));
    SqlClause innerOr = (SqlClause) andClause.expressions().get(1);
    assertThat(innerOr.operand(), is(Operand.OR));
    assertThat(innerOr.expressions(), hasSize(2));

    // Right: d
    assertPredicate((SqlPredicate) outer.expressions().get(1), SearchField.FORMAT_FIELD_1, Operand.EQ,
        new ExactTerm("bark"));
  }

  @Test
  void unparenthesizedOrThenTwoAndGroupsTest() {
    // a or b and c or d and e — expected: OR( a, AND(b,c), AND(d,e) ) — wait, no!
    // JEXL precedence: AND > OR, so: a OR (b AND c) OR (d AND e)
    // expected: OR( a, AND(b,c), AND(d,e) )
    final ASTJexlScript script = jexlEngine.parseExpression(
        "a==\"w1\" || b==\"w2\" && b==\"w3\" || a==\"w4\" && b==\"w5\"");
    script.childrenAccept(underTest, builder);

    SqlClause clause = (SqlClause) builder.build();
    assertThat(clause.operand(), is(Operand.OR));
    assertThat(clause.expressions(), hasSize(3));

    assertPredicate((SqlPredicate) clause.expressions().get(0), SearchField.FORMAT_FIELD_1, Operand.EQ,
        new ExactTerm("w1"));

    SqlClause firstAnd = (SqlClause) clause.expressions().get(1);
    assertThat(firstAnd.operand(), is(Operand.AND));
    assertThat(firstAnd.expressions(), hasSize(2));

    SqlClause secondAnd = (SqlClause) clause.expressions().get(2);
    assertThat(secondAnd.operand(), is(Operand.AND));
    assertThat(secondAnd.expressions(), hasSize(2));
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

  @Test
  void singlePredicateTest() {
    // format == "nuget" alone — no operator at all, exercises size==1 early return
    final ASTJexlScript script = jexlEngine.parseExpression("a==\"woof\"");
    script.childrenAccept(underTest, builder);

    assertPredicate((SqlPredicate) builder.build(), SearchField.FORMAT_FIELD_1, Operand.EQ, new ExactTerm("woof"));
  }

  @Test
  void notEqualCombinedWithAndTest() {
    // format != "maven2" and path =^ "/foo"
    // != expands internally to OR(format IS NULL, format != maven2), then AND with path
    // expected: AND( OR(null, !=maven2), path=^/foo )
    final ASTJexlScript script = jexlEngine.parseExpression("a != \"woof\" && b =^ \"foo\"");
    script.childrenAccept(underTest, builder);

    SqlClause clause = (SqlClause) builder.build();
    assertThat(clause.operand(), is(Operand.AND));
    assertThat(clause.expressions(), hasSize(2));

    // Left side: OR(null, !=woof) — the expanded != predicate
    assertThat(clause.expressions().get(0), instanceOf(SqlClause.class));
    SqlClause notEqClause = (SqlClause) clause.expressions().get(0);
    assertThat(notEqClause.operand(), is(Operand.OR));
    assertThat(notEqClause.expressions(), hasSize(2));

    // Right side: path =^ foo
    assertThat(clause.expressions().get(1), instanceOf(SqlPredicate.class));
    assertPredicate((SqlPredicate) clause.expressions().get(1), SearchField.FORMAT_FIELD_2, Operand.EQ,
        new WildcardTerm("foo", false));
  }

  @Test
  void nexus53265RegressionTest() {
    // Exact expression from the NEXUS-53265 bug report — previously crashed with HTTP 500
    // format == "nuget" and path =^ "/pws-blueprint." or path =^ "/v3/content/pws-blueprint"
    // expected: OR( AND(format=nuget, path=^/pws-blueprint.), path=^/v3/content/pws-blueprint )
    lenient().when(service.getSearchField("format")).thenReturn(Optional.of(SearchField.FORMAT_FIELD_1));
    lenient().when(service.getSearchField("path")).thenReturn(Optional.of(SearchField.PATHS));

    final ASTJexlScript script = jexlEngine.parseExpression(
        "format == \"nuget\" and path =^ \"/pws-blueprint.\" or path =^ \"/v3/content/pws-blueprint\"");
    script.childrenAccept(underTest, builder);

    SqlClause clause = (SqlClause) builder.build();
    assertThat(clause.operand(), is(Operand.OR));
    assertThat(clause.expressions(), hasSize(2));

    // Left: AND(format=nuget, path=^/pws-blueprint.)
    assertThat(clause.expressions().get(0), instanceOf(SqlClause.class));
    SqlClause andClause = (SqlClause) clause.expressions().get(0);
    assertThat(andClause.operand(), is(Operand.AND));
    assertThat(andClause.expressions(), hasSize(2));
    assertPredicate((SqlPredicate) andClause.expressions().get(0), SearchField.FORMAT_FIELD_1, Operand.EQ,
        new ExactTerm("nuget"));
    assertPredicate((SqlPredicate) andClause.expressions().get(1), SearchField.PATHS, Operand.EQ,
        new WildcardTerm("/pws-blueprint.", false));

    // Right: path=^/v3/content/pws-blueprint (no format check — intentional, matches any format)
    assertPredicate((SqlPredicate) clause.expressions().get(1), SearchField.PATHS, Operand.EQ,
        new WildcardTerm("/v3/content/pws-blueprint", false));
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
