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
package org.sonatype.nexus.repository.upload.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;
import org.sonatype.nexus.repository.types.VirtualType;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadHandler;
import org.sonatype.nexus.rest.ValidationErrorXO;
import org.sonatype.nexus.rest.ValidationErrorsException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UploadManagerImplTest
    extends TestSupport
{
  private UploadManagerImpl underTest;

  @Mock
  UploadHandler handlerA;

  @Mock
  UploadDefinition uploadA;

  @Mock
  UploadHandler handlerB;

  @Mock
  UploadDefinition uploadB;

  @Mock
  Repository repository;

  @Before
  public void setup() {
    when(handlerA.getDefinition()).thenReturn(uploadA);
    when(handlerB.getDefinition()).thenReturn(uploadB);

    when(repository.getFormat()).thenReturn(new Format("a")
    {
    });
    when(repository.getType()).thenReturn(new HostedType());

    Map<String, UploadHandler> handlers = new HashMap<>();
    handlers.put("a", handlerA);
    handlers.put("b", handlerB);

    underTest = new UploadManagerImpl(handlers);
  }

  @Test
  public void testGetAvailable() {
    assertThat(underTest.getAvailableDefinitions(), contains(uploadA, uploadB));
  }

  @Test
  public void testGetByFormat() {
    assertThat(underTest.getByFormat("a"), is(uploadA));
    assertThat(underTest.getByFormat("b"), is(uploadB));
  }

  @Test
  public void testHandle() throws IOException {
    ComponentUpload component = mock(ComponentUpload.class);
    when(component.getAssetUploads()).thenReturn(Collections.singletonList(new AssetUpload()));
    underTest.handle(repository, component);

    verify(handlerA, times(1)).handle(repository, component);
    verify(handlerB, never()).handle(repository, component);

    // Try the other, to be sure!
    reset(handlerA, handlerB);
    when(handlerB.getDefinition()).thenReturn(uploadB);

    when(repository.getFormat()).thenReturn(new Format("b")
    {
    });

    underTest.handle(repository, component);

    verify(handlerB, times(1)).handle(repository, component);
    verify(handlerA, never()).handle(repository, component);
  }

  @Test
  public void testHandle_unsupportedRepositoryFormat() throws IOException {
    ComponentUpload component = mock(ComponentUpload.class);
    when(repository.getFormat()).thenReturn(new Format("c")
    {
    });

    expectExceptionOnUpload(repository, component, "Uploading components to 'c' repositories is unsupported");
  }

  @Test
  public void testHandle_unsupportedRepositoryGroupType() throws IOException {
    when(repository.getType()).thenReturn(new GroupType());
    expectExceptionOnUpload(repository, mock(ComponentUpload.class),
        "Uploading components to a 'group' type repository is unsupported, must be 'hosted'");
  }

  @Test
  public void testHandle_unsupportedRepositoryProxyType() throws IOException {
    when(repository.getType()).thenReturn(new ProxyType());
    expectExceptionOnUpload(repository, mock(ComponentUpload.class),
        "Uploading components to a 'proxy' type repository is unsupported, must be 'hosted'");
  }

  @Test
  public void testHandle_unsupportedRepositoryVirtualType() throws IOException {
    when(repository.getType()).thenReturn(new VirtualType());
    expectExceptionOnUpload(repository, mock(ComponentUpload.class),
        "Uploading components to a 'virtual' type repository is unsupported, must be 'hosted'");
  }


  private void expectExceptionOnUpload(final Repository repository,
                                       final ComponentUpload component,
                                       final String message) throws IOException
  {
    try {
      underTest.handle(repository, component);
      fail("Expected exception to be thrown");
    }
    catch (ValidationErrorsException exception) {
      List<String> messages = exception.getValidationErrors().stream()
          .map(ValidationErrorXO::getMessage)
          .collect(Collectors.toList());
      assertThat(messages, contains(message));
    }
  }
}
