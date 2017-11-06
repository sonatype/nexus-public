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
package org.sonatype.nexus.repository.browse.internal.resources;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;

/**
 * Utility for working with asset maps that come out of Elastic
 *
 * @since 3.7
 */
class AssetMapUtils
{
  private AssetMapUtils() {
    // empty
  }

  /**
   * Get a value from an asset map that has come out of Elastic Search
   *
   * Out of Elastic comes a component map which contains an 'assets' entry, which itself is a map containing other
   * maps. In the REST API we have query parameters like 'assets.attributes.checksum.sha1'. Given the 'assets' map and
   * an identifier such as this, this method will attempt to pull the nested value out.
   *
   * @param assetMap the Map containing all the asset attributes
   * @param identifier a value from {@link SearchResource#ASSET_SEARCH_PARAMS}
   * @return Value, if found
   */
  @SuppressWarnings("unchecked")
  static Optional<Object> getValueFromAssetMap(final Map<String, Object> assetMap, final String identifier) {
    if (isNullOrEmpty(identifier) || assetMap.isEmpty()) {
      return Optional.empty();
    }

    List<String> keys = newArrayList(identifier.split("\\."));

    if ("assets".equals(keys.get(0))) {
      keys.remove(0);
    }

    Object value = assetMap;
    for (String key : keys) {
      if (value == null) {
        return Optional.empty();
      }
      value = ((Map<String, Object>) value).get(key);
    }
    return Optional.ofNullable(value);
  }
}
