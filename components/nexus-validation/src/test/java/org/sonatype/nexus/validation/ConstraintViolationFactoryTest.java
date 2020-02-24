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

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.inject.Guice;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConstraintViolationFactoryTest
    extends TestSupport
{
  private static final String JAVA_EL_IMMEDIATE = "${2 + 2}";

  private static final String JAVA_EL_DEFERRED = "{message}";

  private static final String ANY_PATH = "foo";

  private ConstraintViolationFactory cvf;

  @Before
  public void setUp() throws Exception {
    cvf = Guice.createInjector(new ValidationModule()).getInstance(ConstraintViolationFactory.class);
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
