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
package org.sonatype.nexus.validation.internal;

import javax.validation.ConstraintViolation;
import javax.validation.executable.ExecutableValidator;

import org.sonatype.nexus.bootstrap.validation.ValidationConfiguration;
import org.sonatype.nexus.validation.Validate;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ValidationAspectTest

{
  private ValidationAspect validationAspect = new ValidationAspect();;

  @Mock
  private ProceedingJoinPoint joinPoint;

  @Mock
  private MethodSignature methodSignature;

  @Mock
  private ExecutableValidator methodValidator;

  @Mock
  private ConstraintViolation<String> constraintViolation;

  @Before
  public void setUp() {
    ValidationConfiguration.EXECUTABLE_VALIDATOR = methodValidator;
  }

  @After
  public void teardown() {
    ValidationConfiguration.EXECUTABLE_VALIDATOR = null;
  }

  @Test
  public void testValidationPasses() throws Throwable {
    // Mock behavior
    when(joinPoint.getTarget()).thenReturn(new TestClass());
    when(joinPoint.getSignature()).thenReturn(methodSignature);
    when(methodSignature.getMethod()).thenReturn(TestClass.class.getMethod("testMethod", String.class));
    when(joinPoint.getArgs()).thenReturn(new Object[]{"valid"}); // Valid argument
    when(joinPoint.proceed()).thenReturn("success");

    // Execute the aspect
    Object result = validationAspect.validateMethod(joinPoint,
        TestClass.class.getMethod("testMethod", String.class).getAnnotation(Validate.class));

    // Verify interactions
    verify(joinPoint).proceed();
    assert result.equals("success");
  }

  // Internal test class with a method annotated with @Validate
  private static class TestClass
  {
    @Validate
    public String testMethod(final String input) {
      return "Processed: " + input;
    }
  }
}
