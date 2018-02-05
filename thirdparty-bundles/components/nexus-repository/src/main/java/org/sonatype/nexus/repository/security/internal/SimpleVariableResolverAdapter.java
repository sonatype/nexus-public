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
package org.sonatype.nexus.repository.security.internal;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.selector.VariableSourceBuilder;

import com.orientechnologies.orient.core.record.impl.ODocument;
import org.elasticsearch.search.lookup.SourceLookup;

/**
 * Simple implementation that will expose the path/format variable resolvers
 *
 * @since 3.1
 */
@Named(SimpleVariableResolverAdapter.NAME)
@Singleton
public class SimpleVariableResolverAdapter
    extends VariableResolverAdapterSupport
    implements VariableResolverAdapter
{
  static final String NAME = "simple";

  @Override
  protected void addFromRequest(final VariableSourceBuilder builder, final Request request) {
    //no-op the simple impl just allows for the path/format variable resolvers in the support class
  }

  @Override
  protected void addFromDocument(final VariableSourceBuilder builder, final ODocument document) {
    //no-op the simple impl just allows for the path/format variable resolvers in the support class
  }

  @Override
  protected void addFromAsset(final VariableSourceBuilder builder, final Asset asset) {
    //no-op the simple impl just allows for the path/format variable resolvers in the support class
  }

  @Override
  protected void addFromSourceLookup(final VariableSourceBuilder builder,
                                     final SourceLookup sourceLookup,
                                     final Map<String, Object> asset)
  {
    //no-op the simple impl just allows for the path/format variable resolvers in the support class
  }
}
