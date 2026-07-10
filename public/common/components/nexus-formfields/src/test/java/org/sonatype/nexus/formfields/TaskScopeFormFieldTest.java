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
 * {@link TaskScopeFormField} tests.
 */
public class TaskScopeFormFieldTest
{
  private static final String ID = "testId";

  private static final String INITIAL_VALUE = "testInitialValue";

  private static final String EMPTY = "";

  @Test
  public void when_GettingType_Expect_TaskScopeConstant() {
    TaskScopeFormField field = new TaskScopeFormField(ID, true, INITIAL_VALUE);
    assertThat(field.getType(), equalTo("taskScope"));
  }

  @Test
  public void when_CreatingNew_Required_Expect_FieldsAreSet() {
    TaskScopeFormField field = new TaskScopeFormField(ID, true, INITIAL_VALUE);
    assertThat(field.getId(), equalTo(ID));
    assertTrue(field.isRequired());
    assertThat(field.getInitialValue(), equalTo(INITIAL_VALUE));
    assertThat(field.getLabel(), equalTo(""));
    assertThat(field.getHelpText(), equalTo(""));
    assertThat(field.getRegexValidation(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_NotRequired_Expect_FieldsAreSet() {
    TaskScopeFormField field = new TaskScopeFormField(ID, false, null);
    assertThat(field.getId(), equalTo(ID));
    assertFalse(field.isRequired());
    assertThat(field.getInitialValue(), is(nullValue()));
    assertThat(field.getLabel(), equalTo(""));
    assertThat(field.getHelpText(), equalTo(""));
    assertThat(field.getRegexValidation(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_Expect_LabelIsEmptyStringNotNull() {
    TaskScopeFormField field = new TaskScopeFormField(ID, true, INITIAL_VALUE);
    assertThat(field.getLabel(), is(notNullValue()));
    assertThat(field.getLabel(), equalTo(""));
  }

  @Test
  public void when_CreatingNew_Expect_HelpTextIsEmptyStringNotNull() {
    TaskScopeFormField field = new TaskScopeFormField(ID, true, INITIAL_VALUE);
    assertThat(field.getHelpText(), is(notNullValue()));
    assertThat(field.getHelpText(), equalTo(""));
  }

  @Test
  public void when_CreatingNew_WithEmptyInitialValue_Expect_EmptyInitialValuePreserved() {
    TaskScopeFormField field = new TaskScopeFormField(ID, true, EMPTY);
    assertThat(field.getInitialValue(), is(notNullValue()));
    assertThat(field.getInitialValue(), equalTo(""));
  }

  @Test
  public void when_CreatingNew_WithNullId_Expect_IdIsNull() {
    TaskScopeFormField field = new TaskScopeFormField(null, true, INITIAL_VALUE);
    assertThat(field.getId(), is(nullValue()));
  }

  @Test
  public void when_CreatingNew_Expect_DisabledIsFalseByDefault() {
    TaskScopeFormField field = new TaskScopeFormField(ID, true, INITIAL_VALUE);
    assertFalse(field.isDisabled());
  }

  @Test
  public void when_CreatingNew_Expect_ReadOnlyIsFalseByDefault() {
    TaskScopeFormField field = new TaskScopeFormField(ID, true, INITIAL_VALUE);
    assertFalse(field.isReadOnly());
  }

  @Test
  public void when_CreatingNew_Expect_AllowAutocompleteIsFalseByDefault() {
    TaskScopeFormField field = new TaskScopeFormField(ID, true, INITIAL_VALUE);
    assertFalse(field.getAllowAutocomplete());
  }

  @Test
  public void when_GettingAttributes_Expect_NonNullEmptyMap() {
    TaskScopeFormField field = new TaskScopeFormField(ID, true, INITIAL_VALUE);
    assertThat(field.getAttributes(), is(notNullValue()));
    assertTrue(field.getAttributes().isEmpty());
  }

  @Test
  public void when_GettingAttributesRepeatedly_Expect_SameInstance() {
    TaskScopeFormField field = new TaskScopeFormField(ID, true, INITIAL_VALUE);
    assertThat(field.getAttributes(), is(sameInstance(field.getAttributes())));
  }
}
