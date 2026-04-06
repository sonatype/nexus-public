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

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.coreui.AssetXO;
import org.sonatype.nexus.coreui.ComponentHelper;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.query.PageResult;
import org.sonatype.nexus.repository.query.QueryOptions;
import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.selector.SelectorFactory;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class SelectorPreviewResourceTest
    extends Test5Support
{
  @Mock
  private ComponentHelper componentHelper;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private SelectorFactory selectorFactory;

  @InjectMocks
  private SelectorPreviewResource underTest;

  @Test
  void previewContentWithSpecificRepository() {
    Repository repository = mock(Repository.class);
    SelectorPreviewRequest request = new SelectorPreviewRequest();
    request.setRepository("my-repo");
    request.setType("csel");
    request.setExpression("path =^ \"/foo\"");

    when(repositoryManager.get("my-repo")).thenReturn(repository);
    PageResult<AssetXO> expectedResult = new PageResult<>(5, List.of(new AssetXO()));
    when(componentHelper.previewAssets(any(RepositorySelector.class), anyList(), anyString(), any(QueryOptions.class)))
        .thenReturn(expectedResult);

    PageResult<AssetXO> result = underTest.previewContent(request);

    assertThat(result, is(notNullValue()));
    assertThat(result.getTotal(), is(5L));
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

    PageResult<AssetXO> result = underTest.previewContent(request);

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

    PageResult<AssetXO> result = underTest.previewContent(request);

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

    PageResult<AssetXO> result = underTest.previewContent(request);

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

    PageResult<AssetXO> result = underTest.previewContent(request);

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
}
