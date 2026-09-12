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
 * NuGet dependency snippet generator.
 *
 * Ported from Classic NX.nuget.controller.NuGetDependencySnippetController. The Chocolatey
 * snippet points at this Nexus instance's NuGet repository, built from the current origin.
 */
export const generate: SnippetGenerator = (component) => {
  const { name, version, repositoryName } = component;
  // Guard window so the generator is safe to call outside a browser (SSR/Node/tests).
  const origin = typeof window !== 'undefined' ? window.location.origin : '';
  // Only point Chocolatey at this instance's NuGet repo when we actually know its name;
  // otherwise emit the plain command rather than a `.../repository/undefined/` source URL.
  const source = repositoryName ? ` --source="${origin}/repository/${repositoryName}/"` : '';

  return [
    { displayName: 'Package Manager', snippetText: `Install-Package ${name} -Version ${version}` },
    { displayName: '.NET CLI', snippetText: `dotnet add package ${name} --version ${version}` },
    { displayName: 'Paket CLI', snippetText: `paket add ${name} --version ${version}` },
    {
      displayName: 'Chocolatey CLI',
      snippetText: `choco install ${name} --version ${version}${source}`,
    },
  ];
};
