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
package org.sonatype.nexus.repository.pypi.internal;

import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.view.Content.CONTENT_LAST_MODIFIED;

public class PyPiGroupFacetTest
    extends TestSupport
{
  @Mock
  RepositoryManager repositoryManager;

  @Mock
  ConstraintViolationFactory constraintViolationFactory;

  @Mock
  Repository repository;

  @Mock
  Response response;

  @Mock
  private Content originalContent;

  @Mock
  private Content content;

  private Map<Repository, Response> responses;

  private PyPiGroupFacet underTest;

  @Before
  public void setUp() throws Exception {
    AttributesMap attributesMap = new AttributesMap();
    attributesMap.set(CONTENT_LAST_MODIFIED, DateTime.now());

    when(originalContent.getAttributes()).thenReturn(attributesMap);

    underTest = new PyPiGroupFacet(repositoryManager, constraintViolationFactory, new GroupType()) {
      @Override
      public boolean isStale(@Nullable final Content content) {
        return false;
      }
    };

    responses = ImmutableMap.of(repository, response);
  }

  @Test
  public void whenNoDataModifiedToCompareShouldBeStale() {
    assertTrue(underTest.isStale(null, null, null));
  }

  @Test
  public void whenDateModifiedPresentAndNoResponsesShouldNotBeStale() {
    assertFalse(underTest.isStale(null, originalContent, ImmutableMap.of()));
  }

  @Test
  public void whenResponseHasNoContentShouldNotBeStale() {
    when(response.getStatus()).thenReturn(Status.success(200));
    when(response.getPayload()).thenReturn(null);

    assertFalse(underTest.isStale(null, originalContent, responses));
  }

  @Test
  public void whenResponseIsNewerThanLastModifiedShouldBeStale() {
    when(response.getStatus()).thenReturn(Status.success(200));
    when(response.getPayload()).thenReturn(content);
    AttributesMap attributes = new AttributesMap();
    attributes.set(CONTENT_LAST_MODIFIED,
        originalContent.getAttributes().get(CONTENT_LAST_MODIFIED, DateTime.class).plusMinutes(1));

    when(content.getAttributes()).thenReturn(attributes);

    assertTrue(underTest.isStale(null, originalContent, responses));
  }

  @Test
  public void whenCacheTokenOsInvalidShouldBeStale() {
    underTest = new PyPiGroupFacet(repositoryManager, constraintViolationFactory, new GroupType()) {
      @Override
      protected boolean isStale(@Nullable final Content content) {
        return true;
      }
    };

    assertTrue(underTest.isStale(null, originalContent, ImmutableMap.of()));
  }
}
