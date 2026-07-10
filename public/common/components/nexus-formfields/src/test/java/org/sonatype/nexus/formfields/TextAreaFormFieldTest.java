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
 * {@link TextAreaFormField} tests.
 */
public class TextAreaFormFieldTest
{
  private static final String ID = "textAreaId";

  private static final String LABEL = "textAreaLabel";

  private static final String HELP_TEXT = "textAreaHelpText";

  private static final String REGEX = ".*";

  private static final String INITIAL_VALUE = "textAreaInitialValue";

  @Test
  public void when_CreatingNew_WithAllArgsAndReadOnly_Expect_CorrectId() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX, true);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndReadOnly_Expect_CorrectLabel() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX, true);
    assertThat(field.getLabel(), equalTo(LABEL));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndReadOnly_Expect_CorrectHelpText() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX, true);
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndReadOnly_Expect_CorrectRegexValidation() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX, true);
    assertThat(field.getRegexValidation(), equalTo(REGEX));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndReadOnly_AndRequiredTrue_Expect_RequiredIsTrue() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX, true);
    assertTrue(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndReadOnlyTrue_Expect_ReadOnlyIsTrue() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX, true);
    assertTrue(field.isReadOnly());
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndReadOnlyFalse_Expect_ReadOnlyIsFalse() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX, false);
    assertFalse(field.isReadOnly());
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectId() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectLabel() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getLabel(), equalTo(LABEL));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectHelpText() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectRegexValidation() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getRegexValidation(), equalTo(REGEX));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_AndRequiredTrue_Expect_RequiredIsTrue() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertTrue(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_ReadOnlyIsFalse() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertFalse(field.isReadOnly());
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectId() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectLabel() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getLabel(), equalTo(LABEL));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectHelpText() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_RegexValidationIsNull() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getRegexValidation(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_AndRequiredFalse_Expect_RequiredIsFalse() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, false);
    assertFalse(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_CorrectId() {
    TextAreaFormField field = new TextAreaFormField(ID);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_LabelIsNull() {
    TextAreaFormField field = new TextAreaFormField(ID);
    assertThat(field.getLabel(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_HelpTextIsNull() {
    TextAreaFormField field = new TextAreaFormField(ID);
    assertThat(field.getHelpText(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_RegexValidationIsNull() {
    TextAreaFormField field = new TextAreaFormField(ID);
    assertThat(field.getRegexValidation(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_RequiredIsFalse() {
    TextAreaFormField field = new TextAreaFormField(ID);
    assertFalse(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_InitialValueIsNull() {
    TextAreaFormField field = new TextAreaFormField(ID);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_GettingType_Expect_TextAreaConstant() {
    TextAreaFormField field = new TextAreaFormField(ID);
    assertThat(field.getType(), equalTo("text-area"));
  }

  @Test
  public void when_SettingInitialValue_Expect_FluentReturnsSameInstance() {
    TextAreaFormField field = new TextAreaFormField(ID);
    TextAreaFormField result = field.withInitialValue(INITIAL_VALUE);
    assertThat(result, is(sameInstance(field)));
  }

  @Test
  public void when_SettingInitialValue_Expect_InitialValueReflected() {
    TextAreaFormField field = new TextAreaFormField(ID).withInitialValue(INITIAL_VALUE);
    assertThat(field.getInitialValue(), equalTo(INITIAL_VALUE));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndReadOnly_Expect_DisabledIsFalseByDefault() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX, true);
    assertFalse(field.isDisabled());
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndReadOnly_Expect_InitialValueIsNull() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX, true);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndReadOnly_AndRequiredFalse_Expect_RequiredIsFalse() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, false, REGEX, true);
    assertFalse(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndReadOnly_AndNullRegex_Expect_RegexValidationIsNull() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, null, true);
    assertThat(field.getRegexValidation(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_ReadOnlyIsFalseByDefault() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertFalse(field.isReadOnly());
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_DisabledIsFalseByDefault() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertFalse(field.isDisabled());
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_InitialValueIsNull() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_AndRequiredTrue_Expect_RequiredIsTrue() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, true);
    assertTrue(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_ReadOnlyIsFalseByDefault() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, false);
    assertFalse(field.isReadOnly());
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_DisabledIsFalseByDefault() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, false);
    assertFalse(field.isDisabled());
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_InitialValueIsNull() {
    TextAreaFormField field = new TextAreaFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_ReadOnlyIsFalse() {
    TextAreaFormField field = new TextAreaFormField(ID);
    assertFalse(field.isReadOnly());
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_DisabledIsFalse() {
    TextAreaFormField field = new TextAreaFormField(ID);
    assertFalse(field.isDisabled());
  }

  @Test
  public void when_GettingAttributes_Expect_NonNullEmptyMap() {
    TextAreaFormField field = new TextAreaFormField(ID);
    assertThat(field.getAttributes(), is(notNullValue()));
    assertTrue(field.getAttributes().isEmpty());
  }

  @Test
  public void when_GettingAttributesRepeatedly_Expect_SameInstance() {
    TextAreaFormField field = new TextAreaFormField(ID);
    assertThat(field.getAttributes(), is(sameInstance(field.getAttributes())));
  }

  @Test
  public void when_GettingAllowAutocomplete_Expect_FalseByDefault() {
    TextAreaFormField field = new TextAreaFormField(ID);
    assertFalse(field.getAllowAutocomplete());
  }

  @Test
  public void when_SettingInitialValueToNull_Expect_InitialValueIsNull() {
    TextAreaFormField field = new TextAreaFormField(ID).withInitialValue(INITIAL_VALUE);
    TextAreaFormField result = field.withInitialValue(null);
    assertThat(result, is(sameInstance(field)));
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_SettingInitialValueTwice_Expect_LastValueRetained() {
    TextAreaFormField field = new TextAreaFormField(ID)
        .withInitialValue("firstValue")
        .withInitialValue(INITIAL_VALUE);
    assertThat(field.getInitialValue(), equalTo(INITIAL_VALUE));
  }
}
