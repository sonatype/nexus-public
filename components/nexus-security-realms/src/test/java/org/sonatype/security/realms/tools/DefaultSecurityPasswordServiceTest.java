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
package org.sonatype.security.realms.tools;

import org.sonatype.security.AbstractSecurityTestCase;

import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.crypto.hash.Hash;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DefaultSecurityPasswordServiceTest
    extends AbstractSecurityTestCase
{
  DefaultSecurityPasswordService passwordService;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    this.passwordService = (DefaultSecurityPasswordService) lookup(PasswordService.class, "default");
  }

  @Test
  public void testSha1Hash() {
    String password = "admin123";
    String sha1Hash = "f865b53623b121fd34ee5426c792e5c33af8c227";

    assertThat(this.passwordService.passwordsMatch(password, sha1Hash), is(true));
  }

  @Test
  public void testMd5Hash() {
    String password = "admin123";
    String md5Hash = "0192023a7bbd73250516f069df18b500";

    assertThat(this.passwordService.passwordsMatch(password, md5Hash), is(true));
  }

  @Test
  public void testShiro1HashFormat() {
    String password = "admin123";
    String shiro1Hash = "$shiro1$SHA-512$1024$zjU1u+Zg9UNwuB+HEawvtA==$IzF/OWzJxrqvB5FCe/2+UcZhhZYM2pTu0TEz7Ybnk65AbbEdUk9ntdtBzkN8P3gZby2qz6MHKqAe8Cjai9c4Gg==";

    assertThat(this.passwordService.passwordsMatch(password, shiro1Hash), is(true));
  }

  @Test
  public void testInvalidSha1Hash() {
    String password = "admin123";
    String sha1Hash = "f865b53623b121fd34ee5426c792e5c33af8c228";

    assertThat(this.passwordService.passwordsMatch(password, sha1Hash), is(false));
  }

  @Test
  public void testInvalidMd5Hash() {
    String password = "admin123";
    String md5Hash = "0192023a7bbd73250516f069df18b501";

    assertThat(this.passwordService.passwordsMatch(password, md5Hash), is(false));
  }

  @Test
  public void testInvalidShiro1HashFormat() {
    String password = "admin123";
    String shiro1Hash = "$shiro1$SHA-512$1024$zjU1u+Zg9UNwuB+HEawvtA==$IzF/OWzjxrqvB5FCe/2+UcZhhZYM2pTu0TEz7Ybnk65AbbEdUk9ntdtBzkN8P3gZby2qz6MHKqAe8Cjai9c4Gg==";

    assertThat(this.passwordService.passwordsMatch(password, shiro1Hash), is(false));
  }

  @Test
  public void testHash() {
    String password = "testpassword";
    Hash hash = this.passwordService.hashPassword(password);

    assertThat(this.passwordService.passwordsMatch(password, hash), is(true));
  }
}
