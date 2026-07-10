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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link RepoTargetComboFormField} tests.
 */
public class RepoTargetComboFormFieldTest
{
  private static final String ID = "testId";

  private static final String LABEL = "testLabel";

  private static final String HELP_TEXT = "testHelpText";

  private static final String REGEX = ".*";

  @Test
  public void when_CreatingNew_WithAllArgs_Expect_FieldsAreSet() {
    RepoTargetComboFormField field = new RepoTargetComboFormField(ID, LABEL, HELP_TEXT, true, REGEX);
    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(LABEL));
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
    assertTrue(field.isRequired());
    // TRAP (see NEXUS-53405): Combobox's 5-arg constructor maps the 5th argument to initialValue (V),
    // not regexValidation, and forces regexValidation to null. So the value passed as "regexValidation"
    // actually lands in initialValue and getRegexValidation() must remain null.
    assertThat(field.getRegexValidation(), nullValue());
    assertThat(field.getInitialValue(), equalTo(REGEX));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_NotRequired_Expect_RequiredFalse() {
    RepoTargetComboFormField field = new RepoTargetComboFormField(ID, LABEL, HELP_TEXT, false, REGEX);
    assertFalse(field.isRequired());
    assertThat(field.getRegexValidation(), nullValue());
    assertThat(field.getInitialValue(), equalTo(REGEX));
  }

  @Test
  public void when_CreatingNew_WithAllArgs_NullFifthArg_Expect_InitialValueNull() {
    // The 5th argument flows into initialValue; passing null must leave both initialValue and
    // regexValidation null (i.e. no accidental cross-wiring).
    RepoTargetComboFormField field = new RepoTargetComboFormField(ID, LABEL, HELP_TEXT, true, null);
    assertThat(field.getInitialValue(), nullValue());
    assertThat(field.getRegexValidation(), nullValue());
  }

  @Test
  public void when_CreatingNew_WithIdLabelHelpTextRequired_Expect_FieldsAreSet() {
    RepoTargetComboFormField field = new RepoTargetComboFormField(ID, LABEL, HELP_TEXT, false);
    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(LABEL));
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
    assertFalse(field.isRequired());
    // This constructor must NOT set a regex or an initial value (unlike the 5-arg trap constructor).
    assertThat(field.getRegexValidation(), nullValue());
    assertThat(field.getInitialValue(), nullValue());
  }

  @Test
  public void when_CreatingNew_WithIdLabelHelpTextRequired_Required_Expect_RequiredTrue() {
    RepoTargetComboFormField field = new RepoTargetComboFormField(ID, LABEL, HELP_TEXT, true);
    assertTrue(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithIdRequired_Expect_DefaultLabelAndHelpText() {
    RepoTargetComboFormField field = new RepoTargetComboFormField(ID, true);
    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(RepoTargetComboFormField.DEFAULT_LABEL));
    assertThat(field.getHelpText(), equalTo(RepoTargetComboFormField.DEFAULT_HELP_TEXT));
    assertTrue(field.isRequired());
    assertThat(field.getRegexValidation(), nullValue());
    assertThat(field.getInitialValue(), nullValue());
  }

  @Test
  public void when_CreatingNew_WithIdRequiredFalse_Expect_DefaultLabelAndHelpText() {
    RepoTargetComboFormField field = new RepoTargetComboFormField(ID, false);
    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(RepoTargetComboFormField.DEFAULT_LABEL));
    assertThat(field.getHelpText(), equalTo(RepoTargetComboFormField.DEFAULT_HELP_TEXT));
    assertFalse(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithIdOnly_Expect_DefaultsAndNotRequired() {
    RepoTargetComboFormField field = new RepoTargetComboFormField(ID);
    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(RepoTargetComboFormField.DEFAULT_LABEL));
    assertThat(field.getHelpText(), equalTo(RepoTargetComboFormField.DEFAULT_HELP_TEXT));
    assertFalse(field.isRequired());
    assertThat(field.getRegexValidation(), nullValue());
    assertThat(field.getInitialValue(), nullValue());
  }

  @Test
  public void when_CreatingNew_Expect_NotDisabledNotReadOnly() {
    // Constructors must not enable disabled/readOnly; both default to false.
    RepoTargetComboFormField field = new RepoTargetComboFormField(ID);
    assertFalse(field.isDisabled());
    assertFalse(field.isReadOnly());
  }

  @Test
  public void when_CreatingNew_WithNullId_Expect_NullIdAccepted() {
    // No checkNotNull on id anywhere in the constructor chain; null is accepted without NPE.
    RepoTargetComboFormField field = new RepoTargetComboFormField(null);
    assertThat(field.getId(), nullValue());
    assertThat(field.getLabel(), equalTo(RepoTargetComboFormField.DEFAULT_LABEL));
    assertThat(field.getHelpText(), equalTo(RepoTargetComboFormField.DEFAULT_HELP_TEXT));
  }

  @Test
  public void when_CreatingNew_WithNullLabelAndHelpText_Expect_NullsAccepted() {
    RepoTargetComboFormField field = new RepoTargetComboFormField(ID, null, null, true);
    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), nullValue());
    assertThat(field.getHelpText(), nullValue());
    assertTrue(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithEmptyStrings_Expect_EmptyStringsPreserved() {
    RepoTargetComboFormField field = new RepoTargetComboFormField("", "", "", false);
    assertThat(field.getId(), equalTo(""));
    assertThat(field.getLabel(), equalTo(""));
    assertThat(field.getHelpText(), equalTo(""));
  }

  @Test
  public void when_GettingType_Expect_RepoTargetConstant() {
    // Overrides Combobox#getType(); must be the repo-target constant and NOT the inherited "combobox".
    assertThat(new RepoTargetComboFormField(ID).getType(), is("repo-target"));
    assertThat(new RepoTargetComboFormField(ID).getType(), not(equalTo("combobox")));
  }

  @Test
  public void when_GettingStoreApi_Expect_RepositoryTargetReadApi() {
    assertThat(new RepoTargetComboFormField(ID).getStoreApi(), is("coreui_RepositoryTarget.read"));
  }

  @Test
  public void when_GettingStoreFilters_Expect_Null() {
    assertThat(new RepoTargetComboFormField(ID).getStoreFilters(), nullValue());
  }

  @Test
  public void when_GettingIdMapping_Expect_Null() {
    assertThat(new RepoTargetComboFormField(ID).getIdMapping(), nullValue());
  }

  @Test
  public void when_GettingNameMapping_Expect_Null() {
    assertThat(new RepoTargetComboFormField(ID).getNameMapping(), nullValue());
  }

  @Test
  public void when_CheckingDefaultConstants_Expect_ExpectedValues() {
    assertThat(RepoTargetComboFormField.DEFAULT_LABEL, equalTo("Repository Target"));
    assertThat(RepoTargetComboFormField.DEFAULT_HELP_TEXT, equalTo("Select the repository target to apply "));
  }
}
