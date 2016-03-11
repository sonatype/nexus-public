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
package org.sonatype.nexus.testsuite.npm.smoke;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.sonatype.nexus.testsuite.npm.NpmMockRegistryITSupport;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.stringContainsInOrder;

/**
 * NPM CLI IT for NPM plugin with simple "install" invocation. The test required the "npm" command to be present on
 * path. It performs an installation of "commonjs" package, with all of it dependencies. Assertion is made that
 * all the needed versions are fetched (versions present in mock NPM repository) from NX, and that NPM command exited
 * with success.
 *
 * This IT requires the "npm" command be present on path. Also, no clue how this IT would behave on Windows, as it's
 * tested on OSX and would probably also work on Linux.
 */
public class NpmInstallIT
    extends NpmMockRegistryITSupport
{
  public NpmInstallIT(String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Test
  public void npmCliInstall() throws Exception {
    // create a NPM Proxy repository that proxies mock NPM registry
    createNpmProxyRepository(testMethodName());

    final File localDirectory = util.createTempDir();
    final File projectDir = util.createTempDir();
    final String cmd = String
        .format("npm install commonjs --registry %s --cache %s --prefix %s --userconfig not-exists",
            nexus().getUrl().toExternalForm() + "content/repositories/" + testMethodName(),
            localDirectory.getAbsolutePath(), projectDir.getAbsolutePath());

    log("CMD: {}", cmd);
    final Runtime rt = Runtime.getRuntime();
    final Process npm = rt.exec(cmd);
    final int exitCode = npm.waitFor();

    // Really no clue why npm CLI uses both for non-error output
    final String stdOut = CharStreams.toString(new InputStreamReader(npm.getInputStream(), Charsets.UTF_8));
    final String stdErr = CharStreams.toString(new InputStreamReader(npm.getErrorStream(), Charsets.UTF_8));

    log("STDRR:");
    log(stdErr);

    log("STDOUT:");
    log(stdOut);

    assertThat(stdOut, containsString("commonjs@0.0.1"));
    assertThat(stdOut, containsString("system@0.1.0"));
    assertThat(stdOut, stringContainsInOrder(Arrays.asList("test@0.6.0", "ansi-font@0.0.2")));

    assertThat(exitCode, equalTo(0));
  }
}