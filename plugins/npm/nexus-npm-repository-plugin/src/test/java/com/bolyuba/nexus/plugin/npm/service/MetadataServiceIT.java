/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.apachehttpclient.Hc4Provider;
import org.sonatype.nexus.configuration.application.ApplicationDirectories;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.cache.PathCache;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.PreparedContentLocator;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.RepositoryItemUidLock;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;
import org.sonatype.nexus.proxy.storage.remote.httpclient.HttpClientManager;
import org.sonatype.nexus.web.BaseUrlHolder;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.bolyuba.nexus.plugin.npm.group.DefaultNpmGroupRepository;
import com.bolyuba.nexus.plugin.npm.group.NpmGroupRepository;
import com.bolyuba.nexus.plugin.npm.group.NpmGroupRepositoryConfigurator;
import com.bolyuba.nexus.plugin.npm.hosted.DefaultNpmHostedRepository;
import com.bolyuba.nexus.plugin.npm.hosted.NpmHostedRepository;
import com.bolyuba.nexus.plugin.npm.hosted.NpmHostedRepositoryConfigurator;
import com.bolyuba.nexus.plugin.npm.proxy.DefaultNpmProxyRepository;
import com.bolyuba.nexus.plugin.npm.proxy.NpmProxyRepository;
import com.bolyuba.nexus.plugin.npm.proxy.NpmProxyRepositoryConfigurator;
import com.bolyuba.nexus.plugin.npm.service.internal.MetadataParser;
import com.bolyuba.nexus.plugin.npm.service.internal.MetadataServiceFactoryImpl;
import com.bolyuba.nexus.plugin.npm.service.internal.PackageRootIterator;
import com.bolyuba.nexus.plugin.npm.service.internal.ProxyMetadataTransport;
import com.bolyuba.nexus.plugin.npm.service.internal.orient.OrientMetadataStore;
import com.bolyuba.nexus.plugin.npm.service.internal.proxy.HttpProxyMetadataTransport;
import com.bolyuba.nexus.plugin.npm.service.tarball.TarballSource;
import com.bolyuba.nexus.plugin.npm.service.tarball.internal.HttpTarballTransport;
import com.bolyuba.nexus.plugin.npm.service.tarball.internal.Sha1HashPayloadValidator;
import com.bolyuba.nexus.plugin.npm.service.tarball.internal.SizePayloadValidator;
import com.bolyuba.nexus.plugin.npm.service.tarball.internal.TarballSourceImpl;
import com.bolyuba.nexus.plugin.npm.service.tarball.internal.TarballValidator;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetadataServiceIT
    extends TestSupport
{
  private File tmpDir;

  private NpmHostedRepository npmHostedRepository1;

  private NpmHostedRepository npmHostedRepository2;

  private NpmProxyRepository npmProxyRepository;

  private NpmGroupRepository npmGroupRepository;

  @Mock
  private ApplicationDirectories applicationDirectories;

  @Mock
  private Hc4Provider hc4Provider;

  @Mock
  private HttpClientManager httpClientManager;

  private OrientMetadataStore metadataStore;

  private MetadataServiceFactoryImpl metadataService;

  private MetadataParser metadataParser;

  private ProxyMetadataTransport proxyMetadataTransport;

  private TarballSource tarballSource;

  @Before
  public void setup() throws Exception {
    BaseUrlHolder.set("http://localhost:8081/nexus");
    tmpDir = util.createTempDir();

    final HttpClient httpClient = HttpClients.createDefault();

    when(applicationDirectories.getWorkDirectory(anyString())).thenReturn(tmpDir);
    when(applicationDirectories.getTemporaryDirectory()).thenReturn(tmpDir);
    when(httpClientManager.create(any(ProxyRepository.class), any(RemoteStorageContext.class))).thenReturn(
        httpClient);

    metadataStore = new OrientMetadataStore(applicationDirectories, 10);
    metadataParser = new MetadataParser(applicationDirectories.getTemporaryDirectory());
    // proxy transport but without root fetch, to not harrass registry and make tests dead slow
    proxyMetadataTransport = new HttpProxyMetadataTransport(metadataParser, httpClientManager)
    {
      @Override
      public PackageRootIterator fetchRegistryRoot(final NpmProxyRepository npmProxyRepository) throws IOException {
        return PackageRootIterator.EMPTY;
      }
    };
    metadataService = new MetadataServiceFactoryImpl(metadataStore, metadataParser, proxyMetadataTransport);

    when(hc4Provider.createHttpClient(Mockito.any(RemoteStorageContext.class))).thenReturn(httpClient);

    final HttpTarballTransport httpTarballTransport = new HttpTarballTransport(hc4Provider);

    tarballSource = new TarballSourceImpl(applicationDirectories, httpTarballTransport,
        ImmutableMap.<String, TarballValidator>of(
            "size", new SizePayloadValidator(), "sha1", new Sha1HashPayloadValidator()));

    // dummy uid and uidLock
    final RepositoryItemUid uid = mock(RepositoryItemUid.class);
    when(uid.getLock()).thenReturn(mock(RepositoryItemUidLock.class));

    // not using mock as it would OOM when it tracks invocations, as we work with large files here
    npmHostedRepository1 = new DefaultNpmHostedRepository(mock(ContentClass.class), mock(
        NpmHostedRepositoryConfigurator.class), metadataService)
    {
      @Override
      public String getId() {
        return "hosted1";
      }

      @Override
      public RepositoryItemUid createUid(String path) { return uid; }
    };
    npmHostedRepository2 = new DefaultNpmHostedRepository(mock(ContentClass.class), mock(
        NpmHostedRepositoryConfigurator.class), metadataService)
    {
      @Override
      public String getId() {
        return "hosted2";
      }

      @Override
      public RepositoryItemUid createUid(String path) { return uid; }
    };

    // not using mock as it would OOM when it tracks invocations, as we work with large files here
    npmProxyRepository = new DefaultNpmProxyRepository(mock(ContentClass.class), mock(
        NpmProxyRepositoryConfigurator.class), metadataService, tarballSource)
    {
      @Override
      public String getId() {
        return "proxy";
      }

      @Override
      public boolean isItemAgingActive() { return true; }

      @Override
      public int getItemMaxAge() { return 10; }

      @Override
      public String getRemoteUrl() { return "http://registry.npmjs.org/"; }

      @Override
      public RepositoryItemUid createUid(String path) { return uid; }

      @Override
      public ProxyMode getProxyMode() { return ProxyMode.ALLOW; }

      @Override
      public PathCache getNotFoundCache() { return mock(PathCache.class); }
    };

    // not using mock as it would OOM when it tracks invocations, as we work with large files here
    npmGroupRepository = new DefaultNpmGroupRepository(mock(ContentClass.class), mock(
        NpmGroupRepositoryConfigurator.class), metadataService)
    {
      @Override
      public String getId() {
        return "hosted";
      }

      @Override
      public List<Repository> getMemberRepositories() {
        final List<Repository> result = Lists.newArrayList();
        result.add(npmHostedRepository1);
        result.add(npmHostedRepository2);
        result.add(npmProxyRepository);
        return result;
      }
    };

    metadataStore.start();
  }

  @After
  public void teardown() throws Exception {
    metadataStore.stop();
    BaseUrlHolder.unset();
  }

  /**
   * Simple smoke test that pushes _real_ NPM registry root data into the store and then performs some queries
   * against it. This is huge, as we operate on a 40MB JSON file, database will have around 90k entries.
   */
  @Test
  public void registryRootRoundtrip() throws Exception {
    final ContentLocator input = new PreparedContentLocator(
        new FileInputStream(util.resolveFile("src/test/npm/ROOT_small.json")),
        NpmRepository.JSON_MIME_TYPE, -1);

    // this is "illegal" case using internal stuff, but is for testing only
    metadataStore
        .updatePackages(npmProxyRepository, metadataParser.parseRegistryRoot(npmProxyRepository.getId(), input));

    log("Splice done");
    // we pushed all into DB, now query
    assertThat(metadataStore.listPackageNames(npmProxyRepository), hasSize(4));

    final PackageRoot commonjs = metadataStore.getPackageByName(npmProxyRepository, "commonjs");
    assertThat(commonjs, notNullValue());
  }

  /**
   * Testing deletion from proxy repositories.
   */
  @Test
  public void proxyPackageDeletionRoundtrip() throws Exception {
    final ContentLocator input = new PreparedContentLocator(
        new FileInputStream(util.resolveFile("src/test/npm/ROOT_small.json")),
        NpmRepository.JSON_MIME_TYPE, -1);

    // this is "illegal" case using internal stuff, but is for testing only
    metadataStore
        .updatePackages(npmProxyRepository, metadataParser.parseRegistryRoot(npmProxyRepository.getId(), input));

    log("Splice done");
    // we pushed all into DB, now query
    assertThat(metadataStore.listPackageNames(npmProxyRepository), hasSize(4));
    assertThat(metadataStore.getPackageByName(npmProxyRepository, "ansi-font"), notNullValue());
    assertThat(metadataStore.getPackageByName(npmProxyRepository, "commonjs"), notNullValue());

    npmProxyRepository.getMetadataService().deletePackage("commonjs");

    assertThat(metadataStore.listPackageNames(npmProxyRepository), hasSize(3));
    assertThat(metadataStore.getPackageByName(npmProxyRepository, "ansi-font"), notNullValue());
    assertThat(metadataStore.getPackageByName(npmProxyRepository, "commonjs"), nullValue());

    npmProxyRepository.getMetadataService().deleteAllMetadata();
    assertThat(metadataStore.listPackageNames(npmProxyRepository), hasSize(0));
  }

  /**
   * Testing deletion from hosted repositories.
   */
  @Test
  public void hostedPackageDeletionRoundtrip() throws Exception {
    final ContentLocator input = new PreparedContentLocator(
        new FileInputStream(util.resolveFile("src/test/npm/ROOT_small.json")),
        NpmRepository.JSON_MIME_TYPE, -1);

    // this is "illegal" case using internal stuff, but is for testing only
    metadataStore
        .updatePackages(npmHostedRepository1, metadataParser.parseRegistryRoot(npmHostedRepository1.getId(), input));

    log("Splice done");
    // we pushed all into DB, now query
    assertThat(metadataStore.listPackageNames(npmHostedRepository1), hasSize(4));
    assertThat(metadataStore.getPackageByName(npmHostedRepository1, "ansi-font"), notNullValue());
    assertThat(metadataStore.getPackageByName(npmHostedRepository1, "commonjs"), notNullValue());

    npmHostedRepository1.getMetadataService().deletePackage("commonjs");

    assertThat(metadataStore.listPackageNames(npmHostedRepository1), hasSize(3));
    assertThat(metadataStore.getPackageByName(npmHostedRepository1, "ansi-font"), notNullValue());
    assertThat(metadataStore.getPackageByName(npmHostedRepository1, "commonjs"), nullValue());

    npmHostedRepository1.getMetadataService().deleteAllMetadata();
    assertThat(metadataStore.listPackageNames(npmHostedRepository1), hasSize(0));
  }

  @Test
  public void proxyPackageRootRoundtrip() throws Exception {
    // this call will get it from remote, store, and return it as raw stream
    final StringContentLocator contentLocator = (StringContentLocator) npmProxyRepository.getMetadataService()
        .producePackageVersion(new PackageRequest(new ResourceStoreRequest("/commonjs/0.0.1")));
    JSONObject proxiedV001 = new JSONObject(
        ByteSource.wrap(contentLocator.getByteArray()).asCharSource(Charsets.UTF_8).read());

    // get the one from file
    final File jsonFile = util.resolveFile("src/test/npm/ROOT_commonjs.json");
    JSONObject onDisk = new JSONObject(Files.toString(jsonFile, Charsets.UTF_8));
    onDisk.getJSONObject("versions").getJSONObject("0.0.1").getJSONObject("dist").put("tarball",
        "http://localhost:8081/nexus/content/repositories/proxy/commonjs/-/commonjs-0.0.1.tgz");
    JSONObject versions = onDisk.getJSONObject("versions");
    JSONObject diskV001 = versions.getJSONObject("0.0.1");
    diskV001.remove("_id"); // TODO: See MetadataGenerator#filterPackageVersion
    diskV001.remove("_rev"); // TODO: See MetadataGenerator#filterPackageVersion

    JSONAssert.assertEquals(diskV001, proxiedV001, false);
  }

  /**
   * Simple smoke test that pushes _real_ NPM registry package root and then queries it, like in a hosted repository.
   */
  @Test
  public void hostedPackageRootRoundtrip() throws Exception {
    final File jsonFile = util.resolveFile("src/test/npm/ROOT_commonjs.json");
    final ContentLocator input = new PreparedContentLocator(
        new FileInputStream(jsonFile),
        NpmRepository.JSON_MIME_TYPE, -1);
    final PackageRequest request = new PackageRequest(new ResourceStoreRequest("/commonjs"));
    npmHostedRepository1.getMetadataService()
        .consumePackageRoot(npmHostedRepository1.getMetadataService().parsePackageRoot(
            request, input));

    assertThat(metadataStore.listPackageNames(npmHostedRepository1), hasSize(1));

    final PackageRoot commonjs = metadataStore.getPackageByName(npmHostedRepository1, "commonjs");
    assertThat(commonjs.getName(), equalTo("commonjs"));
    assertThat(commonjs.isUnpublished(), is(false));
    assertThat(commonjs.isIncomplete(), is(false));

    final PackageVersion commonjs_0_0_1 = commonjs.getVersions().get("0.0.1");
    assertThat(commonjs_0_0_1, notNullValue());
    assertThat(commonjs_0_0_1.getName(), equalTo("commonjs"));
    assertThat(commonjs_0_0_1.getVersion(), equalTo("0.0.1"));
    assertThat(commonjs_0_0_1.isIncomplete(), is(false));

    JSONObject onDisk = new JSONObject(Files.toString(jsonFile, Charsets.UTF_8));
    onDisk.remove("_attachments");
    onDisk.remove("_id"); // TODO: See MetadataGenerator#filterPackageVersion
    onDisk.remove("_rev"); // TODO: See MetadataGenerator#filterPackageVersion
    onDisk.getJSONObject("versions").getJSONObject("0.0.1")
        .remove("_id"); // TODO: See MetadataGenerator#filterPackageVersion
    onDisk.getJSONObject("versions").getJSONObject("0.0.1")
        .remove("_rev"); // TODO: See MetadataGenerator#filterPackageVersion
    onDisk.getJSONObject("versions").getJSONObject("0.0.1").getJSONObject("dist")
        .put("tarball", "http://localhost:8081/nexus/content/repositories/hosted1/commonjs/-/commonjs-0.0.1.tgz");
    final StringContentLocator contentLocator = (StringContentLocator) npmHostedRepository1.getMetadataService()
        .producePackageRoot(new PackageRequest(new ResourceStoreRequest("/commonjs")));
    JSONObject onStore = new JSONObject(
        ByteSource.wrap(contentLocator.getByteArray()).asCharSource(Charsets.UTF_8).read());

    JSONAssert.assertEquals(onDisk, onStore, false);
  }

  /**
   * Simple smoke test that pushes _real_ NPM registry packages and then queries them, like in a hosted repository.
   * Checks are made is time object properly maintained or not.
   */
  @Test
  public void hostedPackageTimeMaintenance() throws Exception {
    // deploy 0.0.1
    {
      final File jsonFile = util.resolveFile("src/test/npm/ROOT_commonjs_v1.json");
      final ContentLocator input = new PreparedContentLocator(
          new FileInputStream(jsonFile),
          NpmRepository.JSON_MIME_TYPE, -1);
      final PackageRequest request = new PackageRequest(new ResourceStoreRequest("/commonjs"));

      npmHostedRepository1.getMetadataService()
          .consumePackageRoot(npmHostedRepository1.getMetadataService().parsePackageRoot(
              request, input));
    }

    String created; // carries initial deploy

    // grab deployed one and check
    {
      final PackageRoot commonjs = metadataStore.getPackageByName(npmHostedRepository1, "commonjs");
      assertThat(commonjs.getRaw(), hasKey("time"));
      final Map<String, String> time = (Map<String, String>) commonjs.getRaw().get("time");
      assertThat(time.entrySet(), hasSize(3));
      created = time.get("created");
      assertThat(created, notNullValue());
      assertThat(time, hasEntry("created", created));
      assertThat(time, hasEntry("modified", created));
      assertThat(time, hasEntry("0.0.1", created));
    }

    // deploy 0.0.2
    {
      final File jsonFile = util.resolveFile("src/test/npm/ROOT_commonjs_v2.json");
      final ContentLocator input = new PreparedContentLocator(
          new FileInputStream(jsonFile),
          NpmRepository.JSON_MIME_TYPE, -1);
      final PackageRequest request = new PackageRequest(new ResourceStoreRequest("/commonjs"));

      npmHostedRepository1.getMetadataService()
          .consumePackageRoot(npmHostedRepository1.getMetadataService().parsePackageRoot(
              request, input));
    }

    String modified; // carries always latest deploy

    // grab deployed one and check
    {
      final PackageRoot commonjs = metadataStore.getPackageByName(npmHostedRepository1, "commonjs");
      assertThat(commonjs.getRaw(), hasKey("time"));
      final Map<String, String> time = (Map<String, String>) commonjs.getRaw().get("time");
      assertThat(time.entrySet(), hasSize(4));
      modified = time.get("modified");
      assertThat(created, notNullValue());
      assertThat(time, hasEntry("created", created)); // not updated
      assertThat(time, hasEntry("modified", modified)); // updated
      assertThat(time, hasEntry("0.0.1", created)); // not updated
      assertThat(time, hasEntry("0.0.2", modified)); // updated
    }
  }

  /**
   * Simple smoke test that checks group functionality, it should aggregate members. Not using merge, so
   * first member "shades" the package in next member.
   */
  @Test
  public void groupPackageRootRoundtripWithoutMerge() throws Exception {
    // deploy private project to hosted1 repo
    {
      final File jsonFile = util.resolveFile("src/test/npm/ROOT_testproject_patched.json");
      final ContentLocator input = new PreparedContentLocator(
          new FileInputStream(jsonFile),
          NpmRepository.JSON_MIME_TYPE, -1);
      final PackageRequest request = new PackageRequest(new ResourceStoreRequest("/testproject"));
      npmHostedRepository1.getMetadataService()
          .consumePackageRoot(npmHostedRepository1.getMetadataService().parsePackageRoot(
              request, input));
    }
    // deploy private patched project to hosted2 repo
    {
      final File jsonFile = util.resolveFile("src/test/npm/ROOT_testproject.json");
      final ContentLocator input = new PreparedContentLocator(
          new FileInputStream(jsonFile),
          NpmRepository.JSON_MIME_TYPE, -1);
      final PackageRequest request = new PackageRequest(new ResourceStoreRequest("/testproject"));
      npmHostedRepository2.getMetadataService()
          .consumePackageRoot(npmHostedRepository2.getMetadataService().parsePackageRoot(
              request, input));
    }

    // disable merging
    npmGroupRepository.getMetadataService().setMergeMetadata(false);

    // proxy is set up against registry.npmjs.org, so no need to seed it

    // verify we have all what registry.mpmjs.org has + testproject
    final PackageRoot commonjs = npmGroupRepository.getMetadataService()
        .generatePackageRoot(new PackageRequest(new ResourceStoreRequest("/commonjs")));
    assertThat(commonjs, notNullValue());

    final PackageRoot testproject = npmGroupRepository.getMetadataService()
        .generatePackageRoot(new PackageRequest(new ResourceStoreRequest("/testproject")));
    assertThat(testproject, notNullValue());

    assertThat(testproject.getVersions(), not(hasKey("0.0.0"))); // is shaded
    assertThat(testproject.getVersions(), hasKey("0.0.0-patched"));
    assertThat((Map<String, String>) testproject.getRaw().get("dist-tags"), hasEntry("latest", "0.0.0-patched"));

    final PackageRootIterator iterator = npmGroupRepository.getMetadataService().generateRegistryRoot(
        new PackageRequest(new ResourceStoreRequest("/", true, false)));
    boolean found = false;
    int count = 0;
    while (iterator.hasNext()) {
      PackageRoot root = iterator.next();
      if ("testproject".equals(root.getName())) {
        found = true;
      }
      count++;
    }
    assertThat(count, greaterThan(1)); // we have ALL from registry.npmjs.org + testproject
    assertThat(found, is(true)); // we need to have testproject in there
  }

  /**
   * Simple smoke test that checks group functionality, it should aggregate members. Using merge, so member
   * metadata should not shade each other.
   */
  @Test
  public void groupPackageRootRoundtripWithMerge() throws Exception {
    // deploy private project to hosted1 repo
    {
      final File jsonFile = util.resolveFile("src/test/npm/ROOT_testproject_patched.json");
      final ContentLocator input = new PreparedContentLocator(
          new FileInputStream(jsonFile),
          NpmRepository.JSON_MIME_TYPE, -1);
      final PackageRequest request = new PackageRequest(new ResourceStoreRequest("/testproject"));
      npmHostedRepository1.getMetadataService()
          .consumePackageRoot(npmHostedRepository1.getMetadataService().parsePackageRoot(
              request, input));
    }
    // deploy private patched project to hosted2 repo
    {
      final File jsonFile = util.resolveFile("src/test/npm/ROOT_testproject.json");
      final ContentLocator input = new PreparedContentLocator(
          new FileInputStream(jsonFile),
          NpmRepository.JSON_MIME_TYPE, -1);
      final PackageRequest request = new PackageRequest(new ResourceStoreRequest("/testproject"));
      npmHostedRepository2.getMetadataService()
          .consumePackageRoot(npmHostedRepository2.getMetadataService().parsePackageRoot(
              request, input));
    }

    // enable merging
    npmGroupRepository.getMetadataService().setMergeMetadata(true);

    // proxy is set up against registry.npmjs.org, so no need to seed it

    // verify we have all what registry.mpmjs.org has + testproject
    final PackageRoot commonjs = npmGroupRepository.getMetadataService()
        .generatePackageRoot(new PackageRequest(new ResourceStoreRequest("/commonjs")));
    assertThat(commonjs, notNullValue());

    final PackageRoot testproject = npmGroupRepository.getMetadataService()
        .generatePackageRoot(new PackageRequest(new ResourceStoreRequest("/testproject")));
    assertThat(testproject, notNullValue());

    assertThat(testproject.getVersions(), hasKey("0.0.0"));
    assertThat(testproject.getVersions(), hasKey("0.0.0-patched"));
    // TODO: none of these are latest, as proxied registry ALSO has this package
    // TODO: this IT might break if package gets removed from registry (unlikely but possible), in which case the patched should become latest version
    assertThat((Map<String, String>) testproject.getRaw().get("dist-tags"), not(hasEntry("latest", "0.0.0")));
    assertThat((Map<String, String>) testproject.getRaw().get("dist-tags"), not(hasEntry("latest", "0.0.0-patched")));

    final PackageRootIterator iterator = npmGroupRepository.getMetadataService().generateRegistryRoot(
        new PackageRequest(new ResourceStoreRequest("/", true, false)));
    boolean found = false;
    int count = 0;
    while (iterator.hasNext()) {
      PackageRoot root = iterator.next();
      if ("testproject".equals(root.getName())) {
        found = true;
      }
      count++;
    }
    assertThat(count, greaterThan(1)); // we have ALL from registry.npmjs.org + testproject
    assertThat(found, is(true)); // we need to have testproject in there
  }
}
