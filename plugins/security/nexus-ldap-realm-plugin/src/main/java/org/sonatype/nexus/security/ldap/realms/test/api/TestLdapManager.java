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
package org.sonatype.nexus.security.ldap.realms.test.api;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.security.ldap.realms.AbstractLdapManager;
import org.sonatype.security.ldap.LdapAuthenticator;
import org.sonatype.security.ldap.dao.LdapGroupDAO;
import org.sonatype.security.ldap.dao.LdapUserDAO;
import org.sonatype.security.ldap.realms.persist.LdapConfiguration;

/**
 * Must NOT be singleton!
 */
@Named(TestLdapManager.NAME)
public class TestLdapManager
    extends AbstractLdapManager
{
  public static final String NAME = "TestLdapManager";

  private LdapConfiguration ldapConfiguration;

  @Inject
  public TestLdapManager(LdapAuthenticator ldapAuthenticator, LdapUserDAO ldapUserManager,
      LdapGroupDAO ldapGroupManager, LdapConfiguration ldapConfiguration)
  {
    super(ldapAuthenticator, ldapUserManager, ldapGroupManager, ldapConfiguration);
  }

  @Override
  public LdapConfiguration getLdapConfiguration() {
    return ldapConfiguration;
  }

  public void setLdapConfiguration(LdapConfiguration ldapConfiguration) {
    this.ldapConfiguration = ldapConfiguration;
  }

}
