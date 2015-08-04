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
package org.sonatype.nexus.testsuite.ruby;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;

import org.sonatype.nexus.ruby.client.RubyProxyRepository;
import org.sonatype.nexus.ruby.cuba.api.ApiV1DependenciesCuba;

import org.hamcrest.Matcher;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.client.core.condition.NexusStatusConditions.any27AndLater;
import static org.sonatype.nexus.testsuite.ruby.TestUtils.lastLine;

public class DownloadsOnEmptyRepositoriesIT
    extends RubyITSupport
{
  public DownloadsOnEmptyRepositoriesIT(String repoId) {
    super(repoId);
  }

  @Test
  public void uploadGemWithPushCommand() throws Exception {
    assertThatDependencyFileLengthForHosted(equalTo(4l));
    assertThatDependencyFileLengthForProxies(equalTo(4l));

    // make sure the credentials file has the right permissions otherwise the push command fails silently
    Files.setPosixFilePermissions(new File(getBundleTargetDirectory(), ".gem/credentials").toPath(),
        PosixFilePermissions.fromString("rw-------"));

    File gem = testData().resolveFile("pre-0.1.0.beta.gem");
    assertThat(lastLine(gemRunner().push("gemshost", gem)),
        equalTo("Pushing gem to http://127.0.0.1:"+nexus().getPort()+"/nexus/content/repositories/gemshost..."));

    assertGem("gemshost", gem.getName());

    // install the "pre" gem from repository
    assertThat(lastLine(gemRunner().install("gemshost", "--pre", "pre")), equalTo("1 gem installed"));

    assertThatDependencyFileLengthForHosted(greaterThan(4l));
    assertThatDependencyFileLengthForProxies(equalTo(4l));

    repositories().get(RubyProxyRepository.class, "gemsproxy").withMetadataMaxAge(0).save();

    assertThatDependencyFileLengthForHosted(greaterThan(4l));
    assertThatDependencyFileLengthForProxies(greaterThan(4l));
  }

  private void assertThatDependencyFileLengthForHosted(Matcher<Long> matcher) {
    assertThatDependencyFileLength("gemshost", matcher);
    assertThatDependencyFileLength("gemshostgroup", matcher);
    assertThatDependencyFileLength("gemsgroup", matcher);
  }

  private void assertThatDependencyFileLengthForProxies(Matcher<Long> matcher) {
    assertThatDependencyFileLength("gemsproxy", matcher);
    assertThatDependencyFileLength("gemsproxygroup", matcher);
  }

  private void assertThatDependencyFileLength(String repoId, Matcher<Long> matcher) {
    File pre = assertFileDownload(repoId, "api/v1/dependencies/pre" + ApiV1DependenciesCuba.RUBY, is(true));
    assertThat("length of dependencies file of repo: " + repoId, pre.length(), matcher);
  }

  @Test
  public void download() throws Exception {
    download("gemshost");
    download("gemsproxy");
    download("gemshostgroup");
    download("gemsproxygroup");
    download("gemsgroup");
  }

  private void download(String repoId) throws Exception {
    log("== START {}", repoId);
    assertAllSpecsIndexDownload(repoId);
    // on an empty repo these directories still missing
    assertFileDownload(repoId, "/gems", is(true));
    assertFileDownload(repoId, "/quick", is(true));
    assertFileDownload(repoId, "/api", is(true));
    assertFileDownload(repoId, "/maven", is(true));
    log("== END {}", repoId);
  }

  private void assertAllSpecsIndexDownload(String repoId) throws IOException {
    assertSpecsIndexdownload(repoId, "specs");
    assertSpecsIndexdownload(repoId, "prerelease_specs");
    assertSpecsIndexdownload(repoId, "latest_specs");
  }

  private void assertSpecsIndexdownload(String repoId, String name) throws IOException {
    if (any27AndLater().isSatisfiedBy(client().getNexusStatus())) {
      // skip this test for 2.6.x nexus :
      // something goes wrong but that is a formal feature not used by any ruby client
      assertFileDownload(repoId, "/" + name + ".4.8", is(true));
    }
    assertFileDownload(repoId, "/" + name + ".4.8.gz", is(true));
  }
}