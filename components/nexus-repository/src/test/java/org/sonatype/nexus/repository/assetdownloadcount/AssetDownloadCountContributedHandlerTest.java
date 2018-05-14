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
package org.sonatype.nexus.repository.assetdownloadcount;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.assetdownloadcount.internal.AssetDownloadCountContributedHandler;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AssetDownloadCountContributedHandlerTest
    extends TestSupport
{
  @Mock
  private AssetDownloadCountStore assetDownloadCountStore;

  @Mock
  private Repository repository;

  @Mock
  private Request request;

  @Mock
  private Response response;

  @Mock
  private Context context;

  @Mock
  private Content content;

  @Mock
  private AttributesMap attributesMap;

  @Mock
  private Asset asset;

  private AssetDownloadCountContributedHandler underTest;

  @Before
  public void setUp() throws Exception {
    when(context.getRepository()).thenReturn(repository);
    when(context.getRequest()).thenReturn(request);
    when(context.proceed()).thenReturn(response);

    when(request.getAction()).thenReturn(HttpMethods.GET);

    when(response.getStatus()).thenReturn(Status.success(200));
    when(response.getPayload()).thenReturn(content);

    when(repository.getName()).thenReturn("myreponame");

    when(content.getAttributes()).thenReturn(attributesMap);

    when(attributesMap.get(Asset.class)).thenReturn(asset);

    when(asset.name()).thenReturn("myassetname");

    when(assetDownloadCountStore.isEnabled()).thenReturn(true);

    underTest = new AssetDownloadCountContributedHandler(assetDownloadCountStore);
  }

  @Test
  public void testHandle_nullResponse() throws Exception {
    when(context.proceed()).thenReturn(null);

    underTest.handle(context);

    verify(assetDownloadCountStore).isEnabled();
    verifyNoMoreInteractions(assetDownloadCountStore);
  }

  @Test
  public void testHandle_unsuccessfulResponse() throws Exception {
    when(response.getStatus()).thenReturn(Status.failure(404));

    underTest.handle(context);

    verify(assetDownloadCountStore).isEnabled();
    verifyNoMoreInteractions(assetDownloadCountStore);
  }

  @Test
  public void testHandle_invalidRequestAction() throws Exception {
    when(request.getAction()).thenReturn(HttpMethods.DELETE);

    underTest.handle(context);

    verify(assetDownloadCountStore).isEnabled();
    verifyNoMoreInteractions(assetDownloadCountStore);
  }

  @Test
  public void testHandle_countIncremented() throws Exception {
    underTest.handle(context);
    underTest.handle(context);
    underTest.handle(context);

    verify(assetDownloadCountStore, times(3)).incrementCount("myreponame", "myassetname");
  }

  @Test
  public void testHandle_disabled() throws Exception {
    when(assetDownloadCountStore.isEnabled()).thenReturn(false);

    underTest.handle(context);
    verify(assetDownloadCountStore).isEnabled();
    verifyNoMoreInteractions(assetDownloadCountStore);
  }
}
