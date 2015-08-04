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
package org.sonatype.plugin.nexus.testenvironment;

import java.io.File;

public class MavenArtifact
{

  private String artifactId;

  private String classifier;

  private String groupId;

  private File outputDirectory;

  private String outputProperty;

  private String type;

  private String version;

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public MavenArtifact() {
    super();
  }

  public MavenArtifact(String groupId, String artifactId) {
    this();
    this.groupId = groupId;
    this.artifactId = artifactId;
  }

  public MavenArtifact(String groupId, String artifactId, String classifier, String type) {
    this(groupId, artifactId);
    this.classifier = classifier;
    this.type = type;
  }

  public MavenArtifact(String groupId, String artifactId, String classifier, String type, String outputProperty) {
    this(groupId, artifactId, classifier, type);
    this.outputProperty = outputProperty;
  }

  public String getOutputProperty() {
    return outputProperty;
  }

  public void setOutputProperty(String outputProperty) {
    this.outputProperty = outputProperty;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getClassifier() {
    return classifier;
  }

  public String getGroupId() {
    return groupId;
  }

  public File getOutputDirectory() {
    return outputDirectory;
  }

  public String getType() {
    if (type == null) {
      return "jar";
    }
    return type;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  public void setClassifier(String classifier) {
    this.classifier = classifier;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public void setOutputDirectory(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
    result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
    result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
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
    MavenArtifact other = (MavenArtifact) obj;
    if (artifactId == null) {
      if (other.artifactId != null) {
        return false;
      }
    }
    else if (!artifactId.equals(other.artifactId)) {
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
    if (groupId == null) {
      if (other.groupId != null) {
        return false;
      }
    }
    else if (!groupId.equals(other.groupId)) {
      return false;
    }
    if (type == null) {
      if (other.type != null) {
        return false;
      }
    }
    else if (!type.equals(other.type)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(groupId).append(':').//
        append(artifactId).append(':').//
        append(classifier).append(':').//
        append(type).//
        toString();
  }

}
