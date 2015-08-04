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
package org.sonatype.security.realms.tools;

import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import org.sonatype.security.AbstractSecurityTestCase;

import com.google.inject.Binder;
import com.google.inject.name.Names;
import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;
import junit.framework.Assert;

public class ResourceMergingManagerThreadedTest
    extends AbstractSecurityTestCase
{
  private ConfigurationManager manager;

  private int expectedPrivilegeCount = 0;

  @Inject
  private List<StaticSecurityResource> injectedStaticResources;

  @Inject
  private List<DynamicSecurityResource> injectedDynamicResources;

  @Override
  public void configure(Properties properties) {
    super.configure(properties);

    //Overriding default value set in parent
    properties.put("security-xml-file",
        "target/test-classes/org/sonatype/jsecurity/configuration/static-merging/security.xml");
  }

  @Override
  public void configure(Binder binder) {
    super.configure(binder);

    binder.bind(StaticSecurityResource.class).annotatedWith(Names.named("default")).to(UnitTestSecurityResource.class);
    binder.bind(DynamicSecurityResource.class).annotatedWith(Names.named("default"))
        .to(UnitTestDynamicSecurityResource.class);

    int staticResourceCount = 100;
    for (int ii = 0; ii < staticResourceCount - 1; ii++) {
      binder.bind(StaticSecurityResource.class).annotatedWith(Names.named("test-" + ii))
          .to(UnitTestSecurityResource.class);
    }

    int dynamicResourceCount = 100;
    for (int ii = 0; ii < dynamicResourceCount - 1; ii++) {
      binder.bind(DynamicSecurityResource.class).annotatedWith(Names.named("test-" + ii))
          .to(UnitTestDynamicSecurityResource.class);
    }
  }

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    this.manager = (ConfigurationManager) lookup(ConfigurationManager.class, "resourceMerging");

    // test the lookup, make sure we have 100
    Assert.assertEquals(100, injectedStaticResources.size());
    Assert.assertEquals(100, injectedDynamicResources.size());

    this.expectedPrivilegeCount = this.manager.listPrivileges().size();

    // 100 static items with 3 privs each + 100 dynamic items
    Assert.assertEquals((100 * 3) + 100, expectedPrivilegeCount);

    for (DynamicSecurityResource dynamicSecurityResource : injectedDynamicResources) {
      Assert.assertFalse(dynamicSecurityResource.isDirty());
    }
  }

  public void testThreading()
      throws Throwable
  {
    TestFramework.runOnce(new MultithreadedTestCase()
    {
      // public void initialize()
      // {
      //
      // }

      public void thread1() {
        ((UnitTestDynamicSecurityResource) injectedDynamicResources.get(1)).setDirty(true);
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread2() {
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread3() {
        ((UnitTestDynamicSecurityResource) injectedDynamicResources.get(3)).setDirty(true);
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread4() {
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread5() {
        ((UnitTestDynamicSecurityResource) injectedDynamicResources.get(5)).setDirty(true);
        Assert.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

    });// , Integer.MAX_VALUE, Integer.MAX_VALUE ); // uncomment this for debugging, if you don't the framework
    // will timeout and close your debug session

    for (DynamicSecurityResource dynamicSecurityResource : injectedDynamicResources) {

      Assert.assertFalse(dynamicSecurityResource.isDirty());
      Assert
          .assertTrue("Get config should be called on each dynamic resource after set dirty is called on any of them: "
              + ((UnitTestDynamicSecurityResource) dynamicSecurityResource).getId(),
              ((UnitTestDynamicSecurityResource) dynamicSecurityResource).isConfigCalledAfterSetDirty());
    }

  }

}
