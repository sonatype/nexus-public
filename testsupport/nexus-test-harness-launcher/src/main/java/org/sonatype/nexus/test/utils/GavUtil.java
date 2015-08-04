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
package org.sonatype.nexus.test.utils;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.maven.index.artifact.Gav;
import org.codehaus.plexus.util.StringUtils;

public class GavUtil
{

  public static Gav newGav(String groupId, String artifactId, String version) {
    return newGav(groupId, artifactId, version, "jar");
  }

  public static Gav newGav(String groupId, String artifactId, String version, String packging) {
    return new Gav(groupId, artifactId, version, null, packging, null, null, null, false, null, false, null);
  }

  public static String getRelitivePomPath(Gav gav)
      throws FileNotFoundException
  {
    return getRelitiveArtifactPath(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), "pom", null);
  }

  public static String getRelitiveArtifactPath(Gav gav)
      throws FileNotFoundException
  {
    long timestamp = gav.getSnapshotTimeStamp() != null ? gav.getSnapshotTimeStamp() : 0;
    int buildNumber = gav.getSnapshotBuildNumber() != null ? gav.getSnapshotBuildNumber() : 0;

    return getRelitiveArtifactPath(gav.getGroupId(), gav.getArtifactId(), gav.getVersion(), gav.getExtension(),
        gav.getClassifier(), gav.isSnapshot(), timestamp, buildNumber);
  }

  public static String getRelitiveArtifactPath(String groupId, String artifactId, String version, String extension,
                                               String classifier)
      throws FileNotFoundException
  {
    return getRelitiveArtifactPath(groupId, artifactId, version, extension, classifier, false, 0, 0);
  }

  private static String getRelitiveArtifactPath(String groupId, String artifactId, String version, String extension,
                                                String classifier, boolean snapshot, long timestamp, int buildNumber)
      throws FileNotFoundException
  {
    String classifierPart = StringUtils.isEmpty(classifier) ? "" : "-" + classifier;
    String fileVersion = version;
    if (snapshot && timestamp > 0) {
      fileVersion = version.replaceFirst("SNAPSHOT",
          new SimpleDateFormat("yyyyMMdd.HHmmss").format(new Date(timestamp)) + "-" + buildNumber);
    }
    return groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + fileVersion
        + classifierPart + "." + extension;
  }

}
