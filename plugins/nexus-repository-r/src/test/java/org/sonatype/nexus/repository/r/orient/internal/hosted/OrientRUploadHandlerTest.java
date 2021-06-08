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
package org.sonatype.nexus.repository.r.orient.internal.hosted;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.r.orient.OrientRHostedFacet;
import org.sonatype.nexus.repository.r.RFormat;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.r.internal.util.PackageValidator.NOT_VALID_EXTENSION_ERROR_MESSAGE;
import static org.sonatype.nexus.repository.r.internal.util.PackageValidator.NOT_VALID_PATH_ERROR_MESSAGE;
import static org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type.STRING;

public class OrientRUploadHandlerTest
    extends TestSupport
{
  private static final String REPO_NAME = "r-hosted";

  private OrientRUploadHandler underTest;

  @Mock
  private Repository repository;

  @Mock
  private OrientRHostedFacetImpl rHostedFacet;

  @Mock
  private StorageTx storageTx;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private PartPayload payload;

  @Mock
  private ContentPermissionChecker contentPermissionChecker;

  @Captor
  private ArgumentCaptor<VariableSource> captor;

  private static final String PACKAGE_PATH = "bin/macosx/el-capitan/contrib";

  private static final String PACKAGE_NAME = "r-package.tar.gz";

  private static final String WRONG_PACKAGE_PATH = "";

  private static final String WRONG_PACKAGE_NAME = "r-package.xxx";

  private final String PACKAGE_PATH_FULL = String.format("%s/%s", PACKAGE_PATH, PACKAGE_NAME);

  private static final String NU_ID = "nuId";

  @Before
  public void setup() throws IOException {
    when(contentPermissionChecker.isPermitted(eq(REPO_NAME), eq(RFormat.NAME), eq(BreadActions.EDIT), any()))
        .thenReturn(true);

    underTest = new OrientRUploadHandler(new SimpleVariableResolverAdapter(), contentPermissionChecker, emptySet());

    when(repository.getName()).thenReturn(REPO_NAME);
    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(repository.facet(OrientRHostedFacet.class)).thenReturn(rHostedFacet);

    Asset asset = mockAsset(PACKAGE_PATH_FULL);
    when(rHostedFacet.upload(PACKAGE_PATH_FULL, payload)).thenReturn(asset);
  }

  @Test
  public void testGetDefinition() {
    UploadDefinition def = underTest.getDefinition();
    assertThat(def.getFormat(), is(RFormat.NAME));
    assertThat(def.isMultipleUpload(), is(false));
    assertThat(def.getAssetFields(), contains(field("pathId", "Package Path", null, false, STRING, null)));
    assertThat(def.getAssetFields().size(), is(1));
    assertThat(def.getComponentFields().size(), is(0));
  }

  @Test
  public void testHandle() throws IOException {
    ComponentUpload component = createComponentUpload(PACKAGE_PATH, PACKAGE_NAME);

    UploadResponse uploadResponse = underTest.handle(repository, component);
    assertThat(uploadResponse.getAssetPaths(), contains(PACKAGE_PATH_FULL));
    assertThat(uploadResponse.getComponentId().getValue(), is(NU_ID));

    verify(contentPermissionChecker).isPermitted(eq(REPO_NAME), eq(RFormat.NAME), eq(BreadActions.EDIT),
        captor.capture());

    VariableSource source = captor.getValue();

    assertThat(source.get("format"), is(Optional.of(RFormat.NAME)));
    assertThat(source.get("path"), is(Optional.of(PACKAGE_PATH_FULL)));
  }

  @Test
  public void testHandle_unauthorized() throws IOException {
    when(contentPermissionChecker.isPermitted(eq(REPO_NAME), eq(RFormat.NAME), eq(BreadActions.EDIT), any()))
        .thenReturn(false);

    ComponentUpload component = createComponentUpload(PACKAGE_PATH, PACKAGE_NAME);
    checkValidationFailed(component, String.format("Not authorized for requested path '%s'", PACKAGE_PATH_FULL));
  }

  @Test
  public void testHandleValidationExceptionWrongPath() throws IOException {
    ComponentUpload component = createComponentUpload(WRONG_PACKAGE_PATH, PACKAGE_NAME);
    checkValidationFailed(component, NOT_VALID_PATH_ERROR_MESSAGE);
  }

  @Test
  public void testHandleValidationExceptionWrongExtension() throws IOException {
    ComponentUpload component = createComponentUpload(PACKAGE_PATH, WRONG_PACKAGE_NAME);
    checkValidationFailed(component, NOT_VALID_EXTENSION_ERROR_MESSAGE);
  }

  private void checkValidationFailed(final ComponentUpload component, final String expectedErrorMessage)
      throws IOException
  {
    try {
      underTest.handle(repository, component);
      fail("Expected validation exception");
    }
    catch (ValidationErrorsException e) {
      assertThat(e.getValidationErrors().size(), is(1));
      assertThat(e.getValidationErrors().get(0).getMessage(),
          is(expectedErrorMessage));
    }
  }

  private ComponentUpload createComponentUpload(final String packagePath, final String packageName) {
    when(payload.getName()).thenReturn(packageName);
    ComponentUpload component = new ComponentUpload();
    AssetUpload asset = new AssetUpload();
    asset.setFields(Collections.singletonMap(OrientRUploadHandler.PATH_ID, packagePath));
    asset.setPayload(payload);
    component.getAssetUploads().add(asset);
    return component;
  }

  private static UploadFieldDefinition field(final String name,
                                             final String displayName,
                                             final String helpText,
                                             final boolean optional,
                                             final Type type,
                                             final String group)
  {
    return new UploadFieldDefinition(name, displayName, helpText, optional, type, group);
  }

  private static Asset mockAsset(final String name) {
    Asset asset = mock(Asset.class);
    when(asset.componentId()).thenReturn(new DetachedEntityId(NU_ID));
    when(asset.name()).thenReturn(name);
    return asset;
  }
}
