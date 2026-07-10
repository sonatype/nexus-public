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
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * {@link NumberTextFormField} tests.
 */
public class NumberTextFormFieldTest
{
  private static final String ID = "numberId";

  private static final String LABEL = "numberLabel";

  private static final String HELP_TEXT = "numberHelpText";

  private static final String REGEX = "[0-9]+";

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectId() {
    NumberTextFormField field = new NumberTextFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectLabel() {
    NumberTextFormField field = new NumberTextFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getLabel(), equalTo(LABEL));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectHelpText() {
    NumberTextFormField field = new NumberTextFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectRegexValidation() {
    NumberTextFormField field = new NumberTextFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getRegexValidation(), equalTo(REGEX));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_AndRequiredTrue_Expect_RequiredIsTrue() {
    NumberTextFormField field = new NumberTextFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertTrue(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectId() {
    NumberTextFormField field = new NumberTextFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectLabel() {
    NumberTextFormField field = new NumberTextFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getLabel(), equalTo(LABEL));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectHelpText() {
    NumberTextFormField field = new NumberTextFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_RegexValidationIsNull() {
    NumberTextFormField field = new NumberTextFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getRegexValidation(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_AndRequiredFalse_Expect_RequiredIsFalse() {
    NumberTextFormField field = new NumberTextFormField(ID, LABEL, HELP_TEXT, false);
    assertFalse(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_CorrectId() {
    NumberTextFormField field = new NumberTextFormField(ID);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_LabelIsNull() {
    NumberTextFormField field = new NumberTextFormField(ID);
    assertThat(field.getLabel(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_HelpTextIsNull() {
    NumberTextFormField field = new NumberTextFormField(ID);
    assertThat(field.getHelpText(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_RequiredIsFalse() {
    NumberTextFormField field = new NumberTextFormField(ID);
    assertFalse(field.isRequired());
  }

  @Test
  public void when_CreatingNew_Expect_MinimumValueIsNull() {
    NumberTextFormField field = new NumberTextFormField(ID);
    assertThat(field.getMinimumValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_Expect_MaximumValueIsNull() {
    NumberTextFormField field = new NumberTextFormField(ID);
    assertThat(field.getMaximumValue(), is(nullValue()));
  }

  @Test
  public void when_GettingType_Expect_NumberConstant() {
    NumberTextFormField field = new NumberTextFormField(ID);
    assertThat(field.getType(), equalTo("number"));
  }

  @Test
  public void when_SettingInitialValue_Expect_FluentReturnsSameInstance() {
    NumberTextFormField field = new NumberTextFormField(ID);
    NumberTextFormField result = field.withInitialValue(42);
    assertThat(result, is(sameInstance(field)));
  }

  @Test
  public void when_SettingInitialValue_Expect_InitialValueReflected() {
    NumberTextFormField field = new NumberTextFormField(ID).withInitialValue(42);
    assertThat(field.getInitialValue(), equalTo((Number) 42));
  }

  @Test
  public void when_SettingMinimumValue_Expect_FluentReturnsSameInstance() {
    NumberTextFormField field = new NumberTextFormField(ID);
    NumberTextFormField result = field.withMinimumValue(1);
    assertThat(result, is(sameInstance(field)));
  }

  @Test
  public void when_SettingMinimumValue_Expect_MinimumValueReflected() {
    NumberTextFormField field = new NumberTextFormField(ID).withMinimumValue(1);
    assertThat(field.getMinimumValue(), equalTo((Number) 1));
  }

  @Test
  public void when_SettingMinimumValueNull_Expect_NullPointerException() {
    NumberTextFormField field = new NumberTextFormField(ID);
    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> field.withMinimumValue(null));
    assertThat(exception.getMessage(), is(nullValue()));
  }

  @Test
  public void when_SettingMaximumValue_Expect_FluentReturnsSameInstance() {
    NumberTextFormField field = new NumberTextFormField(ID);
    NumberTextFormField result = field.withMaximumValue(100);
    assertThat(result, is(sameInstance(field)));
  }

  @Test
  public void when_SettingMaximumValue_Expect_MaximumValueReflected() {
    NumberTextFormField field = new NumberTextFormField(ID).withMaximumValue(100);
    assertThat(field.getMaximumValue(), equalTo((Number) 100));
  }

  @Test
  public void when_SettingMaximumValueNull_Expect_NullPointerException() {
    NumberTextFormField field = new NumberTextFormField(ID);
    NullPointerException exception =
        assertThrows(NullPointerException.class, () -> field.withMaximumValue(null));
    assertThat(exception.getMessage(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_InitialValueIsNull() {
    NumberTextFormField field = new NumberTextFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_MinimumValueIsNull() {
    NumberTextFormField field = new NumberTextFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getMinimumValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_MaximumValueIsNull() {
    NumberTextFormField field = new NumberTextFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getMaximumValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_InitialValueIsNull() {
    NumberTextFormField field = new NumberTextFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_MinimumValueIsNull() {
    NumberTextFormField field = new NumberTextFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getMinimumValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_MaximumValueIsNull() {
    NumberTextFormField field = new NumberTextFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getMaximumValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_RegexValidationIsNull() {
    NumberTextFormField field = new NumberTextFormField(ID);
    assertThat(field.getRegexValidation(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_InitialValueIsNull() {
    NumberTextFormField field = new NumberTextFormField(ID);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_SettingInitialValueNull_Expect_NoExceptionAndValueIsNull() {
    NumberTextFormField field = new NumberTextFormField(ID).withInitialValue(42);
    field.withInitialValue(null);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_SettingInitialValueNull_Expect_FluentReturnsSameInstance() {
    NumberTextFormField field = new NumberTextFormField(ID);
    NumberTextFormField result = field.withInitialValue(null);
    assertThat(result, is(sameInstance(field)));
  }

  @Test
  public void when_SettingInitialValue_WithNegative_Expect_InitialValueReflected() {
    NumberTextFormField field = new NumberTextFormField(ID).withInitialValue(-42);
    assertThat(field.getInitialValue(), equalTo((Number) (-42)));
  }

  @Test
  public void when_SettingInitialValue_WithZero_Expect_InitialValueReflected() {
    NumberTextFormField field = new NumberTextFormField(ID).withInitialValue(0);
    assertThat(field.getInitialValue(), equalTo((Number) 0));
  }

  @Test
  public void when_SettingInitialValue_WithLargeLong_Expect_InitialValueReflected() {
    NumberTextFormField field = new NumberTextFormField(ID).withInitialValue(Long.MAX_VALUE);
    assertThat(field.getInitialValue(), equalTo((Number) Long.MAX_VALUE));
  }

  @Test
  public void when_SettingInitialValue_WithDouble_Expect_InitialValueReflected() {
    NumberTextFormField field = new NumberTextFormField(ID).withInitialValue(3.14d);
    assertThat(field.getInitialValue(), equalTo((Number) 3.14d));
  }

  @Test
  public void when_SettingMinimumValue_WithNegative_Expect_MinimumValueReflected() {
    NumberTextFormField field = new NumberTextFormField(ID).withMinimumValue(-100);
    assertThat(field.getMinimumValue(), equalTo((Number) (-100)));
  }

  @Test
  public void when_SettingMinimumValue_WithZero_Expect_MinimumValueReflected() {
    NumberTextFormField field = new NumberTextFormField(ID).withMinimumValue(0);
    assertThat(field.getMinimumValue(), equalTo((Number) 0));
  }

  @Test
  public void when_SettingMaximumValue_WithLargeLong_Expect_MaximumValueReflected() {
    NumberTextFormField field = new NumberTextFormField(ID).withMaximumValue(Long.MAX_VALUE);
    assertThat(field.getMaximumValue(), equalTo((Number) Long.MAX_VALUE));
  }

  @Test
  public void when_SettingMinimumValue_Twice_Expect_LastValueRetained() {
    NumberTextFormField field = new NumberTextFormField(ID).withMinimumValue(1).withMinimumValue(2);
    assertThat(field.getMinimumValue(), equalTo((Number) 2));
  }

  @Test
  public void when_SettingMaximumValue_Twice_Expect_LastValueRetained() {
    NumberTextFormField field = new NumberTextFormField(ID).withMaximumValue(100).withMaximumValue(50);
    assertThat(field.getMaximumValue(), equalTo((Number) 50));
  }

  @Test
  public void when_SettingMinimumGreaterThanMaximum_Expect_BothRetainedWithoutValidation() {
    NumberTextFormField field = new NumberTextFormField(ID).withMinimumValue(100).withMaximumValue(1);
    assertThat(field.getMinimumValue(), equalTo((Number) 100));
    assertThat(field.getMaximumValue(), equalTo((Number) 1));
  }

  @Test
  public void when_ChainingFluentSetters_Expect_SameInstance() {
    NumberTextFormField field = new NumberTextFormField(ID);
    NumberTextFormField result = field.withInitialValue(50).withMinimumValue(0).withMaximumValue(100);
    assertThat(result, is(sameInstance(field)));
  }

  @Test
  public void when_ChainingFluentSetters_Expect_AllStateRetained() {
    NumberTextFormField field =
        new NumberTextFormField(ID).withInitialValue(50).withMinimumValue(0).withMaximumValue(100);
    assertThat(field.getInitialValue(), equalTo((Number) 50));
    assertThat(field.getMinimumValue(), equalTo((Number) 0));
    assertThat(field.getMaximumValue(), equalTo((Number) 100));
  }

  @Test
  public void when_SettingMinimumValueNull_AfterValueSet_Expect_OriginalValueRetained() {
    NumberTextFormField field = new NumberTextFormField(ID).withMinimumValue(5);
    assertThrows(NullPointerException.class, () -> field.withMinimumValue(null));
    assertThat(field.getMinimumValue(), equalTo((Number) 5));
  }

  @Test
  public void when_SettingMaximumValueNull_AfterValueSet_Expect_OriginalValueRetained() {
    NumberTextFormField field = new NumberTextFormField(ID).withMaximumValue(5);
    assertThrows(NullPointerException.class, () -> field.withMaximumValue(null));
    assertThat(field.getMaximumValue(), equalTo((Number) 5));
  }
}
