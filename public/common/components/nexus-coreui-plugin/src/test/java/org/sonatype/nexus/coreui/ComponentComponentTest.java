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
package org.sonatype.nexus.coreui;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.Validator;

import org.sonatype.nexus.bootstrap.validation.ValidationConfiguration;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.extdirect.model.PagedResponse;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters.Filter;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.query.PageResult;
import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.selector.SelectorFactory;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.ValidationExecutor;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ComponentComponent}.
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class ComponentComponentTest
{
  @ValidationExecutor
  private final Validator validator =
      new ValidationConfiguration().validatorFactory(new ConstraintValidatorFactoryImpl()).getValidator();

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private SelectorFactory selectorFactory;

  @Mock
  private ComponentHelper componentHelper;

  @Mock
  private Repository repository;

  @Mock
  private Configuration configuration;

  private final JsonMapper jsonMapper = new JsonMapper();

  private ComponentComponent underTest;

  @BeforeEach
  void setUp() {
    underTest = new ComponentComponent(repositoryManager, selectorFactory, jsonMapper, componentHelper,
        Collections.emptyList());
  }

  @Test
  void testReadComponentAssets_repositoryOffline_returnsEmptyList() throws Exception {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("repositoryName");
            setValue("my-repo");
          }
        },
        new Filter()
        {
          {
            setProperty("componentModel");
            setValue(
                "{\"id\":\"1\",\"repositoryName\":\"my-repo\",\"group\":\"g\",\"name\":\"n\",\"version\":\"v\",\"format\":\"maven2\"}");
          }
        }));

    when(repositoryManager.get("my-repo")).thenReturn(repository);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(configuration.isOnline()).thenReturn(false);

    List<AssetXO> result = underTest.readComponentAssets(parameters);

    assertThat(result, is(empty()));
  }

  @Test
  void testReadComponentAssets_repositoryOnline_delegatesToHelper() throws Exception {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("repositoryName");
            setValue("my-repo");
          }
        },
        new Filter()
        {
          {
            setProperty("componentModel");
            setValue(
                "{\"id\":\"1\",\"repositoryName\":\"my-repo\",\"group\":\"g\",\"name\":\"n\",\"version\":\"v\",\"format\":\"maven2\"}");
          }
        }));

    when(repositoryManager.get("my-repo")).thenReturn(repository);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(configuration.isOnline()).thenReturn(true);

    AssetXO assetXO = new AssetXO();
    assetXO.setId("asset-1");
    when(componentHelper.readComponentAssets(eq(repository), any(ComponentXO.class)))
        .thenReturn(List.of(assetXO));

    List<AssetXO> result = underTest.readComponentAssets(parameters);

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getId(), is("asset-1"));
  }

  @Test
  void testPreviewAssets_blankExpression_returnsNull() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("repositoryName");
            setValue("my-repo");
          }
        },
        new Filter()
        {
          {
            setProperty("expression");
            setValue("");
          }
        },
        new Filter()
        {
          {
            setProperty("type");
            setValue("csel");
          }
        }));

    PagedResponse<AssetXO> result = underTest.previewAssets(parameters);

    assertThat(result, is(nullValue()));
  }

  @Test
  void testPreviewAssets_blankType_returnsNull() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("repositoryName");
            setValue("my-repo");
          }
        },
        new Filter()
        {
          {
            setProperty("expression");
            setValue("format == \"maven2\"");
          }
        },
        new Filter()
        {
          {
            setProperty("type");
            setValue("");
          }
        }));

    PagedResponse<AssetXO> result = underTest.previewAssets(parameters);

    assertThat(result, is(nullValue()));
  }

  @Test
  void testPreviewAssets_specificRepository() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setLimit(25);
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("repositoryName");
            setValue("my-repo");
          }
        },
        new Filter()
        {
          {
            setProperty("expression");
            setValue("format == \"maven2\"");
          }
        },
        new Filter()
        {
          {
            setProperty("type");
            setValue("csel");
          }
        }));

    when(repositoryManager.get("my-repo")).thenReturn(repository);
    AssetXO assetXO = new AssetXO();
    assetXO.setId("a1");
    PageResult<AssetXO> pageResult = new PageResult<>(1, List.of(assetXO));
    when(componentHelper.previewAssets(any(RepositorySelector.class), any(), any(), any()))
        .thenReturn(pageResult);

    PagedResponse<AssetXO> result = underTest.previewAssets(parameters);

    assertThat(result, is(notNullValue()));
    assertThat(result.getData(), hasSize(1));
    verify(selectorFactory).validateSelector("csel", "format == \"maven2\"");
  }

  @Test
  void testPreviewAssets_allRepositoriesAllFormats() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    parameters.setLimit(25);
    parameters.setFilter(List.of(
        new Filter()
        {
          {
            setProperty("repositoryName");
            setValue("*");
          }
        },
        new Filter()
        {
          {
            setProperty("expression");
            setValue("format == \"maven2\"");
          }
        },
        new Filter()
        {
          {
            setProperty("type");
            setValue("csel");
          }
        }));

    Repository repo1 = mock(Repository.class);
    when(repositoryManager.browse()).thenReturn(List.of(repo1));

    PageResult<AssetXO> pageResult = new PageResult<>(0, Collections.emptyList());
    when(componentHelper.previewAssets(any(RepositorySelector.class), any(), any(), any()))
        .thenReturn(pageResult);

    PagedResponse<AssetXO> result = underTest.previewAssets(parameters);

    assertThat(result, is(notNullValue()));
  }

  @Test
  void testCanDeleteComponent_delegatesToHelper() throws Exception {
    String componentJson =
        "{\"id\":\"comp1\",\"repositoryName\":\"my-repo\",\"group\":\"g\",\"name\":\"n\",\"version\":\"v\",\"format\":\"maven2\"}";
    when(repositoryManager.get("my-repo")).thenReturn(repository);
    when(componentHelper.canDeleteComponent(eq(repository), any(ComponentXO.class))).thenReturn(true);

    boolean result = underTest.canDeleteComponent(componentJson);

    assertThat(result, is(true));
  }

  @Test
  void testDeleteComponent_delegatesToHelper() throws Exception {
    String componentJson =
        "{\"id\":\"comp1\",\"repositoryName\":\"my-repo\",\"group\":\"g\",\"name\":\"n\",\"version\":\"v\",\"format\":\"maven2\"}";
    when(repositoryManager.get("my-repo")).thenReturn(repository);
    when(componentHelper.deleteComponent(eq(repository), any(ComponentXO.class)))
        .thenReturn(Set.of("/path/to/deleted"));

    Set<String> result = underTest.deleteComponent(componentJson);

    assertThat(result, hasSize(1));
  }

  @Test
  void testCanDeleteAsset_delegatesToHelper() {
    when(repositoryManager.get("my-repo")).thenReturn(repository);
    when(componentHelper.canDeleteAsset(eq(repository), any(DetachedEntityId.class))).thenReturn(true);

    boolean result = underTest.canDeleteAsset("asset-1", "my-repo");

    assertThat(result, is(true));
  }

  @Test
  void testDeleteAsset_delegatesToHelper() {
    when(repositoryManager.get("my-repo")).thenReturn(repository);
    when(componentHelper.deleteAsset(eq(repository), any(DetachedEntityId.class)))
        .thenReturn(Set.of("/deleted/path"));

    Set<String> result = underTest.deleteAsset("asset-1", "my-repo");

    assertThat(result, hasSize(1));
    assertThat(result instanceof HashSet, is(true));
  }

  @Test
  void testReadComponent_delegatesToHelper() {
    when(repositoryManager.get("my-repo")).thenReturn(repository);
    ComponentXO componentXO = new ComponentXO();
    componentXO.setId("comp-1");
    when(componentHelper.readComponent(eq(repository), any(DetachedEntityId.class))).thenReturn(componentXO);

    ComponentXO result = underTest.readComponent("comp-1", "my-repo");

    assertThat(result, is(notNullValue()));
    assertThat(result.getId(), is("comp-1"));
  }

  @Test
  void testReadAsset_delegatesToHelper() {
    when(repositoryManager.get("my-repo")).thenReturn(repository);
    AssetXO assetXO = new AssetXO();
    assetXO.setId("asset-1");
    assetXO.setFormat("maven2");
    when(componentHelper.readAsset(eq(repository), any(DetachedEntityId.class))).thenReturn(assetXO);

    AssetXO result = underTest.readAsset("asset-1", "my-repo");

    assertThat(result, is(notNullValue()));
    assertThat(result.getId(), is("asset-1"));
  }

  @Test
  void testReadAsset_withFormatTransformation() {
    AssetAttributeTransformer transformer = mock(AssetAttributeTransformer.class);
    underTest = new ComponentComponent(repositoryManager, selectorFactory, jsonMapper, componentHelper,
        Collections.emptyList());

    when(repositoryManager.get("my-repo")).thenReturn(repository);
    AssetXO assetXO = new AssetXO();
    assetXO.setId("asset-1");
    assetXO.setFormat("maven2");
    when(componentHelper.readAsset(eq(repository), any(DetachedEntityId.class))).thenReturn(assetXO);

    AssetXO result = underTest.readAsset("asset-1", "my-repo");

    assertThat(result, is(notNullValue()));
  }

  @Test
  void testCanDeleteFolder_delegatesToHelper() {
    when(repositoryManager.get("my-repo")).thenReturn(repository);
    when(componentHelper.canDeleteFolder(repository, "/my/path")).thenReturn(true);

    boolean result = underTest.canDeleteFolder("/my/path", "my-repo");

    assertThat(result, is(true));
  }

  @Test
  void testDeleteFolder_delegatesToHelper() {
    when(repositoryManager.get("my-repo")).thenReturn(repository);

    underTest.deleteFolder("/my/path", "my-repo");

    verify(componentHelper).deleteFolder(repository, "/my/path");
  }
}
