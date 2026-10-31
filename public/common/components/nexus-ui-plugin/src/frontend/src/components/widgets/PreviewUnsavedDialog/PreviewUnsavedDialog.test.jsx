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
import { act, render, screen } from '@testing-library/react';
import PreviewUnsavedDialog from './PreviewUnsavedDialog';

// Mock Radix themes with portal-free DOM doubles (same approach as SettingsForm.test.jsx)
jest.mock('@radix-ui/themes', () => {
  const ReactLib = require('react');
  return {
    AlertDialog: {
      Root: ({ children, open }) =>
        open ? ReactLib.createElement('div', { 'data-testid': 'preview-unsaved-dialog' }, children) : null,
      Content: ({ children }) => ReactLib.createElement('div', null, children),
      Title: ({ children }) => ReactLib.createElement('h2', null, children),
      Description: ({ children }) => ReactLib.createElement('p', null, children),
      Cancel: ({ children }) => children,
      Action: ({ children }) => children,
    },
    Button: ({ children, onClick }) =>
      ReactLib.createElement('button', { type: 'button', onClick }, children),
    Flex: ({ children }) => ReactLib.createElement('div', null, children),
  };
});

describe('PreviewUnsavedDialog', () => {
  const selectors = {
    title: () => screen.queryByRole('heading', { name: 'Unsaved Changes' }),
    stay: () => screen.queryByRole('button', { name: 'Stay' }),
    leave: () => screen.queryByRole('button', { name: 'Leave' }),
  };

  afterEach(() => {
    delete window.showPreviewUnsavedDialog;
  });

  it('registers window.showPreviewUnsavedDialog on mount', () => {
    render(<PreviewUnsavedDialog />);
    expect(typeof window.showPreviewUnsavedDialog).toBe('function');
  });

  it('deletes window.showPreviewUnsavedDialog on unmount', () => {
    const { unmount } = render(<PreviewUnsavedDialog />);
    unmount();
    expect(window.showPreviewUnsavedDialog).toBeUndefined();
  });

  it('opens the dialog with Stay and Leave when invoked', () => {
    render(<PreviewUnsavedDialog />);
    expect(selectors.title()).not.toBeInTheDocument();
    act(() => {
      window.showPreviewUnsavedDialog();
    });
    expect(selectors.title()).toBeInTheDocument();
    expect(selectors.stay()).toBeInTheDocument();
    expect(selectors.leave()).toBeInTheDocument();
  });

  it('resolves true when Leave is clicked', async () => {
    render(<PreviewUnsavedDialog />);
    let promise;
    act(() => {
      promise = window.showPreviewUnsavedDialog();
    });
    act(() => {
      selectors.leave().click();
    });
    await expect(promise).resolves.toBe(true);
  });

  it('resolves false when Stay is clicked', async () => {
    render(<PreviewUnsavedDialog />);
    let promise;
    act(() => {
      promise = window.showPreviewUnsavedDialog();
    });
    act(() => {
      selectors.stay().click();
    });
    await expect(promise).resolves.toBe(false);
  });
});
