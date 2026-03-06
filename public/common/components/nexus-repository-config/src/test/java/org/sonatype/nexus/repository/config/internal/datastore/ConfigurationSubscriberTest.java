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
package org.sonatype.nexus.repository.config.internal.datastore;

import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.MissingRepositoryException;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationCreatedEvent;
import org.sonatype.nexus.repository.config.ConfigurationDeletedEvent;
import org.sonatype.nexus.repository.config.ConfigurationUpdatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigurationSubscriberTest
    extends TestSupport
{
  private static final String TEST_REPO_NAME = "test-repo";

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Configuration configuration;

  @Mock
  private ConfigurationCreatedEvent createdEvent;

  @Mock
  private ConfigurationUpdatedEvent updatedEvent;

  @Mock
  private ConfigurationDeletedEvent deletedEvent;

  private ConfigurationSubscriber underTest;

  @Before
  public void setup() {
    underTest = new ConfigurationSubscriber(repositoryManager);
  }

  @Test
  public void testOnCreatedEvent_missingConfiguration_shouldSkipCreate() throws Exception {
    when(createdEvent.isLocal()).thenReturn(false);
    when(createdEvent.getRepositoryName()).thenReturn(TEST_REPO_NAME);
    when(repositoryManager.retrieveConfigurationByName(TEST_REPO_NAME)).thenReturn(Optional.empty());

    underTest.on(createdEvent);

    verify(repositoryManager).retrieveConfigurationByName(TEST_REPO_NAME);
    verify(repositoryManager, never()).create(any());
  }

  @Test
  public void testOnCreatedEvent_configurationExists_shouldCreate() throws Exception {
    when(createdEvent.isLocal()).thenReturn(false);
    when(createdEvent.getRepositoryName()).thenReturn(TEST_REPO_NAME);
    when(repositoryManager.retrieveConfigurationByName(TEST_REPO_NAME)).thenReturn(Optional.of(configuration));

    underTest.on(createdEvent);

    verify(repositoryManager).retrieveConfigurationByName(TEST_REPO_NAME);
    verify(repositoryManager).create(configuration);
  }

  @Test
  public void testOnUpdatedEvent_missingConfiguration_shouldSkipUpdate() throws Exception {
    when(updatedEvent.isLocal()).thenReturn(false);
    when(updatedEvent.getRepositoryName()).thenReturn(TEST_REPO_NAME);
    when(repositoryManager.retrieveConfigurationByName(TEST_REPO_NAME)).thenReturn(Optional.empty());

    underTest.on(updatedEvent);

    verify(repositoryManager).retrieveConfigurationByName(TEST_REPO_NAME);
    verify(repositoryManager, never()).update(any());
  }

  @Test
  public void testOnUpdatedEvent_configurationExists_shouldUpdate() throws Exception {
    when(updatedEvent.isLocal()).thenReturn(false);
    when(updatedEvent.getRepositoryName()).thenReturn(TEST_REPO_NAME);
    when(repositoryManager.retrieveConfigurationByName(TEST_REPO_NAME)).thenReturn(Optional.of(configuration));

    underTest.on(updatedEvent);

    verify(repositoryManager).retrieveConfigurationByName(TEST_REPO_NAME);
    verify(repositoryManager).update(configuration);
  }

  @Test
  public void testOnDeletedEvent_missingRepositoryException_shouldHandleGracefully() throws Exception {
    when(deletedEvent.isLocal()).thenReturn(false);
    when(deletedEvent.getRepositoryName()).thenReturn(TEST_REPO_NAME);
    doThrow(new MissingRepositoryException(TEST_REPO_NAME)).when(repositoryManager).delete(TEST_REPO_NAME);

    underTest.on(deletedEvent);

    verify(repositoryManager).delete(TEST_REPO_NAME);
  }

  @Test
  public void testOnDeletedEvent_repositoryExists_shouldDelete() throws Exception {
    when(deletedEvent.isLocal()).thenReturn(false);
    when(deletedEvent.getRepositoryName()).thenReturn(TEST_REPO_NAME);

    underTest.on(deletedEvent);

    verify(repositoryManager).delete(TEST_REPO_NAME);
  }

  @Test
  public void testOnCreatedEvent_localEvent_shouldNotReplicate() throws Exception {
    when(createdEvent.isLocal()).thenReturn(true);

    underTest.on(createdEvent);

    verify(repositoryManager, never()).retrieveConfigurationByName(any());
    verify(repositoryManager, never()).create(any());
  }

  @Test
  public void testOnUpdatedEvent_localEvent_shouldNotReplicate() throws Exception {
    when(updatedEvent.isLocal()).thenReturn(true);

    underTest.on(updatedEvent);

    verify(repositoryManager, never()).retrieveConfigurationByName(any());
    verify(repositoryManager, never()).update(any());
  }

  @Test
  public void testOnDeletedEvent_localEvent_shouldNotReplicate() throws Exception {
    when(deletedEvent.isLocal()).thenReturn(true);

    underTest.on(deletedEvent);

    verify(repositoryManager, never()).delete(any());
  }
}
