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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.sonatype.security.ldap.realms.DefaultLdapContextFactory.NEXUS_LDAP_ENV_PREFIX;

public class DefaultLdapContextFactoryTest
{

  private DefaultLdapContextFactory underTest;

  @Before
  public void setUp() {
    underTest = new DefaultLdapContextFactory();
  }

  @Test
  public void testValidLdapUrl() {
    underTest.setUrl("ldap://localhost:439");
    assertThat(underTest.getSetupEnvironment(null, null, false).get(Context.PROVIDER_URL),
        is("ldap://localhost:439"));
  }

  @Test
  public void testValidLdapsUrl() {
    underTest.setUrl("ldaps://localhost:439");
    assertThat(underTest.getSetupEnvironment(null, null, false).get(Context.PROVIDER_URL),
        is("ldaps://localhost:439"));
  }

  @Test(expected = NullPointerException.class)
  public void testNoLdapUrl() {
    underTest.getSetupEnvironment(null, null, false);
  }

  @Test
  public void testUsePooling()
      throws NamingException
  {
    underTest.setUrl("ldap://localhost:439");
    underTest.setUsePooling(true);
    assertThat(underTest.getSetupEnvironment("user", "pass", true)
        .get(DefaultLdapContextFactory.SUN_CONNECTION_POOLING_ENV_PROPERTY), is("true"));
    // only pool for system context
    assertThat(underTest.getSetupEnvironment("user", "pass", false)
        .get(DefaultLdapContextFactory.SUN_CONNECTION_POOLING_ENV_PROPERTY), nullValue());
    // only pool for auth necessary
    assertThat(underTest.getSetupEnvironment(null, null, true)
        .get(DefaultLdapContextFactory.SUN_CONNECTION_POOLING_ENV_PROPERTY), nullValue());
  }

  @Test
  public void testUserPass() {
    underTest.setUrl("ldap://localhost:439");
    {
      final Hashtable<String, String> env = underTest.getSetupEnvironment("user", "pass", true);
      assertThat(env.get(Context.SECURITY_PRINCIPAL), is("user"));
      assertThat(env.get(Context.SECURITY_CREDENTIALS), is("pass"));
    }
    {
      final Hashtable<String, String> env = underTest.getSetupEnvironment("user", "pass", false);
      assertThat(env.get(Context.SECURITY_PRINCIPAL), is("user"));
      assertThat(env.get(Context.SECURITY_CREDENTIALS), is("pass"));
    }
    {
      final Hashtable<String, String> env = underTest.getSetupEnvironment(null, null, false);
      assertThat(env.get(Context.SECURITY_PRINCIPAL), nullValue());
      assertThat(env.get(Context.SECURITY_CREDENTIALS), nullValue());
    }
  }

  @Test
  public void testAdditionalEnv() {
    underTest.setUrl("ldap://localhost:439");
    final HashMap<String, String> map = Maps.newHashMap();
    map.put("test", "value");
    // this should not be used
    map.put(Context.SECURITY_PRINCIPAL, "somethingelse");

    underTest.setAdditionalEnvironment(map);

    final Hashtable<String, String> env = underTest.getSetupEnvironment("user", "pass", true);
    assertThat(env.get("test"), is("value"));
    assertThat(env.get(Context.SECURITY_PRINCIPAL), is("user"));
    assertThat(env.get(Context.SECURITY_CREDENTIALS), is("pass"));
  }

  @Test
  public void testAuthenticationTypeNull() {
    underTest.setUrl("ldap://localhost:439");

    {
      final Hashtable<String, String> env = underTest.getSetupEnvironment("user", "pass", true);
      assertThat(env.get(Context.SECURITY_AUTHENTICATION), is("simple"));
    }
    {
      final Hashtable<String, String> env = underTest.getSetupEnvironment("user", "pass", false);
      assertThat(env.get(Context.SECURITY_AUTHENTICATION), is("simple"));
    }

    underTest.setAuthentication("none");
    {
      final Hashtable<String, String> env = underTest.getSetupEnvironment("user", "pass", true);
      assertThat(env.get(Context.SECURITY_AUTHENTICATION), is("none"));
    }
    {
      final Hashtable<String, String> env = underTest.getSetupEnvironment("user", "pass", false);
      assertThat(env.get(Context.SECURITY_AUTHENTICATION), is("simple"));
    }

    underTest.setAuthentication("somethingelse");
    {
      final Hashtable<String, String> env = underTest.getSetupEnvironment("user", "pass", true);
      assertThat(env.get(Context.SECURITY_AUTHENTICATION), is("somethingelse"));
    }
    {
      final Hashtable<String, String> env = underTest.getSetupEnvironment("user", "pass", false);
      assertThat(env.get(Context.SECURITY_AUTHENTICATION), is("somethingelse"));
    }
  }

  @Test
  public void testEnvVarsFromAppContext() {
    Properties backup = System.getProperties();

    try {
      System.setProperty(NEXUS_LDAP_ENV_PREFIX + "foo", "bar");
      System.setProperty(NEXUS_LDAP_ENV_PREFIX + "bar", "foo");
      System.setProperty("foo", "bar2");

      DefaultLdapContextFactory underTest = new DefaultLdapContextFactory();
      underTest.setUrl("ldap://localhost:439");
      Hashtable<String, String> env = underTest.getSetupEnvironment("user", "pass", true);

      assertThat(env.get("foo"), is("bar"));
      assertThat(env.get("bar"), is("foo"));
    }
    finally {
      System.setProperties(backup);
    }
  }

}
