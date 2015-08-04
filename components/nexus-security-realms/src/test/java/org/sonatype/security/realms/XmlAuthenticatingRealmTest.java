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
package org.sonatype.security.realms;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.security.model.CPrivilege;
import org.sonatype.security.model.CProperty;
import org.sonatype.security.model.CRole;
import org.sonatype.security.model.CUser;
import org.sonatype.security.realms.tools.DefaultConfigurationManager;
import org.sonatype.security.usermanagement.PasswordGenerator;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.realm.Realm;
import org.eclipse.sisu.launch.InjectedTestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class XmlAuthenticatingRealmTest
    extends InjectedTestCase
{
  private final String SECURITY_FILE_PATH = getBasedir() + "/target/jsecurity/security.xml";

  private final String SECURITY_CONFIGURATION_FILE_PATH = getBasedir() + "/target/jsecurity/security-configuration.xml";

  private File configFile = new File(SECURITY_FILE_PATH);

  private XmlAuthenticatingRealm realm;

  private DefaultConfigurationManager configurationManager;

  private PasswordGenerator passwordGenerator;

  private PasswordService passwordService;

  private CUser testUser;

  @Override
  public void configure(Properties properties) {
    properties.put("security-xml-file", SECURITY_FILE_PATH);
    properties.put("application-conf", SECURITY_CONFIGURATION_FILE_PATH);
    super.configure(properties);
  }

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    realm = (XmlAuthenticatingRealm) lookup(Realm.class, "XmlAuthenticatingRealm");

    configurationManager = lookup(DefaultConfigurationManager.class);

    configurationManager.clearCache();

    passwordGenerator = lookup(PasswordGenerator.class, "default");
    passwordService = lookup(PasswordService.class, "default");

    configFile.delete();
  }

  public void testSuccessfulAuthentication()
      throws Exception
  {
    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE);

    UsernamePasswordToken upToken = new UsernamePasswordToken("username", "password");

    AuthenticationInfo ai = realm.getAuthenticationInfo(upToken);

    String password = new String((char[]) ai.getCredentials());

    assertThat(this.passwordService.passwordsMatch("password", password), is(true));
  }

  public void testCreateWithPassowrd()
      throws Exception
  {
    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE);

    String clearPassword = "default-password";
    String username = "testCreateWithPassowrdEmailUserId";

    CUser user = new CUser();
    user.setEmail("testCreateWithPassowrdEmail@somewhere");
    user.setFirstName("testCreateWithPassowrdEmail");
    user.setLastName("testCreateWithPassowrdEmail");
    user.setStatus(CUser.STATUS_ACTIVE);
    user.setId(username);

    Set<String> roles = new HashSet<String>();
    roles.add("role");

    configurationManager.createUser(user, clearPassword, roles);

    UsernamePasswordToken upToken = new UsernamePasswordToken("testCreateWithPassowrdEmailUserId", clearPassword);

    AuthenticationInfo ai = realm.getAuthenticationInfo(upToken);

    String password = new String((char[]) ai.getCredentials());

    assertThat(this.passwordService.passwordsMatch(clearPassword, password), is(true));
  }

  public void testFailedAuthentication()
      throws Exception
  {
    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE);

    UsernamePasswordToken upToken = new UsernamePasswordToken("username", "badpassword");

    try {
      realm.getAuthenticationInfo(upToken);

      fail("Authentication should have failed");
    }
    catch (AuthenticationException e) {
      // good
    }
  }

  public void testDisabledAuthentication()
      throws Exception
  {
    buildTestAuthenticationConfig(CUser.STATUS_DISABLED);

    UsernamePasswordToken upToken = new UsernamePasswordToken("username", "password");

    try {
      realm.getAuthenticationInfo(upToken);

      fail("Authentication should have failed");
    }
    catch (AuthenticationException e) {
      // good
    }
  }

  public void testDetectLegacyUser()
      throws Exception
  {
    String password = "password";
    String username = "username";
    buildLegacyTestAuthenticationConfig(password);

    UsernamePasswordToken upToken = new UsernamePasswordToken(username, password);
    AuthenticationInfo ai = realm.getAuthenticationInfo(upToken);
    CUser updatedUser = this.configurationManager.readUser(username);
    String hash = new String((char[]) ai.getCredentials());

    assertThat(this.passwordService.passwordsMatch(password, hash), is(true));
    assertThat(this.passwordService.passwordsMatch(password, updatedUser.getPassword()), is(true));
  }

  private void buildTestAuthenticationConfig(String status)
      throws InvalidConfigurationException
  {
    buildTestAuthenticationConfig(status, this.hashPassword("password"));
  }

  private void buildTestAuthenticationConfig(String status, String hash)
      throws InvalidConfigurationException
  {
    CPrivilege priv = new CPrivilege();
    priv.setId("priv");
    priv.setName("name");
    priv.setDescription("desc");
    priv.setType("method");

    CProperty prop = new CProperty();
    prop.setKey("method");
    prop.setValue("read");
    priv.addProperty(prop);

    prop = new CProperty();
    prop.setKey("permission");
    prop.setValue("somevalue");
    priv.addProperty(prop);

    configurationManager.createPrivilege(priv);

    CRole role = new CRole();
    role.setName("name");
    role.setId("role");
    role.setDescription("desc");
    role.setSessionTimeout(50);
    role.addPrivilege("priv");

    configurationManager.createRole(role);

    testUser = new CUser();
    testUser.setEmail("dummyemail@somewhere");
    testUser.setFirstName("dummyFirstName");
    testUser.setLastName("dummyLastName");
    testUser.setStatus(status);
    testUser.setId("username");
    testUser.setPassword(hash);

    Set<String> roles = new HashSet<String>();
    roles.add("role");

    configurationManager.createUser(testUser, roles);
    configurationManager.save();
  }

  private void buildLegacyTestAuthenticationConfig(String password)
      throws InvalidConfigurationException
  {
    buildTestAuthenticationConfig(CUser.STATUS_ACTIVE, this.legacyHashPassword(password));
  }

  private String hashPassword(String password) {
    return this.passwordService.encryptPassword(password);
  }

  private String legacyHashPassword(String password) {
    return this.passwordGenerator.hashPassword(password);
  }
}
