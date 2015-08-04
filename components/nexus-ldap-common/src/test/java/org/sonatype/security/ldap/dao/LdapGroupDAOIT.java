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
import java.util.Set;

import javax.naming.Context;
import javax.naming.ldap.InitialLdapContext;

import org.sonatype.security.ldap.LdapTestSupport;

import org.junit.Test;

public class LdapGroupDAOIT
    extends LdapTestSupport
{

  @Test
  public void testSimple()
      throws Exception
  {
    doTestWithGroupMemberFormat("cn=${username},ou=people,o=sonatype");
  }

  @Test
  public void testUsingDNInGroupMemberFormat()
      throws Exception
  {
    doTestWithGroupMemberFormat("${dn}");
  }

  protected void doTestWithGroupMemberFormat(String groupMemberFormat)
      throws Exception
  {
    Map<String, Object> env = new HashMap<String, Object>();
    // Create a new context pointing to the overseas partition
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, "ldap://localhost:" + getLdapServer().getPort() + "/o=sonatype");
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
    configuration.setGroupMemberFormat(groupMemberFormat);
    configuration.setLdapGroupsAsRoles(true);
    configuration.setUserMemberOfAttribute("");

    LdapGroupDAO lgm = (LdapGroupDAO) lookup(LdapGroupDAO.class.getName());

    Set<String> groups = lgm.getGroupMembership("cstamas", initialContext, configuration);
    assertTrue(groups.contains("public"));
    assertTrue(groups.contains("snapshots"));

    groups = lgm.getGroupMembership("Fox, Brian", initialContext, configuration);
    assertTrue(groups.contains("public"));
    assertTrue(groups.contains("releases"));

    groups = lgm.getGroupMembership("jvanzyl", initialContext, configuration);
    assertTrue(groups.contains("public"));
    assertTrue(groups.contains("releases"));
    assertTrue(groups.contains("snapshots"));
  }
}
