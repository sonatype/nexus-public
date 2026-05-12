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
package org.sonatype.nexus.repository.apt.datastore.internal.hosted;

import java.util.Optional;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.metadata.AptMetadataRebuildSchedulerFacet;
import org.sonatype.nexus.repository.apt.internal.snapshot.AptSnapshotHandler;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.BytesPayload;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.http.HttpMethods.DELETE;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD;
import static org.sonatype.nexus.repository.http.HttpMethods.POST;

/**
 * Test for {@link AptHostedHandler}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AptHostedHandlerTest

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
  private AptHostedFacet hostedFacet;

  @Mock
  private AptMetadataRebuildSchedulerFacet schedulerFacet;

  private AttributesMap attributes;

  private AptHostedHandler underTest;

  @Before
  public void setUp() {
    underTest = new AptHostedHandler();
    attributes = new AttributesMap();

    when(context.getRepository()).thenReturn(repository);
    when(context.getRequest()).thenReturn(request);
    when(context.getAttributes()).thenReturn(attributes);

    when(repository.facet(AptContentFacet.class)).thenReturn(contentFacet);
    when(repository.facet(AptHostedFacet.class)).thenReturn(hostedFacet);
    when(repository.facet(AptMetadataRebuildSchedulerFacet.class)).thenReturn(schedulerFacet);
  }

  private void setAssetPath(final String path) {
    attributes.set(AptSnapshotHandler.State.class, new AptSnapshotHandler.State(path));
  }

  @Test
  public void testGet_ReturnsContent_WhenAvailable() throws Exception {
    String path = "dists/focal/InRelease";
    setAssetPath(path);
    when(request.getAction()).thenReturn(GET);

    Content content = createContent("InRelease content");
    when(contentFacet.get(path)).thenReturn(Optional.of(content));

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(200));
    assertThat(response.getPayload(), notNullValue());
    verify(contentFacet).get(path);
    // Critical: verify no rebuild is triggered
    verify(hostedFacet, never()).rebuildMetadata();
  }

  @Test
  public void testGet_Returns404_WhenContentMissing_AndNoRebuildTriggered() throws Exception {
    String path = "dists/focal/InRelease";
    setAssetPath(path);
    when(request.getAction()).thenReturn(GET);
    when(contentFacet.get(path)).thenReturn(Optional.empty());

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(404));
    assertThat(response.getPayload(), nullValue());
    verify(contentFacet).get(path);
    // Critical: verify no rebuild is triggered even when content is missing
    verify(hostedFacet, never()).rebuildMetadata();
  }

  @Test
  public void testGet_PackagesFile_ReturnsContent() throws Exception {
    String path = "dists/focal/main/binary-amd64/Packages";
    setAssetPath(path);
    when(request.getAction()).thenReturn(GET);

    Content content = createContent("Packages content");
    when(contentFacet.get(path)).thenReturn(Optional.of(content));

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(200));
    verify(hostedFacet, never()).rebuildMetadata();
  }

  @Test
  public void testGet_DebPackage_ReturnsContent() throws Exception {
    String path = "pool/main/h/hello/hello_2.10-2_amd64.deb";
    setAssetPath(path);
    when(request.getAction()).thenReturn(GET);

    Content content = createContent("deb package content");
    when(contentFacet.get(path)).thenReturn(Optional.of(content));

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(200));
    verify(hostedFacet, never()).rebuildMetadata();
  }

  @Test
  public void testHead_ReturnsContent_WhenAvailable() throws Exception {
    String path = "dists/focal/Release";
    setAssetPath(path);
    when(request.getAction()).thenReturn(HEAD);

    Content content = createContent("Release content");
    when(contentFacet.get(path)).thenReturn(Optional.of(content));

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(200));
    verify(hostedFacet, never()).rebuildMetadata();
  }

  @Test
  public void testHead_Returns404_WhenContentMissing() throws Exception {
    String path = "dists/focal/Release";
    setAssetPath(path);
    when(request.getAction()).thenReturn(HEAD);
    when(contentFacet.get(path)).thenReturn(Optional.empty());

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(404));
    verify(hostedFacet, never()).rebuildMetadata();
  }

  @Test
  public void testPost_RebuildIndexes_SchedulesRebuild() throws Exception {
    setAssetPath("rebuild-indexes");
    when(request.getAction()).thenReturn(POST);

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(200));
    verify(schedulerFacet).maybeScheduleRebuild();
    verify(hostedFacet, never()).rebuildMetadata();
  }

  @Test
  public void testPost_InvalidPath_Returns405() throws Exception {
    setAssetPath("some/invalid/path");
    when(request.getAction()).thenReturn(POST);

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(405));
    verify(hostedFacet, never()).rebuildMetadata();
  }

  @Test
  public void testDelete_Returns405() throws Exception {
    setAssetPath("dists/focal/Release");
    when(request.getAction()).thenReturn(DELETE);

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(405));
  }

  @Test
  public void testUnsupportedMethod_Returns405() throws Exception {
    setAssetPath("dists/focal/Release");
    when(request.getAction()).thenReturn("PATCH");

    Response response = underTest.handle(context);

    assertThat(response.getStatus().getCode(), is(405));
  }

  private Content createContent(final String data) {
    BytesPayload payload = new BytesPayload(data.getBytes(), "application/octet-stream");
    return new Content(payload);
  }
}
