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
package org.sonatype.nexus.validation.constraint

import javax.validation.Validation
import javax.validation.Validator

import org.sonatype.goodies.testsupport.TestSupport

import org.junit.Before
import org.junit.Test

import static org.sonatype.nexus.validation.constraint.CaseType.LOWER
import static org.sonatype.nexus.validation.constraint.CaseType.UPPER

/**
 * Tests for {@link Case}.
 */
class CaseTest
    extends TestSupport
{
  private Validator validator

  @Before
  public void setUp() throws Exception {
    validator = Validation.buildDefaultValidatorFactory().validator
  }

  class UperCaseSubject
  {
    @Case(UPPER)
    String value
  }

  @Test
  void 'test UPPER case'() {
    def violations

    violations = validator.validate(new UperCaseSubject(value: 'YES'))
    log violations
    assert violations.size() == 0

    violations = validator.validate(new UperCaseSubject(value: 'No'))
    log violations
    assert violations.size() == 1
  }

  class LowerCaseSubject
  {
    @Case(LOWER)
    String value
  }

  @Test
  void 'test LOWER case'() {
    def violations

    violations = validator.validate(new LowerCaseSubject(value: 'yes'))
    log violations
    assert violations.size() == 0

    violations = validator.validate(new LowerCaseSubject(value: 'No'))
    log violations
    assert violations.size() == 1
  }
}
