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
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link TemplateFormField} tests.
 */
public class TemplateFormFieldTest
{
  private static final String ID = "testId";

  private static final String INITIAL_VALUE = "testInitialValue";

  @Test
  public void when_GettingType_Expect_TemplateOnly() {
    TemplateFormField<String> field = new TemplateFormField<String>(ID, true, INITIAL_VALUE);
    assertThat(field.getType(), equalTo("templateOnly"));
  }

  @Test
  public void when_GettingType_WithRequiredFalse_Expect_TemplateOnly() {
    TemplateFormField<String> field = new TemplateFormField<String>(ID, false, INITIAL_VALUE);
    assertThat(field.getType(), equalTo("templateOnly"));
  }

  @Test
  public void when_CreatingNew_WithRequiredTrue_Expect_FieldsAreSet() {
    TemplateFormField<String> field = new TemplateFormField<String>(ID, true, INITIAL_VALUE);
    assertThat(field.getId(), equalTo(ID));
    assertTrue(field.isRequired());
    assertThat(field.getInitialValue(), equalTo(INITIAL_VALUE));
  }

  @Test
  public void when_CreatingNew_WithRequiredFalse_Expect_RequiredIsFalse() {
    TemplateFormField<String> field = new TemplateFormField<String>(ID, false, INITIAL_VALUE);
    assertThat(field.getId(), equalTo(ID));
    assertFalse(field.isRequired());
    assertThat(field.getInitialValue(), equalTo(INITIAL_VALUE));
  }

  @Test
  public void when_CreatingNew_Expect_OptionalFieldsAreNull() {
    TemplateFormField<String> field = new TemplateFormField<String>(ID, true, INITIAL_VALUE);
    assertThat(field.getLabel(), nullValue());
    assertThat(field.getHelpText(), nullValue());
    assertThat(field.getRegexValidation(), nullValue());
  }

  @Test
  public void when_CreatingNew_WithNullInitialValue_Expect_InitialValueIsNull() {
    TemplateFormField<String> field = new TemplateFormField<String>(ID, false, null);
    assertThat(field.getInitialValue(), nullValue());
  }

  @Test
  public void when_CreatingNew_WithNullId_Expect_IdIsNull() {
    // the constructor performs no null-check on id, so a null id is permitted (no NPE)
    TemplateFormField<String> field = new TemplateFormField<String>(null, true, INITIAL_VALUE);
    assertThat(field.getId(), nullValue());
  }

  @Test
  public void when_CreatingNew_Expect_DisabledIsFalseByDefault() {
    TemplateFormField<String> field = new TemplateFormField<String>(ID, true, INITIAL_VALUE);
    assertFalse(field.isDisabled());
  }

  @Test
  public void when_CreatingNew_Expect_ReadOnlyIsFalseByDefault() {
    TemplateFormField<String> field = new TemplateFormField<String>(ID, true, INITIAL_VALUE);
    assertFalse(field.isReadOnly());
  }

  @Test
  public void when_CreatingNew_Expect_AllowAutocompleteIsFalseByDefault() {
    TemplateFormField<String> field = new TemplateFormField<String>(ID, true, INITIAL_VALUE);
    assertFalse(field.getAllowAutocomplete());
  }

  @Test
  public void when_GettingAttributes_Expect_EmptyMapByDefault() {
    TemplateFormField<String> field = new TemplateFormField<String>(ID, true, INITIAL_VALUE);
    assertThat(field.getAttributes(), anEmptyMap());
  }

  @Test
  public void when_GettingAttributes_Twice_Expect_SameLazyInitializedInstance() {
    TemplateFormField<String> field = new TemplateFormField<String>(ID, true, INITIAL_VALUE);
    assertThat(field.getAttributes(), sameInstance(field.getAttributes()));
  }

  @Test
  public void when_CreatingNew_WithNonStringType_Expect_InitialValueIsExactReference() {
    // value above the Integer cache range so a distinct object is retained, verifying the
    // generic initialValue is stored and returned as-is (not copied/boxed differently)
    Integer value = 123456;
    TemplateFormField<Integer> field = new TemplateFormField<Integer>(ID, true, value);
    assertThat(field.getInitialValue(), equalTo(value));
    assertThat(field.getInitialValue(), sameInstance(value));
  }
}
