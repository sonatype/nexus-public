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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class ScriptXOTest
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

  @Test
  public void defaultConstructorLeavesFieldsNull() {
    ScriptXO scriptXO = new ScriptXO();

    assertThat(scriptXO.getName(), is(nullValue()));
    assertThat(scriptXO.getContent(), is(nullValue()));
    assertThat(scriptXO.getType(), is(nullValue()));
  }

  @Test
  public void parameterizedConstructorSetsAllFields() {
    ScriptXO scriptXO = new ScriptXO("myName", "myContent", "myType");

    assertThat(scriptXO.getName(), is("myName"));
    assertThat(scriptXO.getContent(), is("myContent"));
    assertThat(scriptXO.getType(), is("myType"));
  }

  @Test
  public void settersUpdateFields() {
    ScriptXO scriptXO = new ScriptXO();

    scriptXO.setName("name");
    scriptXO.setContent("content");
    scriptXO.setType("type");

    assertThat(scriptXO.getName(), is("name"));
    assertThat(scriptXO.getContent(), is("content"));
    assertThat(scriptXO.getType(), is("type"));
  }

  @Test
  public void toStringContainsAllFields() {
    ScriptXO scriptXO = new ScriptXO("name", "content", "type");

    assertThat(scriptXO.toString(), is("ScriptXO{name='name', content='content', type='type'}"));
  }

  @Test
  public void equalsIsReflexive() {
    ScriptXO scriptXO = new ScriptXO("name", "content", "type");

    assertThat(scriptXO.equals(scriptXO), is(true));
  }

  @Test
  public void equalsReturnsFalseForNull() {
    ScriptXO scriptXO = new ScriptXO("name", "content", "type");

    assertThat(scriptXO.equals(null), is(false));
  }

  @Test
  public void equalsReturnsFalseForDifferentType() {
    ScriptXO scriptXO = new ScriptXO("name", "content", "type");

    assertThat(scriptXO.equals("not a ScriptXO"), is(false));
  }

  @Test
  public void equalsReturnsTrueForEquivalentInstances() {
    ScriptXO first = new ScriptXO("name", "content", "type");
    ScriptXO second = new ScriptXO("name", "content", "type");

    assertThat(first.equals(second), is(true));
  }

  @Test
  public void equalsReturnsFalseWhenNameDiffers() {
    ScriptXO first = new ScriptXO("name", "content", "type");
    ScriptXO second = new ScriptXO("otherName", "content", "type");

    assertThat(first.equals(second), is(false));
  }

  @Test
  public void equalsReturnsFalseWhenContentDiffers() {
    ScriptXO first = new ScriptXO("name", "content", "type");
    ScriptXO second = new ScriptXO("name", "otherContent", "type");

    assertThat(first.equals(second), is(false));
  }

  @Test
  public void equalsReturnsFalseWhenTypeDiffers() {
    ScriptXO first = new ScriptXO("name", "content", "type");
    ScriptXO second = new ScriptXO("name", "content", "otherType");

    assertThat(first.equals(second), is(false));
  }

  @Test
  public void hashCodeIsEqualForEquivalentInstances() {
    ScriptXO first = new ScriptXO("name", "content", "type");
    ScriptXO second = new ScriptXO("name", "content", "type");

    assertThat(first.hashCode(), is(second.hashCode()));
  }

  @Test
  public void hashCodeMatchesObjectsHash() {
    ScriptXO scriptXO = new ScriptXO("name", "content", "type");

    assertThat(scriptXO.hashCode(), is(Objects.hash("name", "content", "type")));
  }

  @Test
  public void hashCodeDiffersForDifferentValues() {
    ScriptXO first = new ScriptXO("name", "content", "type");
    ScriptXO second = new ScriptXO("otherName", "content", "type");

    assertThat(first.hashCode(), is(not(second.hashCode())));
  }

  @Test
  public void typeIsAlwaysRequired() {
    ScriptXO scriptXO = createScriptXO("validName", "content", null);

    Set<ConstraintViolation<ScriptXO>> errors = validator.validate(scriptXO);

    validateAndAssertError(errors, "type", MUST_NOT_BE_EMPTY);
  }

  @Test
  public void emptyContentFailsValidation() {
    ScriptXO scriptXO = createScriptXO("validName", "", "type");

    Set<ConstraintViolation<ScriptXO>> errors = validator.validate(scriptXO);

    validateAndAssertError(errors, "content", MUST_NOT_BE_EMPTY);
  }

  @Test
  public void emptyTypeFailsValidation() {
    ScriptXO scriptXO = createScriptXO("validName", "content", "");

    Set<ConstraintViolation<ScriptXO>> errors = validator.validate(scriptXO);

    validateAndAssertError(errors, "type", MUST_NOT_BE_EMPTY);
  }

  @Test
  public void emptyNameFailsNotEmptyAndPatternConstraints() {
    ScriptXO scriptXO = createScriptXO("", "content", "type");

    Set<ConstraintViolation<ScriptXO>> errors = validator.validate(scriptXO);

    assertThat(errors.size(), is(2));
    for (ConstraintViolation<ScriptXO> violation : errors) {
      assertThat(violation.getPropertyPath().toString(), is("name"));
    }
    Set<String> messages = errors.stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toSet());
    assertThat(messages, containsInAnyOrder(MUST_NOT_BE_EMPTY, NAME_FIELD_PATTERN_MESSAGE));
  }

  @Test
  public void equalsReturnsTrueForTwoDefaultInstances() {
    assertThat(new ScriptXO().equals(new ScriptXO()), is(true));
  }

  @Test
  public void hashCodeMatchesObjectsHashForDefaultInstance() {
    assertThat(new ScriptXO().hashCode(), is(Objects.hash(null, null, null)));
  }
}
