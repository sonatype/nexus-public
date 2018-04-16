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

import javax.inject.Inject;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.client.core.exception.NexusClientBadRequestException;
import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.core.subsystem.content.Content;
import org.sonatype.nexus.client.core.subsystem.content.Location;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.rest.BaseUrl;
import org.sonatype.nexus.ruby.BundleRunner;
import org.sonatype.nexus.ruby.GemRunner;
import org.sonatype.nexus.ruby.client.RubyGroupRepository;
import org.sonatype.nexus.ruby.client.RubyHostedRepository;
import org.sonatype.nexus.ruby.client.RubyProxyRepository;
import org.sonatype.nexus.testsuite.client.Scheduler;
import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy.Strategy;
import org.sonatype.sisu.filetasks.FileTaskBuilder;

import org.hamcrest.Matcher;
import org.jruby.embed.ScriptingContainer;
import org.junit.After;
import org.junit.Before;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.sonatype.sisu.filetasks.builder.FileRef.file;

@NexusStartAndStopStrategy(Strategy.EACH_TEST)
public abstract class RubyITSupport
    extends NexusRunningParametrizedITSupport
{

  @Inject
  protected FileTaskBuilder overlays;

  protected GemRunner gemRunner;

  protected ScriptingContainer ruby;

  private BundleRunner bundleRunner;

  public RubyITSupport(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  // ==

  // TODO: this might be moved to some subclass. Current Ruby ITs did use almost same config, but it might change in future
  @Before
  public void mayCreateRepositories() {
    try {
      // ask for repo
      repositories().get("gemshost");
    }
    catch (NexusClientNotFoundException e) {
      // create these ONLY if not exists on remote
      // as on test classes with multiple test methods @Before will be called multiple time while NX started only once
      final RubyHostedRepository gemshost = createRubyHostedRepository("gemshost");
      final RubyProxyRepository gemsproxy = createRubyProxyRepository("gemsproxy", gemshost.contentUri());
      createRubyGroupRepository("gemshostgroup", gemshost.id());
      createRubyGroupRepository("gemsproxygroup", gemsproxy.id());
      createRubyGroupRepository("gemsgroup", gemshost.id(), gemsproxy.id());
    }
  }

  @Before
  public void configureScriptingContainer() {
    overlays.create()
        .file(file(new File(getBundleTargetDirectory(), ".gem/nexus")))
        .containing("---\n" +
            ":url: http://localhost:" + nexus().getPort() + "/nexus/content/repositories/gemshost\n" +
            ":authorization: Basic YWRtaW46YWRtaW4xMjM=\n")
        .run();
    overlays.create()
        .file(file(new File(getBundleTargetDirectory(), ".gem/credentials")))
        .containing("---\n" +
            ":rubygems_api_key: blablablabla\n" +
            ":test: Basic YWRtaW46YWRtaW4xMjM=\n").run();
  }

  @After
  public void cleanup() {
    if (ruby != null) {
      ruby.terminate();
      bundleRunner = null;
      gemRunner = null;
    }
  }

  // ==

  protected File getBundleTargetDirectory() {
    return nexus().getConfiguration().getTargetDirectory();
  }

  private ScriptingContainer ruby() {
    if (this.ruby == null) {
      this.ruby = createScriptingContainer();
    }
    return this.ruby;
  }

  protected ScriptingContainer createScriptingContainer() {
    return new ITestJRubyScriptingContainer(getBundleTargetDirectory(),
        new File(getBundleTargetDirectory(), "rubygems"));
  }

  protected GemRunner gemRunner() {
    if (gemRunner == null) {
      gemRunner = createGemRunner();
    }
    return gemRunner;
  }

  protected BundleRunner bundleRunner() {
    if (bundleRunner == null) {
      bundleRunner = createBundleRunner();
    }
    return bundleRunner;
  }

  GemRunner createGemRunner() {
    BaseUrl base = client().getConnectionInfo().getBaseUrl();
    return new GemRunner(ruby(), base.toString() + "content/repositories/");
  }

  BundleRunner createBundleRunner() {
    return new BundleRunner(ruby());
  }

  protected File assertFileDownload(String repoId, String name, Matcher<Boolean> matcher) {
    File download = downloadFile(repoId, name);
    // from version 2.4.0-03 onwards count empty files as non-existing
    assertThat(name, download.exists() && download.length() > 0, matcher);
    return download;
  }

  protected File assertFileDownloadSize(String repoId, String name, Matcher<Long> matcher) {
    File download = downloadFile(repoId, name);
    assertThat("exists " + name, download.exists(), is(true));
    assertThat("size of " + name, download.length(), matcher);
    download.deleteOnExit();
    return download;
  }

  protected File assertSpecsDownload(String repoId, String name, Object... expected) {
    File download = downloadFile(repoId, name);
    assertThat("exists " + name, download.exists(), is(true));
    // unpack index and consolidate into easy to compare format
    Object[] specs = (Object[]) ruby().runScriptlet("Marshal.load(Gem.gunzip(File.new('" + download.toPath() + "').read)).collect { |c| \"#{c[0]}-#{c[1]}-#{c[2]}\" }.to_java");
    assertThat("specs", specs, is(expected));
    download.deleteOnExit();
    return download;
  }


  protected File downloadFile(String repoId, String name) {
    File download = new File(util.createTempDir(), "null");
    try {
      content().download(new Location(repoId, name), download);
    }
    catch (Exception e) {
      // just ignore it and let matcher test
    }
    download.deleteOnExit();
    return download;
  }

  protected void assertFileRemoval(String repoId, String name, Matcher<Boolean> matcher) throws IOException {
      try {
        content().delete(new Location(repoId, name));
        assertThat(name, true, matcher);
      }
      catch (NexusClientBadRequestException e) {
        // no allowed
        assertThat(name, false, matcher);
      }
      catch (NexusClientNotFoundException e) {
        // not found
        assertThat(name, false, matcher);
      }
  }

  @Override
  protected NexusBundleConfiguration configureNexus(final NexusBundleConfiguration configuration) {
    return configuration
        .setLogLevel("org.sonatype.nexus.ruby", "TRACE")
        .setLogLevel("org.sonatype.nexus.plugins.ruby", "TRACE")
        .setLogLevel("remote.storage.outbound", "DEBUG")
        .addPlugins(
            artifactResolver().resolvePluginFromDependencyManagement(
                "org.sonatype.nexus.plugins", "nexus-ruby-plugin"
            )
        );
  }

  protected File installLatestNexusGem() {
    return installLatestNexusGem(false);
  }

  protected File installLatestNexusGem(boolean withBundler) {
    //nexus gem
    File nexusGem;
    try {
      nexusGem = artifactResolver().resolveFromDependencyManagement("rubygems", "nexus", "gem", null, null, null);
    }
    catch (RuntimeException e) {
      throw new RuntimeException("maybe you forgot to install nexus gem", e);
    }

    if (withBundler) {
      // install nexus + bundler gem
      File bundlerGem = artifactResolver()
          .resolveFromDependencyManagement("rubygems", "bundler", "gem", null, null, null);
      gemRunner().install(nexusGem, bundlerGem);
    }
    else {
      // install nexus gem
      gemRunner().install(nexusGem);
    }
    assertThat(gemRunner().list().toString(), containsString("nexus ("));

    return nexusGem;
  }

  // == Client

  protected Scheduler scheduler() {
    return client().getSubsystem(Scheduler.class);
  }

  protected Content content() {
    return client().getSubsystem(Content.class);
  }

  protected Repositories repositories() {
    return client().getSubsystem(Repositories.class);
  }

  protected RubyHostedRepository createRubyHostedRepository(final String id) {
    checkNotNull(id);
    return repositories().create(RubyHostedRepository.class, id).withName(id).save();
  }

  protected RubyProxyRepository createRubyProxyRepository(final String id, final String url) {
    checkNotNull(id);
    checkNotNull(url);
    return repositories().create(RubyProxyRepository.class, id).withName(id).asProxyOf(url).save();
  }

  protected RubyGroupRepository createRubyGroupRepository(final String id, final String... members) {
    checkNotNull(id);
    return repositories().create(RubyGroupRepository.class, id).withName(id).addMember(members).save();
  }

  protected void assertGem(String repoId, String name) {
    String gemName = "gems/" + name;
    String gemspecName = "quick/Marshal.4.8/" + name + "spec.rz";
    String dependencyName = "api/v1/dependencies/" + name.replaceFirst("-.*$", ".ruby");

    assertFileDownload(repoId, gemName, is(true));
    assertFileDownload(repoId, gemspecName, is(true));
    assertFileDownload(repoId, "api/" + gemName, is(true));
    assertFileDownload(repoId, "api/" + gemspecName, is(true));
    assertFileDownload(repoId, dependencyName, is(true));
  }
}
