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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Component;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;

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

  public static final String ATTRIBUTES = "attributes";

  public static final String CONTENT_TYPE = "content_type";

  public static final String FORMAT = "format";

  public static final String NAME = "name";

  public static final String GROUP = "group";

  public static final String REPOSITORY_NAME = "repository_name";

  public static final String VERSION = "version";

  public static final String ASSETS = "assets";

  @Override
  public String getMetadata(final Component component,
                            final Iterable<Asset> assets,
                            final Map<String, Object> additional)
  {
    checkNotNull(component);
    checkNotNull(assets);
    checkNotNull(additional);

    Map<String, Object> metadata = new HashMap<>();
    put(metadata, FORMAT, component.format());
    put(metadata, GROUP, component.group());
    put(metadata, NAME, component.name());
    put(metadata, VERSION, component.version());
    put(metadata, ATTRIBUTES, component.attributes().backing());

    List<Map<String, Object>> allAssetMetadata = new ArrayList<>();
    for (Asset asset : assets) {
      Map<String, Object> assetMetadata = new HashMap<>();
      put(assetMetadata, NAME, asset.name());
      put(assetMetadata, CONTENT_TYPE, asset.contentType());
      put(assetMetadata, ATTRIBUTES, asset.attributes().backing());

      allAssetMetadata.add(assetMetadata);
    }
    if (!allAssetMetadata.isEmpty()) {
      metadata.put(ASSETS, allAssetMetadata);
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
