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
package org.sonatype.nexus.repository.storage;

import org.sonatype.nexus.repository.security.VariableResolverAdapterSupport;
import org.sonatype.nexus.selector.ConstantVariableResolver;
import org.sonatype.nexus.selector.VariableSource;
import org.sonatype.nexus.selector.VariableSourceBuilder;

import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Adapts persisted assets to variable resolvers.
 *
 * @since 3.22
 */
public abstract class AssetVariableResolverSupport
    extends VariableResolverAdapterSupport
    implements AssetVariableResolver
{
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

  public VariableSource fromAsset(final Asset asset) {
    VariableSourceBuilder builder = new VariableSourceBuilder();
    builder.addResolver(new ConstantVariableResolver(asset.name(), PATH));
    builder.addResolver(new ConstantVariableResolver(asset.format(), FORMAT));
    addFromAsset(builder, asset);

    return builder.build();
  }

  protected abstract void addFromAsset(VariableSourceBuilder builder, Asset asset);
}
