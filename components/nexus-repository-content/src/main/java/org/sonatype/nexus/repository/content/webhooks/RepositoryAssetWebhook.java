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
package org.sonatype.nexus.repository.content.webhooks;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.InitiatorProvider;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPurgedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUpdatedEvent;
import org.sonatype.nexus.repository.content.store.InternalIds;
import org.sonatype.nexus.repository.rest.api.RepositoryItemIDXO;
import org.sonatype.nexus.repository.webhooks.RepositoryWebhook;
import org.sonatype.nexus.webhooks.WebhookPayload;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class RepositoryAssetWebhook
    extends RepositoryWebhook
{
  public static final String NAME = "asset";

  private final NodeAccess nodeAccess;

  private final InitiatorProvider initiatorProvider;

  @Inject
  public RepositoryAssetWebhook(final NodeAccess nodeAccess, final InitiatorProvider initiatorProvider) {
    this.nodeAccess = checkNotNull(nodeAccess);
    this.initiatorProvider = checkNotNull(initiatorProvider);
  }

  @Override
  public String getName() {
    return NAME;
  }

  private enum EventAction
  {
    CREATED,
    UPDATED,
    DELETED,
    PURGED
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetCreatedEvent event) {
    maybeQueue(event, EventAction.CREATED);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetUpdatedEvent event) {
    maybeQueue(event, EventAction.UPDATED);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetDeletedEvent event) {
    maybeQueue(event, EventAction.DELETED);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetPurgedEvent event) {
    maybeQueue(getPayload(event, EventAction.PURGED));
  }

  /**
   * Maybe queue {@link WebhookRequest} for event matching subscriptions.
   */
  private void maybeQueue(final AssetEvent event, final EventAction eventAction) {
    maybeQueue(getPayload(event, eventAction));
  }

  /**
   * Maybe queue {@link WebhookRequest} for event matching subscriptions.
   */
  private void maybeQueue(final RepositoryAssetWebhookPayload payload) {
    subscriptions.forEach(subscription -> {
      RepositoryWebhook.Configuration configuration = (RepositoryWebhook.Configuration) subscription.getConfiguration();
      if (configuration.getRepository().equals(payload.getRepositoryName())) {
        queue(subscription, payload);
      }
    });
  }

  private RepositoryAssetWebhookPayload getPayload(final String repositoryName, final EventAction eventAction) {
    return new RepositoryAssetWebhookPayload(
        nodeAccess.getId(),
        new Date(),
        initiatorProvider.get(),
        repositoryName,
        eventAction);
  }

  private RepositoryAssetWebhookPayload getPayload(final AssetEvent event, final EventAction eventAction) {
    Optional<Repository> repository = event.getRepository();
    String repositoryName = repository.map(Repository::getName).orElse(null);
    String format = repository.map(r -> r.getFormat().getValue()).orElse(null);

    Asset asset = event.getAsset();
    EntityId assetId = InternalIds.toExternalId(InternalIds.internalAssetId(asset));
    RepositoryAssetWebhookPayload payload = getPayload(repositoryName, eventAction);
    payload.setAsset(new RepositoryAssetWebhookPayload.RepositoryAsset(
        assetId.getValue(),
        new RepositoryItemIDXO(repositoryName, assetId.getValue()).getValue(),
        format,
        asset.path()));
    return payload;
  }

  private RepositoryAssetWebhookPayload getPayload(final AssetPurgedEvent event, final EventAction eventAction) {
    Optional<Repository> repository = event.getRepository();
    String repositoryName = repository.map(Repository::getName).orElse(null);
    RepositoryAssetWebhookPayload payload = getPayload(repositoryName, eventAction);
    payload.setAssets(Arrays.stream(event.getAssetIds())
        .mapToObj(id -> InternalIds.toExternalId(id).getValue())
        .toArray(String[]::new));
    return payload;
  }

  static class RepositoryAssetWebhookPayload
      extends WebhookPayload
  {
    private final String repositoryName;

    private final EventAction action;

    private RepositoryAsset asset;

    private String[] assets;

    public RepositoryAssetWebhookPayload(
        final String nodeId,
        final Date timestamp,
        final String initiator,
        final String repositoryName,
        final EventAction action)
    {
      this.repositoryName = repositoryName;
      this.action = action;
      setNodeId(nodeId);
      setTimestamp(timestamp);
      setInitiator(initiator);
    }

    public String getRepositoryName() {
      return repositoryName;
    }

    public EventAction getAction() {
      return action;
    }

    public RepositoryAsset getAsset() {
      return asset;
    }

    public void setAsset(RepositoryAsset asset) {
      this.asset = asset;
    }

    public String[] getAssets() {
      return assets;
    }

    public void setAssets(String[] assets) {
      this.assets = assets;
    }

    public static class RepositoryAsset
    {
      private String id;

      private String assetId;

      private String format;

      private String name;

      public RepositoryAsset(String id, String assetId, String format, String name) {
        this.id = id;
        this.assetId = assetId;
        this.format = format;
        this.name = name;
      }

      public String getId() {
        return id;
      }

      public String getAssetId() {
        return assetId;
      }

      public String getFormat() {
        return format;
      }

      public String getName() {
        return name;
      }
    }
  }
}
