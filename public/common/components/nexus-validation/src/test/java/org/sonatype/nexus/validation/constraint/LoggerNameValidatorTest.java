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

package org.sonatype.nexus.validation.constraint;

import org.junit.Test;

import static org.junit.Assert.assertThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class LoggerNameValidatorTest

{
  private static final String INVALID_CHARS_MESSAGE =
      "Logger name cannot include <, >, &, ', \", /, newline, or tab characters";

  private static final String REQUIRED_MESSAGE = "Logger name is required";

  // ===== VALID CASES (Should Pass) =====

  @Test
  public void testValidRootLogger() {
    LoggerNameValidator.validate("ROOT");
  }

  @Test
  public void testValidSimpleNames() {
    LoggerNameValidator.validate("a");
    LoggerNameValidator.validate("A");
    LoggerNameValidator.validate("test");
    LoggerNameValidator.validate("MyLogger");
  }

  @Test
  public void testValidPackageNames() {
    LoggerNameValidator.validate("com.example.MyClass");
    LoggerNameValidator.validate("org.apache.sonu.http");
    LoggerNameValidator.validate("org.springframework.web");
  }

  @Test
  public void testValidWithSpecialAllowedChars() {
    LoggerNameValidator.validate("com.example.test_class");
    LoggerNameValidator.validate("com.example.test$inner");
    LoggerNameValidator.validate("org.test123.logger456");
  }

  @Test
  public void testValidLongName() {
    LoggerNameValidator.validate("com.verylongpackagename.with.many.dots.and.segments.MyVeryLongLoggerClassName");
  }

  // ===== INVALID CASES - NULL/EMPTY =====

  @Test
  public void testNullLoggerName() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LoggerNameValidator.validate(null));
    assertThat(exception.getMessage(), is(REQUIRED_MESSAGE));
  }

  @Test
  public void testEmptyLoggerName() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LoggerNameValidator.validate(""));
    assertThat(exception.getMessage(), is(REQUIRED_MESSAGE));
  }

  @Test
  public void testWhitespaceOnlyLoggerName() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LoggerNameValidator.validate("   "));
    assertThat(exception.getMessage(), is(REQUIRED_MESSAGE));
  }

  // ===== INVALID CASES - XML-BREAKING CHARACTERS =====

  @Test
  public void testLessThanCharacter() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LoggerNameValidator.validate("com.example<test"));
    assertThat(exception.getMessage(), is(INVALID_CHARS_MESSAGE));
  }

  @Test
  public void testGreaterThanCharacter() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LoggerNameValidator.validate("com.example>test"));
    assertThat(exception.getMessage(), is(INVALID_CHARS_MESSAGE));
  }

  @Test
  public void testAmpersandCharacter() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LoggerNameValidator.validate("com.example&test"));
    assertThat(exception.getMessage(), is(INVALID_CHARS_MESSAGE));
  }

  @Test
  public void testSingleQuoteCharacter() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LoggerNameValidator.validate("com.example'test"));
    assertThat(exception.getMessage(), is(INVALID_CHARS_MESSAGE));
  }

  @Test
  public void testDoubleQuoteCharacter() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LoggerNameValidator.validate("com.example\"test"));
    assertThat(exception.getMessage(), is(INVALID_CHARS_MESSAGE));
  }

  // ===== INVALID CASES - URL-BREAKING CHARACTERS =====

  @Test
  public void testForwardSlashCharacter() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LoggerNameValidator.validate("com.example/test"));
    assertThat(exception.getMessage(), is(INVALID_CHARS_MESSAGE));
  }

  @Test
  public void testNewlineCharacter() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LoggerNameValidator.validate("com.example\ntest"));
    assertThat(exception.getMessage(), is(INVALID_CHARS_MESSAGE));
  }

  @Test
  public void testCarriageReturnCharacter() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LoggerNameValidator.validate("com.example\rtest"));
    assertThat(exception.getMessage(), is(INVALID_CHARS_MESSAGE));
  }

  @Test
  public void testTabCharacter() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LoggerNameValidator.validate("com.example\ttest"));
    assertThat(exception.getMessage(), is(INVALID_CHARS_MESSAGE));
  }

  // ===== INVALID CASES - COMBINATIONS =====

  @Test
  public void testMultipleInvalidCharacters() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LoggerNameValidator.validate("com<example>test&more"));
    assertThat(exception.getMessage(), is(INVALID_CHARS_MESSAGE));
  }

  @Test
  public void testInvalidCharacterAtBeginning() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LoggerNameValidator.validate("<com.example.test"));
    assertThat(exception.getMessage(), is(INVALID_CHARS_MESSAGE));
  }

  @Test
  public void testInvalidCharacterAtEnd() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LoggerNameValidator.validate("com.example.test>"));
    assertThat(exception.getMessage(), is(INVALID_CHARS_MESSAGE));
  }

  // ===== EDGE CASES =====

  @Test
  public void testOnlyInvalidCharacter() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LoggerNameValidator.validate("<"));
    assertThat(exception.getMessage(), is(INVALID_CHARS_MESSAGE));
  }

  @Test
  public void testValidCharactersSurroundingInvalid() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LoggerNameValidator.validate("valid<invalid>valid"));
    assertThat(exception.getMessage(), is(INVALID_CHARS_MESSAGE));
  }

}
