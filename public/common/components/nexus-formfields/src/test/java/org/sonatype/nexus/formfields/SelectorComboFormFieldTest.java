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
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SelectorComboFormFieldTest
{
  private static final String ID = "selectorId";

  private static final String LABEL = "selectorLabel";

  private static final String HELP_TEXT = "selectorHelpText";

  private static final String REGEX = ".*";

  private static final String DEFAULT_LABEL = "Content Selector";

  private static final String DEFAULT_HELP_TEXT = "Select the content selector.";

  private SelectorComboFormField underTest;

  @Before
  public void setUp() {
    underTest = new SelectorComboFormField(ID);
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_DefaultLabelHelpTextAndOptional() {
    assertThat(underTest.getId(), equalTo(ID));
    assertThat(underTest.getLabel(), equalTo(DEFAULT_LABEL));
    assertThat(underTest.getHelpText(), equalTo(DEFAULT_HELP_TEXT));
    assertFalse(underTest.isRequired());
    assertThat(underTest.getRegexValidation(), nullValue());
    // id-only constructor must leave initialValue unset and flags at their defaults
    assertThat(underTest.getInitialValue(), nullValue());
    assertFalse(underTest.isDisabled());
    assertFalse(underTest.isReadOnly());
  }

  @Test
  public void when_CreatingNew_WithIdAndRequired_Expect_DefaultLabelHelpTextAndRequired() {
    SelectorComboFormField field = new SelectorComboFormField(ID, true);
    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(DEFAULT_LABEL));
    assertThat(field.getHelpText(), equalTo(DEFAULT_HELP_TEXT));
    assertTrue(field.isRequired());
    assertThat(field.getRegexValidation(), nullValue());
    assertThat(field.getInitialValue(), nullValue());
  }

  @Test
  public void when_CreatingNew_WithIdLabelHelpTextRequired_Expect_FieldsAreSet() {
    SelectorComboFormField field = new SelectorComboFormField(ID, LABEL, HELP_TEXT, true);
    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(LABEL));
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
    assertTrue(field.isRequired());
    assertThat(field.getRegexValidation(), nullValue());
    assertThat(field.getInitialValue(), nullValue());
  }

  @Test
  public void when_CreatingNew_WithIdLabelHelpTextOptional_Expect_FieldsAreSetAndOptional() {
    // exercises the OPTIONAL (required=false) branch of the 4-arg constructor
    SelectorComboFormField field = new SelectorComboFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(LABEL));
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
    assertFalse(field.isRequired());
    assertThat(field.getRegexValidation(), nullValue());
    assertThat(field.getInitialValue(), nullValue());
  }

  @Test
  public void when_CreatingNew_WithRegexValidation_Expect_FieldsAreSet() {
    SelectorComboFormField field = new SelectorComboFormField(ID, LABEL, HELP_TEXT, false, REGEX);
    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(LABEL));
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
    assertFalse(field.isRequired());
    // Combobox's 5-arg constructor maps the 5th argument to initialValue, not regexValidation (see NEXUS-53405)
    assertThat(field.getRegexValidation(), nullValue());
    assertThat(field.getInitialValue(), equalTo(REGEX));
  }

  @Test
  public void when_CreatingNew_With5Args_AndArbitraryInitialValue_Expect_5thArgIsInitialValueNotRegex() {
    // make the trap explicit with a value that is clearly NOT a regex: it must still land in initialValue
    String initial = "preselected-selector";
    SelectorComboFormField field = new SelectorComboFormField(ID, LABEL, HELP_TEXT, true, initial);
    assertTrue(field.isRequired());
    assertThat(field.getInitialValue(), equalTo(initial));
    assertThat(field.getRegexValidation(), nullValue());
  }

  @Test
  public void when_CreatingNew_With5Args_AndNullInitialValue_Expect_RegexAndInitialValueNull() {
    SelectorComboFormField field = new SelectorComboFormField(ID, LABEL, HELP_TEXT, true, null);
    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(LABEL));
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
    assertTrue(field.isRequired());
    assertThat(field.getRegexValidation(), nullValue());
    assertThat(field.getInitialValue(), nullValue());
  }

  @Test
  public void when_Created_Expect_IsSelectableComboboxFormField() {
    assertThat(underTest, instanceOf(Selectable.class));
    assertThat(underTest, instanceOf(Combobox.class));
    assertThat(underTest, instanceOf(FormField.class));
  }

  @Test
  public void when_GettingType_Expect_Combobox() {
    assertThat(underTest.getType(), is("combobox"));
  }

  @Test
  public void when_GettingStoreApi_Expect_SelectorReadReferences() {
    assertThat(underTest.getStoreApi(), is("coreui_Selector.readReferences"));
  }

  @Test
  public void when_GettingStoreFilters_Expect_Null() {
    assertThat(underTest.getStoreFilters(), nullValue());
  }

  @Test
  public void when_GettingIdMapping_Expect_Null() {
    assertThat(underTest.getIdMapping(), nullValue());
  }

  @Test
  public void when_GettingNameMapping_Expect_Null() {
    assertThat(underTest.getNameMapping(), nullValue());
  }

  @Test
  public void when_GettingAllowAutocomplete_Expect_False() {
    assertFalse(underTest.getAllowAutocomplete());
  }

  @Test
  public void when_GettingAttributes_Expect_EmptyMapAndSameInstanceOnRepeatedCalls() {
    Map<String, Object> attributes = underTest.getAttributes();
    assertThat(attributes, notNullValue());
    assertThat(attributes, anEmptyMap());
    // lazy-init must return the same instance on repeated calls
    assertThat(underTest.getAttributes(), sameInstance(attributes));
  }
}
