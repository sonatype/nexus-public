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
package org.sonatype.nexus.validation;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Test;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.validation.internal.AopAwareParanamerParameterNameProvider;

import java.lang.reflect.Method;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class ParanamerValidationTest
    extends TestSupport
{

  private ValidatorFactory factory;

  @Before
  public void setUp() {
    factory = Validation.byDefaultProvider()
        .configure()
        .parameterNameProvider(new AopAwareParanamerParameterNameProvider())
        .buildValidatorFactory();
  }

  private static class TestSubject
  {
    public void test(@NotNull String text) {
      // ignore
    }
  }

  @Test
  public void validateMethodResolveParameterName() throws NoSuchMethodException {
    Method method = TestSubject.class.getMethod("test", String.class);
    TestSubject obj = new TestSubject();

    Object[] values = {null};
    Class<?>[] groups = {};
    Set<ConstraintViolation<TestSubject>> violations =
        factory.getValidator().forExecutables().validateParameters(obj, method, values, groups);
    assertThat(violations, hasSize(1));
    ConstraintViolation<TestSubject> violation = violations.iterator().next();

    // should be 'text' instead of 'arg0'
    assertThat(violation.getPropertyPath().toString(), equalTo("test.text"));
  }
}
