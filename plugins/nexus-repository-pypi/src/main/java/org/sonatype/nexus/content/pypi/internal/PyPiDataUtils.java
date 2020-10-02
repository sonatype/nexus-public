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
package org.sonatype.nexus.content.pypi.internal;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAttributes;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.pypi.internal.PyPiFormat;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.SUPPORTED_ATTRIBUTES;

/**
 * @since 3.next
 */
public class PyPiDataUtils
{
  /**
   * Copies PyPI attributes from a map into the format attributes for the asset. We put almost all the format info on
   * the asset, not the component. While most should not differ between uploads for the same name and version, it is
   * possible, so mitigate by associating with assets.
   */
  static void copyFormatAttributes(final FluentAsset asset, final Map<String, String> attributes) {
    checkNotNull(asset);
    checkNotNull(attributes);

    Map<String, Object> formatAttributes = (Map<String, Object>) asset.attributes().get(PyPiFormat.NAME);
    if (formatAttributes == null) {
      formatAttributes = new HashMap<>();
    }

    for (String attribute : SUPPORTED_ATTRIBUTES) {
      String value = Strings.nullToEmpty(attributes.get(attribute)).trim();
      if (!value.isEmpty()) {
        formatAttributes.put(attribute, value);
      }
    }

    asset.withAttribute(PyPiFormat.NAME, formatAttributes);
  }

  static void copyFormatAttributes(final FluentComponent component, final Map<String, String> attributes) {
    checkNotNull(component);
    checkNotNull(attributes);

    Map<String, Object> formatAttributes = (Map<String, Object>) component.attributes().get(PyPiFormat.NAME);
    if (formatAttributes == null) {
      formatAttributes = new HashMap<>();
    }

    formatAttributes.putAll(attributes);
    component.withAttribute(PyPiFormat.NAME, formatAttributes);
  }

  static void setFormatAttribute(final FluentAsset asset, final String key, final Object value) {
    checkNotNull(asset);
    checkNotNull(key);
    checkNotNull(value);

    setFormatAttribute(asset, (Map<String, Object>) asset.attributes().get(PyPiFormat.NAME), key, value);
  }

  static void setFormatAttribute(FluentComponent component, String key, Object value) {
    checkNotNull(component);
    checkNotNull(key);
    checkNotNull(value);

    setFormatAttribute(component, (Map<String, Object>) component.attributes().get(PyPiFormat.NAME), key, value);
  }

  static void setFormatAttribute(final FluentAttributes fluentAttributes,
                                 @Nullable Map<String, Object> formatAttributes,
                                 final String key,
                                 final Object value)
  {
    if (formatAttributes == null) {
      formatAttributes = new HashMap<>();
    }
    formatAttributes.put(key, value);
    fluentAttributes.withAttribute(PyPiFormat.NAME, formatAttributes);
  }
}
