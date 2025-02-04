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
package org.sonatype.nexus.validation.constraint;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ConstraintViolation;

import org.junit.Before;
import org.junit.Test;
import org.sonatype.goodies.testsupport.TestSupport;

import java.util.Set;

import static org.sonatype.nexus.validation.constraint.CaseType.LOWER;
import static org.sonatype.nexus.validation.constraint.CaseType.UPPER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class CaseTest
    extends TestSupport
{

  private Validator validator;

  @Before
  public void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  private static class UpperCaseSubject
  {
    @Case(UPPER)
    private String value;

    public UpperCaseSubject(String value) {
      this.value = value;
    }
  }

  @Test
  public void testUpperCase() {
    Set<ConstraintViolation<UpperCaseSubject>> violations;

    violations = validator.validate(new UpperCaseSubject("YES"));
    assertThat(violations, hasSize(0));

    violations = validator.validate(new UpperCaseSubject("No"));
    assertThat(violations, hasSize(1));
  }

  private static class LowerCaseSubject
  {
    @Case(LOWER)
    private String value;

    public LowerCaseSubject(String value) {
      this.value = value;
    }
  }

  @Test
  public void testLowerCase() {
    Set<ConstraintViolation<LowerCaseSubject>> violations;

    violations = validator.validate(new LowerCaseSubject("yes"));
    assertThat(violations, hasSize(0));

    violations = validator.validate(new LowerCaseSubject("No"));
    assertThat(violations, hasSize(1));
  }
}
