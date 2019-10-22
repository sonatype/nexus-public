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
package org.sonatype.nexus.repository.cocoapods.internal.pod.git;

import org.sonatype.nexus.repository.cocoapods.internal.CocoapodsConfig;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @since 3.19
 */
@RunWith(JUnitParamsRunner.class)
public class GitApiHelperTest
{
  @Test
  @Parameters(method = "provideTestGenerateGitHubApiUriParams")
  public void testGenerateGitHubApiUri(
      final String host,
      final String vendor,
      final String repository,
      final String ref,
      final String apiUri)
  {
    CocoapodsConfig config =
        new CocoapodsConfig("https://api.github.com", "https://bitbucket.org", "https://gitlab.com");
    GitArtifactInfo info = new GitArtifactInfo(host, vendor, repository, ref);
    assertThat(GitApiHelper.buildApiUri(info, config).toString(), is(apiUri));
  }

  private static Object[] provideTestGenerateGitHubApiUriParams() {
    return new Object[]{
        new Object[]{
            "github.com", "mycheck888", "MyCheckWalletUI", "1.2.3",
            "https://api.github.com/repos/mycheck888/MyCheckWalletUI/tarball/1.2.3"
        },
        new Object[]{
            "github.com", "mycheck888", "MyCheckWalletUI", null,
            "https://api.github.com/repos/mycheck888/MyCheckWalletUI/tarball/"
        },
        new Object[]{
            "bitbucket.org", "jefrydagucci", "asbaseiosproject", "v0.9.2",
            "https://bitbucket.org/jefrydagucci/asbaseiosproject/get/v0.9.2.tar.gz"
        },
        new Object[]{
            "bitbucket.org", "jefrydagucci", "asbaseiosproject", null,
            "https://bitbucket.org/jefrydagucci/asbaseiosproject/get/master.tar.gz"
        },
        new Object[]{
            "gitlab.com", "abtasty-public", "mobile/abtastysdkios", "1.1",
            "https://gitlab.com/abtasty-public/mobile/abtastysdkios/-/archive/1.1/1.1.tar.gz"
        },
        new Object[]{
            "gitlab.com", "abtasty-public", "mobile/abtastysdkios", null,
            "https://gitlab.com/abtasty-public/mobile/abtastysdkios/-/archive/master/master.tar.gz"
        }
    };
  }
}
