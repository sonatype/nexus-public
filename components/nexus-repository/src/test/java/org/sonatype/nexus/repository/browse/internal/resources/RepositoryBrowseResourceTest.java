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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
import org.sonatype.nexus.repository.browse.internal.model.BrowseListItem;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.BrowseNode;
import org.sonatype.nexus.repository.storage.BrowseNodeStore;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
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

public class RepositoryBrowseResourceTest
    extends TestSupport
{
  private static final String URL_PREFIX = "http://localhost:8888/service/siesta/repository/browse/";
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

  private RepositoryBrowseResource underTest;

  @Before
  public void before() throws Exception {
    when(uriInfo.getAbsolutePath()).thenReturn(UriBuilder.fromPath(URL_PREFIX + "central/").build());

    when(securityHelper.allPermitted(any())).thenReturn(true);
    when(templateHelper.parameters()).thenReturn(new TemplateParameters());
    when(templateHelper.render(any(),any())).thenReturn("something");
    when(repository.getName()).thenReturn("repositoryName");
    when(repository.getFormat()).thenReturn(new Format("format") {});
    when(repository.getType()).thenReturn(new ProxyType());

    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(storageFacet.txSupplier()).thenReturn(txSupplier);
    when(txSupplier.get()).thenReturn(storageTx);

    when(storageTx.findAsset(any(), any())).thenReturn(asset);

    when(browseNodeStore.getChildrenByPath(repository, Collections.emptyList(), null)).thenReturn(
        Collections.singleton(new BrowseNode().withRepositoryName("repositoryName").withPath("org")));
    when(browseNodeStore.getChildrenByPath(repository, Collections.singletonList("org"), null))
        .thenReturn(Collections.singleton(new BrowseNode().withRepositoryName("repositoryName").withPath("sonatype")));
    when(repositoryManager.get("repositoryName")).thenReturn(repository);

    underTest = new RepositoryBrowseResource(repositoryManager, browseNodeStore, templateHelper, securityHelper);
  }

  @Test
  public void validateRootRequest() throws Exception {
    when(uriInfo.getAbsolutePath()).thenReturn(UriBuilder.fromPath(URL_PREFIX + "central/").build());

    underTest.getHtml("repositoryName", "", uriInfo);

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

    underTest.getHtml("repositoryName", "org/", uriInfo);

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
  public void validateOrderedEntries() throws Exception {
    List<BrowseNode> nodes = asList(
        new BrowseNode().withRepositoryName("repositoryName").withPath("a.txt").withAssetId(mock(EntityId.class)),
        new BrowseNode().withRepositoryName("repositoryName").withPath("Org"),
        new BrowseNode().withRepositoryName("repositoryName").withPath("com"),
        new BrowseNode().withRepositoryName("repositoryName").withPath("B.txt").withAssetId(mock(EntityId.class)));
    when(browseNodeStore.getChildrenByPath(repository, Collections.emptyList(), null)).thenReturn(nodes);

    underTest.getHtml("repositoryName", "", uriInfo);

    ArgumentCaptor<TemplateParameters> argument = ArgumentCaptor.forClass(TemplateParameters.class);
    verify(templateHelper).render(any(), argument.capture());

    List<BrowseListItem> listItems = (List<BrowseListItem>) argument.getValue().get().get("listItems");
    assertThat(listItems.size(), is(4));

    assertThat(listItems.get(0).getName(), is("com"));
    assertThat(listItems.get(1).getName(), is("Org"));
    assertThat(listItems.get(2).getName(), is("a.txt"));
    assertThat(listItems.get(3).getName(), is("B.txt"));
  }

  @Test
  public void validateAsset() throws Exception {
    List<BrowseNode> nodes = asList(new BrowseNode().withRepositoryName("repositoryName").withPath("a.txt")
        .withAssetId(mock(EntityId.class)));
    when(asset.size()).thenReturn(1024L);
    when(asset.blobUpdated()).thenReturn(new DateTime(0));
    when(asset.name()).thenReturn("a1.txt");
    when(repository.getUrl()).thenReturn("http://foo/bar");
    when(browseNodeStore.getChildrenByPath(repository, Collections.emptyList(), null)).thenReturn(nodes);

    underTest.getHtml("repositoryName", "", uriInfo);

    ArgumentCaptor<TemplateParameters> argument = ArgumentCaptor.forClass(TemplateParameters.class);
    verify(templateHelper).render(any(), argument.capture());

    List<BrowseListItem> listItems = (List<BrowseListItem>) argument.getValue().get().get("listItems");
    assertThat(listItems.size(), is(1));

    BrowseListItem item = listItems.get(0);
    assertThat(item.getName(), is("a.txt"));
    assertThat(item.getSize(), is("1024"));
    assertThat(item.getLastModified(), is(String.valueOf(asset.blobUpdated())));
    assertThat(item.getResourceUri(), is("http://foo/bar/a1.txt"));
  }

  @Test
  public void validateRootRequestWithoutTrailingSlash() throws Exception {
    when(uriInfo.getAbsolutePath()).thenReturn(
        UriBuilder.fromPath(URL_PREFIX + "central").build());

    Response response = underTest.getHtml("repositoryName", "", uriInfo);
    assertThat(response.getStatus(), is(303));
    assertThat(response.getHeaders().get("location").get(0).toString(), is(URL_PREFIX + "central/"));
  }

  @Test
  public void validateNonRootRequestWithoutTrailingSlash() throws Exception {
    when(uriInfo.getAbsolutePath()).thenReturn(
        UriBuilder.fromPath(URL_PREFIX + "central/org").build());

    Response response = underTest.getHtml("repositoryName", "org", uriInfo);
    assertThat(response.getStatus(), is(303));
    assertThat(response.getHeaders().get("location").get(0).toString(), is(URL_PREFIX + "central/org/"));
  }

  @Test
  public void validatePathNotFoundRequest() throws Exception {
    expectedException.expect(WebApplicationException.class);
    expectedException.expectMessage("Path not found");

    underTest.getHtml("repositoryName", "missing", uriInfo);
  }

  @Test
  public void validateRepositoryNotFoundRequest() throws Exception {
    expectedException.expect(WebApplicationException.class);
    expectedException.expectMessage("Repository not found");

    underTest.getHtml("missing", "org", uriInfo);
  }

  @Test
  public void validateRepositoryNotAuthorizedRequest() throws Exception {
    when(securityHelper.allPermitted(any())).thenReturn(false);
    expectedException.expect(WebApplicationException.class);
    expectedException.expectMessage("Repository not found");

    underTest.getHtml("repositoryName", "org", uriInfo);
  }
}
