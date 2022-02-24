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
package org.sonatype.nexus.repository.security;

import java.util.Map;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.search.AssetSearchResult;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.selector.VariableSource;

import org.elasticsearch.search.lookup.SourceLookup;

/**
 * Generate a variable source from a context, to be used for content selector evaluation
 * @since 3.1
 */
public interface VariableResolverAdapter
{
  VariableSource fromRequest(Request request, Repository repository);

  /**
   * Creates a {@link VariableSource} from an ES-indexed asset.
   */
  VariableSource fromSourceLookup(SourceLookup sourceLookup, Map<String, Object> asset);

  /**
   * @since 3.8
   */
  VariableSource fromCoordinates(String format, String path, Map<String, String> coordinates);

  /**
   * @since 3.18
   */
  VariableSource fromPath(String path, String format);

  VariableSource fromSearchResult(ComponentSearchResult component, AssetSearchResult asset);
}
