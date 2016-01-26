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

import org.sonatype.nexus.ruby.client.RubyProxyRepository;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.testsuite.ruby.TestUtils.lastLine;
import static org.sonatype.nexus.testsuite.ruby.TestUtils.numberOfLines;

public class GemLifecycleIT
    extends RubyITSupport
{

  private static final String GEMSPROXY = "gemsproxy";
  private static final String GEMSHOST = "gemshost";

  public GemLifecycleIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Test
  public void uploadGemWithNexusGemCommand() throws Exception {
    uploadGemWithNexusGemCommand(GEMSHOST);
    uploadGemWithNexusGemCommand(GEMSPROXY);
    uploadGemWithNexusGemCommand("gemshostgroup");
    uploadGemWithNexusGemCommand("gemsproxygroup");
    uploadGemWithNexusGemCommand("gemsgroup");
  }

  private void uploadGemWithNexusGemCommand(String repoId) throws Exception {
    log("== START {}", repoId);
    cleanup();
    repositories().get(RubyProxyRepository.class, "gemsproxy").withMetadataMaxAge(0).withNotFoundCacheTTL(0).save();

    // start with an empty repo
    assertFileDownloadSize(repoId, "latest_specs.4.8.gz", equalTo(24l));
    assertFileDownloadSize(repoId, "specs.4.8.gz", equalTo(24l));

    Thread.sleep(900L); // give chance for "cached" and "remote" file timestamps to drift as Last-Modified header has second resolution

    File nexusGem = installLatestNexusGem();

    String gemName = "gems/" + nexusGem.getName();
    String gemspecName = "quick/Marshal.4.8/" + nexusGem.getName() + "spec.rz";

    // make sure our gem is not on the repository
    assertFileDownload(repoId, gemName, is(false));
    assertFileDownload(repoId, gemspecName, is(false));

    // upload gem to gemshost - repoId is hardcoded into config-file
    File config = new File(getBundleTargetDirectory(), ".gem/nexus");
    String nx1 = gemRunner().nexus(config, nexusGem);
    String nx2 = gemRunner().nexus(config, nexusGem);
    assertThat(nx1, lastLine(nx1), equalTo("Created"));
    assertThat(nx2, lastLine(nx2), endsWith("not allowed"));

    assertGem(repoId, nexusGem.getName());

    // now we have one remote gem - i.e. a bigger size of the index files
    assertFileDownloadSize(repoId, "latest_specs.4.8.gz", equalTo(76l));
    assertFileDownloadSize(repoId, "specs.4.8.gz", equalTo(76l));

    // reinstall the gem from repository
    assertThat(lastLine(gemRunner().install(repoId, "nexus")), equalTo("1 gem installed"));

    File winGem = testData().resolveFile("win.gem");
    // mismatch filenames on upload
    assertThat(lastLine(gemRunner().nexus(config, winGem)), equalTo("something went wrong"));

    // delete nexus gem
    moreAsserts(repoId, gemName, gemspecName, 
        "api/v1/dependencies/" + nexusGem.getName().replaceFirst("-.*$", ".ruby"));

    // now use the "right" filename on upload
    winGem = testData().resolveFile("win-2-x86-mswin32-60.gem");
    assertThat(lastLine(gemRunner().nexus(config, winGem)), equalTo("Created"));
    assertGem(repoId, winGem.getName());

    if (repoId.equals(GEMSHOST)) {
      doTestRebuildRubygemsMetadata();
    }
    else if (repoId.equals(GEMSPROXY)) {
      doTestPurgeBrokenFiles();
    }

    // just cleanup remaining gems on hosted
    assertFileRemoval(GEMSHOST, "gems/" + winGem.getName(), is(true));

    log("== END {}", repoId);
  }

  private void doTestPurgeBrokenFiles() throws InterruptedException, IOException {
    String repoId = GEMSPROXY;
    File storage = new File(nexus().getWorkDirectory(), "storage/" + repoId);

    File gemspecRz = new File(storage, "quick/Marshal.4.8/w/win-2-x86-mswin32-60.gemspec.rz");
    FileUtils.write(gemspecRz, "");

    File dependency = new File(storage, "api/v1/dependencies/win.ruby");
    FileUtils.write(dependency, "hop");

    scheduler().run("PurgeBrokenRubygemsMetadataTask", null);
    Thread.sleep(200); // give some time to server to at least perform schedule
    scheduler().waitForAllTasksToStop(); // wait for it

    assertThat("deleted corrupted gemspec.rz file", gemspecRz.exists(), is(false));
    assertThat("deleted corrupted dependency file", dependency.exists(), is(false));
  }

  private void doTestRebuildRubygemsMetadata() throws InterruptedException, IOException {
    String repoId = GEMSHOST;
    assertFileDownloadSize(repoId, "latest_specs.4.8.gz", equalTo(80l));
    assertFileDownloadSize(repoId, "specs.4.8.gz", equalTo(80l));
    assertFileDownloadSize(repoId, "prerelease_specs.4.8.gz", equalTo(24l));

    File storage = new File(nexus().getWorkDirectory(), "storage/" + repoId);
    assertThat("delete specs.4.8.gz", new File(storage, "specs.4.8.gz").delete());
    assertThat("delete latest_specs.4.8.gz", new File(storage, "latest_specs.4.8.gz").delete());
    assertThat("delete prerelease_specs.4.8.gz", new File(storage, "prerelease_specs.4.8.gz").delete());

    File gemspecRz = new File(storage, "quick/Marshal.4.8/w/win-2-x86-mswin32-60.gemspec.rz");
    long len = gemspecRz.length();
    FileUtils.write(gemspecRz, "");

    File dependency = new File(storage, "api/v1/dependencies/hulohup.ruby");
    FileUtils.write(dependency, "hop");

    scheduler().run("RebuildRubygemsMetadataTask", null);
    Thread.sleep(200); // give some time to server to at least perform schedule
    scheduler().waitForAllTasksToStop(); // wait for it

    assertFileDownloadSize(repoId, "latest_specs.4.8.gz", equalTo(80l));
    assertFileDownloadSize(repoId, "specs.4.8.gz", equalTo(80l));
    assertFileDownloadSize(repoId, "prerelease_specs.4.8.gz", equalTo(24l));
    assertThat("recreated gemspec.rz", gemspecRz.length(), equalTo(len));
    assertThat("deleted corrupted dependency file", dependency.exists(), is(false));
  }

  private void deleteHostedFiles(String gemName, String gemspecName, String dependencyName) throws IOException {
    String repoId = GEMSHOST;
    // can not delete gemspec files
    assertFileRemoval(repoId, gemspecName, is(false));

    assertFileDownload(repoId, gemName, is(true));
    assertFileDownload(repoId, gemspecName, is(true));
    assertFileDownload(repoId, dependencyName, is(true));

    // can delete gem files which also deletes the associated files
    assertFileRemoval(repoId, gemName, is(true));

    assertFileDownload(repoId, gemName, is(false));
    assertFileDownload(repoId, gemspecName, is(false));
    // the dependency files exist also for none-existing gems
    assertFileDownload(repoId, dependencyName, is(true));

    // TODO specs index files
  }

  private void deleteProxiedFiles(String gemName, String gemspecName, String dependencyName) throws IOException {
    String repoId = GEMSPROXY;
    gemName = gemName.replace("nexus", "n/nexus");
    gemspecName = gemspecName.replace("nexus", "n/nexus");

    // can delete any file
    assertFileRemoval(repoId, gemspecName, is(true));
    assertFileRemoval(repoId, gemspecName, is(false));

    assertFileRemoval(repoId, gemName, is(true));
    assertFileRemoval(repoId, gemName, is(false));

    assertFileRemoval(repoId, dependencyName, is(true));
    assertFileRemoval(repoId, dependencyName, is(false));

    // after delete the file will be fetched from the source again
    assertFileDownload(repoId, gemName, is(true));
    assertFileDownload(repoId, gemspecName, is(true));
    assertFileDownload(repoId, dependencyName, is(true));

    // just clean up
    assertFileRemoval(repoId, gemName, is(true));
    assertFileRemoval(repoId, gemspecName, is(true));

    // TODO specs index files
  }

  private void moreAsserts(String repoId, String gemName, String gemspecName, String dependencyName) throws IOException {
    if (repoId.contains("proxy")) {
      deleteProxiedFiles(gemName, gemspecName, dependencyName);
    }
    deleteHostedFiles(gemName, gemspecName, dependencyName);
  }
}
