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
package org.sonatype.nexus.coreui;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.coreui.internal.UploadService;
import org.sonatype.nexus.repository.upload.UploadDefinition;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

class UploadComponentTest
    extends Test5Support
{
  @Mock
  UploadService uploadService;

  @InjectMocks
  UploadComponent underTest;

  @Test
  void getUploadDefinitionsReturnsOnlyUiUploadDefinitions() {
    UploadDefinition uiDef = new UploadDefinition("maven2", true, true, true,
        Collections.emptyList(), Collections.emptyList());
    UploadDefinition nonUiDef = new UploadDefinition("raw", false, true, false,
        Collections.emptyList(), Collections.emptyList());

    when(uploadService.getAvailableDefinitions()).thenReturn(Arrays.asList(uiDef, nonUiDef));

    Collection<UploadDefinition> result = underTest.getUploadDefinitions();

    assertThat(result, hasSize(1));
    assertThat(result.iterator().next().getFormat(), is("maven2"));
  }

  @Test
  void getUploadDefinitionsReturnsEmptyWhenNoUiUploads() {
    UploadDefinition nonUiDef = new UploadDefinition("raw", false, true, false,
        Collections.emptyList(), Collections.emptyList());

    when(uploadService.getAvailableDefinitions()).thenReturn(Collections.singletonList(nonUiDef));

    Collection<UploadDefinition> result = underTest.getUploadDefinitions();

    assertThat(result, is(empty()));
  }

  @Test
  void getUploadDefinitionsReturnsEmptyWhenNoDefinitions() {
    when(uploadService.getAvailableDefinitions()).thenReturn(Collections.emptyList());

    Collection<UploadDefinition> result = underTest.getUploadDefinitions();

    assertThat(result, is(empty()));
  }

  @Test
  void getUploadDefinitionsReturnsAllWhenAllAreUiUploads() {
    UploadDefinition uiDef1 = new UploadDefinition("maven2", true, true, true,
        Collections.emptyList(), Collections.emptyList());
    UploadDefinition uiDef2 = new UploadDefinition("npm", true, false, false,
        Collections.emptyList(), Collections.emptyList());

    when(uploadService.getAvailableDefinitions()).thenReturn(Arrays.asList(uiDef1, uiDef2));

    Collection<UploadDefinition> result = underTest.getUploadDefinitions();

    assertThat(result, hasSize(2));
  }
}
