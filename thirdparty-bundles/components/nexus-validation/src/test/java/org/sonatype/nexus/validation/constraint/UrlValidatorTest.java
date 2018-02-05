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

import java.net.URI;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UrlValidatorTest
    extends TestSupport
{

  UrlValidator urlValidator;

  @Before
  public void setup() throws Exception {
    urlValidator = new UrlValidator();
  }

  @Test
  public void extendsConstraintValidatorSupport() throws Exception {
    assertThat(urlValidator, is(instanceOf(ConstraintValidatorSupport.class)));
  }

  @Test
  public void validWhenNull() throws Exception {
    assertTrue(urlValidator.isValid(null, null));
  }

  @Test
  public void notValidWhenEmpty() throws Exception {
    assertFalse(urlValidator.isValid(URI.create(""), null));
  }

  @Test
  public void validHttp() throws Exception {
    assertTrue(urlValidator.isValid(URI.create("http://www.example.com"), null));
  }

  @Test
  public void validHttps() throws Exception {
    assertTrue(urlValidator.isValid(URI.create("https://www.example.com"), null));
  }

  @Test
  public void invalid() throws Exception {
    assertFalse(urlValidator.isValid(URI.create("www"), null));
  }

  @Test
  public void invalidWithoutProtocol() throws Exception {
    assertFalse(urlValidator.isValid(URI.create("www.example.com"), null));
  }

}
