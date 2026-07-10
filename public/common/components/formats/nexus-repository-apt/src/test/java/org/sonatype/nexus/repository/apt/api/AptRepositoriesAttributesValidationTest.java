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
package org.sonatype.nexus.repository.apt.api;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for APT repository attributes validation rules.
 * Validates that proxy repositories have optional distribution fields while hosted repositories require it.
 */
class AptRepositoriesAttributesValidationTest
{
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void testHostedAttributes_validDistribution() {
    AptHostedRepositoriesAttributes attributes = new AptHostedRepositoriesAttributes("focal");

    Set<ConstraintViolation<AptHostedRepositoriesAttributes>> violations = validator.validate(attributes);

    assertThat("Valid distribution should pass validation", violations, is(empty()));
  }

  @Test
  void testHostedAttributes_nullDistributionFailsValidation() {
    AptHostedRepositoriesAttributes attributes = new AptHostedRepositoriesAttributes(null);

    Set<ConstraintViolation<AptHostedRepositoriesAttributes>> violations = validator.validate(attributes);

    assertThat("Null distribution should fail validation for hosted repository",
        violations, hasSize(1));
    assertThat("Should have violation on distribution field",
        violations.iterator().next().getPropertyPath().toString(), is("distribution"));
  }

  @Test
  void testHostedAttributes_emptyDistributionFailsValidation() {
    AptHostedRepositoriesAttributes attributes = new AptHostedRepositoriesAttributes("");

    Set<ConstraintViolation<AptHostedRepositoriesAttributes>> violations = validator.validate(attributes);

    assertThat("Empty distribution should fail validation for hosted repository",
        violations, hasSize(1));
    assertThat("Should have violation on distribution field",
        violations.iterator().next().getPropertyPath().toString(), is("distribution"));
  }

  @Test
  void testProxyAttributes_validDistributionAndFlat() {
    AptProxyRepositoriesAttributes attributes = new AptProxyRepositoriesAttributes("jammy", false, false);

    Set<ConstraintViolation<AptProxyRepositoriesAttributes>> violations = validator.validate(attributes);

    assertThat("Valid proxy attributes should pass validation", violations, is(empty()));
  }

  @Test
  void testProxyAttributes_nullDistributionIsValid() {
    // This is the key test: distribution is OPTIONAL for proxy repositories
    AptProxyRepositoriesAttributes attributes = new AptProxyRepositoriesAttributes(null, false, false);

    Set<ConstraintViolation<AptProxyRepositoriesAttributes>> violations = validator.validate(attributes);

    assertThat("Null distribution should be VALID for proxy repository (it's optional)",
        violations, is(empty()));
  }

  @Test
  void testProxyAttributes_emptyDistributionIsValid() {
    // Empty string is also valid for proxy repositories
    AptProxyRepositoriesAttributes attributes = new AptProxyRepositoriesAttributes("", false, false);

    Set<ConstraintViolation<AptProxyRepositoriesAttributes>> violations = validator.validate(attributes);

    assertThat("Empty distribution should be VALID for proxy repository (it's optional)",
        violations, is(empty()));
  }

  @Test
  void testProxyAttributes_flatRepositoryWithoutDistribution() {
    AptProxyRepositoriesAttributes attributes = new AptProxyRepositoriesAttributes(null, true, false);

    Set<ConstraintViolation<AptProxyRepositoriesAttributes>> violations = validator.validate(attributes);

    assertThat("Flat proxy repository should work without distribution",
        violations, is(empty()));
  }

  @Test
  void testProxyAttributes_nullFlatFailsValidation() {
    // Flat is required for proxy repositories
    AptProxyRepositoriesAttributes attributes = new AptProxyRepositoriesAttributes("focal", null, false);

    Set<ConstraintViolation<AptProxyRepositoriesAttributes>> violations = validator.validate(attributes);

    assertThat("Null flat should fail validation for proxy repository",
        violations, hasSize(1));
    assertThat("Should have violation on flat field",
        violations.iterator().next().getPropertyPath().toString(), is("flat"));
  }

  @Test
  void testProxyAttributes_withDistributionAndFlat() {
    AptProxyRepositoriesAttributes attributes = new AptProxyRepositoriesAttributes("bionic", true, false);

    Set<ConstraintViolation<AptProxyRepositoriesAttributes>> violations = validator.validate(attributes);

    assertThat("Proxy with both distribution and flat should pass validation",
        violations, is(empty()));
  }

  @Test
  void testHostedAttributes_constructorAndGetters() {
    AptHostedRepositoriesAttributes attributes = new AptHostedRepositoriesAttributes("focal");

    assertThat("Distribution should be accessible", attributes.getDistribution(), is("focal"));
  }

  @Test
  void testHostedAttributes_acceptsNullDistributionButValidationWillFail() {
    AptHostedRepositoriesAttributes attributes = new AptHostedRepositoriesAttributes(null);

    assertThat("Constructor accepts null (validation happens later)",
        attributes.getDistribution(), is(nullValue()));
  }

  @Test
  void testHostedAttributes_acceptsEmptyDistributionButValidationWillFail() {
    AptHostedRepositoriesAttributes attributes = new AptHostedRepositoriesAttributes("");

    assertThat("Constructor accepts empty string (validation happens later)",
        attributes.getDistribution(), is(""));
  }

  @Test
  void testProxyAttributes_constructorAndGettersWithBothFields() {
    AptProxyRepositoriesAttributes attributes = new AptProxyRepositoriesAttributes("jammy", false, false);

    assertThat("Distribution should be accessible", attributes.getDistribution(), is("jammy"));
    assertThat("Flat should be accessible", attributes.getFlat(), is(false));
  }

  @Test
  void testProxyAttributes_acceptsNullDistribution() {
    AptProxyRepositoriesAttributes attributes = new AptProxyRepositoriesAttributes(null, false, false);

    assertThat("Proxy repository allows null distribution",
        attributes.getDistribution(), is(nullValue()));
    assertThat("Flat should still be accessible", attributes.getFlat(), is(false));
  }

  @Test
  void testProxyAttributes_acceptsEmptyDistribution() {
    AptProxyRepositoriesAttributes attributes = new AptProxyRepositoriesAttributes("", false, false);

    assertThat("Proxy repository allows empty distribution", attributes.getDistribution(), is(""));
    assertThat("Flat should still be accessible", attributes.getFlat(), is(false));
  }

  @Test
  void testProxyAttributes_flatRepositoryWithoutDistributionGetter() {
    AptProxyRepositoriesAttributes attributes = new AptProxyRepositoriesAttributes(null, true, false);

    assertThat("Flat proxy repository works without distribution",
        attributes.getDistribution(), is(nullValue()));
    assertThat("Flat should be true", attributes.getFlat(), is(true));
  }

  @Test
  void testProxyAttributes_acceptsNullFlatButValidationWillFail() {
    AptProxyRepositoriesAttributes attributes = new AptProxyRepositoriesAttributes("focal", null, false);

    assertThat("Distribution should be set", attributes.getDistribution(), is("focal"));
    assertThat("Constructor accepts null flat (validation happens later)",
        attributes.getFlat(), is(nullValue()));
  }

  @Test
  void testProxyAttributes_gettersWorkIndependently() {
    AptProxyRepositoriesAttributes attributes = new AptProxyRepositoriesAttributes("bionic", true, false);

    assertThat("Proxy distribution getter should work", attributes.getDistribution(), is("bionic"));
    assertThat("Flat getter should work", attributes.getFlat(), is(true));
  }
}
