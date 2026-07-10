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
import React from 'react';
import {renderHook} from '@testing-library/react';
import {useContextAwareRouteName} from '../useContextAwareRouteName';
import {PreviewUIContext} from '../PreviewUIContext';

function wrapper(isPreview: boolean) {
  return ({children}: {children: React.ReactNode}) =>
    React.createElement(PreviewUIContext.Provider, {value: isPreview}, children);
}

describe('useContextAwareRouteName', () => {
  it('returns the name unchanged when not in preview mode', () => {
    const {result} = renderHook(() => useContextAwareRouteName('browse.search'), {
      wrapper: wrapper(false),
    });
    expect(result.current).toBe('browse.search');
  });

  it('prefixes with preview. when in preview mode', () => {
    const {result} = renderHook(() => useContextAwareRouteName('browse.search'), {
      wrapper: wrapper(true),
    });
    expect(result.current).toBe('preview.browse.search');
  });

  it('returns undefined unchanged', () => {
    const {result} = renderHook(() => useContextAwareRouteName(undefined), {
      wrapper: wrapper(true),
    });
    expect(result.current).toBeUndefined();
  });

  it('keeps already-prefixed preview. name in preview mode', () => {
    const {result} = renderHook(() => useContextAwareRouteName('preview.browse.search'), {
      wrapper: wrapper(true),
    });
    expect(result.current).toBe('preview.browse.search');
  });

  it('strips preview. prefix when not in preview mode', () => {
    const {result} = renderHook(() => useContextAwareRouteName('preview.browse.search'), {
      wrapper: wrapper(false),
    });
    expect(result.current).toBe('browse.search');
  });
});
