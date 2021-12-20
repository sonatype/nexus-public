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
package org.sonatype.nexus.rest.indexng;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.AbstractMavenRepoContentTests;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.index.IndexerManager;
import org.sonatype.nexus.index.Searcher;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.rest.model.NexusNGArtifact;
import org.sonatype.nexus.rest.model.SearchNGResponse;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.plexus.rest.resource.PlexusResourceException;
import org.sonatype.plexus.rest.resource.error.ErrorMessage;
import org.sonatype.plexus.rest.resource.error.ErrorResponse;
import org.sonatype.sisu.goodies.testsupport.group.Slow;

import com.google.common.collect.Lists;
import org.apache.lucene.queryParser.ParseException;
import org.apache.maven.index.SearchType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;
import org.restlet.Context;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for SearchNGIndexPlexusResource
 */
@Category(Slow.class) // ~27s
public class SearchNGIndexPlexusResourceTest
    extends AbstractMavenRepoContentTests
{

  @Override
  protected boolean runWithSecurityDisabled() {
    return true;
  }

  @Test
  public void testPlexusResourceException()
      throws Exception
  {
    SearchNGIndexPlexusResource resource = new SearchNGIndexPlexusResource(Lists.<Searcher>newArrayList());
    Map<String, String> terms = new HashMap<String, String>(4);
    terms.put("q", "!");
    Searcher searcher = mock(Searcher.class);
    when(searcher.canHandle(Mockito.any(Map.class))).thenReturn(true);

    when(
        searcher.flatIteratorSearch(Mockito.any(Map.class), anyString(), anyInt(), anyInt(), anyInt(),
            anyBoolean(), Mockito.any(SearchType.class), Mockito.any(List.class)))
        // emulate current indexer search behavior, illegal query results in IllegalArgEx with the ParseEx as cause
        .thenThrow(new IllegalArgumentException(new ParseException("mock")));

    try {
      resource.searchByTerms(terms, "rid", 1, 1, false, Arrays.asList(searcher));
      Assert.fail("Expected PlexusResourceException");
    }
    catch (PlexusResourceException e) {
      ErrorResponse resultObject = (ErrorResponse) e.getResultObject();
      assertThat(resultObject, notNullValue());
      List<ErrorMessage> errors = resultObject.getErrors();
      assertThat(errors, hasSize(1));
      ErrorMessage errorMessage = errors.get(0);

      // ID needs to be stable for UI handling
      assertThat(errorMessage.getId(), equalTo("search"));
      assertThat(errorMessage.getMsg(), containsString("mock"));
    }
  }

  @Test
  public void uncollapse()
      throws Exception
  {
    wairForAsyncEventsToCalmDown();
    waitForTasksToStop();

    int artifactCount = 30; // this is bellow collapse threshold
    SearchNGResponse result = deployAndSearch(artifactCount, DeployType.RELEASE);
    Assert.assertEquals(artifactCount, result.getData().size());
    ensureLatestIsPresent(result, DeployType.RELEASE, "30");
  }

  @Test
  public void collapse()
      throws Exception
  {
    wairForAsyncEventsToCalmDown();
    waitForTasksToStop();

    int artifactCount = 60; // this is above collapse threshold
    SearchNGResponse result = deployAndSearch(artifactCount, DeployType.RELEASE);
    Assert.assertEquals(1, result.getData().size());
    ensureLatestIsPresent(result, DeployType.RELEASE, "60");
  }

  @Test
  public void uncollapseMixed()
      throws Exception
  {
    wairForAsyncEventsToCalmDown();
    waitForTasksToStop();

    int artifactCount = 15; // this is bellow collapse threshold (count twice as DeployType.BOTH!)
    SearchNGResponse result = deployAndSearch(artifactCount, DeployType.BOTH);
    Assert.assertEquals(artifactCount * 2, result.getData().size());
    ensureLatestIsPresent(result, DeployType.BOTH, "15");
  }

  @Test
  public void collapseMixed()
      throws Exception
  {
    wairForAsyncEventsToCalmDown();
    waitForTasksToStop();

    int artifactCount = 60; // this is above collapse threshold
    SearchNGResponse result = deployAndSearch(artifactCount, DeployType.BOTH);
    Assert.assertEquals(1, result.getData().size()); // collapse by GA, so rel/snap is irrelevant
    ensureLatestIsPresent(result, DeployType.BOTH, "60");
  }

  private enum DeployType
  {
    RELEASE, SNAPSHOT, BOTH;
  }

  private void ensureLatestIsPresent(final SearchNGResponse response, final DeployType deployType,
                                     final String latestVersion)
  {
    for (NexusNGArtifact artifactHit : response.getData()) {
      if (deployType == DeployType.RELEASE || deployType == DeployType.BOTH) {
        assertThat(artifactHit.getLatestRelease(), equalTo(latestVersion));
        assertThat(artifactHit.getLatestReleaseRepositoryId(), equalTo("releases")); // hardcoded in
        // deployAndSearch
      }
      else {
        assertThat(artifactHit.getLatestRelease(), nullValue());
        assertThat(artifactHit.getLatestReleaseRepositoryId(), nullValue());
      }
      if (deployType == DeployType.SNAPSHOT || deployType == DeployType.BOTH) {
        assertThat(artifactHit.getLatestSnapshot(), equalTo(latestVersion + "-SNAPSHOT"));
        assertThat(artifactHit.getLatestSnapshotRepositoryId(), equalTo("snapshots")); // hardcoded in
        // deployAndSearch
      }
      else {
        assertThat(artifactHit.getLatestSnapshot(), nullValue());
        assertThat(artifactHit.getLatestSnapshotRepositoryId(), nullValue());
      }
    }
  }

  private SearchNGResponse deployAndSearch(int artifactCount, final DeployType deployType)
      throws Exception
  {
    final String key = "nexus5412";
    final Repository releases = repositoryRegistry.getRepository("releases");
    final Repository snapshots = repositoryRegistry.getRepository("snapshots");
    for (int i = 1; i <= artifactCount; i++) {
      if (deployType == DeployType.RELEASE || deployType == DeployType.BOTH) {
        deployDummyArtifact(releases, key, Integer.toString(i));
      }
      if (deployType == DeployType.SNAPSHOT || deployType == DeployType.BOTH) {
        deployDummyArtifact(snapshots, key, Integer.toString(i) + "-SNAPSHOT");
      }
    }
    wairForAsyncEventsToCalmDown();
    waitForTasksToStop();

    return search(key);
  }

  private SearchNGResponse search(final String key)
      throws Exception, ResourceException
  {
    final SearchNGIndexPlexusResource subject =
        (SearchNGIndexPlexusResource) lookup(PlexusResource.class, SearchNGIndexPlexusResource.ROLE_HINT);
    Context context = new Context();
    Request request = new Request();
    Reference ref = new Reference("http://localhost:12345/");
    request.setRootRef(ref);
    request.setResourceRef(new Reference(ref, SearchNGIndexPlexusResource.RESOURCE_URI + "?q=" + key
        + "&collapseresults=true"));
    Response response = new Response(request);

    // perform a search
    return subject.get(context, request, response, null);
  }

  private void deployDummyArtifact(final Repository releases, String key, String version)
      throws Exception
  {
    StringBuilder path = new StringBuilder();
    path.append("/org/").append(key);
    path.append('/').append(version);
    path.append('/').append(key).append('-').append(version).append(".jar");
    final ResourceStoreRequest request = new ResourceStoreRequest(path.toString());
    releases.storeItem(request, new ByteArrayInputStream("Junk JAR".getBytes()), null);
  }

  @Test
  public void versionCollation()
      throws Exception
  {
    wairForAsyncEventsToCalmDown();
    waitForTasksToStop();

    final Repository releases = repositoryRegistry.getRepository("releases");

    final String key = "nexus5422";

    deployDummyArtifact(releases, key, "2");
    deployDummyArtifact(releases, key, "2.0");
    deployDummyArtifact(releases, key, "2.0.0");
    deployDummyArtifact(releases, key, "2.0.0.0");
    deployDummyArtifact(releases, key, "2.0.0.0.0");

    wairForAsyncEventsToCalmDown();
    waitForTasksToStop();

    SearchNGResponse result = search(key);

    Assert.assertEquals(5, result.getData().size());
  }

  @Test
  public void multipleRepositories()
      throws Exception
  {
    wairForAsyncEventsToCalmDown();
    waitForTasksToStop();

    final Repository releases = repositoryRegistry.getRepository("releases");

    final M2Repository releases2 = (M2Repository) this.lookup(Repository.class, M2Repository.ID);
    final CRepository repoConfig = new DefaultCRepository();
    repoConfig.setId("releases2");
    repoConfig.setExposed(true);
    repoConfig.setProviderRole(Repository.class.getName());
    repoConfig.setProviderHint("maven2");
    releases2.configure(repoConfig);
    repositoryRegistry.addRepository(releases2);

    wairForAsyncEventsToCalmDown();
    waitForTasksToStop();

    final String key = "dummy";

    deployDummyArtifact(releases, key, "1");
    deployDummyArtifact(releases2, key, "1");
    deployDummyArtifact(releases, key, "2");
    deployDummyArtifact(releases2, key, "2");

    wairForAsyncEventsToCalmDown();
    waitForTasksToStop();

    SearchNGResponse result = search(key);

    Assert.assertEquals(2, result.getData().size());
    for (int i = 0; i < 2; i++) {
      NexusNGArtifact nexusNGArtifact = result.getData().get(0);
      Assert.assertEquals(2, nexusNGArtifact.getArtifactHits().size());
      Assert.assertEquals(releases.getId(), nexusNGArtifact.getArtifactHits().get(0).getRepositoryId());
      Assert.assertEquals(releases2.getId(), nexusNGArtifact.getArtifactHits().get(1).getRepositoryId());
    }
  }

  @Test
  public void emptyResult()
      throws Exception
  {
    wairForAsyncEventsToCalmDown();
    waitForTasksToStop();

    SearchNGResponse result = search("notfound");

    // have to bypass getXXX methods because they are not used by JSON renderer
    Assert.assertNotNull(Whitebox.getInternalState(result, "repoDetails"));
    Assert.assertNotNull(Whitebox.getInternalState(result, "data"));
  }

  @Test
  public void testUncollapseResults()
      throws Exception
  {
    fillInRepo();
    wairForAsyncEventsToCalmDown();
    waitForTasksToStop();

    final IndexerManager indexerManager = lookup(IndexerManager.class);
    indexerManager.reindexAllRepositories("/", true);

    SearchNGIndexPlexusResource subject =
        (SearchNGIndexPlexusResource) lookup(PlexusResource.class, SearchNGIndexPlexusResource.ROLE_HINT);

    Context context = new Context();
    Request request = new Request();
    Reference ref = new Reference("http://localhost:12345/");
    request.setRootRef(ref);
    request.setResourceRef(new Reference(ref, SearchNGIndexPlexusResource.RESOURCE_URI
        + "?q=nexus&collapseresults=true"));

    Response response = new Response(request);
    SearchNGResponse result = subject.get(context, request, response, null);

    // explanation:
    // we test here, does this resource "expand" the result set even if the request told to collaps
    // (like UI does). This happens when result set (the grid count in search UI) would contain less
    // rows than COLLAPSE_OVERRIDE_TRESHOLD = 35 lines. If yes, it will repeat the search but uncollapsed
    // kinda overriding the "hint" that was in original request (see request query parameters above).
    //
    // Found items uncollapsed (without any specific order, is unstable):
    // org.sonatype.nexus:nexus:1.3.0-SNAPSHOT
    // org.sonatype.nexus:nexus-indexer:1.0-beta-4
    // org.sonatype.nexus:nexus-indexer:1.0-beta-5-SNAPSHOT
    // org.sonatype.nexus:nexus-indexer:1.0-beta-4-SNAPSHOT
    // org.sonatype.nexus:nexus-indexer:1.0-beta-3-SNAPSHOT
    // org.sonatype.nexus:nexus:1.2.2-SNAPSHOT
    // org.sonatype:nexus-3148:1.0.SNAPSHOT
    //
    // Found items collapsed (G:A:maxVersion):
    // org.sonatype.nexus:nexus:1.3.0-SNAPSHOT
    // org.sonatype.nexus:nexus-indexer:1.0-beta-4 (rel preferred over snap)
    // org.sonatype:nexus-3148:1.0.SNAPSHOT

    // we assert that the grid would contain 7, not 3 hits (corresponds to grid lines in Search UI)
    Assert.assertEquals(7, result.getData().size());
  }
}
