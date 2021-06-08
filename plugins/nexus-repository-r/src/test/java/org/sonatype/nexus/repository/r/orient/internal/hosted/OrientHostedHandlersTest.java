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
package org.sonatype.nexus.repository.r.orient.internal.hosted;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.r.orient.OrientRHostedFacet;
import org.sonatype.nexus.repository.r.orient.internal.hosted.OrientHostedHandlers;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher.State;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.http.HttpStatus.BAD_REQUEST;
import static org.sonatype.nexus.repository.r.internal.util.PackageValidator.NOT_VALID_EXTENSION_ERROR_MESSAGE;
import static org.sonatype.nexus.repository.r.internal.util.PackageValidator.NOT_VALID_PATH_ERROR_MESSAGE;

public class OrientHostedHandlersTest
    extends TestSupport
{
  public static final String PATH_VALUE = "part1/part2";

  public static final String FILENAME_VALUE = "Filename.tgz";

  public static final String WRONG_PATH_VALUE = "part1only";

  public static final String WRONG_FILENAME_VALUE = "Filename.xxx";

  public static final String FULL_PATH_VALUE = PATH_VALUE + "/" + FILENAME_VALUE;

  public static final String WRONG_PATH_FULL_PATH_VALUE = WRONG_PATH_VALUE + "/" + FILENAME_VALUE;

  public static final String WRONG_EXTENSION_FULL_PATH_VALUE = PATH_VALUE + "/" + WRONG_FILENAME_VALUE;

  @Mock
  Context context;

  @Mock
  Repository repository;

  @Mock
  OrientRHostedFacet rHostedFacet;

  @Mock
  Content content;

  @Mock
  Request request;

  @Mock
  Payload payload;

  AttributesMap attributesMap;

  State state;

  HashMap<String, String> tokens;

  OrientHostedHandlers underTest;

  @Before
  public void setup() throws Exception {
    underTest = new OrientHostedHandlers();
    initialiseTestFixtures();
    initialiseMockBehaviour();
    setupForGetContentTest();
  }

  @Test
  public void okWhenArchiveFound() throws Exception {
    assertStatus(underTest.getArchive, 200);
  }

  @Test
  public void okWhenPackagesFound() throws Exception {
    assertStatus(underTest.getArchive, 200);
  }

  @Test
  public void rebuildsLostPackages() throws Exception {
    when(rHostedFacet.getStoredContent(anyString())).thenReturn(null);
    when(rHostedFacet.buildAndPutPackagesGz(anyString())).thenReturn(content);

    assertStatus(underTest.getPackages, 200);
    verify(rHostedFacet, times(1)).getStoredContent(anyString());
    verify(rHostedFacet, times(1)).buildAndPutPackagesGz(anyString());
  }

  @Test
  public void notFoundWhenPackagesNotFound() throws Exception {
    when(rHostedFacet.getStoredContent(anyString())).thenReturn(null);
    when(rHostedFacet.buildAndPutPackagesGz(anyString())).thenReturn(null);

    assertStatus(underTest.getPackages, 404);
  }

  @Test
  public void notFoundWhenArchiveNotFound() throws Exception {
    when(rHostedFacet.getStoredContent(anyString())).thenReturn(null);
    assertStatus(underTest.getArchive, 404);
  }

  @Test
  public void okWhenPut() throws Exception {
    assertStatus(underTest.putArchive, 200);
  }

  @Test
  public void repositoryUploadWhenPut() throws Exception {
    underTest.putArchive.handle(context);
    verify(rHostedFacet).upload(FULL_PATH_VALUE, payload);
  }

  @Test
  public void repositoryUploadFailedWrongExtension() throws Exception {
    when(request.getPath()).thenReturn(WRONG_EXTENSION_FULL_PATH_VALUE);
    Response response = underTest.putArchive.handle(context);
    verify(rHostedFacet, times(0)).upload(WRONG_EXTENSION_FULL_PATH_VALUE, payload);
    assertThat(response.getStatus().getCode(), is(equalTo(BAD_REQUEST)));
    assertThat(response.getStatus().getMessage(), is(equalTo(NOT_VALID_EXTENSION_ERROR_MESSAGE)));
  }

  @Test
  public void repositoryUploadFailedWrongPath() throws Exception {
    when(request.getPath()).thenReturn(WRONG_PATH_FULL_PATH_VALUE);
    Response response = underTest.putArchive.handle(context);
    verify(rHostedFacet, times(0)).upload(WRONG_PATH_FULL_PATH_VALUE, payload);
    assertThat(response.getStatus().getCode(), is(equalTo(BAD_REQUEST)));
    assertThat(response.getStatus().getMessage(), is(equalTo(NOT_VALID_PATH_ERROR_MESSAGE)));
  }

  private void assertStatus(final Handler handler, final int status) throws Exception {
    Response response = handler.handle(context);
    assertThat(response.getStatus().getCode(), is(equalTo(status)));
  }

  private void initialiseTestFixtures() {
    attributesMap = new AttributesMap();
    tokens = new HashMap<>();
    state = new TestState(tokens);
    attributesMap.set(State.class, state);
  }

  private void initialiseMockBehaviour() {
    when(context.getAttributes()).thenReturn(attributesMap);
    when(context.getRepository()).thenReturn(repository);
    when(context.getRequest()).thenReturn(request);
    when(request.getPayload()).thenReturn(payload);
    when(request.getPath()).thenReturn(FULL_PATH_VALUE);
    when(repository.facet(OrientRHostedFacet.class)).thenReturn(rHostedFacet);
  }

  private void setupForGetContentTest() {
    tokens.put("filename", FILENAME_VALUE);
    when(rHostedFacet.getStoredContent(anyString())).thenReturn(content);
  }

  class TestState
      implements State
  {

    private final Map<String, String> tokens;

    public TestState(final Map<String, String> tokens) {
      this.tokens = tokens;
    }

    @Override
    public String pattern() {
      return null;
    }

    @Override
    public Map<String, String> getTokens() {
      return tokens;
    }
  }
}
