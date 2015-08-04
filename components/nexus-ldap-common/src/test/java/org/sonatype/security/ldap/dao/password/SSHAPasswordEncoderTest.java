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
package org.sonatype.security.ldap.dao.password;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Assert;
import org.junit.Test;

public class SSHAPasswordEncoderTest
    extends TestSupport
{
  @Test
  public void testVerify()
      throws Exception
  {
    String encPassword = "{SSHA}FBProvj7X/SW+7nYtd83uX/noSQ6reGv";

    SSHAPasswordEncoder encoder = new SSHAPasswordEncoder();

    Assert.assertTrue(encoder.isPasswordValid(encPassword, "password", null));
    Assert.assertTrue(encoder.isPasswordValid("{ssha}FBProvj7X/SW+7nYtd83uX/noSQ6reGv", "password", null));
    Assert.assertFalse(encoder.isPasswordValid(
        "{ssha}FBProvj7X/SW+7nYtd83uX/noSQ6reGv",
        "FBProvj7X/SW+7nYtd83uX/noSQ6reGv",
        null));
    Assert.assertFalse(encoder.isPasswordValid(encPassword, "Password", null));
    Assert.assertFalse(encoder.isPasswordValid(encPassword, "junk", null));
    Assert.assertFalse(encoder.isPasswordValid(encPassword, "", null));
    Assert.assertFalse(encoder.isPasswordValid(encPassword, null, null));

    Assert.assertTrue(encoder.isPasswordValid("FBProvj7X/SW+7nYtd83uX/noSQ6reGv", "password", null));
    Assert.assertFalse(encoder.isPasswordValid("notValid", "password", null));
  }

  @Test
  public void testEncode()
      throws Exception
  {
    SSHAPasswordEncoder encoder = new SSHAPasswordEncoder();
    byte[] salt = new byte[]{58, -83, -31, -81};

    Assert.assertEquals("{SSHA}FBProvj7X/SW+7nYtd83uX/noSQ6reGv", encoder.encodePassword("password", salt));

    try {
      // salt must be byte[], this salt below is string and should throw IAE
      encoder.encodePassword("password", ":abc");
      Assert.fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      // expected
    }

    String clearPass = "foobar";
    Assert.assertTrue(encoder.isPasswordValid(encoder.encodePassword(clearPass, null), clearPass, null));
    Assert.assertTrue(encoder.isPasswordValid(
        encoder.encodePassword(clearPass, "byte[]".getBytes()),
        clearPass,
        null));

    try {
      encoder.encodePassword(clearPass, new Object());
      Assert.fail("expected: IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      // expected
    }
  }
}
