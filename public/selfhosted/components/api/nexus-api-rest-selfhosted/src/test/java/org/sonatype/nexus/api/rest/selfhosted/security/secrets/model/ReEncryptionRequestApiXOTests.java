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
package org.sonatype.nexus.api.rest.selfhosted.security.secrets.model;

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

public class ReEncryptionRequestApiXOTests
{
  private static Validator validator;

  @BeforeClass
  public static void setup() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  public void testValidationFailsEmptyKeyId() {
    ReEncryptionRequestApiXO xo = new ReEncryptionRequestApiXO();
    Set<ConstraintViolation<ReEncryptionRequestApiXO>> violations = validator.validate(xo);
    Assertions.assertThat(violations).hasSize(1);

    xo = new ReEncryptionRequestApiXO("", null);
    violations = validator.validate(xo);
    Assertions.assertThat(violations).hasSize(1);
  }

  @Test
  public void testValidationSucceed() {
    ReEncryptionRequestApiXO xo = new ReEncryptionRequestApiXO("test-key", null);
    Set<ConstraintViolation<ReEncryptionRequestApiXO>> violations = validator.validate(xo);
    Assertions.assertThat(violations).isEmpty();
  }

  @Test
  public void testFieldsSet() {
    ReEncryptionRequestApiXO xo = new ReEncryptionRequestApiXO("test-key", "mail@test.com");
    Set<ConstraintViolation<ReEncryptionRequestApiXO>> violations = validator.validate(xo);
    Assertions.assertThat(violations).isEmpty();
    Assertions.assertThat(xo.getSecretKeyId()).isEqualTo("test-key");
    Assertions.assertThat(xo.getNotifyEmail()).isEqualTo("mail@test.com");
  }
}
