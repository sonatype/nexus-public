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
package org.sonatype.nexus.testsuite.support.filters;

import java.util.Map;

import org.sonatype.nexus.testsuite.support.Filter;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for map based filter replacement. Subclasses just need to provide the filtering map.
 *
 * @since 2.2
 */
public abstract class MapFilterSupport
    implements Filter
{

  /**
   * Filters using mapping provided by subclasses.
   *
   * @param context filtering context. Cannot be null.
   * @param value   value to be filtered. Cannot be null.
   * @return filtered value
   */
  public String filter(final Map<String, String> context, final String value) {
    String filtered = checkNotNull(value);
    // TODO use a matcher for ${*}?
    if (filtered.contains("${")) {
      final Map<String, String> mappings = mappings(context, value);
      if (mappings != null) {
        for (final Map.Entry<String, String> property : mappings.entrySet()) {
          filtered = filtered.replace("${" + property.getKey() + "}", property.getValue());
        }
      }
    }
    return filtered;
  }

  /**
   * Returns the mappings between placeholders and values that this filer supports.
   *
   * @param context filtering context. Never null.
   * @param value   value to be filtered. Cannot be null.
   * @return mappings. Should not be null.
   */
  abstract Map<String, String> mappings(final Map<String, String> context, final String value);

}
