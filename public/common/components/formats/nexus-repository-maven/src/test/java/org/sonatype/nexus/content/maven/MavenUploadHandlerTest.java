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
package org.sonatype.nexus.content.maven;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.content.maven.internal.MavenVariableResolverAdapter;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.FluentBlobs;
import org.sonatype.nexus.repository.importtask.ImportStreamConfiguration;
import org.sonatype.nexus.repository.maven.LayoutPolicy;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenMetadataRebuildFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.VersionPolicy;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.Maven2MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.MavenPomGenerator;
import org.sonatype.nexus.repository.maven.internal.VersionPolicyValidator;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type;
import org.sonatype.nexus.repository.upload.UploadRegexMap;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.selector.VariableSource;
import org.sonatype.nexus.mime.MimeSupport;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type.BOOLEAN;
import static org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type.STRING;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MavenUploadHandlerTest

{
  private static final String GROUP_NAME_COORDINATES = "Component coordinates";

  private static final String REPO_NAME = "maven-hosted";

  private MavenUploadHandler underTest;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  Repository repository;

  @Mock
  MavenContentFacet mavenFacet;

  @Mock
  PartPayload jarPayload;

  @Mock
  PartPayload sourcesPayload;

  @Mock
  VersionPolicyValidator versionPolicyValidator;

  @Mock
  TempBlob tempBlob;

  @Mock
  MavenMetadataRebuildFacet mavenMetadataRebuildFacet;

  @Mock
  private ContentPermissionChecker contentPermissionChecker;

  @Mock
  private MavenPomGenerator mavenPomGenerator;

  @Mock
  private MimeSupport mimeSupport;

  @Captor
  private ArgumentCaptor<VariableSource> captor;

  @Before
  public void setup() throws IOException {
    when(versionPolicyValidator.validArtifactPath(any(), any())).thenReturn(true);
    when(contentPermissionChecker.isPermitted(eq(REPO_NAME), eq(Maven2Format.NAME), eq(BreadActions.EDIT), any()))
        .thenReturn(true);

    when(mavenPomGenerator.generatePom(any(), any(), any(), any())).thenReturn("<project/>");

    // Setup MimeSupport mock to return application/octet-stream for all extensions by default
    when(mimeSupport.guessMimeTypeFromPath(any())).thenReturn("application/octet-stream");

    Maven2MavenPathParser pathParser = new Maven2MavenPathParser();
    underTest = new MavenUploadHandler(pathParser, new MavenVariableResolverAdapter(pathParser),
        contentPermissionChecker, versionPolicyValidator, mavenPomGenerator, emptySet(), mimeSupport);

    when(repository.getName()).thenReturn(REPO_NAME);
    when(repository.getFormat()).thenReturn(new Maven2Format());
    when(repository.facet(MavenContentFacet.class)).thenReturn(mavenFacet);
    when(repository.facet(MavenFacet.class)).thenReturn(mavenFacet);
    when(repository.facet(MavenMetadataRebuildFacet.class)).thenReturn(mavenMetadataRebuildFacet);

    FluentBlobs blobs = mock(FluentBlobs.class);
    when(mavenFacet.blobs()).thenReturn(blobs);
    when(blobs.ingest(any(Payload.class), any())).thenReturn(tempBlob);

    when(mavenFacet.getVersionPolicy()).thenReturn(VersionPolicy.RELEASE);
    when(mavenFacet.layoutPolicy()).thenReturn(LayoutPolicy.STRICT);

    Content content = mock(Content.class);
    AttributesMap attributesMap = mock(AttributesMap.class);
    Asset assetPayload = mock(Asset.class);
    when(attributesMap.get(Asset.class)).thenReturn(assetPayload);
    when(attributesMap.require(eq(Content.CONTENT_LAST_MODIFIED), eq(DateTime.class))).thenReturn(DateTime.now());
    AssetBlob blob = mock(AssetBlob.class);
    when(assetPayload.blob()).thenReturn(Optional.of(blob));
    Map<String, String> checksums = Collections.singletonMap(
        HashAlgorithm.SHA1.name(),
        "da39a3ee5e6b4b0d3255bfef95601890afd80709");
    when(blob.checksums()).thenReturn(checksums);
    when(content.getAttributes()).thenReturn(attributesMap);
    when(mavenFacet.put(any(), any())).thenReturn(content);
  }

  @Test
  public void testGetDefinition() {
    UploadDefinition def = underTest.getDefinition();

    assertThat(def.isMultipleUpload(), is(true));
    // Order is important on fields as it affects the UI
    assertThat(def.getComponentFields(), contains(
        field("groupId", "Group ID", null, false, STRING, GROUP_NAME_COORDINATES),
        field("artifactId", "Artifact ID", null, false, STRING, GROUP_NAME_COORDINATES),
        field("version", "Version", null, false, STRING, GROUP_NAME_COORDINATES),
        field("generate-pom", "Generate a POM file with these coordinates", null, true, BOOLEAN,
            GROUP_NAME_COORDINATES),
        field("packaging", "Packaging", null, true, STRING, GROUP_NAME_COORDINATES)));
    assertThat(def.getAssetFields(), contains(
        field("classifier", "Classifier", null, true, STRING, null),
        field("extension", "Extension", null, false, STRING, null)));
  }

  @Test
  public void testGetDefinitionWithExtensionContributions() {
    // Rebuilding the uploadhandler to provide a set of definition extensions
    Maven2MavenPathParser pathParser = new Maven2MavenPathParser();
    underTest = new MavenUploadHandler(pathParser, new MavenVariableResolverAdapter(pathParser),
        contentPermissionChecker, versionPolicyValidator, mavenPomGenerator, getDefinitionExtensions(), mimeSupport);
    UploadDefinition def = underTest.getDefinition();

    assertThat(def.isMultipleUpload(), is(true));
    // Order is important on fields as it affects the UI
    assertThat(def.getComponentFields(), contains(
        field("groupId", "Group ID", null, false, STRING, GROUP_NAME_COORDINATES),
        field("artifactId", "Artifact ID", null, false, STRING, GROUP_NAME_COORDINATES),
        field("version", "Version", null, false, STRING, GROUP_NAME_COORDINATES),
        field("generate-pom", "Generate a POM file with these coordinates", null, true, BOOLEAN,
            GROUP_NAME_COORDINATES),
        field("packaging", "Packaging", null, true, STRING, GROUP_NAME_COORDINATES),
        field("foo", "Foo", null, true, STRING, "bar")));

    assertThat(def.getAssetFields(), contains(
        field("classifier", "Classifier", null, true, STRING, null),
        field("extension", "Extension", null, false, STRING, null)));
  }

  @Test
  public void testGetDefinition_regex() {
    UploadRegexMap regexMap = underTest.getDefinition().getRegexMap();
    assertNotNull(regexMap);
    assertNotNull(regexMap.getRegex());
    assertThat(regexMap.getFieldList(), contains("classifier", "extension"));
  }

  @Test
  public void testHandle() throws IOException {
    ComponentUpload componentUpload = new ComponentUpload();

    componentUpload.getFields().put("groupId", "org.apache.maven");
    componentUpload.getFields().put("artifactId", "tomcat");
    componentUpload.getFields().put("version", "5.0.28");

    AssetUpload assetUpload = new AssetUpload();
    assetUpload.getFields().put("extension", "jar");
    assetUpload.setPayload(jarPayload);
    componentUpload.getAssetUploads().add(assetUpload);

    assetUpload = new AssetUpload();
    assetUpload.getFields().put("classifier", "sources");
    assetUpload.getFields().put("extension", "jar");
    assetUpload.setPayload(sourcesPayload);
    componentUpload.getAssetUploads().add(assetUpload);

    UploadResponse uploadResponse = underTest.handle(repository, componentUpload);
    assertThat(uploadResponse.getAssetPaths(), contains("/org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar",
        "/org/apache/maven/tomcat/5.0.28/tomcat-5.0.28-sources.jar"));

    ArgumentCaptor<MavenPath> pathCapture = ArgumentCaptor.forClass(MavenPath.class);
    verify(mavenFacet, times(4)).put(pathCapture.capture(), any(Payload.class));

    List<MavenPath> paths = pathCapture.getAllValues();

    assertThat(paths, hasSize(4));

    MavenPath path = paths.get(0);
    assertNotNull(path);
    assertThat(path.getPath(), is("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar"));
    assertCoordinates(path.getCoordinates(), "org.apache.maven", "tomcat", "5.0.28", null, "jar");

    path = paths.get(1);
    assertNotNull(path);
    assertThat(path.getPath(), is("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar.sha1"));
    assertCoordinates(path.getCoordinates(), "org.apache.maven", "tomcat", "5.0.28", null, "jar.sha1");

    path = paths.get(2);
    assertNotNull(path);
    assertThat(path.getPath(), is("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28-sources.jar"));
    assertCoordinates(path.getCoordinates(), "org.apache.maven", "tomcat", "5.0.28", "sources", "jar");

    path = paths.get(3);
    assertNotNull(path);
    assertThat(path.getPath(), is("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28-sources.jar.sha1"));
    assertCoordinates(path.getCoordinates(), "org.apache.maven", "tomcat", "5.0.28", "sources", "jar.sha1");

    verify(contentPermissionChecker, times(2)).isPermitted(eq(REPO_NAME), eq(Maven2Format.NAME), eq(BreadActions.EDIT),
        captor.capture());

    List<VariableSource> sources = captor.getAllValues();

    assertVariableSource(sources.get(0), "/org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar", "org.apache.maven",
        "tomcat", "5.0.28", null, "jar");
    assertVariableSource(sources.get(1), "/org/apache/maven/tomcat/5.0.28/tomcat-5.0.28-sources.jar",
        "org.apache.maven", "tomcat", "5.0.28", "sources", "jar");

    verify(mavenMetadataRebuildFacet).rebuildMetadata("org.apache.maven", "tomcat", "5.0.28", false, false);
  }

  @Test
  public void testHandle_generatePom() throws IOException {
    ComponentUpload componentUpload = new ComponentUpload();

    componentUpload.getFields().put("groupId", "org.apache.maven");
    componentUpload.getFields().put("artifactId", "tomcat");
    componentUpload.getFields().put("version", "5.0.28");
    componentUpload.getFields().put("generate-pom", "true");

    AssetUpload assetUpload = new AssetUpload();
    assetUpload.getFields().put("extension", "jar");
    assetUpload.setPayload(jarPayload);
    componentUpload.getAssetUploads().add(assetUpload);

    UploadResponse uploadResponse = underTest.handle(repository, componentUpload);
    assertThat(uploadResponse.getAssetPaths(), contains("/org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar",
        "/org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.pom"));

    ArgumentCaptor<MavenPath> paths = ArgumentCaptor.forClass(MavenPath.class);
    verify(mavenFacet, times(4)).put(paths.capture(), any());

    MavenPath pomPath = paths.getAllValues().get(2);
    assertTrue(pomPath.isPom());
    assertThat(pomPath, is(new MavenPath("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.pom", null)));

    MavenPath path = paths.getAllValues().get(3);
    assertFalse(path.isPom());
    assertThat(path, is(new MavenPath("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.pom.sha1", null)));

    verify(mavenPomGenerator).generatePom("org.apache.maven", "tomcat", "5.0.28", null);

    // verify packaging is passed when set
    componentUpload.getFields().put("packaging", "eclipse-plugin");
    underTest.handle(repository, componentUpload);
    verify(mavenPomGenerator).generatePom("org.apache.maven", "tomcat", "5.0.28", "eclipse-plugin");
  }

  @Test
  public void testHandle_unauthorized() throws IOException {
    when(contentPermissionChecker.isPermitted(eq(REPO_NAME), eq(Maven2Format.NAME), eq(BreadActions.EDIT), any()))
        .thenReturn(false);

    ComponentUpload componentUpload = new ComponentUpload();

    componentUpload.getFields().put("groupId", "org.apache.maven");
    componentUpload.getFields().put("artifactId", "tomcat");
    componentUpload.getFields().put("version", "5.0.28");

    AssetUpload assetUpload = new AssetUpload();
    assetUpload.getFields().put("extension", "jar");
    assetUpload.setPayload(jarPayload);
    componentUpload.getAssetUploads().add(assetUpload);

    try {
      underTest.handle(repository, componentUpload);
      fail("Expected validation exception");
    }
    catch (ValidationErrorsException e) {
      assertThat(e.getValidationErrors().size(), is(1));
      assertThat(e.getValidationErrors().get(0).getMessage(),
          is("Not authorized for requested path '/org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar'"));
    }
  }

  @Test
  public void testHandle_snapshotToRelease() throws Exception {
    when(mavenFacet.getVersionPolicy()).thenReturn(VersionPolicy.RELEASE);
    when(versionPolicyValidator.validArtifactPath(any(), any())).thenReturn(false);

    ComponentUpload componentUpload = new ComponentUpload();

    componentUpload.getFields().put("groupId", "org.apache.maven");
    componentUpload.getFields().put("artifactId", "tomcat");
    componentUpload.getFields().put("version", "5.0.28-SNAPSHOT");

    AssetUpload assetUpload = new AssetUpload();
    assetUpload.getFields().put("extension", "jar");
    assetUpload.setPayload(jarPayload);
    componentUpload.getAssetUploads().add(assetUpload);

    try {
      underTest.handle(repository, componentUpload);
      fail("Expected version policy mismatch exception");
    }
    catch (ValidationErrorsException e) {
      assertThat(e.getValidationErrors().size(), is(1));
      assertThat(e.getValidationErrors().get(0).getMessage(),
          is("Version policy mismatch, cannot upload SNAPSHOT content to RELEASE repositories for file 'org/apache/maven/tomcat/5.0.28-SNAPSHOT/tomcat-5.0.28-SNAPSHOT.jar'"));
    }
  }

  @Test
  public void testHandle_snapshot_not_supported() throws Exception {
    when(mavenFacet.getVersionPolicy()).thenReturn(VersionPolicy.SNAPSHOT);
    when(versionPolicyValidator.validArtifactPath(any(), any())).thenReturn(false);

    ComponentUpload componentUpload = new ComponentUpload();

    componentUpload.getFields().put("groupId", "org.apache.maven");
    componentUpload.getFields().put("artifactId", "tomcat");
    componentUpload.getFields().put("version", "5.0.28-SNAPSHOT");

    AssetUpload assetUpload = new AssetUpload();
    assetUpload.getFields().put("extension", "jar");
    assetUpload.setPayload(jarPayload);
    componentUpload.getAssetUploads().add(assetUpload);

    try {
      underTest.handle(repository, componentUpload);
      fail("Expected version policy mismatch exception");
    }
    catch (ValidationErrorsException e) {
      assertThat(e.getValidationErrors().size(), is(1));
      assertThat(e.getValidationErrors().get(0).getMessage(),
          is("Upload to snapshot repositories not supported, use the maven client."));
    }
  }

  @Test
  public void testHandle_parentGroupIdAndVersion() throws Exception {
    when(tempBlob.get()).thenReturn(getClass().getResourceAsStream("./pom.xml"));
    ComponentUpload componentUpload = new ComponentUpload();

    AssetUpload assetUpload = new AssetUpload();
    assetUpload.setPayload(jarPayload);
    assetUpload.setFields(Collections.singletonMap("extension", "pom"));
    componentUpload.getAssetUploads().add(assetUpload);

    underTest.handle(repository, componentUpload);

    ArgumentCaptor<MavenPath> pathCapture = ArgumentCaptor.forClass(MavenPath.class);
    verify(mavenFacet, times(2)).put(pathCapture.capture(), any(Payload.class));

    List<MavenPath> paths = pathCapture.getAllValues();

    assertThat(paths, hasSize(2));

    MavenPath path = paths.get(0);
    assertNotNull(path);
    assertThat(path.getPath(), is("aParentGroupId/anArtifactId/2.0/anArtifactId-2.0.pom"));
    assertCoordinates(path.getCoordinates(), "aParentGroupId", "anArtifactId", "2.0", null, "pom");

    path = paths.get(1);
    assertThat(path.getPath(), is("aParentGroupId/anArtifactId/2.0/anArtifactId-2.0.pom.sha1"));
    assertCoordinates(path.getCoordinates(), "aParentGroupId", "anArtifactId", "2.0", null, "pom.sha1");
  }

  /**
   * Test added to address NEXUS-18196 which was fixed parsing large pom files
   * see, https://github.com/codehaus-plexus/plexus-utils/commit/dd1c85f268f2e56cf0b8b4116119738431c98522
   */
  @Test
  public void testAddingLargePom() throws Exception {
    when(tempBlob.get()).thenReturn(getClass().getResourceAsStream("./large_pom.xml"));
    ComponentUpload componentUpload = new ComponentUpload();

    AssetUpload assetUpload = new AssetUpload();
    assetUpload.setPayload(jarPayload);
    assetUpload.setFields(Collections.singletonMap("extension", "pom"));
    componentUpload.getAssetUploads().add(assetUpload);

    UploadResponse uploadResponse = underTest.handle(repository, componentUpload);

    assertThat(uploadResponse.getAssetPaths(), is(notNullValue()));
  }

  @Test
  public void testHandle_nullCoordinates() throws Exception {
    ComponentUpload componentUpload = new ComponentUpload();

    // note the slashes here are causing the MavenPathParser to choke
    componentUpload.getFields().put("groupId", "a</groupId>");
    componentUpload.getFields().put("artifactId", "a</artifactId>");
    componentUpload.getFields().put("version", "a</version>");
    componentUpload.getFields().put("packaging", "a</packaging>");
    componentUpload.getFields().put("generate-pom", "true");

    AssetUpload assetUpload = new AssetUpload();
    assetUpload.getFields().put("extension", "jar");
    assetUpload.setPayload(jarPayload);
    componentUpload.getAssetUploads().add(assetUpload);

    try {
      underTest.handle(repository, componentUpload);
      fail("Expected invalid coordinates exception");
    }
    catch (ValidationErrorsException e) {
      assertThat(e.getValidationErrors().size(), is(1));
      assertThat(e.getValidationErrors().get(0).getMessage(),
          is("Cannot generate maven coordinate from assembled path 'a</groupId>/a</artifactId>/a</version>/a</artifactId>-a</version>.jar'"));
    }
  }

  @Test
  public void testValidatePom() {
    Model model = new Model();
    model.setGroupId("testGroup");
    model.setArtifactId("testArtifact");
    model.setVersion("1.0");

    underTest.validatePom(model);
  }

  @Test
  public void testValidatePom_parentGroup() {
    Model model = new Model();
    model.setParent(new Parent());
    model.getParent().setGroupId("parentGroup");
    model.setArtifactId("testArtifact");
    model.setVersion("1.0");

    underTest.validatePom(model);
  }

  @Test
  public void testValidatePom_parentVersion() {
    Model model = new Model();
    model.setParent(new Parent());
    model.getParent().setVersion("2.0");
    model.setGroupId("testGroup");
    model.setArtifactId("testArtifact");

    underTest.validatePom(model);
  }

  @Test(expected = ValidationErrorsException.class)
  public void testValidatePom_nullModel() {
    underTest.validatePom(null);
  }

  @Test(expected = ValidationErrorsException.class)
  public void testValidatePom_missingGroupId() {
    Model model = new Model();
    model.setArtifactId("testArtifact");
    model.setVersion("1.0");
    underTest.validatePom(model);
  }

  @Test(expected = ValidationErrorsException.class)
  public void testValidatePom_missingArtifactId() {
    Model model = new Model();
    model.setGroupId("testGroup");
    model.setVersion("1.0");
    underTest.validatePom(model);
  }

  @Test(expected = ValidationErrorsException.class)
  public void testValidatePom_missingVersion() {
    Model model = new Model();
    model.setArtifactId("testArtifact");
    model.setGroupId("testGroup");
    underTest.validatePom(model);
  }

  @Test(expected = ValidationErrorsException.class)
  public void testValidatePom_groupIdWithProperty() {
    Model model = new Model();
    model.setGroupId("${aProperty}");
    model.setArtifactId("testArtifact");
    model.setVersion("1.0");
    underTest.validatePom(model);
  }

  @Test(expected = ValidationErrorsException.class)
  public void testValidatePom_artifactIdWithProperty() {
    Model model = new Model();
    model.setGroupId("testGroup");
    model.setArtifactId("${aProperty}");
    model.setVersion("1.0");
    underTest.validatePom(model);
  }

  @Test(expected = ValidationErrorsException.class)
  public void testValidatePom_versionWithProperty() {
    Model model = new Model();
    model.setArtifactId("testArtifact");
    model.setGroupId("testGroup");
    model.setVersion("${aProperty}");
    underTest.validatePom(model);
  }

  @Test
  public void testHandle_doubleDotInGroupId() throws IOException {
    ComponentUpload componentUpload = new ComponentUpload();

    componentUpload.getFields().put("groupId", "foo/../g/a/v/a-v.jar");
    componentUpload.getFields().put("artifactId", "artifactId");
    componentUpload.getFields().put("version", "version");

    AssetUpload assetUpload = new AssetUpload();
    assetUpload.getFields().put("extension", "jar");
    assetUpload.setPayload(jarPayload);
    componentUpload.getAssetUploads().add(assetUpload);

    underTest.handle(repository, componentUpload);

    ArgumentCaptor<MavenPath> pathCapture = ArgumentCaptor.forClass(MavenPath.class);
    verify(mavenFacet, times(2)).put(pathCapture.capture(), any(Payload.class));

    List<MavenPath> paths = pathCapture.getAllValues();

    assertThat(paths, hasSize(2));

    MavenPath path = paths.get(0);
    assertNotNull(path);
    assertThat(path.getPath(), is("foo////g/a/v/a-v/jar/artifactId/version/artifactId-version.jar"));
    assertCoordinates(path.getCoordinates(), "foo....g.a.v.a-v.jar", "artifactId", "version", null, "jar");

  }

  @Test
  public void testHandle_doubleDotInArtifactId() throws IOException {
    ComponentUpload componentUpload = new ComponentUpload();

    componentUpload.getFields().put("groupId", "groupId");
    componentUpload.getFields().put("artifactId", "/../g/a/v/a-v.jar");
    componentUpload.getFields().put("version", "version");

    AssetUpload assetUpload = new AssetUpload();
    assetUpload.getFields().put("extension", "jar");
    assetUpload.setPayload(jarPayload);
    componentUpload.getAssetUploads().add(assetUpload);

    try {
      underTest.handle(repository, componentUpload);
      fail("Expected ValidationErrorsException");
    }
    catch (ValidationErrorsException e) {
      assertThat(e.getValidationErrors().size(), is(1));
      assertThat(e.getValidationErrors().get(0).getMessage(),
          is("Path is not allowed to have '.' or '..' segments: '/groupId//../g/a/v/a-v.jar/version//../g/a/v/a-v.jar-version.jar'"));
    }
  }

  @Test
  public void testHandle_doubleDotInVersion() throws IOException {
    ComponentUpload componentUpload = new ComponentUpload();

    componentUpload.getFields().put("groupId", "groupId");
    componentUpload.getFields().put("artifactId", "artifactId");
    componentUpload.getFields().put("version", "/../g/a/v/a-v.jar");

    AssetUpload assetUpload = new AssetUpload();
    assetUpload.getFields().put("extension", "jar");
    assetUpload.setPayload(jarPayload);
    componentUpload.getAssetUploads().add(assetUpload);

    try {
      underTest.handle(repository, componentUpload);
      fail("Expected ValidationErrorsException");
    }
    catch (ValidationErrorsException e) {
      assertThat(e.getValidationErrors().size(), is(1));
      assertThat(e.getValidationErrors().get(0).getMessage(),
          is("Path is not allowed to have '.' or '..' segments: '/groupId/artifactId//../g/a/v/a-v.jar/artifactId-/../g/a/v/a-v.jar.jar'"));
    }
  }

  @Test
  public void testHandle_doubleDotInExtension() throws IOException {
    ComponentUpload componentUpload = new ComponentUpload();

    componentUpload.getFields().put("groupId", "groupId");
    componentUpload.getFields().put("artifactId", "artifactId");
    componentUpload.getFields().put("version", "version");

    AssetUpload assetUpload = new AssetUpload();
    assetUpload.getFields().put("extension", "/../g/a/v/a-v.jar");
    assetUpload.setPayload(jarPayload);
    componentUpload.getAssetUploads().add(assetUpload);

    try {
      underTest.handle(repository, componentUpload);
      fail("Expected ValidationErrorsException");
    }
    catch (ValidationErrorsException e) {
      assertThat(e.getValidationErrors().size(), is(1));
      assertThat(e.getValidationErrors().get(0).getMessage(),
          is("Path is not allowed to have '.' or '..' segments: '/groupId/artifactId/version/artifactId-version./../g/a/v/a-v.jar'"));
    }
  }

  @Test
  public void testHandle_snapshot_asset() throws IOException {
    when(versionPolicyValidator.validArtifactPath(any(), any())).thenReturn(false);
    File file = temporaryFolder.newFile("artifact-1.0-20201124.222716-1.jar");
    Content result =
        underTest.handle(repository, file, "group/artifact/1.0-SNAPSHOT/artifact-1.0-20201124.222716-1.jar");

    assertNull(result);
  }

  @Test
  public void testHandle_snapshot_metadata() throws IOException {
    when(versionPolicyValidator.validMetadataPath(any(), any())).thenReturn(false);
    File file = temporaryFolder.newFile("maven-metadata.xml");
    Content result = underTest.handle(repository, file, "group/artifact/1.0-SNAPSHOT/maven-metadata.xml");

    assertNull(result);
  }

  // ========================= Stream Import Tests =========================

  @Test
  public void testHandleStreamConfiguration_jarFile() throws IOException {
    // Setup test data
    byte[] jarContent = "fake jar content".getBytes();
    InputStream inputStream = new ByteArrayInputStream(jarContent);
    String assetName = "/org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar";

    ImportStreamConfiguration configuration = new ImportStreamConfiguration(repository, inputStream, assetName);

    // Mock blob ingestion to return the content from stream
    FluentBlobs blobs = mock(FluentBlobs.class);
    when(mavenFacet.blobs()).thenReturn(blobs);
    when(blobs.ingest(eq(inputStream), eq(null), any())).thenReturn(tempBlob);

    // Execute
    Content result = underTest.doPut(configuration);

    // Verify
    assertNotNull(result);

    // Verify that stream was ingested
    verify(blobs).ingest(eq(inputStream), eq(null), any());

    // Verify the payload was passed to underlying doPut with correct content type
    ArgumentCaptor<Payload> payloadCaptor = ArgumentCaptor.forClass(Payload.class);
    ArgumentCaptor<MavenPath> pathCaptor = ArgumentCaptor.forClass(MavenPath.class);
    verify(mavenFacet, times(2)).put(pathCaptor.capture(), payloadCaptor.capture());

    // First call should be the main JAR file
    List<MavenPath> capturedPaths = pathCaptor.getAllValues();
    List<Payload> capturedPayloads = payloadCaptor.getAllValues();

    MavenPath jarPath = capturedPaths.get(0);
    Payload jarPayload = capturedPayloads.get(0);
    assertThat(jarPath.getPath(), is("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar"));
    // Windows registry may return "application/jar" instead of "application/java-archive"
    assertThat(jarPayload.getContentType(), anyOf(is("application/java-archive"), is("application/jar")));

    // Second call should be the generated checksum file
    MavenPath checksumPath = capturedPaths.get(1);
    assertThat(checksumPath.getPath(), is("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar.sha1"));
  }

  @Test
  public void testHandleStreamConfiguration_pomFile() throws IOException {
    // Setup test data
    byte[] pomContent = "<project></project>".getBytes();
    InputStream inputStream = new ByteArrayInputStream(pomContent);
    String assetName = "/org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.pom";

    ImportStreamConfiguration configuration = new ImportStreamConfiguration(repository, inputStream, assetName);

    // Setup specific mock for POM file MIME type
    when(mimeSupport.guessMimeTypeFromPath("/org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.pom"))
        .thenReturn("application/xml");

    // Mock blob ingestion
    FluentBlobs blobs = mock(FluentBlobs.class);
    when(mavenFacet.blobs()).thenReturn(blobs);
    when(blobs.ingest(eq(inputStream), eq(null), any())).thenReturn(tempBlob);

    // Execute
    Content result = underTest.doPut(configuration);

    // Verify
    assertNotNull(result);
    verify(blobs).ingest(eq(inputStream), eq(null), any());

    // Verify content type was set correctly - POM files also generate checksum files
    ArgumentCaptor<Payload> payloadCaptor = ArgumentCaptor.forClass(Payload.class);
    ArgumentCaptor<MavenPath> pathCaptor = ArgumentCaptor.forClass(MavenPath.class);
    verify(mavenFacet, times(2)).put(pathCaptor.capture(), payloadCaptor.capture());

    // First call should be the main POM file
    List<MavenPath> capturedPaths = pathCaptor.getAllValues();
    List<Payload> capturedPayloads = payloadCaptor.getAllValues();

    MavenPath pomPath = capturedPaths.get(0);
    Payload pomPayload = capturedPayloads.get(0);
    assertThat(pomPath.getPath(), is("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.pom"));
    assertThat(pomPayload.getContentType(), is("application/xml"));

    // Second call should be the generated checksum file
    MavenPath checksumPath = capturedPaths.get(1);
    assertThat(checksumPath.getPath(), is("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.pom.sha1"));
  }

  @Test
  public void testHandleStreamConfiguration_hashFile() throws IOException {
    // Setup test data - hash files should be skipped like in file-based imports
    byte[] hashContent = "da39a3ee5e6b4b0d3255bfef95601890afd80709".getBytes();
    InputStream inputStream = new ByteArrayInputStream(hashContent);
    String assetName = "/org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar.sha1";

    ImportStreamConfiguration configuration = new ImportStreamConfiguration(repository, inputStream, assetName);

    // Execute - hash files should be skipped in stream imports (like file imports)
    Content result = underTest.handle(configuration);

    // Verify hash files are skipped (return null) to match file-based behavior
    assertNull(result);

    // Verify no blob ingestion occurred since hash files are skipped
    verify(mavenFacet.blobs(), times(0)).ingest(any(InputStream.class), any(), any());
  }

  @Test
  public void testDetermineContentTypeFromPath_variousExtensions() {
    // Setup specific mock behaviors for different file paths
    when(mimeSupport.guessMimeTypeFromPath("/path/to/file.jar")).thenReturn("application/java-archive");
    when(mimeSupport.guessMimeTypeFromPath("/path/to/file.war")).thenReturn("application/java-archive");
    when(mimeSupport.guessMimeTypeFromPath("/path/to/file.ear")).thenReturn("application/java-archive");
    when(mimeSupport.guessMimeTypeFromPath("/path/to/file.JAR")).thenReturn("application/java-archive");

    when(mimeSupport.guessMimeTypeFromPath("/path/to/file.pom")).thenReturn("application/xml");
    when(mimeSupport.guessMimeTypeFromPath("/path/to/file.xml")).thenReturn("application/xml");
    when(mimeSupport.guessMimeTypeFromPath("/path/to/file.POM")).thenReturn("application/xml");

    when(mimeSupport.guessMimeTypeFromPath("/path/to/file.jar.md5")).thenReturn("text/plain");
    when(mimeSupport.guessMimeTypeFromPath("/path/to/file.jar.sha1")).thenReturn("text/plain");
    when(mimeSupport.guessMimeTypeFromPath("/path/to/file.jar.sha256")).thenReturn("text/plain");
    when(mimeSupport.guessMimeTypeFromPath("/path/to/file.jar.sha512")).thenReturn("text/plain");
    when(mimeSupport.guessMimeTypeFromPath("/path/to/file.jar.asc")).thenReturn("text/plain");

    when(mimeSupport.guessMimeTypeFromPath("/path/to/file.unknown")).thenReturn("application/octet-stream");

    // Test JAR files - Windows registry may return "application/jar" via Files.probeContentType
    assertThat(underTest.determineContentTypeFromPath("/path/to/file.jar"),
        anyOf(is("application/java-archive"), is("application/jar")));
    assertThat(underTest.determineContentTypeFromPath("/path/to/file.war"),
        anyOf(is("application/java-archive"), is("application/jar")));
    assertThat(underTest.determineContentTypeFromPath("/path/to/file.ear"),
        anyOf(is("application/java-archive"), is("application/jar")));

    // Test XML files - Windows registry may return "text/xml" via Files.probeContentType
    assertThat(underTest.determineContentTypeFromPath("/path/to/file.pom"),
        anyOf(is("application/xml"), is("text/xml")));
    assertThat(underTest.determineContentTypeFromPath("/path/to/file.xml"),
        anyOf(is("application/xml"), is("text/xml")));

    // Test hash/signature files
    assertThat(underTest.determineContentTypeFromPath("/path/to/file.jar.md5"), is("text/plain"));
    assertThat(underTest.determineContentTypeFromPath("/path/to/file.jar.sha1"), is("text/plain"));
    assertThat(underTest.determineContentTypeFromPath("/path/to/file.jar.sha256"), is("text/plain"));
    assertThat(underTest.determineContentTypeFromPath("/path/to/file.jar.sha512"), is("text/plain"));

    // Test unknown extension
    assertThat(underTest.determineContentTypeFromPath("/path/to/file.unknown"), is("application/octet-stream"));

    // Test case insensitivity
    assertThat(underTest.determineContentTypeFromPath("/path/to/file.JAR"),
        anyOf(is("application/java-archive"), is("application/jar")));
    assertThat(underTest.determineContentTypeFromPath("/path/to/file.POM"),
        anyOf(is("application/xml"), is("text/xml")));
  }

  @Test
  public void testHandleStreamConfiguration_withMavenValidation() throws IOException {
    // Setup test data
    byte[] jarContent = "fake jar content".getBytes();
    InputStream inputStream = new ByteArrayInputStream(jarContent);
    String assetName = "/org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar";

    ImportStreamConfiguration configuration = new ImportStreamConfiguration(repository, inputStream, assetName);

    // Mock blob ingestion
    FluentBlobs blobs = mock(FluentBlobs.class);
    when(mavenFacet.blobs()).thenReturn(blobs);
    when(blobs.ingest(any(InputStream.class), eq(null), any())).thenReturn(tempBlob);

    // Execute - call handle() not doPut() to trigger validation logic
    Content result = underTest.handle(configuration);

    // Verify Maven validation was applied
    verify(versionPolicyValidator).validArtifactPath(eq(VersionPolicy.RELEASE), any(Coordinates.class));
    assertNotNull(result);
  }

  @Test
  public void testHandleStreamConfiguration_ignoredPath() throws IOException {
    // Setup test data - archetype catalog should be ignored
    byte[] content = "catalog content".getBytes();
    InputStream inputStream = new ByteArrayInputStream(content);
    String assetName = "/archetype-catalog.xml";

    ImportStreamConfiguration configuration = new ImportStreamConfiguration(repository, inputStream, assetName);

    // Execute
    Content result = underTest.handle(configuration);

    // Verify ignored files return null
    assertNull(result);

    // Verify no interaction with blob store
    verify(mavenFacet.blobs(), times(0)).ingest(any(InputStream.class), any(), any());
  }

  @Test
  public void testHandleStreamConfiguration_validationFailure() throws IOException {
    // Setup validation failure
    when(versionPolicyValidator.validArtifactPath(any(), any())).thenReturn(false);

    // Setup test data
    byte[] jarContent = "fake jar content".getBytes();
    InputStream inputStream = new ByteArrayInputStream(jarContent);
    String assetName = "/org/apache/maven/tomcat/5.0.28-SNAPSHOT/tomcat-5.0.28-SNAPSHOT.jar";

    ImportStreamConfiguration configuration = new ImportStreamConfiguration(repository, inputStream, assetName);

    // Execute
    Content result = underTest.handle(configuration);

    // Verify validation failure results in null
    assertNull(result);
  }

  @Test
  public void testHandleStreamConfiguration_hashFileSkipped() throws IOException {
    // Setup test data - hash files should be skipped when hardlinking is disabled (default)
    byte[] hashContent = "da39a3ee5e6b4b0d3255bfef95601890afd80709".getBytes();
    InputStream inputStream = new ByteArrayInputStream(hashContent);
    String assetName = "/org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar.sha1";

    ImportStreamConfiguration configuration = new ImportStreamConfiguration(repository, inputStream, assetName);

    // Execute
    Content result = underTest.handle(configuration);

    // Verify hash files are skipped when hardlinking is not enabled
    assertNull(result);

    // Verify no blob ingestion occurred
    verify(mavenFacet.blobs(), times(0)).ingest(any(InputStream.class), any(), any());
  }

  @Test
  public void testHandleStreamConfiguration_properPathParsing() throws IOException {
    // Setup test data
    byte[] jarContent = "fake jar content".getBytes();
    InputStream inputStream = new ByteArrayInputStream(jarContent);
    String assetName = "/com/example/my-artifact/1.2.3/my-artifact-1.2.3-sources.jar";

    ImportStreamConfiguration configuration = new ImportStreamConfiguration(repository, inputStream, assetName);

    // Mock blob ingestion
    FluentBlobs blobs = mock(FluentBlobs.class);
    when(mavenFacet.blobs()).thenReturn(blobs);
    when(blobs.ingest(any(InputStream.class), eq(null), any())).thenReturn(tempBlob);

    // Execute
    Content result = underTest.doPut(configuration);

    // Verify Maven path was parsed correctly - will have multiple calls due to checksum generation
    ArgumentCaptor<MavenPath> pathCaptor = ArgumentCaptor.forClass(MavenPath.class);
    verify(mavenFacet, times(2)).put(pathCaptor.capture(), any(Payload.class));

    // First call should be the main JAR file
    List<MavenPath> capturedPaths = pathCaptor.getAllValues();
    MavenPath capturedPath = capturedPaths.get(0);
    assertThat(capturedPath.getPath(), is("com/example/my-artifact/1.2.3/my-artifact-1.2.3-sources.jar"));

    Coordinates coords = capturedPath.getCoordinates();
    assertThat(coords.getGroupId(), is("com.example"));
    assertThat(coords.getArtifactId(), is("my-artifact"));
    assertThat(coords.getVersion(), is("1.2.3"));
    assertThat(coords.getClassifier(), is("sources"));
    assertThat(coords.getExtension(), is("jar"));

    // Second call should be the generated checksum file
    MavenPath checksumPath = capturedPaths.get(1);
    assertThat(checksumPath.getPath(), is("com/example/my-artifact/1.2.3/my-artifact-1.2.3-sources.jar.sha1"));

    assertNotNull(result);
  }

  @Test
  public void testDoPut_doesNotCreateChecksumForHashFiles() throws IOException {
    // Setup: Create a MavenPath for a hash file (.sha1)
    Maven2MavenPathParser parser = new Maven2MavenPathParser();
    MavenPath sha1Path = parser.parsePath("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar.sha1");

    // Mock the content that would be returned
    Content content = mock(Content.class);
    AttributesMap attributesMap = mock(AttributesMap.class);
    when(content.getAttributes()).thenReturn(attributesMap);
    when(mavenFacet.put(eq(sha1Path), any(Payload.class))).thenReturn(content);

    // Execute: Upload a hash file
    Payload payload = mock(Payload.class);
    Content result = underTest.doPut(repository, sha1Path, payload);

    // Verify: Only one put call should occur (for the hash file itself),
    // no additional checksum files should be created for a hash file
    verify(mavenFacet, times(1)).put(eq(sha1Path), any(Payload.class));
    assertNotNull(result);
  }

  @Test
  public void testDoPut_createsChecksumForNonHashFiles() throws IOException {
    // Setup: Create a MavenPath for a regular JAR file (not a hash)
    Maven2MavenPathParser parser = new Maven2MavenPathParser();
    MavenPath jarPath = parser.parsePath("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar");

    // Mock the content that would be returned with checksum information
    Content content = mock(Content.class);
    AttributesMap attributesMap = mock(AttributesMap.class);
    Asset asset = mock(Asset.class);
    AssetBlob blob = mock(AssetBlob.class);
    Map<String, String> checksums = Collections.singletonMap(
        HashAlgorithm.SHA1.name(),
        "da39a3ee5e6b4b0d3255bfef95601890afd80709");

    when(content.getAttributes()).thenReturn(attributesMap);
    when(attributesMap.get(Asset.class)).thenReturn(asset);
    when(attributesMap.require(eq(Content.CONTENT_LAST_MODIFIED), eq(DateTime.class))).thenReturn(DateTime.now());
    when(asset.blob()).thenReturn(Optional.of(blob));
    when(blob.checksums()).thenReturn(checksums);
    when(mavenFacet.put(any(MavenPath.class), any(Payload.class))).thenReturn(content);

    // Execute: Upload a JAR file
    Payload payload = mock(Payload.class);
    Content result = underTest.doPut(repository, jarPath, payload);

    // Verify: Two put calls should occur - one for the JAR and one for the checksum
    ArgumentCaptor<MavenPath> pathCaptor = ArgumentCaptor.forClass(MavenPath.class);
    verify(mavenFacet, times(2)).put(pathCaptor.capture(), any(Payload.class));

    List<MavenPath> paths = pathCaptor.getAllValues();
    assertThat(paths.get(0).getPath(), is("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar"));
    assertThat(paths.get(1).getPath(), is("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar.sha1"));
    assertNotNull(result);
  }

  @Test
  public void testDoPut_handlesMultipleHashTypes() throws IOException {
    // Setup: Create a MavenPath for different hash file types
    Maven2MavenPathParser parser = new Maven2MavenPathParser();

    // Test MD5 hash file
    MavenPath md5Path = parser.parsePath("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar.md5");
    Content content = mock(Content.class);
    AttributesMap attributesMap = mock(AttributesMap.class);
    when(content.getAttributes()).thenReturn(attributesMap);
    when(mavenFacet.put(eq(md5Path), any(Payload.class))).thenReturn(content);

    Payload payload = mock(Payload.class);
    underTest.doPut(repository, md5Path, payload);

    // Verify: Only one put call for MD5 hash file
    verify(mavenFacet, times(1)).put(eq(md5Path), any(Payload.class));

    // Reset mocks for SHA256 test
    when(mavenFacet.put(any(MavenPath.class), any(Payload.class))).thenReturn(content);

    // Test SHA256 hash file
    MavenPath sha256Path = parser.parsePath("org/apache/maven/tomcat/5.0.28/tomcat-5.0.28.jar.sha256");
    underTest.doPut(repository, sha256Path, payload);

    // Verify: Only one additional put call for SHA256 hash file
    verify(mavenFacet, times(2)).put(any(MavenPath.class), any(Payload.class));
  }

  private static void assertVariableSource(
      final VariableSource source,
      final String path,
      final String groupId,
      final String artifactId,
      final String version,
      final String classifier,
      final String extension)
  {
    int size = (classifier == null) ? 6 : 7;

    assertThat(source.getVariableSet(), hasSize(size));
    assertThat(source.get("format"), is(Optional.of(Maven2Format.NAME)));
    assertThat(source.get("path"), is(Optional.of(path)));
    assertThat(source.get("coordinate.groupId"), is(Optional.of(groupId)));
    assertThat(source.get("coordinate.artifactId"), is(Optional.of(artifactId)));
    assertThat(source.get("coordinate.version"), is(Optional.of(version)));
    if (classifier != null) {
      assertThat(source.get("coordinate.classifier"), is(Optional.of(classifier)));
    }
    assertThat(source.get("coordinate.extension"), is(Optional.of(extension)));
  }

  private static void assertCoordinates(
      final Coordinates actual,
      final String groupId,
      final String artifactId,
      final String version,
      final String classifier,
      final String extension)
  {
    assertThat(actual.getGroupId(), is(groupId));
    assertThat(actual.getArtifactId(), is(artifactId));
    assertThat(actual.getVersion(), is(version));
    assertThat(actual.getClassifier(), is(classifier));
    assertThat(actual.getExtension(), is(extension));

    assertNull(actual.getBuildNumber());
    assertNull(actual.getTimestamp());
  }

  private UploadFieldDefinition field(
      final String name,
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

  private class TestUploadDefinitionExtension
      implements UploadDefinitionExtension
  {

    @Override
    public UploadFieldDefinition contribute() {
      return new UploadFieldDefinition("foo", "Foo", null, true, STRING, "bar");
    }
  }
}
