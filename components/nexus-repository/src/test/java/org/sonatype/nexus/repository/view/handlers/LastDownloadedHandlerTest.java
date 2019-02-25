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
package org.sonatype.nexus.repository.view.handlers;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetManager;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.transaction.UnitOfWork;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
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
  private Asset asset;

  @Mock
  private Repository repository;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private StorageTx tx;

  @Mock
  private Request request;
  
  @Mock
  private EntityMetadata entityMetadata;
  
  @Mock
  private EntityId id;

  @Mock
  private AssetManager assetManager;

  private AttributesMap attributes;

  private LastDownloadedHandler underTest;

  @Before
  public void setup() throws Exception {
    configureHappyPath();

    underTest = new LastDownloadedHandler(assetManager);

    UnitOfWork.beginBatch(tx);
  }

  @After
  public void tearDown() {
    UnitOfWork.end();
  }

  @Test
  public void shouldMarkAssetAsDownloadedWhenSuccessfulGetRequest() throws Exception {
    when(assetManager.maybeUpdateLastDownloaded(asset)).thenReturn(true);

    Response handledResponse = underTest.handle(context);

    verify(tx).saveAsset(asset);

    assertThat(handledResponse, is(equalTo(response)));
  }

  @Test
  public void shouldMarkAssetAsDownloadedWhenSuccessfulHeadRequest() throws Exception {
    when(request.getAction()).thenReturn(HEAD);
    when(assetManager.maybeUpdateLastDownloaded(asset)).thenReturn(true);

    Response handledResponse = underTest.handle(context);

    verify(tx).saveAsset(asset);

    assertThat(handledResponse, is(equalTo(response)));
  }

  @Test
  public void shouldMarkAssetAsDownloadedWhenNotModified() throws Exception {
    when(response.getStatus()).thenReturn(new Status(false, 304));
    when(assetManager.maybeUpdateLastDownloaded(asset)).thenReturn(true);
    
    Response handledResponse = underTest.handle(context);

    verify(tx).saveAsset(asset);

    assertThat(handledResponse, is(equalTo(response)));
  }

  @Test
  public void shouldNotMarkAssetAsDownloadedOnFailure() throws Exception {
    when(response.getStatus()).thenReturn(new Status(false, 500));

    verifyNoExceptionThrownAndSaveNotCalled();
  }

  @Test
  public void doNotUpdateWhenDateNotChanged() throws Exception {
    when(assetManager.maybeUpdateLastDownloaded(asset)).thenReturn(false);

    verifyNoExceptionThrownAndSaveNotCalled();
  }

  @Test
  public void shouldIgnoreNullContent() throws Exception {
    when(response.getPayload()).thenReturn(null);

    verifyNoExceptionThrownAndSaveNotCalled();
  }

  @Test
  public void shouldIgnoreMissingAttribute() throws Exception {
    attributes.remove(Asset.class);

    verifyNoExceptionThrownAndSaveNotCalled();
  }

  @Test
  public void shouldIgnoreStringContent() throws Exception {
    when(response.getPayload()).thenReturn(new StringPayload("message", null));

    verifyNoExceptionThrownAndSaveNotCalled();
  }

  @Test
  public void onlyUpdateForGetAndHeadRequests() throws Exception {
    assertDownloadedTimeNotUpdatedFor(PUT, POST, DELETE, OPTIONS);
  }

  @Test
  public void handleNullOnSecondFindAsset() throws Exception {
    underTest.updateLastDownloadedTime(tx, null);
  }
  
  private void assertDownloadedTimeNotUpdatedFor(String... methods) throws Exception {
    for (String method : methods) {
      when(request.getAction()).thenReturn(method);

      verifyNoExceptionThrownAndSaveNotCalled();
    }
  }

  private void verifyNoExceptionThrownAndSaveNotCalled() throws Exception {
    Response handledResponse = underTest.handle(context);

    verify(tx, never()).saveAsset(asset);

    assertThat(handledResponse, is(equalTo(response)));
  }

  private void configureHappyPath() throws Exception {
    attributes = new AttributesMap();

    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(storageFacet.txSupplier()).thenReturn(() -> tx);

    when(context.proceed()).thenReturn(response);
    when(context.getRepository()).thenReturn(repository);
    when(context.getRequest()).thenReturn(request);
    when(request.getAction()).thenReturn(GET);

    when(response.getPayload()).thenReturn(payload);

    when(payload.getAttributes()).thenReturn(attributes);

    when(response.getStatus()).thenReturn(new Status(true, 200));
    when(assetManager.maybeUpdateLastDownloaded(asset)).thenReturn(true);
    when(asset.getEntityMetadata()).thenReturn(entityMetadata);
    
    when(entityMetadata.getId()).thenReturn(id);
    
    when(tx.findAsset(any())).thenReturn(asset);

    attributes.set(Asset.class, asset);
  }
}
