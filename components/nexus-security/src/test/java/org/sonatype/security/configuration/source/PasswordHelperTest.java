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

import java.io.FileNotFoundException;

import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.sisu.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.sonatype.security.configuration.source.PhraseService.LEGACY_PHRASE_SERVICE;

/**
 * UT for {@link PasswordHelper}.
 */
public class PasswordHelperTest
    extends TestSupport
{
  private PasswordHelper legacyPasswordHelper;

  private PasswordHelper customPasswordHelper;

  @Before
  public void setUp() throws Exception {
    legacyPasswordHelper = new PasswordHelper(new DefaultPlexusCipher(), LEGACY_PHRASE_SERVICE);
    customPasswordHelper = new PasswordHelper(new DefaultPlexusCipher(), new AbstractPhraseService(true)
    {
      @Override
      protected String getMasterPhrase() {
        return "sterces, sterces, sterces";
      }
    });
  }

  @Test
  public void testCustomMasterPhrase() throws Exception {
    String password = "clear-text-password";
    String encodedPass = customPasswordHelper.encrypt(password);

    try {
      legacyPasswordHelper.decrypt(encodedPass);
      fail("Expected PlexusCipherException");
    }
    catch (PlexusCipherException e) {
      // expected: default phrase should not work here
    }

    assertThat(password, is(customPasswordHelper.decrypt(encodedPass)));
  }

  @Test
  public void testLegacyPhraseFallback() throws Exception {
    String password = "clear-text-password";
    String encodedPass = legacyPasswordHelper.encrypt(password);

    assertThat(password, is(legacyPasswordHelper.decrypt(encodedPass)));

    // should still work by falling back to legacy pass-phrase
    assertThat(password, is(customPasswordHelper.decrypt(encodedPass)));
  }

  @Test
  public void testCustomPhraseFile() throws Exception {
    PhraseService phraseService = new FilePhraseService(util.resolveFile("target/test-classes/custom.enc"));
    PasswordHelper underTest = new PasswordHelper(new DefaultPlexusCipher(), phraseService);

    String password = "clear-text-password";
    String encodedPass = underTest.encrypt(password);

    assertThat(password, is(underTest.decrypt(encodedPass)));
  }

  @Test
  public void testMissingPhraseFile() throws Exception {
    PhraseService phraseService = new FilePhraseService(util.resolveFile("target/test-classes/missing.enc"));
    PasswordHelper underTest = new PasswordHelper(new DefaultPlexusCipher(), phraseService);

    String password = "clear-text-password";
    try {
      underTest.encrypt(password);
      fail("Expected RuntimeException wrapping FileNotFoundException");
    }
    catch (RuntimeException e) {
      assertThat(e.getCause(), is(instanceOf(FileNotFoundException.class)));
    }
  }
}
