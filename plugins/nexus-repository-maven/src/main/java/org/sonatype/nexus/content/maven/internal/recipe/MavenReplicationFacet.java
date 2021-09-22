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
package org.sonatype.nexus.content.maven.internal.recipe;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.content.AttributeChangeSet;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.replication.ReplicationFacetSupport;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPathParser;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.content.AttributeOperation.SET;
import static org.sonatype.nexus.repository.maven.internal.Maven2Format.NAME;
import static org.sonatype.nexus.repository.replication.ReplicationUtils.getChecksumsFromProperties;

/**
 * A {@link ReplicationFacet} for maven repositories.
 *
 * @since 3.31
 */
@Named(NAME)
public class MavenReplicationFacet
    extends ReplicationFacetSupport
{
  private final Map<String, MavenPathParser> mavenPathParsers;

  private MavenPathParser mavenPathParser;

  @Inject
  public MavenReplicationFacet(final Map<String, MavenPathParser> mavenPathParsers)
  {
    this.mavenPathParsers = checkNotNull(mavenPathParsers);
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    mavenPathParser = checkNotNull(mavenPathParsers.get(getRepository().getFormat().getValue()));
  }

  @Override
  public void doReplicate(final String path,
                        final Blob blob,
                        final Map<String, Object> assetAttributes,
                        final Map<String, Object> componentAttributes)
  {
    ContentFacet contentFacet = facet(ContentFacet.class);
    MavenPath mavenPath = mavenPathParser.parsePath(path);

    if (mavenPath.getCoordinates() != null) {
      replicateAssetWithComponent(contentFacet, path, mavenPath, blob, assetAttributes, componentAttributes);
    }
    else {
      replicateAssetWithoutComponent(contentFacet, path, blob, assetAttributes);
    }
  }

  private void replicateAssetWithComponent(final ContentFacet contentFacet,
                                           final String path,
                                           final MavenPath mavenPath,
                                           final Blob blob,
                                           final Map<String, Object> assetAttributes,
                                           final Map<String, Object> componentAttributes) {
    FluentComponent fluentComponent = contentFacet.components()
      .name(mavenPath.getCoordinates().getArtifactId())
      .namespace(mavenPath.getCoordinates().getGroupId())
      .version(mavenPath.getCoordinates().getVersion())
      .getOrCreate();
    FluentAsset fluentAsset = contentFacet.assets().path(path)
      .component(fluentComponent)
      .blob(blob, getChecksumsFromProperties(assetAttributes))
      .save();

    AttributeChangeSet changeSet = new AttributeChangeSet();
    for (Map.Entry<String, Object> entry : assetAttributes.entrySet()) {
      changeSet.attributes(SET, entry.getKey(), entry.getValue());
    }
    fluentAsset.attributes(changeSet);

    for (Map.Entry<String, Object> entry : componentAttributes.entrySet()) {
      fluentComponent.attributes(SET, entry.getKey(), entry.getValue());
    }
  }

  private void replicateAssetWithoutComponent(final ContentFacet contentFacet,
                                              final String path,
                                              final Blob blob,
                                              final Map<String, Object> assetAttributes) {
    FluentAsset fluentAsset = contentFacet.assets().path(path)
      .blob(blob, getChecksumsFromProperties(assetAttributes))
      .save();

    AttributeChangeSet changeSet = new AttributeChangeSet();
    for (Map.Entry<String, Object> entry : assetAttributes.entrySet()) {
      changeSet.attributes(SET, entry.getKey(), entry.getValue());
    }
    fluentAsset.attributes(changeSet);
  }
}
