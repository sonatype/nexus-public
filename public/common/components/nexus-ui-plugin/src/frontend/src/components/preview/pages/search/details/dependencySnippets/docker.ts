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

import type { SnippetGenerator } from './types';

/**
 * Docker dependency snippet generator.
 *
 * Ported from Classic NX.docker.controller.DockerDependencySnippetController. When the asset
 * carries a top-level registry URL (NEXUS-51972) the pull target is prefixed with it.
 */
export const generate: SnippetGenerator = (component, asset) => {
  const yamlIndent = '  ';
  const { name, version } = component;
  const shortName = name.substring(name.lastIndexOf('/') + 1);
  const registry = asset?.registryUrl;
  const pullTarget = registry ? `${registry}/${name}` : name;

  // Every reference to the image must use the registry-qualified target; a bare name would
  // resolve to Docker Hub instead of this Nexus registry when a registryUrl is present.
  return [
    { displayName: 'Docker', snippetText: `docker pull ${pullTarget}:${version}` },
    { displayName: 'Dockerfile', snippetText: `FROM ${pullTarget}:${version}` },
    {
      displayName: 'Compose',
      snippetText:
        'services:\n' +
        `${yamlIndent}${shortName}:\n` +
        `${yamlIndent.repeat(2)}image: ${pullTarget}:${version}`,
    },
    {
      displayName: 'Kubernetes',
      snippetText:
        'spec:\n' +
        `${yamlIndent}containers:\n` +
        `${yamlIndent}- name: ${shortName}\n` +
        `${yamlIndent.repeat(2)}image: ${pullTarget}:${version}`,
    },
  ];
};
