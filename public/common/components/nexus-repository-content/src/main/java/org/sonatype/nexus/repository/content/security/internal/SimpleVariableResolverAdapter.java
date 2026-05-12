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
package org.sonatype.nexus.repository.content.security.internal;

import java.util.Map;

import jakarta.inject.Singleton;

import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.security.AssetVariableResolverSupport;
import org.sonatype.nexus.repository.search.AssetSearchResult;
import org.sonatype.nexus.repository.search.ComponentSearchResult;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.selector.VariableSourceBuilder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Provides only 'path' and 'format' variables for content selector evaluation.
 * This is the correct approach for all repository formats.
 *
 * <p>
 * <b>Important:</b> Do NOT create format-specific VariableResolverAdapters that parse coordinates.
 * CSEL only supports path and format variables. Coordinate-based selectors were removed in NEXUS-27129.
 *
 * <p>
 * <b>Usage:</b> New formats should inject this using {@code @Qualifier("simple")}.
 *
 * <p>
 * <b>Maven/NuGet Exception:</b> MavenVariableResolverAdapter and NugetVariableResolverAdapter exist
 * only for backward compatibility with pre-2021 JEXL selectors. Do not copy this pattern.
 */
@Component
@Qualifier("simple")
@Singleton
public class SimpleVariableResolverAdapter
    extends AssetVariableResolverSupport
{
  @Override
  protected void addFromRequest(final VariableSourceBuilder builder, final Request request) {
    // no-op the simple impl just allows for the path/format variable resolvers in the support class
  }

  @Override
  protected void addFromSourceLookup(final VariableSourceBuilder builder, final Map<String, Object> asset) {
    // no-op the simple impl just allows for the path/format variable resolvers in the support class
  }

  @Override
  protected void addFromSearchResults(
      final VariableSourceBuilder builder,
      final ComponentSearchResult component,
      final AssetSearchResult asset)
  {
    // no-op the simple impl just allows for the path/format variable resolvers in the support class
  }

  @Override
  protected void addFromAsset(final VariableSourceBuilder builder, final FluentAsset asset) {
    // no-op the simple impl just allows for the path/format variable resolvers in the support class
  }
}
