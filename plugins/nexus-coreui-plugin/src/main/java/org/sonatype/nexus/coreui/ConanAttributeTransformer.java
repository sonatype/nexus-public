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
package org.sonatype.nexus.coreui;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static java.util.Collections.emptyMap;

/**
 * This is used for transforming "infoBinary" attribute of a Conan asset.
 */
@Named(ConanAttributeTransformer.CONAN_FORMAT)
@Singleton
public class ConanAttributeTransformer
    extends ComponentSupport
    implements AssetAttributeTransformer
{
  static final String CONAN_FORMAT = "conan";

  static final String INFO_BINARY_ATTRIBUTE = "infoBinary";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final TypeReference<Map<String, Object>> MAP_STRING_OBJECT =
      new TypeReference<Map<String, Object>>() { };

  /**
   * Transforms the given {@link AssetXO} by expanding its "infoBinary"
   * attribute with a map containing the same data with flattened keys.
   *
   * @param assetXO the asset to be transformed
   */
  @Override
  public void transform(final AssetXO assetXO) {

    @SuppressWarnings("unchecked")
    Map<String, Object> formatAttributes = (Map<String, Object>) assetXO.getAttributes().getOrDefault(assetXO.getFormat(), emptyMap());

    if (formatAttributes.containsKey(INFO_BINARY_ATTRIBUTE)) {
      updateFormatAttributesMap(formatAttributes);
    }
  }

  private void updateFormatAttributesMap(Map<String, Object> formatAttributesMap) {
    String json = formatAttributesMap.get(INFO_BINARY_ATTRIBUTE).toString();
    try {
      formatAttributesMap.putAll(getBinaryInfoAsMap(json));
      formatAttributesMap.remove(INFO_BINARY_ATTRIBUTE);
    } catch (IllegalStateException e) {
      log.debug("Failed to parse {} as json", json);
    }
  }

  /**
   * Converts a JSON string into a flattened map.
   *
   * @param json the JSON string to be converted
   * @return a flattened map containing same data with flattened keys
   */
  private Map<String, Object> getBinaryInfoAsMap(final String json) {
    Map<String, Object> binaryInfo = deserialize(json, MAP_STRING_OBJECT);
    return flattenMap(binaryInfo, null);
  }

  /**
   * Flattens a nested map into a single-level map with dot-separated keys.
   *
   * @param map the nested map to be flattened
   * @param parentKey the parent key to be used as a prefix (can be null)
   * @return a flattened map
   */
  private Map<String, Object> flattenMap(final Map<String, Object> map, final String parentKey) {
    Map<String, Object> flattenedMap = new HashMap<>();
    for (Map.Entry<String, ?> entry : map.entrySet()) {
      String key = parentKey == null ? entry.getKey() : parentKey + "." + entry.getKey();
      if (entry.getValue() instanceof Map) {
        flattenedMap.putAll(flattenMap((Map<String, Object>) entry.getValue(), key));
      } else {
        flattenedMap.put(key, entry.getValue());
      }
    }
    return flattenedMap;
  }

  private <T> T deserialize(final String json, final TypeReference<T> type) {
    try {
      return OBJECT_MAPPER.readValue(json, type);
    }
    catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

}
