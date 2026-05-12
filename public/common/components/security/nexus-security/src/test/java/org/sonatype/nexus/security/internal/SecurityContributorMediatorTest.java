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

import org.sonatype.nexus.security.config.SecurityContributor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SecurityContributorMediator}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SecurityContributorMediatorTest
{
  @Mock
  private SecurityConfigurationManagerImpl securityConfigurationManager;

  @Mock
  private SecurityContributor contributor1;

  @Mock
  private SecurityContributor contributor2;

  private SecurityContributorMediator underTest;

  @Before
  public void setUp() {
    underTest = new SecurityContributorMediator(securityConfigurationManager);
  }

  @Test
  public void testEventListener_registersContributors() {
    Map<String, SecurityContributor> contributors = new HashMap<>();
    contributors.put("contributor1", contributor1);
    contributors.put("contributor2", contributor2);

    ApplicationContext eventContext = mock(ApplicationContext.class);
    when(eventContext.getBeansOfType(SecurityContributor.class)).thenReturn(contributors);

    ContextRefreshedEvent event = mock(ContextRefreshedEvent.class);
    when(event.getApplicationContext()).thenReturn(eventContext);

    underTest.on(event);

    verify(securityConfigurationManager, times(1)).addContributor(contributor1);
    verify(securityConfigurationManager, times(1)).addContributor(contributor2);
  }

  @Test
  public void testEventListener_withNoContributors() {
    ApplicationContext eventContext = mock(ApplicationContext.class);
    when(eventContext.getBeansOfType(SecurityContributor.class)).thenReturn(new HashMap<>());

    ContextRefreshedEvent event = mock(ContextRefreshedEvent.class);
    when(event.getApplicationContext()).thenReturn(eventContext);

    underTest.on(event);

    verify(securityConfigurationManager, times(0)).addContributor(contributor1);
  }
}
