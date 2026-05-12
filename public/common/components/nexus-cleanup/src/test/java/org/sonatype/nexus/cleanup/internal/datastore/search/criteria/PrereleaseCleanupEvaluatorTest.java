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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.utils.PreReleaseEvaluator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrereleaseCleanupEvaluatorTest

{
  @Mock
  private Repository repository;

  @Mock
  private PreReleaseEvaluator preReleaseEvaluator;

  private PrereleaseCleanupEvaluator underTest;

  private MockedStatic<QualifierUtil> mockedStatic;

  @BeforeEach
  void setup() {
    mockedStatic = Mockito.mockStatic(QualifierUtil.class);
    List<PreReleaseEvaluator> preReleaseEvaluators = List.of(preReleaseEvaluator);
    Map<String, PreReleaseEvaluator> matchers = Collections.singletonMap("test-format", preReleaseEvaluator);
    when(QualifierUtil.buildQualifierBeanMap(preReleaseEvaluators)).thenReturn(matchers);
    underTest = new PrereleaseCleanupEvaluator(preReleaseEvaluators);
    setupRepositoryAndEvaluator();
  }

  @AfterEach
  void tearDown() {
    mockedStatic.close();
  }

  private void setupRepositoryAndEvaluator() {
    Format formatMock = mock(Format.class);
    when(repository.getFormat()).thenReturn(formatMock);
    when(formatMock.getValue()).thenReturn("test-format");
  }

  @ParameterizedTest
  @CsvSource({
      "true, true, true",
      "true, false, false",
      "false, false, true"
  })
  void testPredicateBehavior(final String value, final boolean isPreRelease, final boolean expectedResult) {
    when(preReleaseEvaluator.isPreRelease(any(Component.class), any(Iterable.class))).thenReturn(isPreRelease);
    BiPredicate<Component, Iterable<Asset>> predicate = underTest.getPredicate(repository, value);
    assertThat(predicate.test(mock(Component.class), emptyList()), is(expectedResult));
  }
}
