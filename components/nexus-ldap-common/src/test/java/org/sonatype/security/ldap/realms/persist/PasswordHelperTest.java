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
package org.sonatype.security.ldap.realms.persist;

import org.sonatype.security.configuration.source.AbstractPhraseService;
import org.sonatype.security.ldap.upgrade.cipher.DefaultPlexusCipher;
import org.sonatype.security.ldap.upgrade.cipher.PlexusCipherException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.sonatype.security.configuration.source.PhraseService.LEGACY_PHRASE_SERVICE;

public class PasswordHelperTest
{
  private PasswordHelper legacyPasswordHelper;

  private PasswordHelper customPasswordHelper;

  @Before
  public void setUp() throws Exception {
    legacyPasswordHelper = new DefaultPasswordHelper(new DefaultPlexusCipher(), LEGACY_PHRASE_SERVICE);
    customPasswordHelper = new DefaultPasswordHelper(new DefaultPlexusCipher(), new AbstractPhraseService(true)
    {
      @Override
      protected String getMasterPhrase() {
        return "sterces, sterces, sterces";
      }
    });
  }

  @Test
  public void testValidPass() throws Exception {
    PasswordHelper ph = legacyPasswordHelper;

    String password = "PASSWORD";
    String encodedPass = ph.encrypt(password);
    Assert.assertEquals(password, ph.decrypt(encodedPass));
  }

  @Test
  public void testNullEncrypt() throws Exception {
    PasswordHelper ph = legacyPasswordHelper;
    Assert.assertNull(ph.encrypt(null));
  }

  @Test
  public void testNullDecrypt() throws Exception {
    PasswordHelper ph = legacyPasswordHelper;
    Assert.assertNull(ph.decrypt(null));
  }

  @Test
  public void testDecryptNonEncyprtedPassword() throws Exception {
    PasswordHelper ph = legacyPasswordHelper;

    try {
      ph.decrypt("clear-text-password");
      Assert.fail("Expected: PlexusCipherException");
    }
    catch (PlexusCipherException e) {
      // expected
    }

  }

  @Test
  public void testCustomMasterPhrase() throws Exception {
    String password = "clear-text-password";
    String encodedPass = customPasswordHelper.encrypt(password);

    try {
      legacyPasswordHelper.decrypt(encodedPass);
      Assert.fail("Expected PlexusCipherException");
    }
    catch (PlexusCipherException e) {
      // expected: default phrase should not work here
    }

    Assert.assertEquals(password, customPasswordHelper.decrypt(encodedPass));
  }

  @Test
  public void testLegacyPhraseFallback() throws Exception {
    String password = "clear-text-password";
    String encodedPass = legacyPasswordHelper.encrypt(password);

    Assert.assertEquals(password, legacyPasswordHelper.decrypt(encodedPass));

    // should still work by falling back to legacy pass-phrase
    Assert.assertEquals(password, customPasswordHelper.decrypt(encodedPass));
  }
}
