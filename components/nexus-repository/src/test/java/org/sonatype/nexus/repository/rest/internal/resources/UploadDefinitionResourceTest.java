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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.NotFoundException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.rest.api.UploadDefinitionXO;
import org.sonatype.nexus.repository.rest.api.UploadFieldDefinitionXO;
import org.sonatype.nexus.repository.upload.UploadConfiguration;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class UploadDefinitionResourceTest
    extends TestSupport
{
  UploadDefinitionResource underTest;

  @Mock
  UploadConfiguration uploadConfiguration;

  @Mock
  UploadManager uploadManager;

  @Before
  public void setUp() throws Exception {
    underTest = new UploadDefinitionResource(uploadManager, uploadConfiguration);
    when(uploadConfiguration.isEnabled()).thenReturn(true);
  }

  @Test
  public void testGet_success() {
    UploadDefinition uploadDefinition = createUploadDefinition("testFormat");
    UploadDefinition uploadDefinition2 = createUploadDefinition("testFormat2");

    when(uploadManager.getAvailableDefinitions()).thenReturn(Arrays.asList(uploadDefinition, uploadDefinition2));

    List<UploadDefinitionXO> xos = underTest.get();
    assertThat(xos.size(), is(2));
    assertXO(uploadDefinition, xos.get(0));
    assertXO(uploadDefinition2, xos.get(1));
  }

  @Test
  public void testGet_disabled() {
    when(uploadConfiguration.isEnabled()).thenReturn(false);
    try {
      underTest.get();
      fail("Expected NotFoundException to be thrown");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is("HTTP 404 Not Found"));
    }
  }

  @Test
  public void testGetByFormat_success() {
    UploadDefinition uploadDefinition = createUploadDefinition("testFormat");
    when(uploadManager.getByFormat("testFormat")).thenReturn(uploadDefinition);

    UploadDefinitionXO xo = underTest.get("testFormat");
    assertXO(uploadDefinition, xo);
  }

  @Test
  public void testGetByFormat_notFound() {
    try {
      underTest.get("notfound");
      fail("Expected NotFoundException to be thrown");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is("Unable to locate upload definition for format 'notfound'"));
    }
  }

  @Test
  public void testGetByFormat_disabled() {
    when(uploadConfiguration.isEnabled()).thenReturn(false);
    try {
      underTest.get("doesntmatter");
      fail("Expected NotFoundException to be thrown");
    }
    catch (NotFoundException e) {
      assertThat(e.getMessage(), is("HTTP 404 Not Found"));
    }
  }

  private UploadDefinition createUploadDefinition(String format) {
    return new UploadDefinition(format, false,
        Collections.singletonList(new UploadFieldDefinition("testField", true, UploadFieldDefinition.Type.STRING)),
        Collections.singletonList(new UploadFieldDefinition("testField", true, UploadFieldDefinition.Type.STRING)));
  }

  private void assertXO(UploadDefinition expected, UploadDefinitionXO actual) {
    assertThat(actual.getFormat(), is(expected.getFormat()));
    assertThat(actual.isMultipleUpload(), is(expected.isMultipleUpload()));

    assertThat(actual.getComponentFields().size(), is(expected.getComponentFields().size()));
    for (int i = 0; i < expected.getComponentFields().size(); i++) {
      assertXO(expected.getComponentFields().get(i), actual.getComponentFields().get(i));
    }

    assertThat(actual.getAssetFields().size(), is(expected.getAssetFields().size() + 1));
    for (int i = 0; i < expected.getAssetFields().size(); i++) {
      assertXO(expected.getAssetFields().get(i), actual.getAssetFields().get(i));
    }
    assertFileXO(actual.getAssetFields().get(actual.getAssetFields().size() - 1));
  }

  private void assertXO(UploadFieldDefinition expected, UploadFieldDefinitionXO actual) {
    assertThat(actual.getName(), is(expected.getName()));
    assertThat(actual.getType(), is(expected.getType().name()));
    assertThat(actual.isOptional(), is(expected.isOptional()));
    assertThat(actual.getDescription(), is(expected.getHelpText()));
  }

  private void assertFileXO(UploadFieldDefinitionXO actual) {
    assertThat(actual.getName(), is("asset"));
    assertThat(actual.getType(), is("FILE"));
    assertThat(actual.isOptional(), is(false));
  }
}
