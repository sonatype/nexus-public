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
package org.sonatype.nexus.bootstrap.security;

import java.util.Collection;

import jakarta.inject.Provider;

import org.sonatype.nexus.common.time.Time;
import org.sonatype.nexus.cache.CacheHelper;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.authc.FirstSuccessfulModularRealmAuthenticator;
import org.sonatype.nexus.security.authz.ExceptionCatchingModularRealmAuthorizer;

import org.apache.shiro.authc.Authenticator;
import org.apache.shiro.authz.Authorizer;
import org.apache.shiro.authz.ModularRealmAuthorizer;
import org.apache.shiro.authz.permission.RolePermissionResolver;
import org.apache.shiro.event.EventBus;
import org.apache.shiro.event.support.DefaultEventBus;
import org.apache.shiro.mgt.SessionStorageEvaluator;
import org.apache.shiro.mgt.SessionsSecurityManager;
import org.apache.shiro.mgt.SubjectDAO;
import org.apache.shiro.nexus.NexusSessionDAO;
import org.apache.shiro.nexus.NexusSessionFactory;
import org.apache.shiro.nexus.NexusSessionStorageEvaluator;
import org.apache.shiro.nexus.NexusSubjectDAO;
import org.apache.shiro.nexus.NexusWebSecurityManager;
import org.apache.shiro.nexus.NexusWebSessionManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SessionFactory;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.web.config.ShiroFilterConfiguration;
import org.apache.shiro.web.filter.mgt.FilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.DefaultWebSubjectFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @since 3.21.2
 */
@Configuration
@ComponentScan("org.apache.shiro.nexus")
public class WebSecurityConfiguration
{

  @Bean
  public SessionFactory sessionFactory() {
    return new NexusSessionFactory();
  }

  @Bean
  public SessionStorageEvaluator sessionStorageEvaluator() {
    return new NexusSessionStorageEvaluator();
  }

  @Bean
  public SubjectDAO subjectDAO() {
    NexusSubjectDAO subjectDAO = new NexusSubjectDAO();
    subjectDAO.setSessionStorageEvaluator(sessionStorageEvaluator());
    return subjectDAO;
  }

  @Bean
  public SessionDAO sessionDAO() {
    return new NexusSessionDAO();
  }

  @Bean
  public Authenticator authenticator() {
    return new FirstSuccessfulModularRealmAuthenticator();
  }

  @Bean
  public Authorizer authorizer(
      final Collection<Realm> realms,
      final Provider<RolePermissionResolver> rolePermissionResolverProvider)
  {
    return new ExceptionCatchingModularRealmAuthorizer(realms, rolePermissionResolverProvider);
  }

  @Bean
  public ShiroFilterConfiguration shiroFilterConfiguration() {
    return new ShiroFilterConfiguration();
  }

  @Bean
  public FilterChainResolver filterChainResolver(final FilterChainManager manager) {
    PathMatchingFilterChainResolver resolver = new PathMatchingFilterChainResolver();
    resolver.setFilterChainManager(manager);
    return resolver;
  }

  @Bean
  public EventBus eventBus() {
    return new DefaultEventBus();
  }

  @Bean
  public SessionsSecurityManager securityManager(
      final Provider<EventManager> eventManager,
      final Provider<CacheHelper> cacheHelper,
      final NexusWebSessionManager webSessionManager,
      @Value("${nexus.shiro.cache.defaultTimeToLive:2m}") final Provider<Time> defaultTimeToLive)
  {
    webSessionManager.setSessionDAO(sessionDAO());
    webSessionManager.setSessionFactory(sessionFactory());
    webSessionManager.setDeleteInvalidSessions(true);

    webSessionManager.setSessionIdCookieEnabled(true);
    webSessionManager.setSessionIdUrlRewritingEnabled(false);
    // webSessionManager.setSessionIdCookie(sessionCookieTemplate());

    NexusWebSecurityManager securityManager = new NexusWebSecurityManager(eventManager, cacheHelper, defaultTimeToLive);
    securityManager.setSubjectDAO(subjectDAO());
    securityManager.setSubjectFactory(new DefaultWebSubjectFactory());

    securityManager.setAuthenticator(authenticator());
    securityManager.setAuthorizer(authorizer());
    securityManager.setSessionManager(webSessionManager);
    securityManager.setEventBus(new DefaultEventBus());

    return securityManager;
  }

  protected Authorizer authorizer() {
    ModularRealmAuthorizer authorizer = new ModularRealmAuthorizer();
    //
    // if (permissionResolver != null) {
    // authorizer.setPermissionResolver(permissionResolver);
    // }
    //
    // if (rolePermissionResolver != null) {
    // authorizer.setRolePermissionResolver(rolePermissionResolver);
    // }

    return authorizer;
  }

}
