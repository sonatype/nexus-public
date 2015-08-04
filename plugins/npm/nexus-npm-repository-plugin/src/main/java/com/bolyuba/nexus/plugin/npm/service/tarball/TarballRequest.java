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
package com.bolyuba.nexus.plugin.npm.service.tarball;

import org.sonatype.nexus.proxy.ResourceStoreRequest;

import com.bolyuba.nexus.plugin.npm.service.PackageRoot;
import com.bolyuba.nexus.plugin.npm.service.PackageVersion;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class encapsulating a NPM Tarball request.
 */
public class TarballRequest
{
  private final ResourceStoreRequest resourceStoreRequest;

  private final PackageRoot packageRoot;

  private final PackageVersion packageVersion;

  public TarballRequest(final ResourceStoreRequest resourceStoreRequest, final PackageRoot packageRoot,
                        final PackageVersion packageVersion)
  {
    this.resourceStoreRequest = checkNotNull(resourceStoreRequest);
    this.packageRoot = checkNotNull(packageRoot);
    this.packageVersion = checkNotNull(packageVersion);

    checkArgument(!packageVersion.isIncomplete(), "Incomplete tarball %s cannot be requested",
        resourceStoreRequest.getRequestPath());
  }

  public ResourceStoreRequest getResourceStoreRequest() {
    return resourceStoreRequest;
  }

  public PackageRoot getPackageRoot() {
    return packageRoot;
  }

  public PackageVersion getPackageVersion() {
    return packageVersion;
  }
}
