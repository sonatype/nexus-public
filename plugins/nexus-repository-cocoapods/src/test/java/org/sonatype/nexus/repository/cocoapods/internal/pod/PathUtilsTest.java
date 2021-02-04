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

import org.sonatype.nexus.repository.cocoapods.internal.PathUtils;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @since 3.27
 */
@RunWith(JUnitParamsRunner.class)
public class PathUtilsTest
{
  @Test
  @Parameters(method = "provideGeneratePodFileNameParams")
  public void testGeneratePodFileName(
      final String name,
      final String version,
      final String downloadUri,
      final String nxrmPath)
  {
    assertThat(PathUtils.buildNxrmPodPath(name, version, URI.create(downloadUri)), is(nxrmPath));
  }

  private static Object[] provideGeneratePodFileNameParams() {
    return new Object[]{
        new Object[]{
            "mycheck888", "1.2.3", "https://api.github.com/repos/mycheck888/MyCheckWalletUI/tarball/1.2.3",
            "pods/mycheck888/1.2.3/1.2.3.tar.gz"
        },
        new Object[]{
            "mycheck888", "1.2.3", "https://api.github.com/repos/mycheck888/MyCheckWalletUI/tarball/",
            "pods/mycheck888/1.2.3/1.2.3.tar.gz"
        },
        new Object[]{
            "packName", "0.9.2", "https://bitbucket.org/jefrydagucci/asbaseiosproject/get/v0.9.2.tar.gz",
            "pods/packName/0.9.2/v0.9.2.tar.gz"
        },
        new Object[]{
            "packName", "0.9.2", "https://bitbucket.org/jefrydagucci/asbaseiosproject/get/master.tar.gz",
            "pods/packName/0.9.2/master.tar.gz"
        },
        new Object[]{
            "packName", "0.9.2", "https://gitlab.com/streethawk/sdks/streethawk-sdk-ios/-/archive/1.10.2/1.10.2.tar.gz",
            "pods/packName/0.9.2/1.10.2.tar.gz"
        },
        new Object[]{
            "packName", "0.9.2", "https://gitlab.com/streethawk/sdks/streethawk-sdk-ios/-/archive/master/master.tar.gz",
            "pods/packName/0.9.2/master.tar.gz"
        },
    };
  }
}
