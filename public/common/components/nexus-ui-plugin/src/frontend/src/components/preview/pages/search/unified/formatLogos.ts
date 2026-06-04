/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

/**
 * Format logos for ecosystem badges in search/browse.
 * Maps API format IDs (e.g. maven2, gitlfs) to logo image modules.
 * Logos sourced from nexus-outreach-content (Supported Formats section).
 */

import ansibleGalaxy from '../../../../../art/logos/formats/ansible-galaxy.png';
import apt from '../../../../../art/logos/formats/apt.png';
import cargo from '../../../../../art/logos/formats/cargo.png';
import cocoapods from '../../../../../art/logos/formats/cocoapods.png';
import composer from '../../../../../art/logos/formats/composer.png';
import conan from '../../../../../art/logos/formats/conan.png';
import conda from '../../../../../art/logos/formats/conda.png';
import docker from '../../../../../art/logos/formats/docker.png';
import gitLfs from '../../../../../art/logos/formats/git-lfs.png';
import go from '../../../../../art/logos/formats/go.png';
import helm from '../../../../../art/logos/formats/helm.png';
import huggingface from '../../../../../art/logos/formats/huggingface.png';
import maven from '../../../../../art/logos/formats/maven.png';
import npm from '../../../../../art/logos/formats/npm.png';
import nuget from '../../../../../art/logos/formats/nuget.png';
import p2 from '../../../../../art/logos/formats/p2.png';
import pypi from '../../../../../art/logos/formats/pypi.png';
import r from '../../../../../art/logos/formats/r.png';
import raw from '../../../../../art/logos/formats/raw.png';
import rubygems from '../../../../../art/logos/formats/rubygems.png';
import swift from '../../../../../art/logos/formats/swift.png';
import terraform from '../../../../../art/logos/formats/terraform.png';
import yum from '../../../../../art/logos/formats/yum.png';

/** API format ID -> logo module (resolved URL at runtime) */
const FORMAT_LOGO_MAP: Record<string, string> = {
  ansiblegalaxy: ansibleGalaxy,
  apt,
  cargo,
  cocoapods,
  composer,
  conan,
  conda,
  docker,
  gitlfs: gitLfs,
  go,
  helm,
  huggingface,
  maven2: maven,
  maven,
  npm,
  nuget,
  p2,
  pypi,
  r,
  raw,
  rubygems,
  swift,
  terraform,
  yum,
};

/**
 * Returns the logo URL for a given format, or undefined if no logo exists.
 *
 * @param format - API format ID (e.g. maven2, npm, gitlfs)
 */
export function getFormatLogo(format: string): string | undefined {
  return FORMAT_LOGO_MAP[format?.toLowerCase()];
}
