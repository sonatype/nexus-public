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

import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.apache.shiro.realm.ldap.AbstractLdapRealm;
import org.apache.shiro.realm.ldap.LdapContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Default implementation of {@link LdapContextFactory} that can be configured or extended to
 * customize the way {@link javax.naming.ldap.LdapContext} objects are retrieved.</p>
 *
 * <p>This implementation of {@link LdapContextFactory} is used by the {@link AbstractLdapRealm} if a
 * factory is not explictly configured.</p>
 *
 * <p>Connection pooling is enabled by default on this factory, but can be disabled using the
 * {@link #usePooling} property.</p>
 *
 * NOTE: copied from jsecurity, fixes problem with context when system auth is anon, and user auth should be simple
 *
 * @author Jeremy Haile
 * @since 0.2
 */
public class DefaultLdapContextFactory
    implements LdapContextFactory
{

    /*--------------------------------------------
    |             C O N S T A N T S             |
    ============================================*/

  /**
   * The Sun LDAP environment property used to enable connection pooling for given context. This is used in the
   * default implementation to enable LDAP connection pooling. Still, connection pooling parameters are set on JVM
   * global level using System Properties.
   */
  protected static final String SUN_CONNECTION_POOLING_ENV_PROPERTY = "com.sun.jndi.ldap.connect.pool";

   /*--------------------------------------------
    |    I N S T A N C E   V A R I A B L E S    |
    ============================================*/

  private static final Logger log = LoggerFactory.getLogger(DefaultLdapContextFactory.class);

  public static final String NEXUS_LDAP_ENV_PREFIX = "nexus.ldap.env.";

  protected String authentication = "simple";

  protected String principalSuffix = null;

  protected String searchBase = null;

  protected String contextFactoryClassName = null;

  protected String url = null;

  protected String referral = null;

  protected String systemUsername = null;

  protected String systemPassword = null;

  private boolean usePooling = true;

  private Map<String, String> additionalEnvironment;

  public DefaultLdapContextFactory() {
    additionalEnvironment = Maps.newHashMap();
    additionalEnvironment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    additionalEnvironment.put(Context.REFERRAL, "follow");
    for (String propertyName : System.getProperties().stringPropertyNames()) {
      if (propertyName.startsWith(NEXUS_LDAP_ENV_PREFIX) &&
          propertyName.length() > NEXUS_LDAP_ENV_PREFIX.length()) {
        String key = propertyName.substring(NEXUS_LDAP_ENV_PREFIX.length());
        String value = System.getProperty(propertyName);
        if (value != null) {
          additionalEnvironment.put(key, value);
        }
      }
    }
  }

    /*--------------------------------------------
    |         C O N S T R U C T O R S           |
    ============================================*/

    /*--------------------------------------------
    |  A C C E S S O R S / M O D I F I E R S    |
    ============================================*/

  /**
   * Sets the type of LDAP authentication to perform when connecting to the LDAP server.  Defaults to "simple"
   *
   * @param authentication the type of LDAP authentication to perform.
   */
  public void setAuthentication(String authentication) {
    this.authentication = authentication;
  }

  /**
   * A suffix appended to the username. This is typically for
   * domain names.  (e.g. "@MyDomain.local")
   *
   * @param principalSuffix the suffix.
   */
  public void setPrincipalSuffix(String principalSuffix) {
    this.principalSuffix = principalSuffix;
  }

  /**
   * The search base for the search to perform in the LDAP server.
   * (e.g. OU=OrganizationName,DC=MyDomain,DC=local )
   *
   * @param searchBase the search base.
   */
  public void setSearchBase(String searchBase) {
    this.searchBase = searchBase;
  }

  /**
   * The context factory to use. This defaults to the SUN LDAP JNDI implementation
   * but can be overridden to use custom LDAP factories.
   *
   * @param contextFactoryClassName the context factory that should be used.
   */
  public void setContextFactoryClassName(String contextFactoryClassName) {
    this.contextFactoryClassName = contextFactoryClassName;
  }

  /**
   * The LDAP url to connect to. (e.g. ldap://<ldapDirectoryHostname>:<port>)
   *
   * @param url the LDAP url.
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Sets the LDAP referral property.  Defaults to "follow"
   *
   * @param referral the referral property.
   */
  public void setReferral(String referral) {
    this.referral = referral;
  }

  /**
   * The system username that will be used when connecting to the LDAP server to retrieve authorization
   * information about a user.  This must be specified for LDAP authorization to work, but is not required for
   * only authentication.
   *
   * @param systemUsername the username to use when logging into the LDAP server for authorization.
   */
  public void setSystemUsername(String systemUsername) {
    this.systemUsername = systemUsername;
  }


  /**
   * The system password that will be used when connecting to the LDAP server to retrieve authorization
   * information about a user.  This must be specified for LDAP authorization to work, but is not required for
   * only authentication.
   *
   * @param systemPassword the password to use when logging into the LDAP server for authorization.
   */
  public void setSystemPassword(String systemPassword) {
    this.systemPassword = systemPassword;
  }

  /**
   * Determines whether or not LdapContext pooling is enabled for connections made using the system user account. In
   * the default implementation, it sets <tt>com.sun.jndi.ldap.connect.pool</tt> property to {@code true}. Still,
   * using
   * <b>system properties</b> like <tt>com.sun.jndi.ldap.connect.pool.protocol</tt> and others you control at
   * JVM-global level the pool properties. If you use a LDAP Context Factory that is not Sun's default
   * implementation,
   * you will need to override the default behavior to use this setting in whatever way your underlying LDAP
   * ContextFactory supports. By default, pooling is enabled.
   *
   * @param usePooling true to enable pooling, or false to disable it.
   */
  public void setUsePooling(boolean usePooling) {
    this.usePooling = usePooling;
  }

  /**
   * These entries are added to the environment map before initializing the LDAP context.
   *
   * @param additionalEnvironment additional environment entries to be configured on the LDAP context.
   */
  public void setAdditionalEnvironment(Map<String, String> additionalEnvironment) {
    this.additionalEnvironment = Maps.newHashMap();
    this.additionalEnvironment.putAll(additionalEnvironment);
  }

  /**
   * These entries are added to the environment map before initializing the LDAP context.
   *
   * @param additionalEnvironment additional environment entries to be configured on the LDAP context.
   */
  public void addAdditionalEnvironment(Map<String, String> additionalEnvironment) {
    this.additionalEnvironment.putAll(additionalEnvironment);
  }

    /*--------------------------------------------
    |               M E T H O D S               |
    ============================================*/

  public LdapContext getSystemLdapContext() throws NamingException {
    return getLdapContext(systemUsername, systemPassword, true);
  }

  public LdapContext getLdapContext(String username, String password) throws NamingException {
    return getLdapContext(username, password, false);
  }


  public LdapContext getLdapContext(Object principal, Object credentials)
      throws NamingException
  {
    return getLdapContext(principal.toString(), credentials.toString(), false);
  }

  public LdapContext getLdapContext(String username, String password, boolean systemContext) throws NamingException {
    return new InitialLdapContext(getSetupEnvironment(username, password, systemContext), null);
  }

  @VisibleForTesting
  Hashtable<String, String> getSetupEnvironment(String username, final String password,
                                                final boolean systemContext)
  {
    checkNotNull(url, "No ldap URL specified (ldap://<hostname>:<port>)");

    if (username != null && principalSuffix != null) {
      username += principalSuffix;
    }

    Hashtable<String, String> env = new Hashtable<String, String>();

    if (additionalEnvironment != null) {
      env.putAll(additionalEnvironment);
    }

    // if the Authentication scheme is none, and this is not the system ctx we need to set the scheme to 'simple'
    if ("none".equals(authentication) && !systemContext) {
      env.put(Context.SECURITY_AUTHENTICATION, "simple");
    }
    else {
      env.put(Context.SECURITY_AUTHENTICATION, authentication);
    }

    if (username != null) {
      env.put(Context.SECURITY_PRINCIPAL, username);
    }
    if (password != null) {
      env.put(Context.SECURITY_CREDENTIALS, password);
    }
    if (contextFactoryClassName != null) {
      env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactoryClassName);
    }
    env.put(Context.PROVIDER_URL, url);
    if (referral != null) {
      env.put(Context.REFERRAL, referral);
    }

    if (usePooling && username != null && systemContext) {
      // Enable connection pooling
      env.put(SUN_CONNECTION_POOLING_ENV_PROPERTY, "true");
    }

    if (log.isDebugEnabled()) {
      final Map<String, String> logEnv = Maps.newHashMap(env);
      if (logEnv.containsKey(Context.SECURITY_CREDENTIALS)) {
        logEnv.put(Context.SECURITY_CREDENTIALS, "***");
      }
      log.debug(
          "Initializing LDAP context using URL [{}] and username [{}] with pooling [{}] and environment {}",
          url, systemUsername, usePooling ? "enabled" : "disabled", logEnv
      );
    }
    return env;
  }
}
