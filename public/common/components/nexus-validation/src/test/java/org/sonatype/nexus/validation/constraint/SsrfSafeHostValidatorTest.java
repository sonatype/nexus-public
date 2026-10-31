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

import org.sonatype.nexus.validation.ssrf.AntiSsrfService;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SsrfSafeHostValidator}.
 */
@ExtendWith(MockitoExtension.class)
class SsrfSafeHostValidatorTest
{
  @Mock
  private ConstraintValidatorContext context;

  @Mock
  private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

  @Mock
  private AntiSsrfService antiSsrfService;

  @InjectMocks
  private SsrfSafeHostValidator underTest;

  @BeforeEach
  void setUp() {
    lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    lenient().when(violationBuilder.addConstraintViolation()).thenReturn(context);
  }

  @Test
  void testNullHostIsvalid() {
    assertThat(underTest.isValid(null, context), is(true));
  }

  @Test
  void testPublicHostIsValid() {
    assertThat(underTest.isValid("example.com", context), is(true));
  }

  @Test
  void testLocalhostHostIsInvalid() {
    doThrow(new ValidationException("Host resolves to private/local network address"))
        .when(antiSsrfService)
        .validateHostWithoutCache("localhost");

    assertThat(underTest.isValid("localhost", context), is(false));
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate("Host resolves to private/local network address");
  }

  @Test
  void testLoopbackIpIsInvalid() {
    doThrow(new ValidationException("Host resolves to private/local network address"))
        .when(antiSsrfService)
        .validateHostWithoutCache("127.0.0.1");

    assertThat(underTest.isValid("127.0.0.1", context), is(false));
  }

  @Test
  void testPrivateIpIsInvalid() {
    doThrow(new ValidationException("Host resolves to private/local network address"))
        .when(antiSsrfService)
        .validateHostWithoutCache("10.0.0.1");

    assertThat(underTest.isValid("10.0.0.1", context), is(false));
  }
}
