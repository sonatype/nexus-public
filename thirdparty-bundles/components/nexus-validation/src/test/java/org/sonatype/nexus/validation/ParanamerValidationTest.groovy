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
package org.sonatype.nexus.validation

import javax.validation.Validation
import javax.validation.ValidatorFactory
import javax.validation.constraints.NotNull

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.validation.internal.AopAwareParanamerParameterNameProvider

import org.junit.Before
import org.junit.Test

/**
 * Paranamer validation test.
 */
class ParanamerValidationTest
    extends TestSupport
{
  def ValidatorFactory factory

  @Before
  void setUp() {
    factory = Validation.byDefaultProvider().configure()
        .parameterNameProvider(new AopAwareParanamerParameterNameProvider())
        .buildValidatorFactory()
  }

  class TestSubject
  {
    def test(@NotNull String text) {
      // ignore
    }
  }

  @Test
  void 'validate method resolve parameter name'() {
    def method = TestSubject.class.getMethod('test', String)
    log method

    def obj = new TestSubject()

    Object[] values = [ null ]
    Class[] groups = []
    def violations = factory.validator.forExecutables().validateParameters(obj, method, values, groups)
    log violations
    assert violations.size() == 1
    def violation = violations.iterator().next()
    log violation

    // should be 'text' instead of 'arg0'
    assert violation.propertyPath.toString() == 'test.text'
  }
}
