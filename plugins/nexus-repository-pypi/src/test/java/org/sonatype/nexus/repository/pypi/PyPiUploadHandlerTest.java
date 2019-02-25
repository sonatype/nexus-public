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
package org.sonatype.nexus.repository.pypi;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.pypi.internal.PyPiAttributes;
import org.sonatype.nexus.repository.pypi.internal.PyPiDataUtils;
import org.sonatype.nexus.repository.pypi.internal.PyPiFormat;
import org.sonatype.nexus.repository.pypi.internal.PyPiHostedFacet;
import org.sonatype.nexus.repository.pypi.internal.PyPiHostedFacetImpl;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.internal.SimpleVariableResolverAdapter;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.selector.VariableSource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type.STRING;

public class PyPiUploadHandlerTest
    extends TestSupport
{
  private static final String REPO_NAME = "pypi-hosted";

  private PyPiUploadHandler underTest;

  @Mock
  private Repository repository;

  @Mock
  private PyPiHostedFacetImpl pyPiFacet;

  @Mock
  private PartPayload payload;

  @Mock
  private StorageTx storageTx;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private ContentPermissionChecker contentPermissionChecker;

  @Captor
  private ArgumentCaptor<VariableSource> captor;

  private Map<String, String> metadata;

  @Before
  public void setup() throws IOException {
    when(contentPermissionChecker.isPermitted(eq(REPO_NAME), eq(PyPiFormat.NAME), eq(BreadActions.EDIT), any()))
        .thenReturn(true);

    underTest = new PyPiUploadHandler(contentPermissionChecker, new SimpleVariableResolverAdapter(), emptySet());
    when(repository.facet(PyPiHostedFacet.class)).thenReturn(pyPiFacet);
    when(repository.getName()).thenReturn(REPO_NAME);

    metadata = new HashMap<>();
    metadata.put("metadata_version", "1.1");
    metadata.put(PyPiAttributes.P_NAME, "sample");
    metadata.put(PyPiAttributes.P_VERSION, "1.2.0");
    metadata.put(PyPiAttributes.P_SUMMARY, "1.2.0");
    metadata.put(PyPiAttributes.P_HOME_PAGE, "https://github.com/pypa/sampleproject");
    metadata.put(PyPiAttributes.P_AUTHOR, "The Python Packaging Authority");
    metadata.put(PyPiAttributes.P_AUTHOR_EMAIL, "pypa-dev@googlegroups.com");
    metadata.put(PyPiAttributes.P_LICENSE, "MIT");
    metadata.put(PyPiAttributes.P_DESCRIPTION, "A sample Python project");
    metadata.put(PyPiAttributes.P_KEYWORDS, "sample setuptools development");
    metadata.put(PyPiAttributes.P_PLATFORM, "UNKNOWN");
    metadata.put(PyPiAttributes.P_CLASSIFIERS, "Development Status :: 3 - Alpha\nIntended Audience :: Developers");
    metadata.put(PyPiAttributes.P_ARCHIVE_TYPE, "zip");
    when(pyPiFacet.extractMetadata(any())).thenReturn(metadata);

    when(pyPiFacet.createPackagePath(any(), any(), any())).thenCallRealMethod();

    TempBlob tempBlob = mock(TempBlob.class);
    when(tempBlob.get()).thenAnswer(invocation -> PyPiUploadHandlerTest.class.getResourceAsStream("internal/sample-1.2.0-py2.7.egg"));
    when(storageFacet.createTempBlob(payload, PyPiDataUtils.HASH_ALGORITHMS)).thenReturn(tempBlob);
    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);

    when(payload.getName()).thenReturn("sample-1.2.0-py2.7.egg");

    Asset asset = mock(Asset.class);
    when(asset.componentId()).thenReturn(new DetachedEntityId("nuId"));
    when(asset.name()).thenReturn("packages/sample/sample-1.2.0-py2.7.egg");

    when(pyPiFacet.upload(any(), any(), any())).thenReturn(asset);
  }

  @Test
  public void testGetDefinition() {
    UploadDefinition def = underTest.getDefinition();

    assertThat(def.getFormat(), is(PyPiFormat.NAME));
    assertThat(def.isMultipleUpload(), is(false));
    assertThat(def.getComponentFields(), empty());
    assertThat(def.getAssetFields(), empty());
  }

  @Test
  public void testGetDefinitionWithExtensionContributions() {
    //Rebuilding uploadhandler to provide a set of upload definition extensions
    underTest = new PyPiUploadHandler(contentPermissionChecker, new SimpleVariableResolverAdapter(), getDefinitionExtensions());
    UploadDefinition def = underTest.getDefinition();

    assertThat(def.getComponentFields(),
        contains(new UploadFieldDefinition("foo", "Foo", null, true, STRING, "bar")));
    assertThat(def.getAssetFields(), empty());
  }

  @Test
  public void testHandle() throws IOException {
    ComponentUpload component = new ComponentUpload();
    AssetUpload asset = new AssetUpload();
    asset.setPayload(payload);
    component.getAssetUploads().add(asset);

    UploadResponse uploadResponse = underTest.handle(repository, component);
    assertThat(uploadResponse.getAssetPaths(), contains("packages/sample/sample-1.2.0-py2.7.egg"));
    assertThat(uploadResponse.getComponentId().getValue(), is("nuId"));

    verify(contentPermissionChecker).isPermitted(eq(REPO_NAME), eq(PyPiFormat.NAME), eq(BreadActions.EDIT),
        captor.capture());

    VariableSource source = captor.getValue();

    assertThat(source.get("format"), is(Optional.of(PyPiFormat.NAME)));
    assertThat(source.get("path"), is(Optional.of("packages/sample/1.2.0/sample-1.2.0-py2.7.egg")));
    assertThat(source.get("coordinate.name"), is(Optional.of("sample")));
    assertThat(source.get("coordinate.version"), is(Optional.of("1.2.0")));

  }

  @Test
  public void testHandle_unauthorized() throws IOException {
    when(contentPermissionChecker.isPermitted(eq(REPO_NAME), eq(PyPiFormat.NAME), eq(BreadActions.EDIT), any()))
        .thenReturn(false);

    ComponentUpload component = new ComponentUpload();
    AssetUpload asset = new AssetUpload();
    asset.setPayload(payload);
    component.getAssetUploads().add(asset);

    try {
      underTest.handle(repository, component);
      fail("Expected validation exception");
    }
    catch (ValidationErrorsException e) {
      assertThat(e.getValidationErrors().size(), is(1));
      assertThat(e.getValidationErrors().get(0).getMessage(), is("Not authorized for requested path 'packages/sample/1.2.0/sample-1.2.0-py2.7.egg'"));
    }
  }

  @Test
  public void testHandle_dot() throws IOException {
    metadata.put(PyPiAttributes.P_NAME, "./foo/../bar");
    metadata.put(PyPiAttributes.P_VERSION, "./foo/../bar");
    metadata.put(PyPiAttributes.P_ARCHIVE_TYPE, "zip");

    ComponentUpload component = new ComponentUpload();
    AssetUpload asset = new AssetUpload();
    asset.setPayload(payload);
    component.getAssetUploads().add(asset);

    try {
      underTest.handle(repository, component);
      fail("Expected validation exception");
    }
    catch (ValidationErrorsException e) {
      assertThat(e.getValidationErrors().size(), is(1));
      assertThat(e.getValidationErrors().get(0).getMessage(), is("Path is not allowed to have '.' or '..' segments: 'packages/-/foo/-/bar/./foo/../bar/sample-1.2.0-py2.7.egg'"));
    }
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
