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
package org.sonatype.nexus.repository.cocoapods.internal.pod;

import java.net.URI;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @since 3.19
 */
@RunWith(JUnitParamsRunner.class)
public class PodPathProviderTest
{
  private PodPathProvider podPathProvider =
      new PodPathProvider("https://api.github.com", "https://bitbucket.org", "https://gitlab.com");

  @Test
  @Parameters(method = "provideGeneratePodFileNameGitParams")
  public void testGeneratePodFileNameGit(
      final String gitUriStr,
      final String name,
      final String version,
      final String ref,
      final String expectedFileName)
  {
    URI gitUri = URI.create(gitUriStr);
    assertThat(podPathProvider.buildNxrmPath(name, version, gitUri, ref), is(expectedFileName));
  }

  @Test
  @Parameters({
      "https://s3.amazonaws.com/mapfit-ios-1/tetragon-releases/tetragon-release-1.3.1.zip, " +
          "Tetragon-mobile, " +
          "1.3.1, " +
          "pods/Tetragon-mobile/1.3.1/https/s3.amazonaws.com/mapfit-ios-1/tetragon-releases/tetragon-release-1.3.1.zip"
  })
  public void testGeneratePodFileNameHttp(
      final String httpUri,
      final String name,
      final String version,
      final String expectedFileName)
  {
    assertThat(podPathProvider.buildNxrmPath(name, version, httpUri), is(expectedFileName));
  }

  private static Object[] provideGeneratePodFileNameGitParams() {
    return new Object[]{
        new Object[]{
            "https://github.com/mycheck888/MyCheckWalletUI.git", "mycheck888", "1.2.3", "1.2.3",
            "pods/mycheck888/1.2.3/https/api.github.com/repos/mycheck888/MyCheckWalletUI/tarball/1.2.3.tar.gz"
        },
        new Object[]{
            "https://github.com/mycheck888/MyCheckWalletUI.git", "mycheck888", "1.2.3", null,
            "pods/mycheck888/1.2.3/https/api.github.com/repos/mycheck888/MyCheckWalletUI/tarball/.tar.gz"
        },
        new Object[]{
            "https://bitbucket.org/jefrydagucci/asbaseiosproject.git", "asbaseiosproject", "v0.9.2",
            "v0.9.2", "pods/asbaseiosproject/v0.9.2/https/bitbucket.org/jefrydagucci/asbaseiosproject/get/v0.9.2.tar.gz"
        },
        new Object[]{
            "https://bitbucket.org/jefrydagucci/asbaseiosproject.git", "asbaseiosproject", "v0.9.2", null,
            "pods/asbaseiosproject/v0.9.2/https/bitbucket.org/jefrydagucci/asbaseiosproject/get/master.tar.gz"
        },
        new Object[]{
            "https://gitlab.com/streethawk/sdks/streethawk-sdk-ios.git", "streethawk", "1.10.2", "1.10.2",
            "pods/streethawk/1.10.2/https/gitlab.com/streethawk/sdks/streethawk-sdk-ios/-/archive/1.10.2/1.10.2.tar.gz"
        },
        new Object[]{
            "https://gitlab.com/streethawk/sdks/streethawk-sdk-ios.git", "streethawk", "1.10.2", null,
            "pods/streethawk/1.10.2/https/gitlab.com/streethawk/sdks/streethawk-sdk-ios/-/archive/master/master.tar.gz"
        }
    };
  }
}
