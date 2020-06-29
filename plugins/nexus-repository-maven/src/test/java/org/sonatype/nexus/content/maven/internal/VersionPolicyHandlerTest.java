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
package org.sonatype.nexus.content.maven.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.content.maven.MavenContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.internal.VersionPolicyValidator;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.http.HttpMethods.PUT;
import static org.sonatype.nexus.repository.maven.MavenPath.SignatureType.GPG;

public class VersionPolicyHandlerTest
    extends TestSupport
{
  @Mock
  private VersionPolicyValidator versionPolicyValidator;

  @Mock
  private Context context;

  @Mock
  private Request request;

  @Mock
  private MavenPath mavenPath;

  @Mock
  private Repository repository;

  @Mock
  private MavenContentFacet mavenContentFacet;

  @InjectMocks
  private VersionPolicyHandler versionPolicyHandler;

  @Before
  public void setup() {
    when(context.getRequest()).thenReturn(request);
  }

  @Test
  public void httpGetShouldProceedWithValidation() throws Exception {
    when(request.getAction()).thenReturn(GET);

    versionPolicyHandler.handle(context);

    verify(context).proceed();
    verifyZeroInteractions(versionPolicyValidator);
  }

  @Test
  public void httpHeadShouldProceedWithValidation() throws Exception {
    when(request.getAction()).thenReturn(HEAD);

    versionPolicyHandler.handle(context);

    verify(context).proceed();
    verifyZeroInteractions(versionPolicyValidator);
  }

  @Test
  public void shouldBeBadRequestWhenVersionPolicyDisallowsCoordinates() throws Exception {
    setupMocks();

    final Response handle = versionPolicyHandler.handle(context);

    assertThat(handle.getStatus().getCode(), is(BAD_REQUEST.getStatusCode()));

    verify(versionPolicyValidator, never()).validMetadataPath(any(), anyString());
  }

  @Test
  public void shouldBeBadRequestWhenMetadataPathIsInvalid() throws Exception {
    setupMocks();
    when(mavenPath.getCoordinates()).thenReturn(null);

    final Response handle = versionPolicyHandler.handle(context);

    assertThat(handle.getStatus().getCode(), is(BAD_REQUEST.getStatusCode()));

    verify(versionPolicyValidator, never()).validArtifactPath(any(), any());
  }

  @Test
  public void shouldProceedWhenVersionPolicyValidationSucceeds() throws Exception {
    setupMocks();
    when(mavenPath.getCoordinates()).thenReturn(null);
    when(versionPolicyValidator.validMetadataPath(any(), anyString())).thenReturn(true);

    versionPolicyHandler.handle(context);

    verify(context).proceed();
    verify(versionPolicyValidator).validMetadataPath(any(), anyString());
  }

  private void setupMocks() {
    AttributesMap attributesMap = new AttributesMap();
    attributesMap.set(MavenPath.class, mavenPath);
    when(request.getAction()).thenReturn(PUT);
    when(context.getAttributes()).thenReturn(attributesMap);
    when(context.getRepository()).thenReturn(repository);
    when(repository.facet(MavenContentFacet.class)).thenReturn(mavenContentFacet);
    when(mavenPath.main()).thenReturn(mavenPath);
    when(mavenPath.getCoordinates()).thenReturn(new Coordinates(false, "g", "a",
        "1", 1L, 1, "1", "test", ".jar", GPG));
  }
}
