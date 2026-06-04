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

import ansibleGalaxyLogo from '../../../../art/logos/formats/ansible-galaxy.png';

/**
 * Human-readable display names for repository formats.
 */
export const FORMAT_LABELS: Record<string, string> = {
  maven2: 'Maven',
  npm: 'npm',
  nuget: 'NuGet',
  pypi: 'PyPI',
  docker: 'Docker',
  helm: 'Helm',
  go: 'Go',
  yum: 'Yum',
  apt: 'APT',
  raw: 'Raw',
  rubygems: 'RubyGems',
  r: 'R',
  conan: 'Conan',
  conda: 'Conda',
  cocoapods: 'CocoaPods',
  gitlfs: 'Git LFS',
  p2: 'p2',
  terraform: 'Terraform',
  composer: 'Composer',
  cargo: 'Cargo (Rust)',
  huggingface: 'Hugging Face',
  alpine: 'Alpine',
  swift: 'Swift',
  pub: 'Pub (Dart)',
  ansiblegalaxy: 'Ansible Galaxy',
};

/**
 * Official logo URLs for various repository formats.
 */
export const FORMAT_LOGOS: Record<string, string> = {
  maven2: 'https://cdn.simpleicons.org/apachemaven',
  npm: 'https://cdn.simpleicons.org/npm',
  docker: 'https://cdn.simpleicons.org/docker',
  nuget: 'https://cdn.simpleicons.org/nuget',
  pypi: 'https://cdn.simpleicons.org/python',
  rubygems: 'https://cdn.simpleicons.org/rubygems',
  helm: 'https://cdn.simpleicons.org/kubernetes',
  go: 'https://cdn.simpleicons.org/go',
  terraform: 'https://cdn.simpleicons.org/terraform',
  cocoapods: 'https://cdn.simpleicons.org/cocoapods',
  r: 'https://cdn.simpleicons.org/r',
  conan: 'https://cdn.simpleicons.org/conan',
  conda: 'https://cdn.simpleicons.org/anaconda',
  cargo: 'https://cdn.simpleicons.org/rust',
  composer: 'https://cdn.simpleicons.org/composer',
  huggingface: 'https://cdn.simpleicons.org/huggingface',
  gitlfs: 'https://cdn.simpleicons.org/gitlfs',
  p2: 'https://cdn.simpleicons.org/eclipseide',
  alpine: 'https://cdn.simpleicons.org/alpinelinux',
  apt: 'https://cdn.simpleicons.org/debian',
  yum: 'https://cdn.simpleicons.org/redhat',
  swift: 'https://cdn.simpleicons.org/swift',
  pub: 'https://cdn.simpleicons.org/dart',
  ansiblegalaxy: ansibleGalaxyLogo,
};
