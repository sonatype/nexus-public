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
package org.sonatype.security.web.guice;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;

import org.sonatype.security.SecuritySystem;
import org.sonatype.security.web.ProtectedPathManager;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import net.sf.ehcache.CacheManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;
import org.apache.shiro.web.filter.authz.HttpMethodPermissionFilter;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.FilterChainResolver;
import org.apache.shiro.web.filter.mgt.NamedFilterList;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.ParameterKeys;
import org.eclipse.sisu.wire.WireModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Verifies functionality of SecurityWebModule.
 *
 * @since 2.6.1
 */
public class SecurityWebModuleTest
{
  private Injector injector;

  @Before
  public void setUp() {
    injector = Guice.createInjector(getWireModule());
  }

  @Test
  public void testInjectionIsSetupCorrectly() {
    SecuritySystem securitySystem = injector.getInstance(SecuritySystem.class);

    SecurityManager securityManager = injector.getInstance(SecurityManager.class);

    RealmSecurityManager realmSecurityManager =
        (RealmSecurityManager) injector.getInstance(WebSecurityManager.class);

    assertThat(securitySystem.getSecurityManager(), sameInstance(securityManager));
    assertThat(securitySystem.getSecurityManager(), sameInstance(realmSecurityManager));

    assertThat(securityManager, instanceOf(DefaultWebSecurityManager.class));
    DefaultSecurityManager defaultSecurityManager = (DefaultSecurityManager) securityManager;

    assertThat(defaultSecurityManager.getSessionManager(), instanceOf(DefaultWebSessionManager.class));
    DefaultSessionManager sessionManager = (DefaultSessionManager) defaultSecurityManager.getSessionManager();
    assertThat(sessionManager.getSessionDAO(), instanceOf(EnterpriseCacheSessionDAO.class));

    SecurityWebFilter shiroFilter = injector.getInstance(SecurityWebFilter.class);
    assertThat(shiroFilter.getFilterChainResolver(), instanceOf(PathMatchingFilterChainResolver.class));

    PathMatchingFilterChainResolver filterChainResolver =
        (PathMatchingFilterChainResolver) shiroFilter.getFilterChainResolver();
    assertThat(filterChainResolver.getFilterChainManager(), instanceOf(DefaultFilterChainManager.class));
    assertThat(filterChainResolver, sameInstance(injector.getInstance(FilterChainResolver.class)));

    // now add a protected path
    ProtectedPathManager protectedPathManager = injector.getInstance(ProtectedPathManager.class);
    protectedPathManager.addProtectedResource("/service/**", "foobar,perms[sample:priv-name]");

    NamedFilterList filterList = filterChainResolver.getFilterChainManager().getChain("/service/**");
    assertThat(filterList.get(0), instanceOf(SimpleAccessControlFilter.class));
    assertThat(filterList.get(1), instanceOf(HttpMethodPermissionFilter.class));

    // test that injection of filters works
    assertThat(((SimpleAccessControlFilter) filterList.get(0)).getSecurityXMLFilePath(),
        equalTo("target/foo/security.xml"));
  }

  @After
  public void stopCache() {
    if (injector != null) {
      injector.getInstance(CacheManager.class).shutdown();
    }
  }

  private Module getWireModule() {
    return new WireModule(getShiroModule(), getSpaceModule(), getPropertiesModule());
  }

  private Module getShiroModule() {
    return new SecurityWebModule(createMock(ServletContext.class), true)
    {
      @Override
      protected void configureShiroWeb() {
        super.configureShiroWeb();

        SimpleAccessControlFilter foobar = new SimpleAccessControlFilter();
        foobar.setApplicationName("Foobar Application");

        bindNamedFilter("foobar", foobar);
        bindNamedFilter("perms", new HttpMethodPermissionFilter());
      }
    };
  }

  private Module getSpaceModule() {
    return new SpaceModule(new URLClassSpace(getClass().getClassLoader()), BeanScanning.INDEX);
  }

  protected AbstractModule getPropertiesModule() {
    return new AbstractModule()
    {
      @Override
      protected void configure() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("security-xml-file", "target/foo/security.xml");
        properties.put("application-conf", "target/plexus-home/conf");
        binder().bind(ParameterKeys.PROPERTIES).toInstance(properties);
      }
    };
  }

  static class SimpleAccessControlFilter
      extends BasicHttpAuthenticationFilter
  {
    @Inject
    @Named("${security-xml-file}")
    private String securityXMLFilePath;

    public String getSecurityXMLFilePath() {
      return securityXMLFilePath;
    }
  }
}
