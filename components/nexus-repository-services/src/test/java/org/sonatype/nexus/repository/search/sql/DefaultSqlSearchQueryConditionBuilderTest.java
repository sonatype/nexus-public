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
package org.sonatype.nexus.repository.search.sql;

import java.util.Map;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.mockito.InjectMocks;

import static com.google.common.collect.ImmutableSet.of;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;

public class DefaultSqlSearchQueryConditionBuilderTest
    extends TestSupport
{
  @InjectMocks
  private DefaultSqlSearchQueryConditionBuilder underTest;

  @Test
  public void shouldCombineConditionsIntoOne() {
    Set<SqlSearchQueryCondition> conditions = of(
        aSqlSearchCondition("component LIKE #{component0} OR component LIKE #{component1}",
            ImmutableMap.of("component0", "def%", "component1", "%hamcrest")),
        aSqlSearchCondition("version IN (#{version0},#{version1},#{version2})",
            ImmutableMap.of("version0", "1.0", "version1", "1.1", "version2", "1.3")),
        aSqlSearchCondition("group = #{group}", ImmutableMap.of("group", "junit")));

    final SqlSearchQueryCondition actual = underTest.combine(conditions);

    assertThat(actual.getSqlConditionFormat(),
        is("(component LIKE #{component0} OR component LIKE #{component1} " +
            "OR version IN (#{version0},#{version1},#{version2}) OR group = #{group})"));

    assertThat(actual.getValues().keySet(),
        containsInAnyOrder("component0", "component1", "version0", "version1", "version2", "group"));

    assertThat(actual.getValues().values(), containsInAnyOrder("def%", "%hamcrest", "1.0", "1.1", "1.3", "junit"));
  }

  @Test
  public void shouldCreateAnEqualsCondition() {
    final String value = "repo1";
    final String expectedFormat = "repository_name = #{filterParams.repository_name}";
    final Map<String, String> expectedValues = ImmutableMap.of("repository_name", value);

    assertThat(underTest.condition("repository_name", value),
        is(aSqlSearchCondition(expectedFormat, expectedValues)));

    assertThat(underTest.condition("repository_name", singleton(value)),
        is(aSqlSearchCondition(expectedFormat, expectedValues)));
  }

  @Test
  public void shouldBeASingleLikeCondition() {
    assertThat(underTest.condition("repository_name", singleton("repo1*")),
        is(aSqlSearchCondition("repository_name LIKE #{filterParams.repository_name}",
            ImmutableMap.of("repository_name", "repo1%"))));
  }

  @Test
  public void shouldCreateAnInCondition() {
    SqlSearchQueryCondition actual = underTest.condition("repository_name", of("repo1", "repo2", "repo3"));

    assertThat(actual.getSqlConditionFormat(), is("repository_name IN (#{filterParams.repository_name0}," +
        "#{filterParams.repository_name1},#{filterParams.repository_name2})"));
    assertThat(actual.getValues().keySet(),
        containsInAnyOrder("repository_name0", "repository_name1", "repository_name2"));
    assertThat(actual.getValues().values(), containsInAnyOrder("repo1", "repo2", "repo3"));
  }

  @Test
  public void shouldBeLikeForZeroOrMore() {
    final SqlSearchQueryCondition actual = underTest.condition("component", of("abc*", "*def", "*ghi*", "?jkl*"));

    assertThat(actual.getSqlConditionFormat(),
        is("(component LIKE #{filterParams.component0} OR component LIKE #{filterParams.component1} " +
            "OR component LIKE #{filterParams.component2} OR component LIKE #{filterParams.component3})"));

    assertThat(actual.getValues().keySet(),
        is(containsInAnyOrder("component0", "component1", "component2", "component3")));

    assertThat(actual.getValues().values(),
        containsInAnyOrder("abc%", "%def", "%ghi%", "_jkl%"));
  }

  @Test
  public void shouldBeLikeForAnySingleCharacter() {
    final SqlSearchQueryCondition actual2 = underTest.condition("component", of("abc?", "?def", "?ghi?", "?j?l"));
    assertThat(actual2.getSqlConditionFormat(),
        is("(component LIKE #{filterParams.component0} OR component LIKE #{filterParams.component1} OR " +
            "component LIKE #{filterParams.component2} OR component LIKE #{filterParams.component3})"));

    assertThat(actual2.getValues().keySet(),
        is(ImmutableSet.of("component0", "component1", "component2", "component3")));

    assertThat(actual2.getValues().values(),
        containsInAnyOrder("abc_", "_def", "_ghi_", "_j_l"));
  }

  @Test
  public void shouldBeExactIfContainsEscapedStar() {
    final SqlSearchQueryCondition actual = underTest.condition("component", of("abc\\*", "\\*def", "ghi\\*jk"));
    assertThat(actual.getSqlConditionFormat(),
        is("component IN (#{filterParams.component0},#{filterParams.component1},#{filterParams.component2})"));

    assertThat(actual.getValues().keySet(),
        is(ImmutableSet.of("component0", "component1", "component2")));

    assertThat(actual.getValues().values(),
        containsInAnyOrder("abc*", "*def", "ghi*jk"));
  }

  @Test
  public void shouldNotReplaceEscapedSymbolWithPercentIfWildcard() {
    final SqlSearchQueryCondition actual =
        underTest.condition("component", of("abc\\**", "\\*def*", "ghi\\*jk*", "?j\\??l"));

    assertThat(actual.getSqlConditionFormat(),
        is("(component LIKE #{filterParams.component0} OR component LIKE #{filterParams.component1} OR " +
            "component LIKE #{filterParams.component2} OR component LIKE #{filterParams.component3})"));

    assertThat(actual.getValues().keySet(),
        is(ImmutableSet.of("component0", "component1", "component2", "component3")));

    assertThat(actual.getValues().values(),
        containsInAnyOrder("abc*%", "*def%", "ghi*jk%", "_j?_l"));
  }

  @Test
  public void shouldEscapePercentIfWildcard() {
    final SqlSearchQueryCondition actual =
        underTest.condition("component", of("abc%*", "%def?", "ghi%jk", "jkl%", "m?n_op*"));

    assertThat(actual.getSqlConditionFormat(),
        is("(component IN (#{filterParams.component0},#{filterParams.component1}) OR " +
            "(component LIKE #{filterParams.component2} OR component LIKE #{filterParams.component3} " +
            "OR component LIKE #{filterParams.component4}))"));

    assertThat(actual.getValues().keySet(),
        is(ImmutableSet.of("component0", "component1", "component2", "component3", "component4")));

    assertThat(actual.getValues().values(),
        containsInAnyOrder("abc%%", "%def_", "ghi%jk", "jkl%", "m_n_op%"));
  }
  @Test
  public void shouldEscapeUnderscoreIfWildcard() {
    final SqlSearchQueryCondition actual =
        underTest.condition("component", of("abc_*", "_def?", "ghi_jk", "jkl_"));

    assertThat(actual.getSqlConditionFormat(),
        is("(component IN (#{filterParams.component0},#{filterParams.component1}) OR " +
            "(component LIKE #{filterParams.component2} OR component LIKE #{filterParams.component3}))"));

    assertThat(actual.getValues().keySet(),
        is(ImmutableSet.of("component0", "component1", "component2", "component3")));

    assertThat(actual.getValues().values(),
        containsInAnyOrder("abc_%", "_def_", "ghi_jk", "jkl_"));
  }

  @Test
  public void shouldEscapeTrailingBackslashIfWildcard() {
    final SqlSearchQueryCondition actual =
        underTest.condition("component", of("abc*\\", "?def\\", "ghi*\\jk", "\\jkl?"));

    assertThat(actual.getSqlConditionFormat(),
        is("(component LIKE #{filterParams.component0} OR component LIKE #{filterParams.component1} OR " +
            "component LIKE #{filterParams.component2} OR component LIKE #{filterParams.component3})"));

    assertThat(actual.getValues().keySet(),
        is(ImmutableSet.of("component0", "component1", "component2", "component3")));

    assertThat(actual.getValues().values(),
        containsInAnyOrder("abc%\\", "_def\\", "ghi%\\jk", "\\jkl_"));
  }

  @Test
  public void shouldCreateExactAndWildcardCondition() {
    SqlSearchQueryCondition actual =
        underTest.condition("repository_name", of("repo1", "repo2", "repo3", "maven*", "raw?"));

    assertThat(actual.getSqlConditionFormat(), is(
        "(repository_name IN (#{filterParams.repository_name0},#{filterParams.repository_name1}," +
            "#{filterParams.repository_name2}) OR (repository_name LIKE #{filterParams.repository_name3} " +
            "OR repository_name LIKE #{filterParams.repository_name4}))"));

    assertThat(actual.getValues().keySet(),
        containsInAnyOrder("repository_name0", "repository_name1", "repository_name2",
            "repository_name3", "repository_name4"));

    assertThat(actual.getValues().values(), containsInAnyOrder("repo1", "repo2", "repo3", "maven%", "raw_"));
  }

  @Test
  public void shouldPrefixParametersInEqualsCondition() {
    String value = "repo1";
    String parameterPrefix = "abc_";
    String expectedFormat = "repository_name = #{filterParams.abc_repository_name}";
    Map<String, String> expectedValues = ImmutableMap.of("abc_repository_name", value);

    assertThat(underTest.condition("repository_name", value, parameterPrefix),
        is(aSqlSearchCondition(expectedFormat, expectedValues)));

    assertThat(underTest.condition("repository_name", singleton(value), parameterPrefix),
        is(aSqlSearchCondition(expectedFormat, expectedValues)));
  }

  @Test
  public void shouldReplaceWildcard() {
    assertThat(underTest.replaceWildcards("foo?"),
        is("foo_"));

    assertThat(underTest.replaceWildcards("foo???"),
        is("foo___"));

    assertThat(underTest.replaceWildcards("bar*"),
        is("bar%"));
  }

  @Test
  public void shouldBuildConditionWithEmptyValue() {
    assertThat(underTest.conditionWithEmptyValue("field"),
        is(aSqlSearchCondition("field = #{filterParams.field} OR field IS NULL", ImmutableMap.of("field", ""))));
  }

  private SqlSearchQueryCondition aSqlSearchCondition(final String conditionFormat, final Map<String, String> values) {
    return new SqlSearchQueryCondition(conditionFormat, values);
  }
}
