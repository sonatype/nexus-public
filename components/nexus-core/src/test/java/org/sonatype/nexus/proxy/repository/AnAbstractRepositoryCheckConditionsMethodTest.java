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

import java.lang.reflect.Field;
import java.util.Map;

import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.DefaultRepositoryItemUidFactory;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidFactory;
import org.sonatype.nexus.proxy.item.uid.CoreRepositoryItemUidAttributeSource;
import org.sonatype.nexus.proxy.item.uid.DefaultRepositoryItemUidAttributeManager;
import org.sonatype.nexus.proxy.item.uid.RepositoryItemUidAttributeManager;
import org.sonatype.nexus.proxy.item.uid.RepositoryItemUidAttributeSource;
import org.sonatype.nexus.proxy.maven.uid.MavenRepositoryItemUidAttributeSource;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.litmus.testsupport.TestSupport;
import org.sonatype.sisu.locks.LocalResourceLockFactory;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnAbstractRepositoryCheckConditionsMethodTest
    extends TestSupport
{
  @Mock
  private RepositoryRegistry repositoryRegistry;

  private AbstractRepository abstractRepository;

  private RepositoryItemUidFactory repositoryItemUidFactory;

  private RepositoryItemUidAttributeManager repositoryItemUidAttributeManager;

  @Before
  public void prepare() throws Exception {
    final Map<String, RepositoryItemUidAttributeSource> attributeSourceMap = ImmutableMap.of(
        "core", new CoreRepositoryItemUidAttributeSource(),
        "maven", new MavenRepositoryItemUidAttributeSource()
    );
    repositoryItemUidAttributeManager = new DefaultRepositoryItemUidAttributeManager(attributeSourceMap);
    repositoryItemUidAttributeManager.reset();

    abstractRepository = mock(AbstractRepository.class);
    // FIXME: HACK Warning, ComponentSupport uses method call on itself to init log, so in mock the log variable is null
    // FIXME: maybe move this code into own "helper" class a la Spring's ReflectionTestUtils
    Field logField = AbstractRepository.class.getSuperclass().getSuperclass().getSuperclass().getSuperclass()
        .getDeclaredField("log");
    logField.setAccessible(true);
    logField.set(abstractRepository, LoggerFactory.getLogger(AbstractRepository.class));

    doCallRealMethod().when(abstractRepository).checkConditions(any(ResourceStoreRequest.class), any(Action.class));
    doReturn("test").when(abstractRepository).getId();
    doReturn(LocalStatus.IN_SERVICE).when(abstractRepository).getLocalStatus();
    doReturn(mock(AccessManager.class)).when(abstractRepository).getAccessManager();
    doReturn(repositoryItemUidAttributeManager).when(abstractRepository).getRepositoryItemUidAttributeManager();
    doReturn(repositoryItemUidFactory).when(abstractRepository).getRepositoryItemUidFactory();

    when(repositoryRegistry.getRepository(anyString())).thenReturn(abstractRepository);

    this.repositoryItemUidFactory = new DefaultRepositoryItemUidFactory(mock(EventBus.class), repositoryRegistry,
        new LocalResourceLockFactory());
    when(abstractRepository.createUid(anyString())).thenAnswer(new Answer<RepositoryItemUid>()
    {
      @Override
      public RepositoryItemUid answer(final InvocationOnMock invocationOnMock) throws Throwable {
        return repositoryItemUidFactory.createUid(abstractRepository, (String) invocationOnMock.getArguments()[0]);
      }
    });
  }

  @Test
  public void plainInternalRequest() throws Exception {
    final ResourceStoreRequest resourceStoreRequest = new ResourceStoreRequest("/some/path");
    resourceStoreRequest.setExternal(false);
    abstractRepository.checkConditions(resourceStoreRequest, Action.read);
  }

  @Test
  public void plainExternalRequest() throws Exception {
    final ResourceStoreRequest resourceStoreRequest = new ResourceStoreRequest("/some/path");
    resourceStoreRequest.setExternal(true);
    abstractRepository.checkConditions(resourceStoreRequest, Action.read);
  }

  @Test
  public void indexInternalRequest() throws Exception {
    final ResourceStoreRequest resourceStoreRequest = new ResourceStoreRequest("/.index/some/path");
    resourceStoreRequest.setExternal(false);
    abstractRepository.checkConditions(resourceStoreRequest, Action.read);
  }

  @Test
  public void indexExternalRequest() throws Exception {
    final ResourceStoreRequest resourceStoreRequest = new ResourceStoreRequest("/.index/some/path");
    resourceStoreRequest.setExternal(true);
    abstractRepository.checkConditions(resourceStoreRequest, Action.read);
  }

  @Test
  public void trashInternalRequest() throws Exception {
    final ResourceStoreRequest resourceStoreRequest = new ResourceStoreRequest("/.nexus/trash/some/path");
    resourceStoreRequest.setExternal(false);
    abstractRepository.checkConditions(resourceStoreRequest, Action.read);
  }

  @Test(expected = ItemNotFoundException.class)
  public void trashExternalRequest() throws Exception {
    final ResourceStoreRequest resourceStoreRequest = new ResourceStoreRequest("/.nexus/trash/some/path");
    resourceStoreRequest.setExternal(true);
    abstractRepository.checkConditions(resourceStoreRequest, Action.read);
  }

  @Test
  public void attributeInternalRequest() throws Exception {
    final ResourceStoreRequest resourceStoreRequest = new ResourceStoreRequest("/.nexus/attributes/some/path");
    resourceStoreRequest.setExternal(false);
    abstractRepository.checkConditions(resourceStoreRequest, Action.read);
  }

  @Test(expected = ItemNotFoundException.class)
  public void attributeExternalRequest() throws Exception {
    final ResourceStoreRequest resourceStoreRequest = new ResourceStoreRequest("/.nexus/attributes/some/path");
    resourceStoreRequest.setExternal(true);
    abstractRepository.checkConditions(resourceStoreRequest, Action.read);
  }

  @Test
  public void attributeExternalRequestWithFeatureDisabled() throws Exception {
    final String attributeAccessEnabled = "nexus.content.attributeAccessEnabled";
    System.setProperty(attributeAccessEnabled, Boolean.TRUE.toString());
    // need to prepare again, to pick up system property changes
    prepare();
    try {
      final ResourceStoreRequest resourceStoreRequest = new ResourceStoreRequest("/.nexus/attributes/some/path");
      resourceStoreRequest.setExternal(true);
      abstractRepository.checkConditions(resourceStoreRequest, Action.read);
    }
    finally {
      System.clearProperty(attributeAccessEnabled);
    }
  }
}
