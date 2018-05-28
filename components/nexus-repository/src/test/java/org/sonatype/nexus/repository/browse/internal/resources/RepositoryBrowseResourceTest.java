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
package org.sonatype.nexus.repository.browse.internal.resources;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.BrowseNodeConfiguration;
import org.sonatype.nexus.repository.browse.internal.model.BrowseListItem;
import org.sonatype.nexus.repository.group.GroupFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.BrowseNode;
import org.sonatype.nexus.repository.storage.BrowseNodeStore;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.BucketStore;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.security.SecurityHelper;

import com.google.common.base.Supplier;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class RepositoryBrowseResourceTest
    extends TestSupport
{
  private static final String URL_PREFIX = "http://localhost:8888/service/rest/repository/browse/";

  private static final String REPOSITORY_NAME = "testRepository";

  private final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private TemplateHelper templateHelper;

  @Mock
  private BrowseNodeStore browseNodeStore;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Repository repository;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private UriInfo uriInfo;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private Supplier<StorageTx> txSupplier;

  @Mock
  private StorageTx storageTx;

  @Mock
  private Asset asset;

  private BrowseNodeConfiguration configuration = new BrowseNodeConfiguration();

  @Mock
  private BucketStore bucketStore;

  @Mock
  private Bucket bucket;

  private RepositoryBrowseResource underTest;

  @Before
  public void before() throws Exception {
    when(uriInfo.getAbsolutePath()).thenReturn(UriBuilder.fromPath(URL_PREFIX + "central/").build());

    when(securityHelper.allPermitted(any())).thenReturn(true);
    when(templateHelper.parameters()).thenReturn(new TemplateParameters());
    when(templateHelper.render(any(),any())).thenReturn("something");
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getFormat()).thenReturn(new Format("format") {});
    when(repository.getType()).thenReturn(new ProxyType());

    when(repository.optionalFacet(GroupFacet.class)).thenReturn(Optional.empty());

    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(storageFacet.txSupplier()).thenReturn(txSupplier);
    when(txSupplier.get()).thenReturn(storageTx);

    when(storageTx.findAsset(any())).thenReturn(asset);

    EntityId bucketId = mock(EntityId.class);
    when(asset.bucketId()).thenReturn(bucketId);
    when(bucketStore.getById(bucketId)).thenReturn(bucket);
    when(bucket.getRepositoryName()).thenReturn(REPOSITORY_NAME);


    when(browseNodeStore
        .getByPath(repository, Collections.emptyList(), configuration.getMaxHtmlNodes(), null))
        .thenReturn(Collections.singleton(browseNode("org")));
    when(browseNodeStore
        .getByPath(repository, Collections.singletonList("org"), configuration.getMaxHtmlNodes(), null))
        .thenReturn(Collections.singleton(browseNode("sonatype")));
    when(repositoryManager.get(REPOSITORY_NAME)).thenReturn(repository);

    underTest = new RepositoryBrowseResource(repositoryManager, browseNodeStore, configuration, bucketStore,
        templateHelper, securityHelper);
  }

  @Test
  public void validateRootRequest() throws Exception {
    when(uriInfo.getAbsolutePath()).thenReturn(UriBuilder.fromPath(URL_PREFIX + "central/").build());

    underTest.getHtml(REPOSITORY_NAME, "", null, uriInfo);

    ArgumentCaptor<TemplateParameters> argument = ArgumentCaptor.forClass(TemplateParameters.class);
    verify(templateHelper).render(any(), argument.capture());
    assertThat(argument.getValue().get().get("requestPath"), is("/"));
    assertThat(((Collection<BrowseListItem>)argument.getValue().get().get("listItems")).size(), is(1));
    assertThat(((Collection<BrowseListItem>) argument.getValue().get().get("listItems")).iterator().next().getName(),
        is("org"));
    assertThat(
        ((Collection<BrowseListItem>) argument.getValue().get().get("listItems")).iterator().next().getResourceUri(),
        is("org/"));
    assertThat(
        ((Collection<BrowseListItem>) argument.getValue().get().get("listItems")).iterator().next().isCollection(),
        is(true));
  }

  @Test
  public void validateNonRootRequest() throws Exception {
    when(uriInfo.getAbsolutePath()).thenReturn(UriBuilder.fromPath(URL_PREFIX + "central/org/").build());

    underTest.getHtml(REPOSITORY_NAME, "org/", null, uriInfo);

    ArgumentCaptor<TemplateParameters> argument = ArgumentCaptor.forClass(TemplateParameters.class);
    verify(templateHelper).render(any(), argument.capture());
    assertThat(argument.getValue().get().get("requestPath"), is("/org/"));
    assertThat(((Collection<BrowseListItem>)argument.getValue().get().get("listItems")).size(), is(1));
    assertThat(((Collection<BrowseListItem>) argument.getValue().get().get("listItems")).iterator().next().getName(),
        is("sonatype"));
    assertThat(
        ((Collection<BrowseListItem>) argument.getValue().get().get("listItems")).iterator().next().getResourceUri(),
        is("sonatype/"));
    assertThat(
        ((Collection<BrowseListItem>) argument.getValue().get().get("listItems")).iterator().next().isCollection(),
        is(true));
  }

  @Test
  public void validateAsset() throws Exception {
    List<BrowseNode> nodes = asList(browseNode("a.txt", mock(EntityId.class), true));
    when(asset.size()).thenReturn(1024L);
    when(asset.blobUpdated()).thenReturn(new DateTime(0));
    when(asset.name()).thenReturn("a1.txt");
    when(repository.getUrl()).thenReturn("http://foo/bar");
    when(browseNodeStore.getByPath(repository, Collections.emptyList(), configuration.getMaxHtmlNodes(), null))
        .thenReturn(nodes);

    underTest.getHtml(REPOSITORY_NAME, "", null, uriInfo);

    ArgumentCaptor<TemplateParameters> argument = ArgumentCaptor.forClass(TemplateParameters.class);
    verify(templateHelper).render(any(), argument.capture());

    List<BrowseListItem> listItems = (List<BrowseListItem>) argument.getValue().get().get("listItems");
    assertThat(listItems.size(), is(1));

    BrowseListItem item = listItems.get(0);
    assertThat(item.getName(), is("a.txt"));
    assertThat(item.getSize(), is("1024"));
    assertThat(item.getLastModified(), is(format.format(asset.blobUpdated().toDate())));
    assertThat(item.getResourceUri(), is("http://foo/bar/a1.txt"));
  }

  @Test
  public void validateAsset_groupRepository() throws Exception {
    String filter = "test";
    Repository groupRepository = mock(Repository.class);
    when(groupRepository.getType()).thenReturn(new GroupType());
    when(groupRepository.getName()).thenReturn("group-repository");
    when(groupRepository.getFormat()).thenReturn(new Format("format") {});
    when(groupRepository.facet(StorageFacet.class)).thenReturn(storageFacet);
    GroupFacet groupFacet = mock(GroupFacet.class);
    when(groupFacet.allMembers()).thenReturn(Arrays.asList(groupRepository, repository));
    when(groupRepository.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet));
    when(repositoryManager.get("group-repository")).thenReturn(groupRepository);

    List<BrowseNode> nodes = asList(browseNode("a.txt", mock(EntityId.class), true));
    when(asset.size()).thenReturn(1024L);
    when(asset.blobUpdated()).thenReturn(new DateTime(0));
    when(asset.name()).thenReturn("a1.txt");
    when(groupRepository.getUrl()).thenReturn("http://foo/bar");
    when(browseNodeStore
        .getByPath(groupRepository, Collections.emptyList(), configuration.getMaxHtmlNodes(), filter))
        .thenReturn(nodes);

    underTest.getHtml("group-repository", "", filter, uriInfo);

    ArgumentCaptor<TemplateParameters> argument = ArgumentCaptor.forClass(TemplateParameters.class);
    verify(templateHelper).render(any(), argument.capture());

    List<BrowseListItem> listItems = (List<BrowseListItem>) argument.getValue().get().get("listItems");
    assertThat(listItems.size(), is(1));

    BrowseListItem item = listItems.get(0);
    assertThat(item.getName(), is("a.txt"));
    assertThat(item.getSize(), is("1024"));
    assertThat(item.getLastModified(), is(format.format(asset.blobUpdated().toDate())));
    assertThat(item.getResourceUri(), is("http://foo/bar/a1.txt"));
  }

  @Test
  public void validateFilterAppliedToNonAssetUrls() throws Exception {
    String filter = "test/test";
    Repository groupRepository = mock(Repository.class);
    when(groupRepository.getType()).thenReturn(new GroupType());
    when(groupRepository.getName()).thenReturn("group-repository");
    when(groupRepository.getFormat()).thenReturn(new Format("format") { });
    when(groupRepository.facet(StorageFacet.class)).thenReturn(storageFacet);
    GroupFacet groupFacet = mock(GroupFacet.class);
    when(groupFacet.allMembers()).thenReturn(Arrays.asList(groupRepository, repository));
    when(groupRepository.optionalFacet(GroupFacet.class)).thenReturn(Optional.of(groupFacet));
    when(repositoryManager.get("group-repository")).thenReturn(groupRepository);

    List<BrowseNode> nodes = asList(browseNode("bar"));
    when(groupRepository.getUrl()).thenReturn("http://foo/");
    when(browseNodeStore
        .getByPath(groupRepository, Collections.emptyList(), configuration.getMaxHtmlNodes(), filter))
        .thenReturn(nodes);

    underTest.getHtml("group-repository", "", filter, uriInfo);

    ArgumentCaptor<TemplateParameters> argument = ArgumentCaptor.forClass(TemplateParameters.class);
    verify(templateHelper).render(any(), argument.capture());

    List<BrowseListItem> listItems = (List<BrowseListItem>) argument.getValue().get().get("listItems");
    assertThat(listItems.size(), is(1));

    BrowseListItem item = listItems.get(0);
    assertThat(item.getName(), is("bar"));
    assertThat(item.getResourceUri(), is("bar/?filter=test%2Ftest"));
  }

  @Test
  public void validateRootRequestWithoutTrailingSlash() throws Exception {
    when(uriInfo.getAbsolutePath()).thenReturn(
        UriBuilder.fromPath(URL_PREFIX + "central").build());

    Response response = underTest.getHtml(REPOSITORY_NAME, "", null, uriInfo);
    assertThat(response.getStatus(), is(303));
    assertThat(response.getHeaders().get("location").get(0).toString(), is(URL_PREFIX + "central/"));
  }

  @Test
  public void validateNonRootRequestWithoutTrailingSlash() throws Exception {
    when(uriInfo.getAbsolutePath()).thenReturn(
        UriBuilder.fromPath(URL_PREFIX + "central/org").build());

    Response response = underTest.getHtml(REPOSITORY_NAME, "org", "", uriInfo);
    assertThat(response.getStatus(), is(303));
    assertThat(response.getHeaders().get("location").get(0).toString(), is(URL_PREFIX + "central/org/"));
  }

  @Test
  public void validatePathNotFoundRequest() throws Exception {
    expectedException.expect(WebApplicationException.class);
    expectedException.expectMessage("Path not found");

    underTest.getHtml(REPOSITORY_NAME, "missing", "", uriInfo);
  }

  @Test
  public void validatePathNotFoundRequestNotAuthorized() throws Exception {
    when(securityHelper.allPermitted(any())).thenReturn(false);
    expectedException.expect(WebApplicationException.class);
    expectedException.expectMessage("Repository not found");

    underTest.getHtml(REPOSITORY_NAME, "missing", null, uriInfo);
  }

  @Test
  public void validateRepositoryNotFoundRequest() throws Exception {
    expectedException.expect(WebApplicationException.class);
    expectedException.expectMessage("Repository not found");

    underTest.getHtml("missing", "org", "", uriInfo);
  }

  @Test
  public void validateRepositoryNotAuthorizedRequest() throws Exception {
    when(browseNodeStore.getByPath(repository, Collections.singletonList("org"),
        configuration.getMaxHtmlNodes(), null))
        .thenReturn(Collections.emptyList());
    when(securityHelper.allPermitted(any())).thenReturn(false);
    expectedException.expect(WebApplicationException.class);
    expectedException.expectMessage("Repository not found");

    underTest.getHtml(REPOSITORY_NAME, "org", "", uriInfo);
  }

  @Test
  public void validateRepositoryWithNoBrowseNodesRequest() throws Exception {
    when(browseNodeStore.getByPath(repository, Collections.emptyList(),
        configuration.getMaxHtmlNodes(), ""))
        .thenReturn(Collections.emptyList());

    underTest.getHtml(REPOSITORY_NAME, "", "", uriInfo);

    ArgumentCaptor<TemplateParameters> argument = ArgumentCaptor.forClass(TemplateParameters.class);
    verify(templateHelper).render(any(), argument.capture());
    assertThat(((Collection<BrowseListItem>)argument.getValue().get().get("listItems")).size(), is(0));
  }

  @Test
  public void validateRepositoryWithNoBrowseNodesRequest_nullResponseFromGetChildrenByPath() throws Exception {
    underTest.getHtml(REPOSITORY_NAME, "", "", uriInfo);

    ArgumentCaptor<TemplateParameters> argument = ArgumentCaptor.forClass(TemplateParameters.class);
    verify(templateHelper).render(any(), argument.capture());
    assertThat(((Collection<BrowseListItem>)argument.getValue().get().get("listItems")).size(), is(0));
  }

  @Test
  public void validateAssetFoldersAreTreatedLikeFolders() {
    BrowseNode folderAsset = browseNode("folderAsset", mock(EntityId.class), false);
    when(browseNodeStore.getByPath(repository, Collections.emptyList(), configuration.getMaxHtmlNodes(), null))
        .thenReturn(asList(folderAsset));

    underTest.getHtml(REPOSITORY_NAME, "", null, uriInfo);

    ArgumentCaptor<TemplateParameters> argument = ArgumentCaptor.forClass(TemplateParameters.class);
    verify(templateHelper).render(any(), argument.capture());

    List<BrowseListItem> listItems = (List<BrowseListItem>) argument.getValue().get().get("listItems");
    assertThat(listItems.size(), is(1));

    assertThat(listItems.get(0).getName(), is("folderAsset"));
    assertThat(listItems.get(0).getResourceUri(), is("folderAsset/"));
    assertThat(listItems.get(0).isCollection(), is(true));
  }

  @Test
  public void validateAssetUriIsUrlEscapedToPreventXss() {
    BrowseNode browseNode = browseNode("<img src=\"foo\">");
    when(browseNodeStore.getByPath(repository, Collections.emptyList(), configuration.getMaxHtmlNodes(), null))
        .thenReturn(asList(browseNode));

    underTest.getHtml(REPOSITORY_NAME, "", null, uriInfo);

    ArgumentCaptor<TemplateParameters> argument = ArgumentCaptor.forClass(TemplateParameters.class);
    verify(templateHelper).render(any(), argument.capture());

    List<BrowseListItem> listItems = (List<BrowseListItem>) argument.getValue().get().get("listItems");
    assertThat(listItems.size(), is(1));

    assertThat(listItems.get(0).getName(), is("<img src=\"foo\">"));
    assertThat(listItems.get(0).getResourceUri(), is("%3Cimg%20src%3D%22foo%22%3E/"));
  }

  private BrowseNode browseNode(final String name) {
    BrowseNode node = new BrowseNode();
    node.setRepositoryName(REPOSITORY_NAME);
    node.setName(name);
    return node;
  }

  private BrowseNode browseNode(final String name, final EntityId assetId, final boolean leaf) {
    BrowseNode node = new BrowseNode();
    node.setRepositoryName(REPOSITORY_NAME);
    node.setName(name);
    node.setAssetId(assetId);
    node.setLeaf(leaf);
    return node;
  }
}
