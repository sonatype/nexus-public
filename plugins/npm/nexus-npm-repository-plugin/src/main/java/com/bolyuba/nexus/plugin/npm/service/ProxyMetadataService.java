/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.service;

import java.io.IOException;

import javax.annotation.Nullable;

import org.sonatype.nexus.proxy.ResourceStoreRequest;

import com.bolyuba.nexus.plugin.npm.service.tarball.TarballRequest;

/**
 * Metadata service for proxy repositories. Component generating NPM metadata from underlying store to
 * be sent downstream for consumption by NPM CLI or alike. Still, unlike "plain" generator, this
 * component ensures first that the store contains up to date data and serves that.
 */
public interface ProxyMetadataService
    extends Generator
{
  /**
   * Deletes all metadata cached by this service from the underlying store.
   *
   * @since 2.11.3
   */
  boolean deleteAllMetadata();

  /**
   * Deletes package metadata cached by this service from underlying store, returns {@code true} if package existed.
   *
   * @since 2.11.3
   */
  boolean deletePackage(String packageName);

  /**
   * Expires proxy metadata cache. On next request of an expired metadata, re-fetch will be done from registry.
   */
  boolean expireMetadataCaches(PackageRequest request);

  /**
   * Updates package root in metadata store. To be used mostly by proxy mechanism to store some extra properties,
   * not to modify actual metadata.
   */
  PackageRoot consumeRawPackageRoot(PackageRoot packageRoot) throws IOException;

  /**
   * Creates a {@link TarballRequest} out of a {@link ResourceStoreRequest}, if applicable. It relies on metadata store
   * to find out what package and what version of it is requested. If the request does not meet formal criteria (ie.
   * request path is not for a tarball request), or no package or corresponding version is found, {@code null} is
   * returned to signal that request is not a tarball request.
   */
  @Nullable
  TarballRequest createTarballRequest(ResourceStoreRequest request) throws IOException;
}
