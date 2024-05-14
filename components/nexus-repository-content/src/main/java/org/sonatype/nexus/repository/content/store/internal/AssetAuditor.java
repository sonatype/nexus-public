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
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.audit.AuditData;
import org.sonatype.nexus.audit.AuditorSupport;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.event.asset.AssetAttributesEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetCreatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDeletedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetDownloadedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetKindEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetPurgedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUpdatedEvent;
import org.sonatype.nexus.repository.content.event.asset.AssetUploadedEvent;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static org.sonatype.nexus.common.app.FeatureFlags.ASSET_AUDITOR_ATTRIBUTE_CHANGES_ENABLED_NAMED;

/**
 * Repository asset auditor.
 *
 * @since 3.27
 */
@Named
@Singleton
public class AssetAuditor
    extends AuditorSupport
    implements EventAware
{
  public static final String DOMAIN = "repository.asset";

  private final boolean attributeChangesDetailEnabled;

  @Inject
  public AssetAuditor(@Named(ASSET_AUDITOR_ATTRIBUTE_CHANGES_ENABLED_NAMED) boolean attributeChangesDetailEnabled) {
    registerType(AssetCreatedEvent.class, CREATED_TYPE);

    registerType(AssetDeletedEvent.class, DELETED_TYPE);

    registerType(AssetPurgedEvent.class, PURGE_TYPE);

    registerType(AssetUpdatedEvent.class, UPDATED_TYPE);
    registerType(AssetAttributesEvent.class, UPDATED_TYPE + "-attribute");
    registerType(AssetDownloadedEvent.class, UPDATED_TYPE + "-downloaded");
    registerType(AssetKindEvent.class, UPDATED_TYPE + "-kind");
    registerType(AssetUploadedEvent.class, UPDATED_TYPE + "-uploaded");

    this.attributeChangesDetailEnabled = attributeChangesDetailEnabled;

  }

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetPurgedEvent event) {
    if (isRecording()) {
      String repositoryName =  event.getRepository().map(Repository::getName).orElse("Unknown");

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

  @Subscribe
  @AllowConcurrentEvents
  public void on(final AssetEvent event) {
    if (isRecording()) {
      Asset asset = event.getAsset();

      AuditData data = new AuditData();
      data.setDomain(DOMAIN);
      data.setType(type(event.getClass()));
      data.setContext(asset.path());

      Map<String, Object> attributes = data.getAttributes();
      attributes.put("repository.name", event.getRepository().map(Repository::getName).orElse("Unknown"));
      attributes.put("path", asset.path());
      attributes.put("kind", asset.kind());

      if (event instanceof AssetAttributesEvent && attributeChangesDetailEnabled){
        AssetAttributesEvent attributesEvent = (AssetAttributesEvent) event;

        attributes.put("attribute.changes", attributesEvent.getChanges()
            .stream().map(change -> {
              Map<String, Object> entry = new HashMap<>();
              entry.put("operation", change.getOperation());
              entry.put("key", change.getKey());
              entry.put("value", change.getValue());
              return entry;
            }).collect(Collectors.toList()));
      }

      record(data);
    }
  }
}
