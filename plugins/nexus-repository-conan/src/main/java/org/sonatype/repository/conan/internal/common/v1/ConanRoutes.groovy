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
package org.sonatype.repository.conan.internal.common.v1

import org.sonatype.nexus.repository.view.Route.Builder
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher
import static org.sonatype.nexus.repository.http.HttpMethods.GET
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD
import static org.sonatype.nexus.repository.http.HttpMethods.POST
import static org.sonatype.nexus.repository.http.HttpMethods.PUT
import static org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers.and
import static org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers.or
import static org.sonatype.repository.conan.internal.metadata.ConanMetadata.GROUP
import static org.sonatype.repository.conan.internal.metadata.ConanMetadata.PROJECT
import static org.sonatype.repository.conan.internal.metadata.ConanMetadata.STATE
import static org.sonatype.repository.conan.internal.metadata.ConanMetadata.VERSION

/**
 * @since 3.next
 */
class ConanRoutes
{
  protected static String DOWNLOAD_FORM = "{${PROJECT}:.+}/{${VERSION}:.+}/{${GROUP}:.+}/{${STATE}:.+}"

  protected static String STANDARD_FORM = "{${GROUP}:.+}/{${PROJECT}:.+}/{${VERSION}:.+}/{${STATE}:.+}"

  /**
   * Matches on urls ending with upload_urls
   */
  static Builder uploadUrls() {
    new Builder().matcher(
        and(
            new ActionMatcher(POST),
            or(
                new TokenMatcher("{prefix:.*}/{path:.*}/${DOWNLOAD_FORM}/packages/{sha:.+}/upload_urls"),
                new TokenMatcher("{prefix:.*}/{path:.*}/${DOWNLOAD_FORM}/upload_urls")
            )
        )
    )
  }

  static Builder uploadManifest() {
    new Builder().matcher(
        and(
            new ActionMatcher(PUT),
            or(
                conanManifestPackagesMatcher(),
                conanManifestMatcher()
            )
        )
    )
  }

  static Builder uploadConanfile() {
    new Builder().matcher(
        and(
            new ActionMatcher(PUT),
            or(
                conanFilePackagesMatcher(),
                conanFileMatcher()
            )
        )
    )
  }

  static Builder uploadConanInfo() {
    new Builder().matcher(
        and(
            new ActionMatcher(PUT),
            or(
                conanInfoPackagesMatcher(),
                conanInfoMatcher()
            )
        )
    )
  }

  static Builder uploadConanPackageZip() {
    new Builder().matcher(
        and(
            new ActionMatcher(PUT),
            conanPackageMatcher()
        )
    )
  }

  static Builder uploadConanSource() {
    new Builder().matcher(
        and(
            new ActionMatcher(PUT),
            conanSourceMatcher()
        )
    )
  }

  static Builder uploadConanExportZip() {
    new Builder().matcher(
        and(
            new ActionMatcher(PUT),
            conanExportMatcher()
        )
    )
  }

  /**
   * Matches on urls ending with download_urls
   * @return matcher for initial and package download_urls endpoints
   */
  static Builder downloadUrls() {
    return new Builder().matcher(
        and(
            new ActionMatcher(GET, HEAD),
            or(
                downloadUrlsPackagesMatcher(),
                downloadUrlsMatcher()
            )
        )
    )
  }

  private static TokenMatcher downloadUrlsMatcher() {
    return new TokenMatcher("{prefix:.*}/{path:.*}/${DOWNLOAD_FORM}/download_urls")
  }

  private static TokenMatcher downloadUrlsPackagesMatcher() {
    return new TokenMatcher("{prefix:.*}/{path:.*}/${DOWNLOAD_FORM}/packages/{sha:.+}/download_urls")
  }

  static Builder digest() {
    return new Builder().matcher(
        and(
            new ActionMatcher(GET, HEAD),
            or(
                digestPackagesMatcher(),
                digestMatcher()
            )
        )
    )
  }

  private static TokenMatcher digestMatcher() {
    return new TokenMatcher("{prefix:.*}/{path:.*}/${DOWNLOAD_FORM}/digest")
  }

  private static TokenMatcher digestPackagesMatcher() {
    return new TokenMatcher("{prefix:.*}/{path:.*}/${DOWNLOAD_FORM}/packages/{sha:.+}/digest")
  }

  /**
   * Matches on the manifest files
   * @return matcher for initial and package conanmanifest.txt
   */
  static Builder conanManifest() {
    new Builder().matcher(
        and(
            new ActionMatcher(GET, HEAD),
            or(
                conanManifestPackagesMatcher(),
                conanManifestMatcher()
            )
        )
    )
  }

  private static TokenMatcher conanManifestMatcher() {
    new TokenMatcher("{prefix:.*}/{path:.*}/${STANDARD_FORM}/conanmanifest.txt")
  }

  private static TokenMatcher conanManifestPackagesMatcher() {
    new TokenMatcher("{prefix:.*}/{path:.*}/${STANDARD_FORM}/packages/{sha:.+}/conanmanifest.txt")
  }

  /**
   * Matches on conanfile.py
   * @return matcher for conanfile.py
   */
  static Builder conanFile() {
    new Builder().matcher(
        and(
            new ActionMatcher(GET, HEAD),
            conanFileMatcher()
        )
    )
  }

  private static TokenMatcher conanFileMatcher() {
    new TokenMatcher("{prefix:.*}/{path:.*}/${STANDARD_FORM}/conanfile.py")
  }

  private static TokenMatcher conanFilePackagesMatcher() {
    new TokenMatcher("{prefix:.*}/{path:.*}/${STANDARD_FORM}/packages/{sha:.+}/conanfile.py")
  }

  /**
   * Matches on conaninfo.txt
   * @return matcher for conaninfo.txt
   */
  static Builder conanInfo() {
    new Builder().matcher(
        and(
            new ActionMatcher(GET, HEAD),
            conanInfoPackagesMatcher()
        )
    )
  }

  private static TokenMatcher conanInfoMatcher() {
    new TokenMatcher("{prefix:.*}/{path:.*}/${STANDARD_FORM}/conaninfo.txt")
  }

  private static TokenMatcher conanInfoPackagesMatcher() {
    new TokenMatcher("{prefix:.*}/{path:.*}/${STANDARD_FORM}/packages/{sha:.+}/conaninfo.txt")
  }

  /**
   * Matches on conan_package.tgz
   * @return matcher for conan_package.tgz
   */
  static Builder conanPackage() {
    new Builder().matcher(
        and(
            new ActionMatcher(GET, HEAD),
            conanPackageMatcher()
        )
    )
  }

  static Builder conanSource() {
    new Builder().matcher(
        and(
            new ActionMatcher(GET, HEAD),
            new TokenMatcher("{prefix:.*}/{path:.*}/${STANDARD_FORM}/conan_sources.tgz")
        )
    )
  }

  static Builder conanExport() {
    new Builder().matcher(
        and(
            new ActionMatcher(GET, HEAD),
            new TokenMatcher("{prefix:.*}/{path:.*}/${STANDARD_FORM}/conan_export.tgz")
        )
    )
  }

  static Builder packageSnapshot() {
    new Builder().matcher(
        and(
            new ActionMatcher(HEAD, GET),
            new TokenMatcher("{prefix:.*}/{path:.*}/${DOWNLOAD_FORM}/packages/{sha:.+}")
        )
    )
  }

  private static TokenMatcher conanPackageMatcher() {
    new TokenMatcher("{prefix:.*}/{path:.*}/${STANDARD_FORM}/packages/{sha:.+}/conan_package.tgz")
  }

  private static TokenMatcher conanSourceMatcher() {
    new TokenMatcher("{prefix:.*}/{path:.*}/${STANDARD_FORM}/conan_sources.tgz")
  }

  private static TokenMatcher conanExportMatcher() {
    new TokenMatcher("{prefix:.*}/{path:.*}/${STANDARD_FORM}/conan_export.tgz")
  }

  static Builder searchQuery() {
    new Builder().matcher(
        and(
            new ActionMatcher(GET),
            new TokenMatcher("{prefix:.*}/{path:.*}/search")
        )
    )
  }
}
