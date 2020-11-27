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
package org.sonatype.nexus.repository.content.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.sonatype.nexus.repository.content.RepositoryContent;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAttributes;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;

/**
 * @since 3.29
 */
public class FormatAttributesUtils
{
  public static void setFormatAttributes(final FluentAsset fluent, final Map<String, Object> values) {
    setFormatAttributes(fluent.repository().getFormat().getValue(), fluent, fluent, values);
  }

  public static void setFormatAttributes(final FluentAsset fluent, final Supplier<Map<String, Object>> supplier) {
    setFormatAttributes(fluent, supplier.get());
  }

  public static void setFormatAttributes(final FluentAsset fluent, final String key, final Object value) {
    setFormatAttributes(
        fluent,
        new HashMap<String, Object>()
        {{
          put(key, value);
        }}
    );
  }

  public static void setFormatAttributes(final FluentComponent fluent, final Map<String, Object> values) {
    setFormatAttributes(fluent.repository().getFormat().getValue(), fluent, fluent, values);
  }

  private static void setFormatAttributes(
      final String formatName,
      final RepositoryContent repositoryContent,
      final FluentAttributes attributes,
      final Map<String, Object> values)
  {
    @SuppressWarnings("unchecked")
    Map<String, Object> formatAttributes = (Map<String, Object>) repositoryContent
        .attributes()
        .get(formatName, new HashMap<>());
    formatAttributes.putAll(values);
    attributes.withAttribute(formatName, formatAttributes);
  }
}
