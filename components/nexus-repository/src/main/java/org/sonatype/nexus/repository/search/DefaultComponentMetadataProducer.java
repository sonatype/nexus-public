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
package org.sonatype.nexus.repository.search;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_CONTENT_TYPE;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_FORMAT;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_GROUP;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_NAME;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_VERSION;

/**
 * Default {@link ComponentMetadataProducer} implementation that uses all properties of a component & its assets as
 * metadata.
 *
 * @since 3.0
 */
@Named
@Singleton
public class DefaultComponentMetadataProducer
    implements ComponentMetadataProducer
{

  @Override
  public String getMetadata(final Component component, final Iterable<Asset> assets,
      final Map<String, Object> additional)
  {
    checkNotNull(component);

    Map<String, Object> metadata = Maps.newHashMap();
    put(metadata, P_FORMAT, component.format());
    put(metadata, P_GROUP, component.group());
    put(metadata, P_NAME, component.name());
    put(metadata, P_VERSION, component.version());
    put(metadata, P_ATTRIBUTES, component.attributes().backing());

    List<Map<String, Object>> allAssetMetadata = Lists.newArrayList();
    for (Asset asset : assets) {
      Map<String, Object> assetMetadata = Maps.newHashMap();
      put(assetMetadata, P_NAME, asset.name());
      put(assetMetadata, P_CONTENT_TYPE, asset.contentType());
      put(assetMetadata, P_ATTRIBUTES, asset.attributes().backing());

      allAssetMetadata.add(assetMetadata);
    }
    if (!allAssetMetadata.isEmpty()) {
      put(metadata, "assets", allAssetMetadata.toArray(new Map[allAssetMetadata.size()]));
    }

    metadata.putAll(additional);

    try {
      return JsonUtils.from(metadata);
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private static void put(final Map<String, Object> metadata, final String key, final Object value) {
    if (value != null) {
      metadata.put(key, value);
    }
  }

}
