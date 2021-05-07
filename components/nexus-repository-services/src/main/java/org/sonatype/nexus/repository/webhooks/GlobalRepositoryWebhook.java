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

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.InitiatorProvider;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.manager.RepositoryCreatedEvent;
import org.sonatype.nexus.repository.manager.RepositoryDeletedEvent;
import org.sonatype.nexus.repository.manager.RepositoryUpdatedEvent;
import org.sonatype.nexus.webhooks.GlobalWebhook;
import org.sonatype.nexus.webhooks.Webhook;
import org.sonatype.nexus.webhooks.WebhookPayload;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Global repository {@link Webhook}.
 *
 * @since 3.1
 */
@Named
@Singleton
public class GlobalRepositoryWebhook
    extends GlobalWebhook
{
  public static final String NAME = "repository";

  private final NodeAccess nodeAccess;

  private final InitiatorProvider initiatorProvider;

  @Inject
  public GlobalRepositoryWebhook(final NodeAccess nodeAccess, final InitiatorProvider initiatorProvider) {
    this.nodeAccess = checkNotNull(nodeAccess);
    this.initiatorProvider = checkNotNull(initiatorProvider);
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Subscribe
  @AllowConcurrentEvents
  void on(final RepositoryCreatedEvent event) {
    queue(event.getRepository(), EventAction.CREATED);
  }

  @Subscribe
  @AllowConcurrentEvents
  void on(final RepositoryUpdatedEvent event) {
    queue(event.getRepository(), EventAction.UPDATED);
  }

  @Subscribe
  @AllowConcurrentEvents
  void on(final RepositoryDeletedEvent event) {
    queue(event.getRepository(), EventAction.DELETED);
  }

  private void queue(final Repository repository, final EventAction eventAction) {
    RepositoryWebhookPayload.RepositoryPayload repositoryPayload =
        new RepositoryWebhookPayload.RepositoryPayload(repository.getName(), repository.getType(),
            repository.getFormat());

    RepositoryWebhookPayload payload = new RepositoryWebhookPayload(eventAction, repositoryPayload, nodeAccess.getId(), new Date(), initiatorProvider.get());

    getSubscriptions().forEach(subscription -> queue(subscription, payload));
  }

  public enum EventAction
  {
    CREATED,
    UPDATED,
    DELETED
  }

  public static class RepositoryWebhookPayload
      extends WebhookPayload
  {
    private final EventAction action;

    private final RepositoryPayload repository;

    public RepositoryWebhookPayload(final EventAction action, final RepositoryPayload repository,
                                    final String nodeId,
                                    final Date timestamp,
                                    final String initiator) {
      this.action = checkNotNull(action);
      this.repository = checkNotNull(repository);
      setNodeId(nodeId);
      setTimestamp(timestamp);
      setInitiator(initiator);
    }

    public EventAction getAction() {
      return this.action;
    }

    public RepositoryPayload getRepository() {
      return this.repository;
    }

    public static class RepositoryPayload
    {
      private final String format;

      private final String name;

      private final String type;

      public RepositoryPayload(final String name, final Type type, final Format format) {
        this.name = checkNotNull(name);
        this.type = checkNotNull(type).getValue();
        this.format = checkNotNull(format).getValue();
      }

      public String getFormat() {
        return this.format;
      }

      public String getName() {
        return this.name;
      }

      public String getType() {
        return this.type;
      }
    }
  }
}
