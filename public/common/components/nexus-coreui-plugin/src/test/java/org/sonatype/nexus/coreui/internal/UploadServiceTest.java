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
package org.sonatype.nexus.coreui.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.cache.RepositoryCacheInvalidationService;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.repository.upload.UploadResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class UploadServiceTest
{
  private static final String REPO_NAME = "repo";

  private UploadService component;

  @Mock
  private UploadManager uploadManager;

  @Mock
  private RepositoryManager repositoryManager;

  @Mock
  private RepositoryCacheInvalidationService repositoryCacheInvalidationService;

  @Mock
  private Repository repo;

  @Mock
  private HttpServletRequest request;

  @Before
  public void setup() throws IOException {
    when(repositoryManager.get(REPO_NAME)).thenReturn(repo);

    UploadResponse uploadResponse = new UploadResponse(Collections.singletonList("foo"));
    when(uploadManager.handle(repo, request)).thenReturn(uploadResponse);

    component = new UploadService(
        repositoryManager, uploadManager, repositoryCacheInvalidationService);
  }

  @Test
  public void testUpload_unknownRepository() throws IOException {
    try {
      component.upload("foo", request);
      fail("Expected exception to be thrown");
    }
    catch (NullPointerException e) {
      assertThat(e.getMessage(), is("Specified repository is missing"));
    }
  }

  @Test
  public void testUpload() throws IOException {
    Format format = mock(Format.class);
    when(repo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn(null);
    assertThat(component.upload(REPO_NAME, request), is("foo"));
  }

  @Test
  public void testUploadNpm() throws IOException {
    Format format = mock(Format.class);
    when(repo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("npm");
    assertThat(component.upload(REPO_NAME, request), is("foo"));
    verify(repositoryManager).findContainingGroups(REPO_NAME);
  }

  @Test
  public void testCreateSearchTerm() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("raw");

    String result = component
        .createSearchTerm(mockRepo, Arrays.asList("foo-x.z/bar/bar", "foo-x.z/bar/foo", "foo-x.z/bar/foo/bar"));

    // Multiple files: prefix is the directory "foo-x.z/bar"; the generic
    // (non-Maven/Helm/Terraform) branch then converts "/" → " " for keyword
    // tokenization.
    assertThat(result, is("foo-x.z bar"));
  }

  /**
   * Test that verifies upload functionality works for hosted repositories.
   * This test relates to NEXUS-49573 where the upload button was missing
   * from the browse view for hosted repositories.
   */
  @Test
  public void testUpload_hostedRepository() throws IOException {
    Format format = mock(Format.class);
    when(repo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");
    String result = component.upload(REPO_NAME, request);
    assertThat(result, is("foo"));
    verify(uploadManager).handle(repo, request);
  }

  @Test
  public void testGetAvailableDefinitions() {
    UploadDefinition def = mock(UploadDefinition.class);
    when(uploadManager.getAvailableDefinitions()).thenReturn(List.of(def));

    Collection<UploadDefinition> definitions = component.getAvailableDefinitions();
    assertThat(definitions, hasSize(1));
    verify(uploadManager).getAvailableDefinitions();
  }

  @Test
  public void testCreateSearchTerm_emptyPaths() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("raw");

    String result = component.createSearchTerm(mockRepo, Collections.emptyList());
    assertThat(result, is(nullValue()));
  }

  @Test
  public void testCreateSearchTerm_singlePath() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("raw");

    String result = component.createSearchTerm(mockRepo, List.of("org/example/artifact/1.0/artifact-1.0.jar"));
    assertThat(result, is("org example artifact 1.0"));
  }

  @Test
  public void testCreateSearchTerm_partialCommonPrefix() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("raw");

    String result = component.createSearchTerm(mockRepo,
        Arrays.asList("org/example/foo/1.0/foo.jar", "org/example/bar/1.0/bar.jar"));
    // Multiple files: common prefix is "org/example" (directory), not stripped
    assertThat(result, is("org example"));
  }

  @Test
  public void testCreateSearchTerm_mavenMultipleFiles() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");

    String result = component.createSearchTerm(mockRepo, Arrays.asList(
        "/org/example/myapp/1.0.0/myapp-1.0.0.jar",
        "/org/example/myapp/1.0.0/myapp-1.0.0.pom",
        "/org/example/myapp/1.0.0/myapp-1.0.0-sources.jar"));

    assertThat(result, is("org.example:myapp:1.0.0"));
  }

  @Test
  public void testCreateSearchTerm_mavenSingleFile() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");

    String result = component.createSearchTerm(mockRepo,
        List.of("/org/example/artifact/1.0/artifact-1.0.jar"));

    assertThat(result, is("org.example:artifact:1.0"));
  }

  @Test
  public void testConvertMavenPathToGAV_validPath() {
    String result = component.convertMavenPathToGAV("/org/example/artifact/1.0.0/");
    assertThat(result, is("org.example:artifact:1.0.0"));
  }

  @Test
  public void testConvertMavenPathToGAV_realUserPath() {
    String result = component.convertMavenPathToGAV("/sonatype/nexus/3.91.0/");
    assertThat(result, is("sonatype:nexus:3.91.0"));
  }

  @Test
  public void testUploadNpm_withGroupCacheInvalidation() throws IOException {
    Format format = mock(Format.class);
    when(repo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("npm");
    when(repositoryManager.findContainingGroups(REPO_NAME)).thenReturn(Set.of("npm-group"));

    Repository groupRepo = mock(Repository.class);
    when(repositoryManager.get("npm-group")).thenReturn(groupRepo);

    component.upload(REPO_NAME, request);

    verify(repositoryCacheInvalidationService).processCachesInvalidation(groupRepo);
  }

  // ========== Tests for Other Formats ==========

  @Test
  public void testCreateSearchTerm_npmFormat() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("npm");

    String result = component.createSearchTerm(mockRepo, List.of("/my-package/1.0.0/my-package-1.0.0.tgz"));
    // npm format should return: "my-package 1.0.0"
    assertThat(result, is("my-package 1.0.0"));
  }

  @Test
  public void testCreateSearchTerm_dockerFormat() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("docker");

    String result = component.createSearchTerm(mockRepo,
        List.of("/v2/myimage/manifests/sha256:abc123"));
    // docker format with single file: filename stripped to directory
    assertThat(result, is("v2 myimage manifests"));
  }

  @Test
  public void testCreateSearchTerm_nugetFormat() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("nuget");

    String result = component.createSearchTerm(mockRepo,
        List.of("/mypackage/1.0.0/mypackage.1.0.0.nupkg"));
    // nuget format should return: "mypackage 1.0.0"
    assertThat(result, is("mypackage 1.0.0"));
  }

  @Test
  public void testCreateSearchTerm_pypiFormat() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("pypi");

    String result = component.createSearchTerm(mockRepo,
        List.of("/packages/my-package/1.0.0/my_package-1.0.0-py3-none-any.whl"));
    // pypi format should return path segments as keywords
    assertThat(result, is("packages my-package 1.0.0"));
  }

  @Test
  public void testCreateSearchTerm_rubygemsFormat() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("rubygems");

    String result = component.createSearchTerm(mockRepo,
        List.of("/gems/my-gem-1.0.0.gem"));
    // rubygems format should return path segments as keywords
    assertThat(result, is("gems"));
  }

  // ========== Maven Edge Cases ==========

  @Test
  public void testCreateSearchTerm_mavenSnapshotVersion() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");

    String result = component.createSearchTerm(mockRepo, Arrays.asList(
        "/org/example/myapp/1.0.0-SNAPSHOT/myapp-1.0.0-SNAPSHOT.jar",
        "/org/example/myapp/1.0.0-SNAPSHOT/myapp-1.0.0-SNAPSHOT.pom"));

    // Should correctly handle SNAPSHOT versions
    assertThat(result, is("org.example:myapp:1.0.0-SNAPSHOT"));
  }

  @Test
  public void testCreateSearchTerm_mavenDeeplyNestedGroups() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");

    String result = component.createSearchTerm(mockRepo, Arrays.asList(
        "/com/sonatype/nexus/plugins/myapp/1.0.0/myapp-1.0.0.jar",
        "/com/sonatype/nexus/plugins/myapp/1.0.0/myapp-1.0.0.pom"));

    // Should correctly handle deeply nested group IDs
    assertThat(result, is("com.sonatype.nexus.plugins:myapp:1.0.0"));
  }

  @Test
  public void testCreateSearchTerm_mavenSingleSegmentGroup() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");

    String result = component.createSearchTerm(mockRepo,
        List.of("/example/myapp/1.0/myapp-1.0.jar"));

    // Should handle single-segment group (uncommon but valid)
    assertThat(result, is("example:myapp:1.0"));
  }

  @Test
  public void testConvertMavenPathToGAV_lessThanThreeSegments() {
    // Test fallback behavior for paths with < 3 segments
    String result = component.convertMavenPathToGAV("/myapp/1.0/");
    // Should return space-separated segments (fallback)
    assertThat(result, is("myapp 1.0"));
  }

  @Test
  public void testCreateSearchTerm_pathWithoutLeadingSlash() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("maven2");

    String result = component.createSearchTerm(mockRepo,
        List.of("org/example/artifact/1.0/artifact-1.0.jar"));

    // Should handle paths without leading slash
    assertThat(result, is("org.example:artifact:1.0"));
  }

  @Test
  public void testCreateSearchTerm_npmMultipleFiles() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("npm");

    String result = component.createSearchTerm(mockRepo, Arrays.asList(
        "/@scope/my-package/1.0.0/my-package-1.0.0.tgz",
        "/@scope/my-package/1.0.0/package.json"));

    // Should handle scoped npm packages
    assertThat(result, is("@scope my-package 1.0.0"));
  }

  // ========== Helm Format Tests ==========

  @Test
  public void testConvertHelmPathToName_standardChart() {
    // Standard Helm chart with semantic version
    String result = component.convertHelmPathToName("/nginx-1.0.0.tgz");
    assertThat(result, is("nginx"));
  }

  @Test
  public void testConvertHelmPathToName_preReleaseVersion() {
    // Helm chart with pre-release version (e.g., 1.0.0-rc.1)
    String result = component.convertHelmPathToName("/nginx-1.0.0-rc.1.tgz");
    assertThat(result, is("nginx"));
  }

  @Test
  public void testConvertHelmPathToName_preReleaseBeta() {
    // Helm chart with beta pre-release version
    String result = component.convertHelmPathToName("/myapp-2.3.4-beta.2.tgz");
    assertThat(result, is("myapp"));
  }

  @Test
  public void testConvertHelmPathToName_preReleaseAlpha() {
    // Helm chart with alpha pre-release version
    String result = component.convertHelmPathToName("/myapp-1.0.0-alpha.1.tgz");
    assertThat(result, is("myapp"));
  }

  @Test
  public void testConvertHelmPathToName_hyphenatedChartName() {
    // Chart name contains hyphens
    String result = component.convertHelmPathToName("/my-chart-1.0.0.tgz");
    assertThat(result, is("my-chart"));
  }

  @Test
  public void testConvertHelmPathToName_multipleHyphensInName() {
    // Chart name contains multiple hyphens
    String result = component.convertHelmPathToName("/my-complex-chart-name-2.5.1.tgz");
    assertThat(result, is("my-complex-chart-name"));
  }

  @Test
  public void testConvertHelmPathToName_tarGzExtension() {
    // Chart with .tar.gz extension instead of .tgz
    String result = component.convertHelmPathToName("/nginx-15.14.0.tar.gz");
    assertThat(result, is("nginx"));
  }

  @Test
  public void testConvertHelmPathToName_provenanceFile() {
    // Helm provenance file (.prov extension)
    String result = component.convertHelmPathToName("/nginx-1.0.0.tgz.prov");
    assertThat(result, is("nginx"));
  }

  @Test
  public void testConvertHelmPathToName_noLeadingSlash() {
    // Path without leading slash
    String result = component.convertHelmPathToName("nginx-1.0.0.tgz");
    assertThat(result, is("nginx"));
  }

  @Test
  public void testConvertHelmPathToName_snapshotVersion() {
    // Helm chart with snapshot-like version
    String result = component.convertHelmPathToName("/myapp-1.0.0-SNAPSHOT.tgz");
    assertThat(result, is("myapp"));
  }

  @Test
  public void testConvertHelmPathToName_buildMetadata() {
    // Helm chart with build metadata (e.g., 1.0.0+20130313)
    String result = component.convertHelmPathToName("/myapp-1.0.0+20130313.tgz");
    assertThat(result, is("myapp"));
  }

  @Test
  public void testConvertHelmPathToName_noVersion() {
    // Chart name without apparent version (fallback case)
    String result = component.convertHelmPathToName("/myapp.tgz");
    assertThat(result, is("myapp"));
  }

  @Test
  public void testConvertHelmPathToName_onlyName() {
    // Chart name that ends with something that looks like it starts with digit but isn't semver
    String result = component.convertHelmPathToName("/chart-2name.tgz");
    assertThat(result, is("chart-2name"));
  }

  @Test
  public void testCreateSearchTerm_helmFormat() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("helm");

    String result = component.createSearchTerm(mockRepo, List.of("/nginx-1.0.0.tgz"));
    // Helm format should extract chart name from filename
    assertThat(result, is("nginx"));
  }

  @Test
  public void testCreateSearchTerm_helmFormatWithPreRelease() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("helm");

    String result = component.createSearchTerm(mockRepo, List.of("/nginx-1.0.0-rc.1.tgz"));
    // Helm format should correctly handle pre-release versions
    assertThat(result, is("nginx"));
  }

  @Test
  public void testCreateSearchTerm_helmMultipleCharts() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("helm");

    String result = component.createSearchTerm(mockRepo, Arrays.asList(
        "/nginx-1.0.0.tgz",
        "/nginx-1.0.0.tgz.prov"));
    // Helm format with multiple files should extract common name
    assertThat(result, is("nginx"));
  }

  /**
   * NEXUS-52809: Terraform format must return namespace/name/provider rather
   * than the noisy /v1/modules/... path tokens.
   */
  @Test
  public void testCreateSearchTerm_terraformFormat() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("terraform");

    String result = component.createSearchTerm(mockRepo,
        List.of("/v1/modules/myorg/mymod/local/0.0.5-main-20260323/mymod-0.0.5.zip"));
    assertThat(result, is("myorg/mymod/local"));
  }

  /**
   * NEXUS-52809 reproduction: two modules sharing namespace+provider but
   * different names must yield distinct search terms.
   */
  @Test
  public void testCreateSearchTerm_terraformFormatDistinguishesSiblings() {
    Repository mockRepo = mock(Repository.class);
    Format format = mock(Format.class);
    when(mockRepo.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn("terraform");

    String mymod = component.createSearchTerm(mockRepo,
        List.of("/v1/modules/myorg/mymod/local/0.0.5/mymod-0.0.5.zip"));
    String mymod2 = component.createSearchTerm(mockRepo,
        List.of("/v1/modules/myorg/mymod2/local/1.2.3/mymod2-1.2.3.zip"));

    assertThat(mymod, is("myorg/mymod/local"));
    assertThat(mymod2, is("myorg/mymod2/local"));
  }

  @Test
  public void testConvertPathToSearchTerm_terraformUnexpectedShape() {
    // Defensive fallback: if the path doesn't begin with /v1/modules/...,
    // return the cleaned path rather than null or a partial slice.
    String result = component.convertPathToSearchTerm("terraform", "/some/other/shape");
    assertThat(result, is("some/other/shape"));
  }

  @Test
  public void testConvertPathToSearchTerm_terraformCaseInsensitivePrefix() {
    String result = component.convertPathToSearchTerm(
        "terraform", "/V1/MODULES/myorg/mymod/local/1.0.0/mymod-1.0.0.zip");
    assertThat(result, is("myorg/mymod/local"));
  }

  @Test
  public void testConvertPathToSearchTerm_terraformEmptyPath() {
    // Empty string is passed through unchanged so the caller's empty-cleaned
    // check can fall back to its default keyword behaviour.
    String result = component.convertPathToSearchTerm("terraform", "");
    assertThat(result, is(""));
  }

  @Test
  public void testConvertPathToSearchTerm_terraformNullPath() {
    // Null pass-through is the documented contract of the terraform helper.
    // Note: the Maven, Helm, and generic branches NPE on null today; the
    // production caller (createSearchTerm) guards non-null before dispatch,
    // so this assertion only locks in the contract for direct callers/tests.
    String result = component.convertPathToSearchTerm("terraform", null);
    assertThat(result, is(nullValue()));
  }

  @Test
  public void testConvertPathToSearchTerm_terraformPrefixOnly() {
    // /v1/modules with nothing after it can't yield a module id; expect the
    // cleaned fallback rather than an out-of-bounds slice.
    String result = component.convertPathToSearchTerm("terraform", "/v1/modules");
    assertThat(result, is("v1/modules"));
  }

  @Test
  public void testConvertPathToSearchTerm_terraformTooFewSegments() {
    // Five segments after cleaning is the minimum; four (no provider) must
    // hit the fallback rather than read past the array end.
    String result = component.convertPathToSearchTerm("terraform", "/v1/modules/myorg/mymod");
    assertThat(result, is("v1/modules/myorg/mymod"));
  }

  @Test
  public void testConvertPathToSearchTerm_terraformExactlyFiveSegments() {
    // Exactly five segments (no version, no filename) hits the >= 5 guard and
    // still returns {namespace}/{name}/{provider} rather than the cleaned-path
    // fallback. Unlikely in practice (the Terraform Registry HTTP API always
    // includes a version), but the guard explicitly allows it.
    String result = component.convertPathToSearchTerm("terraform", "/v1/modules/myorg/mymod/local");
    assertThat(result, is("myorg/mymod/local"));
  }
}
