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

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.config.SecurityContributorThreadedTest.SecurityContributorThreadedTestConfiguration;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(SecurityContributorThreadedTestConfiguration.class)
public class SecurityContributorThreadedTest
    extends AbstractSecurityTest
{
  static class SecurityContributorThreadedTestConfiguration
  {
    @Qualifier("default")
    @Primary
    @Bean
    SecurityConfigurationSource securityConfigurationSource() {
      return new PreconfiguredSecurityConfigurationSource(InitialSecurityConfiguration.getConfiguration());
    }

    @Qualifier("static-default")
    @Bean
    SecurityContributor testSecurityContributor2() {
      return new TestSecurityContributor2();
    }

    @Qualifier("dynamic-default")
    @Bean
    MutableTestSecurityContributor mutableTestSecurityContributor() {
      return new MutableTestSecurityContributor();
    }

    @Bean
    static BeanFactoryPostProcessor additionalContributorsRegistrar() {
      return beanFactory -> {
        DefaultListableBeanFactory factory = (DefaultListableBeanFactory) beanFactory;
        // Register 99 more static contributors
        for (int i = 0; i < 99; i++) {
          String name = "static-" + i;
          SecurityContributor contributor = new TestSecurityContributor3();
          factory.registerSingleton(name, contributor);
        }

        // Register 99 more dynamic contributors
        for (int i = 0; i < 99; i++) {
          String name = "dynamic-" + i;
          MutableTestSecurityContributor contributor = new MutableTestSecurityContributor();
          factory.registerSingleton(name, contributor);
        }
      };
    }
  }

  private SecurityConfigurationManager manager;

  private int expectedPrivilegeCount = 0;

  @Autowired
  private List<SecurityContributor> testContributors;

  @Autowired
  private List<MutableTestSecurityContributor> mutableTestContributors;

  @Autowired
  private EventManager eventManager;

  @BeforeEach
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    this.manager = lookup(SecurityConfigurationManager.class);

    // mimic EventManager auto-registration
    eventManager.register(manager);

    initializeInjectedContributors();

    // test the lookup: 100 static + 100 dynamic + 1 UploaderMetadataSecurityContributor
    assertEquals(201, testContributors.size());

    this.expectedPrivilegeCount = this.manager.listPrivileges().size();

    // 100 static items with 3 privs each + 100 dynamic items + 2 from default config + 1 uploader-metadata
    assertEquals((100 * 3) + 100 + 3, expectedPrivilegeCount);
  }

  private void initializeInjectedContributors() {
    // Initialize all programmatically injected dynamic contributors
    for (MutableTestSecurityContributor contributor : mutableTestContributors) {
      try {
        contributor.initialize(eventManager, manager);
      }
      catch (IllegalStateException e) {
        // Already initialized, ignore
      }
    }
  }

  @Test
  void testThreading() throws Throwable {
    TestFramework.runOnce(new MultithreadedTestCase()
    {
      // public void initialize()
      // {
      //
      // }

      public void thread1() {
        mutableTestContributors.get(1).setDirty(true);
        Assertions.assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread2() {
        assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread3() {
        mutableTestContributors.get(3).setDirty(true);
        assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread4() {
        assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }

      public void thread5() {
        mutableTestContributors.get(5).setDirty(true);
        assertEquals(expectedPrivilegeCount, manager.listPrivileges().size());
      }
    });// , Integer.MAX_VALUE, Integer.MAX_VALUE ); // uncomment this for debugging, if you don't the framework
    // will timeout and close your debug session

    for (MutableTestSecurityContributor contributor : mutableTestContributors) {
      assertTrue(contributor.wasConfigRequested(),
          "Get config should be called on each contributor after any changed: " + contributor.getId());
    }
  }
}
