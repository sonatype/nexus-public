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
import org.sonatype.nexus.formfields.AlertBannerFormField.Position;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

/**
 * {@link AlertBannerFormField} tests.
 */
public class AlertBannerFormFieldTest
{
  private static final String TITLE = "testTitle";

  @Test
  public void when_CreatingNew_WithTopPosition_Expect_FieldsAreSet() {
    AlertBannerFormField field = new AlertBannerFormField(Position.TOP, TITLE);
    assertThat(field.getId(), equalTo("topAlertBanner"));
    assertThat(field.getLabel(), equalTo("TOP"));
    assertThat(field.getHelpText(), equalTo(TITLE));
    assertFalse(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithBottomPosition_Expect_FieldsAreSet() {
    AlertBannerFormField field = new AlertBannerFormField(Position.BOTTOM, TITLE);
    assertThat(field.getId(), equalTo("bottomAlertBanner"));
    assertThat(field.getLabel(), equalTo("BOTTOM"));
    assertThat(field.getHelpText(), equalTo(TITLE));
    assertFalse(field.isRequired());
  }

  @Test
  public void when_GettingType_Expect_TemplateOnly() {
    AlertBannerFormField field = new AlertBannerFormField(Position.TOP, TITLE);
    assertThat(field.getType(), equalTo("templateOnly"));
  }

  @Test
  public void when_GettingPositionValues_Expect_TopAndBottom() {
    assertThat(Position.values().length, is(2));
    assertThat(Position.values(), arrayContaining(Position.TOP, Position.BOTTOM));
  }

  @Test
  public void when_ResolvingPositionByName_Expect_CorrectConstant() {
    assertThat(Position.valueOf("TOP"), is(Position.TOP));
    assertThat(Position.valueOf("BOTTOM"), is(Position.BOTTOM));
  }

  @Test
  public void when_ResolvingPositionByUnknownName_Expect_IllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> Position.valueOf("MIDDLE"));
  }

  @Test
  public void when_CreatingNew_Expect_OptionalInheritedFieldsHaveDefaults() {
    AlertBannerFormField field = new AlertBannerFormField(Position.TOP, TITLE);
    assertThat(field.getRegexValidation(), is(nullValue()));
    assertThat(field.getInitialValue(), is(nullValue()));
    assertFalse(field.isDisabled());
    assertFalse(field.isReadOnly());
  }

  @Test
  public void when_GettingType_WithBottomPosition_Expect_TemplateOnly() {
    AlertBannerFormField field = new AlertBannerFormField(Position.BOTTOM, TITLE);
    assertThat(field.getType(), equalTo("templateOnly"));
  }

  @Test
  public void when_CreatingNew_WithNullTitle_Expect_HelpTextIsNull() {
    AlertBannerFormField field = new AlertBannerFormField(Position.BOTTOM, null);
    assertThat(field.getHelpText(), is(nullValue()));
    assertThat(field.getId(), equalTo("bottomAlertBanner"));
    assertThat(field.getLabel(), equalTo("BOTTOM"));
    assertFalse(field.isRequired());
  }

  @Test
  public void when_CreatingNew_WithNullPosition_Expect_NullPointerException() {
    // The constructor has no explicit null guard; a null position fails fast via position.toString().
    // This pins the current (implicit) behavior rather than asserting a documented contract.
    assertThrows(NullPointerException.class, () -> new AlertBannerFormField(null, TITLE));
  }
}
