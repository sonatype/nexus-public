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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.httpbridge.HttpResponseSender;
import org.sonatype.nexus.repository.httpbridge.internal.describe.DescriptionHelper;
import org.sonatype.nexus.repository.httpbridge.internal.describe.DescriptionRenderer;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.ViewFacet;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

import static java.util.Collections.emptyMap;
import static java.util.Objects.nonNull;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_FOUND;
import static org.sonatype.nexus.repository.httpbridge.internal.ViewServlet.REPOSITORY_NOT_FOUND_MESSAGE;

/**
 * Tests for describe functionality of {@link RawRepositoryViewServlet}.
 */
public class RawRepositoryViewServletTest
    extends TestSupport
{
  private static final String RAW_FORMAT_NAME = "raw";

  @Mock
  private HttpServletResponse httpServletResponse;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private HttpServletRequest httpServletRequest;

  @Mock
  private DescriptionRenderer descriptionRenderer;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private Repository repository;

  @Mock
  private Configuration configuration;

  @Mock
  private Format format;

  @Mock
  private ViewFacet viewFacet;

  private DefaultHttpResponseSender defaultResponseSender;

  private RawRepositoryViewServlet underTest;

  @Before
  public void setUp() throws Exception {
    when(httpServletRequest.getPathInfo()).thenReturn("/somepath/");
    when(httpServletRequest.getMethod()).thenReturn(GET);
    when(repositoryManager.get(anyString())).thenReturn(repository);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(repository.getFormat()).thenReturn(format);
    when(repository.facet(ViewFacet.class)).thenReturn(viewFacet);
    when(configuration.isOnline()).thenReturn(true);

    defaultResponseSender = spy(new DefaultHttpResponseSender());
    underTest = spy(rawRepositoryViewServlet());
  }

  @Test
  public void when_RawRepository_Expect_DispatchAndSend() throws Exception {
    when(format.getValue()).thenReturn(RAW_FORMAT_NAME);

    underTest.service(httpServletRequest, httpServletResponse);

    verify(underTest).doService(any(), any());
    verify(underTest).isRawRepo(any(Repository.class));
    verify(underTest).dispatchAndSend(any(), any(), any(), any());
  }

  @Test
  public void when_NonRawRepository_Expect_Send_RepositoryNotFound() throws Exception {
    underTest.service(httpServletRequest, httpServletResponse);

    verify(underTest).doService(any(), any());
    verify(underTest).isRawRepo(any(Repository.class));
    verify(defaultResponseSender).send(eq(null), argThat(repositoryNotFound()), eq(httpServletResponse));
    verify(underTest, never()).dispatchAndSend(any(), any(), any(), any());
  }

  private Matcher<Response> repositoryNotFound() {
    return new ArgumentMatcher<Response>()
    {
      @Override
      public boolean matches(final Object argument) {
        Status status = ((Response) argument).getStatus();
        return status.getCode() == NOT_FOUND
            && nonNull(status.getMessage())
            && status.getMessage().equals(REPOSITORY_NOT_FOUND_MESSAGE);
      }
    };
  }

  private RawRepositoryViewServlet rawRepositoryViewServlet() {
    return new RawRepositoryViewServlet(repositoryManager,
        new HttpResponseSenderSelector(emptyMap(), defaultResponseSender),
        mock(DescriptionHelper.class),
        descriptionRenderer
    )
    {
      @Override
      void dispatchAndSend(final Request request,
                           final ViewFacet facet,
                           final HttpResponseSender sender,
                           final HttpServletResponse httpResponse) throws Exception
      {
        // mock it out, as we are not testing this method
      }
    };
  }
}
