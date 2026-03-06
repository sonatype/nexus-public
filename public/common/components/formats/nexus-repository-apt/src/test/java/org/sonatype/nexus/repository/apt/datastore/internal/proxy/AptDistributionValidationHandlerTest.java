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
package org.sonatype.nexus.repository.apt.datastore.internal.proxy;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link AptDistributionValidationHandler}.
 */
public class AptDistributionValidationHandlerTest
    extends TestSupport
{
  @Mock
  private Context context;

  @Mock
  private Repository repository;

  @Mock
  private Request request;

  @Mock
  private AptContentFacet contentFacet;

  @Mock
  private Response proceedResponse;

  private AptDistributionValidationHandler underTest;

  @Before
  public void setUp() throws Exception {
    underTest = new AptDistributionValidationHandler();

    when(context.getRepository()).thenReturn(repository);
    when(context.getRequest()).thenReturn(request);
    when(context.proceed()).thenReturn(proceedResponse);
    when(repository.facet(AptContentFacet.class)).thenReturn(contentFacet);
  }

  @Test
  public void testHandle_WhenEnforceDistributionFalse_ShouldProceed() throws Exception {
    when(contentFacet.isEnforceDistribution()).thenReturn(false);
    when(request.getPath()).thenReturn("dists/focal/InRelease");

    Response response = underTest.handle(context);

    assertThat(response, is(proceedResponse));
    verify(context).proceed();
  }

  @Test
  public void testHandle_WhenEnforceDistributionTrue_AndDistributionBlank_ShouldProceed() throws Exception {
    when(contentFacet.isEnforceDistribution()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("");
    when(request.getPath()).thenReturn("dists/focal/InRelease");

    Response response = underTest.handle(context);

    assertThat(response, is(proceedResponse));
    verify(context).proceed();
  }

  @Test
  public void testHandle_WhenEnforceDistributionTrue_AndDistributionNull_ShouldProceed() throws Exception {
    when(contentFacet.isEnforceDistribution()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn(null);
    when(request.getPath()).thenReturn("dists/focal/InRelease");

    Response response = underTest.handle(context);

    assertThat(response, is(proceedResponse));
    verify(context).proceed();
  }

  @Test
  public void testHandle_WhenEnforceDistributionTrue_AndRequestMatchesConfigured_ShouldProceed() throws Exception {
    when(contentFacet.isEnforceDistribution()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("jammy");
    when(request.getPath()).thenReturn("dists/jammy/InRelease");

    Response response = underTest.handle(context);

    assertThat(response, is(proceedResponse));
    verify(context).proceed();
  }

  @Test
  public void testHandle_WhenEnforceDistributionTrue_AndRequestMatchesConfigured_WithLeadingSlash_ShouldProceed() throws Exception {
    when(contentFacet.isEnforceDistribution()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("jammy");
    when(request.getPath()).thenReturn("/dists/jammy/main/binary-amd64/Packages");

    Response response = underTest.handle(context);

    assertThat(response, is(proceedResponse));
    verify(context).proceed();
  }

  @Test
  public void testHandle_WhenEnforceDistributionTrue_AndRequestDifferentDistribution_ShouldReturn404() throws Exception {
    when(contentFacet.isEnforceDistribution()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("jammy");
    when(request.getPath()).thenReturn("dists/focal/InRelease");

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(404));
  }

  @Test
  public void testHandle_WhenEnforceDistributionTrue_AndRequestDifferentDistribution_WithLeadingSlash_ShouldReturn404() throws Exception {
    when(contentFacet.isEnforceDistribution()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("jammy");
    when(request.getPath()).thenReturn("/dists/bionic/Release");

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(404));
  }

  @Test
  public void testHandle_WhenEnforceDistributionTrue_AndPathWithoutDistribution_ShouldProceed() throws Exception {
    when(contentFacet.isEnforceDistribution()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("jammy");
    when(request.getPath()).thenReturn("pool/main/h/hello/hello_2.10-2_amd64.deb");

    Response response = underTest.handle(context);

    assertThat(response, is(proceedResponse));
    verify(context).proceed();
  }

  @Test
  public void testHandle_WhenEnforceDistributionTrue_AndPathIsRootDists_ShouldProceed() throws Exception {
    when(contentFacet.isEnforceDistribution()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("jammy");
    when(request.getPath()).thenReturn("dists/");

    Response response = underTest.handle(context);

    assertThat(response, is(proceedResponse));
    verify(context).proceed();
  }

  @Test
  public void testHandle_WhenEnforceDistributionTrue_AndNestedPath_ShouldValidate() throws Exception {
    when(contentFacet.isEnforceDistribution()).thenReturn(true);
    when(contentFacet.getDistribution()).thenReturn("jammy");
    when(request.getPath()).thenReturn("dists/jammy/main/binary-amd64/Packages.gz");

    Response response = underTest.handle(context);

    assertThat(response, is(proceedResponse));
    verify(context).proceed();
  }
}
