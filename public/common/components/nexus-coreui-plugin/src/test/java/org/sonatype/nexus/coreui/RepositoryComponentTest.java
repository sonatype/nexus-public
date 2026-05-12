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
import java.util.List;
import java.util.Map;

import javax.validation.Validator;

import org.sonatype.nexus.bootstrap.validation.ValidationConfiguration;
import org.sonatype.nexus.coreui.search.BrowseableFormatXO;
import org.sonatype.nexus.coreui.service.RepositoryUiService;
import org.sonatype.nexus.extdirect.model.StoreLoadParameters;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Recipe;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.ValidationExecutor;

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
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RepositoryComponent}.
 */
@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class RepositoryComponentTest
{
  @ValidationExecutor
  private final Validator validator =
      new ValidationConfiguration().validatorFactory(new ConstraintValidatorFactoryImpl()).getValidator();

  @Mock
  private RepositoryUiService repositoryUiService;

  private RepositoryComponent underTest;

  @BeforeEach
  void setUp() {
    underTest = new RepositoryComponent(repositoryUiService);
  }

  @Test
  void testRead_delegatesToService() {
    RepositoryXO repoXO = new RepositoryXO();
    repoXO.setName("my-repo");
    when(repositoryUiService.read()).thenReturn(List.of(repoXO));

    List<RepositoryXO> result = underTest.read();

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getName(), is("my-repo"));
    verify(repositoryUiService).read();
  }

  @Test
  void testRead_emptyList() {
    when(repositoryUiService.read()).thenReturn(Collections.emptyList());

    List<RepositoryXO> result = underTest.read();

    assertThat(result, is(empty()));
  }

  @Test
  void testReadRecipes_delegatesToService() {
    ReferenceXO ref = new ReferenceXO("maven2-hosted", "maven2 (hosted)");
    when(repositoryUiService.readRecipes()).thenReturn(List.of(ref));

    List<ReferenceXO> result = underTest.readRecipes();

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getId(), is("maven2-hosted"));
    assertThat(result.get(0).getName(), is("maven2 (hosted)"));
    verify(repositoryUiService).readRecipes();
  }

  @Test
  void testReadFormats_delegatesToService() {
    Format format = mock(Format.class);
    when(repositoryUiService.readFormats()).thenReturn(List.of(format));

    List<Format> result = underTest.readFormats();

    assertThat(result, hasSize(1));
    verify(repositoryUiService).readFormats();
  }

  @Test
  void testGetBrowseableFormats_delegatesToService() {
    BrowseableFormatXO formatXO = new BrowseableFormatXO();
    when(repositoryUiService.getBrowseableFormats()).thenReturn(List.of(formatXO));

    List<BrowseableFormatXO> result = underTest.getBrowseableFormats();

    assertThat(result, hasSize(1));
    verify(repositoryUiService).getBrowseableFormats();
  }

  @Test
  void testReadReferences_delegatesToService() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    RepositoryReferenceXO refXO = new RepositoryReferenceXO(
        "my-repo", "my-repo", "hosted", "maven2", null, "http://localhost/repo", "default", null);
    when(repositoryUiService.readReferences(parameters)).thenReturn(List.of(refXO));

    List<RepositoryReferenceXO> result = underTest.readReferences(parameters);

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getName(), is("my-repo"));
    verify(repositoryUiService).readReferences(parameters);
  }

  @Test
  void testReadReferences_nullParameters() {
    when(repositoryUiService.readReferences(null)).thenReturn(Collections.emptyList());

    List<RepositoryReferenceXO> result = underTest.readReferences(null);

    assertThat(result, is(empty()));
    verify(repositoryUiService).readReferences(null);
  }

  @Test
  void testReadReferencesAddingEntryForAll_delegatesToService() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    when(repositoryUiService.readReferencesAddingEntryForAll(parameters)).thenReturn(Collections.emptyList());

    List<RepositoryReferenceXO> result = underTest.readReferencesAddingEntryForAll(parameters);

    assertThat(result, is(notNullValue()));
    verify(repositoryUiService).readReferencesAddingEntryForAll(parameters);
  }

  @Test
  void testReadReferencesAddingEntriesForAllFormats_delegatesToService() {
    StoreLoadParameters parameters = new StoreLoadParameters();
    when(repositoryUiService.readReferencesAddingEntriesForAllFormats(parameters)).thenReturn(Collections.emptyList());

    List<RepositoryReferenceXO> result = underTest.readReferencesAddingEntriesForAllFormats(parameters);

    assertThat(result, is(notNullValue()));
    verify(repositoryUiService).readReferencesAddingEntriesForAllFormats(parameters);
  }

  // Note: create() cannot be tested directly because @UniqueRepositoryName validator
  // requires Spring-managed UniqueRepositoryNameValidator which cannot be instantiated
  // in unit tests. The create method is a simple delegation to repositoryUiService.create().

  @Test
  void testUpdate_delegatesToService() throws Exception {
    RepositoryXO repositoryXO = createValidRepositoryXO("existing-repo");
    RepositoryXO updatedXO = createValidRepositoryXO("existing-repo");
    when(repositoryUiService.update(repositoryXO)).thenReturn(updatedXO);

    RepositoryXO result = underTest.update(repositoryXO);

    assertThat(result.getName(), is("existing-repo"));
    verify(repositoryUiService).update(repositoryXO);
  }

  @Test
  void testRemove_delegatesToService() throws Exception {
    underTest.remove("my-repo");

    verify(repositoryUiService).remove("my-repo");
  }

  @Test
  void testRebuildIndex_delegatesToService() {
    when(repositoryUiService.rebuildIndex("my-repo")).thenReturn("task-123");

    String result = underTest.rebuildIndex("my-repo");

    assertThat(result, is("task-123"));
    verify(repositoryUiService).rebuildIndex("my-repo");
  }

  @Test
  void testInvalidateCache_delegatesToService() {
    underTest.invalidateCache("my-repo");

    verify(repositoryUiService).invalidateCache("my-repo");
  }

  @Test
  void testReadStatus_delegatesToService() {
    Map<String, String> params = Collections.emptyMap();
    RepositoryStatusXO statusXO = new RepositoryStatusXO();
    statusXO.setRepositoryName("my-repo");
    statusXO.setOnline(true);
    when(repositoryUiService.readStatus(params)).thenReturn(List.of(statusXO));

    List<RepositoryStatusXO> result = underTest.readStatus(params);

    assertThat(result, hasSize(1));
    assertThat(result.get(0).getRepositoryName(), is("my-repo"));
    assertThat(result.get(0).isOnline(), is(true));
    verify(repositoryUiService).readStatus(params);
  }

  @Test
  void testAddRecipe_delegatesToService() {
    Recipe recipe = mock(Recipe.class);

    underTest.addRecipe("maven2-hosted", recipe);

    verify(repositoryUiService).addRecipe("maven2-hosted", recipe);
  }

  @Test
  void testGetRepositoryUiService_returnsInjectedService() {
    assertThat(underTest.getRepositoryUiService(), is(sameInstance(repositoryUiService)));
  }

  private static RepositoryXO createValidRepositoryXO(final String name) {
    RepositoryXO xo = new RepositoryXO();
    xo.setName(name);
    xo.setOnline(true);
    xo.setAttributes(Map.of("storage", Map.<String, Object>of("blobStoreName", "default")));
    return xo;
  }
}
