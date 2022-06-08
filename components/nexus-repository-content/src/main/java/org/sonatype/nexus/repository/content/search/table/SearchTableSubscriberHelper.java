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
package org.sonatype.nexus.repository.content.search.table;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.common.collect.NestedAttributesMap;

public final class SearchTableSubscriberHelper
{
  /**
   * Method to extract custom attribute field 1 for given format from nestedAttributesMap {@link NestedAttributesMap}.
   * Method does nothing if format attributes is absent.
   *
   * @param format              component format
   * @param nestedAttributesMap {@link NestedAttributesMap}
   */
  public static String selectFormatField1(final String format, final NestedAttributesMap nestedAttributesMap) {
    Object formatAttributes = nestedAttributesMap.get(format);
    Map<String, String> attributes =
        formatAttributes instanceof Map ? (Map<String, String>) formatAttributes : new HashMap<>();
    switch (format) {
      case "maven2":
        return attributes.get("baseVersion");
      default: {
        return null;
      }
    }
  }

  /**
   * Method to extract custom attribute field 2 for given format from nestedAttributesMap {@link NestedAttributesMap}.
   * Method does nothing if format attributes is absent.
   *
   * @param format              component format
   * @param nestedAttributesMap {@link NestedAttributesMap}
   */
  public static String selectFormatField2(final String format, final NestedAttributesMap nestedAttributesMap) {
    Object formatAttributes = nestedAttributesMap.get(format);
    Map<String, String> attributes =
        formatAttributes instanceof Map ? (Map<String, String>) formatAttributes : new HashMap<>();
    switch (format) {
      case "maven2": {
        return attributes.get("extension");
      }
      default: {
        return null;
      }
    }
  }

  /**
   * Method to extract custom attribute field 3 for given format from nestedAttributesMap {@link NestedAttributesMap}.
   * Method does nothing if format attributes is absent.
   *
   * @param format              component format
   * @param nestedAttributesMap {@link NestedAttributesMap}
   */
  public static String selectFormatField3(final String format, final NestedAttributesMap nestedAttributesMap) {
    Object formatAttributes = nestedAttributesMap.get(format);
    Map<String, String> attributes =
        formatAttributes instanceof Map ? (Map<String, String>) formatAttributes : new HashMap<>();
    switch (format) {
      case "maven2": {
        return attributes.get("classifier");
      }
      default: {
        return null;
      }
    }
  }
}
