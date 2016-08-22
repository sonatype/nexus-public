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
package org.sonatype.nexus.security;

import java.lang.reflect.Constructor;

import javax.inject.Singleton;
import javax.servlet.ServletContext;

import org.sonatype.nexus.security.authc.FirstSuccessfulModularRealmAuthenticator;
import org.sonatype.nexus.security.authz.ExceptionCatchingModularRealmAuthorizer;

import com.google.common.base.Throwables;
import com.google.inject.binder.AnnotatedBindingBuilder;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authz.Authorizer;
import org.apache.shiro.config.ConfigurationException;
import org.apache.shiro.guice.web.ShiroWebModule;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.mgt.SessionStorageEvaluator;
import org.apache.shiro.mgt.SubjectDAO;
import org.apache.shiro.nexus.NexusSessionDAO;
import org.apache.shiro.nexus.NexusSessionFactory;
import org.apache.shiro.nexus.NexusSessionStorageEvaluator;
import org.apache.shiro.nexus.NexusSubjectDAO;
import org.apache.shiro.nexus.NexusWebSecurityManager;
import org.apache.shiro.nexus.NexusWebSessionManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SessionFactory;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.WebSecurityManager;

/**
 * Shiro security configuration Guice module for the runtime server.
 *
 * @since 2.6.1
 */
public class WebSecurityModule
    extends ShiroWebModule
{
  public WebSecurityModule(final ServletContext servletContext) {
    super(servletContext);
  }

  @Override
  protected void configureShiroWeb() {
    bindRealm().to(EmptyRealm.class); // not used in practice, just here to keep Shiro module happy

    bindSingleton(SessionFactory.class, NexusSessionFactory.class);
    bindSingleton(SessionStorageEvaluator.class, NexusSessionStorageEvaluator.class);
    bindSingleton(SubjectDAO.class, NexusSubjectDAO.class);

    // configure our preferred security components
    bindSingleton(SessionDAO.class, NexusSessionDAO.class);
    bindSingleton(Authenticator.class, FirstSuccessfulModularRealmAuthenticator.class);
    bindSingleton(Authorizer.class, ExceptionCatchingModularRealmAuthorizer.class);
    bindSingleton(FilterChainManager.class, DynamicFilterChainManager.class);

    // path matching resolver has several constructors so we need to point Guice to the appropriate one
    bind(FilterChainResolver.class).toConstructor(ctor(PathMatchingFilterChainResolver.class)).asEagerSingleton();

    // bindings used by external modules
    expose(FilterChainResolver.class);
    expose(FilterChainManager.class);
  }

  // bind a given API to an implementation and make that implementation a singleton
  private <T> void bindSingleton(final Class<T> api, final Class<? extends T> impl) {
    bind(impl).in(Singleton.class);
    bind(api).to(impl);
  }

  @Override
  protected void bindWebSecurityManager(final AnnotatedBindingBuilder<? super WebSecurityManager> bind) {
    bind(NexusWebSecurityManager.class).asEagerSingleton();

    // bind RealmSecurityManager and WebSecurityManager to _same_ component
    bind(RealmSecurityManager.class).to(NexusWebSecurityManager.class);
    bind.to(NexusWebSecurityManager.class);

    // bindings used by external modules
    expose(RealmSecurityManager.class);
    expose(WebSecurityManager.class);
  }

  @Override
  protected void bindSessionManager(final AnnotatedBindingBuilder<SessionManager> bind) {
    // use native web session management instead of delegating to servlet container
    // workaround for NEXUS-5727, see NexusDefaultWebSessionManager javadoc for clues
    bind.to(NexusWebSessionManager.class).asEagerSingleton();
    // this is a PrivateModule, so explicitly binding the NexusDefaultSessionManager class
    bind(NexusWebSessionManager.class);
  }

  /**
   * Empty {@link Realm} - only used to satisfy Shiro's need for an initial realm binding.
   */
  @Singleton
  private static final class EmptyRealm
      implements Realm
  {
    @Override
    public String getName() {
      return getClass().getName();
    }

    @Override
    public boolean supports(final AuthenticationToken token) {
      return false;
    }

    @Override
    public AuthenticationInfo getAuthenticationInfo(final AuthenticationToken token) {
      return null;
    }
  }

  /**
   * @return Public constructor with given parameterTypes; wraps checked exceptions
   */
  private static <T> Constructor<T> ctor(final Class<T> clazz, final Class<?>... parameterTypes) {
    try {
      return clazz.getConstructor(parameterTypes);
    }
    catch (Exception e) {
      Throwables.propagateIfPossible(e);
      throw new ConfigurationException(e);
    }
  }
}
