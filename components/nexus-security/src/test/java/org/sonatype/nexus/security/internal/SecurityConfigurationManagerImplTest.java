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
package org.sonatype.nexus.security.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.MemorySecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfiguration;
import org.sonatype.nexus.security.config.SecurityConfigurationCleaner;
import org.sonatype.nexus.security.config.SecurityConfigurationSource;
import org.sonatype.nexus.security.config.SecurityContributor;

import org.apache.shiro.authc.credential.PasswordService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class SecurityConfigurationManagerImplTest
    extends TestSupport
{
  @Mock
  private SecurityConfigurationSource configSource;

  @Mock
  private SecurityConfigurationCleaner configCleaner;

  @Mock
  private PasswordService passwordService;

  @Mock
  private EventManager eventManager;

  private SecurityConfigurationManagerImpl manager;

  @Before
  public void setUp() {
    when(configSource.loadConfiguration()).thenReturn(new MemorySecurityConfiguration());
    manager = new SecurityConfigurationManagerImpl(configSource, configCleaner, passwordService, eventManager);
  }

  @Test
  public void testGetMergedConfiguration_DontLooseMutationsWhileConfigurationIsBeingRebuild() {
    int[] mutableContributorCallCount = new int[1];
    SecurityContributor mutableContributor = new SecurityContributor()
    {
      @Override
      public SecurityConfiguration getContribution() {
        SecurityConfiguration config = new MemorySecurityConfiguration();
        if (mutableContributorCallCount[0]++ > 0) {
          CPrivilege priv = new CPrivilege();
          priv.setId("test-id");
          priv.setType("test-type");
          config.addPrivilege(priv);
        }
        return config;
      }
    };
    SecurityContributor laterContributor = new SecurityContributor()
    {
      private int callCount;

      @Override
      public SecurityConfiguration getContribution() {
        // the fixture requires laterContributor to get inspected after mutableContributor, double-check sequencing
        assertThat(mutableContributorCallCount[0], is(greaterThan(callCount)));
        if (callCount++ == 0) {
          // this emulates a mutation to mutableContributor after it just had its configuration read
          manager.on(new SecurityContributionChangedEvent());
        }
        return new MemorySecurityConfiguration();
      }
    };
    manager.addContributor(mutableContributor);
    manager.addContributor(laterContributor);
    assertThat(manager.listPrivileges(), hasSize(0));
    assertThat(manager.listPrivileges(), hasSize(1));
    assertThat(mutableContributorCallCount[0], is(2));
  }
}
