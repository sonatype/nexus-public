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

/**
 * Descriptions for all repository recipes (format + type combinations).
 * Used by RepositoryTypeSelector to display helpful context about each repository type.
 */
export const RECIPE_DESCRIPTIONS: Record<string, string> = {
  // Maven
  'maven2-hosted': 'Store your own Maven artifacts in a private repository.',
  'maven2-proxy': 'Cache Maven artifacts from a remote repository like Maven Central.',
  'maven2-group': 'Combine multiple Maven repositories into a single URL.',

  // Docker
  'docker-hosted': 'Store your own Docker images in a private registry.',
  'docker-proxy': 'Cache Docker images from a remote registry like Docker Hub.',
  'docker-group': 'Combine multiple Docker registries into a single URL.',

  // npm
  'npm-hosted': 'Store your own npm packages in a private registry.',
  'npm-proxy': 'Cache npm packages from a remote registry like npmjs.org.',
  'npm-group': 'Combine multiple npm registries into a single URL.',

  // NuGet
  'nuget-hosted': 'Store your own NuGet packages in a private repository.',
  'nuget-proxy': 'Cache NuGet packages from a remote repository like nuget.org.',
  'nuget-group': 'Combine multiple NuGet repositories into a single URL.',

  // PyPI
  'pypi-hosted': 'Store your own Python packages in a private repository.',
  'pypi-proxy': 'Cache Python packages from a remote repository like PyPI.',
  'pypi-group': 'Combine multiple PyPI repositories into a single URL.',

  // RubyGems
  'rubygems-hosted': 'Store your own Ruby gems in a private repository.',
  'rubygems-proxy': 'Cache Ruby gems from a remote repository like rubygems.org.',
  'rubygems-group': 'Combine multiple RubyGems repositories into a single URL.',

  // Yum
  'yum-hosted': 'Store your own RPM packages in a private Yum repository.',
  'yum-proxy': 'Cache RPM packages from a remote Yum repository.',
  'yum-group': 'Combine multiple Yum repositories into a single URL.',

  // APT
  'apt-hosted': 'Store your own Debian packages in a private APT repository.',
  'apt-proxy': 'Cache Debian packages from a remote APT repository.',

  // Alpine
  'alpine-hosted': 'Store your own Alpine Linux packages in a private repository.',
  'alpine-proxy': 'Cache Alpine Linux packages from a remote repository.',
  'alpine-group': 'Combine multiple Alpine repositories into a single URL.',

  // Helm
  'helm-hosted': 'Store your own Helm charts in a private repository.',
  'helm-proxy': 'Cache Helm charts from a remote repository.',

  // Raw
  'raw-hosted': 'Store any binary files in a private repository without specific format requirements.',
  'raw-proxy': 'Cache binary files from a remote location.',
  'raw-group': 'Combine multiple raw repositories into a single URL.',

  // Go
  'go-proxy': 'Cache Go modules from a remote repository.',
  'go-group': 'Combine multiple Go repositories into a single URL.',

  // R
  'r-hosted': 'Store your own R packages in a private repository.',
  'r-proxy': 'Cache R packages from a remote repository like CRAN.',
  'r-group': 'Combine multiple R repositories into a single URL.',

  // Conan
  'conan-proxy': 'Cache Conan C/C++ packages from a remote repository.',

  // Conda
  'conda-proxy': 'Cache Conda packages from a remote repository.',

  // CocoaPods
  'cocoapods-proxy': 'Cache CocoaPods for iOS/macOS development.',

  // Git LFS
  'gitlfs-hosted': 'Store large files for Git LFS in a private repository.',

  // P2
  'p2-proxy': 'Cache Eclipse P2 artifacts and metadata.',

  // Terraform
  'terraform-proxy': 'Cache Terraform modules and providers.',

  // Ansible Galaxy
  'ansiblegalaxy-hosted': 'Store your own Ansible collections in a private repository.',
  'ansiblegalaxy-proxy': 'Cache Ansible collections from a remote repository like Galaxy.',
  'ansiblegalaxy-group': 'Combine multiple Ansible Galaxy repositories into a single URL.',

  // Composer
  'composer-proxy': 'Cache PHP packages from a remote Composer repository like Packagist.',
  'composer-hosted': 'Store your own PHP Composer packages in a private repository.',
  'composer-group': 'Combine multiple Composer repositories into a single URL.',
};

/**
 * General descriptions for repository types.
 */
export const TYPE_DESCRIPTIONS: Record<string, string> = {
  hosted: 'Store your own components and artifacts in a private, local repository. Perfect for internal builds and proprietary software.',
  proxy: 'Cache components from a remote registry (like Maven Central or npmjs.org). This improves build speeds and ensures availability even if the remote is down.',
  group: 'Combine multiple repositories (hosted and proxy) into a single URL. This allows users to access all components from a single entry point.',
};

/**
 * General descriptions for technology formats.
 */
export const FORMAT_DESCRIPTIONS: Record<string, string> = {
  maven2: 'Standard for Java development. Supports snapshots and releases.',
  npm: 'The package manager for JavaScript and Node.js.',
  docker: 'Container image registry for Docker and OCI images.',
  nuget: 'The package manager for .NET development.',
  pypi: 'The official third-party software repository for Python.',
  rubygems: 'The standard ruby package manager.',
  helm: 'The package manager for Kubernetes applications.',
  go: 'The proxy and registry for Go modules.',
  terraform: 'Registry for Terraform providers and modules.',
  cocoapods: 'The dependency manager for Swift and Objective-C Cocoa projects.',
  r: 'The comprehensive R archive network for statistical computing.',
  conan: 'The open-source package manager for C and C++ development.',
  conda: 'Package, dependency, and environment management for any language.',
  gitlfs: 'Git Large File Storage for versioning large files.',
  p2: 'The update mechanism for Eclipse plugins and applications.',
  yum: 'The primary package management tool for RPM-based Linux distributions.',
  apt: 'The advanced package tool for Debian and Ubuntu distributions.',
  alpine: 'Package manager for Alpine Linux distributions.',
  raw: 'Store any file format without specific metadata requirements.',
  ansiblegalaxy: 'Repository for Ansible collections containing roles, modules, and plugins for automation.',
  composer: 'The dependency manager for PHP.',
};

/**
 * Get description for a repository type.
 */
export function getTypeDescription(type: string): string {
  return TYPE_DESCRIPTIONS[type] || `Create a ${type} repository.`;
}

/**
 * Get description for a format.
 */
export function getFormatDescription(format: string): string {
  return FORMAT_DESCRIPTIONS[format] || `Manage ${format} components.`;
}

/**
 * Get description for a repository recipe.
 */
export function getRecipeDescription(format: string, type: string): string {
  const key = `${format}-${type}`;
  return RECIPE_DESCRIPTIONS[key] || `Create a ${format} (${type}) repository.`;
}
