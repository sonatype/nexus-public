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

package org.sonatype.nexus.security.internal;

import org.sonatype.nexus.rest.ValidationErrorsException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @since 3.25
 */
public class PasswordValidatorTest
{
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testValidate_noValidation() {
    PasswordValidator underTest = new PasswordValidator(null, null);

    underTest.validate("foo");
  }

  @Test
  public void testValidate_passValidation() {
    PasswordValidator underTest = new PasswordValidator(".*", null);

    underTest.validate("foo");
  }

  @Test
  public void testValidate_failValidation() {
    PasswordValidator underTest = new PasswordValidator("[a]+", null);

    thrown.expect(ValidationErrorsException.class);
    thrown.expectMessage("Password does not match corporate policy");

    underTest.validate("foo");
  }

  @Test
  public void testValidate_failValidationCustomErrorMessage() {
    PasswordValidator underTest = new PasswordValidator("[a]+", "Bad bad bad");

    thrown.expect(ValidationErrorsException.class);
    thrown.expectMessage("Bad bad bad");

    underTest.validate("foo");
  }
}
