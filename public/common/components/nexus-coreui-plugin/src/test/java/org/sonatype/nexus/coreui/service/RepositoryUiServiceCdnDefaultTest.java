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
package org.sonatype.nexus.coreui.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;

import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.app.GlobalComponentLookupHelper;
import org.sonatype.nexus.coreui.RepositoryXO;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.config.UniqueRepositoryNameValidator;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.manager.internal.FailedRepositoryTracker;
import org.sonatype.nexus.repository.manager.internal.HttpAuthenticationSecretEncoder;
import org.sonatype.nexus.repository.rest.api.RepositoryMetricsService;
import org.sonatype.nexus.repository.rest.api.cdn.CdnDefaultsService;
import org.sonatype.nexus.repository.security.GroupMemberAuthorizationChecker;
import org.sonatype.nexus.repository.security.RepositoryPermissionChecker;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.security.SecurityHelper;

import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.RegisterValidator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * NEXUS-54396 — RepositoryUiService.create is the ExtDirect (classic UI) repository-create path.
 * It must apply the Global Distribution capability default when the request omits the value,
 * mirroring AbstractRepositoriesApiResource. The default-apply is gated by the presence of the
 * CdnDefaultsService bean (Enterprise edition).
 */
@ExtendWith({MockitoExtension.class, AuthenticationExtension.class, ValidationExtension.class})
@WithUser
class RepositoryUiServiceCdnDefaultTest
{
  @RegisterValidator
  @Mock
  UniqueRepositoryNameValidator nameValidator;

  @Mock
  private RepositoryCacheInvalidationService repositoryCacheInvalidationService;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private ConfigurationStore configurationStore;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private TaskScheduler taskScheduler;

  @Mock
  private GlobalComponentLookupHelper typeLookup;

  @Mock
  private RepositoryPermissionChecker repositoryPermissionChecker;

  @Mock
  private FailedRepositoryTracker failedRepositoryTracker;

  @Mock
  private HttpAuthenticationSecretEncoder httpAuthenticationSecretEncoder;

  @Mock
  private GroupMemberAuthorizationChecker groupMemberAuthorizationChecker;

  @Mock
  private RepositoryMetricsService repositoryMetricsService;

  @Mock
  private Recipe rawHostedRecipe;

  @Mock
  private Format rawFormat;

  @Mock
  private Type hostedType;

  @Mock
  private CdnDefaultsService cdnDefaultsService;

  private RepositoryUiService withService;

  private RepositoryUiService withoutService;

  private MockedStatic<QualifierUtil> mockedQualifierUtil;

  @BeforeEach
  void setUp() {
    lenient().when(nameValidator.isValid(any(), any())).thenReturn(true);

    BaseUrlHolder.set("http://nexus-url", "");

    when(rawHostedRecipe.getFormat()).thenReturn(rawFormat);
    lenient().when(rawHostedRecipe.getType()).thenReturn(hostedType);
    lenient().when(rawFormat.getValue()).thenReturn("raw");
    lenient().when(hostedType.getValue()).thenReturn("hosted");

    // Mock QualifierUtil to return a map with "raw-hosted" key
    mockedQualifierUtil = mockStatic(QualifierUtil.class);
    Map<String, Recipe> recipeMap = new HashMap<>();
    recipeMap.put("raw-hosted", rawHostedRecipe);
    mockedQualifierUtil.when(() -> QualifierUtil.buildQualifierBeanMap(any())).thenReturn(recipeMap);

    // Recipe bean map key = "<format>-<type>" via QualifierUtil.
    List<Recipe> recipes = List.of(rawHostedRecipe);

    withService = build(recipes);
    withService.setCdnDefaultsService(cdnDefaultsService);

    withoutService = build(recipes);
    // withoutService leaves cdnDefaultsService null.
  }

  @AfterEach
  void tearDown() {
    BaseUrlHolder.unset();
    if (mockedQualifierUtil != null) {
      mockedQualifierUtil.close();
    }
  }

  @Test
  void create_invokesApplyDefaultIfAbsentWhenServicePresent() throws Exception {
    RepositoryXO xo = buildValidHostedXO();

    // Capture the Configuration passed to repositoryManager.create so we can assert on it if
    // needed. Here we just verify the service was invoked with that exact instance.
    Configuration created = mock(Configuration.class);
    when(repositoryManager.newConfiguration()).thenReturn(created);

    // Stub Repository methods used by asRepository(Repository) and buildStatus(Repository)
    Repository createdRepo = mock(Repository.class);
    when(createdRepo.getName()).thenReturn("my-raw-hosted");
    when(createdRepo.getType()).thenReturn(hostedType);
    when(createdRepo.getFormat()).thenReturn(rawFormat);
    when(createdRepo.getConfiguration()).thenReturn(created);
    when(created.isOnline()).thenReturn(true);
    when(created.getRecipeName()).thenReturn("raw-hosted");
    when(created.getRoutingRuleId()).thenReturn(null);
    when(created.copy()).thenReturn(created);
    when(created.getAttributes()).thenReturn(new HashMap<>());
    when(repositoryManager.create(created)).thenReturn(createdRepo);

    withService.create(xo);

    verify(cdnDefaultsService).applyDefaultIfAbsent(created);
    verify(repositoryManager).create(created);
  }

  @Test
  void create_isNoOpForCdnWhenServiceAbsent() throws Exception {
    RepositoryXO xo = buildValidHostedXO();

    Configuration created = mock(Configuration.class);
    when(repositoryManager.newConfiguration()).thenReturn(created);

    // Stub Repository methods used by asRepository(Repository) and buildStatus(Repository)
    Repository createdRepo = mock(Repository.class);
    when(createdRepo.getName()).thenReturn("my-raw-hosted");
    when(createdRepo.getType()).thenReturn(hostedType);
    when(createdRepo.getFormat()).thenReturn(rawFormat);
    when(createdRepo.getConfiguration()).thenReturn(created);
    when(created.isOnline()).thenReturn(true);
    when(created.getRecipeName()).thenReturn("raw-hosted");
    when(created.getRoutingRuleId()).thenReturn(null);
    when(created.copy()).thenReturn(created);
    when(created.getAttributes()).thenReturn(new HashMap<>());
    when(repositoryManager.create(created)).thenReturn(createdRepo);

    withoutService.create(xo);

    verifyNoInteractions(cdnDefaultsService);
    verify(repositoryManager).create(created);
  }

  private RepositoryXO buildValidHostedXO() {
    RepositoryXO xo = new RepositoryXO();
    xo.setName("my-raw-hosted");
    xo.setRecipe("raw-hosted");
    xo.setOnline(true);
    Map<String, Object> storage = new HashMap<>();
    storage.put("blobStoreName", "default");
    storage.put("strictContentTypeValidation", true);
    Map<String, Map<String, Object>> attrs = new HashMap<>();
    attrs.put("storage", storage);
    xo.setAttributes(attrs);
    return xo;
  }

  private RepositoryUiService build(final List<Recipe> recipes) {
    return new RepositoryUiService(
        repositoryCacheInvalidationService,
        repositoryManager,
        repositoryMetricsService,
        configurationStore,
        securityHelper,
        recipes,
        taskScheduler,
        typeLookup,
        Collections.emptyList(),
        repositoryPermissionChecker,
        failedRepositoryTracker,
        httpAuthenticationSecretEncoder,
        groupMemberAuthorizationChecker);
  }
}
