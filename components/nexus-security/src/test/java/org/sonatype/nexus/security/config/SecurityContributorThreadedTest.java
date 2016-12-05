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
package org.sonatype.nexus.security.config;

import java.util.List;

import javax.inject.Inject;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.AbstractSecurityTest;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;
import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;
import org.junit.Assert;
import org.junit.Test;

public class SecurityContributorThreadedTest
    extends AbstractSecurityTest
{
  private SecurityConfigurationManager manager;

  private int expectedPrivilegeCount = 0;

  @Inject
  private List<SecurityContributor> testContributors;

  @Inject
  private List<MutableTestSecurityContributor> mutableTestContributors;

  @Inject
  private EventManager eventManager;

  @Override
  protected MemorySecurityConfiguration initialSecurityConfiguration() {
    return InitialSecurityConfiguration.getConfiguration();
  }

  @Override
  protected void customizeModules(List<Module> modules) {
    super.customizeModules(modules);
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        bindStaticContributor("static-default", new TestSecurityContributor2());
        bindDynamicContributor("dynamic-default", new MutableTestSecurityContributor());

        int staticResourceCount = 100;
        for (int ii = 0; ii < staticResourceCount - 1; ii++) { // 99 more
          bindStaticContributor("static-" + ii, new TestSecurityContributor3());
        }

        int dynamicResourceCount = 100;
        for (int ii = 0; ii < dynamicResourceCount - 1; ii++) { // 99 more
          bindDynamicContributor("dynamic-" + ii, new MutableTestSecurityContributor());
        }
      }

      private void bindStaticContributor(String name, SecurityContributor instance) {
        bind(SecurityContributor.class).annotatedWith(Names.named(name)).toInstance(instance);
      }

      private void bindDynamicContributor(String name, MutableTestSecurityContributor instance) {
        bind(MutableTestSecurityContributor.class).annotatedWith(Names.named(name)).toInstance(instance);
        bind(SecurityContributor.class).annotatedWith(Names.named(name)).toInstance(instance);
      }
    });
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    this.manager = lookup(SecurityConfigurationManager.class);

    // mimic EventManager auto-registration
    eventManager.register(manager);

    // test the lookup, make sure we have 200
    Assert.assertEquals(200, testContributors.size());

    this.expectedPrivilegeCount = this.manager.listPrivileges().size();

    // 100 static items with 3 privs each + 100 dynamic items + 2 from default config
    Assert.assertEquals((100 * 3) + 100 + 2, expectedPrivilegeCount);
  }

  @Test
  public void testThreading() throws Throwable {
    TestFramework.runOnce(new MultithreadedTestCase()
    {
      // public void initialize()
      // {
      //
      // }

      public void thread1() {
        mutableTestContributors.get(1).setDirty(true);
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread2() {
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread3() {
        mutableTestContributors.get(3).setDirty(true);
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread4() {
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread5() {
        mutableTestContributors.get(5).setDirty(true);
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

    });// , Integer.MAX_VALUE, Integer.MAX_VALUE ); // uncomment this for debugging, if you don't the framework
    // will timeout and close your debug session

    for (MutableTestSecurityContributor contributor : mutableTestContributors) {
      Assert.assertTrue(
          "Get config should be called on each contributor after any changed: " + contributor.getId(),
          contributor.wasConfigRequested());
    }
  }
}
