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

import org.sonatype.nexus.test.PlexusTestCaseSupport;
import org.sonatype.security.ldap.upgrade.cipher.PlexusCipherException;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;

import junit.framework.Assert;
import org.junit.Test;

public class PasswordHelperTest
    extends PlexusTestCaseSupport
{

  @Override
  protected void customizeContainerConfiguration(final ContainerConfiguration containerConfiguration) {
    super.customizeContainerConfiguration(containerConfiguration);
    containerConfiguration.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
  }

  public PasswordHelper getPasswordHelper()
      throws Exception
  {
    return (PasswordHelper) this.lookup(PasswordHelper.class);
  }

  @Test
  public void testValidPass()
      throws Exception
  {
    PasswordHelper ph = this.getPasswordHelper();

    String password = "PASSWORD";
    String encodedPass = ph.encrypt(password);
    Assert.assertEquals(password, ph.decrypt(encodedPass));
  }

  @Test
  public void testNullEncrypt()
      throws Exception
  {
    PasswordHelper ph = this.getPasswordHelper();
    Assert.assertNull(ph.encrypt(null));
  }

  @Test
  public void testNullDecrypt()
      throws Exception
  {
    PasswordHelper ph = this.getPasswordHelper();
    Assert.assertNull(ph.decrypt(null));
  }

  @Test
  public void testDecryptNonEncyprtedPassword()
      throws Exception
  {
    PasswordHelper ph = this.getPasswordHelper();

    try {
      ph.decrypt("clear-text-password");
      Assert.fail("Expected: PlexusCipherException");
    }
    catch (PlexusCipherException e) {
      // expected
    }

  }

}
