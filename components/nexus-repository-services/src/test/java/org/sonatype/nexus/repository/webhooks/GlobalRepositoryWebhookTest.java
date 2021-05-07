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
package org.sonatype.nexus.repository.webhooks;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.audit.InitiatorProvider;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryEvent;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.webhooks.GlobalRepositoryWebhook.RepositoryWebhookPayload;
import org.sonatype.nexus.webhooks.WebhookRequestSendEvent;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.goodies.testsupport.hamcrest.DiffMatchers.equalTo;

public class GlobalRepositoryWebhookTest
    extends TestSupport
{
  @Mock
  private EventManager eventManager;

  @Mock
  private InitiatorProvider initiatorProvider;

  @Mock
  private NodeAccess nodeAccess;

  @Mock
  private RepositoryCreatedEvent repositoryCreatedEvent;

  @Mock
  private RepositoryUpdatedEvent repositoryUpdatedEvent;

  @Mock
  private RepositoryDeletedEvent repositoryDeletedEvent;

  private GlobalRepositoryWebhook globalRepositoryWebhook;

  @Before
  public void before() {
    globalRepositoryWebhook = new GlobalRepositoryWebhook(nodeAccess, initiatorProvider);
    globalRepositoryWebhook.setEventManager(eventManager);

    for (RepositoryEvent repositoryEvent : Lists
        .newArrayList(repositoryCreatedEvent, repositoryDeletedEvent, repositoryUpdatedEvent)) {
      Repository repository = mock(Repository.class);
      when(repository.getName()).thenReturn("name");
      when(repository.getFormat()).thenReturn(new TestFormat());
      when(repository.getType()).thenReturn(new ProxyType());

      when(repositoryEvent.getRepository()).thenReturn(repository);
    }

    RepositoryWebhook.Configuration configuration = mock(RepositoryWebhook.Configuration.class);
    when(configuration.getRepository()).thenReturn("repoName");
    globalRepositoryWebhook.subscribe(configuration);

    when(initiatorProvider.get()).thenReturn("initiator");
    when(nodeAccess.getId()).thenReturn("nodeId");
  }

  @Test
  public void hasTheCorrectEventId() {
    assertThat(globalRepositoryWebhook.getId(), is(equalTo("rm:global:repository")));
  }

  @Test
  public void queuesRepositoryCreatedEvents() {
    globalRepositoryWebhook.on(repositoryCreatedEvent);
    testRepositoryEvent("CREATED");
  }

  @Test
  public void queuesRepositoryUpdatedEvents() {
    globalRepositoryWebhook.on(repositoryUpdatedEvent);
    testRepositoryEvent("UPDATED");
  }

  @Test
  public void queuesRepositoryDeletedEvents() {
    globalRepositoryWebhook.on(repositoryDeletedEvent);
    testRepositoryEvent("DELETED");
  }

  private void testRepositoryEvent(String action) {
    ArgumentCaptor<WebhookRequestSendEvent> assetArgumentCaptor =
        ArgumentCaptor.forClass(WebhookRequestSendEvent.class);
    verify(eventManager).post(assetArgumentCaptor.capture());

    RepositoryWebhookPayload repositoryPayload =
        (RepositoryWebhookPayload) assetArgumentCaptor.getValue().getRequest().getPayload();

    assertThat(repositoryPayload.getInitiator(), is(equalTo("initiator")));
    assertThat(repositoryPayload.getNodeId(), is(equalTo("nodeId")));
    assertThat(repositoryPayload.getAction().toString(), is(equalTo(action)));

    assertThat(repositoryPayload.getRepository().getName(), is(equalTo("name")));
    assertThat(repositoryPayload.getRepository().getFormat(), is(equalTo("format")));
    assertThat(repositoryPayload.getRepository().getType(), is(equalTo("proxy")));
  }

  public class TestFormat
      extends Format
  {
    public TestFormat() {
      super("format");
    }
  }
}
