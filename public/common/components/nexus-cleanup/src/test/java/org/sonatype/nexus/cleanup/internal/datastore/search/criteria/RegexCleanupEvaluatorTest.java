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
package org.sonatype.nexus.cleanup.internal.datastore.search.criteria;

import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegexCleanupEvaluatorTest

{

  @Mock
  private Repository repository;

  private RegexCleanupEvaluator underTest;

  @BeforeEach
  void setup() {
    underTest = new RegexCleanupEvaluator();
  }

  @ParameterizedTest
  @CsvSource({
      ".*\\.jar, test.jar, true",
      ".*\\.jar, test.txt, false",
      "^src/.*, src/main.java, true",
      "^src/.*, bin/main.class, false"
  })
  void testValidRegexPatterns(final String regex, final String assetPath, final boolean expectedResult) {
    Asset asset = mock(Asset.class);
    when(asset.path()).thenReturn(assetPath);

    Predicate<Asset> predicate = underTest.getPredicate(repository, regex);
    assertThat(predicate.test(asset), is(expectedResult));
  }

  @Test
  void testInvalidRegexThrowsException() {
    String invalidRegex = "[a-z";// Invalid regex pattern
    when(repository.getName()).thenReturn("test-repo");
    assertThrows(RuntimeException.class, () -> underTest.getPredicate(repository, invalidRegex));
  }
}
