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
package org.sonatype.security.ldap.dao;

import java.util.Arrays;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Tests for LdapAuthConfiguration
 */
public class LdapAuthConfigurationTest
{

  @Test
  public void testGetUserAttributes() {
    LdapAuthConfiguration ldapAuthConfiguration = new LdapAuthConfiguration();
    ldapAuthConfiguration.setEmailAddressAttribute("emailAddressAttribute");
    ldapAuthConfiguration.setPasswordAttribute(null);
    // unset the defaults (using a mix of empty strings and nulls
    ldapAuthConfiguration.setUserIdAttribute("");
    ldapAuthConfiguration.setUserRealNameAttribute(null);
    ldapAuthConfiguration.setUserMemberOfAttribute("");
    ldapAuthConfiguration.setWebsiteAttribute(null);

    String[] userAttributes = ldapAuthConfiguration.getUserAttributes();
    Assert.assertEquals("Actual result: " + Arrays.asList(userAttributes), 1, userAttributes.length);
    //only non null attributes should be added to the list
    Assert.assertEquals("emailAddressAttribute", userAttributes[0]);

    // set a few more then check the count
    ldapAuthConfiguration.setPasswordAttribute("passwordAttribute");
    ldapAuthConfiguration.setUserIdAttribute("userIdAttribute");

    userAttributes = ldapAuthConfiguration.getUserAttributes();
    Assert.assertEquals("Actual result: " + Arrays.asList(userAttributes), 3, userAttributes.length);

  }
}
