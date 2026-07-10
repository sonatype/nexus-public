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
 * {@link StringTextFormField} tests.
 */
public class StringTextFormFieldTest
{
  private static final String ID = "stringId";

  private static final String LABEL = "stringLabel";

  private static final String HELP_TEXT = "stringHelpText";

  private static final String REGEX = ".*";

  private static final String INITIAL_VALUE = "stringInitialValue";

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectId() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectLabel() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getLabel(), equalTo(LABEL));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectHelpText() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectRegexValidation() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getRegexValidation(), equalTo(REGEX));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_AndRequiredTrue_Expect_RequiredIsTrue() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertTrue(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectId() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectLabel() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getLabel(), equalTo(LABEL));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectHelpText() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_RegexValidationIsNull() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getRegexValidation(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_AndRequiredFalse_Expect_RequiredIsFalse() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, false);
    assertFalse(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_CorrectId() {
    StringTextFormField field = new StringTextFormField(ID);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_LabelIsNull() {
    StringTextFormField field = new StringTextFormField(ID);
    assertThat(field.getLabel(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_HelpTextIsNull() {
    StringTextFormField field = new StringTextFormField(ID);
    assertThat(field.getHelpText(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_RequiredIsFalse() {
    StringTextFormField field = new StringTextFormField(ID);
    assertFalse(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_InitialValueIsNull() {
    StringTextFormField field = new StringTextFormField(ID);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_GettingType_Expect_StringConstant() {
    StringTextFormField field = new StringTextFormField(ID);
    assertThat(field.getType(), equalTo("string"));
  }

  @Test
  public void when_SettingInitialValue_Expect_FluentReturnsSameInstance() {
    StringTextFormField field = new StringTextFormField(ID);
    StringTextFormField result = field.withInitialValue(INITIAL_VALUE);
    assertThat(result, is(sameInstance(field)));
  }

  @Test
  public void when_SettingInitialValue_Expect_InitialValueReflected() {
    StringTextFormField field = new StringTextFormField(ID).withInitialValue(INITIAL_VALUE);
    assertThat(field.getInitialValue(), equalTo(INITIAL_VALUE));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_AndRequiredFalse_Expect_RequiredIsFalse() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, false, REGEX);
    assertFalse(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_InitialValueIsNull() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_DisabledIsFalse() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertFalse(field.isDisabled());
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_ReadOnlyIsFalse() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertFalse(field.isReadOnly());
  }

  @Test
  public void when_CreatingNew_WithAllArgs_AndRequiredTrue_Expect_RequiredIsTrue() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, true);
    assertTrue(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_InitialValueIsNull() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_DisabledIsFalse() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, false);
    assertFalse(field.isDisabled());
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_ReadOnlyIsFalse() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, false);
    assertFalse(field.isReadOnly());
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_RegexValidationIsNull() {
    StringTextFormField field = new StringTextFormField(ID);
    assertThat(field.getRegexValidation(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_DisabledIsFalse() {
    StringTextFormField field = new StringTextFormField(ID);
    assertFalse(field.isDisabled());
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_ReadOnlyIsFalse() {
    StringTextFormField field = new StringTextFormField(ID);
    assertFalse(field.isReadOnly());
  }

  @Test
  public void when_CreatingNew_WithNullId_Expect_IdIsNull() {
    StringTextFormField field = new StringTextFormField(null);
    assertThat(field.getId(), is(nullValue()));
  }

  @Test
  public void when_GettingType_FromAllArgsConstructor_Expect_StringConstant() {
    StringTextFormField field = new StringTextFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getType(), equalTo("string"));
  }

  @Test
  public void when_SettingInitialValueToNull_Expect_FluentReturnsSameInstance() {
    StringTextFormField field = new StringTextFormField(ID).withInitialValue(INITIAL_VALUE);
    StringTextFormField result = field.withInitialValue(null);
    assertThat(result, is(sameInstance(field)));
  }

  @Test
  public void when_SettingInitialValueToNull_Expect_InitialValueIsNull() {
    StringTextFormField field = new StringTextFormField(ID).withInitialValue(INITIAL_VALUE).withInitialValue(null);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_SettingInitialValueRepeatedly_Expect_LastValueReflected() {
    StringTextFormField field = new StringTextFormField(ID)
        .withInitialValue("first")
        .withInitialValue("second");
    assertThat(field.getInitialValue(), equalTo("second"));
  }

  @Test
  public void when_CreatingNew_Expect_AllowAutocompleteIsFalse() {
    StringTextFormField field = new StringTextFormField(ID);
    assertFalse(field.getAllowAutocomplete());
  }

  @Test
  public void when_CreatingNew_Expect_AttributesEmptyAndNotNull() {
    StringTextFormField field = new StringTextFormField(ID);
    assertThat(field.getAttributes(), is(notNullValue()));
    assertTrue(field.getAttributes().isEmpty());
  }

  @Test
  public void when_GettingAttributes_Expect_SameInstanceOnRepeatedCalls() {
    StringTextFormField field = new StringTextFormField(ID);
    assertThat(field.getAttributes(), is(sameInstance(field.getAttributes())));
  }
}
