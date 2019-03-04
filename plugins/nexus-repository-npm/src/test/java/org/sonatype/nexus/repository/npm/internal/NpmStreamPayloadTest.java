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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.MissingAssetBlobException;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamFunction;
import org.sonatype.nexus.repository.view.payloads.StreamPayload.InputStreamSupplier;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NpmStreamPayloadTest
    extends TestSupport
{
  @Spy
  private InputStream input = new ByteArrayInputStream("{}".getBytes());

  @Mock
  private InputStreamFunction<MissingAssetBlobException> missingBlobInputStreamSupplier;

  @Mock
  private OutputStream output;

  @Mock
  private InputStreamSupplier inputStreamSupplier;

  @Mock
  private Asset asset;

  private NpmStreamPayload underTest;

  @Before
  public void setUp() {
    underTest = spy(new NpmStreamPayload(inputStreamSupplier));
  }

  @Test
  public void copy_Uses_NpmStreaming() throws IOException {
    underTest.copy(input, output);

    verify(input, atLeastOnce()).read(any(byte[].class), any(int.class), any(int.class));
    verify(output, atLeastOnce()).write(any(byte[].class), any(int.class), any(int.class));
    verify(underTest).copy(eq(input), eq(output));
  }

  @Test
  public void missingAssetBlobException_On_OpenInputStream_Uses_InputStreamSupplier() throws IOException {
    doThrow(new MissingAssetBlobException(asset)).when(inputStreamSupplier).get();

    underTest.openInputStream();
    verify(missingBlobInputStreamSupplier, never()).apply(any());

    underTest.missingBlobInputStreamSupplier(missingBlobInputStreamSupplier);
    underTest.openInputStream();

    verify(missingBlobInputStreamSupplier).apply(any());
  }
}
