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
package org.sonatype.nexus.repository.rest.internal.api;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.rest.api.RepositoryItemIDXO;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.repository.http.HttpStatus.UNPROCESSABLE_ENTITY;

public class RepositoryItemIDXOTest
    extends TestSupport
{

  @Test
  public void testAssetXOID() {
    String id = "testId";
    String repositoryName = "maven-releases";

    RepositoryItemIDXO repositoryItemIDXO = new RepositoryItemIDXO(repositoryName, id);

    assertThat(repositoryItemIDXO.getValue().contains(id), is(false));
    assertThat(repositoryItemIDXO.getValue().contains(repositoryName), is(false));

    RepositoryItemIDXO decoded = RepositoryItemIDXO.fromString(repositoryItemIDXO.getValue());

    assertThat(decoded.getId(), is(id));
    assertThat(decoded.getRepositoryId(), is(repositoryName));
    assertThat(decoded.getValue(), is(repositoryItemIDXO.getValue()));
  }

  @Test
  public void unprocessableId() {
    try {
      RepositoryItemIDXO.fromString("test");
      fail("Expected a WebApplicationException.");
    }
    catch (WebApplicationException e) {
      assertThat(e.getResponse().getStatus(), is(UNPROCESSABLE_ENTITY));
    }

  }

  @Test
  public void testIllegalArgumentIsNotFound() {
    try {
      RepositoryItemIDXO.fromString("@");
      fail("Expected a NotFoundException.");
    }
    catch (NotFoundException e) {
      assertThat(e.getResponse().getStatus(), is(NOT_FOUND));
    }
  }
}
