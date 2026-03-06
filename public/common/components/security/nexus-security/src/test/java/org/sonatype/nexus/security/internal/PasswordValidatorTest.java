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

package org.sonatype.nexus.security.internal;

import org.sonatype.nexus.rest.ValidationErrorsException;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThrows;

public class PasswordValidatorTest
{
  // Default validation tests (no custom config)

  @Test
  public void defaultValidation_rejectsPasswordShorterThan8Characters() {
    PasswordValidator underTest = new PasswordValidator(null, null);

    ValidationErrorsException exception = assertThrows(
        ValidationErrorsException.class,
        () -> underTest.validate("short"));

    assertThat(exception.getMessage(), containsString("Password must be at least 8 characters"));
  }

  @Test
  public void defaultValidation_rejectsPasswordWithExactly7Characters() {
    PasswordValidator underTest = new PasswordValidator(null, null);

    ValidationErrorsException exception = assertThrows(
        ValidationErrorsException.class,
        () -> underTest.validate("1234567"));

    assertThat(exception.getMessage(), containsString("Password must be at least 8 characters"));
  }

  @Test
  public void defaultValidation_acceptsPasswordWithExactly8Characters() {
    PasswordValidator underTest = new PasswordValidator(null, null);

    underTest.validate("12345678");
  }

  @Test
  public void defaultValidation_acceptsPasswordLongerThan8Characters() {
    PasswordValidator underTest = new PasswordValidator(null, null);

    underTest.validate("mylongpassword123");
  }

  @Test
  public void defaultValidation_acceptsNullPassword() {
    PasswordValidator underTest = new PasswordValidator(null, null);

    underTest.validate(null);
  }

  @Test
  public void defaultValidation_rejectsEmptyPassword() {
    PasswordValidator underTest = new PasswordValidator(null, null);

    ValidationErrorsException exception = assertThrows(
        ValidationErrorsException.class,
        () -> underTest.validate(""));

    assertThat(exception.getMessage(), containsString("Password must be at least 8 characters"));
  }

  @Test
  public void defaultValidation_rejectsShortPasswordOnChange() {
    PasswordValidator underTest = new PasswordValidator(null, null);

    ValidationErrorsException exception = assertThrows(
        ValidationErrorsException.class,
        () -> underTest.validate("abc"));

    assertThat(exception.getMessage(), containsString("Password must be at least 8 characters"));
  }

  // Custom validator override tests

  @Test
  public void customValidator_usesCustomRegexWhenConfigured() {
    PasswordValidator underTest = new PasswordValidator("[A-Z]+", null);

    ValidationErrorsException exception = assertThrows(
        ValidationErrorsException.class,
        () -> underTest.validate("lowercase"));

    assertThat(exception.getMessage(), containsString("Password does not match corporate policy"));
  }

  @Test
  public void customValidator_acceptsMatchingPasswords() {
    PasswordValidator underTest = new PasswordValidator("[A-Z]+", null);

    underTest.validate("UPPERCASE");
  }

  @Test
  public void customValidator_displaysCustomErrorMessage() {
    PasswordValidator underTest = new PasswordValidator("[0-9]+", "Password must contain only numbers");

    ValidationErrorsException exception = assertThrows(
        ValidationErrorsException.class,
        () -> underTest.validate("letters"));

    assertThat(exception.getMessage(), containsString("Password must contain only numbers"));
  }

  @Test
  public void customValidator_permissiveRegexDisablesValidation() {
    PasswordValidator underTest = new PasswordValidator(".*", null);

    underTest.validate("ab");
  }

  @Test
  public void customValidator_permissiveRegexAcceptsEmptyPassword() {
    PasswordValidator underTest = new PasswordValidator(".*", null);

    underTest.validate("");
  }

  // Error message logic tests

  @Test
  public void errorMessage_defaultWithNoCustomValidator() {
    PasswordValidator underTest = new PasswordValidator(null, null);

    ValidationErrorsException exception = assertThrows(
        ValidationErrorsException.class,
        () -> underTest.validate("short"));

    assertThat(exception.getMessage(), containsString("Password must be at least 8 characters"));
  }

  @Test
  public void errorMessage_corporatePolicyWithCustomValidator() {
    PasswordValidator underTest = new PasswordValidator("[a-z]+", null);

    ValidationErrorsException exception = assertThrows(
        ValidationErrorsException.class,
        () -> underTest.validate("123"));

    assertThat(exception.getMessage(), containsString("Password does not match corporate policy"));
  }

  @Test
  public void errorMessage_customMessageOverridesDefault() {
    PasswordValidator underTest = new PasswordValidator(null, "Custom default message");

    ValidationErrorsException exception = assertThrows(
        ValidationErrorsException.class,
        () -> underTest.validate("short"));

    assertThat(exception.getMessage(), containsString("Custom default message"));
  }

  @Test
  public void errorMessage_customMessageOverridesCorporatePolicy() {
    PasswordValidator underTest = new PasswordValidator("[a-z]+", "Custom policy message");

    ValidationErrorsException exception = assertThrows(
        ValidationErrorsException.class,
        () -> underTest.validate("123"));

    assertThat(exception.getMessage(), containsString("Custom policy message"));
  }

  // Edge cases

  @Test
  public void whitespaceValidatorStringUsesDefault() {
    // Whitespace-only validator string is treated as blank, so default validation applies
    PasswordValidator underTest = new PasswordValidator("   ", null);

    // "password" has 8 characters, so it passes default validation
    underTest.validate("password");

    // Short password should fail with default error message
    ValidationErrorsException exception = assertThrows(
        ValidationErrorsException.class,
        () -> underTest.validate("short"));

    assertThat(exception.getMessage(), containsString("Password must be at least 8 characters"));
  }

  @Test
  public void whitespaceErrorMessageUsesDefault() {
    PasswordValidator underTest = new PasswordValidator(null, "   ");

    ValidationErrorsException exception = assertThrows(
        ValidationErrorsException.class,
        () -> underTest.validate("short"));

    assertThat(exception.getMessage(), containsString("Password must be at least 8 characters"));
  }

  @Test
  public void emptyErrorMessageUsesDefault() {
    PasswordValidator underTest = new PasswordValidator(null, "");

    ValidationErrorsException exception = assertThrows(
        ValidationErrorsException.class,
        () -> underTest.validate("short"));

    assertThat(exception.getMessage(), containsString("Password must be at least 8 characters"));
  }
}
