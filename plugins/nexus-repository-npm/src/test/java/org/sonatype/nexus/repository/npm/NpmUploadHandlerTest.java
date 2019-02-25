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
package org.sonatype.nexus.repository.npm;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.repository.npm.internal.ArchiveUtils;
import org.sonatype.nexus.repository.npm.internal.NpmFormat;
import org.sonatype.nexus.repository.npm.internal.NpmHostedFacet;
import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.Repository;
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
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.selector.VariableSource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type.STRING;

public class NpmUploadHandlerTest
    extends TestSupport
{
  private final String REPO_NAME = "npm-hosted";

  private NpmUploadHandler underTest;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Mock
  Repository repository;

  @Mock
  NpmHostedFacet npmFacet;

  @Mock
  PartPayload payload;

  @Mock
  StorageTx storageTx;

  @Mock
  StorageFacet storageFacet;

  @Mock
  private ContentPermissionChecker contentPermissionChecker;

  @Captor
  private ArgumentCaptor<VariableSource> captor;

  private File packageJson;

  @Before
  public void setup() throws IOException, URISyntaxException {
    when(contentPermissionChecker.isPermitted(eq(REPO_NAME), eq(NpmFormat.NAME), eq(BreadActions.EDIT), any()))
        .thenReturn(true);

    underTest = new NpmUploadHandler(contentPermissionChecker, new SimpleVariableResolverAdapter(),
        new NpmPackageParser(), emptySet());
    when(repository.facet(NpmHostedFacet.class)).thenReturn(npmFacet);

    packageJson = new File(NpmUploadHandlerTest.class.getResource("internal/package.json").toURI());
    when(storageFacet.createTempBlob(any(PartPayload.class), any())).thenAnswer(invocation -> {
      TempBlob blob = mock(TempBlob.class);
      when(blob.get()).thenAnswer(i -> ArchiveUtils.pack(tempFolder.newFile(), packageJson, "package/package.json"));
      return blob;
    });

    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(repository.getName()).thenReturn(REPO_NAME);

    Asset asset = mock(Asset.class);
    when(asset.componentId()).thenReturn(new DetachedEntityId("nuId"));
    when(asset.name()).thenReturn("@foo/bar/-/bar/bar-123.gz");
    when(npmFacet.putPackage(any(), any())).thenReturn(asset);
  }

  @Test
  public void testGetDefinition() {
    UploadDefinition def = underTest.getDefinition();

    assertThat(def.isMultipleUpload(), is(false));
    assertThat(def.getComponentFields(), empty());
    assertThat(def.getAssetFields(), empty());
  }

  @Test
  public void testGetDefinitionWithExtensionContributions() {
    //Rebuilding the uploadhandler to provide a set of definition extensions
    underTest = new NpmUploadHandler(contentPermissionChecker, new SimpleVariableResolverAdapter(),
        new NpmPackageParser(), getDefinitionExtensions());
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
    assertThat(uploadResponse.getAssetPaths(), contains("@foo/bar/-/bar/bar-123.gz"));
    assertThat(uploadResponse.getComponentId().getValue(), is("nuId"));

    verify(contentPermissionChecker).isPermitted(eq(REPO_NAME), eq(NpmFormat.NAME), eq(BreadActions.EDIT),
        captor.capture());

    VariableSource source = captor.getValue();

    assertThat(source.getVariableSet(), hasSize(5));
    assertThat(source.get("format"), is(Optional.of(NpmFormat.NAME)));
    assertThat(source.get("path"), is(Optional.of("@foo/bar/-/bar-1.5.3.tgz")));
    assertThat(source.get("coordinate.packageScope"), is(Optional.of("foo")));
    assertThat(source.get("coordinate.packageName"), is(Optional.of("bar")));
    assertThat(source.get("coordinate.version"), is(Optional.of("1.5.3")));
  }

  @Test
  public void testHandle_unauthorized() throws IOException {
    when(contentPermissionChecker.isPermitted(eq(REPO_NAME), eq(NpmFormat.NAME), eq(BreadActions.EDIT), any()))
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
      assertThat(e.getValidationErrors().get(0).getMessage(), is("Not authorized for requested path '@foo/bar/-/bar-1.5.3.tgz'"));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHandlePathWithDotInScope() throws IOException, URISyntaxException {
    handlePath("package-path-with-dot-in-scope.json");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHandlePathWithDotInName() throws IOException, URISyntaxException {
    handlePath("package-path-with-dot-in-name.json");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHandlePathWithUnderscoreInScope() throws IOException, URISyntaxException {
    handlePath("package-path-with-underscore-in-scope.json");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHandlePathWithUnderscoreInName() throws IOException, URISyntaxException {
    handlePath("package-path-with-underscore-in-name.json");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testHandlePathWithDots() throws IOException, URISyntaxException {
    handlePath("package-path-with-dots.json");
  }

  private void handlePath(String jsonFile) throws IOException, URISyntaxException {
    packageJson = new File(NpmUploadHandlerTest.class.getResource("internal/" + jsonFile).toURI());

    ComponentUpload component = new ComponentUpload();
    AssetUpload assetUpload = new AssetUpload();
    assetUpload.setPayload(payload);
    component.getAssetUploads().add(assetUpload);
    underTest.handle(repository, component);
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
