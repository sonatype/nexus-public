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
package org.sonatype.nexus.repository.content.store.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.blobstore.DefaultBlobIdLocationResolver;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.event.asset.AssetAttributesEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDownloadedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetKindEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPurgedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetRestoredEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUpdatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUploadedEvent;
import org.sonatype.nexus.repository.content.event.component.ComponentPurgedEvent;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static org.sonatype.nexus.common.app.FeatureFlags.ASSET_AUDITOR_ATTRIBUTE_CHANGES_ENABLED_VALUE;

/**
 * Repository asset auditor.
 *
 * @since 3.27
 */
@Component
public class AssetAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "repository.asset";

  private final boolean attributeChangesDetailEnabled;

  private final DefaultBlobIdLocationResolver defaultBlobIdLocationResolver;

  @Autowired
  public AssetAuditor(
      @Value(ASSET_AUDITOR_ATTRIBUTE_CHANGES_ENABLED_VALUE) final boolean attributeChangesDetailEnabled,
      final DefaultBlobIdLocationResolver defaultBlobIdLocationResolver)
  {
    registerType(AssetCreatedEvent.class, CREATED_TYPE);

    registerType(AssetDeletedEvent.class, DELETED_TYPE);

    registerType(AssetRestoredEvent.class, RESTORED_TYPE);

    registerType(AssetPurgedEvent.class, PURGE_TYPE);

    registerType(AssetUpdatedEvent.class, UPDATED_TYPE);
    registerType(AssetAttributesEvent.class, UPDATED_TYPE + "-attribute");
    registerType(AssetDownloadedEvent.class, UPDATED_TYPE + "-downloaded");
    registerType(AssetKindEvent.class, UPDATED_TYPE + "-kind");
    registerType(AssetUploadedEvent.class, UPDATED_TYPE + "-uploaded");

    this.attributeChangesDetailEnabled = attributeChangesDetailEnabled;
    this.defaultBlobIdLocationResolver = defaultBlobIdLocationResolver;

  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetPurgedEvent event) {
    if (isRecording()) {
      String repositoryName = event.getRepository().map(Repository::getName).orElse("Unknown");
      List<Asset> assets = event.getAssets();
      if (assets != null && !assets.isEmpty()) {
        for (Asset asset : assets) {
          record(buildAssetAuditData(asset, type(event.getClass()), repositoryName));
        }
      }
      else {
        AuditData data = new AuditData();
        data.setDomain(DOMAIN);
        data.setType(type(event.getClass()));
        data.setContext(repositoryName);
        Map<String, Object> attributes = data.getAttributes();
        attributes.put("repository.name", repositoryName);
        attributes.put("assetIds", Arrays.toString(event.getAssetIds()));
        record(data);
      }
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final ComponentPurgedEvent event) {
    if (isRecording()) {
      String repositoryName = event.getRepository().map(Repository::getName).orElse("Unknown");
      List<FluentAsset> assets = event.getAssets();
      if (assets != null && !assets.isEmpty()) {
        for (Asset asset : assets) {
          record(buildAssetAuditData(asset, PURGE_TYPE, repositoryName));
        }
      }
      else {
        AuditData data = new AuditData();
        data.setDomain(DOMAIN);
        data.setType(PURGE_TYPE);
        data.setContext(repositoryName);
        Map<String, Object> attributes = data.getAttributes();
        attributes.put("repository.name", repositoryName);
        attributes.put("componentIds", Arrays.toString(event.getComponentIds()));
        record(data);
      }
    }
  }

  private AuditData buildAssetAuditData(final Asset asset, final String eventType, final String repositoryName) {
    AuditData data = new AuditData();
    data.setDomain(DOMAIN);
    data.setType(eventType);
    data.setContext(asset.path());

    String blobStorePath = "unknown";
    String blobStoreName = "unknown";

    if (asset.blob().isPresent()) {
      AssetBlob blob = asset.blob().get();
      BlobId blobId = blob.blobRef().getBlobId();
      blobStoreName = blob.blobRef().getStore();
      blobStorePath = defaultBlobIdLocationResolver.getLocation(blobId);
    }
    else {
      log.debug("Asset {} has no blob; using placeholder blob path in audit record", asset.path());
    }

    Map<String, Object> attributes = data.getAttributes();
    attributes.put("repository.name", repositoryName);
    attributes.put("path", asset.path());
    attributes.put("kind", asset.kind());
    attributes.put("blob.path", blobStoreName + "/content/" + blobStorePath);

    return data;
  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetEvent event) {
    if (isRecording()) {
      Asset asset = event.getAsset();
      String repositoryName = event.getRepository().map(Repository::getName).orElse("Unknown");
      AuditData data = buildAssetAuditData(asset, type(event.getClass()), repositoryName);

      if (event instanceof AssetAttributesEvent && attributeChangesDetailEnabled) {
        AssetAttributesEvent attributesEvent = (AssetAttributesEvent) event;

        data.getAttributes()
            .put("attribute.changes", attributesEvent.getChanges()
                .stream()
                .map(change -> {
                  Map<String, Object> entry = new HashMap<>();
                  entry.put("operation", change.getOperation());
                  entry.put("key", change.getKey());
                  entry.put("value", change.getValue());
                  return entry;
                })
                .collect(Collectors.toList()));
      }

      record(data);
    }
  }
}
