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
package com.bolyuba.nexus.plugin.npm.service.internal;

import java.io.IOException;

import javax.annotation.Nullable;

import com.bolyuba.nexus.plugin.npm.proxy.NpmProxyRepository;
import com.bolyuba.nexus.plugin.npm.service.PackageRoot;

/**
 * Transport for NPM Metadata.
 */
public interface ProxyMetadataTransport
{
  /**
   * Fetches remote registry root of the proxied {@link NpmProxyRepository}. The returned iterator MUST BE handled as
   * resource, as it incrementally parsing a potentially huge JSON document!
   */
  PackageRootIterator fetchRegistryRoot(final NpmProxyRepository npmProxyRepository) throws IOException;

  /**
   * Fetches one single package root of the proxied {@link NpmProxyRepository}. Supplied repository and package name
   * must not be {@code null}s, while the expired packageRoot might be {@code null}. If present, metadata from it
   * like ETag will be used to make a "conditional GET", and if remote unchanged, the passed in expired instance
   * is returned. If package not found or cannot be fetched for any reason, {@code null} is returned.
   */
  @Nullable
  PackageRoot fetchPackageRoot(final NpmProxyRepository npmProxyRepository, final String packageName,
                               final @Nullable PackageRoot expired) throws IOException;
}
