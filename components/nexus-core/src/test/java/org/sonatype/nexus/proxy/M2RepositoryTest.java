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
package org.sonatype.nexus.proxy;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.attributes.AttributeStorage;
import org.sonatype.nexus.proxy.attributes.Attributes;
import org.sonatype.nexus.proxy.events.RepositoryItemEventCache;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.proxy.maven.ArtifactStoreRequest;
import org.sonatype.nexus.proxy.maven.ChecksumContentValidator;
import org.sonatype.nexus.proxy.maven.MUtils;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.maven.gav.M2ArtifactRecognizer;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.sisu.litmus.testsupport.group.Slow;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers.exists;

@Category(Slow.class) // ~25s
public class M2RepositoryTest
    extends M2ResourceStoreTest
{
  private static final long A_DAY = 24L * 60L * 60L * 1000L;

  protected static final String SPOOF_RELEASE = "/spoof/spoof/1.0/spoof-1.0.txt";

  protected static final String SPOOF_SNAPSHOT = "/spoof/spoof/1.0-SNAPSHOT/spoof-1.0-SNAPSHOT.txt";

  @Override
  protected String getItemPath() {
    return "/activemq/activemq-core/1.2/activemq-core-1.2.jar";
  }

  @Override
  protected ResourceStore getResourceStore()
      throws NoSuchRepositoryException, IOException
  {
    Repository repo1 = getRepositoryRegistry().getRepository("repo1");

    repo1.setWritePolicy(RepositoryWritePolicy.ALLOW_WRITE);

    getApplicationConfiguration().saveConfiguration();

    return repo1;
  }

  @Test
  public void testPoliciesWithRetrieve()
      throws Exception
  {
    M2Repository repository = (M2Repository) getResourceStore();

    // a "release"
    repository.setRepositoryPolicy(RepositoryPolicy.RELEASE);
    repository.getCurrentCoreConfiguration().commitChanges();

    StorageItem item = getResourceStore().retrieveItem(new ResourceStoreRequest(SPOOF_RELEASE, false));
    checkForFileAndMatchContents(item);

    try {
      item = getResourceStore().retrieveItem(new ResourceStoreRequest(SPOOF_SNAPSHOT, false));

      assertThat("Should not be able to get snapshot from release repo", false);
    }
    catch (ItemNotFoundException e) {
      // good
    }

    // reset NFC
    repository.expireCaches(new ResourceStoreRequest(RepositoryItemUid.PATH_ROOT, true));

    // a "snapshot"
    repository.setRepositoryPolicy(RepositoryPolicy.SNAPSHOT);
    repository.getCurrentCoreConfiguration().commitChanges();

    item = getResourceStore().retrieveItem(new ResourceStoreRequest(SPOOF_SNAPSHOT, false));
    checkForFileAndMatchContents(item);

    try {
      item = getResourceStore().retrieveItem(new ResourceStoreRequest(SPOOF_RELEASE, false));

      assertThat("Should not be able to get release from snapshot repo", false);
    }
    catch (ItemNotFoundException e) {
      // good
    }
  }

  @Test
  public void testPoliciesWithStore()
      throws Exception
  {
    M2Repository repository = (M2Repository) getResourceStore();

    // a "release"
    repository.setRepositoryPolicy(RepositoryPolicy.RELEASE);
    repository.getCurrentCoreConfiguration().commitChanges();

    DefaultStorageFileItem item = new DefaultStorageFileItem(
        repository, new ResourceStoreRequest(SPOOF_RELEASE), true, true, new StringContentLocator(SPOOF_RELEASE)
    );

    repository.storeItem(false, item);

    try {
      item = new DefaultStorageFileItem(
          repository, new ResourceStoreRequest(SPOOF_SNAPSHOT), true, true, new StringContentLocator(SPOOF_SNAPSHOT)
      );

      repository.storeItem(false, item);

      assertThat("Should not be able to store snapshot to release repo", false);
    }
    catch (UnsupportedStorageOperationException e) {
      // good
    }

    // reset NFC
    repository.expireCaches(new ResourceStoreRequest(RepositoryItemUid.PATH_ROOT, true));

    // a "snapshot"
    repository.setRepositoryPolicy(RepositoryPolicy.SNAPSHOT);
    repository.getCurrentCoreConfiguration().commitChanges();

    item = new DefaultStorageFileItem(
        repository, new ResourceStoreRequest(SPOOF_SNAPSHOT), true, true, new StringContentLocator(SPOOF_SNAPSHOT)
    );

    repository.storeItem(false, item);

    try {
      item = new DefaultStorageFileItem(
          repository, new ResourceStoreRequest(SPOOF_RELEASE), true, true, new StringContentLocator(SPOOF_RELEASE)
      );

      repository.storeItem(false, item);

      assertThat("Should not be able to store release to snapshot repo", false);
    }
    catch (UnsupportedStorageOperationException e) {
      // good
    }
  }

  @Test
  public void testShouldServeByPolicies()
      throws Exception
  {
    M2Repository repository = (M2Repository) getResourceStore();

    String releasePom =
        "/org/codehaus/plexus/plexus-container-default/1.0-alpha-40/plexus-container-default-1.0-alpha-40.pom";
    String releaseArtifact =
        "/org/codehaus/plexus/plexus-container-default/1.0-alpha-40/plexus-container-default-1.0-alpha-40.jar";
    String snapshotPom =
        "/org/codehaus/plexus/plexus-container-default/1.0-alpha-41-SNAPSHOT/plexus-container-default-1.0-alpha-41-20071205.190351-1.pom";
    String snapshotArtifact =
        "/org/codehaus/plexus/plexus-container-default/1.0-alpha-41-SNAPSHOT/plexus-container-default-1.0-alpha-41-20071205.190351-1.jar";
    String metadata1 = "/org/codehaus/plexus/plexus-container-default/maven-metadata.xml";
    String metadataR = "/org/codehaus/plexus/plexus-container-default/1.0-alpha-40/maven-metadata.xml";
    String metadataS = "/org/codehaus/plexus/plexus-container-default/1.0-alpha-41-SNAPSHOT/maven-metadata.xml";
    String someDirectory = "/classworlds/";
    String anyNonArtifactFile = "/any/file.txt";

    ResourceStoreRequest request = new ResourceStoreRequest("");

    // it is equiv of repo type: RELEASE
    repository.setRepositoryPolicy(RepositoryPolicy.RELEASE);
    repository.getCurrentCoreConfiguration().commitChanges();

    request.setRequestPath(releasePom);
    assertThat(repository.shouldServeByPolicies(request), is(true));
    request.setRequestPath(releaseArtifact);
    assertThat(repository.shouldServeByPolicies(request), is(true));
    request.setRequestPath(snapshotPom);
    assertThat(repository.shouldServeByPolicies(request), is(false));
    request.setRequestPath(snapshotArtifact);
    assertThat(repository.shouldServeByPolicies(request), is(false));
    request.setRequestPath(metadata1);
    assertThat(repository.shouldServeByPolicies(request), is(true));
    request.setRequestPath(metadataR);
    assertThat(repository.shouldServeByPolicies(request), is(true));
    request.setRequestPath(metadataS);
    assertThat(repository.shouldServeByPolicies(request), is(false));
    request.setRequestPath(someDirectory);
    assertThat(repository.shouldServeByPolicies(request), is(true));
    request.setRequestPath(anyNonArtifactFile);
    assertThat(repository.shouldServeByPolicies(request), is(true));

    // it is equiv of repo type: SNAPSHOT
    repository.setRepositoryPolicy(RepositoryPolicy.SNAPSHOT);
    repository.getCurrentCoreConfiguration().commitChanges();

    request.setRequestPath(releasePom);
    assertThat(repository.shouldServeByPolicies(request), is(false));
    request.setRequestPath(releaseArtifact);
    assertThat(repository.shouldServeByPolicies(request), is(false));
    request.setRequestPath(snapshotPom);
    assertThat(repository.shouldServeByPolicies(request), is(true));
    request.setRequestPath(snapshotArtifact);
    assertThat(repository.shouldServeByPolicies(request), is(true));
    request.setRequestPath(metadata1);
    assertThat(repository.shouldServeByPolicies(request), is(true));
    request.setRequestPath(metadataR);
    assertThat(repository.shouldServeByPolicies(request), is(true));
    request.setRequestPath(metadataS);
    assertThat(repository.shouldServeByPolicies(request), is(true));
    request.setRequestPath(someDirectory);
    assertThat(repository.shouldServeByPolicies(request), is(true));
    request.setRequestPath(anyNonArtifactFile);
    assertThat(repository.shouldServeByPolicies(request), is(true));
  }

  @Test
  public void testGetLatestVersionSimple()
      throws Exception
  {
    M2Repository repository = (M2Repository) getResourceStore();

    List<String> versions = new ArrayList<String>();
    versions.add("1.0.0");
    versions.add("1.0.1");
    versions.add("1.0.2");
    versions.add("1.1.2");
    assertThat(repository.getLatestVersion(versions), is("1.1.2"));
  }

  @Test
  public void testGetLatestVersionClassifiers()
      throws Exception
  {
    M2Repository repository = (M2Repository) getResourceStore();

    List<String> versions = new ArrayList<String>();
    versions.add("1.0-alpha-19");
    versions.add("1.0-alpha-9-stable-1");
    versions.add("1.0-alpha-20");
    versions.add("1.0-alpha-21");
    versions.add("1.0-alpha-22");
    versions.add("1.0-alpha-40");
    assertThat(repository.getLatestVersion(versions), is("1.0-alpha-40"));
  }

  @Test
  public void testIsSnapshot()
      throws Exception
  {
    // M2Repository repository = (M2Repository) getResourceStore();

    assertThat(Gav.isSnapshot("1.0.0"), is(false));
    assertThat(Gav.isSnapshot("1.0.0-SNAPSHOT"), is(true));
    assertThat(Gav.isSnapshot("1.0-alpha-25"), is(false));
    assertThat(Gav.isSnapshot("1.0-alpha-25-20070518.002146-2"), is(true));
  }

  @Test
  public void testExpirationAlwaysUpdate()
      throws Exception
  {
    getLogger().info(
        "Start testExpirationAlwaysUpdate() for '/spoof/maven-metadata.xml' at (" + System.currentTimeMillis() + ")");
    doTestExpiration("/spoof/maven-metadata.xml", 0, 1, 2, 3);
    getLogger().info(
        "Start testExpirationAlwaysUpdate() for '/spoof/spoof/1.0/spoof-1.0.txt' at (" + System.currentTimeMillis() +
            ")");
    doTestExpiration("/spoof/spoof/1.0/spoof-1.0.txt", 0, 1, 2, 3);
  }

  @Test
  public void testExpirationNeverUpdate()
      throws Exception
  {
    getLogger().info(
        "Start testExpirationNeverUpdate() for '/spoof/maven-metadata.xml' at (" + System.currentTimeMillis() + ")");
    doTestExpiration("/spoof/maven-metadata.xml", -1, 1, 1, 1);
    getLogger().info(
        "Start testExpirationNeverUpdate() for '/spoof/spoof/1.0/spoof-1.0.txt' at (" + System.currentTimeMillis() +
            ")");
    doTestExpiration("/spoof/spoof/1.0/spoof-1.0.txt", -1, 1, 1, 1);
  }

  @Test
  public void testExpiration()
      throws Exception
  {
    getLogger().info("Start testExpiration() for '/spoof/maven-metadata.xml' at (" + System.currentTimeMillis() + ")");
    doTestExpiration("/spoof/maven-metadata.xml", 1, 1, 1, 2);
    getLogger().info(
        "Start testExpiration() for '/spoof/spoof/1.0/spoof-1.0.txt' at (" + System.currentTimeMillis() + ")");
    doTestExpiration("/spoof/spoof/1.0/spoof-1.0.txt", 1, 1, 1, 2);
  }

  /**
   * For expiration-related issues and stories see: NEXUS-1675 NEXUS-3065 NEXUS-4099
   * <p />
   *
   * Note: This test is somewhat fragile on Windows in the past if IO is slow or
   * CPU is thrashing at time of test run.
   */
  private void doTestExpiration(String path, final int age, final int... expectedHits)
      throws Exception
  {
    CounterListener ch = new CounterListener();

    M2Repository repository = (M2Repository) getResourceStore();

    eventBus().register(ch);

    File mdFile = new File(new File(getBasedir()), "target/test-classes/repo1" + path);
    long fileTimestamp = mdFile.lastModified();

    assertThat(mdFile, exists());

    try {
      repository.deleteItem(new ResourceStoreRequest("/spoof", true));
    }
    catch (ItemNotFoundException e) {
      // ignore
    }

    repository.setMetadataMaxAge(age);
    repository.setArtifactMaxAge(age);
    repository.getCurrentCoreConfiguration().commitChanges();

    for (int i = 0; i < 10 && !mdFile.setLastModified(System.currentTimeMillis() - (3L * A_DAY)); i++) {
      System.gc(); // helps with FS sync'ing on Windows
      Thread.sleep(500); // wait for FS
    }


    assertThat("File timestamp did not change, first pass", mdFile.lastModified(), not(equalTo(fileTimestamp)));
    fileTimestamp = mdFile.lastModified();

    // We need to wait a bit to avoid the check that last remote check = current time
    Thread.sleep(500);
    final StorageItem item = repository.retrieveItem(new ResourceStoreRequest(path, false));
    getLogger().info(
        path + " -> BEFORE assert 1 requestCount=" + expectedHits[0] + " at (" + System.currentTimeMillis() + ")");
    assertThat("Remote hits count fail after first request at (" + System.currentTimeMillis() + ")",
        ch.getRequestCount(), equalTo(expectedHits[0]));
    getLogger().info(
        path + " -> AFTER assert 1 requestCount=" + expectedHits[0] + " at (" + System.currentTimeMillis() + ")");

    for (int i = 0; i < 10 && !mdFile.setLastModified(System.currentTimeMillis() - (2L * A_DAY)); i++) {
      System.gc(); // helps with FS sync'ing on Windows
      Thread.sleep(500); // wait for FS
    }

    assertThat("File timestamp did not change, second pass", mdFile.lastModified(), not(equalTo(fileTimestamp)));
    fileTimestamp = mdFile.lastModified();

    // We need to wait a bit to avoid the check that last remote check = current time
    Thread.sleep(500);
    // this goes remote depending on age setting
    repository.retrieveItem(new ResourceStoreRequest(path, false));
    getLogger().info(
        path + " -> BEFORE assert 2 requestCount=" + expectedHits[1] + " at (" + System.currentTimeMillis() + ")");
    assertThat("Remote hits count fail after second request at (" + System.currentTimeMillis() + ")",
        ch.getRequestCount(), equalTo(expectedHits[1]));
    getLogger().info(
        path + " -> AFTER assert 2 requestCount=" + expectedHits[1] + " at (" + System.currentTimeMillis() + ")");

    for (int i = 0; i < 10 && !mdFile.setLastModified(System.currentTimeMillis() - (1L * A_DAY)); i++) {
      System.gc(); // helps with FS sync'ing on Windows
      Thread.sleep(500); // wait for FS
    }

    assertThat("File timestamp did not change, third pass", mdFile.lastModified(), not(equalTo(fileTimestamp)));
    fileTimestamp = mdFile.lastModified();

    // set up last checked timestamp so that nexus should go remote
    final RepositoryItemUid uid = item.getRepositoryItemUid();
    final AttributeStorage storage = uid.getRepository().getAttributesHandler().getAttributeStorage();
    final Attributes attributes = item.getRepositoryItemAttributes();
    attributes.setCheckedRemotely(System.currentTimeMillis() - ((Math.abs(age) + 1) * 60 * 1000));
    storage.putAttributes(uid, attributes);

    // We need to wait a bit to avoid the check that last remote check = current time
    Thread.sleep(500);
    repository.retrieveItem(new ResourceStoreRequest(path, false));
    getLogger().info(
        path + " -> BEFORE assert 3 requestCount=" + expectedHits[2] + " at (" + System.currentTimeMillis() + ")");
    assertThat("Remote hits count fail after third request at (" + System.currentTimeMillis() + ")",
        ch.getRequestCount(), equalTo(expectedHits[2]));
    getLogger().info(
        path + " -> AFTER assert 3 requestCount=" + expectedHits[2] + " at (" + System.currentTimeMillis() + ")");

    // cleanup counter listener for next test call to avoid added overhead, logging noise
    eventBus().unregister(ch);
  }

  @Test
  public void testLocalStorageChanges()
      throws Exception
  {
    M2Repository repository = (M2Repository) getResourceStore();

    String changedUrl = repository.getLocalUrl() + "foo";

    repository.setLocalUrl(changedUrl);

    assertFalse("Should not be the same!", changedUrl.equals(repository.getLocalUrl()));

    repository.getCurrentCoreConfiguration().commitChanges();

    assertTrue("Should be the same!", changedUrl.equals(repository.getLocalUrl()));
  }

  @Test
  public void testRemoteStorageChanges()
      throws Exception
  {
    M2Repository repository = (M2Repository) getResourceStore();

    String changedUrl = repository.getRemoteUrl() + "/foo/";

    repository.setRemoteUrl(changedUrl);

    assertFalse("Should not be the same!", changedUrl.equals(repository.getRemoteUrl()));

    repository.getCurrentCoreConfiguration().commitChanges();

    assertTrue("Should be the same!", changedUrl.equals(repository.getRemoteUrl()));
  }

  @Test
  public void testProxyLastRequestedAttribute()
      throws Exception
  {
    M2Repository repository = (M2Repository) this.getRepositoryRegistry().getRepository("repo1");

    String item = "/org/slf4j/slf4j-api/1.4.3/slf4j-api-1.4.3.pom";
    ResourceStoreRequest request = new ResourceStoreRequest(item);
    request.getRequestContext().put(AccessManager.REQUEST_REMOTE_ADDRESS, "127.0.0.1");
    StorageItem storageItem = repository.retrieveItem(request);
    long lastRequest = System.currentTimeMillis() - 10 * A_DAY;
    storageItem.setLastRequested(lastRequest);
    repository.storeItem(false, storageItem);

    // now request the object, the lastRequested timestamp should be updated
    StorageItem resultItem = repository.retrieveItem(request);
    Assert.assertTrue(resultItem.getLastRequested() > lastRequest);

    // check the shadow attributes
    Attributes shadowStorageItem =
        repository.getAttributesHandler().getAttributeStorage().getAttributes(
            repository.createUid(request.getRequestPath()));
    assertThat(shadowStorageItem.getLastRequested(), is(resultItem.getLastRequested()));
  }

  @Test
  public void testHostedLastRequestedAttribute()
      throws Exception
  {
    String itemPath = "/org/test/foo.junk";

    M2Repository repository = (M2Repository) this.getRepositoryRegistry().getRepository("inhouse");
    File inhouseLocalStorageDir =
        new File(
            new URL(((CRepositoryCoreConfiguration) repository.getCurrentCoreConfiguration()).getConfiguration(
                false).getLocalStorage().getUrl()).getFile());

    File artifactFile = new File(inhouseLocalStorageDir, itemPath);
    artifactFile.getParentFile().mkdirs();

    FileUtils.write(artifactFile, "Some Text so the file is not empty");

    ResourceStoreRequest request = new ResourceStoreRequest(itemPath);
    request.getRequestContext().put(AccessManager.REQUEST_REMOTE_ADDRESS, "127.0.0.1");
    StorageItem storageItem = repository.retrieveItem(request);
    long lastRequest = System.currentTimeMillis() - 10 * A_DAY;
    storageItem.setLastRequested(lastRequest);
    repository.storeItem(false, storageItem);

    // now request the object, the lastRequested timestamp should be updated
    StorageItem resultItem = repository.retrieveItem(request);
    Assert.assertTrue(resultItem.getLastRequested() > lastRequest);

    // check the shadow attributes
    Attributes shadowStorageItem =
        repository.getAttributesHandler().getAttributeStorage().getAttributes(
            repository.createUid(request.getRequestPath()));
    assertThat(shadowStorageItem.getLastRequested(), is(resultItem.getLastRequested()));
  }

  @Test
  public void testNEXUS4943RepositoryMetadataManager_expireNotFoundMetadataCaches()
      throws Exception
  {
    // hosted reposes has NFC shut down (see NEXUS-4798)
    MavenProxyRepository m2Repo =
        getRepositoryRegistry().getRepositoryWithFacet("repo1", MavenProxyRepository.class);
    m2Repo.addToNotFoundCache(new ResourceStoreRequest("/some/path/file.jar"));
    m2Repo.addToNotFoundCache(new ResourceStoreRequest("/some/path/maven-metadata.xml"));

    // note: above, that is the "standard" way to write item paths! (starting with slash)
    // below, we tamper directly with NFC, so the lack of starting slash should not confuse you!

    // we have two known entries in NFC
    assertThat(m2Repo.getNotFoundCache().listKeysInCache().size(), equalTo(2));
    assertThat(m2Repo.getNotFoundCache().listKeysInCache(),
        contains("some/path/file.jar", "some/path/maven-metadata.xml"));

    m2Repo.getRepositoryMetadataManager().expireNotFoundMetadataCaches(new ResourceStoreRequest("/"));

    // M2 metadata should be removed, so we have one known entry in NFC
    assertThat(m2Repo.getNotFoundCache().listKeysInCache().size(), equalTo(1));
    assertThat(m2Repo.getNotFoundCache().listKeysInCache(), contains("some/path/file.jar"));
  }

  @Test
  public void testNEXUS4943RepositoryMetadataManager_expireMetadataCaches()
      throws Exception
  {
    // hosted reposes has NFC shut down (see NEXUS-4798)
    MavenProxyRepository m2Repo =
        getRepositoryRegistry().getRepositoryWithFacet("repo1", MavenProxyRepository.class);

    // put some proxied content
    // we need GavBuilder-like class, this below is hilarious!
    final Gav gav =
        new Gav("org.slf4j", "slf4j-api", "1.4.3", null, "jar", null, null, null, false, null, false, null);
    m2Repo.getArtifactStoreHelper().retrieveArtifact(new ArtifactStoreRequest(m2Repo, gav, false));
    // get GAV metadata (is present)
    m2Repo.retrieveItem(new ResourceStoreRequest("/org/slf4j/slf4j-api/1.4.3/maven-metadata.xml"));
    // get GA metadat (not present, will go into NFC
    try {
      m2Repo.retrieveItem(new ResourceStoreRequest("/org/slf4j/slf4j-api/maven-metadata.xml"));
    }
    catch (ItemNotFoundException e) {
      // good, we expected this
    }

    // expire sub-tree to use walking method to update all item attributes to expired=true
    m2Repo.getRepositoryMetadataManager().expireMetadataCaches(new ResourceStoreRequest("/org/"));

    // the GA metadata should be removed from NFC, so we have no known entry in NFC
    assertThat(m2Repo.getNotFoundCache().listKeysInCache().size(), equalTo(0));

    // the GAV metadata should be expired
    assertThat(
        m2Repo.retrieveItem(new ResourceStoreRequest("/org/slf4j/slf4j-api/1.4.3/maven-metadata.xml")).isExpired(),
        is(true));
    // but the JAR should not be expired
    assertThat(
        m2Repo.retrieveItem(new ResourceStoreRequest("/org/slf4j/slf4j-api/1.4.3/slf4j-api-1.4.3.jar")).isExpired(),
        is(false));
  }

  // NEXUS-4218 BEGIN

  @Test
  public void testNEXUS4218HowWeSpottedItMetadataChecksum()
      throws Exception
  {
    // here, we use path that does conform to M2 layout, but is maven metadata checksum, and there is no UA neither
    // remote address
    // this is the actual case made us realize the existence of the bug
    final String path = "/org/slf4j/slf4j-api/1.4.3/maven-metadata.xml.sha1";

    doTestNEXUS4218(path, null, null);
  }

  @Test
  public void testNEXUS4218HowWeSpottedItMetadata()
      throws Exception
  {
    // here, we use path that does conform to M2 layout, but is maven metadata, and there is no UA neither remote
    // address
    final String path = "/org/slf4j/slf4j-api/1.4.3/maven-metadata.xml";

    doTestNEXUS4218(path, null, null);
  }

  @Test
  public void testNEXUS4218JustAnArtifactChecksum()
      throws Exception
  {
    // here, we use path that does conform to M2 layout, but is maven artifact checksum, and there is no UA neither
    // remote address
    final String path = "/org/slf4j/slf4j-api/1.4.3/slf4j-api-1.4.3.jar.sha1";

    doTestNEXUS4218(path, null, null);
  }

  @Test
  public void testNEXUS4218JustAnArtifact()
      throws Exception
  {
    // here, we use path that does conform to M2 layout, but is maven metadata, and there is no UA neither remote
    // address
    final String path = "/org/slf4j/slf4j-api/1.4.3/slf4j-api-1.4.3.jar";

    doTestNEXUS4218(path, null, null);
  }

  @Test
  public void testNEXUS4218HowWeSpottedItMetadataChecksumWithNotTriggeringUAAndIP()
      throws Exception
  {
    // here, we use path that does conform to M2 layout, but is maven metadata checksum, and there is no UA neither
    // remote address
    // this case made us realize the existince of the bug
    final String path = "/org/slf4j/slf4j-api/1.4.3/maven-metadata.xml.sha1";

    doTestNEXUS4218(path, "Apache-Maven/3", "127.0.0.1");
  }

  @Test
  public void testNEXUS4218HowWeSpottedItMetadataWithNotTriggeringUAAndIP()
      throws Exception
  {
    // here, we use path that does conform to M2 layout, but is maven metadata, and there is no UA neither remote
    // address
    final String path = "/org/slf4j/slf4j-api/1.4.3/maven-metadata.xml";

    doTestNEXUS4218(path, "Apache-Maven/3", "127.0.0.1");
  }

  @Test
  public void testNEXUS4218JustAnArtifactChecksumWithNotTriggeringUAAndIP()
      throws Exception
  {
    // here, we use path that does conform to M2 layout, but is maven artifact checksum, and there is no UA neither
    // remote address
    final String path = "/org/slf4j/slf4j-api/1.4.3/slf4j-api-1.4.3.jar.sha1";

    doTestNEXUS4218(path, "Apache-Maven/3", "127.0.0.1");
  }

  @Test
  public void testNEXUS4218JustAnArtifactWithNotTriggeringUAAndIP()
      throws Exception
  {
    // here, we use path that does conform to M2 layout, but is maven metadata, and there is no UA neither remote
    // address
    final String path = "/org/slf4j/slf4j-api/1.4.3/slf4j-api-1.4.3.jar";

    doTestNEXUS4218(path, "Apache-Maven/3", "127.0.0.1");
  }

  @Test
  public void testNEXUS4218HowWeSpottedItMetadataChecksumWithTriggeringUAAndIP()
      throws Exception
  {
    // here, we use path that does conform to M2 layout, but is maven metadata checksum, and there is no UA neither
    // remote address
    // this case made us realize the existince of the bug
    final String path = "/org/slf4j/slf4j-api/1.4.3/maven-metadata.xml.sha1";

    doTestNEXUS4218(path, "Apache-Maven/2", "127.0.0.1");
  }

  @Test
  public void testNEXUS4218HowWeSpottedItMetadataWithTriggeringUAAndIP()
      throws Exception
  {
    // here, we use path that does conform to M2 layout, but is maven metadata, and there is no UA neither remote
    // address
    final String path = "/org/slf4j/slf4j-api/1.4.3/maven-metadata.xml";

    doTestNEXUS4218(path, "Apache-Maven/2", "127.0.0.1");
  }

  @Test
  public void testNEXUS4218JustAnArtifactChecksumWithTriggeringUAAndIP()
      throws Exception
  {
    // here, we use path that does conform to M2 layout, but is maven artifact checksum, and there is no UA neither
    // remote address
    final String path = "/org/slf4j/slf4j-api/1.4.3/slf4j-api-1.4.3.jar.sha1";

    doTestNEXUS4218(path, "Apache-Maven/2", "127.0.0.1");
  }

  @Test
  public void testNEXUS4218JustAnArtifactWithTriggeringUAAndIP()
      throws Exception
  {
    // here, we use path that does conform to M2 layout, but is maven metadata, and there is no UA neither remote
    // address
    final String path = "/org/slf4j/slf4j-api/1.4.3/slf4j-api-1.4.3.jar";

    doTestNEXUS4218(path, "Apache-Maven/2", "127.0.0.1");
  }

  public void doTestNEXUS4218(final String path, final String userAgent, final String remoteAddress)
      throws Exception
  {
    final M2Repository repository = (M2Repository) getResourceStore();
    // we check our expectation agains repository: it has to be Maven2 proxy repository
    Assert.assertTrue(repository.getRepositoryKind().isFacetAvailable(MavenProxyRepository.class));
    Assert.assertTrue(Maven2ContentClass.ID.equals(repository.getRepositoryContentClass().getId()));
    File repositoryLocalStorageDir =
        new File(
            new URL(((CRepositoryCoreConfiguration) repository.getCurrentCoreConfiguration()).getConfiguration(
                false).getLocalStorage().getUrl()).getFile());

    // create a request and equip it as needed
    final ResourceStoreRequest request = new ResourceStoreRequest(path);
    if (userAgent != null) {
      request.getRequestContext().put(AccessManager.REQUEST_AGENT, userAgent);
    }
    if (remoteAddress != null) {
      request.getRequestContext().put(AccessManager.REQUEST_REMOTE_ADDRESS, remoteAddress);
    }

    // invoke expire caches
    repository.expireCaches(request);

    // NO file should be pulled down
    {
      File artifactFile;
      if (!M2ArtifactRecognizer.isChecksum(path)) {
        artifactFile = new File(repositoryLocalStorageDir, path.substring(1));
      }
      else {
        artifactFile =
            new File(repositoryLocalStorageDir, path.substring(1, path.length() - ".sha1".length()));
      }
      Assert.assertFalse(artifactFile.exists());
    }

    // create a request and equip it as needed (requests should NOT be reused!)
    final ResourceStoreRequest retrieveRequest = new ResourceStoreRequest(path);
    if (userAgent != null) {
      retrieveRequest.getRequestContext().put(AccessManager.REQUEST_AGENT, userAgent);
    }
    if (remoteAddress != null) {
      retrieveRequest.getRequestContext().put(AccessManager.REQUEST_REMOTE_ADDRESS, remoteAddress);
    }

    // now do a fetch
    // this verifies that an assertion ("path in question does not exists in proxy cache but does exists on remote")
    // also verifies that cstamas did not mistype the paths in @Test method ;)
    repository.retrieveItem(retrieveRequest);

    // files SHOULD be pulled down
    {
      File artifactFile;
      if (!M2ArtifactRecognizer.isChecksum(path)) {
        artifactFile = new File(repositoryLocalStorageDir, path.substring(1));
      }
      else {
        artifactFile =
            new File(repositoryLocalStorageDir, path.substring(1, path.length() - ".sha1".length()));
      }
      Assert.assertTrue(artifactFile.exists());
    }
  }

  // NEXUS-4218 END

  @Test
  public void testNEXUS5418_retrieveItemWithChecksums()
      throws Exception
  {
    final M2Repository repository = (M2Repository) getResourceStore();

    Assert.assertTrue(repository.getRepositoryKind().isFacetAvailable(ProxyRepository.class));

    StorageItem item = repository.retrieveItem(new ResourceStoreRequest("/spoof/simple.txt"));

    StorageFileItem sha1 =
        (StorageFileItem) repository.retrieveItem(new ResourceStoreRequest("/spoof/simple.txt.sha1"));
    String sha1str = MUtils.readDigestFromFileItem((StorageFileItem) sha1);
    Assert.assertEquals(item.getRepositoryItemAttributes().get(StorageFileItem.DIGEST_SHA1_KEY), sha1str);
    Assert.assertEquals(item.getModified(), sha1.getModified());
    Assert.assertEquals(40, sha1.getLength());

    StorageFileItem md5 =
        (StorageFileItem) repository.retrieveItem(new ResourceStoreRequest("/spoof/simple.txt.md5"));
    String md5str = MUtils.readDigestFromFileItem((StorageFileItem) md5);
    Assert.assertEquals(item.getRepositoryItemAttributes().get(StorageFileItem.DIGEST_MD5_KEY), md5str);
    Assert.assertEquals(item.getModified(), md5.getModified());
    Assert.assertEquals(32, md5.getLength());
  }

  @Test
  public void testNEXUS5418_listChecksums()
      throws Exception
  {
    final M2Repository repository = (M2Repository) getResourceStore();

    // sanity check
    Assert.assertTrue(repository.getRepositoryKind().isFacetAvailable(ProxyRepository.class));
    try {
      repository.list(new ResourceStoreRequest("/spoof"));
      Assert.fail();
    }
    catch (ItemNotFoundException expected) {
      // expected
    }

    // cache an item and its checksums
    repository.retrieveItem(new ResourceStoreRequest("/spoof/simple.txt"));
    repository.retrieveItem(new ResourceStoreRequest("/spoof/simple.txt.sha1"));
    repository.retrieveItem(new ResourceStoreRequest("/spoof/simple.txt.md5"));

    Collection<StorageItem> items = repository.list(new ResourceStoreRequest("/spoof"));

    Map<String, StorageItem> paths = new HashMap<String, StorageItem>();
    for (StorageItem item : items) {
      paths.put(item.getPath(), item);
    }

    StorageFileItem item = (StorageFileItem) paths.get("/spoof/simple.txt");
    Assert.assertNotNull(item);

    StorageFileItem sha1 = (StorageFileItem) paths.get("/spoof/simple.txt.sha1");
    Assert.assertNotNull(sha1);
    String sha1str = MUtils.readDigestFromFileItem((StorageFileItem) sha1);
    Assert.assertEquals(item.getRepositoryItemAttributes().get(StorageFileItem.DIGEST_SHA1_KEY), sha1str);
    Assert.assertEquals(item.getModified(), sha1.getModified());
    Assert.assertEquals(40, sha1.getLength());

    StorageFileItem md5 = (StorageFileItem) paths.get("/spoof/simple.txt.md5");
    Assert.assertNotNull(md5);
    String md5str = MUtils.readDigestFromFileItem((StorageFileItem) md5);
    Assert.assertEquals(item.getRepositoryItemAttributes().get(StorageFileItem.DIGEST_MD5_KEY), md5str);
    Assert.assertEquals(item.getModified(), md5.getModified());
    Assert.assertEquals(32, md5.getLength());
  }

  @Test
  public void testNEXUS5418_storeItemWithChecksums()
      throws Exception
  {
    final M2Repository repository = (M2Repository) getResourceStore();

    Assert.assertTrue(repository.getRepositoryKind().isFacetAvailable(ProxyRepository.class));

    String sha1str = "0123456789012345678901234567890123456789";
    String md5str = "01234567012345670123456701234567";

    StorageItem item = repository.retrieveItem(new ResourceStoreRequest("/spoof/simple.txt"));

    repository.storeItem(false, new DefaultStorageFileItem(
        repository, new ResourceStoreRequest(item.getPath() + ".sha1"), true, true, new StringContentLocator(sha1str))
    );
    repository.storeItem(false, new DefaultStorageFileItem(
        repository, new ResourceStoreRequest(item.getPath() + ".md5"), true, true, new StringContentLocator(md5str))
    );

    // reread the item to refresh attributes map
    item = repository.retrieveItem(new ResourceStoreRequest("/spoof/simple.txt"));

    StorageFileItem sha1 =
        (StorageFileItem) repository.retrieveItem(new ResourceStoreRequest("/spoof/simple.txt.sha1"));
    Assert.assertEquals(item.getRepositoryItemAttributes().get(ChecksumContentValidator.ATTR_REMOTE_SHA1),
        sha1str);
    Assert.assertEquals(item.getModified(), sha1.getModified());
    Assert.assertEquals(40, sha1.getLength());

    StorageFileItem md5 =
        (StorageFileItem) repository.retrieveItem(new ResourceStoreRequest("/spoof/simple.txt.md5"));
    Assert.assertEquals(item.getRepositoryItemAttributes().get(ChecksumContentValidator.ATTR_REMOTE_MD5), md5str);
    Assert.assertEquals(item.getModified(), md5.getModified());
    Assert.assertEquals(32, md5.getLength());
  }

  // ==

  protected class CounterListener
  {
    private int requestCount = 0;

    public int getRequestCount() {
      return this.requestCount;
    }

    @Subscribe
    public void onEvent(RepositoryItemEventCache evt) {
      final RepositoryItemEventCache riec = (RepositoryItemEventCache) evt;
      final String path = riec.getItem().getPath();
      if (path.endsWith("maven-metadata.xml") || path.endsWith("spoof-1.0.txt")) {
        // logging to track if listener received message in time for accurate test assertion
        if (getLogger().isInfoEnabled()) {
          getLogger().info(path + " -> CounterListener increments to " + (++requestCount) + " at ("
              + System.currentTimeMillis() + ")");
        }
        else {
          requestCount = requestCount + 1;
        }
      }
    }

  }

}
