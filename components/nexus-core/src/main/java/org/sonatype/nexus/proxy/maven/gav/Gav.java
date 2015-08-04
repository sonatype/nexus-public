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
package org.sonatype.nexus.proxy.maven.gav;

import java.util.regex.Pattern;

/**
 * An immutable value class representing unique artifact coordinates.
 *
 * @author cstamas
 * @author jvanzyl
 */
public class Gav
{
  private static final String SNAPSHOT_VERSION = "SNAPSHOT";

  private static final Pattern VERSION_FILE_PATTERN =
      Pattern.compile(
          "^(.*)-([0-9]{8}.[0-9]{6})-([0-9]+)$|^([0-9]{8}.[0-9]{6})-([0-9]+)$|^(.*)([0-9]{8}.[0-9]{6})-([0-9]+)$");

  public static boolean isSnapshot(final String version) {
    return VERSION_FILE_PATTERN.matcher(version).matches() || version.endsWith(SNAPSHOT_VERSION);
  }

  /**
   * Enumeration representing Maven artifact hash types
   */
  public enum HashType
  {
    sha1, md5
  }

  /**
   * Enumeration representing Maven artifact signature types
   */
  public enum SignatureType
  {
    gpg;

    @Override
    public String toString() {
      switch (this) {
        case gpg: {
          return "asc";
        }

        default: {
          return "unknown-signature-type";
        }
      }
    }
  }

  private final String groupId;

  private final String artifactId;

  private final String version;

  private final String baseVersion;

  private final String classifier;

  private final String extension;

  private final Integer snapshotBuildNumber;

  private final Long snapshotTimeStamp;

  private final String name;

  private final boolean snapshot;

  private final boolean hash;

  private final HashType hashType;

  private final boolean signature;

  private final SignatureType signatureType;

  public Gav(String groupId, String artifactId, String version) {
    this(groupId, artifactId, version, null, null, null, null, null, false, null, false, null);
  }

  /**
   * Deprecated constructor, left here for backward compatibility. It simply delegates to other constructor and
   * neglects the snapshot redundant parameter.
   *
   * @deprecated The <code>boolean snapshot</code> parameter is simply neglected. Use the constructor without it.
   */
  public Gav(String groupId, String artifactId, String version, String classifier, String extension,
             Integer snapshotBuildNumber, Long snapshotTimeStamp, String name, boolean snapshot, boolean hash,
             HashType hashType, boolean signature, SignatureType signatureType)
  {
    this(groupId, artifactId, version, classifier, extension, snapshotBuildNumber, snapshotTimeStamp, name, hash,
        hashType, signature, signatureType);
  }

  public Gav(String groupId, String artifactId, String version, String classifier, String extension,
             Integer snapshotBuildNumber, Long snapshotTimeStamp, String name, boolean hash, HashType hashType,
             boolean signature, SignatureType signatureType)
  {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.snapshot = isSnapshot(version);

    if (!snapshot) {
      this.baseVersion = null;
    }
    else {
      if (version.contains("SNAPSHOT")) {
        // this is not a timestamped version
        this.baseVersion = null;
      }
      else {
        // this is a timestamped version (verified against pattern, see above)
        // we have XXXXXX-YYYYMMDD.HHMMSS-B
        // but XXXXXX may contain "-" too!

        // if ( new DefaultNexusEnforcer().isStrict() )
        // {
        // this.baseVersion = version.substring( 0, version.lastIndexOf( '-' ) );
        // this.baseVersion = baseVersion.substring( 0, baseVersion.lastIndexOf( '-' ) ) + "-SNAPSHOT";
        // }
        // also there may be no XXXXXX (i.e. when version is strictly named SNAPSHOT
        // BUT this is not the proper scheme, we will simply loosen up here if requested
        // else
        // {
        // trim the part of 'YYYYMMDD.HHMMSS-BN
        String tempBaseVersion = version.substring(0, version.lastIndexOf('-'));
        tempBaseVersion = tempBaseVersion.substring(0, tempBaseVersion.length() - 15);

        if (tempBaseVersion.length() > 0) {
          this.baseVersion = tempBaseVersion + "SNAPSHOT";
        }
        else {
          this.baseVersion = "SNAPSHOT";
        }
        // }
      }
    }

    this.classifier = classifier;
    this.extension = extension;
    this.snapshotBuildNumber = snapshotBuildNumber;
    this.snapshotTimeStamp = snapshotTimeStamp;
    this.name = name;
    this.hash = hash;
    this.hashType = hashType;
    this.signature = signature;
    this.signatureType = signatureType;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public String getBaseVersion() {
    if (baseVersion == null) {
      return getVersion();
    }
    else {
      return baseVersion;
    }
  }

  public String getClassifier() {
    return classifier;
  }

  public String getExtension() {
    return extension;
  }

  public String getName() {
    return name;
  }

  public boolean isSnapshot() {
    return snapshot;
  }

  public Integer getSnapshotBuildNumber() {
    return snapshotBuildNumber;
  }

  public Long getSnapshotTimeStamp() {
    return snapshotTimeStamp;
  }

  public boolean isHash() {
    return hash;
  }

  public HashType getHashType() {
    return hashType;
  }

  public boolean isSignature() {
    return signature;
  }

  public SignatureType getSignatureType() {
    return signatureType;
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = 31 * result + (groupId == null ? 0 : groupId.hashCode());
    result = 31 * result + (artifactId == null ? 0 : artifactId.hashCode());
    result = 31 * result + (version == null ? 0 : version.hashCode());
    result = 31 * result + (baseVersion == null ? 0 : baseVersion.hashCode());
    result = 31 * result + (classifier == null ? 0 : classifier.hashCode());
    result = 31 * result + (extension == null ? 0 : extension.hashCode());
    result = 31 * result + (name == null ? 0 : name.hashCode());
    result = 31 * result + (snapshot ? 1231 : 1237);
    result = 31 * result + (snapshotBuildNumber == null ? 0 : snapshotBuildNumber.hashCode());
    result = 31 * result + (snapshotTimeStamp == null ? 0 : snapshotTimeStamp.hashCode());
    result = 31 * result + (hash ? 1231 : 1237);
    result = 31 * result + (hashType == null ? 0 : hashType.hashCode());
    result = 31 * result + (signature ? 1231 : 1237);
    result = 31 * result + (signatureType == null ? 0 : signatureType.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }

    Gav other = (Gav) obj;

    if (groupId == null) {
      if (other.groupId != null) {
        return false;
      }
    }
    else if (!groupId.equals(other.groupId)) {
      return false;
    }

    if (artifactId == null) {
      if (other.artifactId != null) {
        return false;
      }
    }
    else if (!artifactId.equals(other.artifactId)) {
      return false;
    }

    if (version == null) {
      if (other.version != null) {
        return false;
      }
    }
    else if (!version.equals(other.version)) {
      return false;
    }

    if (baseVersion == null) {
      if (other.baseVersion != null) {
        return false;
      }
    }
    else if (!baseVersion.equals(other.baseVersion)) {
      return false;
    }

    if (classifier == null) {
      if (other.classifier != null) {
        return false;
      }
    }
    else if (!classifier.equals(other.classifier)) {
      return false;
    }

    if (extension == null) {
      if (other.extension != null) {
        return false;
      }
    }
    else if (!extension.equals(other.extension)) {
      return false;
    }

    if (name == null) {
      if (other.name != null) {
        return false;
      }
    }
    else if (!name.equals(other.name)) {
      return false;
    }

    if (snapshot != other.snapshot) {
      return false;
    }

    if (snapshotBuildNumber == null) {
      if (other.snapshotBuildNumber != null) {
        return false;
      }
    }
    else if (!snapshotBuildNumber.equals(other.snapshotBuildNumber)) {
      return false;
    }

    if (snapshotTimeStamp == null) {
      if (other.snapshotTimeStamp != null) {
        return false;
      }
    }
    else if (!snapshotTimeStamp.equals(other.snapshotTimeStamp)) {
      return false;
    }

    if (hash != other.hash) {
      return false;
    }

    if (hashType == null) {
      if (other.hashType != null) {
        return false;
      }
    }
    else if (!hashType.equals(other.hashType)) {
      return false;
    }

    if (signature != other.signature) {
      return false;
    }

    if (signatureType == null) {
      if (other.signatureType != null) {
        return false;
      }
    }
    else if (!signatureType.equals(other.signatureType)) {
      return false;
    }

    return true;
  }
}
