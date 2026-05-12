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

import type { Privilege } from '../../../../security/privileges/types';
import { pickPrivilegeIdsForRequirements } from '../grantPrivilegePicker';

function priv(id: string, permission: string): Privilege {
  return {
    id,
    version: '1',
    name: id,
    description: '',
    type: 'wildcard',
    readOnly: false,
    properties: {},
    permission,
  };
}

describe('grantPrivilegePicker', () => {
  it('AND picks one privilege per requirement', () => {
    const privileges = [priv('p1', 'nexus:a:read'), priv('p2', 'nexus:b:read')];
    const { ids, missing } = pickPrivilegeIdsForRequirements(['nexus:a:read', 'nexus:b:read'], 'AND', privileges);
    expect(missing).toEqual([]);
    expect(ids.sort()).toEqual(['p1', 'p2'].sort());
  });

  it('OR picks a single covering privilege', () => {
    const privileges = [priv('wide', 'nexus:*')];
    const { ids, missing } = pickPrivilegeIdsForRequirements(['nexus:a:read', 'nexus:z:read'], 'OR', privileges);
    expect(missing).toEqual([]);
    expect(ids).toEqual(['wide']);
  });

  it('reports missing when no privilege matches', () => {
    const { ids, missing } = pickPrivilegeIdsForRequirements(['nexus:missing:read'], 'AND', []);
    expect(ids).toEqual([]);
    expect(missing).toContain('nexus:missing:read');
  });
});
