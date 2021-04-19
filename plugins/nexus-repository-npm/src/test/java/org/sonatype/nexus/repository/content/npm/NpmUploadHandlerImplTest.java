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
package org.sonatype.nexus.repository.content.npm;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.goodies.testsupport.junit.TestDataRule;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.fluent.FluentBlobs;
import org.sonatype.nexus.repository.npm.internal.NpmFormat;
import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.selector.VariableSource;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.content.npm.NpmUploadHandlerImpl.HASH_ALGORITHMS;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.P_NAME;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.P_VERSION;
import static org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type.BOOLEAN;
import static org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type.STRING;

public class NpmUploadHandlerImplTest
    extends TestSupport
{
  private static final String REPO_NAME = "npm-hosted";

  @Rule
  public TestDataRule testData = new TestDataRule(util.resolveFile("target/test-classes"));

  @Mock
  private Repository repository;

  @Mock
  private ContentPermissionChecker contentPermissionChecker;

  @Mock
  private PartPayload payload;

  @Mock
  private INpmHostedFacet npmHostedFacet;

  @Mock
  private NpmContentFacet contentFacet;

  @Mock
  private NpmPackageParser npmPackageParser;

  @Mock
  private VariableResolverAdapter variableResolverAdapter;

  private NpmUploadHandlerImpl underTest;

  @Before
  public void setup() throws IOException {
    when(payload.getName()).thenReturn("test-npm.tgz");

    TempBlob tempBlob = mock(TempBlob.class);

    FluentBlobs fluentBlobs = mock(FluentBlobs.class);
    when(fluentBlobs.ingest(payload, HASH_ALGORITHMS)).thenReturn(tempBlob);

    Map<String, Object> packageJson = new HashMap<>();
    packageJson.put(P_NAME, "pName");
    packageJson.put(P_VERSION, "1.0.0");
    when(npmPackageParser.parsePackageJson(tempBlob)).thenReturn(packageJson);

    Content content = mock(Content.class);
    when(npmHostedFacet.putPackage(packageJson, tempBlob)).thenReturn(content);
    AttributesMap attributesMap = mock(AttributesMap.class);
    Asset asset = mock(Asset.class);
    when(attributesMap.get(Asset.class)).thenReturn(asset);
    when(content.getAttributes()).thenReturn(attributesMap);

    when(contentFacet.blobs()).thenReturn(fluentBlobs);

    when(repository.getName()).thenReturn(REPO_NAME);
    when(repository.facet(NpmContentFacet.class)).thenReturn(contentFacet);
    when(repository.facet(INpmHostedFacet.class)).thenReturn(npmHostedFacet);

    when(contentPermissionChecker.isPermitted(eq(REPO_NAME), eq(NpmFormat.NAME), eq(BreadActions.EDIT), any()))
        .thenReturn(true);

    underTest = new NpmUploadHandlerImpl(contentPermissionChecker, variableResolverAdapter, npmPackageParser,
        emptySet());
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
    UploadFieldDefinition uploadDefinitionA = new UploadFieldDefinition("nameA", "helpTextA", false, STRING, "groupA");
    UploadDefinitionExtension uploadDefinitionExtensionA = mock(UploadDefinitionExtension.class);
    when(uploadDefinitionExtensionA.contribute()).thenReturn(uploadDefinitionA);

    UploadFieldDefinition uploadDefinitionB = new UploadFieldDefinition("nameB", true, BOOLEAN);
    UploadDefinitionExtension uploadDefinitionExtensionB = mock(UploadDefinitionExtension.class);
    when(uploadDefinitionExtensionB.contribute()).thenReturn(uploadDefinitionB);

    underTest = new NpmUploadHandlerImpl(contentPermissionChecker, variableResolverAdapter,
        npmPackageParser, Sets.newHashSet(uploadDefinitionExtensionA, uploadDefinitionExtensionB));
    UploadDefinition def = underTest.getDefinition();

    assertThat(def.getComponentFields(),
        containsInAnyOrder(
            equalTo(new UploadFieldDefinition("nameA", "NameA", "helpTextA", false, STRING, "groupA")),
            equalTo(new UploadFieldDefinition("nameB", "NameB", null, true, BOOLEAN, null))));
    assertThat(def.getAssetFields(), empty());
  }

  @Test
  public void testHandle_unauthorized() {
    when(contentPermissionChecker.isPermitted(eq(REPO_NAME), eq(NpmFormat.NAME), eq(BreadActions.EDIT), any()))
        .thenReturn(false);

    ComponentUpload component = new ComponentUpload();
    AssetUpload asset = new AssetUpload();
    asset.setPayload(payload);
    component.getAssetUploads().add(asset);

    assertThrows("Not authorized for requested path 'pName/-/pName-1.0.0.tgz'", ValidationErrorsException.class,
        () -> underTest.handle(repository, component));
  }

  @Test
  public void testWrongExtension()
  {
    when(payload.getName()).thenReturn("test-wrong-npm.node");

    ComponentUpload component = new ComponentUpload();
    AssetUpload assetUpload = new AssetUpload();
    assetUpload.setPayload(payload);
    component.getAssetUploads().add(assetUpload);
    assertThrows("Unsupported extension. Extension must be .tgz", IllegalArgumentException.class, () -> underTest.handle(repository, component));
  }
}
