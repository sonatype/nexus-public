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
package org.sonatype.nexus.repository.httpbridge.internal;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.httpbridge.HttpResponseSender;
import org.sonatype.nexus.repository.httpbridge.LegacyViewConfiguration;
import org.sonatype.nexus.repository.httpbridge.LegacyViewContributor;
import org.sonatype.nexus.repository.httpbridge.internal.describe.DescriptionHelper;
import org.sonatype.nexus.repository.httpbridge.internal.describe.DescriptionRenderer;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.ViewFacet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;

public class LegacyViewServletTest
    extends TestSupport
{
  private static final String LEGACY_ENABLED = "org.sonatype.nexus.repository.httpbridge.internal.HttpBridgeModule.legacy";

  private static final String REPOSITORY_NAME = "test-repo";

  private static final String NOT_FOUND_MESSAGE = "Repository not found";

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private HttpResponseSenderSelector httpResponseSenderSelector;

  @Mock
  private DescriptionHelper descriptionHelper;

  @Mock
  private DescriptionRenderer descriptionRenderer;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private Repository repository;

  @Mock
  private Configuration configuration;

  @Mock
  private Enumeration<String> names;

  @Mock
  private ViewFacet viewFacet;

  @Mock
  private HttpResponseSender sender;

  @Mock
  private LegacyViewContributor rawContributor;

  @Mock
  private LegacyViewConfiguration rawConfiguration;

  private List<LegacyViewContributor> legacyViewContributors;

  private LegacyViewServlet viewServlet;

  @Before
  public void setup() throws Exception {
    System.setProperty(LEGACY_ENABLED, "true");

    legacyViewContributors = new ArrayList<>();
    legacyViewContributors.add(rawContributor);

    viewServlet = buildServlet();

    mockNames();
    mockRequest();
    mockRepository();
    mockContributor();
    mockResponseSender();
  }

  @Test
  public void sendRequestWhenLegacyEnabled() throws Exception {
    viewServlet.doService(request, response);

    verify(sender).send(isNotNull(Request.class), any(Response.class), eq(response));
  }

  @Test
  public void sendNotFoundWhenLegacyUrlAndRepositoryNotFound() throws Exception {
    when(repository.getFormat()).thenReturn(new Format("raw") { });
    when(repositoryManager.get(REPOSITORY_NAME)).thenReturn(null);
    when(request.getPathInfo()).thenReturn("/test-repo/content.txt");
    when(request.getRequestURI()).thenReturn("/content/sites/test-repo/content.txt");

    viewServlet.doService(request, response);
    ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
    verify(sender).send(eq(null), responseCaptor.capture(), eq(response));

    assertThat(responseCaptor.getValue().getStatus().getCode(), is(equalTo(404)));
    assertThat(responseCaptor.getValue().getStatus().getMessage(), is(equalTo(NOT_FOUND_MESSAGE)));
  }

  @Test
  public void sendNotFoundWhenLegacyUrlAndFormatDoesNotMatch() throws Exception {
    when(repository.getFormat()).thenReturn(new Format("yum") { });
    when(request.getPathInfo()).thenReturn("/test-repo/content.txt");
    when(request.getRequestURI()).thenReturn("/content/sites/test-repo/content.txt");

    viewServlet.doService(request, response);
    ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
    verify(sender).send(eq(null), responseCaptor.capture(), eq(response));

    assertThat(responseCaptor.getValue().getStatus().getCode(), is(equalTo(NOT_FOUND)));
    assertThat(responseCaptor.getValue().getStatus().getMessage(), is(equalTo(NOT_FOUND_MESSAGE)));
  }

  private LegacyViewServlet buildServlet() {
    return new LegacyViewServlet(repositoryManager, httpResponseSenderSelector,
        descriptionHelper, descriptionRenderer, legacyViewContributors, true);
  }

  private void mockNames() {
    when(names.hasMoreElements()).thenAnswer(new Answer<Boolean>()
    {
      int count = 0;

      @Override
      public Boolean answer(final InvocationOnMock invocationOnMock) {
        return count++ == 0;
      }
    });
    when(names.nextElement()).thenReturn("header-name");
  }

  private void mockRequest() {
    when(request.getPathInfo()).thenReturn("/test-repo/remaining-path");
    when(request.getRequestURI()).thenReturn("/test-repo/remaining-path");
    when(request.getHeaderNames()).thenReturn(names);
    when(request.getHeaders("header-name")).thenReturn(names);
    when(request.getParameterNames()).thenReturn(names);
    when(request.getAttributeNames()).thenReturn(names);
    when(request.getMethod()).thenReturn("GET");
  }

  private void mockRepository() {
    when(repositoryManager.get(REPOSITORY_NAME)).thenReturn(repository);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(repository.facet(ViewFacet.class)).thenReturn(viewFacet);
    when(repository.getFormat()).thenReturn(new Format("maven") { });
    when(configuration.isOnline()).thenReturn(true);
  }

  private void mockContributor() {
    when(rawContributor.contribute()).thenReturn(rawConfiguration);
    when(rawConfiguration.getRequestPattern()).thenReturn(Pattern.compile("/content/sites/.*"));
    when(rawConfiguration.getFormat()).thenReturn("raw");
  }

  private void mockResponseSender() {
    when(httpResponseSenderSelector.sender(any(Repository.class))).thenReturn(sender);
    when(httpResponseSenderSelector.defaultSender()).thenReturn(sender);
  }
}
