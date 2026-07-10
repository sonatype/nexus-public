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
 * {@link UrlFormField} tests.
 */
public class UrlFormFieldTest
{
  private static final String ID = "urlId";

  private static final String LABEL = "urlLabel";

  private static final String HELP_TEXT = "urlHelpText";

  private static final String REGEX = ".*";

  private static final String INITIAL_VALUE = "urlInitialValue";

  private static final String ATTR_KEY = "urlAttrKey";

  private static final String ATTR_VALUE = "urlAttrValue";

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectId() {
    UrlFormField field = new UrlFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectLabel() {
    UrlFormField field = new UrlFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getLabel(), equalTo(LABEL));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectHelpText() {
    UrlFormField field = new UrlFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_CorrectRegexValidation() {
    UrlFormField field = new UrlFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getRegexValidation(), equalTo(REGEX));
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_AndRequiredTrue_Expect_RequiredIsTrue() {
    UrlFormField field = new UrlFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertTrue(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_AndMandatory_Expect_RequiredIsTrue() {
    UrlFormField field = new UrlFormField(ID, LABEL, HELP_TEXT, FormField.MANDATORY, REGEX);
    assertTrue(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_AndOptional_Expect_RequiredIsFalse() {
    UrlFormField field = new UrlFormField(ID, LABEL, HELP_TEXT, FormField.OPTIONAL, REGEX);
    assertFalse(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithAllArgsAndRegex_Expect_InitialValueIsNull() {
    // the 5th constructor argument maps to regexValidation, NOT initialValue
    UrlFormField field = new UrlFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectId() {
    UrlFormField field = new UrlFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectLabel() {
    UrlFormField field = new UrlFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getLabel(), equalTo(LABEL));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_CorrectHelpText() {
    UrlFormField field = new UrlFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_RegexValidationIsNull() {
    UrlFormField field = new UrlFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getRegexValidation(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_InitialValueIsNull() {
    UrlFormField field = new UrlFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_AndRequiredFalse_Expect_RequiredIsFalse() {
    UrlFormField field = new UrlFormField(ID, LABEL, HELP_TEXT, false);
    assertFalse(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithAllArgs_AndMandatory_Expect_RequiredIsTrue() {
    UrlFormField field = new UrlFormField(ID, LABEL, HELP_TEXT, FormField.MANDATORY);
    assertTrue(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_CorrectId() {
    UrlFormField field = new UrlFormField(ID);
    assertThat(field.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_LabelIsNull() {
    UrlFormField field = new UrlFormField(ID);
    assertThat(field.getLabel(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_HelpTextIsNull() {
    UrlFormField field = new UrlFormField(ID);
    assertThat(field.getHelpText(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_RequiredIsFalse() {
    UrlFormField field = new UrlFormField(ID);
    assertFalse(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_RegexValidationIsNull() {
    UrlFormField field = new UrlFormField(ID);
    assertThat(field.getRegexValidation(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_InitialValueIsNull() {
    UrlFormField field = new UrlFormField(ID);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_NotDisabledByDefault() {
    UrlFormField field = new UrlFormField(ID);
    assertFalse(field.isDisabled());
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_NotReadOnlyByDefault() {
    UrlFormField field = new UrlFormField(ID);
    assertFalse(field.isReadOnly());
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_AllowAutocompleteIsFalseByDefault() {
    UrlFormField field = new UrlFormField(ID);
    assertFalse(field.getAllowAutocomplete());
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_AttributesNotNullAndEmptyByDefault() {
    UrlFormField field = new UrlFormField(ID);
    assertThat(field.getAttributes(), is(notNullValue()));
    assertTrue(field.getAttributes().isEmpty());
  }

  @Test
  public void when_CreatingNew_WithNullId_Expect_IdIsNull() {
    // the constructor does not enforce non-null ids
    UrlFormField field = new UrlFormField(null);
    assertThat(field.getId(), is(nullValue()));
  }

  @Test
  public void when_GettingAttributes_Expect_SameInstanceOnRepeatedCalls() {
    UrlFormField field = new UrlFormField(ID);
    assertThat(field.getAttributes(), is(sameInstance(field.getAttributes())));
  }

  @Test
  public void when_GettingType_Expect_UrlConstant() {
    UrlFormField field = new UrlFormField(ID);
    assertThat(field.getType(), equalTo("url"));
  }

  @Test
  public void when_GettingType_WithAllArgsAndRegex_Expect_UrlConstant() {
    // getType() overrides StringTextFormField's "string" regardless of which constructor is used
    UrlFormField field = new UrlFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getType(), equalTo("url"));
  }

  @Test
  public void when_SettingInitialValue_Expect_FluentReturnsSameInstance() {
    UrlFormField field = new UrlFormField(ID);
    StringTextFormField result = field.withInitialValue(INITIAL_VALUE);
    assertThat(result, is(sameInstance((StringTextFormField) field)));
  }

  @Test
  public void when_SettingInitialValue_Expect_InitialValueReflected() {
    UrlFormField field = new UrlFormField(ID);
    field.withInitialValue(INITIAL_VALUE);
    assertThat(field.getInitialValue(), equalTo(INITIAL_VALUE));
  }

  @Test
  public void when_SettingInitialValueNull_Expect_FluentReturnsSameInstance() {
    UrlFormField field = new UrlFormField(ID);
    StringTextFormField result = field.withInitialValue(null);
    assertThat(result, is(sameInstance((StringTextFormField) field)));
  }

  @Test
  public void when_SettingInitialValueNull_Expect_InitialValueIsNull() {
    UrlFormField field = new UrlFormField(ID);
    field.withInitialValue(null);
    assertThat(field.getInitialValue(), is(nullValue()));
  }

  @Test
  public void when_SettingInitialValueTwice_Expect_LatestValueRetained() {
    UrlFormField field = new UrlFormField(ID);
    field.withInitialValue(INITIAL_VALUE);
    field.withInitialValue("other");
    assertThat(field.getInitialValue(), equalTo("other"));
  }

  @Test
  public void when_AddingAttribute_Expect_FluentReturnsSameInstance() {
    UrlFormField field = new UrlFormField(ID);
    AbstractFormField<String> result = field.withAttribute(ATTR_KEY, ATTR_VALUE);
    assertThat(result, is(sameInstance((AbstractFormField<String>) field)));
  }

  @Test
  public void when_AddingAttribute_Expect_AttributeReflected() {
    UrlFormField field = new UrlFormField(ID);
    field.withAttribute(ATTR_KEY, ATTR_VALUE);
    assertThat(field.getAttributes().get(ATTR_KEY), equalTo((Object) ATTR_VALUE));
  }
}
