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
package org.sonatype.nexus.repository.rest.api;

import java.util.List;

import jakarta.ws.rs.NotFoundException;

import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type;
import org.sonatype.nexus.repository.upload.UploadManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadDefinitionResourceTest
{
  @Mock
  private UploadManager uploadManager;

  private UploadDefinitionResource underTest;

  private UploadDefinition rawDefinition;

  @BeforeEach
  void setUp() {
    underTest = new UploadDefinitionResource(uploadManager);
    rawDefinition = new UploadDefinition(
        "raw", true, true, false,
        List.of(new UploadFieldDefinition("directory", false, Type.STRING, "Component attributes")),
        List.of(new UploadFieldDefinition("filename", false, Type.STRING)),
        null);
  }

  @Test
  void get_mapsAllDefinitionsAndAppendsSyntheticAssetFileField() {
    when(uploadManager.getAvailableDefinitions()).thenReturn(List.of(rawDefinition));

    List<UploadDefinitionXO> result = underTest.get();

    assertThat(result, hasSize(1));
    UploadDefinitionXO xo = result.get(0);
    assertThat(xo.getFormat(), is("raw"));
    assertThat(xo.getComponentFields(), hasSize(1));
    assertThat(xo.getComponentFields().get(0).getName(), is("directory"));
    List<String> assetNames = xo.getAssetFields().stream().map(UploadFieldDefinitionXO::getName).toList();
    assertThat(assetNames, contains("filename", "asset"));
  }

  @Test
  void get_emptyWhenNoDefinitions() {
    when(uploadManager.getAvailableDefinitions()).thenReturn(emptyList());
    assertThat(underTest.get(), hasSize(0));
  }

  @Test
  void getByFormat_returnsMatchingDefinitionWithSyntheticAssetFileField() {
    when(uploadManager.getByFormat("raw")).thenReturn(rawDefinition);

    UploadDefinitionXO xo = underTest.get("raw");

    assertThat(xo.getFormat(), is("raw"));
    List<String> assetNames = xo.getAssetFields().stream().map(UploadFieldDefinitionXO::getName).toList();
    assertThat(assetNames, contains("filename", "asset"));
  }

  @Test
  void getByFormat_throwsNotFoundWhenFormatUnknown() {
    when(uploadManager.getByFormat("unknown")).thenReturn(null);

    assertThrows(NotFoundException.class, () -> underTest.get("unknown"));
  }

  @Test
  void getByFormat_throwsNotFoundWhenApiUploadUnsupported() {
    // A format that allows UI upload but not API upload must still 404 on the API endpoint:
    // get(format) gates on isApiUpload(), so uiUpload=true, apiUpload=false exercises the guard.
    UploadDefinition noApiUpload = new UploadDefinition(
        "raw", true, false, false, emptyList(), emptyList(), null);
    when(uploadManager.getByFormat("raw")).thenReturn(noApiUpload);

    assertThrows(NotFoundException.class, () -> underTest.get("raw"));
  }
}
