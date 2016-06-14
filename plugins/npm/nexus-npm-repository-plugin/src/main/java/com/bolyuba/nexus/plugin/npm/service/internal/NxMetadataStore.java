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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.attributes.Attributes;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.uid.IsHiddenAttribute;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.bolyuba.nexus.plugin.npm.service.PackageRoot;
import com.google.common.base.Throwables;

import static com.bolyuba.nexus.plugin.npm.service.PackageRoot.PROP_CACHED;
import static com.bolyuba.nexus.plugin.npm.service.PackageRoot.PROP_EXPIRED;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * NX storage backed {@link MetadataStore} implementation.
 */
@Singleton
@Named("default")
public class NxMetadataStore
    extends ComponentSupport
    implements MetadataStore, EventSubscriber
{
  private static final String NPM_ATTRIBUTE_PREFIX = "npm.";

  private final MetadataParser metadataParser;

  @Inject
  public NxMetadataStore(final MetadataParser metadataParser) {
    this.metadataParser = checkNotNull(metadataParser);
  }

  @Override
  public List<String> listPackageNames(final NpmRepository repository) {
    checkNotNull(repository);

    ArrayList<String> result = new ArrayList<>();
    List<String> firstLevelNames = listCollNamesOnPath(repository, "/");
    for (String firstLevelName : firstLevelNames) {
      if (firstLevelName.startsWith("@")) {
        // we have scoped name "@scope/name", so go level deeper
        List<String> secondLevelNames = listCollNamesOnPath(repository, "/" + firstLevelName);
        for (String secondLevelName : secondLevelNames) {
          result.add(firstLevelName + "/" + secondLevelName);
        }
      }
      else {
        result.add(firstLevelName);
      }
    }
    return result;
  }

  @Override
  public PackageRoot getPackageByName(final NpmRepository repository, final String packageName) {
    checkNotNull(repository);
    checkNotNull(packageName);
    try {
      StorageFileItem file = (StorageFileItem) repository.retrieveItem(true, packageJsonRequest(packageName));

      PackageRoot packageRoot = metadataParser.parsePackageRootAny(repository.getId(), file.getContentLocator());
      for (Map.Entry<String, String> attrentry : file.getRepositoryItemAttributes().asMap().entrySet()) {
        if (attrentry.getKey().startsWith(NPM_ATTRIBUTE_PREFIX)) {
          packageRoot.getProperties().put(
              attrentry.getKey().substring(NPM_ATTRIBUTE_PREFIX.length()),
              attrentry.getValue()
          );
        }
      }
      // these two must come from core ones, possibly overriding copied ones
      packageRoot.getProperties().put(PROP_EXPIRED, Boolean.valueOf(file.isExpired()).toString());
      packageRoot.getProperties().put(PROP_CACHED, Long.toString(file.getRepositoryItemAttributes().getStoredLocally()));
      return packageRoot;
    }
    catch (ItemNotFoundException e) {
      return null;
    }
    catch (IOException | IllegalOperationException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public boolean deletePackages(final NpmRepository repository) {
    checkNotNull(repository);

    try {
      repository.deleteItem(true, new ResourceStoreRequest("/"));
      return true;
    }
    catch (ItemNotFoundException e) {
      return false;
    }
    catch (IOException | UnsupportedStorageOperationException | IllegalOperationException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public boolean deletePackageByName(final NpmRepository repository, final String packageName) {
    checkNotNull(repository);
    checkNotNull(packageName);
    try {
      repository.deleteItem(true, packageDirectoryRequest(packageName));
      return true;
    }
    catch (ItemNotFoundException e) {
      return false;
    }
    catch (IOException | UnsupportedStorageOperationException | IllegalOperationException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public PackageRoot replacePackage(final NpmRepository repository, final PackageRoot packageRoot) {
    checkNotNull(repository);
    checkNotNull(packageRoot);
    try {
      StorageFileItem file = new DefaultStorageFileItem(
          repository,
          packageJsonRequest(packageRoot.getName()),
          true,
          true,
          metadataParser.producePackageRoot(packageRoot)
      );
      final Attributes attributes = file.getRepositoryItemAttributes();
      for (Map.Entry<String, String> e : packageRoot.getProperties().entrySet()) {
        attributes.put(NPM_ATTRIBUTE_PREFIX + e.getKey(), e.getValue());
      }
      // these two are special
      attributes.setExpired(Boolean.TRUE.toString().equals(packageRoot.getProperties().get(PROP_EXPIRED)));
      if (packageRoot.getProperties().containsKey(PROP_CACHED)) {
        attributes.setStoredLocally(Long.valueOf(packageRoot.getProperties().get(PROP_CACHED)));
      }
      repository.storeItem(true, file);
      return packageRoot;
    }
    catch (IOException | UnsupportedStorageOperationException | IllegalOperationException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public PackageRoot updatePackage(final NpmRepository repository, final PackageRoot packageRoot) {
    checkNotNull(repository);
    checkNotNull(packageRoot);

    PackageRoot existing = getPackageByName(repository, packageRoot.getName());
    if (existing != null) {
      existing.overlay(packageRoot);
      return replacePackage(repository, existing);
    }
    else {
      return replacePackage(repository, packageRoot);
    }
  }

  @Override
  public int updatePackages(final NpmRepository repository, final Iterator<PackageRoot> packageRootIterator) {
    checkNotNull(repository);
    checkNotNull(packageRootIterator);
    int count = 0;
    while (packageRootIterator.hasNext()) {
      final PackageRoot packageRoot = packageRootIterator.next();
      updatePackage(repository, packageRoot);
      count++;
    }
    return count;
  }

  private ResourceStoreRequest packageDirectoryRequest(final String packageName) {
    return request("/" + packageName);
  }

  private ResourceStoreRequest packageJsonRequest(final String packageName) {
    return request("/" + packageName + "/package.json");
  }

  private ResourceStoreRequest request(final String path) {
    ResourceStoreRequest request = new ResourceStoreRequest(path);
    request.getRequestContext().put(NpmRepository.NPM_METADATA_NO_SERVICE, Boolean.TRUE);
    return request;
  }

  /**
   * Lists directories on given path, used by {@link #listPackageNames(NpmRepository)}.
   */
  private List<String> listCollNamesOnPath(final NpmRepository repository, final String path) {
    ArrayList<String> result = new ArrayList<>();
    try {
      Collection<StorageItem> items = repository.list(true, request(path));
      for (StorageItem item : items) {
        if (!item.getRepositoryItemUid().getBooleanAttributeValue(IsHiddenAttribute.class)
            && item instanceof StorageCollectionItem) {
          result.add(item.getName());
        }
      }
      return result;
    }
    catch (ItemNotFoundException e) {
      return result;
    }
    catch (IOException | IllegalOperationException e) {
      throw Throwables.propagate(e);
    }
  }
}
