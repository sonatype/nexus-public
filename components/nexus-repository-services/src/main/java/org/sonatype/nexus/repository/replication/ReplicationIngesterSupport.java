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
package org.sonatype.nexus.repository.replication;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.sonatype.goodies.common.ComponentSupport;

import org.joda.time.DateTime;

/**
 * @since 3.31
 */
public abstract class ReplicationIngesterSupport
    extends ComponentSupport
    implements ReplicationIngester
{
  public Map<String, Object> extractAssetAttributesFromProperties(final Properties props) {
    return extractAttributesFromProperties(props, ASSET_ATTRIBUTES_PREFIX);
  }

  public Map<String, Object> extractComponentAttributesFromProperties(final Properties props) {
    return extractAttributesFromProperties(props, COMPONENT_ATTRIBUTES_PREFIX);
  }

  private Map<String, Object> extractAttributesFromProperties(Properties props, final String prefix) {
    Map<String, Object> backingAssetAttributes = new HashMap<>();
    Set<String> keys = props.stringPropertyNames();
    for (String key : keys) {
      if (key.startsWith(prefix)) {
        String value = props.getProperty(key);
        key = key.substring(prefix.length());
        String[] flattenedAttributesParts = key.split("\\.");
        key = flattenedAttributesParts[0];

        unflattenAttributes(backingAssetAttributes, flattenedAttributesParts[0],
            Arrays.copyOfRange(flattenedAttributesParts, 1, flattenedAttributesParts.length),
            convertAttributeValue(key, value));
      }
    }
    return backingAssetAttributes;
  }

  /**
   * Recursively unflattens a dot separated String in a Map
   */
  protected void unflattenAttributes(Map<String, Object> backing, String root, String[] children, Object value) {
    if (children.length > 1) {
      if (backing.containsKey(root)) {
        unflattenAttributes((Map<String, Object>) backing.get(root), children[0],
            Arrays.copyOfRange(children, 1, children.length), value);
      }
      else {
        Map<String, Object> rootMap = new HashMap<>();
        backing.put(root, rootMap);
        unflattenAttributes(rootMap, children[0], Arrays.copyOfRange(children, 1, children.length), value);
      }
    }
    else {
      if (backing.containsKey(root)) {
        ((Map<String, Object>) backing.get(root)).put(children[0], value);
      }
      else {
        Map<String, Object> newEntry = new HashMap<>();
        newEntry.put(children[0], value);
        backing.put(root, newEntry);
      }
    }
  }

  protected Object convertAttributeValue(final String key, final String value) {
    if (value == null) {
      return null;
    }

    if (value.startsWith(VALUE_DATE_PREFIX)) {
      return new Date(Long.parseLong(value.substring(VALUE_DATE_PREFIX.length())));
    }
    else if (value.startsWith(VALUE_JODA_DATE_TIME_PREFIX)) {
      return new DateTime(Long.parseLong(value.substring(VALUE_JODA_DATE_TIME_PREFIX.length())));
    }
    else if (value.startsWith(VALUE_DATE_TIME_PREFIX)) {
      return new Date(Long.parseLong(value.substring(VALUE_DATE_TIME_PREFIX.length()))).toInstant().atOffset(
          ZoneOffset.UTC);
    }

    return value;
  }
}
