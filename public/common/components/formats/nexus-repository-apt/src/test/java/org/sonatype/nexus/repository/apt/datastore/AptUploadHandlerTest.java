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
package org.sonatype.nexus.repository.apt.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.datastore.internal.hosted.AptHostedFacet;
import org.sonatype.nexus.repository.apt.internal.AptPackageParser;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile.ControlField;
import org.sonatype.nexus.repository.apt.internal.debian.PackageInfo;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.security.internal.SimpleVariableResolverAdapter;
import org.sonatype.nexus.repository.importtask.ImportStreamConfiguration;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.security.BreadActions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.MockedStatic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AptUploadHandler}
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AptUploadHandlerTest

{
  private static final String REPO_NAME = "apt-hosted";

  @Mock
  private ContentPermissionChecker contentPermissionChecker;

  @Mock
  private Repository repository;

  @Mock
  private AptContentFacet aptContentFacet;

  @Mock
  private AptHostedFacet hostedFacet;

  private AptUploadHandler underTest;

  @Before
  public void setup() {
    underTest = new AptUploadHandler(new SimpleVariableResolverAdapter(), contentPermissionChecker,
        Collections.emptySet());

    when(repository.getName()).thenReturn(REPO_NAME);
    when(repository.getFormat()).thenReturn(new AptFormat());
    when(repository.facet(AptContentFacet.class)).thenReturn(aptContentFacet);
    when(repository.facet(AptHostedFacet.class)).thenReturn(hostedFacet);

    when(contentPermissionChecker.isPermitted(eq(REPO_NAME), eq(AptFormat.NAME), eq(BreadActions.EDIT), any()))
        .thenReturn(true);
  }

  @Test
  public void testSupportsExportImport() {
    assertThat(underTest.supportsExportImport(), is(true));
  }

  @Test
  public void testHandle_streamImport() throws IOException {
    InputStream inputStream = mock(InputStream.class);

    TempBlob tempBlob = mock(TempBlob.class);
    when(aptContentFacet.getTempBlob(any(InputStream.class), any())).thenReturn(tempBlob);

    // Mock the package parser
    PackageInfo packageInfo = mock(PackageInfo.class);
    ControlFile controlFile = mock(ControlFile.class);
    when(controlFile.getField("Package")).thenReturn(java.util.Optional.of(new ControlField("Package", "example")));
    when(controlFile.getField("Version")).thenReturn(java.util.Optional.of(new ControlField("Version", "1.0.0")));
    when(controlFile.getField("Architecture"))
        .thenReturn(java.util.Optional.of(new ControlField("Architecture", "amd64")));
    when(packageInfo.getControlFile()).thenReturn(controlFile);

    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(hostedFacet.put(any(), any(), any())).thenReturn(fluentAsset);

    Content expectedContent = mock(Content.class);
    when(fluentAsset.markAsCached(any(org.sonatype.nexus.repository.view.Payload.class))).thenReturn(fluentAsset);
    when(fluentAsset.download()).thenReturn(expectedContent);

    ImportStreamConfiguration configuration = new ImportStreamConfiguration(repository, inputStream,
        "/pool/main/e/example/example_1.0.0_amd64.deb");

    try (MockedStatic<AptPackageParser> mockedParser = mockStatic(AptPackageParser.class)) {
      mockedParser.when(() -> AptPackageParser.parsePackageInfo(tempBlob)).thenReturn(packageInfo);

      Content actualContent = underTest.handle(configuration);

      assertThat(actualContent, is(expectedContent));
      verify(aptContentFacet).getTempBlob(any(InputStream.class), any());
      verify(hostedFacet).put(any(), any(), any());
    }
  }

  @Test
  public void testHandle_streamImport_differentPath() throws IOException {
    InputStream inputStream = mock(InputStream.class);

    TempBlob tempBlob = mock(TempBlob.class);
    when(aptContentFacet.getTempBlob(any(InputStream.class), any())).thenReturn(tempBlob);

    // Mock the package parser
    PackageInfo packageInfo = mock(PackageInfo.class);
    ControlFile controlFile = mock(ControlFile.class);
    when(controlFile.getField("Package")).thenReturn(java.util.Optional.of(new ControlField("Package", "test-pkg")));
    when(controlFile.getField("Version")).thenReturn(java.util.Optional.of(new ControlField("Version", "2.1.3")));
    when(controlFile.getField("Architecture"))
        .thenReturn(java.util.Optional.of(new ControlField("Architecture", "all")));
    when(packageInfo.getControlFile()).thenReturn(controlFile);

    FluentAsset fluentAsset = mock(FluentAsset.class);
    when(hostedFacet.put(any(), any(), any())).thenReturn(fluentAsset);

    Content expectedContent = mock(Content.class);
    when(fluentAsset.markAsCached(any(org.sonatype.nexus.repository.view.Payload.class))).thenReturn(fluentAsset);
    when(fluentAsset.download()).thenReturn(expectedContent);

    ImportStreamConfiguration configuration = new ImportStreamConfiguration(repository, inputStream,
        "/pool/contrib/t/test-pkg/test-pkg_2.1.3_all.deb");

    try (MockedStatic<AptPackageParser> mockedParser = mockStatic(AptPackageParser.class)) {
      mockedParser.when(() -> AptPackageParser.parsePackageInfo(tempBlob)).thenReturn(packageInfo);

      Content actualContent = underTest.handle(configuration);

      assertThat(actualContent, is(expectedContent));
      verify(hostedFacet).put(any(), any(), any());
    }
  }
}
