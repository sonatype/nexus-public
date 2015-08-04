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
package org.sonatype.nexus.proxy.maven.metadata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.maven.gav.M2GavCalculator;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataBuilder;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataException;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataOperation;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataUtil;
import org.sonatype.nexus.proxy.maven.metadata.operations.ModelVersionUtility;
import org.sonatype.nexus.proxy.maven.metadata.operations.SetSnapshotOperation;
import org.sonatype.nexus.proxy.maven.metadata.operations.SnapshotOperand;
import org.sonatype.nexus.proxy.maven.metadata.operations.TimeUtil;

import com.google.common.collect.Maps;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.codehaus.plexus.util.StringUtils;

/**
 * Process maven metadata in snapshot version directory
 *
 * @author juven
 */
public class VersionDirMetadataProcessor
    extends AbstractMetadataProcessor
{

  public VersionDirMetadataProcessor(AbstractMetadataHelper metadataHelper) {
    super(metadataHelper);
  }

  @Override
  public boolean shouldProcessMetadata(String path) {
    Collection<String> names = metadataHelper.gavData.get(path);

    if (names != null && !names.isEmpty()) {
      return true;
    }

    return false;
  }

  @Override
  public void processMetadata(final String path, final Metadata oldMd)
      throws IOException
  {
    final Metadata md = createMetadata(path);

    // NEXUS-4766
    // To avoid wrong guessing of classifier / extension in case of classifier with dots,
    // read existing metadata (assumed to be okay due to the fact that was uploaded by Maven)
    // and for snapshot versions replace classifier / extension
    maybeFixClassifierAndExtension(md, oldMd);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    MetadataBuilder.write(md, outputStream);

    String mdString = outputStream.toString();

    outputStream.close();

    metadataHelper.store(mdString, path + AbstractMetadataHelper.METADATA_SUFFIX);
  }

  private void maybeFixClassifierAndExtension(final Metadata mdNew, final Metadata mdOld) {
    if (mdNew != null && mdOld != null) {
      final Map<String, SnapshotVersion> svOld = createSnapshotVersionMap(mdOld);
      if (!svOld.isEmpty()) {
        final Map<String, SnapshotVersion> svNew = createSnapshotVersionMap(mdNew);
        for (final Map.Entry<String, SnapshotVersion> entry : svNew.entrySet()) {
          final SnapshotVersion vOld = svOld.get(entry.getKey());
          if (vOld != null) {
            final SnapshotVersion vNew = entry.getValue();
            vNew.setClassifier(vOld.getClassifier());
            vNew.setExtension(vOld.getExtension());
          }
        }
      }
    }
  }

  private Map<String, SnapshotVersion> createSnapshotVersionMap(final Metadata md) {
    final Map<String, SnapshotVersion> sv = Maps.newHashMap();
    if (md.getVersioning() != null && md.getVersioning().getSnapshotVersions() != null) {
      for (final SnapshotVersion v : md.getVersioning().getSnapshotVersions()) {
        if (v.getClassifier() != null) {
          sv.put(String.format("%s-%s.%s", v.getVersion(), v.getClassifier(), v.getExtension()), v);
        }
      }
    }
    return sv;
  }

  private Metadata createMetadata(String path)
      throws IOException
  {
    try {
      Metadata md = new Metadata();

      md.setGroupId(calculateGroupId(path));

      md.setArtifactId(calculateArtifactId(path));

      md.setVersion(calculateVersion(path));

      ModelVersionUtility.setModelVersion(md, ModelVersionUtility.LATEST_MODEL_VERSION);

      versioning(md, getGavs(path, metadataHelper.gavData.get(path)));

      return md;
    }
    catch (MetadataException e) {
      throw new IOException(e);
    }
  }

  private Collection<Gav> getGavs(String path, Collection<String> items) {
    if (!path.endsWith("/")) {
      path = path + "/";
    }
    M2GavCalculator calc = new M2GavCalculator();

    List<Gav> gavs = new ArrayList<Gav>();
    Collections.sort((ArrayList<String>) items);
    for (String item : items) {
      final Gav gav = calc.pathToGav(path + item);
      if (gav != null) {
        gavs.add(gav);
      }
    }

    return gavs;
  }

  private String calculateGroupId(String path) {
    String gaPath = path.substring(0, path.lastIndexOf('/'));

    return gaPath.substring(1, gaPath.lastIndexOf('/')).replace('/', '.');
  }

  private String calculateArtifactId(String path) {
    String gaPath = path.substring(0, path.lastIndexOf('/'));

    return gaPath.substring(gaPath.lastIndexOf('/') + 1);
  }

  private String calculateVersion(String path) {
    return path.substring(path.lastIndexOf('/') + 1);
  }

  void versioning(Metadata metadata, Collection<Gav> artifactNames)
      throws MetadataException
  {
    List<MetadataOperation> ops = new ArrayList<MetadataOperation>();

    for (Gav gav : artifactNames) {
      ops.add(new SetSnapshotOperation(new SnapshotOperand(ModelVersionUtility.LATEST_MODEL_VERSION,
          TimeUtil.getUTCTimestamp(), buildSnapshot(gav), buildVersion(gav))));
    }

    MetadataBuilder.changeMetadata(metadata, ops);
  }

  private SnapshotVersion[] buildVersion(Gav gav)
      throws MetadataException
  {
    if (gav.getBaseVersion().equals(gav.getVersion())) {
      return new SnapshotVersion[0];
    }

    SnapshotVersion snap = new SnapshotVersion();
    snap.setClassifier(gav.getClassifier());
    snap.setExtension(gav.getExtension());
    snap.setVersion(gav.getVersion());

    Snapshot timestamp = buildSnapshot(gav);
    if (timestamp != null) {
      snap.setUpdated(timestamp.getTimestamp().replace(".", ""));
    }
    else {
      snap.setUpdated(TimeUtil.getUTCTimestamp().replace(".", ""));
    }
    return new SnapshotVersion[]{snap};
  }

  private Snapshot buildSnapshot(Gav gav) {
    Snapshot result = new Snapshot();

    final String version = gav.getVersion();

    if (version.equals(gav.getBaseVersion())) {
      return null;
    }

    int lastHyphenPos = version.lastIndexOf('-');

    int buildNumber = Integer.parseInt(version.substring(lastHyphenPos + 1));

    String timestamp = version.substring(gav.getBaseVersion().length() - 8, lastHyphenPos);

    result.setLocalCopy(false);

    result.setBuildNumber(buildNumber);

    result.setTimestamp(timestamp);

    return result;
  }

  @Override
  public void postProcessMetadata(String path) {
    metadataHelper.gavData.remove(path);
  }

  @Override
  protected boolean isMetadataCorrect(Metadata oldMd, String path)
      throws IOException
  {
    if (oldMd.getArtifactId() == null || oldMd.getGroupId() == null || oldMd.getVersion() == null
        || oldMd.getVersioning() == null || oldMd.getVersioning().getSnapshot() == null
        || oldMd.getVersioning().getSnapshot().getTimestamp() == null) {
      return false;
    }

    Metadata md = createMetadata(path);

    if (StringUtils.equals(oldMd.getArtifactId(), md.getArtifactId())
        && StringUtils.equals(oldMd.getGroupId(), md.getGroupId())
        && StringUtils.equals(oldMd.getVersion(), md.getVersion())
        && md.getVersioning() != null
        && md.getVersioning().getSnapshot() != null
        && StringUtils.equals(oldMd.getVersioning().getSnapshot().getTimestamp(),
        md.getVersioning().getSnapshot().getTimestamp())
        && oldMd.getVersioning().getSnapshot().getBuildNumber() == md.getVersioning().getSnapshot().getBuildNumber()
        && (oldMd.getVersioning().getVersions().containsAll(md.getVersioning().getVersions()) &&
        md.getVersioning().getVersions().containsAll(
            oldMd.getVersioning().getVersions()))
        && equals(oldMd.getVersioning().getSnapshotVersions(), md.getVersioning().getSnapshotVersions())) {
      return true;
    }

    return false;
  }

  private boolean equals(List<SnapshotVersion> old, List<SnapshotVersion> md) {
    if (old.size() != md.size()) {
      return false;
    }

    for (SnapshotVersion version : md) {
      SnapshotVersion oldVersion = MetadataUtil.searchForEquivalent(version, old);
      if (oldVersion == null) {
        return false;
      }

      if (!StringUtils.equals(oldVersion.getVersion(), version.getVersion())
          || !StringUtils.equals(oldVersion.getUpdated(), version.getUpdated())) {
        return false;
      }
    }

    return true;
  }
}
