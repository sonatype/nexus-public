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

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.security.config.SecurityContributor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SecurityContributorMediator}.
 *
 * Tests the lifecycle-managed registration of security contributors and duplicate prevention.
 */
public class SecurityContributorMediatorTest
    extends TestSupport
{
  @Mock
  private SecurityConfigurationManagerImpl securityConfigurationManager;

  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private SecurityContributor contributor1;

  @Mock
  private SecurityContributor contributor2;

  private SecurityContributorMediator underTest;

  @Before
  public void setUp() {
    underTest = new SecurityContributorMediator(securityConfigurationManager, applicationContext);
  }

  @Test
  public void testDoStart_registersAllContributors() throws Exception {
    // Setup: Mock application context to return two contributors
    Map<String, SecurityContributor> contributors = new HashMap<>();
    contributors.put("contributor1", contributor1);
    contributors.put("contributor2", contributor2);
    when(applicationContext.getBeansOfType(SecurityContributor.class)).thenReturn(contributors);

    // Execute: Call doStart (simulates SECURITY phase startup)
    underTest.doStart();

    // Verify: Both contributors were registered
    verify(securityConfigurationManager, times(1)).addContributor(contributor1);
    verify(securityConfigurationManager, times(1)).addContributor(contributor2);
  }

  @Test
  public void testEventListener_registersNewContributors() {
    // Setup: Mock application context to return one contributor
    Map<String, SecurityContributor> contributors = new HashMap<>();
    contributors.put("contributor1", contributor1);

    ApplicationContext eventContext = mock(ApplicationContext.class);
    when(eventContext.getBeansOfType(SecurityContributor.class)).thenReturn(contributors);

    ContextRefreshedEvent event = mock(ContextRefreshedEvent.class);
    when(event.getApplicationContext()).thenReturn(eventContext);

    // Execute: Trigger the event listener
    underTest.on(event);

    // Verify: Contributor was registered
    verify(securityConfigurationManager, times(1)).addContributor(contributor1);
  }

  @Test
  public void testDuplicatePrevention_doesNotRegisterTwice() throws Exception {
    // Setup: Mock application context to return the same contributor
    Map<String, SecurityContributor> contributors = new HashMap<>();
    contributors.put("contributor1", contributor1);
    when(applicationContext.getBeansOfType(SecurityContributor.class)).thenReturn(contributors);

    // Execute: Call doStart first
    underTest.doStart();

    // Setup event with same contributor
    ApplicationContext eventContext = mock(ApplicationContext.class);
    when(eventContext.getBeansOfType(SecurityContributor.class)).thenReturn(contributors);

    ContextRefreshedEvent event = mock(ContextRefreshedEvent.class);
    when(event.getApplicationContext()).thenReturn(eventContext);

    // Execute: Trigger event listener with same contributor
    underTest.on(event);

    // Verify: Contributor was only registered once (duplicate prevented)
    verify(securityConfigurationManager, times(1)).addContributor(contributor1);
  }

  @Test
  public void testDoStart_withNoContributors() throws Exception {
    // Setup: Mock application context to return empty map
    when(applicationContext.getBeansOfType(SecurityContributor.class)).thenReturn(new HashMap<>());

    // Execute: Call doStart
    underTest.doStart();

    // Verify: No contributors were registered (no exception thrown)
    verify(securityConfigurationManager, times(0)).addContributor(contributor1);
    verify(securityConfigurationManager, times(0)).addContributor(contributor2);
  }

  @Test
  public void testMultipleEventListener_callsWithDifferentContributors() throws Exception {
    // Setup: First call with contributor1
    Map<String, SecurityContributor> contributors1 = new HashMap<>();
    contributors1.put("contributor1", contributor1);
    when(applicationContext.getBeansOfType(SecurityContributor.class)).thenReturn(contributors1);

    underTest.doStart();

    // Setup: Second event with contributor2 (simulates plugin loaded after startup)
    Map<String, SecurityContributor> contributors2 = new HashMap<>();
    contributors2.put("contributor2", contributor2);

    ApplicationContext eventContext = mock(ApplicationContext.class);
    when(eventContext.getBeansOfType(SecurityContributor.class)).thenReturn(contributors2);

    ContextRefreshedEvent event = mock(ContextRefreshedEvent.class);
    when(event.getApplicationContext()).thenReturn(eventContext);

    // Execute: Trigger event listener
    underTest.on(event);

    // Verify: Both contributors were registered (once each)
    verify(securityConfigurationManager, times(1)).addContributor(contributor1);
    verify(securityConfigurationManager, times(1)).addContributor(contributor2);
  }
}
