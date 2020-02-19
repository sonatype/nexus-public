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
package org.sonatype.nexus.security.authc;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CRole;
import org.sonatype.nexus.security.config.CUser;
import org.sonatype.nexus.security.config.memory.MemoryCPrivilege;
import org.sonatype.nexus.security.config.memory.MemoryCUser;
import org.sonatype.nexus.security.internal.AuthenticatingRealmImpl;
import org.sonatype.nexus.security.internal.SecurityConfigurationManagerImpl;

import com.google.common.hash.Hashing;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.CredentialsException;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.realm.Realm;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AuthenticatingRealmImplTest
    extends AbstractSecurityTest
{
  private AuthenticatingRealmImpl realm;

  private SecurityConfigurationManagerImpl configurationManager;

  private PasswordService passwordService;

  private CUser testUser;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    realm = (AuthenticatingRealmImpl) lookup(Realm.class, AuthenticatingRealmImpl.NAME);
    configurationManager = lookup(SecurityConfigurationManagerImpl.class);
    passwordService = lookup(PasswordService.class, "default");
  }

  @Test
  public void testSuccessfulAuthentication() throws Exception {
    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE);

    UsernamePasswordToken upToken = new UsernamePasswordToken("username", "password");
    AuthenticationInfo ai = realm.getAuthenticationInfo(upToken);
    String password = new String((char[]) ai.getCredentials());
    assertThat(this.passwordService.passwordsMatch("password", password), is(true));
  }

  @Test
  public void testCreateWithPassowrd() throws Exception {
    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE);

    String clearPassword = "default-password";
    String username = "testCreateWithPassowrdEmailUserId";

    CUser user = user("testCreateWithPassowrdEmail@somewhere", "testCreateWithPassowrdEmail",
        "testCreateWithPassowrdEmail", CUser.STATUS_ACTIVE, username, null);

    Set<String> roles = new HashSet<String>();
    roles.add("role");

    configurationManager.createUser(user, clearPassword, roles);

    UsernamePasswordToken upToken = new UsernamePasswordToken("testCreateWithPassowrdEmailUserId", clearPassword);
    AuthenticationInfo ai = realm.getAuthenticationInfo(upToken);
    String password = new String((char[]) ai.getCredentials());

    assertThat(passwordService.passwordsMatch(clearPassword, password), is(true));
  }

  @Test
  public void testFailedAuthentication() throws Exception {
    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE);

    UsernamePasswordToken upToken = new UsernamePasswordToken("username", "badpassword");

    thrown.expect(IncorrectCredentialsException.class);
    realm.getAuthenticationInfo(upToken);
  }

  @Test
  public void testDisabledAuthentication() throws Exception {
    buildTestAuthenticationConfig(CUser.STATUS_DISABLED);
    UsernamePasswordToken upToken = new UsernamePasswordToken("username", "password");

    thrown.expect(DisabledAccountException.class);
    realm.getAuthenticationInfo(upToken);
  }

  @Test
  public void testGetAuthenticationInfo_userStatusChangePassword() throws Exception {
    buildTestAuthenticationConfig(CUser.STATUS_CHANGE_PASSWORD);

    UsernamePasswordToken upToken = new UsernamePasswordToken("username", "password");
    AuthenticationInfo ai = realm.getAuthenticationInfo(upToken);
    String password = new String((char[]) ai.getCredentials());
    assertThat(this.passwordService.passwordsMatch("password", password), is(true));
  }

  @Test
  public void testDetectLegacyUser() throws Exception {
    String password = "password";
    String username = "username";
    buildLegacyTestAuthenticationConfig(password);

    UsernamePasswordToken upToken = new UsernamePasswordToken(username, password);
    AuthenticationInfo ai = realm.getAuthenticationInfo(upToken);
    CUser updatedUser = this.configurationManager.readUser(username);
    String hash = new String((char[]) ai.getCredentials());

    assertThat(passwordService.passwordsMatch(password, hash), is(true));
    assertThat(passwordService.passwordsMatch(password, updatedUser.getPassword()), is(true));
  }

  @Test
  public void testNoneExistentUser() throws Exception {
    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE);
    UsernamePasswordToken upToken = new UsernamePasswordToken("non-existent-user", "password");

    thrown.expect(UnknownAccountException.class);
    realm.getAuthenticationInfo(upToken);
  }

  @Test
  public void testEmptyPassword() throws Exception {
    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE);
    UsernamePasswordToken upToken = new UsernamePasswordToken("username", (String) null);

    thrown.expect(CredentialsException.class);
    realm.getAuthenticationInfo(upToken);
  }

  private void buildTestAuthenticationConfig(final String status) throws Exception {
    buildTestAuthenticationConfig(status, this.hashPassword("password"));
  }

  private void buildTestAuthenticationConfig(final String status, final String hash) throws Exception {
    CPrivilege priv = new MemoryCPrivilege();
    priv.setId("priv");
    priv.setName("name");
    priv.setDescription("desc");
    priv.setType("method");
    priv.setProperty("method", "read");
    priv.setProperty("permission", "somevalue");

    configurationManager.createPrivilege(priv);

    CRole role = configurationManager.newRole();
    role.setName("name");
    role.setId("role");
    role.setDescription("desc");
    role.addPrivilege("priv");

    configurationManager.createRole(role);

    testUser = user("dummyemail@somewhere", "dummyFirstName", "dummyLastName", status, "username", hash);

    Set<String> roles = new HashSet<String>();
    roles.add("role");

    configurationManager.createUser(testUser, roles);
  }

  private String hashPassword(final String password) {
    return passwordService.encryptPassword(password);
  }

  @SuppressWarnings("deprecation")
  private String legacyHashPassword(final String password) {
    return Hashing.sha1().hashString(password, StandardCharsets.UTF_8).toString();
  }

  private void buildLegacyTestAuthenticationConfig(final String password) throws Exception {
    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE, legacyHashPassword(password));
  }

  private static CUser user(
      final String email,
      final String firstName,
      final String lastName,
      final String status,
      final String id,
      final String passwordHash)
  {
    CUser testUser = new MemoryCUser();
    testUser.setEmail(email);
    testUser.setFirstName(firstName);
    testUser.setLastName(lastName);
    testUser.setStatus(status);
    testUser.setId(id);
    testUser.setPassword(passwordHash);
    return testUser;
  }
}
