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
package org.sonatype.nexus.formfields;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link PasswordFormField} tests.
 */
public class PasswordFormFieldTest
{
  private static final String ID = "passwordId";

  private static final String LABEL = "passwordLabel";

  private static final String HELP_TEXT = "passwordHelpText";

  private static final String REGEX = ".*";

  private static final String INITIAL_VALUE = "secret";

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectId() {
    PasswordFormField field = new PasswordFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectLabel() {
    PasswordFormField field = new PasswordFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getLabel(), equalTo(LABEL));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectHelpText() {
    PasswordFormField field = new PasswordFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectRegexValidation() {
    PasswordFormField field = new PasswordFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getRegexValidation(), equalTo(REGEX));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_AndRequiredTrue_Expect_RequiredIsTrue() {
    PasswordFormField field = new PasswordFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertTrue(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectId() {
    PasswordFormField field = new PasswordFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectLabel() {
    PasswordFormField field = new PasswordFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getLabel(), equalTo(LABEL));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectHelpText() {
    PasswordFormField field = new PasswordFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_RegexValidationIsNull() {
    PasswordFormField field = new PasswordFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getRegexValidation(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_AndRequiredFalse_Expect_RequiredIsFalse() {
    PasswordFormField field = new PasswordFormField(ID, LABEL, HELP_TEXT, false);
    assertFalse(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_CorrectId() {
    PasswordFormField field = new PasswordFormField(ID);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_LabelIsNull() {
    PasswordFormField field = new PasswordFormField(ID);
    assertThat(field.getLabel(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_HelpTextIsNull() {
    PasswordFormField field = new PasswordFormField(ID);
    assertThat(field.getHelpText(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_RequiredIsFalse() {
    PasswordFormField field = new PasswordFormField(ID);
    assertFalse(field.isRequired());
  }

  @Test
  public void when_GettingType_Expect_PasswordConstant() {
    PasswordFormField field = new PasswordFormField(ID);
    assertThat(field.getType(), equalTo("password"));
  }

  @Test
  public void when_SettingInitialValue_Expect_FluentReturnsSameInstance() {
    PasswordFormField field = new PasswordFormField(ID);
    PasswordFormField result = field.withInitialValue(INITIAL_VALUE);
    assertThat(result, is(sameInstance(field)));
  }

  @Test
  public void when_SettingInitialValue_Expect_InitialValueReflected() {
    PasswordFormField field = new PasswordFormField(ID).withInitialValue(INITIAL_VALUE);
    assertThat(field.getInitialValue(), equalTo(INITIAL_VALUE));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_InitialValueIsNull() {
    PasswordFormField field = new PasswordFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_AndOptional_Expect_RequiredIsFalse() {
    PasswordFormField field = new PasswordFormField(ID, LABEL, HELP_TEXT, FormField.OPTIONAL, REGEX);
    assertFalse(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithAllArgs_AndMandatory_Expect_RequiredIsTrue() {
    PasswordFormField field = new PasswordFormField(ID, LABEL, HELP_TEXT, FormField.MANDATORY);
    assertTrue(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_InitialValueIsNull() {
    PasswordFormField field = new PasswordFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_RegexValidationIsNull() {
    PasswordFormField field = new PasswordFormField(ID);
    assertThat(field.getRegexValidation(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_InitialValueIsNull() {
    PasswordFormField field = new PasswordFormField(ID);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_Expect_NotDisabledByDefault() {
    PasswordFormField field = new PasswordFormField(ID);
    assertFalse(field.isDisabled());
  }

  @Test
  public void when_CreatingNew_Expect_NotReadOnlyByDefault() {
    PasswordFormField field = new PasswordFormField(ID);
    assertFalse(field.isReadOnly());
  }

  @Test
  public void when_CreatingNew_Expect_AllowAutocompleteIsFalseByDefault() {
    PasswordFormField field = new PasswordFormField(ID);
    assertFalse(field.getAllowAutocomplete());
  }

  @Test
  public void when_CreatingNew_Expect_AttributesNotNullAndEmptyByDefault() {
    PasswordFormField field = new PasswordFormField(ID);
    assertThat(field.getAttributes(), is(notNullValue()));
    assertTrue(field.getAttributes().isEmpty());
  }

  @Test
  public void when_GettingAttributes_Expect_SameInstanceOnRepeatedCalls() {
    PasswordFormField field = new PasswordFormField(ID);
    assertThat(field.getAttributes(), is(sameInstance(field.getAttributes())));
  }

  @Test
  public void when_Instantiated_Expect_IsEncryptedMarker() {
    PasswordFormField field = new PasswordFormField(ID);
    assertTrue(field instanceof Encrypted);
  }

  @Test
  public void when_SettingInitialValueNull_Expect_FluentReturnsSameInstance() {
    PasswordFormField field = new PasswordFormField(ID);
    PasswordFormField result = field.withInitialValue(null);
    assertThat(result, is(sameInstance(field)));
  }

  @Test
  public void when_SettingInitialValueNull_Expect_InitialValueIsNull() {
    PasswordFormField field = new PasswordFormField(ID).withInitialValue(null);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_SettingInitialValueTwice_Expect_LatestValueRetained() {
    PasswordFormField field = new PasswordFormField(ID).withInitialValue(INITIAL_VALUE).withInitialValue("other");
    assertThat(field.getInitialValue(), equalTo("other"));
  }
}
