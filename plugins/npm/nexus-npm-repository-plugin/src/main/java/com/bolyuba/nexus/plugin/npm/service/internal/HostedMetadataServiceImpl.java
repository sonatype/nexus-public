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

import org.sonatype.nexus.proxy.item.ContentLocator;

import com.bolyuba.nexus.plugin.npm.hosted.NpmHostedRepository;
import com.bolyuba.nexus.plugin.npm.service.HostedMetadataService;
import com.bolyuba.nexus.plugin.npm.service.PackageRequest;
import com.bolyuba.nexus.plugin.npm.service.PackageRoot;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link HostedMetadataService} implementation.
 */
public class HostedMetadataServiceImpl
    extends GeneratorWithStoreSupport<NpmHostedRepository>
    implements HostedMetadataService
{
  public HostedMetadataServiceImpl(final NpmHostedRepository npmHostedRepository,
                                   final MetadataStore metadataStore,
                                   final MetadataParser metadataParser)
  {
    super(npmHostedRepository, metadataParser, metadataStore);
  }

  @Override
  public boolean deleteAllMetadata() {
    log.info("Deleting all hosted npm metadata from {}", getNpmRepository().getId());
    return metadataStore.deletePackages(getNpmRepository());
  }

  @Override
  public boolean deletePackage(final String packageName) {
    checkNotNull(packageName, "null package name");
    log.info("Deleting hosted npm package {} metadata from {}", packageName, getNpmRepository().getId());
    return metadataStore.deletePackageByName(getNpmRepository(), packageName);
  }

  @Override
  public PackageRoot parsePackageRoot(final PackageRequest request, final ContentLocator contentLocator)
      throws IOException
  {
    checkArgument(request.isPackageRoot(), "Package root request expected, but got %s",
        request.getPath());
    final PackageRoot packageRoot = metadataParser.parsePackageRoot(getNpmRepository().getId(), contentLocator);
    checkArgument(request.getName().equals(packageRoot.getName()),
        "Package root name '%s' and parsed content name '%s' mismatch", request.getName(), packageRoot.getName());
    checkArgument(!packageRoot.isIncomplete(), "Incomplete package root parsed");
    return packageRoot;
  }

  /**
   * Note: this happens within exclusive lock.
   */
  @Override
  public PackageRoot consumePackageRoot(final PackageRoot packageRoot)
      throws IOException
  {
    PackageRoot existing = metadataStore.getPackageByName(getNpmRepository(), packageRoot.getName());
    if (existing != null) {
      existing.overlay(packageRoot);
    }
    else {
      existing = packageRoot;
    }
    existing.maintainTime(); // maintain time, as we are hosted repo service
    return metadataStore.updatePackage(getNpmRepository(), existing);
  }

  @Nullable
  @Override
  protected PackageRoot doGeneratePackageRoot(final PackageRequest request) throws IOException {
    return metadataStore.getPackageByName(getNpmRepository(), request.getName());
  }
}
