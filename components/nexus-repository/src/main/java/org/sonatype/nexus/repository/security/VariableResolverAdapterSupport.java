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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.selector.ConstantVariableResolver;
import org.sonatype.nexus.selector.PropertiesResolver;
import org.sonatype.nexus.selector.VariableResolver;
import org.sonatype.nexus.selector.VariableSource;
import org.sonatype.nexus.selector.VariableSourceBuilder;

import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Adapts different contexts to variable resolvers
 *
 * @since 3.1
 */
public abstract class VariableResolverAdapterSupport
  implements VariableResolverAdapter
{
  private static final String PATH = "path";
  private static final String FORMAT = "format";

  @Override
  public VariableSource fromRequest(Request request, Repository repository) {
    Set<VariableResolver> variableResolvers = new HashSet<>();
    variableResolvers.add(new ConstantVariableResolver(request.getPath(), PATH));
    variableResolvers.add(new ConstantVariableResolver(repository.getFormat().getValue(), FORMAT));
    addFromRequest(variableResolvers, request);

    VariableSourceBuilder variableSourceBuilder = new VariableSourceBuilder();
    variableResolvers.forEach(variableSourceBuilder::addResolver);
    return variableSourceBuilder.build();
  }

  protected abstract void addFromRequest(Set<VariableResolver> variableResolvers, Request request);

  @Override
  public VariableSource fromDocument(ODocument document) {
    String path = document.field(AssetEntityAdapter.P_NAME, String.class);
    String format = document.field(AssetEntityAdapter.P_FORMAT, String.class);

    Set<VariableResolver> variableResolvers = new HashSet<>();
    variableResolvers.add(new ConstantVariableResolver(path, PATH));
    variableResolvers.add(new ConstantVariableResolver(format, FORMAT));
    addFromDocument(variableResolvers, document);

    VariableSourceBuilder variableSourceBuilder = new VariableSourceBuilder();
    variableResolvers.forEach(variableSourceBuilder::addResolver);
    return variableSourceBuilder.build();
  }

  protected abstract void addFromDocument(Set<VariableResolver> variableResolvers, ODocument document);

  @Override
  public VariableSource fromAsset(Asset asset) {
    Set<VariableResolver> variableResolvers = new HashSet<>();
    variableResolvers.add(new ConstantVariableResolver(asset.name(), PATH));
    variableResolvers.add(new ConstantVariableResolver(asset.format(), FORMAT));
    addFromAsset(variableResolvers, asset);

    VariableSourceBuilder variableSourceBuilder = new VariableSourceBuilder();
    variableResolvers.forEach(variableSourceBuilder::addResolver);
    return variableSourceBuilder.build();
  }

  protected abstract void addFromAsset(Set<VariableResolver> variableResolvers, Asset asset);

  protected void addCoordinates(Set<VariableResolver> resolvers, Map<String, String> coordinates) {
    resolvers.add(new PropertiesResolver<>("coordinate", coordinates));
  }
}
