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
package org.sonatype.nexus.repository.maven.internal;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.selector.VariableSourceBuilder;

import com.orientechnologies.orient.core.record.impl.ODocument;
import org.elasticsearch.search.lookup.SourceLookup;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maven2 implementation will expose the groupId/artifactId/version/extension/classifier attributes when available
 * @since 3.1
 */
@Named(Maven2Format.NAME)
public class MavenVariableResolverAdapter
    extends VariableResolverAdapterSupport
    implements VariableResolverAdapter
{
  private final MavenPathParser mavenPathParser;

  @Inject
  public MavenVariableResolverAdapter(@Named(Maven2Format.NAME) final MavenPathParser mavenPathParser) {
    this.mavenPathParser = checkNotNull(mavenPathParser);
  }

  @Override
  protected void addFromRequest(final VariableSourceBuilder builder, final Request request) {
    addMavenCoordinates(builder, request.getPath());
  }

  @Override
  protected void addFromDocument(final VariableSourceBuilder builder, final ODocument document) {
    addMavenCoordinates(builder, document.field(AssetEntityAdapter.P_NAME, String.class));
  }

  @Override
  protected void addFromAsset(final VariableSourceBuilder builder, final Asset asset) {
    addMavenCoordinates(builder, asset.name());
  }

  @Override
  protected void addFromSourceLookup(final VariableSourceBuilder builder,
                                     final SourceLookup sourceLookup,
                                     final Map<String, Object> asset)
  {
    addMavenCoordinates(builder, (String) asset.get(AssetEntityAdapter.P_NAME));
  }

  /**
   * Adds the Maven coordinates extracted from the specified path, if available.
   */
  private void addMavenCoordinates(final VariableSourceBuilder builder, final String path) {
    checkNotNull(builder);
    checkNotNull(path);
    Coordinates coords = mavenPathParser.parsePath(path).getCoordinates();

    if (coords != null) {
      Map<String, String> coordMap = new HashMap<>();
      coordMap.put("groupId", coords.getGroupId());
      coordMap.put("artifactId", coords.getArtifactId());
      coordMap.put("version", coords.getBaseVersion());
      coordMap.put("extension", coords.getExtension());
      coordMap.put("classifier", coords.getClassifier() == null ? "" : coords.getClassifier());

      addCoordinates(builder, coordMap);
    }
  }
}
