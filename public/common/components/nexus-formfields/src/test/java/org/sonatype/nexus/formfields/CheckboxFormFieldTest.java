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
 * {@link CheckboxFormField} tests.
 */
public class CheckboxFormFieldTest
{
  private static final String ID = "checkboxId";

  private static final String LABEL = "checkboxLabel";

  private static final String HELP_TEXT = "checkboxHelpText";

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectId() {
    CheckboxFormField field = new CheckboxFormField(ID, LABEL, HELP_TEXT, true);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectLabel() {
    CheckboxFormField field = new CheckboxFormField(ID, LABEL, HELP_TEXT, true);
    assertThat(field.getLabel(), equalTo(LABEL));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectHelpText() {
    CheckboxFormField field = new CheckboxFormField(ID, LABEL, HELP_TEXT, true);
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_AndRequiredTrue_Expect_RequiredIsTrue() {
    CheckboxFormField field = new CheckboxFormField(ID, LABEL, HELP_TEXT, true);
    assertTrue(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithAllArgs_AndRequiredFalse_Expect_RequiredIsFalse() {
    CheckboxFormField field = new CheckboxFormField(ID, LABEL, HELP_TEXT, false);
    assertFalse(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_CorrectId() {
    CheckboxFormField field = new CheckboxFormField(ID);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_LabelIsNull() {
    CheckboxFormField field = new CheckboxFormField(ID);
    assertThat(field.getLabel(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_HelpTextIsNull() {
    CheckboxFormField field = new CheckboxFormField(ID);
    assertThat(field.getHelpText(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_RequiredIsFalse() {
    CheckboxFormField field = new CheckboxFormField(ID);
    assertFalse(field.isRequired());
  }

  @Test
  public void when_GettingType_Expect_CheckboxConstant() {
    CheckboxFormField field = new CheckboxFormField(ID);
    assertThat(field.getType(), equalTo("checkbox"));
  }

  @Test
  public void when_SettingInitialValue_Expect_FluentReturnsSameInstance() {
    CheckboxFormField field = new CheckboxFormField(ID);
    CheckboxFormField result = field.withInitialValue(Boolean.TRUE);
    assertThat(result, is(sameInstance(field)));
  }

  @Test
  public void when_SettingInitialValueTrue_Expect_InitialValueIsTrue() {
    CheckboxFormField field = new CheckboxFormField(ID).withInitialValue(Boolean.TRUE);
    assertThat(field.getInitialValue(), equalTo(Boolean.TRUE));
  }

  @Test
  public void when_SettingInitialValueFalse_Expect_InitialValueIsFalse() {
    CheckboxFormField field = new CheckboxFormField(ID).withInitialValue(Boolean.FALSE);
    assertThat(field.getInitialValue(), equalTo(Boolean.FALSE));
  }

  @Test
  public void when_GettingType_WithAllArgs_Expect_CheckboxConstant() {
    CheckboxFormField field = new CheckboxFormField(ID, LABEL, HELP_TEXT, true);
    assertThat(field.getType(), equalTo("checkbox"));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_InitialValueIsNull() {
    CheckboxFormField field = new CheckboxFormField(ID);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_InitialValueIsNull() {
    CheckboxFormField field = new CheckboxFormField(ID, LABEL, HELP_TEXT, true);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_RegexValidationIsNull() {
    CheckboxFormField field = new CheckboxFormField(ID);
    assertThat(field.getRegexValidation(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_RegexValidationIsNull() {
    CheckboxFormField field = new CheckboxFormField(ID, LABEL, HELP_TEXT, true);
    assertThat(field.getRegexValidation(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_Expect_NotDisabled() {
    CheckboxFormField field = new CheckboxFormField(ID, LABEL, HELP_TEXT, true);
    assertFalse(field.isDisabled());
  }

  @Test
  public void when_CreatingNew_Expect_NotReadOnly() {
    CheckboxFormField field = new CheckboxFormField(ID, LABEL, HELP_TEXT, true);
    assertFalse(field.isReadOnly());
  }

  @Test
  public void when_CreatingNew_Expect_AllowAutocompleteIsFalse() {
    CheckboxFormField field = new CheckboxFormField(ID);
    assertFalse(field.getAllowAutocomplete());
  }

  @Test
  public void when_GettingAttributes_Expect_NonNullAndEmpty() {
    CheckboxFormField field = new CheckboxFormField(ID);
    assertThat(field.getAttributes(), is(notNullValue()));
    assertTrue(field.getAttributes().isEmpty());
  }

  @Test
  public void when_GettingAttributes_Repeatedly_Expect_SameInstance() {
    CheckboxFormField field = new CheckboxFormField(ID);
    assertThat(field.getAttributes(), is(sameInstance(field.getAttributes())));
  }

  @Test
  public void when_SettingInitialValueNull_Expect_InitialValueIsNull() {
    CheckboxFormField field = new CheckboxFormField(ID).withInitialValue(null);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_SettingInitialValueNull_Expect_FluentReturnsSameInstance() {
    CheckboxFormField field = new CheckboxFormField(ID);
    CheckboxFormField result = field.withInitialValue(null);
    assertThat(result, is(sameInstance(field)));
  }

  @Test
  public void when_OverwritingInitialValue_Expect_LastValueWins() {
    CheckboxFormField field = new CheckboxFormField(ID)
        .withInitialValue(Boolean.TRUE)
        .withInitialValue(Boolean.FALSE);
    assertThat(field.getInitialValue(), equalTo(Boolean.FALSE));
  }

  @Test
  public void when_ClearingInitialValueWithNull_Expect_InitialValueIsNull() {
    CheckboxFormField field = new CheckboxFormField(ID)
        .withInitialValue(Boolean.TRUE)
        .withInitialValue(null);
    assertThat(field.getInitialValue(), is(nullValue()));
  }
}
