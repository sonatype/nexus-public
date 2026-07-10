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

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link AbstractFormField} (the {@link FormField} support base) and the {@link FormField} interface default
 * methods, exercised through a concrete subclass.
 *
 * Named without the {@code Abstract} prefix because Surefire is configured to exclude {@code Abstract*} test classes,
 * so a base test named {@code AbstractFormFieldTest} would never actually run.
 */
public class FormFieldSupportTest
{
  private static final String ID = "testId";

  private static final String TYPE = "test";

  private static final String LABEL = "testLabel";

  private static final String HELP_TEXT = "testHelpText";

  private static final String REGEX = ".*";

  private static final String INITIAL_VALUE = "testInitialValue";

  private TestFormField field;

  @Before
  public void setUp() {
    field = new TestFormField(ID);
  }

  @Test
  public void when_CreatingNew_WithId_Expect_IdAndDefaults() {
    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getType(), equalTo(TYPE));
    assertThat(field.getLabel(), nullValue());
    assertThat(field.getHelpText(), nullValue());
    assertThat(field.getRegexValidation(), nullValue());
    assertThat(field.getInitialValue(), nullValue());
    assertFalse(field.isRequired());
    assertFalse(field.isDisabled());
    assertFalse(field.isReadOnly());
  }

  @Test
  public void when_CreatingNew_WithLabelHelpTextRequired_Expect_FieldsAreSet() {
    TestFormField created = new TestFormField(ID, LABEL, HELP_TEXT, true);
    assertThat(created.getId(), equalTo(ID));
    assertThat(created.getLabel(), equalTo(LABEL));
    assertThat(created.getHelpText(), equalTo(HELP_TEXT));
    assertTrue(created.isRequired());
    assertThat(created.getRegexValidation(), nullValue());
    assertThat(created.getInitialValue(), nullValue());
  }

  @Test
  public void when_CreatingNew_WithRegexValidation_Expect_FieldsAreSet() {
    TestFormField created = new TestFormField(ID, LABEL, HELP_TEXT, false, REGEX);
    assertThat(created.getId(), equalTo(ID));
    assertThat(created.getLabel(), equalTo(LABEL));
    assertThat(created.getHelpText(), equalTo(HELP_TEXT));
    assertFalse(created.isRequired());
    assertThat(created.getRegexValidation(), equalTo(REGEX));
    assertThat(created.getInitialValue(), nullValue());
  }

  @Test
  public void when_CreatingNew_WithInitialValue_Expect_FieldsAreSet() {
    TestFormField created = new TestFormField(ID, LABEL, HELP_TEXT, true, REGEX, INITIAL_VALUE);
    assertThat(created.getId(), equalTo(ID));
    assertThat(created.getLabel(), equalTo(LABEL));
    assertThat(created.getHelpText(), equalTo(HELP_TEXT));
    assertTrue(created.isRequired());
    assertThat(created.getRegexValidation(), equalTo(REGEX));
    assertThat(created.getInitialValue(), equalTo(INITIAL_VALUE));
  }

  @Test
  public void when_SettingHelpText_Expect_HelpTextIsSet() {
    field.setHelpText(HELP_TEXT);
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
  }

  @Test
  public void when_SettingId_Expect_IdIsSet() {
    field.setId("newId");
    assertThat(field.getId(), equalTo("newId"));
  }

  @Test
  public void when_SettingRegexValidation_Expect_RegexValidationIsSet() {
    field.setRegexValidation(REGEX);
    assertThat(field.getRegexValidation(), equalTo(REGEX));
  }

  @Test
  public void when_SettingRequired_Expect_RequiredIsSet() {
    field.setRequired(true);
    assertTrue(field.isRequired());
  }

  @Test
  public void when_SettingDisabled_Expect_DisabledIsSet() {
    field.setDisabled(true);
    assertTrue(field.isDisabled());
  }

  @Test
  public void when_SettingReadOnly_Expect_ReadOnlyIsSet() {
    field.setReadOnly(true);
    assertTrue(field.isReadOnly());
  }

  @Test
  public void when_SettingLabel_Expect_LabelIsSet() {
    field.setLabel(LABEL);
    assertThat(field.getLabel(), equalTo(LABEL));
  }

  @Test
  public void when_SettingInitialValue_Expect_InitialValueIsSet() {
    field.setInitialValue(INITIAL_VALUE);
    assertThat(field.getInitialValue(), equalTo(INITIAL_VALUE));
  }

  @Test
  public void when_GettingAttributes_Expect_LazyInitAndSameMapOnRepeatedCalls() {
    Map<String, Object> attributes = field.getAttributes();
    assertThat(attributes, notNullValue());
    assertThat(field.getAttributes(), sameInstance(attributes));
  }

  @Test
  public void when_AddingAttribute_Expect_SameInstanceAndValueStored() {
    AbstractFormField<String> result = field.withAttribute("key", "value");
    assertThat(result, sameInstance(field));
    assertThat(field.getAttributes().get("key"), equalTo((Object) "value"));
  }

  @Test
  public void when_GettingAllowAutocomplete_Expect_False() {
    assertFalse(field.getAllowAutocomplete());
  }

  @Test
  public void when_CreatingNew_WithNullInitialValue_Expect_InitialValueIsNull() {
    TestFormField created = new TestFormField(ID, LABEL, HELP_TEXT, true, REGEX, null);
    assertThat(created.getId(), equalTo(ID));
    assertThat(created.getLabel(), equalTo(LABEL));
    assertThat(created.getHelpText(), equalTo(HELP_TEXT));
    assertTrue(created.isRequired());
    assertThat(created.getRegexValidation(), equalTo(REGEX));
    assertThat(created.getInitialValue(), nullValue());
  }

  @Test
  public void when_UsingRequiredConstants_Expect_ConstantsMapToRequiredFlag() {
    assertTrue(FormField.MANDATORY);
    assertFalse(FormField.OPTIONAL);

    TestFormField mandatory = new TestFormField(ID, LABEL, HELP_TEXT, FormField.MANDATORY);
    assertTrue(mandatory.isRequired());

    TestFormField optional = new TestFormField(ID, LABEL, HELP_TEXT, FormField.OPTIONAL);
    assertFalse(optional.isRequired());
  }

  @Test
  public void when_SettingInitialValueToNull_Expect_InitialValueIsNull() {
    field.setInitialValue(INITIAL_VALUE);
    assertThat(field.getInitialValue(), equalTo(INITIAL_VALUE));
    field.setInitialValue(null);
    assertThat(field.getInitialValue(), nullValue());
  }

  @Test
  public void when_SettingRegexValidationToNull_Expect_RegexValidationIsNull() {
    field.setRegexValidation(REGEX);
    assertThat(field.getRegexValidation(), equalTo(REGEX));
    field.setRegexValidation(null);
    assertThat(field.getRegexValidation(), nullValue());
  }

  @Test
  public void when_SettersCalled_Expect_ConstructorValuesOverridden() {
    TestFormField created = new TestFormField(ID, LABEL, HELP_TEXT, true, REGEX, INITIAL_VALUE);

    created.setId("newId");
    created.setLabel("newLabel");
    created.setHelpText("newHelpText");
    created.setRegexValidation("newRegex");
    created.setInitialValue("newInitialValue");
    created.setRequired(false);
    created.setDisabled(true);
    created.setReadOnly(true);

    assertThat(created.getId(), equalTo("newId"));
    assertThat(created.getLabel(), equalTo("newLabel"));
    assertThat(created.getHelpText(), equalTo("newHelpText"));
    assertThat(created.getRegexValidation(), equalTo("newRegex"));
    assertThat(created.getInitialValue(), equalTo("newInitialValue"));
    assertFalse(created.isRequired());
    assertTrue(created.isDisabled());
    assertTrue(created.isReadOnly());
  }

  @Test
  public void when_GettingAttributes_Expect_InitiallyEmpty() {
    assertThat(field.getAttributes(), aMapWithSize(0));
  }

  @Test
  public void when_OverwritingAttributeKey_Expect_ValueReplaced() {
    field.withAttribute("key", "first");
    field.withAttribute("key", "second");
    assertThat(field.getAttributes(), aMapWithSize(1));
    assertThat(field.getAttributes().get("key"), equalTo((Object) "second"));
  }

  @Test
  public void when_AddingMultipleAttributes_Expect_AllStoredViaChaining() {
    AbstractFormField<String> result = field.withAttribute("key1", "value1").withAttribute("key2", "value2");
    assertThat(result, sameInstance(field));
    assertThat(field.getAttributes(), aMapWithSize(2));
    assertThat(field.getAttributes().get("key1"), equalTo((Object) "value1"));
    assertThat(field.getAttributes().get("key2"), equalTo((Object) "value2"));
  }

  /**
   * Concrete {@link AbstractFormField} used to exercise the abstract support base and the {@link FormField} interface
   * default methods. Deliberately does not override {@link FormField#getAllowAutocomplete()}.
   */
  static class TestFormField
      extends AbstractFormField<String>
  {
    TestFormField(final String id) {
      super(id);
    }

    TestFormField(final String id, final String label, final String helpText, final boolean required) {
      super(id, label, helpText, required);
    }

    TestFormField(
        final String id,
        final String label,
        final String helpText,
        final boolean required,
        final String regexValidation)
    {
      super(id, label, helpText, required, regexValidation);
    }

    TestFormField(
        final String id,
        final String label,
        final String helpText,
        final boolean required,
        final String regexValidation,
        final String initialValue)
    {
      super(id, label, helpText, required, regexValidation, initialValue);
    }

    @Override
    public String getType() {
      return TYPE;
    }
  }
}
