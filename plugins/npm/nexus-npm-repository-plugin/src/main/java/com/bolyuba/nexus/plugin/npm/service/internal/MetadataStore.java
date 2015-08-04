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

import java.util.Iterator;
import java.util.List;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.bolyuba.nexus.plugin.npm.service.PackageRoot;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * Database.
 */
public interface MetadataStore
{
  /**
   * Returns the names of present packages in the store.
   */
  List<String> listPackageNames(NpmRepository repository);

  /**
   * Retrieves a package from store by name.
   */
  PackageRoot getPackageByName(NpmRepository repository, String packageName);

  /**
   * Deletes all packages from store, returns {@code true} if packages existed.
   *
   * @since 2.11.3
   */
  boolean deletePackages(NpmRepository repository);

  /**
   * Deletes package from store, returns {@code true} if package existed.
   */
  boolean deletePackageByName(NpmRepository repository, String packageName);

  /**
   * Replaces one single package, without merging it.
   */
  PackageRoot replacePackage(NpmRepository repository, PackageRoot packageRoot);

  /**
   * Updates one single package, merging it if necessary.
   *
   * @see PackageRoot#overlay(PackageRoot)
   */
  PackageRoot updatePackage(NpmRepository repository, PackageRoot packageRoot);

  /**
   * Massive update of packages, merging each of them if necessary.
   *
   * @see PackageRoot#overlay(PackageRoot)
   */
  int updatePackages(NpmRepository repository, Iterator<PackageRoot> packageRootIterator);

  /**
   * Massive update of packages, applying a function on them.
   */
  int updatePackages(NpmRepository repository, Predicate<PackageRoot> predicate,
                     Function<PackageRoot, PackageRoot> function);
}
