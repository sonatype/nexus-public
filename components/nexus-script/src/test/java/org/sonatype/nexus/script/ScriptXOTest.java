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
package org.sonatype.nexus.script;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ScriptXOTest
    extends TestSupport
{

  private static final String NAME_FIELD_PATTERN_MESSAGE =
      "Only letters, digits, underscores(_), hyphens(-), and dots(.) are allowed and may not start with underscore or dot.";

  private static final String MUST_NOT_BE_EMPTY = "must not be empty";

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  private static final List<String> INVALID_NAMES = Arrays.asList(
      "#", "*", " ", "'", "\\", "/", "?", "<", ">", "|", "\r", "\n", "\t", ",", "+", "@", "&", "å", "©", "不", "β", "خ",
      "_leadingUnderscore", ".", "..");

  private static final List<String> VALID_NAMES = Arrays.asList("Foo_1.2-3", "foo.", "-0.", "a", "1");

  private ScriptXO createScriptXO(String name, String content, String type) {
    ScriptXO scriptXO = new ScriptXO();
    scriptXO.setName(name);
    scriptXO.setContent(content);
    scriptXO.setType(type);
    return scriptXO;
  }

  private void validateAndAssertError(
      Set<ConstraintViolation<ScriptXO>> errors,
      String expectedPath,
      String expectedMessage)
  {
    assertThat(errors.size(), is(1));
    ConstraintViolation<ScriptXO> violation = errors.iterator().next();
    assertThat(violation.getPropertyPath().toString(), is(expectedPath));
    assertThat(violation.getMessage(), is(expectedMessage));
  }

  @Test
  public void nameAndContentAreAlwaysRequired() {
    ScriptXO scriptXO = createScriptXO(null, "content", "type");

    // Validate 'name' is required
    Set<ConstraintViolation<ScriptXO>> nameErrors = validator.validate(scriptXO);
    validateAndAssertError(nameErrors, "name", MUST_NOT_BE_EMPTY);

    // Validate 'content' is required
    scriptXO.setName("validName");
    scriptXO.setContent(null);
    Set<ConstraintViolation<ScriptXO>> contentErrors = validator.validate(scriptXO);
    validateAndAssertError(contentErrors, "content", MUST_NOT_BE_EMPTY);
  }

  @Test
  public void invalidNamesShouldFailValidation() {
    for (String name : INVALID_NAMES) {
      ScriptXO scriptXO = createScriptXO(name, "content", "type");
      Set<ConstraintViolation<ScriptXO>> errors = validator.validate(scriptXO);
      validateAndAssertError(errors, "name", NAME_FIELD_PATTERN_MESSAGE);
    }
  }

  @Test
  public void validNamesShouldPassValidation() {
    for (String name : VALID_NAMES) {
      ScriptXO scriptXO = createScriptXO(name, "content", "type");
      Set<ConstraintViolation<ScriptXO>> errors = validator.validate(scriptXO);
      assertThat(errors.size(), is(0));
    }
  }
}
