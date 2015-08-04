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
package org.sonatype.nexus.proxy.repository;

import org.sonatype.nexus.configuration.ConfigurationPrepareForSaveEvent;
import org.sonatype.nexus.configuration.Configurator;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryExternalConfigurationHolderFactory;
import org.sonatype.nexus.events.Event;
import org.sonatype.nexus.proxy.events.RepositoryRegistryEventRemove;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doReturn;

/**
 * Unit tests for {@link AbstractGroupRepository}
 *
 * @since 2.2
 */
public class AnAbstractGroupRepositoryTest
    extends TestSupport
{

  @Mock(answer = Answers.RETURNS_SMART_NULLS)
  private Repository repo;

  @Mock
  private Event basicEvent;

  @Mock
  private RepositoryRegistryEventRemove removeEvent;

  @Mock
  private ConfigurationPrepareForSaveEvent prepSaveEvent;

  @Mock
  private CRepository cRepo;

  @Mock(answer = Answers.RETURNS_SMART_NULLS)
  private AbstractGroupRepositoryConfiguration extConfig;

  @Spy
  private AbstractGroupRepository groupRepo = new AbstractGroupRepository()
  {
    @Override
    protected Configurator getConfigurator() {
      return null;
    }

    @Override
    protected CRepositoryExternalConfigurationHolderFactory<?> getExternalConfigurationHolderFactory() {
      return null;
    }

    @Override
    public RepositoryKind getRepositoryKind() {
      return null;
    }

    @Override
    public ContentClass getRepositoryContentClass() {
      return null;
    }
  };


  @Test
  public void onEventNullExternalConfigurationShouldNotNPE()
      throws Exception
  {
    doReturn(null).when(groupRepo).getExternalConfiguration(anyBoolean());
    groupRepo.onEvent(removeEvent);
    groupRepo.onEvent(prepSaveEvent);
  }

  @Test
  public void onEventNullCurrentConfigurationShouldNotNPE() {
    doReturn(null).when(groupRepo).getCurrentConfiguration(anyBoolean());
    groupRepo.onEvent(removeEvent);
    groupRepo.onEvent(prepSaveEvent);
  }

  @Test
  public void onEventCurrentConfigurationShouldNotNPE() {
    doReturn(repo).when(removeEvent).getRepository(); // not expected to return null
    doReturn(extConfig).when(groupRepo).getExternalConfiguration(anyBoolean());
    doReturn(cRepo).when(groupRepo).getCurrentConfiguration(anyBoolean());
    groupRepo.onEvent(removeEvent);
    groupRepo.onEvent(prepSaveEvent);

  }


}
