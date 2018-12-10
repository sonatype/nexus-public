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
package org.sonatype.nexus.repository.raw;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.raw.internal.RawFormat;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.internal.SimpleVariableResolverAdapter;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type;
import org.sonatype.nexus.repository.upload.UploadRegexMap;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.security.BreadActions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type.STRING;

public class RawUploadHandlerTest
    extends TestSupport
{
  private RawUploadHandler underTest;

  @Mock
  Content content;

  @Mock
  AttributesMap attributesMap;

  @Mock
  Asset assetPayload;

  @Mock
  Repository repository;

  @Mock
  RawContentFacet rawFacet;

  @Mock
  PartPayload jarPayload;

  @Mock
  PartPayload sourcesPayload;

  @Mock
  StorageTx storageTx;

  @Mock
  private ContentPermissionChecker contentPermissionChecker;

  private String REPO_NAME = "raw-hosted";

  @Before
  public void setup() throws IOException {
    when(contentPermissionChecker.isPermitted(eq(REPO_NAME), eq(RawFormat.NAME), eq(BreadActions.EDIT), any()))
        .thenReturn(true);

    underTest = new RawUploadHandler(contentPermissionChecker, new SimpleVariableResolverAdapter(), emptySet());
    when(repository.getFormat()).thenReturn(new RawFormat());
    when(repository.facet(RawContentFacet.class)).thenReturn(rawFacet);
    when(repository.getName()).thenReturn(REPO_NAME);

    StorageFacet storageFacet = mock(StorageFacet.class);
    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
  }

  @Test
  public void testGetDefinition() {
    UploadDefinition def = underTest.getDefinition();

    assertThat(def.isMultipleUpload(), is(true));
    assertThat(def.getComponentFields(), contains(field("directory", "Directory",
        "Destination for uploaded files (e.g. /path/to/files/)", false, STRING, "Component attributes")));
    assertThat(def.getAssetFields(), contains(field("filename", "Filename", null, false, STRING, null)));
  }

  @Test
  public void testGetDefinitionWithExtensionContributions() {
    //Rebuilding the uploadhandler to provide a set of definition extensions
    underTest = new RawUploadHandler(contentPermissionChecker, new SimpleVariableResolverAdapter(), getDefinitionExtensions());
    UploadDefinition def = underTest.getDefinition();

    assertThat(def.getComponentFields(),
        contains(
            field("directory", "Directory", "Destination for uploaded files (e.g. /path/to/files/)", false, STRING, "Component attributes"),
            field("foo", "Foo", null, true, STRING, "bar")));
    assertThat(def.getAssetFields(), contains(field("filename", "Filename", null, false, STRING, null)));
  }

  @Test
  public void testGetDefinition_regex() {
    UploadRegexMap regexMap = underTest.getDefinition().getRegexMap();
    assertNotNull(regexMap);
    assertNotNull(regexMap.getRegex());
    assertThat(regexMap.getFieldList(), contains("filename"));
  }

  @Test
  public void testHandle() throws IOException {
    ComponentUpload component = new ComponentUpload();

    component.getFields().put("directory", "org/apache/maven");

    AssetUpload asset = new AssetUpload();
    asset.getFields().put("filename", "foo.jar");
    asset.setPayload(jarPayload);
    component.getAssetUploads().add(asset);

    asset = new AssetUpload();
    asset.getFields().put("filename", "bar.jar");
    asset.setPayload(sourcesPayload);
    component.getAssetUploads().add(asset);

    when(assetPayload.componentId()).thenReturn(new DetachedEntityId("foo"));
    when(attributesMap.get(Asset.class)).thenReturn(assetPayload);
    when(content.getAttributes()).thenReturn(attributesMap);
    when(rawFacet.put(any(), any())).thenReturn(content);
    UploadResponse uploadResponse = underTest.handle(repository, component);
    assertThat(uploadResponse.getAssetPaths(), contains("org/apache/maven/foo.jar", "org/apache/maven/bar.jar"));
    assertThat(uploadResponse.getComponentId().getValue(), is("foo"));

    ArgumentCaptor<String> pathCapture = ArgumentCaptor.forClass(String.class);
    verify(rawFacet, times(2)).put(pathCapture.capture(), any(PartPayload.class));

    List<String> paths = pathCapture.getAllValues();

    assertThat(paths, hasSize(2));

    String path = paths.get(0);
    assertNotNull(path);
    assertThat(path, is("org/apache/maven/foo.jar"));

    path = paths.get(1);
    assertNotNull(path);
    assertThat(path, is("org/apache/maven/bar.jar"));
  }

  @Test
  public void testHandle_unauthorized() throws IOException {
    when(contentPermissionChecker.isPermitted(eq(REPO_NAME), eq(RawFormat.NAME), eq(BreadActions.EDIT), any()))
        .thenReturn(false);
    ComponentUpload component = new ComponentUpload();

    component.getFields().put("directory", "org/apache/maven");

    AssetUpload asset = new AssetUpload();
    asset.getFields().put("filename", "foo.jar");
    asset.setPayload(jarPayload);
    component.getAssetUploads().add(asset);

    asset = new AssetUpload();
    asset.getFields().put("filename", "bar.jar");
    asset.setPayload(sourcesPayload);
    component.getAssetUploads().add(asset);

    try {
      underTest.handle(repository, component);
      fail("Expected validation exception");
    }
    catch (ValidationErrorsException e) {
      assertThat(e.getValidationErrors().size(), is(1));
      assertThat(e.getValidationErrors().get(0).getMessage(), is("Not authorized for requested path 'org/apache/maven/foo.jar'"));
    }
  }

  @Test
  public void testHandle_dotDirectory() throws IOException {
    ComponentUpload component = new ComponentUpload();
    component.getFields().put("directory", ".");

    AssetUpload asset = new AssetUpload();
    asset.getFields().put("filename", "foo.jar");
    asset.setPayload(jarPayload);
    component.getAssetUploads().add(asset);

    try {
      underTest.handle(repository, component);
      fail("Expected validation exception");
    }
    catch (ValidationErrorsException e) {
      assertThat(e.getValidationErrors().size(), is(1));
      assertThat(e.getValidationErrors().get(0).getMessage(), is("Path is not allowed to have '.' or '..' segments: './foo.jar'"));
    }
  }

  @Test
  public void testHandle_doubleDotDirectory() throws IOException {
    ComponentUpload component = new ComponentUpload();
    component.getFields().put("directory", "..");

    AssetUpload asset = new AssetUpload();
    asset.getFields().put("filename", "foo.jar");
    asset.setPayload(jarPayload);
    component.getAssetUploads().add(asset);

    try {
      underTest.handle(repository, component);
      fail("Expected validation exception");
    }
    catch (ValidationErrorsException e) {
      assertThat(e.getValidationErrors().size(), is(1));
      assertThat(e.getValidationErrors().get(0).getMessage(), is("Path is not allowed to have '.' or '..' segments: '../foo.jar'"));
    }
  }

  @Test
  public void testHandle_covertDoubleDotDirectory() throws IOException {
    ComponentUpload component = new ComponentUpload();
    component.getFields().put("directory", "foo/..");

    AssetUpload asset = new AssetUpload();
    asset.getFields().put("filename", "foo.jar");
    asset.setPayload(jarPayload);
    component.getAssetUploads().add(asset);

    try {
      underTest.handle(repository, component);
      fail("Expected validation exception");
    }
    catch (ValidationErrorsException e) {
      assertThat(e.getValidationErrors().size(), is(1));
      assertThat(e.getValidationErrors().get(0).getMessage(), is("Path is not allowed to have '.' or '..' segments: 'foo/../foo.jar'"));
    }
  }

  @Test
  public void testHandle_dotFilename() throws IOException {
    ComponentUpload component = new ComponentUpload();
    component.getFields().put("directory", "foo");

    AssetUpload asset = new AssetUpload();
    asset.getFields().put("filename", ".");
    asset.setPayload(jarPayload);
    component.getAssetUploads().add(asset);

    try {
      underTest.handle(repository, component);
      fail("Expected validation exception");
    }
    catch (ValidationErrorsException e) {
      assertThat(e.getValidationErrors().size(), is(1));
      assertThat(e.getValidationErrors().get(0).getMessage(), is("Path is not allowed to have '.' or '..' segments: 'foo/.'"));
    }
  }

  @Test
  public void testHandle_doubleDotFilename() throws IOException {
    ComponentUpload component = new ComponentUpload();
    component.getFields().put("directory", "foo");

    AssetUpload asset = new AssetUpload();
    asset.getFields().put("filename", "..");
    asset.setPayload(jarPayload);
    component.getAssetUploads().add(asset);

    try {
      underTest.handle(repository, component);
      fail("Expected validation exception");
    }
    catch (ValidationErrorsException e) {
      assertThat(e.getValidationErrors().size(), is(1));
      assertThat(e.getValidationErrors().get(0).getMessage(), is("Path is not allowed to have '.' or '..' segments: 'foo/..'"));
    }
  }

  @Test
  public void testHandle_covertDoubleDotFilename() throws IOException {
    ComponentUpload component = new ComponentUpload();
    component.getFields().put("directory", "foo");

    AssetUpload asset = new AssetUpload();
    asset.getFields().put("filename", "foo/../../foo.jar");
    asset.setPayload(jarPayload);
    component.getAssetUploads().add(asset);

    try {
      underTest.handle(repository, component);
      fail("Expected validation exception");
    }
    catch (ValidationErrorsException e) {
      assertThat(e.getValidationErrors().size(), is(1));
      assertThat(e.getValidationErrors().get(0).getMessage(), is("Path is not allowed to have '.' or '..' segments: 'foo/foo/../../foo.jar'"));
    }
  }

  @Test
  public void testHandle_normalizePath() throws IOException {
    testNormalizePath("/", "goo.jar", "goo.jar");
    testNormalizePath("/foo", "goo.jar", "foo/goo.jar");
    testNormalizePath("/foo", "/goo.jar", "foo/goo.jar");
    testNormalizePath("/foo/", "goo.jar", "foo/goo.jar");
    testNormalizePath("/foo/", "/goo.jar", "foo/goo.jar");
    testNormalizePath("foo/", "goo.jar", "foo/goo.jar");
    testNormalizePath("foo/", "/goo.jar", "foo/goo.jar");
    testNormalizePath("foo", "goo.jar", "foo/goo.jar");
    testNormalizePath("foo", "/goo.jar", "foo/goo.jar");
    testNormalizePath("//foo//", "//goo.jar", "foo/goo.jar");
    testNormalizePath("//////foo///////", "//////goo.jar//////", "foo/goo.jar");
    testNormalizePath("  foo  ", "  goo.jar  ", "foo/goo.jar");

    testNormalizePath("foo", "bar/goo.jar", "foo/bar/goo.jar");
    testNormalizePath("foo/bar", "car/goo.jar", "foo/bar/car/goo.jar");
  }

  private void testNormalizePath(String directory, String file, String expectedPath) throws IOException {
    reset(rawFacet);
    ComponentUpload component = new ComponentUpload();

    component.getFields().put("directory", directory);

    AssetUpload asset = new AssetUpload();
    asset.getFields().put("filename", file);
    asset.setPayload(jarPayload);
    component.getAssetUploads().add(asset);

    when(assetPayload.componentId()).thenReturn(new DetachedEntityId("foo"));
    when(attributesMap.get(Asset.class)).thenReturn(assetPayload);
    when(content.getAttributes()).thenReturn(attributesMap);
    when(rawFacet.put(any(), any())).thenReturn(content);
    underTest.handle(repository, component);

    ArgumentCaptor<String> pathCapture = ArgumentCaptor.forClass(String.class);
    verify(rawFacet).put(pathCapture.capture(), any(PartPayload.class));

    String path = pathCapture.getValue();
    assertNotNull(path);
    assertThat(path, is(expectedPath));
  }

  private UploadFieldDefinition field(final String name,
                                      final String displayName,
                                      final String helpText,
                                      final boolean optional,
                                      final Type type,
                                      final String group)
  {
    return new UploadFieldDefinition(name, displayName, helpText, optional, type, group);
  }

  private Set<UploadDefinitionExtension> getDefinitionExtensions() {
    return singleton(new TestUploadDefinitionExtension());
  }

  private class TestUploadDefinitionExtension implements UploadDefinitionExtension {

    @Override
    public UploadFieldDefinition contribute() {
      return new UploadFieldDefinition("foo", "Foo", null, true, Type.STRING, "bar");
    }
  }
}
