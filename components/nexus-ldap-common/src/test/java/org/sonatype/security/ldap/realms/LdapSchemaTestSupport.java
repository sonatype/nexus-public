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
package org.sonatype.security.ldap.realms;

import java.util.Set;
import java.util.SortedSet;

import org.sonatype.security.ldap.LdapConstants;
import org.sonatype.security.ldap.LdapTestSupport;
import org.sonatype.security.ldap.dao.LdapDAOException;
import org.sonatype.security.ldap.dao.LdapUser;
import org.sonatype.security.ldap.dao.NoSuchLdapGroupException;
import org.sonatype.security.ldap.dao.NoSuchLdapUserException;
import org.sonatype.security.ldap.realms.persist.LdapConfiguration;

import junit.framework.Assert;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.Realm;
import org.codehaus.plexus.context.Context;
import org.junit.Test;

public abstract class LdapSchemaTestSupport
    extends LdapTestSupport
{

  private LdapManager ldapManager;

  private Realm realm;

  @Override
  protected void customizeContext(Context context) {
    super.customizeContext(context);

    String classname = this.getClass().getName();
    context.put("test-path", getBasedir() + "/target/test-classes/" + classname.replace('.', '/'));
  }

  /*
   * (non-Javadoc)
   * @see org.sonatype.ldaptestsuite.AbstractLdapTestEnvironment#setUp()
   */
  @Override
  public void setUp()
      throws Exception
  {
    super.setUp();
    assertNotNull(lookup(LdapConfiguration.class));
    this.realm = this.lookup(Realm.class, LdapConstants.REALM_NAME);
    this.ldapManager = this.lookup(LdapManager.class);
  }

  @Test
  public void testUserManager()
      throws Exception
  {
    LdapUser user = this.ldapManager.getUser("cstamas");
    assertEquals("cstamas", user.getUsername());
    // assertEquals( "Tamas Cservenak", user.getRealName() );

    assertTrue(this.isPasswordsEncrypted() || ("cstamas123".equals(user.getPassword())));

    user = this.ldapManager.getUser("brianf");
    assertEquals("brianf", user.getUsername());
    // assertEquals( "Brian Fox", user.getRealName() );
    assertTrue(this.isPasswordsEncrypted() || ("brianf123".equals(user.getPassword())));

    user = this.ldapManager.getUser("jvanzyl");
    assertEquals("jvanzyl", user.getUsername());
    // assertEquals( "Jason Van Zyl", user.getRealName() );
    assertTrue(this.isPasswordsEncrypted() || ("jvanzyl123".equals(user.getPassword())));

    try {
      user = this.ldapManager.getUser("intruder");
      fail("Expected NoSuchUserException");
    }
    catch (NoSuchLdapUserException e) {
      // good
    }
  }

  @Test
  public void testGroupManager()
      throws Exception
  {
    Set<String> groups = this.ldapManager.getUserRoles("cstamas");
    assertEquals(2, groups.size());
    assertTrue(groups.contains("public"));
    assertTrue(groups.contains("snapshots"));

    groups = this.ldapManager.getUserRoles("brianf");
    assertEquals(2, groups.size());
    assertTrue(groups.contains("public"));
    assertTrue(groups.contains("releases"));

    groups = this.ldapManager.getUserRoles("jvanzyl");
    assertEquals(3, groups.size());
    assertTrue(groups.contains("public"));
    assertTrue(groups.contains("releases"));
    assertTrue(groups.contains("snapshots"));
  }

  @Test
  public void testSuccessfulAuthentication()
      throws Exception
  {

    UsernamePasswordToken upToken = new UsernamePasswordToken("brianf", "brianf123");

    AuthenticationInfo ai = realm.getAuthenticationInfo(upToken);

    assertNull(ai.getCredentials());

    // String password = new String( (char[]) ai.getCredentials() );
    //
    // // password is plain text
    // assertEquals( "brianf123", password );
  }

  @Test
  public void testWrongPassword()
      throws Exception
  {
    UsernamePasswordToken upToken = new UsernamePasswordToken("brianf", "JUNK");
    try {
      Assert.assertNull(realm.getAuthenticationInfo(upToken));
    }
    catch (AuthenticationException e) {
      // expected
    }
  }

  @Test
  public void testFailedAuthentication() {

    UsernamePasswordToken upToken = new UsernamePasswordToken("username", "password");
    try {
      realm.getAuthenticationInfo(upToken);
      Assert.fail("Expected AuthenticationException exception.");
    }
    catch (AuthenticationException e) {
      // expected
    }
  }

  protected boolean isPasswordsEncrypted() {
    return false;
  }

  @Test
  public void testSearch()
      throws LdapDAOException
  {
    Set<LdapUser> users = this.ldapManager.searchUsers("cstamas", null);
    assertEquals(1, users.size());
    LdapUser user = users.iterator().next();
    assertEquals("cstamas", user.getUsername());
    assertTrue(this.isPasswordsEncrypted() || ("cstamas123".equals(user.getPassword())));

    users = this.ldapManager.searchUsers("br", null);
    assertEquals(1, users.size());
    user = users.iterator().next();
    assertEquals("brianf", user.getUsername());
    // assertEquals( "Brian Fox", user.getRealName() );
    assertTrue(this.isPasswordsEncrypted() || ("brianf123".equals(user.getPassword())));

    users = this.ldapManager.searchUsers("j", null);
    assertEquals(1, users.size());
    user = users.iterator().next();
    assertEquals("jvanzyl", user.getUsername());
    // assertEquals( "Jason Van Zyl", user.getRealName() );
    assertTrue(this.isPasswordsEncrypted() || ("jvanzyl123".equals(user.getPassword())));

    users = this.ldapManager.searchUsers("INVALID", null);
    assertEquals(0, users.size());
  }

  @Test
  public void testGetAllGroups()
      throws LdapDAOException
  {
    SortedSet<String> groupIds = this.ldapManager.getAllGroups();

    assertTrue("GroupIds: " + groupIds, groupIds.contains("public"));
    assertTrue("GroupIds: " + groupIds, groupIds.contains("releases"));
    assertTrue("GroupIds: " + groupIds, groupIds.contains("snapshots"));
    assertEquals("GroupIds: " + groupIds, 3, groupIds.size());

  }

  @Test
  public void testGetGroupName()
      throws LdapDAOException, NoSuchLdapGroupException
  {
    assertEquals("public", this.ldapManager.getGroupName("public"));
    try {
      this.ldapManager.getGroupName("p");
      fail("Expected NoSuchLdapGroupException");
    }
    catch (NoSuchLdapGroupException e) {
      // expected
    }
  }

}
