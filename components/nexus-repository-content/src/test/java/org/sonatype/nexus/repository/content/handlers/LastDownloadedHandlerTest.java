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
package org.sonatype.nexus.repository.content.handlers;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.time.UTC;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.capability.GlobalRepositorySettings;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.time.Duration.ofDays;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.http.HttpMethods.DELETE;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.http.HttpMethods.OPTIONS;
import static org.sonatype.nexus.repository.http.HttpMethods.POST;
import static org.sonatype.nexus.repository.http.HttpMethods.PUT;

public class LastDownloadedHandlerTest
    extends TestSupport
{
  @Mock
  private Context context;

  @Mock
  private Response response;

  @Mock
  private Content payload;

  @Mock
  private FluentAsset asset;

  @Mock
  private Repository repository;

  @Mock
  private Request request;

  @Mock
  private GlobalRepositorySettings globalSettings;

  @Mock
  private LastDownloadedAttributeHandler lastDownloadedAttributeHandler;

  private AttributesMap attributes;

  private LastDownloadedHandler underTest;

  @Before
  public void setup() throws Exception {
    configureHappyPath();

    underTest = new LastDownloadedHandler(globalSettings);
    underTest.injectExtraDependencies(lastDownloadedAttributeHandler);
  }

  @Test
  public void shouldMarkAssetAsDownloadedWhenSuccessfulGetRequest() throws Exception {
    Response handledResponse = underTest.handle(context);

    verify(asset).markAsDownloaded();

    assertThat(handledResponse, is(equalTo(response)));
  }

  @Test
  public void shouldMarkAssetAsDownloadedWhenSuccessfulHeadRequest() throws Exception {
    when(request.getAction()).thenReturn(HEAD);

    Response handledResponse = underTest.handle(context);

    verify(asset).markAsDownloaded();

    assertThat(handledResponse, is(equalTo(response)));
  }

  @Test
  public void shouldMarkAssetAsDownloadedWhenNotModified() throws Exception {
    when(response.getStatus()).thenReturn(new Status(false, 304));

    Response handledResponse = underTest.handle(context);

    verify(asset).markAsDownloaded();

    assertThat(handledResponse, is(equalTo(response)));
  }

  @Test
  public void shouldNotMarkAssetAsDownloadedOnFailure() throws Exception {
    when(response.getStatus()).thenReturn(new Status(false, 500));

    testNoExceptionThrownAndVerifySaveNotCalled();
  }

  @Test
  public void shouldUpdateIfNotRecentlyChanged() throws Exception {
    when(asset.lastDownloaded()).thenReturn(of(UTC.now().minusDays(2)));
    when(globalSettings.getLastDownloadedInterval()).thenReturn(ofDays(1));

    Response handledResponse = underTest.handle(context);

    verify(asset).markAsDownloaded();

    assertThat(handledResponse, is(equalTo(response)));
  }

  @Test
  public void shouldNotUpdateIfRecentlyChanged() throws Exception {
    when(asset.lastDownloaded()).thenReturn(of(UTC.now()));
    when(globalSettings.getLastDownloadedInterval()).thenReturn(ofDays(1));

    testNoExceptionThrownAndVerifySaveNotCalled();
  }

  @Test
  public void shouldIgnoreNullContent() throws Exception {
    when(response.getPayload()).thenReturn(null);

    testNoExceptionThrownAndVerifySaveNotCalled();
  }

  @Test
  public void shouldIgnoreMissingAttribute() throws Exception {
    attributes.remove(Asset.class);

    testNoExceptionThrownAndVerifySaveNotCalled();
  }

  @Test
  public void shouldIgnoreStringContent() throws Exception {
    when(response.getPayload()).thenReturn(new StringPayload("message", null));

    testNoExceptionThrownAndVerifySaveNotCalled();
  }

  @Test
  public void onlyUpdateForGetAndHeadRequests() throws Exception {
    assertDownloadedTimeNotUpdatedFor(PUT, POST, DELETE, OPTIONS);
  }

  private void assertDownloadedTimeNotUpdatedFor(String... methods) throws Exception {
    for (String method : methods) {
      when(request.getAction()).thenReturn(method);

      testNoExceptionThrownAndVerifySaveNotCalled();
    }
  }

  private void testNoExceptionThrownAndVerifySaveNotCalled() throws Exception {
    Response handledResponse = underTest.handle(context);

    verify(asset, never()).markAsDownloaded();

    assertThat(handledResponse, is(equalTo(response)));
  }

  private void configureHappyPath() throws Exception {
    when(asset.lastDownloaded()).thenReturn(empty());

    attributes = new AttributesMap();
    attributes.set(Asset.class, asset);

    when(context.proceed()).thenReturn(response);
    when(context.getRepository()).thenReturn(repository);
    when(context.getRequest()).thenReturn(request);
    when(request.getAction()).thenReturn(GET);
    when(response.getPayload()).thenReturn(payload);
    when(response.getStatus()).thenReturn(new Status(true, 200));
    when(payload.getAttributes()).thenReturn(attributes);
  }
}
