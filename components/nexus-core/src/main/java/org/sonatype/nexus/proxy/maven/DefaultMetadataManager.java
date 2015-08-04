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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.maven.metadata.operations.ModelVersionUtility;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotVersion;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.codehaus.plexus.util.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Component responsible for metadata maintenance.
 *
 * @author cstamas
 * @todo add some unit tests
 */
@Singleton
@Named
public class DefaultMetadataManager
    extends ComponentSupport
    implements MetadataManager
{
  static final String LATEST_VERSION = "LATEST";

  static final String SNAPSHOT_VERSION = "SNAPSHOT";

  static final String RELEASE_VERSION = "RELEASE";

  private final MetadataUpdater metadataUpdater;

  private final MetadataLocator metadataLocator;

  @Inject
  public DefaultMetadataManager(final MetadataUpdater metadataUpdater, 
                                final MetadataLocator metadataLocator) {
    this.metadataUpdater = checkNotNull(metadataUpdater);
    this.metadataLocator = checkNotNull(metadataLocator);
  }

  @Override
  public void deployArtifact(ArtifactStoreRequest request)
      throws IOException
  {
    metadataUpdater.deployArtifact(request);
  }

  @Override
  public Gav resolveArtifact(ArtifactStoreRequest gavRequest)
      throws IOException
  {
    MavenRepository repository = gavRequest.getMavenRepository();

    String version = gavRequest.getVersion();

    Gav gav = null;

    if (LATEST_VERSION.equals(gavRequest.getVersion())) {
      // TODO: a workaround, adding dummy versions, only to make Gav happy
      gav =
          new Gav(gavRequest.getGroupId(), gavRequest.getArtifactId(),
              RepositoryPolicy.SNAPSHOT.equals(repository.getRepositoryPolicy()) ? "1-SNAPSHOT" : "1",
              gavRequest.getClassifier(), gavRequest.getExtension(), null, null, null, false, null, false, null);

      version = resolveLatest(gavRequest, gav);
    }
    else if (RELEASE_VERSION.equals(gavRequest.getVersion())) {
      // TODO: a workaround, adding dummy versions, only to make Gav happy
      gav =
          new Gav(gavRequest.getGroupId(), gavRequest.getArtifactId(),
              RepositoryPolicy.SNAPSHOT.equals(repository.getRepositoryPolicy()) ? "1-SNAPSHOT" : "1",
              gavRequest.getClassifier(), gavRequest.getExtension(), null, null, null, false, null, false, null);

      version = resolveRelease(gavRequest, gav);
    }

    if (LATEST_VERSION.equals(version) || RELEASE_VERSION.equals(version)) {
      // Nexus was not able to resolve those
      return null;
    }
    else {
      gav =
          new Gav(gavRequest.getGroupId(), gavRequest.getArtifactId(), version, gavRequest.getClassifier(),
              gavRequest.getExtension(), null, null, null, false, null, false, null);

      // if it is not "timestamped" version, try to get it
      if (gav.isSnapshot() && gav.getVersion().equals(gav.getBaseVersion())) {
        gav = resolveSnapshot(gavRequest, gav);
      }

      return gav;
    }
  }

  protected String resolveLatest(ArtifactStoreRequest gavRequest, Gav gav)
      throws IOException
  {
    MavenRepository repository = gavRequest.getMavenRepository();

    Metadata gaMd =
        metadataLocator.retrieveGAMetadata(new ArtifactStoreRequest(gavRequest.getMavenRepository(), gav,
            gavRequest.isRequestLocalOnly(), gavRequest.isRequestRemoteOnly()));

    if (gaMd.getVersioning() == null) {
      gaMd.setVersioning(new Versioning());
    }

    String latest = gaMd.getVersioning().getLatest();

    if (StringUtils.isEmpty(latest) && gaMd.getVersioning().getVersions() != null) {
      List<String> versions = gaMd.getVersioning().getVersions();

      // iterate over versions for the end, and grab the first snap found
      for (int i = versions.size() - 1; i >= 0; i--) {
        if (RepositoryPolicy.RELEASE.equals(repository.getRepositoryPolicy())) {
          if (!Gav.isSnapshot(versions.get(i))) {
            latest = versions.get(i);

            break;
          }
        }
        else if (RepositoryPolicy.SNAPSHOT.equals(repository.getRepositoryPolicy())) {
          if (Gav.isSnapshot(versions.get(i))) {
            latest = versions.get(i);

            break;
          }
        }
        else {
          latest = versions.get(i);

          break;
        }
      }
    }

    if (!StringUtils.isEmpty(latest)) {
      return latest;
    }
    else {
      return gavRequest.getVersion();
    }
  }

  protected String resolveRelease(ArtifactStoreRequest gavRequest, Gav gav)
      throws IOException
  {
    MavenRepository repository = gavRequest.getMavenRepository();

    if (RepositoryPolicy.SNAPSHOT.equals(repository.getRepositoryPolicy())) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Not a RELEASE repository for resolving GAV: " + gav.getGroupId() + " : " + gav.getArtifactId()
                + " : " + gav.getVersion() + " in repository " + repository.getId());
      }

      return gavRequest.getVersion();
    }

    if (log.isDebugEnabled()) {
      log.debug(
          "Resolving snapshot version for GAV: " + gavRequest.getGroupId() + " : " + gavRequest.getArtifactId()
              + " : " + gavRequest.getVersion() + " in repository " + repository.getId());
    }

    Metadata gaMd =
        metadataLocator.retrieveGAMetadata(new ArtifactStoreRequest(gavRequest.getMavenRepository(), gav,
            gavRequest.isRequestLocalOnly(), gavRequest.isRequestRemoteOnly()));

    if (gaMd.getVersioning() == null) {
      gaMd.setVersioning(new Versioning());
    }

    String release = gaMd.getVersioning().getRelease();

    if (StringUtils.isEmpty(release) && gaMd.getVersioning().getVersions() != null) {
      List<String> versions = gaMd.getVersioning().getVersions();

      // iterate over versions for the end, and grab the first snap found
      for (int i = versions.size() - 1; i >= 0; i--) {
        if (!Gav.isSnapshot(versions.get(i))) {
          release = versions.get(i);

          break;
        }
      }
    }

    if (!StringUtils.isEmpty(release)) {
      if (log.isDebugEnabled()) {
        log.debug("Resolved gav version from '" + gav.getVersion() + "' to '" + release + "'");
      }

      return release;
    }
    else {
      return gavRequest.getVersion();
    }
  }

  @Override
  public Gav resolveSnapshot(ArtifactStoreRequest gavRequest, Gav gav)
      throws IOException
  {
    MavenRepository repository = gavRequest.getMavenRepository();

    if (gav.isSnapshot() && (!gav.getVersion().equals(gav.getBaseVersion()))) {
      // it is already a timestamped version, return it unmodified
      return gav;
    }

    if (log.isDebugEnabled()) {
      log.debug(
          "Resolving snapshot version for GAV: " + gav.getGroupId() + " : " + gav.getArtifactId() + " : "
              + gav.getVersion() + " in repository " + repository.getId());
    }

    Metadata gavMd =
        metadataLocator.retrieveGAVMetadata(new ArtifactStoreRequest(gavRequest.getMavenRepository(), gav,
            gavRequest.isRequestLocalOnly(), gavRequest.isRequestRemoteOnly()));

    if (gavMd.getVersioning() == null) {
      gavMd.setVersioning(new Versioning());
    }

    if (ModelVersionUtility.Version.V110.compareTo(ModelVersionUtility.getModelVersion(gavMd)) <= 0) {
      return resolveSnapshotFromM3Metadata(gavRequest, gav, gavMd);
    }
    else {
      return resolveSnapshotFromM2Metadata(gavRequest, gav, gavMd);
    }
  }

  protected Gav resolveSnapshotFromM2Metadata(final ArtifactStoreRequest gavRequest, final Gav gav,
                                              final Metadata gavMd)
      throws IOException
  {
    String latest = null;

    Long buildTs = null;

    Integer buildNo = null;

    Snapshot current = gavMd.getVersioning().getSnapshot();

    // NEXUS-4284: we have non null current, with no timestamp field in the wild out there
    // so current != null is not enough
    if (current != null && StringUtils.isNotBlank(current.getTimestamp()) && (current.getBuildNumber() > 0)) {
      latest = gav.getBaseVersion();

      latest = latest.replace(SNAPSHOT_VERSION, current.getTimestamp() + "-" + current.getBuildNumber());

      buildTs = getTimeFromMetadataTimestampMaven2(current.getTimestamp());

      buildNo = current.getBuildNumber();
    }

    if (!StringUtils.isEmpty(latest) && Gav.isSnapshot(latest)) {
      if (log.isDebugEnabled()) {
        log.debug("Resolved gav version from '" + gav.getVersion() + "' to '" + latest + "'");
      }

      Gav result =
          new Gav(gav.getGroupId(), gav.getArtifactId(), latest, gav.getClassifier(), gav.getExtension(),
              buildNo, buildTs, gav.getName(), gav.isHash(), gav.getHashType(), gav.isSignature(),
              gav.getSignatureType());

      return result;
    }
    else {
      return gav;
    }
  }

  protected Gav resolveSnapshotFromM3Metadata(final ArtifactStoreRequest gavRequest, final Gav gav,
                                              final Metadata gavMd)
      throws IOException
  {
    for (SnapshotVersion sv : gavMd.getVersioning().getSnapshotVersions()) {
      if (StringUtils.equals(sv.getExtension(), gav.getExtension())
          && StringUtils.equals(StringUtils.defaultString(sv.getClassifier(), ""),
          StringUtils.defaultString(gav.getClassifier(), ""))) {
        Long buildTs = getTimeFromMetadataTimestampMaven3Updated(sv.getUpdated());

        Integer buildNo = getBuildNumberForMetadataMaven3Value(sv.getVersion());

        return new Gav(gav.getGroupId(), gav.getArtifactId(), sv.getVersion(), gav.getClassifier(),
            gav.getExtension(), buildNo, buildTs, gav.getName(), gav.isHash(), gav.getHashType(),
            gav.isSignature(), gav.getSignatureType());

      }
    }

    // even if model version is 1.1.0, we have no snapshots versions?
    return resolveSnapshotFromM2Metadata(gavRequest, gav, gavMd);
  }

  private static final String METADATA_TIMESTAMP_FORMAT_MAVEN2 = "yyyyMMdd.HHmmss";

  private static final String METADATA_TIMESTAMP_FORMAT_MAVEN3_UPDATED = "yyyyMMddHHmmss";

  /**
   * Convert a metadata timestamp in the specified format to its time since epoch millis equiv in the UTC timezone
   *
   * @param the      SimpleDateFormat format the parse the string with.
   * @param tsString a metadata timestamp string
   * @return the long millis
   * @throws NullPointerException     if arguments are null
   * @throws IllegalArgumentException if dateFormat is invalid
   */
  private static Long getTimeFromMetadataTimestamp(final String dateFormat, final String tsString) {
    try {
      SimpleDateFormat df = new SimpleDateFormat(dateFormat, Locale.US);
      df.setTimeZone(TimeZone.getTimeZone("GMT-00:00"));
      return Long.valueOf(df.parse(tsString).getTime());
    }
    catch (ParseException e) {
      return null;
    }
  }

  protected static Long getTimeFromMetadataTimestampMaven3Updated(final String tsString) {
    return getTimeFromMetadataTimestamp(METADATA_TIMESTAMP_FORMAT_MAVEN3_UPDATED, tsString);
  }

  protected static Long getTimeFromMetadataTimestampMaven2(final String tsString) {
    return getTimeFromMetadataTimestamp(METADATA_TIMESTAMP_FORMAT_MAVEN2, tsString);
  }

  protected static Integer getBuildNumberForMetadataMaven3Value(final String valueString) {
    try {
      final int lastIdx = valueString.lastIndexOf('-');

      if (lastIdx > -1) {
        return Integer.valueOf(valueString.substring(lastIdx + 1));
      }
      else {
        return 0;
      }
    }
    catch (NumberFormatException e) {
      return null;
    }
  }
}
