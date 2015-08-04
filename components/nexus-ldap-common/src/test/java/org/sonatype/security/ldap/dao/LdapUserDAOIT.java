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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.ldap.InitialLdapContext;

import org.sonatype.security.ldap.LdapTestSupport;

import org.junit.Test;

public class LdapUserDAOIT
    extends LdapTestSupport
{

  @Test
  public void testSimple()
      throws Exception
  {

    Map<String, Object> env = new HashMap<String, Object>();
    // Create a new context pointing to the overseas partition
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, "ldap://localhost:" + this.getLdapServer().getPort() + "/o=sonatype");
    env.put(Context.SECURITY_PRINCIPAL, "uid=admin,ou=system");
    env.put(Context.SECURITY_CREDENTIALS, "secret");
    env.put(Context.SECURITY_AUTHENTICATION, "simple");

    // if want to use explicitly ApacheDS and not the Sun supplied ones
    // env.put( Context.PROVIDER_URL, "o=sonatype" );
    // env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.jndi.ServerContextFactory" );

    InitialLdapContext initialContext = new InitialLdapContext(new Hashtable<String, Object>(env), null);

    LdapAuthConfiguration configuration = new LdapAuthConfiguration();
    configuration.setUserBaseDn("ou=people");
    configuration.setGroupBaseDn("ou=groups");
    configuration.setGroupObjectClass("groupOfUniqueNames");
    configuration.setGroupMemberAttribute("uniqueMember");
    configuration.setUserRealNameAttribute("cn");

    LdapUserDAO lum = (LdapUserDAO) lookup(LdapUserDAO.class.getName());

    LdapUser user = lum.getUser("cstamas", initialContext, configuration);
    assertEquals("cstamas", user.getUsername());
    // assertEquals( "Tamas Cservenak", user.getRealName() );
    assertEquals("cstamas123", user.getPassword());

    user = lum.getUser("Fox, Brian", initialContext, configuration);
    assertEquals("Fox, Brian", user.getUsername());
    // assertEquals( "Brian Fox", user.getRealName() );
    assertEquals("brianf123", user.getPassword());

    user = lum.getUser("jvanzyl", initialContext, configuration);
    assertEquals("jvanzyl", user.getUsername());
    // assertEquals( "Jason Van Zyl", user.getRealName() );
    assertEquals("jvanzyl123", user.getPassword());

    try {
      user = lum.getUser("intruder", initialContext, configuration);
      fail();
    }
    catch (NoSuchLdapUserException e) {
      // good
    }

    configuration.setLdapFilter("description=nexus");
    // must succeed because cstamas has the attribute description set to nexus
    user = lum.getUser("cstamas", initialContext, configuration);
    try {
      // must fail because of the ldapFilter that jvanzyl user don't have
      user = lum.getUser("jvanzyl", initialContext, configuration);
      fail();
    }
    catch (NoSuchLdapUserException e) {
      // good
    }
  }

}
