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

import { act, renderHook, waitFor } from '@testing-library/react';
import { useRoleDetailEdit } from '../useRoleDetailEdit';
import type { Role } from '../types';

const NEW_NAME = 'Renamed';
const NEW_DESCRIPTION = 'New description';

const baseRole: Role = {
  id: 'r',
  version: '1',
  source: 'Default',
  name: 'Original',
  description: 'Original description',
  readOnly: false,
  privileges: ['p1'],
  roles: ['r1'],
};

describe('useRoleDetailEdit', () => {
  it('starts non-dirty with pending equal to committed', () => {
    const { result } = renderHook(() =>
      useRoleDetailEdit({ role: baseRole, save: jest.fn(), onSaved: jest.fn() }),
    );
    expect(result.current.isDirty).toBe(false);
    expect(result.current.pending.name).toBe(baseRole.name);
    expect(result.current.committed.name).toBe(baseRole.name);
  });

  it('changeName mutates pending and flips isDirty', () => {
    const { result } = renderHook(() =>
      useRoleDetailEdit({ role: baseRole, save: jest.fn(), onSaved: jest.fn() }),
    );
    act(() => result.current.changeName(NEW_NAME));
    expect(result.current.pending.name).toBe(NEW_NAME);
    expect(result.current.isDirty).toBe(true);
    expect(result.current.committed.name).toBe(baseRole.name);
  });

  it('changeDescription flips isDirty', () => {
    const { result } = renderHook(() =>
      useRoleDetailEdit({ role: baseRole, save: jest.fn(), onSaved: jest.fn() }),
    );
    act(() => result.current.changeDescription(NEW_DESCRIPTION));
    expect(result.current.isDirty).toBe(true);
  });

  it('applyPrivileges + applyRoles feed pending', () => {
    const { result } = renderHook(() =>
      useRoleDetailEdit({ role: baseRole, save: jest.fn(), onSaved: jest.fn() }),
    );
    act(() => result.current.applyPrivileges(['a', 'b']));
    act(() => result.current.applyRoles(['x']));
    expect(result.current.pending.privileges).toEqual(['a', 'b']);
    expect(result.current.pending.roles).toEqual(['x']);
  });

  it('cancel reverts pending to committed', () => {
    const { result } = renderHook(() =>
      useRoleDetailEdit({ role: baseRole, save: jest.fn(), onSaved: jest.fn() }),
    );
    act(() => result.current.changeName(NEW_NAME));
    expect(result.current.isDirty).toBe(true);
    act(() => result.current.cancel());
    expect(result.current.pending.name).toBe(baseRole.name);
    expect(result.current.isDirty).toBe(false);
  });

  it('save fires the injected save with pending, then commits + calls onSaved', async () => {
    const saved: Role = { ...baseRole, name: NEW_NAME, version: '2' };
    const save = jest.fn().mockResolvedValue(saved);
    const onSaved = jest.fn();
    const { result } = renderHook(() => useRoleDetailEdit({ role: baseRole, save, onSaved }));

    act(() => result.current.changeName(NEW_NAME));
    act(() => result.current.save());

    await waitFor(() => {
      expect(onSaved).toHaveBeenCalledWith(saved);
    });
    expect(save).toHaveBeenCalled();
    const passed = save.mock.calls[0][0];
    expect(passed.name).toBe(NEW_NAME);
    expect(result.current.isDirty).toBe(false);
    expect(result.current.committed.name).toBe(NEW_NAME);
  });

  it('save failure records saveError and keeps pending dirty', async () => {
    const save = jest.fn().mockRejectedValue(new Error('boom'));
    const { result } = renderHook(() =>
      useRoleDetailEdit({ role: baseRole, save, onSaved: jest.fn() }),
    );

    act(() => result.current.changeName(NEW_NAME));
    act(() => result.current.save());

    await waitFor(() => {
      expect(result.current.saveError).toBe('boom');
    });
    expect(result.current.isDirty).toBe(true);
    expect(result.current.pending.name).toBe(NEW_NAME);
  });
});
