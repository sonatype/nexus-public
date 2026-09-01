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
 * npm dependency snippet generator.
 *
 * Ported from Classic NX.npm.controller.NpmDependencySnippetController. A non-empty group is
 * treated as the package scope and rendered as `@scope/name`.
 */
export const generate: SnippetGenerator = (component) => {
  const { group, name, version } = component;
  const dependencyName = (group ? `@${group}/` : '') + name;

  return [
    {
      displayName: 'npm',
      description: 'Install runtime dependency',
      snippetText: `npm install ${dependencyName}@${version}`,
    },
    {
      displayName: 'Yarn',
      description: 'Install runtime dependency',
      snippetText: `yarn add ${dependencyName}@${version}`,
    },
    {
      displayName: 'package.json',
      description: 'Install runtime dependency to the package.json\'s "dependencies" section',
      snippetText: `"${dependencyName}": "${version}"`,
    },
  ];
};
