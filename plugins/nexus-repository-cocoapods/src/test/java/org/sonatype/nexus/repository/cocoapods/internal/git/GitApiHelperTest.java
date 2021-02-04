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
package org.sonatype.nexus.repository.cocoapods.internal.git;

import java.net.URI;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @since 3.19
 */
@RunWith(JUnitParamsRunner.class)
public class GitApiHelperTest
{
  @Test
  @Parameters(method = "provideTestGenerateGitHubApiUriParams")
  public void testGenerateGitHubApiUri(
      final String gitUri,
      final String ref,
      final String downloadUri)
  {
    GitApiHelper gitApiHelper =
        new GitApiHelper("https://api.github.com", "https://bitbucket.org", "https://gitlab.com");

    assertThat(gitApiHelper.buildDownloadURI(URI.create(gitUri), ref).toString(), is(downloadUri));
  }

  private static Object[] provideTestGenerateGitHubApiUriParams() {
    return new Object[]{
        new Object[]{
            "https://github.com/mycheck888/MyCheckWalletUI.git", "1.2.3",
            "https://api.github.com/repos/mycheck888/MyCheckWalletUI/tarball/1.2.3"
        },
        new Object[]{
            "https://github.com/mycheck888/MyCheckWalletUI.git", null,
            "https://api.github.com/repos/mycheck888/MyCheckWalletUI/tarball/"
        },
        new Object[]{
            "https://bitbucket.org/jefrydagucci/asbaseiosproject.git", "v0.9.2",
            "https://bitbucket.org/jefrydagucci/asbaseiosproject/get/v0.9.2.tar.gz"
        },
        new Object[]{
            "https://bitbucket.org/jefrydagucci/asbaseiosproject.git", null,
            "https://bitbucket.org/jefrydagucci/asbaseiosproject/get/master.tar.gz"
        },
        new Object[]{
            "https://gitlab.com/streethawk/sdks/streethawk-sdk-ios.git", "1.10.2",
            "https://gitlab.com/streethawk/sdks/streethawk-sdk-ios/-/archive/1.10.2/1.10.2.tar.gz"
        },
        new Object[]{
            "https://gitlab.com/streethawk/sdks/streethawk-sdk-ios.git", null,
            "https://gitlab.com/streethawk/sdks/streethawk-sdk-ios/-/archive/master/master.tar.gz"
        }
    };
  }
}
