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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;

import org.sonatype.nexus.bootstrap.validation.ValidationConfiguration;
import org.sonatype.nexus.validation.internal.SpringConstraintValidatorFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ConstraintViolationFactoryTest

{
  private static final String JAVA_EL_IMMEDIATE = "${2 + 2}";

  private static final String JAVA_EL_DEFERRED = "{message}";

  private static final String ANY_PATH = "foo";

  private ValidationConfiguration configuration = new ValidationConfiguration();

  @Mock
  private ApplicationContext context;

  @InjectMocks
  private SpringConstraintValidatorFactory constraintValidatorFactory;

  private ConstraintViolationFactory cvf;

  @Before
  public void setUp() throws Exception {
    when(context.getBean(any(Class.class))).thenThrow(new NoSuchBeanDefinitionException(NotNull.class));
    ValidatorFactory factory = configuration.validatorFactory(constraintValidatorFactory);
    cvf = new ConstraintViolationFactory(() -> configuration.validator(factory));
  }

  @After
  public void teardown() {
    ValidationConfiguration.EXECUTABLE_VALIDATOR = null;
  }

  @Test
  public void shouldStripJavaExpression() {
    // immediate evaluation of JAVA_EL_IMMEDIATE would yield 4, and that would mean we are open to code injection
    ConstraintViolation<?> violation = cvf.createViolation(ANY_PATH, JAVA_EL_IMMEDIATE);
    assertThat(violation.getMessage(), is("{2 + 2}"));
  }

  @Test
  public void shouldEvaluateDeferredExpressionLanguage() {
    // deferred evaluation of JAVA_EL_DEFERRED should yield content of HelperAnnotation.message
    ConstraintViolation<?> violation = cvf.createViolation(ANY_PATH, JAVA_EL_DEFERRED);
    assertThat(violation.getMessage(), is(""));
  }
}
