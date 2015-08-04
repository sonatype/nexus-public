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
package org.sonatype.security.ldap.usermanagement;

import org.sonatype.security.ldap.dao.LdapUser;
import org.sonatype.security.ldap.realms.LdapManager;
import org.sonatype.security.usermanagement.User;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LdapUserManager}.
 */
public class LdapUserManagerMockedTest
    extends TestSupport
{

  private LdapUserManager underTest;

  @Mock
  private LdapManager ldapManager;

  @Mock
  private LdapUser user;

  @Before
  public void setup() {
    this.underTest = new LdapUserManager(ldapManager);
  }

  @Test
  public void stripEmailWhitespace()
      throws Exception
  {
    when(ldapManager.getUser("test")).thenReturn(user);
    when(user.getEmail()).thenReturn(" email@with.whitespace.invalid ");

    final User user = underTest.getUser("test");

    assertThat(user.getEmailAddress(), is("email@with.whitespace.invalid"));
  }

}
