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
package org.sonatype.plugin.metadata;

import org.sonatype.nexus.proxy.maven.packaging.ArtifactPackagingMapper;

import com.google.common.base.Preconditions;
import org.codehaus.plexus.util.StringUtils;

/**
 * GAV coordinate fully describes the coordinates, it's "full detail". Is used as key in plugin descriptor, but also as
 * coordinates for plugin dependencies.
 */
@Deprecated
public final class GAVCoordinate
{
  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  private final String groupId;

  private final String artifactId;

  private final String version;

  private final String classifier;

  private final String type;

  // ----------------------------------------------------------------------
  // Constructors
  // ----------------------------------------------------------------------

  public GAVCoordinate(final String groupId, final String artifactId, final String version) {
    this(groupId, artifactId, version, null, null);
  }

  public GAVCoordinate(final String groupId, final String artifactId, final String version, final String classifier,
                       final String type)
  {
    this.groupId = Preconditions.checkNotNull(groupId);
    this.artifactId = Preconditions.checkNotNull(artifactId);
    this.version = Preconditions.checkNotNull(version);
    this.classifier = classifier;
    this.type = type;
  }

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public String getClassifier() {
    return classifier;
  }

  public String getType() {
    return type;
  }

  public String getFinalName(final ArtifactPackagingMapper packagingMapper) {
    final StringBuilder buf = new StringBuilder();
    buf.append(artifactId).append('-').append(version);
    if (StringUtils.isNotEmpty(classifier)) {
      buf.append('-').append(classifier);
    }
    if (StringUtils.isNotEmpty(type)) {
      buf.append('.').append(packagingMapper.getExtensionForPackaging(type));
    }
    else {
      buf.append(".jar");
    }
    return buf.toString();
  }

  public boolean matchesByGA(final GAVCoordinate coord) {
    return StringUtils.equals(getGroupId(), coord.getGroupId())
        && StringUtils.equals(getArtifactId(), coord.getArtifactId());
  }

  @Override
  public boolean equals(final Object rhs) {
    if (this == rhs) {
      return true;
    }
    if (!(rhs instanceof GAVCoordinate)) {
      return false;
    }
    return toString().equals(rhs.toString());
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public String toString() {
    final StringBuilder buf = new StringBuilder();
    buf.append(groupId).append(':').append(artifactId).append(':').append(version);
    final boolean haveType = StringUtils.isNotEmpty(type);
    if (StringUtils.isNotEmpty(classifier)) {
      buf.append(':').append(classifier);
    }
    else if (haveType) {
      buf.append(':');
    }
    if (haveType) {
      buf.append(':').append(type);
    }
    return buf.toString();
  }
}
