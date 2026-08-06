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
package org.sonatype.nexus.repository.proxy;

import java.util.Map;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link UrlEscapeRulesParser}
 */
public class UrlEscapeRulesParserTest
{
  @Test
  public void testParseRepositoryRules_RepositoryPresent() {
    String config = "pypi-proxy={+:%2B,#:%3F},maven-central={+:%2B}";
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(config, "pypi-proxy");

    assertThat(rules, is(notNullValue()));
    assertThat(rules, hasEntry("+", "%2B"));
    assertThat(rules, hasEntry("#", "%3F"));
    assertThat(rules.size(), is(2));
  }

  @Test
  public void testParseRepositoryRules_RepositoryAbsent() {
    String config = "pypi-proxy={+:%2B}";
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(config, "other-repo");

    assertThat(rules, is(nullValue()));
  }

  @Test
  public void testParseRepositoryRules_EmptyRules() {
    String config = "pypi-proxy={},maven-central={+:%2B}";
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(config, "pypi-proxy");

    assertThat(rules, is(notNullValue()));
    assertTrue(rules.isEmpty());
  }

  @Test
  public void testParseRepositoryRules_NullConfig() {
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(null, "any-repo");

    assertThat(rules, is(nullValue()));
  }

  @Test
  public void testParseRepositoryRules_EmptyConfig() {
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules("", "any-repo");

    assertThat(rules, is(nullValue()));
  }

  @Test
  public void testParseRepositoryRules_WhitespaceConfig() {
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules("   ", "any-repo");

    assertThat(rules, is(nullValue()));
  }

  @Test
  public void testParseRepositoryRules_NullRepositoryName() {
    String config = "pypi-proxy={+:%2B}";
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(config, null);

    assertThat(rules, is(nullValue()));
  }

  @Test
  public void testParseRepositoryRules_EmptyRepositoryName() {
    String config = "pypi-proxy={+:%2B}";
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(config, "");

    assertThat(rules, is(nullValue()));
  }

  @Test
  public void testParseRepositoryRules_MalformedConfig_MissingCloseBrace() {
    String config = "pypi-proxy={+:%2B,maven-central={missing-brace";
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(config, "pypi-proxy");

    // Malformed config (missing closing brace) cannot be parsed - returns null
    assertThat(rules, is(nullValue()));
  }

  @Test
  public void testParseRepositoryRules_SpecialCharacters() {
    // Note: ':' cannot be used as a pattern because it's the delimiter
    // Test with characters that can be parsed correctly, including space
    String config = "my-repo={%:%25,+:plus,#:hash, :%20}";
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(config, "my-repo");

    assertThat(rules, is(notNullValue()));
    assertThat(rules, hasEntry("%", "%25"));
    assertThat(rules, hasEntry("+", "plus"));
    assertThat(rules, hasEntry("#", "hash"));
    assertThat(rules, hasEntry(" ", "%20"));
    assertThat(rules.size(), is(4));
  }

  @Test
  public void testParseRepositoryRules_RepositoryNameWithSpecialChars() {
    String config = "my-pypi-repo-123={+:%2B}";
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(config, "my-pypi-repo-123");

    assertThat(rules, is(notNullValue()));
    assertThat(rules, hasEntry("+", "%2B"));
  }

  @Test
  public void testParseRepositoryRules_WhitespaceInRules() {
    String config = "repo={ + : %2B , # : %3F }";
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(config, "repo");

    assertThat(rules, is(notNullValue()));
    assertThat(rules, hasEntry("+", "%2B"));
    assertThat(rules, hasEntry("#", "%3F"));
  }

  @Test
  public void testParseRepositoryRules_MalformedRule_Skipped() {
    String config = "repo={+:%2B,invalid-rule,#:%3F}";
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(config, "repo");

    assertThat(rules, is(notNullValue()));
    assertThat(rules, hasEntry("+", "%2B"));
    assertThat(rules, hasEntry("#", "%3F"));
    assertThat(rules, not(hasKey("invalid-rule")));
  }

  @Test
  public void testParseRepositoryRules_EmptyRulePart_Skipped() {
    String config = "repo={+:%2B,,#:%3F}";
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(config, "repo");

    assertThat(rules, is(notNullValue()));
    assertThat(rules, hasEntry("+", "%2B"));
    assertThat(rules, hasEntry("#", "%3F"));
    assertThat(rules.size(), is(2));
  }

  @Test
  public void testParseRepositoryRules_MultipleRepositories() {
    String config = "pypi-proxy={+:%2B,#:%3F},maven-central={+:%2B},npm-registry={}";

    Map<String, String> pypiRules = UrlEscapeRulesParser.parseRepositoryRules(config, "pypi-proxy");
    assertThat(pypiRules, is(notNullValue()));
    assertThat(pypiRules, hasEntry("+", "%2B"));
    assertThat(pypiRules, hasEntry("#", "%3F"));

    Map<String, String> mavenRules = UrlEscapeRulesParser.parseRepositoryRules(config, "maven-central");
    assertThat(mavenRules, is(notNullValue()));
    assertThat(mavenRules, hasEntry("+", "%2B"));

    Map<String, String> npmRules = UrlEscapeRulesParser.parseRepositoryRules(config, "npm-registry");
    assertThat(npmRules, is(notNullValue()));
    assertTrue(npmRules.isEmpty());
  }

  @Test
  public void testParseRepositoryRules_ComplexConfig() {
    String config = "repo1={+:%2B,#:%3F,?:%3F},repo2={::%3A},repo3={}";

    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(config, "repo1");

    assertThat(rules, is(notNullValue()));
    assertThat(rules, hasEntry("+", "%2B"));
    assertThat(rules, hasEntry("#", "%3F"));
    assertThat(rules, hasEntry("?", "%3F"));
    assertThat(rules.size(), is(3));
  }

  // Security validation tests

  @Test
  public void testParseRepositoryRules_ConfigExceedsMaxLength() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 11000; i++) {
      sb.append("a");
    }
    String hugeConfig = sb.toString();

    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(hugeConfig, "repo");
    assertThat(rules, is(nullValue()));
  }

  @Test
  public void testParseRepositoryRules_MaxRulesLimit() {
    StringBuilder sb = new StringBuilder("repo={");
    for (int i = 0; i < 60; i++) {
      if (i > 0)
        sb.append(",");
      sb.append("char").append(i).append(":value").append(i);
    }
    sb.append("}");
    String config = sb.toString();

    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(config, "repo");
    assertThat(rules, is(notNullValue()));
    // Should stop at 50 rules
    assertTrue(rules.size() <= 50);
  }

  @Test
  public void testParseRepositoryRules_InvalidPercentEncoding() {
    String config = "repo={+:%ZZ}"; // Invalid percent encoding
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(config, "repo");

    // Invalid replacement should be skipped
    assertThat(rules, is(notNullValue()));
    assertTrue(rules.isEmpty());
  }

  @Test
  public void testParseRepositoryRules_ValidPercentEncoding() {
    String config = "repo={+:%2B,#:%23, :%20}";
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(config, "repo");

    assertThat(rules, is(notNullValue()));
    assertThat(rules, hasEntry("+", "%2B"));
    assertThat(rules, hasEntry("#", "%23"));
    assertThat(rules, hasEntry(" ", "%20"));
  }

  @Test
  public void testParseRepositoryRules_PlainTextReplacement() {
    String config = "repo={+:plus,#:hash}";
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(config, "repo");

    assertThat(rules, is(notNullValue()));
    assertThat(rules, hasEntry("+", "plus"));
    assertThat(rules, hasEntry("#", "hash"));
  }

  @Test
  public void testParseRepositoryRules_ControlCharacterRejected() {
    // Control characters (except tab, newline, carriage return) should be rejected
    String config = "repo={+:" + (char) 1 + "}";
    Map<String, String> rules = UrlEscapeRulesParser.parseRepositoryRules(config, "repo");

    assertThat(rules, is(notNullValue()));
    assertTrue(rules.isEmpty());
  }

  @Test
  public void testParseRepositoryRules_PatternCaching() {
    String config = "repo1={+:%2B},repo2={#:%23}";

    // Parse twice for same repo - should use cached pattern
    Map<String, String> rules1 = UrlEscapeRulesParser.parseRepositoryRules(config, "repo1");
    Map<String, String> rules2 = UrlEscapeRulesParser.parseRepositoryRules(config, "repo1");

    assertThat(rules1, is(notNullValue()));
    assertThat(rules2, is(notNullValue()));
    assertThat(rules1, hasEntry("+", "%2B"));
    assertThat(rules2, hasEntry("+", "%2B"));

    // Clear cache and verify it still works
    UrlEscapeRulesParser.clearCache();
    Map<String, String> rules3 = UrlEscapeRulesParser.parseRepositoryRules(config, "repo1");
    assertThat(rules3, is(notNullValue()));
    assertThat(rules3, hasEntry("+", "%2B"));
  }
}
