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
package org.sonatype.security.ldap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.sonatype.security.authentication.AuthenticationException;
import org.sonatype.security.ldap.dao.LdapUser;
import org.sonatype.security.ldap.dao.password.PasswordEncoderManager;

import org.apache.shiro.realm.ldap.LdapContextFactory;
import org.apache.shiro.realm.ldap.LdapUtils;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
@Named
public class LdapAuthenticator
{
  private final PasswordEncoderManager passwordManager;

  @Inject
  public LdapAuthenticator(final PasswordEncoderManager passwordManager) {
    this.passwordManager = checkNotNull(passwordManager);
  }

  public void authenticateUserWithPassword(LdapUser ldapUser, String password) throws AuthenticationException {
    // use the passwordmanager
    if (!this.passwordManager.isPasswordValid(ldapUser.getPassword(), password, null)) {
      throw new AuthenticationException("User '" + ldapUser.getUsername() + "' cannot be authenticated.");
    }
  }

  public void authenticateUserWithBind(LdapUser ldapUser, String password,
                                       LdapContextFactory ldapContextFactory, String authScheme)
      throws AuthenticationException
  {
    String userId = ldapUser.getUsername();

    // Binds using the username and password provided by the user.

    String bindUsername = ldapUser.getDn();

    // if we are authorizing against DIGEST-MD5 or CRAM-MD5 then username is not the DN
    if ("DIGEST-MD5".equals(authScheme) || "CRAM-MD5".equals(authScheme)) {
      bindUsername = userId;
    }

    // check using bind
    this.checkPasswordUsingBind(ldapContextFactory, bindUsername, password);

  }

  private void checkPasswordUsingBind(LdapContextFactory ldapContextFactory, String user, String pass)
      throws AuthenticationException
  {
    LdapContext ctx = null;
    try {
      ctx = ldapContextFactory.getLdapContext(user, pass);
    }
    catch (javax.naming.AuthenticationException e) {
      throw new AuthenticationException("User '" + user + "' cannot be authenticated.", e);
    }
    catch (NamingException e) {
      throw new AuthenticationException("User '" + user + "' cannot be authenticated.", e);
    }
    finally {
      LdapUtils.closeContext(ctx);
    }
  }

}
