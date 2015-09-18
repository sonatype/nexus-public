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

import org.sonatype.nexus.security.AbstractSecurityTest;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;
import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;
import org.junit.Assert;
import org.junit.Test;

public class ResourceMergingManagerThreadedTest
    extends AbstractSecurityTest
{
  private SecurityConfigurationManager manager;

  private int expectedPrivilegeCount = 0;

  @Inject
  private List<StaticSecurityConfigurationResource> injectedStaticResources;

  @Inject
  private List<DynamicSecurityConfigurationResource> injectedDynamicResources;

  @Override
  protected MemorySecurityConfiguration initialSecurityConfiguration() {
    return ResourceMergingConfigurationManagerTestSecurity.securityModel();
  }

  @Override
  protected void customizeModules(List<Module> modules) {
    super.customizeModules(modules);
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(StaticSecurityConfigurationResource.class).annotatedWith(Names.named("default"))
            .toInstance(new StaticSecurityConfigurationResource2());
        bind(DynamicSecurityConfigurationResource.class).annotatedWith(Names.named("default"))
            .toInstance(new UnitTestDynamicSecurityConfigurationResource());

        int staticResourceCount = 100;
        for (int ii = 0; ii < staticResourceCount - 1; ii++) {
          bind(StaticSecurityConfigurationResource.class).annotatedWith(Names.named("test-" + ii))
              .toInstance(new StaticSecurityConfigurationResource3());
        }

        int dynamicResourceCount = 100;
        for (int ii = 0; ii < dynamicResourceCount - 1; ii++) {
          bind(DynamicSecurityConfigurationResource.class).annotatedWith(Names.named("test-" + ii))
              .toInstance(new UnitTestDynamicSecurityConfigurationResource());
        }
      }
    });
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    this.manager = lookup(SecurityConfigurationManager.class);

    // test the lookup, make sure we have 100
    Assert.assertEquals(100, injectedStaticResources.size());
    Assert.assertEquals(100, injectedDynamicResources.size());

    this.expectedPrivilegeCount = this.manager.listPrivileges().size();

    // 100 static items with 3 privs each + 100 dynamic items + 2 from default config
    Assert.assertEquals((100 * 3) + 100 + 2, expectedPrivilegeCount);

    for (DynamicSecurityConfigurationResource dynamicSecurityConfigurationResource : injectedDynamicResources) {
      Assert.assertFalse(dynamicSecurityConfigurationResource.isDirty());
    }
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
        ((UnitTestDynamicSecurityConfigurationResource) injectedDynamicResources.get(1)).setDirty(true);
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread2() {
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread3() {
        ((UnitTestDynamicSecurityConfigurationResource) injectedDynamicResources.get(3)).setDirty(true);
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread4() {
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread5() {
        ((UnitTestDynamicSecurityConfigurationResource) injectedDynamicResources.get(5)).setDirty(true);
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

    });// , Integer.MAX_VALUE, Integer.MAX_VALUE ); // uncomment this for debugging, if you don't the framework
    // will timeout and close your debug session

    for (DynamicSecurityConfigurationResource dynamicSecurityConfigurationResource : injectedDynamicResources) {

      Assert.assertFalse(dynamicSecurityConfigurationResource.isDirty());
      Assert
          .assertTrue("Get config should be called on each dynamic resource after set dirty is called on any of them: "
              + ((UnitTestDynamicSecurityConfigurationResource) dynamicSecurityConfigurationResource).getId(),
              ((UnitTestDynamicSecurityConfigurationResource) dynamicSecurityConfigurationResource).isConfigCalledAfterSetDirty());
    }
  }
}
