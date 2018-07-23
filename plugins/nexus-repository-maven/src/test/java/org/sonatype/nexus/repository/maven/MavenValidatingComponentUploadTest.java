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
package org.sonatype.nexus.repository.maven;

import java.util.Collections;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type;
import org.sonatype.nexus.repository.upload.UploadRegexMap;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.rest.ValidationErrorXO;
import org.sonatype.nexus.rest.ValidationErrorsException;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class MavenValidatingComponentUploadTest
    extends TestSupport
{

  private static final String MAVEN_CLASSIFIER_AND_EXTENSION_EXTRACTOR_REGEX = "-(?:(?:\\.?\\d)+)(?:-(?:SNAPSHOT|\\d+))?(?:-(\\w+))?\\.((?:\\.?\\w)+)$";

  private static final String GROUP_NAME_COORDINATES = "Component coordinates";

  @Mock
  private UploadDefinition uploadDefinition;

  @Mock
  private PartPayload jarPayload;

  private ComponentUpload componentUpload;

  @Before
  public void setup() {
    when(uploadDefinition.getComponentFields()).thenReturn(asList(
        new UploadFieldDefinition("groupId", "Group ID", null, false, Type.STRING, GROUP_NAME_COORDINATES),
        new UploadFieldDefinition("artifactId", "Artifact ID", null, false, Type.STRING, GROUP_NAME_COORDINATES),
        new UploadFieldDefinition("version", false, Type.STRING, GROUP_NAME_COORDINATES),
        new UploadFieldDefinition("generate-pom", "Generate a POM", null, true, Type.BOOLEAN, GROUP_NAME_COORDINATES),
        new UploadFieldDefinition("packaging", true, Type.STRING, GROUP_NAME_COORDINATES)));
    when(uploadDefinition.getAssetFields()).thenReturn(asList(
        new UploadFieldDefinition("classifier", true, Type.STRING),
        new UploadFieldDefinition("extension", false, Type.STRING)));
    when(uploadDefinition.getFormat()).thenReturn(Maven2Format.NAME);
    when(uploadDefinition.getRegexMap()).thenReturn(new UploadRegexMap(
        MAVEN_CLASSIFIER_AND_EXTENSION_EXTRACTOR_REGEX, "classifier", "extension"));
    when(jarPayload.getFieldName()).thenReturn("foo.jar");
    when(jarPayload.getName()).thenReturn("asset");

    componentUpload = new ComponentUpload();
  }

  @Test
  public void testValidate_missingField() {
    AssetUpload assetUpload = new AssetUpload();
    assetUpload.getFields().put("extension", "jar");
    assetUpload.setPayload(jarPayload);
    componentUpload.getAssetUploads().add(assetUpload);

    expectExceptionOnValidate(componentUpload,
          "Missing required component field 'Group ID'",
          "Missing required component field 'Artifact ID'",
          "Missing required component field 'Version'");
  }

  @Test
  public void testValidate_missingAssetField() {
    AssetUpload assetUpload = new AssetUpload();
    assetUpload.setPayload(jarPayload);
    componentUpload.getAssetUploads().add(assetUpload);

    componentUpload.getFields().put("groupId", "org.apache.maven");
    componentUpload.getFields().put("artifactId", "tomcat");
    componentUpload.getFields().put("version", "5.0.28");

    expectExceptionOnValidate(componentUpload,"Missing required asset field 'Extension' on '1'");
  }

  @Test
  public void testValidate_unknownField() {
    AssetUpload assetUpload = new AssetUpload();
    assetUpload.setPayload(jarPayload);
    assetUpload.getFields().put("extension", "jar");
    assetUpload.getFields().put("bar", "bar");
    componentUpload.getAssetUploads().add(assetUpload);

    componentUpload.getFields().put("groupId", "org.apache.maven");
    componentUpload.getFields().put("artifactId", "tomcat");
    componentUpload.getFields().put("version", "5.0.28");
    componentUpload.getFields().put("foo", "foo");

    expectExceptionOnValidate(componentUpload,
        "Unknown component field 'foo'", "Unknown field 'bar' on asset '1'");
  }

  @Test
  public void testValidate_allowMissingComponentFieldsWhenPomAssetIsPresent() {
    AssetUpload assetUpload = new AssetUpload();
    assetUpload.setPayload(jarPayload);
    assetUpload.setFields(Collections.singletonMap("extension", "pom"));
    componentUpload.getAssetUploads().add(assetUpload);

    MavenValidatingComponentUpload validated = new MavenValidatingComponentUpload(uploadDefinition, componentUpload);
    validated.getComponentUpload();
  }


  @Test
  public void testValidate_duplicates() {
    AssetUpload assetUploadOne = new AssetUpload();
    assetUploadOne.getFields().putAll(ImmutableMap.of("extension", "x", "classifier", "y"));
    assetUploadOne.setPayload(jarPayload);

    AssetUpload assetUploadTwo = new AssetUpload();
    assetUploadTwo.getFields().putAll(ImmutableMap.of("extension", "x", "classifier", "y"));
    assetUploadTwo.setPayload(jarPayload);

    AssetUpload assetUploadThree = new AssetUpload();
    assetUploadThree.getFields().putAll(ImmutableMap.of("extension", "x"));
    assetUploadThree.setPayload(jarPayload);

    componentUpload.getFields().putAll(ImmutableMap.of("groupId", "g", "artifactId", "a", "version", "1"));
    componentUpload.getAssetUploads().addAll(asList(assetUploadOne, assetUploadTwo, assetUploadThree));

    expectExceptionOnValidate(componentUpload, "The assets 1 and 2 have identical coordinates");
  }

  private void expectExceptionOnValidate(final ComponentUpload component, final String... message)
  {
    try {
      MavenValidatingComponentUpload validated = new MavenValidatingComponentUpload(uploadDefinition, component);
      validated.getComponentUpload();
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
