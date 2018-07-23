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
package org.sonatype.nexus.repository.npm.internal;

import java.io.InputStream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.npm.internal.NpmPublishParser;
import org.sonatype.nexus.repository.npm.internal.NpmPublishRequest;
import org.sonatype.nexus.repository.npm.internal.NpmRequestParser;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.user.UserNotFoundException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NpmRequestParserTest
    extends TestSupport
{
  @Mock
  Payload payload;

  @Mock
  InputStream inputStream;

  @Mock
  TempBlob tempBlob;

  @Mock
  Blob blob;

  @Mock
  BlobId blobId;

  @Mock
  Repository repository;

  @Mock
  StorageFacet storageFacet;

  @Mock
  NpmPublishParser parser;

  @Mock
  NpmPublishRequest request;

  @Mock
  JsonParser jsonParser;

  @Mock
  SecuritySystem securitySystem;

  NpmRequestParser underTest;

  @Before
  public void setUp() throws Exception {
    underTest = spy(new NpmRequestParser(securitySystem));

    when(tempBlob.get()).thenReturn(inputStream);
    when(tempBlob.getBlob()).thenReturn(blob);
    when(blob.getId()).thenReturn(blobId);
    when(blobId.toString()).thenReturn("blob-id");
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(storageFacet.createTempBlob(eq(payload), any(Iterable.class))).thenAnswer(invocation -> tempBlob);
    when(parser.parse(null)).thenReturn(request);
    when(securitySystem.currentUser()).thenThrow(UserNotFoundException.class);
  }

  @Test
  public void parsePublishOrUnpublishJson() throws Exception {
    doReturn(parser).when(underTest).npmPublishParserFor(any(JsonParser.class), eq(storageFacet));
    try (NpmPublishRequest returnedRequest = underTest.parsePublish(repository, payload)) {
      assertThat(returnedRequest, is(request));
    }
  }

  @Test
  public void parsePublishOrUnpublishJsonWithCharsetChange() throws Exception {
    doThrow(new JsonParseException(jsonParser, "Invalid UTF-8")).when(underTest)
        .parseNpmPublish(eq(storageFacet), eq(tempBlob), eq(UTF_8));
    doReturn(request).when(underTest).parseNpmPublish(eq(storageFacet), eq(tempBlob), eq(ISO_8859_1));
    try (NpmPublishRequest returnedRequest = underTest.parsePublish(repository, payload)) {
      assertThat(returnedRequest, is(request));
    }
    verify(underTest).parseNpmPublish(storageFacet, tempBlob, UTF_8);
    verify(underTest).parseNpmPublish(storageFacet, tempBlob, ISO_8859_1);
  }
}
