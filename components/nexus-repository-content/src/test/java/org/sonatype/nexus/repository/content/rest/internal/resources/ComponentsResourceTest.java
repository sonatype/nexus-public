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
package org.sonatype.nexus.repository.content.rest.internal.resources;

import javax.servlet.http.HttpServletRequest;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.maintenance.MaintenanceService;
import org.sonatype.nexus.repository.rest.api.ComponentXOFactory;
import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.upload.UploadConfiguration;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.repository.upload.UploadResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComponentsResourceTest
    extends TestSupport
{
  private static final String testRepoName = "test-repo";

  private ComponentsResource underTest;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  Format format;

  @Mock
  private Repository testRepo;

  @Mock
  private RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  @Mock
  private MaintenanceService maintenanceService;

  @Mock
  private UploadManager uploadManager;

  @Mock
  private UploadConfiguration uploadConfiguration;

  @Mock
  private ComponentXOFactory componentXOFactory;

  @Before
  public void setUp() throws Exception {
    configureMockedRepository(testRepo, testRepoName, "http://localhost:8081/repository/test-repo");

    when(uploadConfiguration.isEnabled()).thenReturn(true);

    underTest = new ComponentsResource(repositoryManagerRESTAdapter, maintenanceService, uploadManager,
        uploadConfiguration, componentXOFactory);
  }

  @SuppressWarnings("java:S2699")
  @Test
  public void uploadComponent() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);

    UploadResponse uploadResponse = new UploadResponse(emptyList());
    when(uploadManager.handle(testRepo, request)).thenReturn(uploadResponse);
    underTest.uploadComponent(testRepoName, request);
  }

  protected void configureMockedRepository(final Repository repository, final String name, final String url)
  {
    when(repositoryManagerRESTAdapter.getRepository(name)).thenReturn(repository);
    when(repository.getUrl()).thenReturn(url);
    when(repository.getName()).thenReturn(name);
    when(repository.getFormat()).thenReturn(format);
  }
}
