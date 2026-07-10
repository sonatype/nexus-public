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
import static org.junit.Assert.assertTrue;

/**
 * {@link PanelMessageFormField} tests.
 */
public class PanelMessageFormFieldTest
{
  private static final String ID = "testId";

  private static final String LABEL = "testLabel";

  private static final String HELP_TEXT = "testHelpText";

  @Test
  public void when_GettingType_Expect_PanelMessageConstant() {
    PanelMessageFormField field =
        new PanelMessageFormField(ID, LABEL, HELP_TEXT, PanelMessageFormField.INFO_PANEL_TYPE);
    assertThat(field.getType(), equalTo("panelMessage"));
  }

  @Test
  public void when_CreatingWithLabel_Expect_FieldsAreSet() {
    PanelMessageFormField field =
        new PanelMessageFormField(ID, LABEL, HELP_TEXT, PanelMessageFormField.INFO_PANEL_TYPE);
    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), equalTo(LABEL));
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
    assertFalse(field.isRequired());
    assertThat(field.getAttributes().get(PanelMessageFormField.PANEL_TYPE_ATTRIBUTE_KEY),
        equalTo((Object) PanelMessageFormField.INFO_PANEL_TYPE));
  }

  @Test
  public void when_CreatingWithoutLabel_Expect_LabelIsNullAndFieldsAreSet() {
    PanelMessageFormField field =
        new PanelMessageFormField(ID, HELP_TEXT, PanelMessageFormField.WARNING_PANEL_TYPE);
    assertThat(field.getId(), equalTo(ID));
    assertThat(field.getLabel(), nullValue());
    assertThat(field.getHelpText(), equalTo(HELP_TEXT));
    assertFalse(field.isRequired());
    assertThat(field.getAttributes().get(PanelMessageFormField.PANEL_TYPE_ATTRIBUTE_KEY),
        equalTo((Object) PanelMessageFormField.WARNING_PANEL_TYPE));
  }

  @Test
  public void when_CreatingWithNullPanelType_Expect_NullStoredForPanelType() {
    PanelMessageFormField field = new PanelMessageFormField(ID, HELP_TEXT, null);
    assertTrue(field.getAttributes().containsKey(PanelMessageFormField.PANEL_TYPE_ATTRIBUTE_KEY));
    assertThat(field.getAttributes().get(PanelMessageFormField.PANEL_TYPE_ATTRIBUTE_KEY), nullValue());
  }

  @Test
  public void when_AccessingPanelTypeConstants_Expect_ExpectedValues() {
    assertThat(PanelMessageFormField.INFO_PANEL_TYPE, is("info"));
    assertThat(PanelMessageFormField.WARNING_PANEL_TYPE, is("warning"));
    assertThat(PanelMessageFormField.PANEL_TYPE_ATTRIBUTE_KEY, is("panelType"));
  }

  @Test
  public void when_CreatingWithLabel_Expect_InheritedDefaultsAreSet() {
    PanelMessageFormField field =
        new PanelMessageFormField(ID, LABEL, HELP_TEXT, PanelMessageFormField.INFO_PANEL_TYPE);
    assertFalse(field.isRequired());
    assertFalse(field.isDisabled());
    assertFalse(field.isReadOnly());
    assertFalse(field.getAllowAutocomplete());
    assertThat(field.getRegexValidation(), nullValue());
    assertThat(field.getInitialValue(), nullValue());
  }

  @Test
  public void when_CreatingWithoutLabel_Expect_InheritedDefaultsAreSet() {
    PanelMessageFormField field =
        new PanelMessageFormField(ID, HELP_TEXT, PanelMessageFormField.WARNING_PANEL_TYPE);
    assertFalse(field.isRequired());
    assertFalse(field.isDisabled());
    assertFalse(field.isReadOnly());
    assertFalse(field.getAllowAutocomplete());
    assertThat(field.getRegexValidation(), nullValue());
    assertThat(field.getInitialValue(), nullValue());
  }

  @Test
  public void when_CreatingWithLabelAndNullPanelType_Expect_KeyPresentWithNullValue() {
    PanelMessageFormField field = new PanelMessageFormField(ID, LABEL, HELP_TEXT, null);
    assertTrue(field.getAttributes().containsKey(PanelMessageFormField.PANEL_TYPE_ATTRIBUTE_KEY));
    assertThat(field.getAttributes().get(PanelMessageFormField.PANEL_TYPE_ATTRIBUTE_KEY), nullValue());
  }

  @Test
  public void when_CreatingWithArbitraryPanelType_Expect_StoredVerbatimWithoutValidation() {
    String customType = "  customPanelType  ";
    PanelMessageFormField labeled = new PanelMessageFormField(ID, LABEL, HELP_TEXT, customType);
    assertThat(labeled.getAttributes().get(PanelMessageFormField.PANEL_TYPE_ATTRIBUTE_KEY),
        equalTo((Object) customType));
    assertThat(labeled.getAttributes().size(), is(1));

    PanelMessageFormField unlabeled = new PanelMessageFormField(ID, HELP_TEXT, "");
    assertThat(unlabeled.getAttributes().get(PanelMessageFormField.PANEL_TYPE_ATTRIBUTE_KEY),
        equalTo((Object) ""));
    assertThat(unlabeled.getAttributes().size(), is(1));
  }

  @Test
  public void when_GettingAttributesRepeatedly_Expect_SameInstance() {
    PanelMessageFormField field =
        new PanelMessageFormField(ID, LABEL, HELP_TEXT, PanelMessageFormField.INFO_PANEL_TYPE);
    assertThat(field.getAttributes(), sameInstance(field.getAttributes()));
  }
}
