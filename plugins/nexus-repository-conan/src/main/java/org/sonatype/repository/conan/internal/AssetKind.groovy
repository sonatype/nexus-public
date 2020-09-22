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
package org.sonatype.repository.conan.internal

import org.sonatype.nexus.repository.cache.CacheControllerHolder.CacheType

import static org.sonatype.nexus.repository.cache.CacheControllerHolder.METADATA

/**
 * @since 3.next
 */
enum AssetKind {
  DOWNLOAD_URL(METADATA, "download_urls"),
  DIGEST(METADATA, "digest"),
  CONAN_MANIFEST(METADATA, "conanmanifest.txt"),
  CONAN_FILE(METADATA, "conanfile.py"),
  CONAN_INFO(METADATA, "conaninfo.txt"),
  CONAN_PACKAGE(METADATA, "conan_package.tgz"),
  CONAN_SOURCES(METADATA, "conan_sources.tgz"),
  CONAN_EXPORT(METADATA, "conan_export.tgz"),
  CONAN_PACKAGE_SNAPSHOT(METADATA, null)

  private final CacheType cacheType

  private final String filename

  AssetKind(final CacheType cacheType, final String filename) {
    this.cacheType = cacheType
    this.filename = filename
  }

  CacheType getCacheType() {
    return cacheType
  }

  String getFilename() {
    return filename
  }

  static AssetKind valueFromFileName(final String filename) {
    values().find { it.filename == filename }
  }
}
