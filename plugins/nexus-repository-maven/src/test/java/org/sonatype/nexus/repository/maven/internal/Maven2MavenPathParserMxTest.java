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

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.MavenPath.SignatureType;
import org.sonatype.nexus.repository.maven.MavenPathParser;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * UT for {@link Maven2MavenPathParser}, that varies all the known forms of GAV elements.
 *
 * @since 3.0
 */
@RunWith(Parameterized.class)
public class Maven2MavenPathParserMxTest
    extends TestSupport
{
  public static String[] GROUP_IDS = {"single", "multiple.group.elements"};

  public static String[] ARTIFACT_IDS = {"simple", "artifact-with-dashes"};

  public static String[][] VERSIONS = { // [baseVersion, version pairs]
                                        {"1.0", "1.0"},
                                        {"1.0-alpha-1", "1.0-alpha-1"},
                                        {"1.0-SNAPSHOT", "1.0-SNAPSHOT"},
                                        {"1.0-SNAPSHOT", "1.0-20150317.161100-10"},
                                        {"1.0.SNAPSHOT", "1.0.20150317.161100-10"},
                                        {"1.0SNAPSHOT", "1.020150317.161100-10"}
  };

  public static String[] CLASSIFIERS = {null, "single", "with-dash", "with.dot"};

  public static String[] EXTENSIONS = {
      "jar", "jar.sha1", "jar.asc", "jar.asc.md5", "pom.md5.asc", "jar.md5.sha1", "jar.sha1.md5", "tar.gz",
      "tar.anyext.sha1", "tar.anyext.md5", "tar.anyext.asc", "tar.anyext.asc.md5",
      "cpio.anyext.sha1", "cpio.anyext.md5", "cpio.anyext.asc", "cpio.anyext.asc.md5",
      "nk.os.sha1", "nk.os.md5", "nk.os.asc", "nk.os.asc.md5"
  };

  @Parameters
  public static List<String[]> parameters() {
    final List<String[]> result = Lists.newArrayList();
    for (String g : GROUP_IDS) {
      for (String a : ARTIFACT_IDS) {
        for (String[] v : VERSIONS) {
          for (String c : CLASSIFIERS) {
            for (String e : EXTENSIONS) {
              final String[] params = new String[6];
              params[0] = g;
              params[1] = a;
              params[2] = v[0];
              params[3] = v[1];
              params[4] = c;
              params[5] = e;
              result.add(params);
            }
          }
        }
      }
    }
    return result;
  }

  private final MavenPathParser subject = new Maven2MavenPathParser();

  @Parameter(0)
  public String pGroupId;

  @Parameter(1)
  public String pArtifactId;

  @Parameter(2)
  public String pBaseVersion;

  @Parameter(3)
  public String pVersion;

  @Parameter(4)
  public String pClassifier;

  @Parameter(5)
  public String pExtension;

  private String path() {
    if (pClassifier != null) {
      return "/" + pGroupId.replace('.', '/') + "/" + pArtifactId + "/" + pBaseVersion + "/" + pArtifactId + "-" +
          pVersion + "-" + pClassifier + "." + pExtension;
    }
    else {
      return "/" + pGroupId.replace('.', '/') + "/" + pArtifactId + "/" + pBaseVersion + "/" + pArtifactId + "-" +
          pVersion + "." + pExtension;
    }
  }

  @Test
  public void affirmativeMxTest() {
    final String path = path();

    final boolean snapshot = pBaseVersion.endsWith("SNAPSHOT");
    final HashType hashType = path.endsWith(".sha1") ? HashType.SHA1 : (path.endsWith(".md5") ? HashType.MD5 : null);
    final SignatureType signatureType = path.contains(".asc") ? SignatureType.GPG : null;

    final MavenPath mavenPath = subject.parsePath(path);
    assertThat(mavenPath, notNullValue());
    assertThat(mavenPath.getPath(), equalTo(path.substring(1)));
    assertThat(mavenPath.getHashType(), equalTo(hashType));
    assertThat(mavenPath.getCoordinates(), notNullValue());
    assertThat(mavenPath.getCoordinates().isSnapshot(), equalTo(snapshot));
    assertThat(mavenPath.getCoordinates().getGroupId(), equalTo(pGroupId));
    assertThat(mavenPath.getCoordinates().getArtifactId(), equalTo(pArtifactId));
    assertThat(mavenPath.getCoordinates().getVersion(), equalTo(pVersion));
    assertThat(mavenPath.getCoordinates().getBaseVersion(), equalTo(pBaseVersion));
    assertThat(mavenPath.getCoordinates().getClassifier(), equalTo(pClassifier));
    assertThat(mavenPath.getCoordinates().getExtension(), equalTo(pExtension));
    assertThat(mavenPath.getCoordinates().getSignatureType(), equalTo(signatureType));
  }
}
