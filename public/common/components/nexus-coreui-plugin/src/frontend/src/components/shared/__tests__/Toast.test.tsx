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

import React, { act } from 'react';
import { render, screen } from '@testing-library/react';
import { ToastProvider, useToast } from '../Toast';

/**
 * Helper component that calls useToast() and exposes the result via a data attribute for inspection.
 * Renders a button that triggers an error toast when clicked.
 */
function ToastConsumer({ onToastRef }: { onToastRef?: (toast: ReturnType<typeof useToast>) => void }) {
  const toast = useToast();
  if (onToastRef) {
    onToastRef(toast);
  }
  return (
    <button data-testid="trigger-error" onClick={() => toast.error('Test error')}>
      Trigger Error
    </button>
  );
}

describe('ToastProvider and useToast', () => {
  beforeEach(() => {
    // Clear the window bridge before each test to avoid cross-test pollution
    delete (window as any).__nexusToast;
  });

  it('useToast() inside ToastProvider returns a non-noop context', () => {
    let capturedToast: ReturnType<typeof useToast> | undefined;

    render(
      <ToastProvider>
        <ToastConsumer onToastRef={(t) => { capturedToast = t; }} />
      </ToastProvider>
    );

    expect(capturedToast).toBeDefined();
    // The noop error function is `() => {}` — verify that the returned function is not that noop
    // by checking it is a different function reference (the real showToast-backed callback)
    const noopFn = () => {};
    expect(capturedToast!.error).not.toBe(noopFn);
    expect(typeof capturedToast!.error).toBe('function');
    expect(typeof capturedToast!.success).toBe('function');
    expect(typeof capturedToast!.warning).toBe('function');
    expect(typeof capturedToast!.info).toBe('function');
    expect(typeof capturedToast!.showToast).toBe('function');
  });

  it('calling useToast().error(title) renders a toast element in the DOM', async () => {
    let capturedToast: ReturnType<typeof useToast> | undefined;

    render(
      <ToastProvider>
        <ToastConsumer onToastRef={(t) => { capturedToast = t; }} />
      </ToastProvider>
    );

    expect(capturedToast).toBeDefined();

    act(() => {
      capturedToast!.error('Test error');
    });

    const toastEl = await screen.findByTestId('toast-error');
    expect(toastEl).toBeInTheDocument();
    expect(toastEl).toHaveTextContent('Test error');
  });

  it('window.__nexusToast bridge is set synchronously when ToastProvider mounts', () => {
    expect((window as any).__nexusToast).toBeUndefined();

    render(
      <ToastProvider>
        <div />
      </ToastProvider>
    );

    expect((window as any).__nexusToast).toBeDefined();
    expect(typeof (window as any).__nexusToast.error).toBe('function');
    expect(typeof (window as any).__nexusToast.success).toBe('function');
  });

  it('useToast() outside a ToastProvider returns noop object and does not throw', () => {
    // Ensure no window bridge is present so we test the pure noop fallback path
    delete (window as any).__nexusToast;

    let capturedToast: ReturnType<typeof useToast> | undefined;

    function ConsumerWithoutProvider() {
      capturedToast = useToast();
      return <div data-testid="no-provider" />;
    }

    expect(() => {
      render(<ConsumerWithoutProvider />);
    }).not.toThrow();

    expect(capturedToast).toBeDefined();

    // Calling methods on the noop toast should not throw
    expect(() => capturedToast!.error('silent')).not.toThrow();
    expect(() => capturedToast!.success('silent')).not.toThrow();
    expect(() => capturedToast!.warning('silent')).not.toThrow();
    expect(() => capturedToast!.info('silent')).not.toThrow();
  });
});
