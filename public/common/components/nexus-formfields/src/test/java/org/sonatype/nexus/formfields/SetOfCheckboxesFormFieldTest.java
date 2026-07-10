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

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link SetOfCheckboxesFormField} tests.
 */
public class SetOfCheckboxesFormFieldTest
{
  private static final String ID = "testId";

  private static final String LABEL = "testLabel";

  private static final String HELP_TEXT = "testHelpText";

  private SetOfCheckboxesFormField field;

  @Before
  public void setUp() {
    field = new SetOfCheckboxesFormField(ID, LABEL, HELP_TEXT, true);
  }

  @Test
  public void when_CreatingNew_Expect_FieldsAreSet() {
    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(LABEL));
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
    assertTrue(field.isRequired());
  }

  @Test
  public void when_CreatingNew_NotRequired_Expect_RequiredIsFalse() {
    SetOfCheckboxesFormField notRequired = new SetOfCheckboxesFormField(ID, LABEL, HELP_TEXT, false);
    assertFalse(notRequired.isRequired());
    assertThat(notRequired.getRegexValidation(), nullValue());
    assertThat(notRequired.getInitialValue(), nullValue());
  }

  @Test
  public void when_CreatingNew_Expect_IsAbstractFormField() {
    assertThat(field, instanceOf(AbstractFormField.class));
  }

  @Test
  public void when_CreatingNew_Expect_IsFormField() {
    assertThat(field, instanceOf(FormField.class));
  }

  @Test
  public void when_GettingType_Expect_SetOfCheckboxes() {
    assertThat(field.getType(), equalTo("setOfCheckboxes"));
  }

  @Test
  public void when_CreatingNew_Required_Expect_RegexValidationIsNull() {
    assertThat(field.getRegexValidation(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_Required_Expect_InitialValueIsNull() {
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_Expect_NotDisabled() {
    assertFalse(field.isDisabled());
  }

  @Test
  public void when_CreatingNew_Expect_NotReadOnly() {
    assertFalse(field.isReadOnly());
  }

  @Test
  public void when_CreatingNew_Expect_AllowAutocompleteIsFalse() {
    assertFalse(field.getAllowAutocomplete());
  }

  @Test
  public void when_CreatingNew_WithNullArgs_Expect_StoredWithoutValidation() {
    SetOfCheckboxesFormField nullField = new SetOfCheckboxesFormField(null, null, null, false);
    assertThat(nullField.getId(), is(nullValue()));
    assertThat(nullField.getLabel(), is(nullValue()));
    assertThat(nullField.getHelpText(), is(nullValue()));
    assertFalse(nullField.isRequired());
  }

  @Test
  public void when_GettingAttributes_Expect_NonNullAndEmpty() {
    assertThat(field.getAttributes(), is(notNullValue()));
    assertTrue(field.getAttributes().isEmpty());
  }

  @Test
  public void when_GettingAttributes_Repeatedly_Expect_SameInstance() {
    assertThat(field.getAttributes(), is(sameInstance(field.getAttributes())));
  }

  @Test
  public void when_AddingAttribute_Expect_SameInstanceAndValueStored() {
    AbstractFormField<Boolean> result = field.withAttribute("key", "value");
    assertThat(result, is(sameInstance(field)));
    assertThat(field.getAttributes().get("key"), equalTo((Object) "value"));
  }

  @Test
  public void when_SettingInitialValueTrue_Expect_InitialValueIsTrue() {
    field.setInitialValue(Boolean.TRUE);
    assertThat(field.getInitialValue(), equalTo(Boolean.TRUE));
  }

  @Test
  public void when_SettingInitialValueFalse_Expect_InitialValueIsFalse() {
    field.setInitialValue(Boolean.FALSE);
    assertThat(field.getInitialValue(), equalTo(Boolean.FALSE));
  }

  @Test
  public void when_SettingInitialValueNull_Expect_InitialValueIsNull() {
    field.setInitialValue(Boolean.TRUE);
    field.setInitialValue(null);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_SettingRequiredFalse_Expect_RequiredIsFalse() {
    field.setRequired(false);
    assertFalse(field.isRequired());
  }

  @Test
  public void when_SettingDisabledTrue_Expect_IsDisabled() {
    field.setDisabled(true);
    assertTrue(field.isDisabled());
  }

  @Test
  public void when_SettingReadOnlyTrue_Expect_IsReadOnly() {
    field.setReadOnly(true);
    assertTrue(field.isReadOnly());
  }

  @Test
  public void when_SettingLabel_Expect_LabelUpdated() {
    field.setLabel("newLabel");
    assertThat(field.getLabel(), equalTo("newLabel"));
  }

  @Test
  public void when_SettingId_Expect_IdUpdated() {
    field.setId("newId");
    assertThat(field.getId(), equalTo("newId"));
  }

  @Test
  public void when_SettingHelpText_Expect_HelpTextUpdated() {
    field.setHelpText("newHelpText");
    assertThat(field.getHelpText(), equalTo("newHelpText"));
  }

  @Test
  public void when_SettingRegexValidation_Expect_RegexValidationUpdated() {
    field.setRegexValidation(".*");
    assertThat(field.getRegexValidation(), equalTo(".*"));
  }
}
