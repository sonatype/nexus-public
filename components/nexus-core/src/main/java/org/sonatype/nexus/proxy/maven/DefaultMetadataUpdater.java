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
package org.sonatype.nexus.proxy.maven;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.maven.metadata.operations.AddPluginOperation;
import org.sonatype.nexus.proxy.maven.metadata.operations.AddVersionOperation;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataBuilder;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataException;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataOperation;
import org.sonatype.nexus.proxy.maven.metadata.operations.ModelVersionUtility;
import org.sonatype.nexus.proxy.maven.metadata.operations.PluginOperand;
import org.sonatype.nexus.proxy.maven.metadata.operations.SetSnapshotOperation;
import org.sonatype.nexus.proxy.maven.metadata.operations.SnapshotOperand;
import org.sonatype.nexus.proxy.maven.metadata.operations.StringOperand;
import org.sonatype.nexus.proxy.maven.metadata.operations.TimeUtil;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.annotations.VisibleForTesting;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.codehaus.plexus.util.StringUtils;

@Named
@Singleton
public class DefaultMetadataUpdater
    extends ComponentSupport
    implements MetadataUpdater
{
  private final MetadataLocator locator;

  @Inject
  public DefaultMetadataUpdater(MetadataLocator locator) {
    this.locator = locator;
  }

  public void deployArtifact(ArtifactStoreRequest request)
      throws IOException
  {
    if (!doesImpactMavenMetadata(request.getGav())) {
      return;
    }

    try {
      List<MetadataOperation> operations = null;

      Gav gav = locator.getGavForRequest(request);

      // GAV

      Metadata gavMd = locator.retrieveGAVMetadata(request);

      operations = new ArrayList<MetadataOperation>();

      // simply making it foolproof?
      gavMd.setGroupId(gav.getGroupId());

      gavMd.setArtifactId(gav.getArtifactId());

      gavMd.setVersion(gav.getBaseVersion());

      // GAV metadata is only meaningful to snapshot artifacts
      if (gav.isSnapshot()) {
        operations.add(new SetSnapshotOperation(new SnapshotOperand(
            ModelVersionUtility.getModelVersion(gavMd), TimeUtil.getUTCTimestamp(),
            MetadataBuilder.createSnapshot(request.getVersion()), buildVersioning(gav))));

        MetadataBuilder.changeMetadata(gavMd, operations);

        locator.storeGAVMetadata(request, gavMd);
      }

      // GA

      operations = new ArrayList<MetadataOperation>();

      Metadata gaMd = locator.retrieveGAMetadata(request);

      operations.add(new AddVersionOperation(new StringOperand(ModelVersionUtility.getModelVersion(gaMd),
          gav.getBaseVersion())));

      MetadataBuilder.changeMetadata(gaMd, operations);

      locator.storeGAMetadata(request, gaMd);

      // G (if is plugin)

      operations = new ArrayList<MetadataOperation>();

      if (StringUtils.equals("maven-plugin", locator.retrievePackagingFromPom(request))) {
        Metadata gMd = locator.retrieveGMetadata(request);

        Plugin pluginElem = locator.extractPluginElementFromPom(request);

        if (pluginElem != null) {
          operations.add(new AddPluginOperation(new PluginOperand(
              ModelVersionUtility.getModelVersion(gMd), pluginElem)));

          MetadataBuilder.changeMetadata(gMd, operations);

          locator.storeGMetadata(request, gMd);
        }

      }
    }
    catch (MetadataException e) {
      throw new LocalStorageException("Not able to apply changes!", e);
    }
  }

  @VisibleForTesting
  boolean doesImpactMavenMetadata(final Gav requestGav) {
    // hashes and signatures are "meta"
    // released artifacts with classifiers do not change metadata
    return !(requestGav.isHash() || requestGav.isSignature() ||
        (StringUtils.isNotBlank(requestGav.getClassifier()) && !requestGav.isSnapshot()));
  }

  private SnapshotVersion buildVersioning(Gav gav) {
    SnapshotVersion version = new SnapshotVersion();
    version.setClassifier(gav.getClassifier());
    version.setExtension(gav.getExtension());
    version.setVersion(gav.getVersion());
    version.setUpdated(TimeUtil.getUTCTimestamp());
    return version;
  }

}
