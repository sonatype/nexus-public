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
package org.sonatype.security.guice;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.security.SecuritySystem;
import org.sonatype.sisu.ehcache.CacheManagerComponent;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import net.sf.ehcache.CacheManager;
import org.apache.shiro.cache.ehcache.EhCacheManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.nexus5727.FixedDefaultSessionManager;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.ParameterKeys;
import org.eclipse.sisu.wire.WireModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Verifies functionality of SecurityModule.
 */
public class SecurityModuleTest
    extends TestSupport
{
  private Injector injector;

  @Before
  public void setUp() {
    injector = Guice.createInjector(getWireModule());
  }

  @Test
  public void testInjectionIsSetupCorrectly() {
    SecuritySystem securitySystem = injector.getInstance(SecuritySystem.class);
    // See DefaultSecuritySystem, that applies cache
    // TODO: this should be done with Guice binding?
    securitySystem.start();

    SecurityManager securityManager = injector.getInstance(SecurityManager.class);

    RealmSecurityManager realmSecurityManager = injector.getInstance(RealmSecurityManager.class);

    assertThat(securitySystem.getSecurityManager(), sameInstance(securityManager));
    assertThat(securitySystem.getSecurityManager(), sameInstance(realmSecurityManager));

    assertThat(securityManager, instanceOf(DefaultSecurityManager.class));
    DefaultSecurityManager defaultSecurityManager = (DefaultSecurityManager) securityManager;

    assertThat(defaultSecurityManager.getSessionManager(), instanceOf(FixedDefaultSessionManager.class));
    FixedDefaultSessionManager sessionManager =
        (FixedDefaultSessionManager) defaultSecurityManager.getSessionManager();
    assertThat(sessionManager.getSessionDAO(), instanceOf(EnterpriseCacheSessionDAO.class));
    assertThat(
        ((EhCacheManager) ((EnterpriseCacheSessionDAO) sessionManager.getSessionDAO()).getCacheManager())
            .getCacheManager(),
        sameInstance(injector.getInstance(CacheManagerComponent.class).getCacheManager()));
  }

  @After
  public void stopCache() {
    if (injector != null) {
      injector.getInstance(CacheManager.class).shutdown();
    }
  }

  private Module getWireModule() {
    return new WireModule(new SecurityModule(), getSpaceModule(), getPropertiesModule());
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
        properties.put("security-xml-file", util.resolvePath("target/foo/security.xml"));
        properties.put("application-conf", util.resolvePath("target/plexus-home/conf"));
        binder().bind(ParameterKeys.PROPERTIES).toInstance(properties);
      }
    };
  }
}
