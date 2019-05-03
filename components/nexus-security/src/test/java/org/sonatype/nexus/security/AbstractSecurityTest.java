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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.cache.CacheManager;
import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.goodies.testsupport.TestUtil;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.security.config.PreconfiguredSecurityConfigurationSource;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfigurationSource;
import org.sonatype.nexus.security.realm.RealmConfiguration;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.testcommon.event.SimpleEventManager;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.apache.shiro.util.ThreadContext;
import org.eclipse.sisu.inject.BeanLocator;
import org.eclipse.sisu.space.BeanScanning;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;
import org.junit.After;
import org.junit.Before;

import static org.mockito.Mockito.mock;

public abstract class AbstractSecurityTest
  extends TestSupport
{
  protected final TestUtil util = new TestUtil(this);

  @Inject
  private BeanLocator beanLocator;

  @Before
  public final void doSetUp() throws Exception {
    List<Module> modules = new ArrayList<>();

    customizeModules(modules);
    modules.add(new SpaceModule(new URLClassSpace(getClass().getClassLoader()), BeanScanning.INDEX));
    Guice.createInjector(new WireModule(modules));

    setUp();
  }

  @After
  public final void doTearDown() throws Exception {
    tearDown();
  }

  protected void customizeModules(List<Module> modules) {
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        install(new WebSecurityModule(mock(ServletContext.class)));

        bind(SecurityConfigurationSource.class).annotatedWith(Names.named("default")).toInstance(
            new PreconfiguredSecurityConfigurationSource(initialSecurityConfiguration()));

        RealmConfiguration realmConfiguration = new RealmConfiguration();
        realmConfiguration.setRealmNames(Arrays.asList("MockRealmA", "MockRealmB"));
        bind(RealmConfiguration.class).annotatedWith(Names.named("initial")).toInstance(realmConfiguration);

        bind(ApplicationDirectories.class).toInstance(mock(ApplicationDirectories.class));
        bind(NodeAccess.class).toInstance(mock(NodeAccess.class));
        bind(AnonymousManager.class).toInstance(mock(AnonymousManager.class));
        bind(EventManager.class).toInstance(getEventManager());

        requestInjection(AbstractSecurityTest.this);
      }
    });
  }

  protected EventManager getEventManager() {
    return new SimpleEventManager();
  }

  protected <T> T lookup(Class<T> role) {
    return beanLocator.locate(Key.get(role)).iterator().next().getValue();
  }

  protected <T> T lookup(Class<T> role, String hint) {
    return beanLocator.locate(Key.get(role, Names.named(hint))).iterator().next().getValue();
  }

  protected void setUp() throws Exception {
    getSecuritySystem().start();
  }

  protected void tearDown() throws Exception {
    try {
      getSecuritySystem().stop();
    }
    catch (Exception e) {
      util.getLog().warn("Failed to stop security-system", e);
    }

    try {
      lookup(CacheManager.class).close();
    }
    catch (Exception e) {
      util.getLog().warn("Failed to shutdown cache-manager", e);
    }

    ThreadContext.remove();
  }

  protected SecuritySystem getSecuritySystem() {
    return lookup(SecuritySystem.class);
  }

  protected UserManager getUserManager() {
    return lookup(UserManager.class);
  }

  protected SecurityConfiguration initialSecurityConfiguration() {
    return BaseSecurityConfig.get();
  }

  protected final SecurityConfiguration getSecurityConfiguration() {
    return lookup(SecurityConfigurationSource.class, "default").getConfiguration();
  }
}
