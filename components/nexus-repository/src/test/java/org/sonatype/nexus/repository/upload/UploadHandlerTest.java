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
package org.sonatype.nexus.repository.upload;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.rest.ValidationErrorXO;
import org.sonatype.nexus.rest.ValidationErrorsException;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type.STRING;

public class UploadHandlerTest
    extends TestSupport
{
  @Mock
  private UploadHandler uploadHandler;

  @Mock
  private UploadDefinition uploadDefinition;

  @Mock
  private ComponentUpload componentUpload;

  @Mock
  private AssetUpload assetUpload;

  @Before
  public void setup() {
    uploadHandler = new UploadHandler()
    {
      @Override
      public Collection<String> handle(final Repository repository, final ComponentUpload upload) throws IOException {
        return null;
      }

      @Override
      public UploadDefinition getDefinition() {
        return uploadDefinition;
      }

      @Override
      public VariableResolverAdapter getVariableResolverAdapter() {
        return null;
      }

      @Override
      public ContentPermissionChecker contentPermissionChecker() {
        return null;
      }
    };

    when(uploadDefinition.getComponentFields()).thenReturn(emptyList());
    when(uploadDefinition.getAssetFields()).thenReturn(emptyList());
  }

  @Test
  public void testValidate_missingAssets() {
    expectExceptionOnValidate(componentUpload, "No assets found in upload");
  }

  @Test
  public void testValidate_missingAssetField() {
    when(uploadDefinition.getComponentFields()).thenReturn(emptyList());
    when(uploadDefinition.getAssetFields()).thenReturn(
        Collections.singletonList(new UploadFieldDefinition("foo", false, STRING)));

    when(assetUpload.getPayload()).thenReturn(mock(PartPayload.class));
    when(componentUpload.getAssetUploads()).thenReturn(Collections.singletonList(assetUpload));

    expectExceptionOnValidate(componentUpload, "Missing required asset field 'Foo' on '1'");
  }

  @Test
  public void testValidate_missingComponentField() {
    when(uploadDefinition.getAssetFields()).thenReturn(emptyList());
    when(uploadDefinition.getComponentFields()).thenReturn(
        Collections.singletonList(new UploadFieldDefinition("bar", false, STRING)));

    when(assetUpload.getPayload()).thenReturn(mock(PartPayload.class));
    when(componentUpload.getAssetUploads()).thenReturn(Collections.singletonList(assetUpload));

    expectExceptionOnValidate(componentUpload, "Missing required component field 'Bar'");
  }

  @Test
  public void testValidate_noViolations() {
    when(uploadDefinition.getAssetFields()).thenReturn(
        Collections.singletonList(new UploadFieldDefinition("foo", false, STRING)));
    when(uploadDefinition.getComponentFields()).thenReturn(
        Collections.singletonList(new UploadFieldDefinition("bar", false, STRING)));

    when(assetUpload.getPayload()).thenReturn(mock(PartPayload.class));
    when(assetUpload.getFields()).thenReturn(singletonMap("foo", "fooValue"));
    when(assetUpload.getField("foo")).thenReturn("fooValue");

    when(componentUpload.getAssetUploads()).thenReturn(Collections.singletonList(assetUpload));
    when(componentUpload.getFields()).thenReturn(singletonMap("bar", "barValue"));
    when(componentUpload.getField("bar")).thenReturn("barValue");

    try {
      uploadHandler.validate((componentUpload));
    }
    catch (ValidationErrorsException e) {
      fail(format("Unexpected validation exception thrown '%s'", e));
    }
  }

  @Test
  public void testValidate_duplicates() {
    when(uploadDefinition.getAssetFields()).thenReturn(Arrays.asList(new UploadFieldDefinition("field1", true, STRING),
        new UploadFieldDefinition("field2", true, STRING)));

    AssetUpload assetUploadOne = new AssetUpload();
    assetUploadOne.getFields().putAll(ImmutableMap.of("field1", "x", "field2", "y"));
    assetUploadOne.setPayload(mock(PartPayload.class));

    AssetUpload assetUploadTwo = new AssetUpload();
    assetUploadTwo.getFields().putAll(ImmutableMap.of("field1", "x", "field2", "y"));
    assetUploadTwo.setPayload(mock(PartPayload.class));

    AssetUpload assetUploadThree = new AssetUpload();
    assetUploadThree.getFields().putAll(ImmutableMap.of("field1", "x"));
    assetUploadThree.setPayload(mock(PartPayload.class));

    ComponentUpload componentUpload = new ComponentUpload();
    componentUpload.getAssetUploads().addAll(Arrays.asList(assetUploadOne, assetUploadTwo, assetUploadThree));

    expectExceptionOnValidate(componentUpload, "The assets 1 and 2 have identical coordinates");
  }

  private void expectExceptionOnValidate(final ComponentUpload component, final String message)
  {
    try {
      uploadHandler.validate(component);
      fail("Expected exception to be thrown");
    }
    catch (ValidationErrorsException exception) {
      List<String> messages = exception.getValidationErrors().stream()
          .map(ValidationErrorXO::getMessage)
          .collect(toList());
      assertThat(messages, contains(message));
    }
  }
}
