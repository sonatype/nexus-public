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
import static org.junit.Assert.assertFalse;
import static org.hamcrest.Matchers.equalTo;

/**
 * {@link AbstractFormField} tests.
 */
public class AbstractFormFieldTest
{
  private static final String ID = "testId";

  private static final String TYPE = "testField";

  private AbstractFormField<String> formField;

  @Before
  public void setUp() {
    formField = new AbstractFormField<String>(ID)
    {
      @Override
      public String getType() {
        return TYPE;
      }
    };
  }

  @Test
  public void when_CreatingNew_WithId_Expect_CorrectId() {
    assertThat(formField.getId(), equalTo(ID));
  }

  @Test
  public void when_CreatingNew_WithId_Expect_CorrectType() {
    assertThat(formField.getType(), equalTo(TYPE));
  }

  @Test
  public void when_CreatingNew_WithId_Expect_RequiredIsFalse() {
    assertFalse(formField.isRequired());
  }

  @Test
  public void when_CreatingNew_WithId_Expect_DisabledIsFalse() {
    assertFalse(formField.isDisabled());
  }

  @Test
  public void when_CreatingNew_WithId_Expect_IsReadOnlyIsFalse() {
    assertFalse(formField.isReadOnly());
  }
}
