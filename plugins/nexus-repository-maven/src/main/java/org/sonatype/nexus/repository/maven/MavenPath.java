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
package org.sonatype.nexus.repository.maven;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.sonatype.nexus.common.hash.HashAlgorithm;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maven repository path. Every item in repository may have hashes, stored on paths with proper
 * suffixes, and artifact paths have non-null coordinates.
 *
 * @since 3.0
 */
@Immutable
public class MavenPath
{
  public enum HashType
  {
    SHA1("sha1", HashAlgorithm.SHA1),

    MD5("md5", HashAlgorithm.MD5);

    /**
     * {@link HashAlgorithm}s corresponding to {@link HashType}s.
     */
    public static final List<HashAlgorithm> ALGORITHMS = ImmutableList
        .of(SHA1.getHashAlgorithm(), MD5.getHashAlgorithm());

    private final String ext;

    private final HashAlgorithm hashAlgorithm;

    HashType(final String ext, final HashAlgorithm hashAlgorithm) {
      this.ext = ext;
      this.hashAlgorithm = hashAlgorithm;
    }

    public String getExt() {
      return ext;
    }

    public HashAlgorithm getHashAlgorithm() {
      return hashAlgorithm;
    }
  }

  public enum SignatureType
  {
    GPG("asc");

    private final String ext;

    SignatureType(final String ext) {
      this.ext = ext;
    }

    public String getExt() {
      return ext;
    }
  }

  public static class Coordinates
  {
    private final boolean snapshot;

    private final String groupId;

    private final String artifactId;

    private final String version;

    private final Long timestamp;

    private final Integer buildNumber;

    private final String baseVersion;

    private final String classifier;

    private final String extension;

    private final SignatureType signatureType;

    public Coordinates(final boolean snapshot,
                       final String groupId,
                       final String artifactId,
                       final String version,
                       @Nullable final Long timestamp,
                       @Nullable final Integer buildNumber,
                       final String baseVersion,
                       @Nullable final String classifier,
                       final String extension,
                       final SignatureType signatureType)
    {
      this.snapshot = snapshot;
      this.groupId = checkNotNull(groupId);
      this.artifactId = checkNotNull(artifactId);
      this.version = checkNotNull(version);
      this.timestamp = snapshot ? timestamp : null;
      this.buildNumber = snapshot ? buildNumber : null;
      this.baseVersion = checkNotNull(baseVersion);
      this.classifier = classifier;
      this.extension = checkNotNull(extension);
      this.signatureType = signatureType;
    }

    public boolean isSnapshot() {
      return snapshot;
    }

    @Nonnull
    public String getGroupId() {
      return groupId;
    }

    @Nonnull
    public String getArtifactId() {
      return artifactId;
    }

    @Nonnull
    public String getVersion() {
      return version;
    }

    @Nullable
    public Long getTimestamp() {
      return timestamp;
    }

    @Nullable
    public Integer getBuildNumber() {
      return buildNumber;
    }

    @Nonnull
    public String getBaseVersion() {
      return baseVersion;
    }

    @Nullable
    public String getClassifier() {
      return classifier;
    }

    @Nonnull
    public String getExtension() {
      return extension;
    }

    @Nullable
    public SignatureType getSignatureType() {
      return signatureType;
    }
  }

  private final String path;

  private final String fileName;

  private final HashType hashType;

  private final Coordinates coordinates;

  public MavenPath(final String path, final Coordinates coordinates)
  {
    this.path = checkNotNull(path);
    this.fileName = this.path.substring(path.lastIndexOf('/') + 1);
    HashType ht = null;
    for (HashType v : HashType.values()) {
      if (this.fileName.endsWith("." + v.getExt())) {
        ht = v;
        break;
      }
    }
    this.hashType = ht;
    this.coordinates = coordinates;
  }

  @Nonnull
  public String getPath() {
    return path;
  }

  @Nonnull
  public String getFileName() {
    return fileName;
  }

  /**
   * Returns hash type if this path points at Maven hash file, otherwise {@code null}.
   */
  @Nullable
  public HashType getHashType() {
    return hashType;
  }

  /**
   * Returns the Maven coordinates if this path is an artifact path, otherwise {@code null}.
   */
  @Nullable
  public Coordinates getCoordinates() {
    return coordinates;
  }

  /**
   * Returns {@code true} if this path is subordinate (is hash or signature) of another path.
   *
   * @see {@link #subordinateOf()}
   */
  public boolean isSubordinate() {
    return isHash() || isSignature();
  }

  /**
   * Returns {@code true} if this path represents a hash.
   */
  public boolean isHash() {
    return hashType != null;
  }

  /**
   * Returns {@code true} if this path represents a signature.
   */
  public boolean isSignature() {
    return coordinates != null && coordinates.getSignatureType() != null;
  }

  /**
   * Returns {@code true} if this path represents an artifact POM.
   */
  public boolean isPom() {
    return coordinates != null && "pom".equals(coordinates.getExtension());
  }

  /**
   * Returns the "main", non-subordinate path of this path. The "main" path is never a hash nor a signature.
   */
  @Nonnull
  public MavenPath main() {
    MavenPath mavenPath = this;
    while (mavenPath.isSubordinate()) {
      mavenPath = mavenPath.subordinateOf();
    }
    return mavenPath;
  }

  /**
   * Returns the "parent" path, that this path is subordinate of, or this instance if it is not a subordinate.
   */
  @Nonnull
  public MavenPath subordinateOf() {
    if (hashType != null) {
      int hashSuffixLen = hashType.getExt().length() + 1; // the dot
      Coordinates mainCoordinates = null;
      if (coordinates != null) {
        mainCoordinates = new Coordinates(
            coordinates.isSnapshot(),
            coordinates.getGroupId(),
            coordinates.getArtifactId(),
            coordinates.getVersion(),
            coordinates.getTimestamp(),
            coordinates.getBuildNumber(),
            coordinates.getBaseVersion(),
            coordinates.getClassifier(),
            coordinates.getExtension().substring(0, coordinates.getExtension().length() - hashSuffixLen),
            coordinates.getSignatureType()
        );
      }
      return new MavenPath(
          path.substring(0, path.length() - hashSuffixLen),
          mainCoordinates
      );
    }
    else if (coordinates != null && coordinates.getSignatureType() != null) {
      int signatureSuffixLen = coordinates.getSignatureType().getExt().length() + 1; // the dot
      Coordinates mainCoordinates = new Coordinates(
          coordinates.isSnapshot(),
          coordinates.getGroupId(),
          coordinates.getArtifactId(),
          coordinates.getVersion(),
          coordinates.getTimestamp(),
          coordinates.getBuildNumber(),
          coordinates.getBaseVersion(),
          coordinates.getClassifier(),
          coordinates.getExtension().substring(0, coordinates.getExtension().length() - signatureSuffixLen),
          null
      );
      return new MavenPath(
          path.substring(0, path.length() - signatureSuffixLen),
          mainCoordinates
      );
    }
    return this;
  }

  /**
   * Returns path of passed in hash type that is subordinate of this path. This path cannot be hash.
   */
  @Nonnull
  public MavenPath hash(final HashType hashType) {
    checkNotNull(hashType);
    checkArgument(this.hashType == null, "This path is already a hash: %s", this);
    Coordinates hashCoordinates = null;
    if (coordinates != null) {
      hashCoordinates = new Coordinates(
          coordinates.isSnapshot(),
          coordinates.getGroupId(),
          coordinates.getArtifactId(),
          coordinates.getVersion(),
          coordinates.getTimestamp(),
          coordinates.getBuildNumber(),
          coordinates.getBaseVersion(),
          coordinates.getClassifier(),
          coordinates.getExtension() + "." + hashType.getExt(),
          coordinates.getSignatureType()
      );
    }
    return new MavenPath(
        path + "." + hashType.getExt(),
        hashCoordinates
    );
  }

  /**
   * Returns path of passed in signature type that is subordinate of this path. This path cannot be
   * hash nor signature.
   */
  @Nonnull
  public MavenPath signature(final SignatureType signatureType) {
    checkNotNull(signatureType);
    checkArgument(hashType == null, "This path is already a hash: %s", this);
    checkArgument(coordinates != null, "Only artifact paths may have signatures: %s", this);
    checkArgument(coordinates.getSignatureType() == null, "This path is already a signature: %s", this);
    Coordinates signatureCoordinates = new Coordinates(
        coordinates.isSnapshot(),
        coordinates.getGroupId(),
        coordinates.getArtifactId(),
        coordinates.getVersion(),
        coordinates.getTimestamp(),
        coordinates.getBuildNumber(),
        coordinates.getBaseVersion(),
        coordinates.getClassifier(),
        coordinates.getExtension() + "." + signatureType.getExt(),
        signatureType
    );
    return new MavenPath(
        path + "." + signatureType.getExt(),
        signatureCoordinates
    );
  }

  /**
   * Returns path pointing to given extension and optional classifier within this same GAV. Only usable for artifact
   * paths, those having non-null {@link #getCoordinates()}.
   */
  @Nonnull
  public MavenPath locate(final String extension, @Nullable final String classifier) {
    checkNotNull(extension);
    checkArgument(coordinates != null, "Only artifact paths may locate: %s", this);

    MavenPath origin = main();
    Coordinates newCoordinates = new Coordinates(
        origin.coordinates.isSnapshot(),
        origin.coordinates.getGroupId(),
        origin.coordinates.getArtifactId(),
        origin.coordinates.getVersion(),
        origin.coordinates.getTimestamp(),
        origin.coordinates.getBuildNumber(),
        origin.coordinates.getBaseVersion(),
        classifier,
        extension,
        null
    );
    // strip ".ext"
    String newPath = origin.path.substring(0, origin.path.length() - (origin.coordinates.extension.length() + 1));
    if (origin.coordinates.classifier != null) {
      // strip "-classifier"
      newPath = newPath.substring(0, newPath.length() - origin.coordinates.classifier.length() + 1);
    }
    if (classifier != null) {
      newPath += "-" + classifier;
    }
    newPath += "." + extension;
    return new MavenPath(
        newPath,
        newCoordinates
    );
  }

  /**
   * Returns path pointing to POM within this same GAV. Only usable for artifact
   * paths, those having non-null {@link #getCoordinates()}.
   */
  @Nonnull
  public MavenPath locatePom() {
    return locate("pom", null);
  }

  /**
   * Returns path pointing to non-classifier artifact within this same GAV. Only usable for artifact
   * paths, those having non-null {@link #getCoordinates()}.
   */
  @Nonnull
  public MavenPath locateMainArtifact(final String extension) {
    return locate(extension, null);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MavenPath)) {
      return false;
    }
    MavenPath that = (MavenPath) o;
    return path.equals(that.path);
  }

  @Override
  public int hashCode() {
    return path.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "path='" + path + '\'' +
        ", fileName='" + fileName + '\'' +
        ", hashType=" + hashType +
        '}';
  }
}
