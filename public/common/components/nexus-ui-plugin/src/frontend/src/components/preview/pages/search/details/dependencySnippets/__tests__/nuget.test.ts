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

import { generate } from '../nuget';
import type { SnippetComponentModel } from '../types';

describe('nuget dependency snippet generator', () => {
  const component: SnippetComponentModel = {
    format: 'nuget',
    group: '',
    name: 'Newtonsoft.Json',
    version: '13.0.3',
    repositoryName: 'nuget-hosted',
  };

  it('emits the Classic tool set in order', () => {
    expect(generate(component).map((s) => s.displayName)).toEqual([
      'Package Manager',
      '.NET CLI',
      'Paket CLI',
      'Chocolatey CLI',
    ]);
  });

  it('produces snippet text matching Classic, sourcing the repo URL from origin', () => {
    const s: Record<string, string> = {};
    for (const g of generate(component)) s[g.displayName] = g.snippetText;
    expect(s['Package Manager']).toBe('Install-Package Newtonsoft.Json -Version 13.0.3');
    expect(s['.NET CLI']).toBe('dotnet add package Newtonsoft.Json --version 13.0.3');
    expect(s['Paket CLI']).toBe('paket add Newtonsoft.Json --version 13.0.3');
    expect(s['Chocolatey CLI']).toBe(
      `choco install Newtonsoft.Json --version 13.0.3 --source="${window.location.origin}/repository/nuget-hosted/"`,
    );
  });

  it('omits the --source flag when the repository name is unknown (no "undefined" in the URL)', () => {
    const noRepo: SnippetComponentModel = {
      format: 'nuget',
      group: '',
      name: 'Newtonsoft.Json',
      version: '13.0.3',
    };
    const s: Record<string, string> = {};
    for (const g of generate(noRepo)) s[g.displayName] = g.snippetText;
    expect(s['Chocolatey CLI']).toBe('choco install Newtonsoft.Json --version 13.0.3');
    expect(s['Chocolatey CLI']).not.toContain('undefined');
  });
});
