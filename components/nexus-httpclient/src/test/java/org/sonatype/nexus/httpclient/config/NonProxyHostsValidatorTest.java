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
package org.sonatype.nexus.httpclient.config;

import java.util.Set;

import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;

import javax.validation.ConstraintValidatorContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for {@link NonProxyHostsValidator}.
 */
public class NonProxyHostsValidatorTest
    extends TestSupport
{

  @Mock
  private ConstraintValidatorContext context;

  private NonProxyHostsValidator validator = new NonProxyHostsValidator();

  private void validateAndExpect(String expression, boolean expected) {
    assertThat(validator.isValid(Set.of(expression), context), equalTo(expected));
  }

  @Test
  public void validationPositiveTest() {
    validateAndExpect("sonatype.org", true);
    validateAndExpect("*.sonatype.org", true);
    validateAndExpect("*.sonatype.*", true);
    validateAndExpect("1.2.3.4", true);
    validateAndExpect("*.2.3.4", true);
    validateAndExpect("1.2.3.*", true);
    validateAndExpect("*.2.3.*", true);
    validateAndExpect("10.*", true);
    validateAndExpect("*.10", true);
    validateAndExpect("csétamás.hu", true);
    validateAndExpect("2001:db8:85a3:8d3:1319:8a2e:370:7348", true);
    validateAndExpect("[2001:db8:85a3:8d3:1319:8a2e:370:7348]", true);
    validateAndExpect("[::1]", true);
    validateAndExpect("*:8]", true);
    validateAndExpect("[:*", true);
    validateAndExpect("localhost", true);
  }

  @Test
  public void validationNegativeTest() {
    // these below are the "best effort" we can rule out
    validateAndExpect("", false);
    validateAndExpect("  ", false);
    validateAndExpect("foo|sonatype.org", false);
    validateAndExpect("comma,com", false);
    validateAndExpect("[*:8]", false);
  }
}
