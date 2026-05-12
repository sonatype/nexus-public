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

import java.util.Optional;

import org.sonatype.nexus.repository.rest.sql.SearchField;
import org.sonatype.nexus.repository.search.query.SearchFilter;
import org.sonatype.nexus.repository.search.sql.query.syntax.BooleanTerm;
import org.sonatype.nexus.repository.search.sql.query.syntax.Expression;
import org.sonatype.nexus.repository.search.sql.query.syntax.Operand;
import org.sonatype.nexus.repository.search.sql.query.syntax.SqlPredicate;
import org.sonatype.nexus.rest.ValidationErrorsException;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrereleaseSqlSearchQueryContributionTest
{
  private PrereleaseSqlSearchQueryContribution underTest = new PrereleaseSqlSearchQueryContribution();

  @Test
  void shouldReturnEmptyForNullFilter() {
    Optional<Expression> result = underTest.createPredicate(null);

    assertFalse(result.isPresent());
  }

  @Test
  void shouldCreatePredicateForTrueValue() {
    Optional<Expression> result = underTest.createPredicate(
        new SearchFilter(PrereleaseSqlSearchQueryContribution.NAME, "true"));

    assertTrue(result.isPresent());
    assertThat(result.get(), is(new SqlPredicate(Operand.EQ, SearchField.PRERELEASE, new BooleanTerm(true))));
  }

  @Test
  void shouldCreatePredicateForFalseValue() {
    Optional<Expression> result = underTest.createPredicate(
        new SearchFilter(PrereleaseSqlSearchQueryContribution.NAME, "false"));

    assertTrue(result.isPresent());
    assertThat(result.get(), is(new SqlPredicate(Operand.EQ, SearchField.PRERELEASE, new BooleanTerm(false))));
  }

  @Test
  void shouldBeCaseInsensitiveForTrue() {
    Optional<Expression> result = underTest.createPredicate(
        new SearchFilter(PrereleaseSqlSearchQueryContribution.NAME, "True"));

    assertTrue(result.isPresent());
    assertThat(result.get(), is(new SqlPredicate(Operand.EQ, SearchField.PRERELEASE, new BooleanTerm(true))));
  }

  @Test
  void shouldBeCaseInsensitiveForFalse() {
    Optional<Expression> result = underTest.createPredicate(
        new SearchFilter(PrereleaseSqlSearchQueryContribution.NAME, "FALSE"));

    assertTrue(result.isPresent());
    assertThat(result.get(), is(new SqlPredicate(Operand.EQ, SearchField.PRERELEASE, new BooleanTerm(false))));
  }

  @Test
  void shouldHandleWhitespaceInValue() {
    Optional<Expression> result = underTest.createPredicate(
        new SearchFilter(PrereleaseSqlSearchQueryContribution.NAME, "  true  "));

    assertTrue(result.isPresent());
    assertThat(result.get(), is(new SqlPredicate(Operand.EQ, SearchField.PRERELEASE, new BooleanTerm(true))));
  }

  @Test
  void shouldThrowValidationErrorForInvalidValue() {
    SearchFilter filter = new SearchFilter(PrereleaseSqlSearchQueryContribution.NAME, "invalid");

    ValidationErrorsException exception = assertThrows(ValidationErrorsException.class,
        () -> underTest.createPredicate(filter));

    assertThat(exception.getMessage(), is("Pre-release only supports true or false"));
  }

  @Test
  void shouldThrowValidationErrorForNumericValue() {
    SearchFilter filter = new SearchFilter(PrereleaseSqlSearchQueryContribution.NAME, "1");

    ValidationErrorsException exception = assertThrows(ValidationErrorsException.class,
        () -> underTest.createPredicate(filter));

    assertThat(exception.getMessage(), is("Pre-release only supports true or false"));
  }

  @Test
  void shouldThrowValidationErrorForEmptyString() {
    SearchFilter filter = new SearchFilter(PrereleaseSqlSearchQueryContribution.NAME, "");

    ValidationErrorsException exception = assertThrows(ValidationErrorsException.class,
        () -> underTest.createPredicate(filter));

    assertThat(exception.getMessage(), is("Pre-release only supports true or false"));
  }
}
