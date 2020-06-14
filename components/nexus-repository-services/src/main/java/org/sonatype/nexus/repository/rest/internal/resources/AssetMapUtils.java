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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;

import org.sonatype.nexus.repository.search.query.SearchUtils;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;

/**
 * Utility for working with asset maps that come out of Elastic
 *
 * @since 3.6.1
 */
@Named
@Singleton
class AssetMapUtils
{
  private static final String EMPTY_PARAM = "";

  private final SearchUtils searchUtils;

  @Inject
  public AssetMapUtils(final SearchUtils searchUtils) {
    this.searchUtils = checkNotNull(searchUtils);
  }

  /**
   * Get a value from an asset map that has come out of Elastic Search
   *
   * Out of Elastic comes a component map which contains an 'assets' entry, which itself is a map containing other
   * maps. In the REST API we have query parameters like 'assets.attributes.checksum.sha1'. Given the 'assets' map and
   * an identifier such as this, this method will attempt to pull the nested value out.
   *
   * @param assetMap the Map containing all the asset attributes
   * @param identifier an attribute identifier
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

  /**
   * The convention of providing a parameter without a value can be used
   * with the rest calls to filter out assets that have a value when it's not desired.
   * A scenario where this is applicable is with maven jar artifacts where one jar has
   * a classifier and one jar is without, yet they share the same GAV coordinates.
   *
   * For example, the following set of query parameters:
   *
   * <pre>maven.artifactId=foo&maven.baseVersion=2.7.3&maven.extension=jar&maven.classifier</pre>
   *
   * means search for and return assets that match on these values
   * artifactId=foo, baseVersion=2.7.3, extension=jar and no classifier defined.
   *
   * Alternatively, the following
   *
   * <pre>maven.artifactId=foo&maven.baseVersion=2.7.3&maven.extension=jar&maven.classifier=sources</pre>
   *
   * means search for and return assets that match on these values
   * artifactId=foo, baseVersion=2.7.3, extension=jar and classifier=sources
   *
   * @return boolean to indicate if the response should include (true) or exclude (false) this asset
   */
  @VisibleForTesting
  boolean filterAsset(final Map<String, Object> assetMap, final MultivaluedMap<String, String> assetParams) {
    // short circuit if the assetMap contains an assetAttribute found in the list of empty asset params
    if (excludeAsset(assetMap, getEmptyAssetParams(assetParams))) {
      return false;
    }

    // fetch only the set of assetParams that have values
    final Map<String, String> assetParamsWithValues = getNonEmptyAssetParams(assetParams);

    // if no asset parameters were sent, we'll count that as return all assets
    if (assetParamsWithValues.isEmpty()) {
      return true;
    }

    // loop each asset specific http query parameter to filter out assets that do not apply
    return assetParamsWithValues.entrySet().stream()
        .allMatch(entry -> keepAsset(assetMap, entry.getKey(), entry.getValue()));
  }

  /**
   * Checks the assetMap to see if it has values for any attribute in the paramFilters list. If 'true', this indicates
   * the asset should be excluded from the response.
   *
   * @return boolean indicating if the asset for which this method was called should be excluded from the response
   */
   static boolean excludeAsset(final Map<String, Object> assetMap, final List<String> paramFilters) {
    return paramFilters.stream()
        .anyMatch(filter -> getValueFromAssetMap(assetMap, filter).isPresent());
  }

  /**
   * Helper that simply checks to see if the provided param key is in the asset map and if so, does it's value
   * equal the param value.
   *
   * @return boolean indicating if the asset contains the param key and matches the provided param value
   */
   static boolean keepAsset(final Map<String, Object> assetMap, final String paramKey, final String paramValue) {
    return getValueFromAssetMap(assetMap, paramKey)
        .map(result -> result.equals(paramValue))
        .orElse(false);
  }

  /**
   * Traverses the query parameters to find any parameters without a corresponding
   * value.
   *
   * @return a list of parameter names that have empty values
   */
  @VisibleForTesting
  List<String> getEmptyAssetParams(final MultivaluedMap<String, String> assetParams) {
    return assetParams.entrySet()
        .stream()
        .filter(entry -> EMPTY_PARAM.equals(entry.getValue().get(0)))
        .map(e -> searchUtils.getFullAssetAttributeName(e.getKey()))
        .collect(toList());
  }

  /**
   * Traverses the query parameters to find only parameters with a value.
   *
   * @return a map of parameter names that have values
   */
  @VisibleForTesting
  Map<String, String> getNonEmptyAssetParams(final MultivaluedMap<String, String> assetParams) {
    return assetParams.entrySet()
        .stream()
        .filter(entry -> !EMPTY_PARAM.equals(entry.getValue().get(0)))
        .collect(Collectors
            .toMap(entry -> searchUtils.getFullAssetAttributeName(entry.getKey()), entry -> entry.getValue().get(0)));
  }
}
