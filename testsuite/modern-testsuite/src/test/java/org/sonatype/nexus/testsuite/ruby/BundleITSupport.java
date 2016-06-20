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

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.client.core.subsystem.content.Location;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.sonatype.nexus.client.core.condition.NexusStatusConditions.any27AndLater;
import static org.sonatype.nexus.testsuite.ruby.TestUtils.lastLine;
import static org.sonatype.sisu.filetasks.builder.FileRef.file;
import static org.sonatype.sisu.filetasks.builder.FileRef.path;

public abstract class BundleITSupport
    extends RubyITSupport
{
  private String repoId;

  public BundleITSupport(final String nexusBundleCoordinates, final String repoId) {
    super(nexusBundleCoordinates);
    this.repoId = repoId;
  }

  @Override
  protected ITestJRubyScriptingContainer createScriptingContainer() {
    return new ITestJRubyScriptingContainer(getBundleTargetDirectory(), new File(getBundleTargetDirectory(), "rubygems"),
        new File(getBundleTargetDirectory(), "project/Gemfile"));
  }

  @Before
  public void configureBundler() {
    overlays.copy()
        .directory(file(testData().resolveFile("project")))
        .to().directory(file(new File(getBundleTargetDirectory(), "project"))).run();
    overlays.create()
        .file(file(new File(getBundleTargetDirectory(), "project/bundle/config")))
        .containing("---\nBUNDLE_MIRROR__HTTP://RUBYGEMS.ORG: " +
            "http://localhost:"+ nexus().getPort() +"/nexus/content/repositories/" + repoId + "/").run();
    overlays.rename(file(new File(getBundleTargetDirectory(), "project/bundle"))).to(".bundle").run();
  }

  @Test
  public void nbundleCommand() throws IOException {
    installLatestNexusGem(true);

    assertThat(bundleRunner().config(), containsString("mirror.http://rubygems.org"));
    assertThat(bundleRunner().config(),
        containsString("http://localhost:" + nexus().getPort() + "/nexus/content/repositories/" + repoId));

    String out = bundleRunner().install();

    assertThat(out, containsString("Your bundle is complete!"));
    assertThat(lastLine(out), is("Use `bundle show [gemname]` to see where a bundled gem is installed."));

    // assure that bundle support is working
    assertThat(out, not(containsString("Fetching full source index from http://localhost:4711")));

    assertHostedFiles();

    // TODO check storage to be empty
  }

  protected void checkUpdateOfMissingDependencies() throws IOException {
    Location preAndZip = new Location(repoId, "/api/v1/dependencies?gems=pre,zip");
    File download = new File(util.createTempDir(), "null");
    content().download(preAndZip, download);

    // remove one dependencies file
    Location zip = new Location(repoId, "/api/v1/dependencies/zip.ruby");
    content().delete(zip);

    // make sure that the deleted dependencies file get resurrected
    content().download(preAndZip, download);
    assertThat(content().exists(zip), is(true));

    assertThat(download.length(), equalTo(130l));
  }

  protected File assertFileDownload(String name, Integer len) throws IOException {
    File f = assertFileDownload(repoId, name, is(len != null));
    if (f != null) {
      assertThat((int) f.length(), equalTo(len));
    }
    else {
      Assert.fail("could not read ");
    }
    return f;
  }

  protected void assertHostedFiles() throws IOException {
    // bundler uses this to check existence of the api/v1
    File download = downloadFile(repoId, "/api/v1/dependencies");
    assertThat(download.exists(), is(true));
    assertThat(download.length(), is(0l));

    assertFileDownload("/gems/z/zip-2.0.2.gem", 64000);
    assertFileDownload("/gems/zip-2.0.2.gem", 64000);
    assertFileDownload("/api/v1/dependencies/z/zip.ruby", 80);
    assertFileDownload("/api/v1/dependencies/zip.ruby", 80);
    assertFileDownload("/api/v1/dependencies?gems=zip", 80);
    assertFileDownload("/quick/Marshal.4.8/z/zip-2.0.2.gemspec.rz", 359);
    assertFileDownload("/quick/Marshal.4.8/zip-2.0.2.gemspec.rz", 359);
    if (any27AndLater().isSatisfiedBy(client().getNexusStatus())) {
      // skip this test for 2.6.x nexus :
      // something goes wrong and this feature is undocumented and not complete
      assertFileDownload("/maven/releases/rubygems/zip/maven-metadata.xml", 223);
      assertFileDownload("/maven/releases/rubygems/zip/maven-metadata.xml.sha1", 40);
      assertFileDownload("/maven/prereleases/rubygems/zip/maven-metadata.xml", 192);
      assertFileDownload("/maven/prereleases/rubygems/zip/maven-metadata.xml.sha1", 40);
      assertFileDownload("/maven/releases/rubygems/pre/maven-metadata.xml", 192);
      assertFileDownload("/maven/releases/rubygems/pre/maven-metadata.xml.sha1", 40);
      assertFileDownload("/maven/releases/rubygems/hufflepuf/maven-metadata.xml", 260);
      assertFileDownload("/maven/releases/rubygems/hufflepuf/maven-metadata.xml.sha1", 40);
      assertFileDownload("/maven/prereleases/rubygems/pre/maven-metadata.xml", 237);
      assertFileDownload("/maven/prereleases/rubygems/pre/maven-metadata.xml.sha1", 40);
      assertFileDownload("/maven/prereleases/rubygems/pre/0.1.0.beta-SNAPSHOT/maven-metadata.xml", 740);
      assertFileDownload("/maven/prereleases/rubygems/pre/0.1.0.beta-SNAPSHOT/maven-metadata.xml.sha1", 40);
      assertFileDownload("/maven/releases/rubygems/zip/2.0.2/zip-2.0.2.pom", 1633);
      assertFileDownload("/maven/releases/rubygems/zip/2.0.2/zip-2.0.2.pom.sha1", 40);
      // TODO this is wrong it should not be a snapshot version in this pom !!!!
      assertFileDownload("/maven/releases/rubygems/pre/0.1.0.beta/pre-0.1.0.beta.pom", 1206);
      assertFileDownload("/maven/releases/rubygems/pre/0.1.0.beta/pre-0.1.0.beta.pom.sha1", 40);
      assertFileDownload("/maven/prereleases/rubygems/pre/0.1.0.beta-SNAPSHOT/pre-0.1.0.beta-SNAPSHOT.pom", 1215);
      assertFileDownload("/maven/prereleases/rubygems/pre/0.1.0.beta-SNAPSHOT/pre-0.1.0.beta-SNAPSHOT.pom.sha1", 40);
      assertFileDownload("/maven/releases/rubygems/zip/2.0.2/zip-2.0.2.gem", 64000);
      assertFileDownload("/maven/releases/rubygems/zip/2.0.2/zip-2.0.2.gem.sha1", 40);
      assertFileDownload("/maven/releases/rubygems/pre/0.1.0.beta/pre-0.1.0.beta.gem", 3584);
      assertFileDownload("/maven/releases/rubygems/pre/0.1.0.beta/pre-0.1.0.beta.gem.sha1", 40);
      assertFileDownload("/maven/prereleases/rubygems/pre/0.1.0.beta-SNAPSHOT/pre-0.1.0.beta-SNAPSHOT.gem", 3584);
      assertFileDownload("/maven/prereleases/rubygems/pre/0.1.0.beta-SNAPSHOT/pre-0.1.0.beta-SNAPSHOT.gem.sha1", 40);
    }
  }

  @Override
  protected NexusBundleConfiguration configureNexus(NexusBundleConfiguration configuration) {
    configuration = super.configureNexus(configuration);
    return configuration
        .addOverlays(
            overlays.copy()
                .directory(file(testData().resolveFile("repo")))
                .to().directory(path("sonatype-work/nexus/storage/gemshost"))
        );
  }
}
