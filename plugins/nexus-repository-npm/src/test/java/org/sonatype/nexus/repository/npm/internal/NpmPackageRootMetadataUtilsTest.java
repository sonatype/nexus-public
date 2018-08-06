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
package org.sonatype.nexus.repository.npm.internal;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.collect.NestedAttributesMap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.repository.npm.internal.NpmVersionComparator.extractNewestVersion;
import static org.sonatype.nexus.repository.npm.internal.NpmVersionComparator.extractAlwaysPackageVersion;
import static org.sonatype.nexus.repository.npm.internal.NpmVersionComparator.extractPackageRootVersionUnlessEmpty;

public class NpmPackageRootMetadataUtilsTest
{
  @Rule
  public TemporaryFolder tempFolderRule = new TemporaryFolder();

  @Test
  @SuppressWarnings("unchecked")
  public void createFullPackageMetadata() throws Exception {
    try {
      assertThat(BaseUrlHolder.isSet(), is(false));
      BaseUrlHolder.set("http://localhost:8080/");

      File packageJsonFile = new File(NpmPackageRootMetadataUtilsTest.class.getResource("package.json").toURI());
      File archive = tempFolderRule.newFile();
      Map<String, Object> packageJson = new NpmPackageParser()
          .parsePackageJson(() -> ArchiveUtils.pack(archive, packageJsonFile, "package/package.json"));

      NestedAttributesMap packageMetadata = NpmPackageRootMetadataUtils
          .createFullPackageMetadata(new NestedAttributesMap("metadata", packageJson),
              "npm-hosted",
              "abcd",
              null,
              extractAlwaysPackageVersion);

      assertThat(packageMetadata.get("_id"), is("@foo/bar"));
      assertThat(packageMetadata.get("author"), is("Foo Bar Authors"));
      assertThat(packageMetadata.get("bugs"), is("https://github.com/foo/bar/issues"));
      assertThat(packageMetadata.get("description"), is("I pity the foo"));
      assertThat(packageMetadata.get("homepage"), is("https://github.com/foo/bar"));
      assertThat(packageMetadata.get("license"), is("MIT"));
      assertThat(packageMetadata.get("name"), is("@foo/bar"));
      assertThat(packageMetadata.get("readme"), is("First 64k..."));
      assertThat(packageMetadata.get("readmeFilename"), is("package/README.md"));

      assertHuman(packageMetadata, "maintainers", "foo@example.com", "Foo Bar Maintainers");
      assertHuman(packageMetadata, "contributors", "foo@example.com", "Foo Bar Contributors");

      assertTrue(packageMetadata.contains("users")); // empty

      assertTrue(packageMetadata.child("time").contains("created"));
      assertTrue(packageMetadata.child("time").contains("modified"));
      assertTrue(packageMetadata.child("time").contains("1.5.3"));

      assertThat(packageMetadata.child("dist-tags").get("latest"), is("1.5.3"));
      assertThat((List<String>) packageMetadata.get("keywords", List.class), contains("Rocky"));

      assertThat(packageMetadata.child("repository").get("type"), is("git"));
      assertThat(packageMetadata.child("repository").get("url"), is("https://github.com/foo/bar.git"));

      // Version specific metadata
      NestedAttributesMap versionData = packageMetadata.child("versions").child("1.5.3");
      assertThat(versionData.get("_id"), is("@foo/bar@1.5.3"));
      assertThat(versionData.get("author"), is("Foo Bar Authors"));
      assertThat(versionData.get("deprecated"), is("Deprecated"));
      assertThat(versionData.get("description"), is("I pity the foo"));
      assertThat(versionData.get("license"), is("MIT"));
      assertThat(versionData.get("main"), is("index.js"));
      assertThat(versionData.get("name"), is("@foo/bar"));
      assertThat(versionData.get("readme"), is("First 64k..."));
      assertThat(versionData.get("readmeFilename"), is("package/README.md"));
      assertThat(versionData.get("version"), is("1.5.3"));
      assertThat(versionData.get("_hasShrinkwrap"), is(false));

      assertThat(versionData.child("bin").get("myapp"), is("./cli.js"));

      assertThat(versionData.child("directories").get("bin"), is("./bin"));

      assertThat(versionData.child("dist").get("shasum"), is("abcd"));
      assertThat(versionData.child("dist").get("tarball"),
          is("http://localhost:8080/repository/npm-hosted/@foo/bar/-/bar-1.5.3.tgz"));

      assertThat(versionData.child("engines").get("node"), is(">= 6.9.0"));
      assertThat(versionData.child("engines").get("npm"), is(">= 3.0.0"));

      assertHuman(versionData, "maintainers", "foo@example.com", "Foo Bar Maintainers");
      assertHuman(versionData, "contributors", "foo@example.com", "Foo Bar Contributors");

      assertDependencies(versionData, "dependencies", "dep", "1.0");
      assertDependencies(versionData, "optionalDependencies", "opt", "2.0");
      assertDependencies(versionData, "devDependencies", "dev", "3.0");
      assertDependencies(versionData, "bundleDependencies", "bundle", "4.0");
      assertDependencies(versionData, "peerDependencies", "peer", "5.0");
    }
    finally {
      BaseUrlHolder.unset();
    }
  }

  @SuppressWarnings("unchecked")
  private static void assertDependencies(final NestedAttributesMap metadata,
                                         final String fieldName,
                                         final String dep,
                                         final String depVersion)
  {
    assertTrue(metadata.contains(fieldName));
    Map<String, Object> dependencies = metadata.get(fieldName, Map.class);
    assertThat(dependencies.keySet(), hasSize(1));
    assertThat(dependencies.get(dep), is(depVersion));
  }

  @SuppressWarnings("unchecked")
  private static void assertHuman(final NestedAttributesMap parent,
                                  final String fieldName,
                                  final String email,
                                  final String name)
  {

    assertTrue(parent.contains(fieldName));
    List<Map<String, Object>> humans = parent.get(fieldName, List.class);
    assertThat(humans, hasSize(1));
    assertThat(humans.get(0).get("email"), is(email));
    assertThat(humans.get(0).get("name"), is(name));
  }
}
