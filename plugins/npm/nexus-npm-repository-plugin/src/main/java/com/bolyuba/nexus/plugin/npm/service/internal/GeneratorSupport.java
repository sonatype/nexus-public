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

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.web.BaseUrlHolder;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.common.SimpleFormat;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.bolyuba.nexus.plugin.npm.service.Generator;
import com.bolyuba.nexus.plugin.npm.service.PackageRequest;
import com.bolyuba.nexus.plugin.npm.service.PackageRoot;
import com.bolyuba.nexus.plugin.npm.service.PackageVersion;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link Generator} support class.
 */
public abstract class GeneratorSupport<R extends NpmRepository>
    extends ComponentSupport
    implements Generator
{
  private final R npmRepository;

  protected final MetadataParser metadataParser;

  protected GeneratorSupport(final R npmRepository,
                             final MetadataParser metadataParser)
  {
    this.npmRepository = checkNotNull(npmRepository);
    this.metadataParser = checkNotNull(metadataParser);
  }

  protected R getNpmRepository() {
    return npmRepository;
  }

  @Override
  public boolean isNpmMetadataServiced(final ResourceStoreRequest request) {
    if (RepositoryItemUid.PATH_ROOT.equals(request.getRequestPath()) // root
        || (request.isExternal() && request.getRequestUrl().contains("/service/local/") && request.getRequestUrl().contains("/content/")) // UI Browse Storage
        || request.getRequestContext().containsKey(NpmRepository.NPM_METADATA_NO_SERVICE, false)) {
      // shut down NPM MD+tarball service completely
      return false;
    }
    return true;
  }

  @Override
  public ContentLocator produceRegistryRoot(final PackageRequest request) throws IOException {
    return metadataParser.produceRegistryRoot(generateRegistryRoot(request));
  }

  @Nullable
  @Override
  public ContentLocator producePackageRoot(final PackageRequest request) throws IOException {
    final PackageRoot root = generatePackageRoot(request);
    if (root == null) {
      return null;
    }
    filterPackageRoot(root);
    return metadataParser.producePackageRoot(root);
  }

  @Nullable
  @Override
  public ContentLocator producePackageVersion(final PackageRequest request) throws IOException {
    final PackageVersion version = generatePackageVersion(request);
    if (version == null || version.isIncomplete()) {
      return null;
    }
    filterPackageVersion(version);
    return metadataParser.producePackageVersion(version);
  }

  @Override
  public PackageRootIterator generateRegistryRoot(final PackageRequest request) throws IOException {
    return doGenerateRegistryRoot(request);
  }

  protected abstract PackageRootIterator doGenerateRegistryRoot(final PackageRequest request) throws IOException;

  @Nullable
  @Override
  public PackageRoot generatePackageRoot(final PackageRequest request) throws IOException {
    checkArgument(request.isPackageRoot(), "Package root request expected, but got %s",
        request.getPath());
    final PackageRoot root = doGeneratePackageRoot(request);
    if (root == null) {
      return null;
    }
    filterPackageRootDist(request, root);
    return root;
  }

  @Nullable
  protected abstract PackageRoot doGeneratePackageRoot(final PackageRequest request) throws IOException;

  @Nullable
  @Override
  public PackageVersion generatePackageVersion(final PackageRequest request) throws IOException {
    checkArgument(request.isPackageVersion(), "Package version request expected, but got %s",
        request.getPath());
    final PackageVersion version = doGeneratePackageVersion(request);
    if (version == null) {
      return null;
    }
    filterPackageVersionDist(request, version);
    return version;
  }

  @Nullable
  protected PackageVersion doGeneratePackageVersion(final PackageRequest request) throws IOException {
    checkArgument(request.isPackageVersion(), "Package version request expected, but got %s",
        request.getPath());
    final PackageRoot root = doGeneratePackageRoot(request);
    if (root == null || root.isUnpublished()) {
      return null;
    }
    final PackageVersion version = root.getVersions().get(request.getVersion());
    if (version == null || version.isIncomplete()) {
      return null;
    }
    return version;
  }

  // ==

  /**
   * Removes unwanted fields from JSON document and invokes {@link #filterPackageVersion(PackageVersion)} on each
   * version of the passed in package root.
   */
  protected void filterPackageRoot(final PackageRoot packageRoot) {
    packageRoot.getRaw().remove("_id"); // TODO: why? Original code did this too
    packageRoot.getRaw().remove("_rev"); // TODO: why? Original code did this too
    for (PackageVersion packageVersion : packageRoot.getVersions().values()) {
      filterPackageVersion(packageVersion);
    }
  }

  /**
   * Removes unwanted fields from JSON document.
   */
  protected void filterPackageVersion(final PackageVersion packageVersion) {
    packageVersion.getRaw().remove("_id"); // TODO: why? Original code did this too
    packageVersion.getRaw().remove("_rev"); // TODO: why? Original code did this too
  }

  /**
   * Invokes {@link #filterPackageVersionDist(PackageRequest, PackageVersion)} on each version of the passed in package
   * root.
   */
  protected void filterPackageRootDist(final PackageRequest packageRequest, final PackageRoot packageRoot) {
    for (PackageVersion packageVersion : packageRoot.getVersions().values()) {
      filterPackageVersionDist(packageRequest, packageVersion);
    }
  }

  /**
   * Overwrites, hence, modifies the package version document by setting the tarball URL and shasums to expected
   * values. Package version document modified with this method should NOT be saved back into store, they should be
   * sent for downstream consumption only!
   */
  protected void filterPackageVersionDist(final PackageRequest packageRequest, final PackageVersion packageVersion) {
    if (npmRepository.adaptToFacet(GroupRepository.class) != null) {
      packageVersion.setDistTarball(SimpleFormat
          .format("%s/content/groups/%s/%s/-/%s", BaseUrlHolder.get(), npmRepository.getId(),
              packageVersion.getName(), packageVersion.getDistTarballFilename()));
    }
    else {
      packageVersion.setDistTarball(SimpleFormat
          .format("%s/content/repositories/%s/%s/-/%s", BaseUrlHolder.get(), npmRepository.getId(),
              packageVersion.getName(), packageVersion.getDistTarballFilename()));
    }
    final String versionTarballShasum = PackageVersion.createShasumVersionKey(packageVersion.getVersion());
    if (packageVersion.getRoot().getProperties().containsKey(versionTarballShasum)) {
      // this publishes proper SHA1 for ALL packages already proxies by NX
      packageVersion.setDistShasum(packageVersion.getRoot().getProperties().get(versionTarballShasum));
    }
  }
}
