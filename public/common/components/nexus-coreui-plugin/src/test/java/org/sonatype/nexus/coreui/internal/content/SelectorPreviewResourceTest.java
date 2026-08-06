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
package org.sonatype.nexus.coreui.internal.content;

import java.util.List;

import org.sonatype.nexus.coreui.AssetXO;
import org.sonatype.nexus.coreui.ComponentHelper;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.query.PageResult;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.selector.SelectorFactory;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class SelectorPreviewResourceTest
{
  @Mock
  private ComponentHelper componentHelper;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private SelectorFactory selectorFactory;

  @Mock
  private RepositoryPermissionChecker repositoryPermissionChecker;

  @InjectMocks
  private SelectorPreviewResource underTest;

  @BeforeEach
  void setUp() {
    // Default pass-through: every collected repository is permitted. Use lenient() + doAnswer so
    // stub overrides in individual tests don't re-invoke this lambda with a null argument during
    // Mockito's `when()` recording, and unused stubbing doesn't fail strict tests.
    Mockito.lenient()
        .doAnswer(inv -> Lists.newArrayList(inv.<Iterable<Repository>>getArgument(0)))
        .when(repositoryPermissionChecker)
        .userCanBrowseRepositories(ArgumentMatchers.<Iterable<Repository>>any());
  }

  @Test
  void previewContentWithSpecificRepository() {
    Repository repository = mock(Repository.class);
    SelectorPreviewRequest request = new SelectorPreviewRequest();
    request.setRepository("my-repo");
    request.setType("csel");
    request.setExpression("path =^ \"/foo\"");

    when(repositoryManager.get("my-repo")).thenReturn(repository);
    AssetXO asset = new AssetXO();
    asset.setName("/foo/bar.jar");
    asset.setRepositoryName("my-repo");
    PageResult<AssetXO> helperResult = new PageResult<>(5, List.of(asset));
    when(componentHelper.previewAssets(any(RepositorySelector.class), anyList(), anyString(), any(QueryOptions.class)))
        .thenReturn(helperResult);

    PageResult<SelectorPreviewAssetXO> result = underTest.previewContent(request);

    assertThat(result, is(notNullValue()));
    assertThat(result.getTotal(), is(5L));
    assertThat(result.getResults(), hasSize(1));
    assertThat(result.getResults().get(0).name(), is("/foo/bar.jar"));
    assertThat(result.getResults().get(0).repositoryName(), is("my-repo"));
    verify(selectorFactory).validateSelector("csel", "path =^ \"/foo\"");
    verify(repositoryManager).get("my-repo");
  }

  @Test
  void previewContentWithAllRepositories() {
    Repository repo1 = mock(Repository.class);
    Repository repo2 = mock(Repository.class);

    SelectorPreviewRequest request = new SelectorPreviewRequest();
    request.setRepository("*");
    request.setType("CSEL");
    request.setExpression("path =^ \"/bar\"");

    when(repositoryManager.browse()).thenReturn(List.of(repo1, repo2));
    PageResult<AssetXO> expectedResult = new PageResult<>(10, List.of(new AssetXO()));
    when(componentHelper.previewAssets(any(RepositorySelector.class), anyList(), anyString(), any(QueryOptions.class)))
        .thenReturn(expectedResult);

    PageResult<SelectorPreviewAssetXO> result = underTest.previewContent(request);

    assertThat(result, is(notNullValue()));
    verify(selectorFactory).validateSelector("csel", "path =^ \"/bar\"");
  }

  @Test
  void previewContentWithAllOfFormat() {
    Repository mavenRepo = mock(Repository.class);
    Format mavenFormat = mock(Format.class);
    when(mavenFormat.toString()).thenReturn("maven2");
    when(mavenRepo.getFormat()).thenReturn(mavenFormat);

    Repository npmRepo = mock(Repository.class);
    Format npmFormat = mock(Format.class);
    when(npmFormat.toString()).thenReturn("npm");
    when(npmRepo.getFormat()).thenReturn(npmFormat);

    SelectorPreviewRequest request = new SelectorPreviewRequest();
    // The format prefix is "*-" so "*-maven2" selects all repos of format maven2
    request.setRepository("*-maven2");
    request.setType("csel");
    request.setExpression("path =^ \"/com\"");

    when(repositoryManager.browse()).thenReturn(List.of(mavenRepo, npmRepo));
    PageResult<AssetXO> expectedResult = new PageResult<>(3, List.of(new AssetXO()));
    when(componentHelper.previewAssets(any(RepositorySelector.class), anyList(), eq("path =^ \"/com\""),
        any(QueryOptions.class))).thenReturn(expectedResult);

    PageResult<SelectorPreviewAssetXO> result = underTest.previewContent(request);

    assertThat(result, is(notNullValue()));
    verify(selectorFactory).validateSelector("csel", "path =^ \"/com\"");
  }

  @Test
  void previewContentWithAllRepositoriesReturnsEmptyWhenNoBrowseResults() {
    SelectorPreviewRequest request = new SelectorPreviewRequest();
    request.setRepository("*");
    request.setType("csel");
    request.setExpression("path =^ \"/test\"");

    when(repositoryManager.browse()).thenReturn(emptyList());

    PageResult<SelectorPreviewAssetXO> result = underTest.previewContent(request);

    assertThat(result, is(notNullValue()));
    assertThat(result.getTotal(), is(0L));
    assertThat(result.getResults(), is(empty()));
    verify(selectorFactory).validateSelector("csel", "path =^ \"/test\"");
    verifyNoInteractions(componentHelper);
  }

  @Test
  void previewContentWithFormatFilterReturnsEmptyWhenNoReposMatchFormat() {
    Repository npmRepo = mock(Repository.class);
    Format npmFormat = mock(Format.class);
    when(npmFormat.toString()).thenReturn("npm");
    when(npmRepo.getFormat()).thenReturn(npmFormat);

    SelectorPreviewRequest request = new SelectorPreviewRequest();
    request.setRepository("*-maven2");
    request.setType("csel");
    request.setExpression("path =^ \"/org\"");

    when(repositoryManager.browse()).thenReturn(List.of(npmRepo));

    PageResult<SelectorPreviewAssetXO> result = underTest.previewContent(request);

    assertThat(result, is(notNullValue()));
    assertThat(result.getTotal(), is(0L));
    assertThat(result.getResults(), is(empty()));
    verifyNoInteractions(componentHelper);
  }

  @Test
  void previewContentValidatesSelectorExpression() {
    Repository repository = mock(Repository.class);
    SelectorPreviewRequest request = new SelectorPreviewRequest();
    request.setRepository("my-repo");
    request.setType("CSEL");
    request.setExpression("path == \"/test\"");

    when(repositoryManager.get("my-repo")).thenReturn(repository);
    PageResult<AssetXO> expectedResult = new PageResult<>(0, emptyList());
    when(componentHelper.previewAssets(any(RepositorySelector.class), anyList(), anyString(), any(QueryOptions.class)))
        .thenReturn(expectedResult);

    underTest.previewContent(request);

    // Type should be lowercased when passed to validateSelector
    verify(selectorFactory).validateSelector("csel", "path == \"/test\"");
  }

  @Test
  void previewContentReturnsEmptyWhenCallerCannotBrowseAny() {
    Repository repo1 = mock(Repository.class);
    Repository repo2 = mock(Repository.class);

    SelectorPreviewRequest request = new SelectorPreviewRequest();
    request.setRepository("*");
    request.setType("csel");
    request.setExpression("path =^ \"/priv\"");

    when(repositoryManager.browse()).thenReturn(List.of(repo1, repo2));
    doReturn(emptyList()).when(repositoryPermissionChecker)
        .userCanBrowseRepositories(ArgumentMatchers.<Iterable<Repository>>any());

    PageResult<SelectorPreviewAssetXO> result = underTest.previewContent(request);

    assertThat(result, is(notNullValue()));
    assertThat(result.getTotal(), is(0L));
    assertThat(result.getResults(), is(empty()));
    verifyNoInteractions(componentHelper);
  }

  @Test
  void previewContentFiltersToPermittedSubsetOnPartialBrowse() {
    Repository permittedRepo = mock(Repository.class);
    Repository forbiddenRepo = mock(Repository.class);

    SelectorPreviewRequest request = new SelectorPreviewRequest();
    request.setRepository("*");
    request.setType("csel");
    request.setExpression("path =^ \"/mixed\"");

    when(repositoryManager.browse()).thenReturn(List.of(permittedRepo, forbiddenRepo));
    doReturn(List.of(permittedRepo)).when(repositoryPermissionChecker)
        .userCanBrowseRepositories(ArgumentMatchers.<Iterable<Repository>>any());
    PageResult<AssetXO> expectedResult = new PageResult<>(2, List.of(new AssetXO()));
    when(componentHelper.previewAssets(any(RepositorySelector.class), anyList(), anyString(), any(QueryOptions.class)))
        .thenReturn(expectedResult);

    PageResult<SelectorPreviewAssetXO> result = underTest.previewContent(request);

    assertThat(result, is(notNullValue()));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Repository>> repoCaptor = ArgumentCaptor.forClass(List.class);
    verify(componentHelper).previewAssets(any(RepositorySelector.class), repoCaptor.capture(), anyString(),
        any(QueryOptions.class));
    assertThat(repoCaptor.getValue(), contains(permittedRepo));
  }

  @Test
  void previewContentReturnsEmptyWhenSpecificRepositoryNotBrowsable() {
    Repository repository = mock(Repository.class);
    SelectorPreviewRequest request = new SelectorPreviewRequest();
    request.setRepository("secret-repo");
    request.setType("csel");
    request.setExpression("path =^ \"/nope\"");

    when(repositoryManager.get("secret-repo")).thenReturn(repository);
    doReturn(emptyList()).when(repositoryPermissionChecker)
        .userCanBrowseRepositories(ArgumentMatchers.<Iterable<Repository>>any());

    PageResult<SelectorPreviewAssetXO> result = underTest.previewContent(request);

    assertThat(result, is(notNullValue()));
    assertThat(result.getTotal(), is(0L));
    assertThat(result.getResults(), is(empty()));
    verifyNoInteractions(componentHelper);
  }

  @Test
  void previewContentReturnsEmptyWhenSpecificRepositoryNotFound() {
    SelectorPreviewRequest request = new SelectorPreviewRequest();
    request.setRepository("does-not-exist");
    request.setType("csel");
    request.setExpression("path =^ \"/gone\"");

    when(repositoryManager.get("does-not-exist")).thenReturn(null);

    PageResult<SelectorPreviewAssetXO> result = underTest.previewContent(request);

    assertThat(result, is(notNullValue()));
    assertThat(result.getTotal(), is(0L));
    assertThat(result.getResults(), is(empty()));
    verifyNoInteractions(componentHelper);
    verify(selectorFactory).validateSelector("csel", "path =^ \"/gone\"");
  }

  @Test
  void previewContentResponseContainsOnlyNameAndRepositoryName() {
    Repository repository = mock(Repository.class);
    SelectorPreviewRequest request = new SelectorPreviewRequest();
    request.setRepository("my-repo");
    request.setType("csel");
    request.setExpression("path =^ \"/foo\"");

    when(repositoryManager.get("my-repo")).thenReturn(repository);

    // Full-shape AssetXO from the helper — everything except name/repositoryName must be dropped.
    AssetXO fullAsset = new AssetXO();
    fullAsset.setName("/foo/bar.jar");
    fullAsset.setRepositoryName("my-repo");
    fullAsset.setBlobRef("blob-ref-should-not-leak");
    fullAsset.setContentType("application/java-archive");
    fullAsset.setCreatedBy("uploader-should-not-leak");
    fullAsset.setCreatedByIp("192.0.2.1");
    fullAsset.setSize(12345L);
    fullAsset.setId("internal-id-should-not-leak");
    // Helper's total (99) intentionally exceeds the results list size (1): total is the DB-count
    // across permitted repos while results is limit-truncated. The resource forwards total
    // verbatim, so callers see the same paginated shape as before this slim-DTO refactor.
    PageResult<AssetXO> helperResult = new PageResult<>(99, List.of(fullAsset));
    when(componentHelper.previewAssets(any(RepositorySelector.class), anyList(), anyString(), any(QueryOptions.class)))
        .thenReturn(helperResult);

    PageResult<SelectorPreviewAssetXO> result = underTest.previewContent(request);

    // Total is forwarded from the helper verbatim — do NOT derive it from result.getResults().size().
    // The mismatch is intentional (total across all matches vs. limit-truncated page).
    assertThat(result.getTotal(), is(99L));
    assertThat(result.getResults(), hasSize(1));
    SelectorPreviewAssetXO xo = result.getResults().get(0);
    assertThat(xo.name(), is("/foo/bar.jar"));
    assertThat(xo.repositoryName(), is("my-repo"));
    // Record has exactly two components — no accessor exists for anything else.
    assertThat(xo.getClass().getRecordComponents().length, is(2));
  }
}
