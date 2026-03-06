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
package org.sonatype.nexus.repository.search.sql.query.h2;

import java.util.Map;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.sql.query.SqlSearchQueryCondition;
import org.sonatype.nexus.repository.search.sql.query.syntax.BooleanTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.ExactTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;
import org.sonatype.nexus.repository.search.sql.query.syntax.LenientTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Operand;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlClause;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlPredicate;
import org.sonatype.nexus.repository.search.sql.query.syntax.TermCollection;
import org.sonatype.nexus.repository.search.sql.query.syntax.WildcardTerm;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

class H2FulltextSearchConditionBuilderTest
    extends Test5Support
{
  @Test
  void testBooleanTerm() {
    Expression expression = new SqlPredicate(Operand.EQ, SearchField.PRERELEASE, new BooleanTerm(true));
    SqlSearchQueryCondition result = underTest().build(expression);

    assertThat(result, is(
        new SqlSearchQueryCondition("cs.prerelease = #{filterParams.cs_prerelease0}", Map.of("cs_prerelease0", true))));
  }

  @Test
  void testExactTermWithTextSearch() {
    Expression expression = new SqlPredicate(Operand.EQ, SearchField.KEYWORDS, new ExactTerm("tomcat"));
    SqlSearchQueryCondition result = underTest().build(expression);

    assertThat(result.getSqlFilter(), is("(LOWER(cs.keywords) LIKE LOWER(#{filterParams.cs_keywords0}))"));
    assertThat(result.getParameters().get("cs_keywords0"), is("%tomcat%"));
  }

  @Test
  void testWildcardTerm() {
    Expression expression = new SqlPredicate(Operand.EQ, SearchField.KEYWORDS, new WildcardTerm("org.apache*"));
    SqlSearchQueryCondition result = underTest().build(expression);

    assertThat(result.getSqlFilter(), is("(LOWER(cs.keywords) LIKE LOWER(#{filterParams.cs_keywords0}))"));
    assertThat(result.getParameters().get("cs_keywords0"), is("%org.apache%%"));
  }

  @Test
  void testLenientTerm() {
    Expression expression = new SqlPredicate(Operand.EQ, SearchField.KEYWORDS, new LenientTerm("search"));
    SqlSearchQueryCondition result = underTest().build(expression);

    assertThat(result.getSqlFilter(), is("(LOWER(cs.keywords) LIKE LOWER(#{filterParams.cs_keywords0}))"));
    assertThat(result.getParameters().get("cs_keywords0"), is("%search%"));
  }

  @Test
  void testMultipleTermsWithAndLogic() {
    Expression expression = new SqlPredicate(Operand.EQ, SearchField.KEYWORDS,
        TermCollection.create(new ExactTerm("maven"), new ExactTerm("plugin")));
    SqlSearchQueryCondition result = underTest().build(expression);

    assertThat(result.getSqlFilter(),
        is("(LOWER(cs.keywords) LIKE LOWER(#{filterParams.cs_keywords0}) AND LOWER(cs.keywords) LIKE LOWER(#{filterParams.cs_keywords1}))"));
    assertThat(result.getParameters().get("cs_keywords0"), is("%maven%"));
    assertThat(result.getParameters().get("cs_keywords1"), is("%plugin%"));
  }

  @Test
  void testMultipleTermsWithOrLogic() {
    Expression expression = new SqlPredicate(Operand.ANY, SearchField.KEYWORDS,
        TermCollection.create(new ExactTerm("junit"), new ExactTerm("mockito")));
    SqlSearchQueryCondition result = underTest().build(expression);

    assertThat(result.getSqlFilter(),
        is("(LOWER(cs.keywords) LIKE LOWER(#{filterParams.cs_keywords0}) OR LOWER(cs.keywords) LIKE LOWER(#{filterParams.cs_keywords1}))"));
    assertThat(result.getParameters().get("cs_keywords0"), is("%junit%"));
    assertThat(result.getParameters().get("cs_keywords1"), is("%mockito%"));
  }

  @Test
  void testMultipleTermsWithInOperator() {
    Expression expression = new SqlPredicate(Operand.IN, SearchField.KEYWORDS,
        TermCollection.create(new ExactTerm("test1"), new ExactTerm("test2")));
    SqlSearchQueryCondition result = underTest().build(expression);

    assertThat(result.getSqlFilter(),
        is("(LOWER(cs.keywords) LIKE LOWER(#{filterParams.cs_keywords0}) OR LOWER(cs.keywords) LIKE LOWER(#{filterParams.cs_keywords1}))"));
    assertThat(result.getParameters().get("cs_keywords0"), is("%test1%"));
    assertThat(result.getParameters().get("cs_keywords1"), is("%test2%"));
  }

  @Test
  void testSpecialCharacterEscaping() {
    // Test escaping of LIKE special characters: %, _, and \
    Expression expression = new SqlPredicate(Operand.EQ, SearchField.KEYWORDS,
        new ExactTerm("test_file%name\\path"));
    SqlSearchQueryCondition result = underTest().build(expression);

    assertThat(result.getSqlFilter(), is("(LOWER(cs.keywords) LIKE LOWER(#{filterParams.cs_keywords0}))"));
    // Should escape %, _, and \ as \%, \_, \\
    assertThat(result.getParameters().get("cs_keywords0"), is("%test\\_file\\%name\\\\path%"));
  }

  @Test
  void testBlankStringFiltering() {
    // Blank strings should result in empty condition
    Expression expression = new SqlPredicate(Operand.EQ, SearchField.KEYWORDS,
        TermCollection.create(new ExactTerm("   "), new ExactTerm("")));
    SqlSearchQueryCondition result = underTest().build(expression);

    // Should result in empty condition when all terms are blank
    assertThat(result.getSqlFilter(), is(""));
    assertThat(result.getParameters().isEmpty(), is(true));
  }

  @Test
  void testBlankStringFilteringWithValidTerm() {
    // Mix of blank and valid strings - blank should be filtered out
    Expression expression = new SqlPredicate(Operand.EQ, SearchField.KEYWORDS,
        TermCollection.create(new ExactTerm("   "), new ExactTerm("valid")));
    SqlSearchQueryCondition result = underTest().build(expression);

    // Should only include the valid term
    assertThat(result.getSqlFilter(), is("(LOWER(cs.keywords) LIKE LOWER(#{filterParams.cs_keywords0}))"));
    assertThat(result.getParameters().get("cs_keywords0"), is("%valid%"));
  }

  @Test
  void testJsonColumnWithExactTerm() {
    // JSON columns should add quotes around exact terms to match JSON array elements
    Expression expression = new SqlPredicate(Operand.EQ, SearchField.TAGS, new ExactTerm("release"));
    SqlSearchQueryCondition result = underTest().build(expression);

    assertThat(result.getSqlFilter(), is("(LOWER(cs.tags) LIKE LOWER(#{filterParams.cs_tags0}))"));
    // Should wrap value in quotes for JSON matching
    assertThat(result.getParameters().get("cs_tags0"), is("%\"release\"%"));
  }

  @Test
  void testJsonColumnWithWildcardTerm() {
    // JSON columns with wildcard should add opening quote only for prefix matching
    Expression expression = new SqlPredicate(Operand.EQ, SearchField.TAGS, new WildcardTerm("rel*"));
    SqlSearchQueryCondition result = underTest().build(expression);

    assertThat(result.getSqlFilter(), is("(LOWER(cs.tags) LIKE LOWER(#{filterParams.cs_tags0}))"));
    // Should add opening quote for JSON prefix matching (wildcard already converted to %)
    assertThat(result.getParameters().get("cs_tags0"), is("%\"rel%%"));
  }

  @Test
  void testTokenizedColumnAlwaysUsesTextSearch() {
    // Tokenized columns always use LIKE queries even for single exact terms
    // because tokenized data is stored as space-separated values (e.g., "0.1 0 1")
    Expression expression = new SqlPredicate(Operand.EQ, SearchField.KEYWORDS, new ExactTerm("exact"));
    SqlSearchQueryCondition result = underTest().build(expression);

    // Should use LIKE even for exact term because KEYWORDS is tokenized
    assertThat(result.getSqlFilter(), containsString("LIKE"));
    assertThat(result.getParameters().get("cs_keywords0"), is("%exact%"));
  }

  @Test
  void testNonTokenizedColumnWithSingleExactTerm() {
    // Non-tokenized columns with a single exact term should use exact matching (not LIKE)
    Expression expression = new SqlPredicate(Operand.EQ, SearchField.NAME, new ExactTerm("component-name"));
    SqlSearchQueryCondition result = underTest().build(expression);

    // Should use = for exact matching, not LIKE
    assertThat(result.getSqlFilter(), is("cs.search_component_name = #{filterParams.cs_search_component_name0}"));
    assertThat(result.getParameters().get("cs_search_component_name0"), is("component-name"));
  }

  @Test
  void testNonTokenizedColumnWithMultipleTerms() {
    // Non-tokenized columns with multiple terms should use text search (LIKE)
    Expression expression = new SqlPredicate(Operand.EQ, SearchField.NAME,
        TermCollection.create(new ExactTerm("comp1"), new ExactTerm("comp2")));
    SqlSearchQueryCondition result = underTest().build(expression);

    // Should use LIKE for multiple terms
    assertThat(result.getSqlFilter(), containsString("LIKE"));
    assertThat(result.getParameters().get("cs_search_component_name0"), is("%comp1%"));
    assertThat(result.getParameters().get("cs_search_component_name1"), is("%comp2%"));
  }

  @Test
  void testWildcardWithLeadingSeparators() {
    // Test handling of wildcard terms with leading separators (similar to Postgres test)
    Expression expression = new SqlPredicate(Operand.EQ, SearchField.KEYWORDS,
        TermCollection.create(new WildcardTerm("maven"), new WildcardTerm("-provider")));
    SqlSearchQueryCondition result = underTest().build(expression);

    assertThat(result.getSqlFilter(),
        is("(LOWER(cs.keywords) LIKE LOWER(#{filterParams.cs_keywords0}) AND LOWER(cs.keywords) LIKE LOWER(#{filterParams.cs_keywords1}))"));
    assertThat(result.getParameters().get("cs_keywords0"), is("%maven%"));
    // The hyphen should be escaped for LIKE
    assertThat(result.getParameters().get("cs_keywords1"), is("%-provider%"));
  }

  @Test
  void testComplexWildcardPattern() {
    // Test complex wildcard patterns with multiple asterisks
    Expression expression = new SqlPredicate(Operand.EQ, SearchField.KEYWORDS,
        new WildcardTerm("org*apache*maven*"));
    SqlSearchQueryCondition result = underTest().build(expression);

    assertThat(result.getSqlFilter(), is("(LOWER(cs.keywords) LIKE LOWER(#{filterParams.cs_keywords0}))"));
    // All asterisks should be converted to % for SQL LIKE
    assertThat(result.getParameters().get("cs_keywords0"), is("%org%apache%maven%%"));
  }

  @Test
  void testMixedTermTypesInClause() {
    // Test a complex clause with different term types
    Expression expression = SqlClause.create(Operand.AND,
        new SqlPredicate(Operand.EQ, SearchField.KEYWORDS, new ExactTerm("java")),
        new SqlPredicate(Operand.EQ, SearchField.NAME, new WildcardTerm("spring*")));
    SqlSearchQueryCondition result = underTest().build(expression);

    // Should combine both predicates with AND
    assertThat(result.getSqlFilter(), containsString("cs.keywords"));
    assertThat(result.getSqlFilter(), containsString("cs.search_component_name"));
    assertThat(result.getParameters().get("cs_keywords0"), is("%java%"));
    assertThat(result.getParameters().get("cs_search_component_name1"), is("%spring%%"));
  }

  private static H2FulltextSearchConditionBuilder underTest() {
    return new H2FulltextSearchConditionBuilder(new H2SearchDB());
  }
}
