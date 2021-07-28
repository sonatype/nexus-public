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
package org.sonatype.nexus.repository.cocoapods;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.view.Content;

/**
 * @since 3.19
 */
@Facet.Exposed
public interface CocoapodsFacet
    extends Facet
{
  @Nullable
  Content get(final String assetPath);

  /**
   * Store Pod File Content
   */
  Content storePodFileContent(final String assetPath,
                              final Content content,
                              final String componentName,
                              final String componentVersion)
      throws IOException;

  /**
   * Store Spec File Content
   */
  Content storeSpecFileContent(final String assetPath,
                               final Content content,
                               @Nullable final Map<String, Object> formatAttributes)
      throws IOException;

  /**
   * Store Cdn Metadata Content
   */
  Content storeCdnMetadataContent(final String assetPath,
                                  final Content content)
      throws IOException;

  /**
   * Get Asset Format Attribute by Key
   */
  @Nullable
  String getAssetFormatAttribute(final String assetPath, final String attributeName);
}
