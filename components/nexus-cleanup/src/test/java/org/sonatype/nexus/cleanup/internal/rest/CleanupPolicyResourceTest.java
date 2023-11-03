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
package org.sonatype.nexus.cleanup.internal.rest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.inject.Provider;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.cleanup.config.CleanupPolicyConfiguration;
import org.sonatype.nexus.cleanup.internal.preview.CSVCleanupPreviewContentWriter;
import org.sonatype.nexus.cleanup.preview.CleanupPreviewHelper;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cleanup.CleanupFeatureCheck;
import org.sonatype.nexus.repository.content.kv.global.GlobalKeyValueStore;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.rest.ValidationErrorXO;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CleanupPolicyResourceTest
    extends TestSupport
{
  @Mock
  private CleanupPolicyStorage cleanupPolicyStorage;

  @Mock
  private CleanupFeatureCheck cleanupFeatureCheck;

  @Mock
  private List<Format> formats;

  @Mock
  private Map<String, CleanupPolicyConfiguration> cleanupFormatConfigurationMap;

  @Mock
  private Provider<CleanupPreviewHelper> cleanupPreviewHelper;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private EventManager eventManager;

  @Mock
  private GlobalKeyValueStore globalKeyValueStore;

  @Mock
  private CSVCleanupPreviewContentWriter csvCleanupPreviewContentWriter;

  private CleanupPolicyResource underTest;

  private final String repositoryName = "test-repo";

  @Before
  public void setUp() throws Exception {
    when(cleanupFormatConfigurationMap.get("default")).thenReturn(mock(CleanupPolicyConfiguration.class));
    Repository repository = mock(Repository.class);
    when(repositoryManager.get(repositoryName)).thenReturn(repository);
    when(repository.getName()).thenReturn(repositoryName);
    when(cleanupFeatureCheck.isPostgres()).thenReturn(true);
  }

  @Test
  public void testPreviewContentCsv() {
    underTest =
        new CleanupPolicyResource(cleanupPolicyStorage, cleanupFeatureCheck, formats, cleanupFormatConfigurationMap,
            cleanupPreviewHelper,
            repositoryManager, eventManager, globalKeyValueStore, true, csvCleanupPreviewContentWriter);

    Response response = underTest.previewContentCsv(null, repositoryName, null, null, null, null, null, null);

    assertThat(response.getStatus(), is(200));
    String contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    String expectedPrefix = "attachment; filename=CleanupPreview-" + repositoryName;
    assertThat(contentDisposition, startsWith(expectedPrefix));
    assertThat(contentDisposition, endsWith(".csv"));

    response = underTest.previewContentCsv("policy-name", repositoryName, null, null, null, null, null, null);

    assertThat(response.getStatus(), is(200));
    contentDisposition = response.getHeaderString("Content-Disposition");
    assertThat(contentDisposition, not(isEmptyOrNullString()));
    expectedPrefix = "attachment; filename=policy-name-" + repositoryName;
    assertThat(contentDisposition, startsWith(expectedPrefix));
    assertThat(contentDisposition, endsWith(".csv"));
  }

  @Test
  public void testExceptionIsThrownIfExclusionCriteriaIsSetInNonPro() {
    when(cleanupFeatureCheck.isRetainSupported("maven2")).thenReturn(false);
    when(cleanupFeatureCheck.isProVersion()).thenReturn(false);

    Format format1 = mock(Format.class);
    when(format1.getValue()).thenReturn("maven2");

    underTest =
        new CleanupPolicyResource(cleanupPolicyStorage, cleanupFeatureCheck, Collections.singletonList(format1),
            cleanupFormatConfigurationMap,
            cleanupPreviewHelper,
            repositoryManager, eventManager, globalKeyValueStore, true, csvCleanupPreviewContentWriter);

    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName("test-cleanup-policy");
    xo.setFormat("maven2");
    xo.setNotes("test");
    xo.setCriteriaLastBlobUpdated(10L);
    xo.setCriteriaLastDownloaded(20L);
    xo.setRetain(10);
    xo.setSortBy("date");

    BadRequestException expected = assertThrows(BadRequestException.class, () -> underTest.add(xo));
    ValidationErrorXO error = (ValidationErrorXO) expected.getResponse().getEntity();

    assertNotNull(expected);
    assertNotNull(error);
    assertEquals("Exclusion criteria is not supported.", error.getMessage());
  }

  @Test
  public void testExceptionIsThrownIfExclusionCriteriaIsSetInNonPostgres() {
    when(cleanupFeatureCheck.isRetainSupported("maven2")).thenReturn(false);
    when(cleanupFeatureCheck.isProVersion()).thenReturn(true);
    when(cleanupFeatureCheck.isPostgres()).thenReturn(false);

    Format format1 = mock(Format.class);
    when(format1.getValue()).thenReturn("maven2");

    underTest =
        new CleanupPolicyResource(cleanupPolicyStorage, cleanupFeatureCheck, Collections.singletonList(format1),
            cleanupFormatConfigurationMap,
            cleanupPreviewHelper,
            repositoryManager, eventManager, globalKeyValueStore, true, csvCleanupPreviewContentWriter);

    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName("test-cleanup-policy");
    xo.setFormat("maven2");
    xo.setNotes("test");
    xo.setRetain(10);
    xo.setSortBy("date");

    BadRequestException expected = assertThrows(BadRequestException.class, () -> underTest.add(xo));
    ValidationErrorXO error = (ValidationErrorXO) expected.getResponse().getEntity();

    assertNotNull(expected);
    assertNotNull(error);
    assertEquals("The current data source is not supported by the exclusion criteria.", error.getMessage());
  }

  @Test
  public void testNotFoundResponseForOrientOrH2() {
    when(cleanupFeatureCheck.isPostgres()).thenReturn(false);
    underTest =
        new CleanupPolicyResource(cleanupPolicyStorage, cleanupFeatureCheck, formats, cleanupFormatConfigurationMap,
            cleanupPreviewHelper,
            repositoryManager, eventManager, globalKeyValueStore, true, csvCleanupPreviewContentWriter);

    Response response = underTest.previewContentCsv(null, repositoryName, null, null, null, null, null, null);

    assertThat(response.getStatus(), is(404));
  }
}
