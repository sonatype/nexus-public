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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.sonatype.nexus.repository.rest.internal.DefaultSearchMappings;
import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.SearchMappingService;
import org.sonatype.nexus.repository.search.sql.query.syntax.ExactTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;
import org.sonatype.nexus.repository.search.sql.query.syntax.LenientTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Operand;
import org.sonatype.nexus.repository.search.sql.query.syntax.RegexWildcardTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.SingleValueTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlClause;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlPredicate;
import org.sonatype.nexus.repository.search.sql.query.syntax.StringTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Term;
import org.sonatype.nexus.repository.search.sql.query.syntax.TermCollection;
import org.sonatype.nexus.repository.search.sql.query.syntax.WildcardTerm;
import org.sonatype.nexus.rest.ValidationErrorsException;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DefaultSqlSearchQueryContributionTest
{
  private static final String GROUP_RAW = "group.raw";

  private DefaultSqlSearchQueryContribution underTest;

  @Before
  public void setup() {
    underTest = new DefaultSqlSearchQueryContribution();
    underTest.init(new SearchMappingService(Arrays.asList(new DefaultSearchMappings())), false);
  }

  @Test
  public void shouldIgnoreNull() {
    assertFalse(underTest.createPredicate(null).isPresent());
  }

  @Test
  public void shouldIgnoreWhitespace() {
    Optional<Expression> result = underTest.createPredicate(new SearchFilter(GROUP_RAW, "    "));

    assertTrue(result.isPresent());
    assertThat(result.get(), is(new SqlPredicate(Operand.EQ, SearchField.NAMESPACE, new ExactTerm(""))));
  }

  @Test
  public void shouldIgnoreUnknownFields() {
    Optional<Expression> result = underTest.createPredicate(new SearchFilter("an_unknown_field", "mockito"));

    assertFalse(result.isPresent());
  }

  @Test
  public void shouldAddConditionToQueryBuilder() {
    Optional<Expression> result = underTest.createPredicate(new SearchFilter(GROUP_RAW, "unit? mockito*"));

    assertTrue(result.isPresent());
    assertThat(result.get(),
        is(SqlClause.create(Operand.OR, new SqlPredicate(Operand.EQ, SearchField.NAMESPACE, new WildcardTerm("unit")),
            new SqlPredicate(Operand.EQ, SearchField.NAMESPACE, new WildcardTerm("mockito")))));
  }

  @Test
  public void testSplit() {
    assertThat(split("foo*"), contains(new WildcardTerm("foo")));
    // When multi-embedded regex is disabled, multi-wildcard patterns are split into separate terms
    assertThat(split("foo?*"), contains(new WildcardTerm("foo")));
    assertThat(split("foo?"), contains(new WildcardTerm("foo")));
    // Multi-wildcard patterns are split when regex is disabled
    assertThat(split("foo*bar*ddd"),
        contains(new WildcardTerm("foo"), new WildcardTerm("bar"), new LenientTerm("ddd")));
    assertThat(split("foo?bar"), contains(new WildcardTerm("foo"), new LenientTerm("bar")));

    // escaped wildcards - these should be ExactTerm since group field now uses exact matching
    assertThat(split("foo\\*bar"), contains(new ExactTerm("foo*bar")));
    assertThat(split("foo\\?bar"), contains(new ExactTerm("foo?bar")));
    assertThat(split("foo\\*"), contains(new ExactTerm("foo*")));
    assertThat(split("foo\\?"), contains(new ExactTerm("foo?")));
    assertThat(split("\\*bar"), contains(new ExactTerm("*bar")));
    assertThat(split("\\?bar"), contains(new ExactTerm("?bar")));
  }

  @Test(expected = ValidationErrorsException.class)
  public void testLeadingAsterick() {
    assertThat(split("*bar"), contains(new LenientTerm("bar")));
  }

  @Test(expected = ValidationErrorsException.class)
  public void testTooFewWildcard() {
    split("ba*");
  }

  @Test(expected = ValidationErrorsException.class)
  public void testLeadingQuestion() {
    assertThat(split("?bar"), contains(new LenientTerm("bar")));
  }

  @Test(expected = ValidationErrorsException.class)
  public void testSpecialCharacterAndLeadingWildcard() {
    assertThat(split("/*"), contains(new LenientTerm("bar")));
  }

  @Test
  public void testCaseInsensitiveGroupSearch() {
    // Test uppercase letters are handled as ExactTerm (exact matching with original case)
    assertThat(split("Support"), contains(new ExactTerm("Support")));
    assertThat(split("TEAM"), contains(new ExactTerm("TEAM")));
    assertThat(split("Maven"), contains(new ExactTerm("Maven")));
  }

  @Test
  public void testHierarchicalGroupNames() {
    // Test hierarchical group names with slashes and mixed case (exact matching)
    assertThat(split("/Support/Team"),
        contains(new ExactTerm("/Support/Team")));
    assertThat(split("org/apache/maven"),
        contains(new ExactTerm("org/apache/maven")));
  }

  @Test
  public void testExactMatchField() {
    // Test that name.raw field (which has exactMatch=true) creates ExactTerm
    Collection<SingleValueTerm<?>> result = splitForField("name.raw", "hello");
    assertThat(result, contains(new ExactTerm("hello")));
  }

  @Test
  public void testExactMatchFieldWithMultipleTerms() {
    // Test that name.raw field with multiple space-separated terms creates multiple ExactTerms
    Collection<SingleValueTerm<?>> result = splitForField("name.raw", "hello world");
    assertThat(result, contains(new ExactTerm("hello"), new ExactTerm("world")));
  }

  @Test
  public void testLenientMatchField() {
    // Test that group.raw field now creates ExactTerm (changed to exactMatch=true for NEXUS-49265)
    Collection<SingleValueTerm<?>> result = splitForField(GROUP_RAW, "support");
    assertThat(result, contains(new ExactTerm("support")));
  }

  @Test
  public void testMultiWildcardSplitsWhenRegexDisabled() {
    // When multi-embedded regex is disabled, multi-wildcard patterns are split
    assertThat(split("goo*com*"), contains(new WildcardTerm("goo"), new WildcardTerm("com")));
    assertThat(split("org*apache*"), contains(new WildcardTerm("org"), new WildcardTerm("apache")));
    assertThat(split("foo?bar"), contains(new WildcardTerm("foo"), new LenientTerm("bar")));
    assertThat(split("a*b*c"), contains(new WildcardTerm("a"), new WildcardTerm("b"), new LenientTerm("c")));
  }

  @Test
  public void testMultiWildcardWithRegexEnabled() {
    // Test with multi-embedded regex enabled
    DefaultSqlSearchQueryContribution withRegex = new DefaultSqlSearchQueryContribution();
    withRegex.init(new SearchMappingService(Arrays.asList(new DefaultSearchMappings())), true);

    // Multi-wildcard patterns with asterisks should use RegexWildcardTerm when enabled
    assertThat(splitForField(withRegex, GROUP_RAW, "goo*com*"), contains(new RegexWildcardTerm("goo*com*")));
    assertThat(splitForField(withRegex, GROUP_RAW, "org*apache*"), contains(new RegexWildcardTerm("org*apache*")));
    assertThat(splitForField(withRegex, GROUP_RAW, "a*b*c"), contains(new RegexWildcardTerm("a*b*c")));

    // Single asterisk in middle should use RegexWildcardTerm (wildcard not at end)
    assertThat(splitForField(withRegex, GROUP_RAW, "foo*bar"), contains(new RegexWildcardTerm("foo*bar")));
    assertThat(splitForField(withRegex, GROUP_RAW, "commons*lang"), contains(new RegexWildcardTerm("commons*lang")));

    // Patterns with ? are handled by the tokenizer (splits on ?), so they don't trigger multi-wildcard detection
    // ? acts as a token separator in the tokenize() method
    assertThat(splitForField(withRegex, GROUP_RAW, "foo?bar"),
        contains(new WildcardTerm("foo"), new LenientTerm("bar")));
    // commons?lang3? splits into "commons" and "lang3?" - the trailing ? makes lang3 a WildcardTerm
    assertThat(splitForField(withRegex, GROUP_RAW, "commons?lang3?"),
        contains(new WildcardTerm("commons"), new WildcardTerm("lang3")));
    assertThat(splitForField(withRegex, GROUP_RAW, "commons?"), contains(new WildcardTerm("commons")));
    assertThat(splitForField(withRegex, GROUP_RAW, "foo?*"), contains(new WildcardTerm("foo")));

    // Single trailing wildcard should still use WildcardTerm for efficient prefix matching
    assertThat(splitForField(withRegex, GROUP_RAW, "foo*"), contains(new WildcardTerm("foo")));
    assertThat(splitForField(withRegex, GROUP_RAW, "test*"), contains(new WildcardTerm("test")));
  }

  @Test
  public void testSingleTrailingWildcardUsesPrefix() {
    // Single wildcard at end should still use WildcardTerm for efficient prefix matching
    assertThat(split("foo*"), contains(new WildcardTerm("foo")));
    assertThat(split("test*"), contains(new WildcardTerm("test")));
  }

  @Test
  public void testRegexWildcardTermPattern() {
    // Verify that RegexWildcardTerm generates correct regex patterns
    RegexWildcardTerm term1 = new RegexWildcardTerm("goo*com*");
    assertThat(term1.toRegexPattern(), is("^goo.*com.*$"));

    RegexWildcardTerm term2 = new RegexWildcardTerm("foo?bar");
    assertThat(term2.toRegexPattern(), is("^foo.bar$"));

    RegexWildcardTerm term3 = new RegexWildcardTerm("a*b*c");
    assertThat(term3.toRegexPattern(), is("^a.*b.*c$"));

    // NEXUS-51599: Test version pattern with dots
    RegexWildcardTerm term4 = new RegexWildcardTerm("3.2*.0");
    assertThat(term4.toRegexPattern(), is("^3\\.2.*\\.0$"));

    // Test escaped special regex chars
    RegexWildcardTerm term5 = new RegexWildcardTerm("test[value]*end");
    assertThat(term5.toRegexPattern(), is("^test\\[value\\].*end$"));
  }

  @Test
  public void testIsMultiWildcardPattern_QuestionMarkOnlyPatterns() {
    // Test with multi-embedded regex enabled
    DefaultSqlSearchQueryContribution withRegex = new DefaultSqlSearchQueryContribution();
    withRegex.init(new SearchMappingService(Arrays.asList(new DefaultSearchMappings())), true);

    // Patterns with only ? (no asterisks) should NOT use RegexWildcardTerm
    // These patterns are handled by the tokenizer which splits on ?
    assertThat(splitForField(withRegex, GROUP_RAW, "commons?"), contains(new WildcardTerm("commons")));
    assertThat(splitForField(withRegex, GROUP_RAW, "commons?lang3?"),
        contains(new WildcardTerm("commons"), new WildcardTerm("lang3")));
    assertThat(splitForField(withRegex, GROUP_RAW, "test?value"),
        contains(new WildcardTerm("test"), new LenientTerm("value")));

    // Verify that patterns with asterisks still trigger RegexWildcardTerm
    assertThat(splitForField(withRegex, GROUP_RAW, "commons*lang"), contains(new RegexWildcardTerm("commons*lang")));
    assertThat(splitForField(withRegex, GROUP_RAW, "commons*lang*"), contains(new RegexWildcardTerm("commons*lang*")));
  }

  @Test
  public void testRegexWildcardOnlyForFieldsThatSupportRegex() {
    // NEXUS-51662: RegexWildcardTerm should only be created for fields with VARCHAR columns
    DefaultSqlSearchQueryContribution withRegex = new DefaultSqlSearchQueryContribution();
    withRegex.init(new SearchMappingService(Arrays.asList(new DefaultSearchMappings())), true);

    // VARCHAR fields (NAMESPACE, NAME, VERSION) should create RegexWildcardTerm for multi-wildcard patterns
    assertThat(splitForField(withRegex, GROUP_RAW, "org*apache*"), contains(new RegexWildcardTerm("org*apache*")));
    assertThat(splitForField(withRegex, "name", "commons*lang*"), contains(new RegexWildcardTerm("commons*lang*")));
    assertThat(splitForField(withRegex, "version", "1*2*3"), contains(new RegexWildcardTerm("1*2*3")));

    // TSVECTOR-only fields (KEYWORDS) should NOT create RegexWildcardTerm for multi-wildcard patterns
    // The pattern will be split into separate WildcardTerms by the tokenizer
    assertThat(splitForField(withRegex, "q", "test*value*"),
        contains(new WildcardTerm("test"), new WildcardTerm("value")));
  }

  @Test
  public void testRegexWildcardTermEdgeCases() {
    // Test wildcard-only patterns
    RegexWildcardTerm term1 = new RegexWildcardTerm("*");
    assertThat(term1.toRegexPattern(), is("^.*$"));

    RegexWildcardTerm term2 = new RegexWildcardTerm("?");
    assertThat(term2.toRegexPattern(), is("^.$"));

    RegexWildcardTerm term3 = new RegexWildcardTerm("**");
    assertThat(term3.toRegexPattern(), is("^.*.*$"));

    RegexWildcardTerm term4 = new RegexWildcardTerm("*?*");
    assertThat(term4.toRegexPattern(), is("^.*..*$"));

    // Test pattern starting with wildcard
    RegexWildcardTerm term5 = new RegexWildcardTerm("*foo");
    assertThat(term5.toRegexPattern(), is("^.*foo$"));

    // Test pattern with multiple consecutive wildcards
    RegexWildcardTerm term6 = new RegexWildcardTerm("a**b");
    assertThat(term6.toRegexPattern(), is("^a.*.*b$"));

    // Test escaped wildcards - should match literal characters
    RegexWildcardTerm term7 = new RegexWildcardTerm("foo\\*bar");
    assertThat(term7.toRegexPattern(), is("^foo\\*bar$"));

    RegexWildcardTerm term8 = new RegexWildcardTerm("foo\\?bar");
    assertThat(term8.toRegexPattern(), is("^foo\\?bar$"));

    // Test trailing backslash
    RegexWildcardTerm term9 = new RegexWildcardTerm("foo\\");
    assertThat(term9.toRegexPattern(), is("^foo\\\\$"));
  }

  private Collection<SingleValueTerm<?>> split(final String value) {
    return splitForField(GROUP_RAW, value);
  }

  private Collection<SingleValueTerm<?>> splitForField(final String field, final String value) {
    return splitForField(underTest, field, value);
  }

  private Collection<SingleValueTerm<?>> splitForField(
      final DefaultSqlSearchQueryContribution contribution,
      final String field,
      final String value)
  {
    Optional<Expression> expression = contribution.createPredicate(new SearchFilter(field, value));

    assertTrue(expression.isPresent());

    Expression expr = expression.get();

    // Handle SqlClause (multiple terms) or SqlPredicate (single term)
    if (expr instanceof SqlClause) {
      SqlClause clause = (SqlClause) expr;
      return clause.expressions()
          .stream()
          .map(e -> ((SqlPredicate) e).getTerm())
          .flatMap(term -> {
            if (term instanceof StringTerm st) {
              return Stream.of(st);
            }
            else if (term instanceof TermCollection tc) {
              return tc.get().stream();
            }
            return Stream.empty();
          })
          .toList();
    }
    else if (expr instanceof SqlPredicate) {
      Term term = ((SqlPredicate) expr).getTerm();

      if (term instanceof StringTerm st) {
        return List.of(st);
      }
      else if (term instanceof TermCollection tc) {
        return tc.get();
      }
    }

    fail("Unknown expression: " + expr);
    return null;
  }
}
