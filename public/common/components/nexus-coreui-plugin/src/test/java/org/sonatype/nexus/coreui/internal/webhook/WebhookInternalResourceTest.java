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
package org.sonatype.nexus.coreui.internal.webhook;

import java.util.List;

import org.sonatype.nexus.coreui.ReferenceXO;
import org.sonatype.nexus.webhooks.GlobalWebhook;
import org.sonatype.nexus.repository.webhooks.RepositoryWebhook;
import org.sonatype.nexus.webhooks.Webhook;
import org.sonatype.nexus.webhooks.WebhookService;
import org.sonatype.nexus.webhooks.WebhookType;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookInternalResourceTest
{
  @Mock
  private WebhookService webhookService;

  @Mock
  private SecurityManager securityManager;

  @Mock
  private Subject subject;

  private WebhookInternalResource underTest;

  @BeforeEach
  void setUp() {
    lenient().when(subject.isAuthenticated()).thenReturn(true);
    lenient().when(subject.isPermitted(any(String.class))).thenReturn(true);
    ThreadContext.bind(securityManager);
    ThreadContext.bind(subject);

    underTest = new WebhookInternalResource(webhookService);
  }

  @AfterEach
  void tearDown() {
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
  }

  @Test
  void getWebhookTypes_returnsGlobalWebhooks_whenTypeIsGlobal() {
    Webhook globalAudit = createWebhook(GlobalWebhook.TYPE, "audit");
    Webhook globalRepository = createWebhook(GlobalWebhook.TYPE, "repository");
    Webhook repoAsset = createWebhook(RepositoryWebhook.TYPE, "asset");

    when(webhookService.getWebhooks()).thenReturn(List.of(globalAudit, globalRepository, repoAsset));

    List<ReferenceXO> result = underTest.getWebhookTypes("global");

    assertThat(result.size(), is(2));
    assertThat(result.get(0).getId(), is("audit"));
    assertThat(result.get(1).getId(), is("repository"));
  }

  @Test
  void getWebhookTypes_returnsRepositoryWebhooks_whenTypeIsRepository() {
    Webhook globalAudit = createWebhook(GlobalWebhook.TYPE, "audit");
    Webhook repoAsset = createWebhook(RepositoryWebhook.TYPE, "asset");
    Webhook repoComponent = createWebhook(RepositoryWebhook.TYPE, "component");

    when(webhookService.getWebhooks()).thenReturn(List.of(globalAudit, repoAsset, repoComponent));

    List<ReferenceXO> result = underTest.getWebhookTypes("repository");

    assertThat(result.size(), is(2));
    assertThat(result.get(0).getId(), is("asset"));
    assertThat(result.get(1).getId(), is("component"));
  }

  @Test
  void getWebhookTypes_defaultsToGlobal_whenTypeIsNull() {
    Webhook globalAudit = createWebhook(GlobalWebhook.TYPE, "audit");
    Webhook repoAsset = createWebhook(RepositoryWebhook.TYPE, "asset");

    when(webhookService.getWebhooks()).thenReturn(List.of(globalAudit, repoAsset));

    List<ReferenceXO> result = underTest.getWebhookTypes(null);

    assertThat(result.size(), is(1));
    assertThat(result.get(0).getId(), is("audit"));
  }

  private Webhook createWebhook(final WebhookType type, final String name) {
    Webhook webhook = mock(Webhook.class);
    lenient().when(webhook.getType()).thenReturn(type);
    lenient().when(webhook.getName()).thenReturn(name);
    return webhook;
  }
}
