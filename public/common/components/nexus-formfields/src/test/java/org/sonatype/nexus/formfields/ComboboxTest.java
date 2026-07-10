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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link Combobox} tests.
 */
public class ComboboxTest
{
  private static final String ID = "testId";

  private static final String LABEL = "testLabel";

  private static final String HELP_TEXT = "testHelpText";

  private TestCombobox underTest;

  @Before
  public void setUp() {
    underTest = new TestCombobox(ID, LABEL, HELP_TEXT, FormField.OPTIONAL);
  }

  @Test
  public void when_CreatingWithIdAndLabel_Expect_DefaultsApplied() {
    TestCombobox combobox = new TestCombobox(ID, LABEL);

    assertThat(combobox.getId(), equalTo(ID));
    assertThat(combobox.getLabel(), equalTo(LABEL));
    assertThat(combobox.getHelpText(), nullValue());
    assertFalse(combobox.isRequired());
    assertThat(combobox.getInitialValue(), nullValue());
    // every constructor chains down forcing regexValidation to null
    assertThat(combobox.getRegexValidation(), nullValue());
    // disabled/readOnly are never set by any constructor
    assertFalse(combobox.isDisabled());
    assertFalse(combobox.isReadOnly());
  }

  @Test
  public void when_CreatingWithIdLabelAndHelpText_Expect_OptionalDefault() {
    TestCombobox combobox = new TestCombobox(ID, LABEL, HELP_TEXT);

    assertThat(combobox.getId(), equalTo(ID));
    assertThat(combobox.getLabel(), equalTo(LABEL));
    assertThat(combobox.getHelpText(), equalTo(HELP_TEXT));
    assertFalse(combobox.isRequired());
    assertThat(combobox.getInitialValue(), nullValue());
    assertThat(combobox.getRegexValidation(), nullValue());
  }

  @Test
  public void when_CreatingWithRequiredFlag_Expect_RequiredHonored() {
    TestCombobox combobox = new TestCombobox(ID, LABEL, HELP_TEXT, FormField.MANDATORY);

    assertThat(combobox.getId(), equalTo(ID));
    assertThat(combobox.getLabel(), equalTo(LABEL));
    assertThat(combobox.getHelpText(), equalTo(HELP_TEXT));
    assertTrue(combobox.isRequired());
    assertThat(combobox.getInitialValue(), nullValue());
    assertThat(combobox.getRegexValidation(), nullValue());
  }

  @Test
  public void when_CreatingWithInitialValue_Expect_InitialValueStored() {
    TestCombobox combobox = new TestCombobox(ID, LABEL, HELP_TEXT, FormField.MANDATORY, "initial");

    assertThat(combobox.getId(), equalTo(ID));
    assertThat(combobox.getLabel(), equalTo(LABEL));
    assertThat(combobox.getHelpText(), equalTo(HELP_TEXT));
    assertTrue(combobox.isRequired());
    assertThat(combobox.getInitialValue(), equalTo("initial"));
    // the 5-arg constructor forces regexValidation to null (it is NOT the 5th argument)
    assertThat(combobox.getRegexValidation(), nullValue());
  }

  @Test
  public void when_CreatingWithInitialValue_Expect_RegexValidationForcedNull() {
    // The 5-arg constructor maps the 5th argument to initialValue and forces regexValidation to null.
    // Use a regex-looking value to make it unambiguous that the value lands in initialValue, not regexValidation.
    TestCombobox combobox = new TestCombobox(ID, LABEL, HELP_TEXT, FormField.OPTIONAL, "[0-9]+");

    assertThat(combobox.getInitialValue(), equalTo("[0-9]+"));
    assertThat(combobox.getRegexValidation(), nullValue());
  }

  @Test
  public void getType_returnsComboboxConstant() {
    assertThat(underTest.getType(), is("combobox"));
  }

  @Test
  public void getIdMapping_defaultsToNull() {
    assertThat(underTest.getIdMapping(), nullValue());
  }

  @Test
  public void getNameMapping_defaultsToNull() {
    assertThat(underTest.getNameMapping(), nullValue());
  }

  @Test
  public void getStoreApi_reflectsImplementation() {
    assertThat(underTest.getStoreApi(), is("coreui_Test.read"));
  }

  @Test
  public void getStoreFilters_defaultsToNull() {
    assertThat(underTest.getStoreFilters(), nullValue());
  }

  @Test
  public void getStoreFilters_reflectsSetValue() {
    Map<String, String> filters = new HashMap<>();
    filters.put("format", "maven2");
    underTest.setStoreFilters(filters);

    assertThat(underTest.getStoreFilters(), is(filters));
    assertThat(underTest.getStoreFilters().get("format"), is("maven2"));
  }

  @Test
  public void withId_returnsSameInstanceAndMutatesId() {
    Combobox<String> result = underTest.withId("newId");

    assertThat(result, is(sameInstance(underTest)));
    assertThat(result, is(instanceOf(Combobox.class)));
    assertThat(underTest.getId(), equalTo("newId"));
  }

  @Test
  public void withId_repeatedCalls_returnSameInstanceAndApplyLastValue() {
    Combobox<String> first = underTest.withId("firstId");
    Combobox<String> second = underTest.withId("secondId");

    assertThat(first, is(sameInstance(underTest)));
    assertThat(second, is(sameInstance(underTest)));
    assertThat(underTest.getId(), equalTo("secondId"));
  }

  @Test
  public void witLabel_returnsSameInstanceAndMutatesLabel() {
    Combobox<String> result = underTest.witLabel("newLabel");

    assertThat(result, is(sameInstance(underTest)));
    assertThat(underTest.getLabel(), equalTo("newLabel"));
  }

  @Test
  public void witHelpText_returnsSameInstanceAndMutatesHelpText() {
    Combobox<String> result = underTest.witHelpText("newHelpText");

    assertThat(result, is(sameInstance(underTest)));
    assertThat(underTest.getHelpText(), equalTo("newHelpText"));
  }

  @Test
  public void withRegexValidation_returnsSameInstanceAndMutatesRegex() {
    Combobox<String> result = underTest.withRegexValidation("[a-z]+");

    assertThat(result, is(sameInstance(underTest)));
    assertThat(underTest.getRegexValidation(), equalTo("[a-z]+"));
  }

  @Test
  public void withRegexValidation_null_returnsSameInstanceAndClearsRegex() {
    underTest.withRegexValidation("[0-9]+");

    Combobox<String> result = underTest.withRegexValidation(null);

    assertThat(result, is(sameInstance(underTest)));
    assertThat(underTest.getRegexValidation(), nullValue());
  }

  @Test
  public void withRequired_true_returnsSameInstanceAndMarksRequired() {
    Combobox<String> result = underTest.withRequired(true);

    assertThat(result, is(sameInstance(underTest)));
    assertTrue(underTest.isRequired());
  }

  @Test
  public void withRequired_false_returnsSameInstanceAndMarksOptional() {
    underTest.withRequired(true);

    Combobox<String> result = underTest.withRequired(false);

    assertThat(result, is(sameInstance(underTest)));
    assertFalse(underTest.isRequired());
  }

  @Test
  public void optional_returnsSameInstanceAndMarksOptional() {
    underTest.withRequired(true);

    Combobox<String> result = underTest.optional();

    assertThat(result, is(sameInstance(underTest)));
    assertFalse(underTest.isRequired());
  }

  @Test
  public void mandatory_returnsSameInstanceAndMarksRequired() {
    Combobox<String> result = underTest.mandatory();

    assertThat(result, is(sameInstance(underTest)));
    assertTrue(underTest.isRequired());
  }

  @Test
  public void mandatoryThenOptional_togglesRequiredState() {
    Combobox<String> mandatoryResult = underTest.mandatory();
    assertThat(mandatoryResult, is(sameInstance(underTest)));
    assertTrue(underTest.isRequired());

    Combobox<String> optionalResult = underTest.optional();
    assertThat(optionalResult, is(sameInstance(underTest)));
    assertFalse(underTest.isRequired());
  }

  @Test
  public void withInitialValue_returnsSameInstanceAndMutatesInitialValue() {
    Combobox<String> result = underTest.withInitialValue("initialValue");

    assertThat(result, is(sameInstance(underTest)));
    assertThat(underTest.getInitialValue(), equalTo("initialValue"));
  }

  @Test
  public void withInitialValue_null_returnsSameInstanceAndClearsInitialValue() {
    underTest.withInitialValue("initialValue");

    Combobox<String> result = underTest.withInitialValue(null);

    assertThat(result, is(sameInstance(underTest)));
    assertThat(underTest.getInitialValue(), nullValue());
  }

  @Test
  public void fluentMethods_chainTogether_returnSameInstanceAndApplyAllState() {
    Combobox<String> result = underTest
        .withId("chainId")
        .witLabel("chainLabel")
        .witHelpText("chainHelpText")
        .withRegexValidation("[a-z]+")
        .withInitialValue("chainValue")
        .mandatory();

    assertThat(result, is(sameInstance(underTest)));
    assertThat(underTest.getId(), equalTo("chainId"));
    assertThat(underTest.getLabel(), equalTo("chainLabel"));
    assertThat(underTest.getHelpText(), equalTo("chainHelpText"));
    assertThat(underTest.getRegexValidation(), equalTo("[a-z]+"));
    assertThat(underTest.getInitialValue(), equalTo("chainValue"));
    assertTrue(underTest.isRequired());
  }

  /**
   * Concrete {@link Combobox} used to exercise the abstract base class. Provides simple implementations of the
   * {@link Selectable} contract that is otherwise left abstract by {@link Combobox}.
   */
  private static class TestCombobox
      extends Combobox<String>
  {
    private Map<String, String> storeFilters;

    TestCombobox(
        final String id,
        final String label,
        final String helpText,
        final boolean required,
        final String initialValue)
    {
      super(id, label, helpText, required, initialValue);
    }

    TestCombobox(final String id, final String label, final String helpText, final boolean required) {
      super(id, label, helpText, required);
    }

    TestCombobox(final String id, final String label, final String helpText) {
      super(id, label, helpText);
    }

    TestCombobox(final String id, final String label) {
      super(id, label);
    }

    void setStoreFilters(final Map<String, String> storeFilters) {
      this.storeFilters = storeFilters;
    }

    @Override
    public String getStoreApi() {
      return "coreui_Test.read";
    }

    @Override
    public Map<String, String> getStoreFilters() {
      return storeFilters;
    }
  }
}
