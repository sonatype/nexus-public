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
package org.sonatype.nexus.repository.apt.internal;

import java.util.List;

import org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem.ContentSpecifier;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.sonatype.nexus.repository.apt.internal.AptFacetHelper.getByHashContentSpecifier;
import static org.sonatype.nexus.repository.apt.internal.AptFacetHelper.getReleasePackageIndexes;
import static org.sonatype.nexus.repository.apt.internal.AptFacetHelper.resolveMetadataRole;
import static org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem.Role.METADATA_BZ2;
import static org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem.Role.METADATA_GZ;
import static org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem.Role.METADATA_RAW;
import static org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem.Role.METADATA_XZ;
import static org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem.Role.PACKAGE_INDEX_GZ;
import static org.sonatype.nexus.repository.apt.internal.snapshot.SnapshotItem.Role.PACKAGE_INDEX_RAW;

class AptFacetHelperTest
{
  // --- resolveMetadataRole ---

  @Test
  void resolveMetadataRole_gz_returnsMetadataGz() {
    assertThat(resolveMetadataRole("main/i18n/Translation-en.gz"), is(METADATA_GZ));
  }

  @Test
  void resolveMetadataRole_bz2_returnsMetadataBz2() {
    assertThat(resolveMetadataRole("main/i18n/Translation-en.bz2"), is(METADATA_BZ2));
  }

  @Test
  void resolveMetadataRole_xz_returnsMetadataXz() {
    assertThat(resolveMetadataRole("main/i18n/Translation-en.xz"), is(METADATA_XZ));
  }

  @Test
  void resolveMetadataRole_noExtension_returnsMetadataRaw() {
    assertThat(resolveMetadataRole("main/i18n/Translation-en"), is(METADATA_RAW));
  }

  @Test
  void resolveMetadataRole_dep11YmlGz_returnsMetadataGz() {
    assertThat(resolveMetadataRole("main/dep11/Components-amd64.yml.gz"), is(METADATA_GZ));
  }

  @Test
  void resolveMetadataRole_sourcesGz_returnsMetadataGz() {
    assertThat(resolveMetadataRole("main/source/Sources.gz"), is(METADATA_GZ));
  }

  @Test
  void resolveMetadataRole_contentsGz_returnsMetadataGz() {
    assertThat(resolveMetadataRole("Contents-amd64.gz"), is(METADATA_GZ));
  }

  @Test
  void resolveMetadataRole_plainContents_returnsMetadataRaw() {
    assertThat(resolveMetadataRole("Contents-amd64"), is(METADATA_RAW));
  }

  // --- getByHashContentSpecifier ---

  @Test
  void getByHashContentSpecifier_buildsCorrectPath() {
    ContentSpecifier spec =
        getByHashContentSpecifier("focal", "main", "amd64", "SHA256", "abc123def456", PACKAGE_INDEX_RAW);
    assertThat(spec.path, equalTo("dists/focal/main/binary-amd64/by-hash/SHA256/abc123def456"));
  }

  @Test
  void getByHashContentSpecifier_normalizesAlgorithmToUpperCase() {
    ContentSpecifier lower =
        getByHashContentSpecifier("focal", "main", "amd64", "sha256", "abc123", PACKAGE_INDEX_RAW);
    ContentSpecifier mixed =
        getByHashContentSpecifier("focal", "main", "amd64", "Sha256", "abc123", PACKAGE_INDEX_RAW);
    assertThat(lower.path, equalTo("dists/focal/main/binary-amd64/by-hash/SHA256/abc123"));
    assertThat(mixed.path, equalTo("dists/focal/main/binary-amd64/by-hash/SHA256/abc123"));
  }

  @Test
  void getByHashContentSpecifier_storesComponentArchAndRole() {
    ContentSpecifier spec =
        getByHashContentSpecifier("focal", "main", "amd64", "MD5", "abcdef", PACKAGE_INDEX_GZ);
    assertThat(spec.component, equalTo("main"));
    assertThat(spec.architecture, equalTo("amd64"));
    assertThat(spec.role, is(PACKAGE_INDEX_GZ));
  }

  // --- getReleasePackageIndexes ---

  @Test
  void getReleasePackageIndexes_nonFlat_storesComponentAndArch() {
    List<ContentSpecifier> specs = getReleasePackageIndexes(false, "focal", "main", "amd64");
    assertThat("Should have 4 package index specifiers", specs.size(), is(4));
    specs.forEach(spec -> {
      assertThat("Non-flat specifier should store component", spec.component, equalTo("main"));
      assertThat("Non-flat specifier should store arch", spec.architecture, equalTo("amd64"));
    });
  }

  @Test
  void getReleasePackageIndexes_flat_hasNullComponentAndArch() {
    List<ContentSpecifier> specs = getReleasePackageIndexes(true, "focal", null, null);
    assertThat("Should have 4 package index specifiers", specs.size(), is(4));
    specs.forEach(spec -> {
      assertThat("Flat specifier should have null component", spec.component, nullValue());
      assertThat("Flat specifier should have null arch", spec.architecture, nullValue());
    });
  }
}
