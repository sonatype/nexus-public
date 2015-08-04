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
package org.sonatype.nexus.proxy.maven.routing.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRemoteStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.AbstractProxyTestEnvironment;
import org.sonatype.nexus.proxy.EnvironmentBuilder;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.routing.PrefixSource;
import org.sonatype.nexus.proxy.maven.routing.discovery.RemoteStrategy;
import org.sonatype.nexus.proxy.maven.routing.discovery.StrategyResult;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.tests.http.server.api.Behaviour;
import org.sonatype.tests.http.server.fluent.Behaviours;
import org.sonatype.tests.http.server.fluent.Server;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class RemotePrefixFileStrategyTest
    extends AbstractRoutingProxyTest
{
  private static final String HOSTED_REPO_ID = "hosted";

  private static final String PROXY_REPO_ID = "proxy";

  private static final String GROUP_REPO_ID = "group";

  private final int remoteServerPort;

  private Server server;

  public RemotePrefixFileStrategyTest()
      throws Exception
  {
    ServerSocket ss = new ServerSocket(0);
    this.remoteServerPort = ss.getLocalPort();
    ss.close();
  }

  @Override
  public void setUp()
      throws Exception
  {
    this.server =
        Server.withPort(remoteServerPort).serve("/").withBehaviours(Behaviours.error(404)).start();
    super.setUp();
  }

  @Override
  protected EnvironmentBuilder createEnvironmentBuilder()
      throws Exception
  {
    // we need one hosted repo only, so build it
    return new EnvironmentBuilder()
    {
      @Override
      public void startService() {
      }

      @Override
      public void stopService() {
      }

      @Override
      public void buildEnvironment(AbstractProxyTestEnvironment env)
          throws ConfigurationException, IOException, ComponentLookupException
      {
        final PlexusContainer container = env.getPlexusContainer();
        final List<String> reposes = new ArrayList<String>();
        {
          // adding one proxy
          final M2Repository repo = (M2Repository) container.lookup(Repository.class, "maven2");
          CRepository repoConf = new DefaultCRepository();
          repoConf.setProviderRole(Repository.class.getName());
          repoConf.setProviderHint("maven2");
          repoConf.setId(PROXY_REPO_ID);
          repoConf.setName(PROXY_REPO_ID);
          repoConf.setNotFoundCacheActive(true);
          repoConf.setLocalStorage(new CLocalStorage());
          repoConf.getLocalStorage().setProvider("file");
          repoConf.getLocalStorage().setUrl(
              env.getApplicationConfiguration().getWorkingDirectory("proxy/store/" + PROXY_REPO_ID).toURI().toURL()
                  .toString());
          Xpp3Dom ex = new Xpp3Dom("externalConfiguration");
          repoConf.setExternalConfiguration(ex);
          M2RepositoryConfiguration exConf = new M2RepositoryConfiguration(ex);
          exConf.setRepositoryPolicy(RepositoryPolicy.RELEASE);
          exConf.setChecksumPolicy(ChecksumPolicy.STRICT_IF_EXISTS);
          repoConf.setRemoteStorage(new CRemoteStorage());
          repoConf.getRemoteStorage().setProvider(
              env.getRemoteProviderHintFactory().getDefaultHttpRoleHint());
          repoConf.getRemoteStorage().setUrl("http://localhost:" + remoteServerPort + "/");
          repo.configure(repoConf);
          // repo.setCacheManager( env.getCacheManager() );
          reposes.add(repo.getId());
          env.getApplicationConfiguration().getConfigurationModel().addRepository(repoConf);
          env.getRepositoryRegistry().addRepository(repo);
        }
        {
          // adding one hosted
          final M2Repository repo = (M2Repository) container.lookup(Repository.class, "maven2");
          CRepository repoConf = new DefaultCRepository();
          repoConf.setProviderRole(Repository.class.getName());
          repoConf.setProviderHint("maven2");
          repoConf.setId(HOSTED_REPO_ID);
          repoConf.setName(HOSTED_REPO_ID);
          repoConf.setLocalStorage(new CLocalStorage());
          repoConf.getLocalStorage().setProvider("file");
          repoConf.getLocalStorage().setUrl(
              env.getApplicationConfiguration().getWorkingDirectory("proxy/store/" + HOSTED_REPO_ID).toURI().toURL()
                  .toString());
          Xpp3Dom exRepo = new Xpp3Dom("externalConfiguration");
          repoConf.setExternalConfiguration(exRepo);
          M2RepositoryConfiguration exRepoConf = new M2RepositoryConfiguration(exRepo);
          exRepoConf.setRepositoryPolicy(RepositoryPolicy.RELEASE);
          exRepoConf.setChecksumPolicy(ChecksumPolicy.STRICT_IF_EXISTS);
          repo.configure(repoConf);
          reposes.add(repo.getId());
          env.getApplicationConfiguration().getConfigurationModel().addRepository(repoConf);
          env.getRepositoryRegistry().addRepository(repo);
        }
        {
          // add a group
          final M2GroupRepository group =
              (M2GroupRepository) container.lookup(GroupRepository.class, "maven2");
          CRepository repoGroupConf = new DefaultCRepository();
          repoGroupConf.setProviderRole(GroupRepository.class.getName());
          repoGroupConf.setProviderHint("maven2");
          repoGroupConf.setId(GROUP_REPO_ID);
          repoGroupConf.setName(GROUP_REPO_ID);
          repoGroupConf.setLocalStorage(new CLocalStorage());
          repoGroupConf.getLocalStorage().setProvider("file");
          repoGroupConf.getLocalStorage().setUrl(
              env.getApplicationConfiguration().getWorkingDirectory("proxy/store/test").toURI().toURL().toString());
          Xpp3Dom exGroupRepo = new Xpp3Dom("externalConfiguration");
          repoGroupConf.setExternalConfiguration(exGroupRepo);
          M2GroupRepositoryConfiguration exGroupRepoConf = new M2GroupRepositoryConfiguration(exGroupRepo);
          exGroupRepoConf.setMemberRepositoryIds(reposes);
          exGroupRepoConf.setMergeMetadata(true);
          group.configure(repoGroupConf);
          env.getApplicationConfiguration().getConfigurationModel().addRepository(repoGroupConf);
          env.getRepositoryRegistry().addRepository(group);
        }
      }
    };
  }

  protected String prefixFile1(boolean withComments) {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw);
    pw.println(TextFilePrefixSourceMarshaller.MAGIC);
    if (withComments) {
      pw.println("# This is mighty prefix file!");
    }
    pw.println("/org/apache/maven");
    pw.println("/org/sonatype");
    if (withComments) {
      pw.println(" # Added later");
    }
    pw.println("/eu/flatwhite");
    return sw.toString();
  }

  @Test
  public void discoverPlaintextPrefixFile()
      throws Exception
  {
    server.stop();
    server =
        Server.withPort(remoteServerPort).serve("/.meta/prefixes.txt").withBehaviours(
            Behaviours.content(prefixFile1(true))).start();
    try {
      final RemoteStrategy subject = lookup(RemoteStrategy.class, RemotePrefixFileStrategy.ID);
      final StrategyResult result =
          subject.discover(getRepositoryRegistry().getRepositoryWithFacet(PROXY_REPO_ID,
              MavenProxyRepository.class));
      assertThat(result.getMessage(),
          equalTo("Remote publishes prefix file (is less than a day old), using it."));

      final PrefixSource entrySource = result.getPrefixSource();
      assertThat(entrySource.supported(), is(true));
      assertThat(entrySource.readEntries(), contains("/org/apache/maven", "/org/sonatype", "/eu/flatwhite"));
      assertThat(entrySource.readEntries().size(), equalTo(3));
    }
    finally {
      server.stop();
    }
  }

  @Test
  public void discoverGzContentEncodingPrefixFile()
      throws Exception
  {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final GZIPOutputStream gos = new GZIPOutputStream(bos);
    gos.write(prefixFile1(true).getBytes(Charset.forName("UTF-8")));
    gos.flush();
    gos.close();

    server.stop();
    server = Server.withPort(remoteServerPort).serve("/.meta/prefixes.txt").withBehaviours(new Behaviour()
    {
      @Override
      public boolean execute(HttpServletRequest request, HttpServletResponse response, Map<Object, Object> ctx)
          throws Exception
      {
        response.setHeader("Content-Encoding", "gzip");
        return true;
      }
    }, Behaviours.content(bos.toByteArray())).start();
    try {
      final RemoteStrategy subject = lookup(RemoteStrategy.class, RemotePrefixFileStrategy.ID);
      final StrategyResult result =
          subject.discover(getRepositoryRegistry().getRepositoryWithFacet(PROXY_REPO_ID,
              MavenProxyRepository.class));
      assertThat(result.getMessage(),
          equalTo("Remote publishes prefix file (is less than a day old), using it."));

      final PrefixSource entrySource = result.getPrefixSource();
      assertThat(entrySource.supported(), is(true));
      assertThat(entrySource.readEntries(), contains("/org/apache/maven", "/org/sonatype", "/eu/flatwhite"));
      assertThat(entrySource.readEntries().size(), equalTo(3));
    }
    finally {
      server.stop();
    }
  }

  /**
   * https://issues.sonatype.org/browse/NXCM-5188 Strict Checksum enforcement breaks Automatic Routing
   * <p>
   * Prefix file retrieval uses plain Proxy transport to get the file from remote peer. This causes problems
   * (actually
   * prevents happening it) if following conditions are met: Proxy repository has STRICT checksum policy and remote
   * prefix file has no checksum published. In this case, relaxing the policy for this request only is okay to do,
   * since "this" Nexus will properly process the prefix file anyway, detecting any problems with it, thus protecting
   * downstream clients too.
   */
  @Test
  public void discoverPlaintextPrefixFileWithoutChecksumsWithStrictChecksumPolicy()
      throws Exception
  {
    server.stop();
    server =
        Server.withPort(remoteServerPort).serve("/.meta/prefixes.txt").withBehaviours(
            Behaviours.content(prefixFile1(true))).start();
    try {
      // setting the policy to STRICT, and note that server set up above publishes
      // the prefix file only, no checksums!
      final MavenProxyRepository mavenProxyRepository =
          getRepositoryRegistry().getRepositoryWithFacet(PROXY_REPO_ID, MavenProxyRepository.class);
      mavenProxyRepository.setChecksumPolicy(ChecksumPolicy.STRICT);
      getApplicationConfiguration().saveConfiguration();

      final RemoteStrategy subject = lookup(RemoteStrategy.class, RemotePrefixFileStrategy.ID);
      final StrategyResult result = subject.discover(mavenProxyRepository);
      assertThat(result.getMessage(),
          equalTo("Remote publishes prefix file (is less than a day old), using it."));

      final PrefixSource entrySource = result.getPrefixSource();
      assertThat(entrySource.supported(), is(true));
      assertThat(entrySource.readEntries(), contains("/org/apache/maven", "/org/sonatype", "/eu/flatwhite"));
      assertThat(entrySource.readEntries().size(), equalTo(3));
    }
    finally {
      server.stop();
    }
  }

}
