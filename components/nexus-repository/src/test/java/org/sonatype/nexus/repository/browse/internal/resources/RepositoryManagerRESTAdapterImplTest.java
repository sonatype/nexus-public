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

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.browse.api.AssetXO;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.Asset;

import com.orientechnologies.orient.core.id.ORID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class RepositoryManagerRESTAdapterImplTest
    extends TestSupport
{
  @Mock
  RepositoryManager repositoryManager;

  @Mock
  Repository repository;

  final String REPOSITORY_NAME = "test";

  RepositoryManagerRESTAdapterImpl underTest;

  @Before
  public void setUp() throws Exception {
    when(repositoryManager.get(REPOSITORY_NAME)).thenReturn(repository);

    underTest = new RepositoryManagerRESTAdapterImpl(repositoryManager);
  }

  @Test
  public void getRepository() throws Exception {
    assertThat(underTest.getRepository(REPOSITORY_NAME), is(repository));
  }

  @Test(expected = NotFoundException.class)
  public void getRepository_notFound() {
    underTest.getRepository("notFound");
  }

  @Test
  public void getRepository_null() {
    try {
      underTest.getRepository(null);
      fail(); //should have thrown exception
    }
    catch (WebApplicationException e) {
      assertThat(e.getResponse().getStatus(), is(422));
    }
  }
}
