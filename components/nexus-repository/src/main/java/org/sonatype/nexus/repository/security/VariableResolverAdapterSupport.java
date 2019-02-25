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
import org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.selector.ConstantVariableResolver;
import org.sonatype.nexus.selector.PropertiesResolver;
import org.sonatype.nexus.selector.VariableSource;
import org.sonatype.nexus.selector.VariableSourceBuilder;

import com.orientechnologies.orient.core.record.impl.ODocument;
import org.elasticsearch.search.lookup.SourceLookup;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adapts different contexts to variable resolvers
 *
 * @since 3.1
 */
public abstract class VariableResolverAdapterSupport
  implements VariableResolverAdapter
{
  protected static final String PATH = "path";
  protected static final String FORMAT = "format";

  @Override
  public VariableSource fromRequest(final Request request, final Repository repository) {
    VariableSourceBuilder builder = new VariableSourceBuilder();
    builder.addResolver(new ConstantVariableResolver(request.getPath().substring(1), PATH));
    builder.addResolver(new ConstantVariableResolver(repository.getFormat().getValue(), FORMAT));
    addFromRequest(builder, request);

    return builder.build();
  }

  protected abstract void addFromRequest(VariableSourceBuilder builder, Request request);

  @Override
  public VariableSource fromDocument(final ODocument document) {
    String path = document.field(AssetEntityAdapter.P_NAME, String.class);
    String format = document.field(AssetEntityAdapter.P_FORMAT, String.class);

    VariableSourceBuilder builder = new VariableSourceBuilder();
    builder.addResolver(new ConstantVariableResolver(path, PATH));
    builder.addResolver(new ConstantVariableResolver(format, FORMAT));
    addFromDocument(builder, document);

    return builder.build();
  }

  protected abstract void addFromDocument(VariableSourceBuilder builder, ODocument document);

  @Override
  public VariableSource fromAsset(final Asset asset) {
    VariableSourceBuilder builder = new VariableSourceBuilder();
    builder.addResolver(new ConstantVariableResolver(asset.name(), PATH));
    builder.addResolver(new ConstantVariableResolver(asset.format(), FORMAT));
    addFromAsset(builder, asset);

    return builder.build();
  }

  protected abstract void addFromAsset(VariableSourceBuilder builder, Asset asset);

  @Override
  public VariableSource fromCoordinates(final String format, final String path, final Map<String, String> coordinates) {
    VariableSourceBuilder builder = new VariableSourceBuilder();
    builder.addResolver(new ConstantVariableResolver(checkNotNull(path), PATH));
    builder.addResolver(new ConstantVariableResolver(checkNotNull(format), FORMAT));

    addCoordinates(builder, coordinates);
    return builder.build();
  }

  @Override
  public VariableSource fromSourceLookup(final SourceLookup sourceLookup, final Map<String, Object> asset) {
    VariableSourceBuilder builder = new VariableSourceBuilder();
    builder.addResolver(
        new ConstantVariableResolver(checkNotNull((String) asset.get(DefaultComponentMetadataProducer.NAME)), PATH));
    builder.addResolver(
        new ConstantVariableResolver(checkNotNull(sourceLookup.get(DefaultComponentMetadataProducer.FORMAT)), FORMAT));
    addFromSourceLookup(builder, sourceLookup, asset);

    return builder.build();
  }

  protected abstract void addFromSourceLookup(VariableSourceBuilder builder,
                                              SourceLookup sourceLookup,
                                              Map<String, Object> asset);

  protected void addCoordinates(final VariableSourceBuilder builder, final Map<String, String> coordinates) {
    builder.addResolver(new PropertiesResolver<>("coordinate", coordinates));
  }
}
