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
package com.sonatype.nexus.ssl.plugin.validator;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HostnameOrIpAddressValidatorTest
{
  private HostnameOrIpAddressValidator underTest;

  @Before
  public void setUp() {
    underTest = new HostnameOrIpAddressValidator();
  }

  /*
   * Null value is considered valid (other constraints such as @NotEmpty handle the null/empty case)
   */
  @Test
  public void isValid_nullValue() {
    assertThat(underTest.isValid(null, null), is(true));
  }

  @Test
  public void isValid_validHostname() {
    assertThat(underTest.isValid("example.com", null), is(true));
  }

  @Test
  public void isValid_validIpv4Address() {
    assertThat(underTest.isValid("192.168.1.1", null), is(true));
  }

  @Test
  public void isValid_validIpv6Address() {
    assertThat(underTest.isValid("2001:db8::1", null), is(true));
  }

  @Test
  public void isValid_localhostHostname() {
    assertThat(underTest.isValid("localhost", null), is(true));
  }

  @Test
  public void isValid_invalidValue() {
    assertThat(underTest.isValid("not a valid hostname or ip!", null), is(false));
  }
}
