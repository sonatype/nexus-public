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
package org.sonatype.nexus.jsecurity;

import org.sonatype.nexus.test.PlexusTestCaseSupport;
import org.sonatype.security.usermanagement.DefaultPasswordGenerator;
import org.sonatype.security.usermanagement.PasswordGenerator;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.junit.Test;

public class DefaultPasswordGeneratorTest
    extends PlexusTestCaseSupport
{

  @Override
  protected void customizeContainerConfiguration(ContainerConfiguration configuration) {
    configuration.setAutoWiring(true);
    configuration.setClassPathScanning(PlexusConstants.SCANNING_CACHE);
  }

  protected DefaultPasswordGenerator pwGenerator;

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    pwGenerator = (DefaultPasswordGenerator) this.lookup(PasswordGenerator.class);
  }

  @Test
  public void testGeneratePassword()
      throws Exception
  {
    String pw = pwGenerator.generatePassword(10, 10);

    assertTrue(pw != null);
    assertTrue(pw.length() == 10);

    String encrypted = pwGenerator.hashPassword(pw);
    String encrypted2 = pwGenerator.hashPassword(pw);

    assertTrue(encrypted != null);
    assertTrue(encrypted2 != null);
    assertFalse(pw.equals(encrypted));
    assertFalse(pw.equals(encrypted2));
    assertTrue(encrypted.equals(encrypted2));

    String newPw = pwGenerator.generatePassword(10, 10);

    assertTrue(newPw != null);
    assertTrue(newPw.length() == 10);
    assertFalse(pw.equals(newPw));

    String newEncrypted = pwGenerator.hashPassword(newPw);
    String newEncrypted2 = pwGenerator.hashPassword(newPw);

    assertTrue(newEncrypted != null);
    assertTrue(newEncrypted2 != null);
    assertFalse(newPw.equals(newEncrypted));
    assertFalse(newPw.equals(newEncrypted2));
    assertTrue(newEncrypted.equals(newEncrypted2));
    assertFalse(encrypted.equals(newEncrypted));
  }
}
