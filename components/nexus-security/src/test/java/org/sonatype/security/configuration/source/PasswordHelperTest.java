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
package org.sonatype.security.configuration.source;

import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

/**
 * UT for {@link PasswordHelper}.
 */
public class PasswordHelperTest
    extends TestSupport
{
  public PasswordHelper newPasswordHelper() throws PlexusCipherException {
    return new PasswordHelper(new DefaultPlexusCipher());
  }

  @Test
  public void testCustomMasterPhrase()
      throws Exception
  {
    String password = "clear-text-password";
    String encodedPass;

    try {
      System.setProperty("nexus.security.masterPhrase", "terces");
      encodedPass = newPasswordHelper().encrypt(password);
    }
    finally {
      System.clearProperty("nexus.security.masterPhrase");
    }

    try
    {
      newPasswordHelper().decrypt(encodedPass);
      fail("Expected PlexusCipherException");
    }
    catch (PlexusCipherException e) {
      // expected: default phrase should not work here
    }

    try {
      System.setProperty("nexus.security.masterPhrase", "terces");
      assertThat(password, is(newPasswordHelper().decrypt(encodedPass)));
    }
    finally {
      System.clearProperty("nexus.security.masterPhrase");
    }
  }

  @Test
  public void testLegacyPhraseFallback()
      throws Exception
  {
    String password = "clear-text-password";
    String encodedPass = newPasswordHelper().encrypt(password);

    assertThat(password, is(newPasswordHelper().decrypt(encodedPass)));

    try {
      System.setProperty("nexus.security.masterPhrase", "terces");
      // should still work by falling back to legacy pass-phrase
      assertThat(password, is(newPasswordHelper().decrypt(encodedPass)));
    }
    finally {
      System.clearProperty("nexus.security.masterPhrase");
    }
  }
}
