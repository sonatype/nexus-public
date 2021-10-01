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
package org.sonatype.nexus.repository.content.replication;

import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.repository.content.AttributeChangeSet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssetBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponentBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;

import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;
import static org.sonatype.nexus.repository.content.AttributeOperation.SET;
import static org.sonatype.nexus.repository.replication.ReplicationUtils.getChecksumsFromProperties;

/**
 * Standard methods for replicating components/assets.
 *
 * @since 3.35
 */
@FeatureFlag(name = DATASTORE_ENABLED)
@Named
@Singleton
public class ReplicationIngestionService
    extends ComponentSupport
{
  public FluentComponent replicateComponent(
      final FluentComponents fluentComponents,
      final Map<String, Object> componentAttributes,
      final String name,
      final String version)
  {
    return replicateComponent(fluentComponents, componentAttributes, name, version, null);
  }

  public FluentComponent replicateComponent(
      final FluentComponents fluentComponents,
      final Map<String, Object> componentAttributes,
      final String name,
      final String version,
      @Nullable final String namespace)
  {
    FluentComponentBuilder componentBuilder = fluentComponents.name(name).version(version);

    if (namespace != null) {
      componentBuilder = componentBuilder.namespace(namespace);
    }

    for (Map.Entry<String, Object> attr : componentAttributes.entrySet()) {
      componentBuilder.attributes(attr.getKey(), attr.getValue());
    }

    return componentBuilder.getOrCreate();
  }

  public void replicateAsset(
      final FluentAssets fluentAssets,
      final String path,
      final Blob blob,
      final Map<String, Object> assetAttributes,
      String kind)
  {
    replicateAsset(fluentAssets, path, blob, assetAttributes, kind, null);
  }

  public void replicateAsset(
      final FluentAssets fluentAssets,
      final String path,
      final Blob blob,
      final Map<String, Object> assetAttributes,
      final String kind,
      @Nullable final FluentComponent component)
  {
    FluentAssetBuilder fluentAssetBuilder =
        fluentAssets.path(path).kind(kind).blob(blob, getChecksumsFromProperties(assetAttributes));

    if (component != null) {
      fluentAssetBuilder = fluentAssetBuilder.component(component);
    }

    FluentAsset asset = fluentAssetBuilder.save();

    AttributeChangeSet changeSet = new AttributeChangeSet();
    assetAttributes.forEach((key, value) -> changeSet.attributes(SET, key, value));
    asset.attributes(changeSet);
  }
}
