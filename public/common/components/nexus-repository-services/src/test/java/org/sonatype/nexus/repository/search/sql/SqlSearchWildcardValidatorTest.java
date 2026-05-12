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
import org.junit.Test;

public class SqlSearchWildcardValidatorTest
{

  @Test
  public void getValidTokens_withValidToken_returnsValidToken() {
    Set<String> validTokens = SqlSearchWildcardValidator.getValidTokens(List.of("search*"));
    Assertions.assertThat(validTokens).isEqualTo(Set.of("search*"));
  }

  @Test
  public void getValidTokens_withInvalidTokens() {
    Object[][] scenarios = new Object[][]{
        {"*maven", "Leading wildcards are prohibited"},
        {"1.*", "3 characters or more are required with a trailing wildcard (*"},
        {"/*", "3 characters or more are required with a trailing wildcard (*"}
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
        {"1.*", "3 characters or more are required with a trailing wildcard (*"},
        {"/*", "3 characters or more are required with a trailing wildcard (*"}
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
}
