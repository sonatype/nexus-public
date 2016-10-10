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

public class SecurityContributorThreadedTest
    extends AbstractSecurityTest
{
  private SecurityConfigurationManager manager;

  private int expectedPrivilegeCount = 0;

  @Inject
  private List<SecurityContributor> staticContributors;

  @Inject
  private List<DynamicSecurityContributor> dynamicContributors;

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
        bind(SecurityContributor.class).annotatedWith(Names.named("default"))
            .toInstance(new TestSecurityContributor2());
        bind(DynamicSecurityContributor.class).annotatedWith(Names.named("default"))
            .toInstance(new MutableTestSecurityContributor());

        int staticResourceCount = 100;
        for (int ii = 0; ii < staticResourceCount - 1; ii++) {
          bind(SecurityContributor.class).annotatedWith(Names.named("test-" + ii))
              .toInstance(new TestSecurityContributor3());
        }

        int dynamicResourceCount = 100;
        for (int ii = 0; ii < dynamicResourceCount - 1; ii++) {
          bind(DynamicSecurityContributor.class).annotatedWith(Names.named("test-" + ii))
              .toInstance(new MutableTestSecurityContributor());
        }
      }
    });
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    this.manager = lookup(SecurityConfigurationManager.class);

    // test the lookup, make sure we have 100
    Assert.assertEquals(100, staticContributors.size());
    Assert.assertEquals(100, dynamicContributors.size());

    this.expectedPrivilegeCount = this.manager.listPrivileges().size();

    // 100 static items with 3 privs each + 100 dynamic items + 2 from default config
    Assert.assertEquals((100 * 3) + 100 + 2, expectedPrivilegeCount);

    for (DynamicSecurityContributor contributor : dynamicContributors) {
      Assert.assertFalse(contributor.isDirty());
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
        ((MutableTestSecurityContributor) dynamicContributors.get(1)).setDirty(true);
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread2() {
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread3() {
        ((MutableTestSecurityContributor) dynamicContributors.get(3)).setDirty(true);
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread4() {
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread5() {
        ((MutableTestSecurityContributor) dynamicContributors.get(5)).setDirty(true);
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

    });// , Integer.MAX_VALUE, Integer.MAX_VALUE ); // uncomment this for debugging, if you don't the framework
    // will timeout and close your debug session

    for (DynamicSecurityContributor contributor : dynamicContributors) {

      Assert.assertFalse(contributor.isDirty());
      Assert
          .assertTrue("Get config should be called on each dynamic resource after set dirty is called on any of them: "
              + ((MutableTestSecurityContributor) contributor).getId(),
              ((MutableTestSecurityContributor) contributor).wasConfigRequested());
    }
  }
}
