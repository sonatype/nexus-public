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

import java.util.List;
import java.util.Set;

import org.sonatype.nexus.rest.ValidationErrorsException;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SqlSearchWildcardValidatorTest
{
  @Before
  public void setUp() {
    // Reset to default before each test
    SqlSearchWildcardValidator.setMinAllowedSymbolsToSearch(3);
  }

  @After
  public void tearDown() {
    // Reset to default after each test
    SqlSearchWildcardValidator.setMinAllowedSymbolsToSearch(3);
  }

  @Test
  public void getValidTokens_withValidToken_returnsValidToken() {
    Set<String> validTokens = SqlSearchWildcardValidator.getValidTokens(List.of("search*"));
    Assertions.assertThat(validTokens).isEqualTo(Set.of("search*"));
  }

  @Test
  public void getValidTokens_withInvalidTokens() {
    Object[][] scenarios = new Object[][]{
        {"*maven", "Leading wildcards are prohibited"},
        {"1.*", "3 characters or more are required with a trailing wildcard (*)"},
        {"/*", "3 characters or more are required with a trailing wildcard (*)"}
    };
    for (Object[] scenario : scenarios) {
      String invalidToken = (String) scenario[0];
      String expectedMessage = (String) scenario[1];
      Assertions.assertThatThrownBy(() -> SqlSearchWildcardValidator.getValidTokens(List.of(invalidToken)))
          .isInstanceOf(ValidationErrorsException.class)
          .hasMessageContaining(expectedMessage);
    }
  }

  @Test
  public void validateToken_withInvalidTokens() {
    Object[][] scenarios = new Object[][]{
        {"*maven", "Leading wildcards are prohibited"},
        {"1.*", "3 characters or more are required with a trailing wildcard (*)"},
        {"/*", "3 characters or more are required with a trailing wildcard (*)"}
    };
    for (Object[] scenario : scenarios) {
      String invalidToken = (String) scenario[0];
      String expectedMessage = (String) scenario[1];
      Assertions.assertThatThrownBy(() -> SqlSearchWildcardValidator.validateToken(invalidToken))
          .isInstanceOf(ValidationErrorsException.class)
          .hasMessageContaining(expectedMessage);
    }
  }

  @Test
  public void containsWildcards_withWildcard() {
    Assertions.assertThat(SqlSearchWildcardValidator.containsWildcards("search*")).isTrue();
    Assertions.assertThat(SqlSearchWildcardValidator.containsWildcards("search?")).isTrue();
  }

  @Test
  public void containsWildcards_withoutWildcard() {
    Assertions.assertThat(SqlSearchWildcardValidator.containsWildcards("search")).isFalse();
  }

  @Test
  public void testConfigurableMinPrefixLength() {
    // Test with default configuration (3 characters)
    Assertions.assertThat(SqlSearchWildcardValidator.getMinAllowedSymbolsToSearch()).isEqualTo(3);

    // Test that short prefixes fail with default
    Assertions.assertThatThrownBy(() -> SqlSearchWildcardValidator.getValidTokens(List.of("1.*")))
        .isInstanceOf(ValidationErrorsException.class)
        .hasMessageContaining("3 characters or more");

    // Change configuration to allow shorter prefixes (simulating Spring injection)
    SqlSearchWildcardValidator.setMinAllowedSymbolsToSearch(1);

    // Now short prefixes should work
    Set<String> validTokens = SqlSearchWildcardValidator.getValidTokens(List.of("1.*"));
    Assertions.assertThat(validTokens).isEqualTo(Set.of("1.*"));

    // Version patterns commonly used
    validTokens = SqlSearchWildcardValidator.getValidTokens(List.of("2.*"));
    Assertions.assertThat(validTokens).isEqualTo(Set.of("2.*"));

    validTokens = SqlSearchWildcardValidator.getValidTokens(List.of("3.7*"));
    Assertions.assertThat(validTokens).isEqualTo(Set.of("3.7*"));
  }

  @Test
  public void testSetMinAllowedSymbolsWithInvalidValue() {
    // Test invalid values through the configuration constructor (which validates)
    // Value < 1 should be rejected and use default
    SqlSearchWildcardConfiguration config = new SqlSearchWildcardConfiguration(0);
    Assertions.assertThat(SqlSearchWildcardValidator.getMinAllowedSymbolsToSearch())
        .isEqualTo(3);

    // Value > 100 should be rejected and use default
    config = new SqlSearchWildcardConfiguration(200);
    Assertions.assertThat(SqlSearchWildcardValidator.getMinAllowedSymbolsToSearch())
        .isEqualTo(3);

    // Negative value should be rejected and use default
    config = new SqlSearchWildcardConfiguration(-5);
    Assertions.assertThat(SqlSearchWildcardValidator.getMinAllowedSymbolsToSearch())
        .isEqualTo(3);
  }

  @Test
  public void testMinPrefixLength_One() {
    SqlSearchWildcardValidator.setMinAllowedSymbolsToSearch(1);

    // Single character prefix should work
    Set<String> validTokens = SqlSearchWildcardValidator.getValidTokens(List.of("1.*"));
    Assertions.assertThat(validTokens).isEqualTo(Set.of("1.*"));

    // Still should reject leading wildcards
    Assertions.assertThatThrownBy(() -> SqlSearchWildcardValidator.getValidTokens(List.of("*test")))
        .isInstanceOf(ValidationErrorsException.class)
        .hasMessageContaining("Leading wildcards are prohibited");
  }

  @Test
  public void testMinPrefixLength_Two() {
    SqlSearchWildcardValidator.setMinAllowedSymbolsToSearch(2);

    // Single character should fail (1* has only 1 char before wildcard)
    Assertions.assertThatThrownBy(() -> SqlSearchWildcardValidator.getValidTokens(List.of("1*")))
        .isInstanceOf(ValidationErrorsException.class)
        .hasMessageContaining("2 characters or more");

    // Two characters should work (1.* has 2 chars before wildcard: "1" and ".")
    Set<String> validTokens = SqlSearchWildcardValidator.getValidTokens(List.of("1.*"));
    Assertions.assertThat(validTokens).isEqualTo(Set.of("1.*"));

    // Three characters should work
    validTokens = SqlSearchWildcardValidator.getValidTokens(List.of("3.1*"));
    Assertions.assertThat(validTokens).isEqualTo(Set.of("3.1*"));
  }

  @Test
  public void testConfigurationIntegration() {
    // Simulate what Spring does: create configuration and call init
    SqlSearchWildcardConfiguration config = new SqlSearchWildcardConfiguration(1);

    // Verify validator is configured
    Assertions.assertThat(SqlSearchWildcardValidator.getMinAllowedSymbolsToSearch()).isEqualTo(1);

    // Verify it works
    Set<String> validTokens = SqlSearchWildcardValidator.getValidTokens(List.of("1.*"));
    Assertions.assertThat(validTokens).isEqualTo(Set.of("1.*"));
  }

  @Test
  public void testConfigurationWithDefault() {
    // Simulate default configuration (no property set)
    SqlSearchWildcardConfiguration config = new SqlSearchWildcardConfiguration(3);

    // Should use default
    Assertions.assertThat(SqlSearchWildcardValidator.getMinAllowedSymbolsToSearch()).isEqualTo(3);
  }

  @Test
  public void testMinPrefixLength_Hundred() {
    // Test boundary value at maximum (100)
    SqlSearchWildcardConfiguration config = new SqlSearchWildcardConfiguration(100);

    // Should accept value 100
    Assertions.assertThat(SqlSearchWildcardValidator.getMinAllowedSymbolsToSearch()).isEqualTo(100);

    // Verify getter returns correct value
    Assertions.assertThat(SqlSearchWildcardValidator.getMinAllowedSymbolsToSearch()).isEqualTo(100);
  }
}
