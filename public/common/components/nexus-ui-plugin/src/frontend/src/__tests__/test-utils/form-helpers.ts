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

import { screen, waitFor, WaitForOptions } from '@testing-library/react';

export interface WaitForFormOptions {
  timeout?: number;
  formSectionTitle?: string;
}

/**
 * Waits for a form to finish loading and render inputs.
 * Handles common async loading patterns in settings pages.
 *
 * This helper addresses the pattern where components:
 * 1. Show a loading spinner while fetching data
 * 2. Only render form inputs after async data loads
 * 3. Tests fail because they query for labels before form renders
 *
 * @example
 * ```typescript
 * it('displays form inputs', async () => {
 *   render(<EmailPage />, { wrapper: TestWrapper });
 *   await waitForFormToLoad();
 *   expect(screen.getByLabelText('SMTP Host')).toBeInTheDocument();
 * });
 * ```
 */
export async function waitForFormToLoad(options?: WaitForFormOptions): Promise<void> {
  const { timeout = 5000, formSectionTitle } = options || {};

  // Wait for loading to complete by checking for form inputs
  // This is the most reliable indicator that the async loading has finished
  await waitFor(
    () => {
      // Check for form inputs - the primary indicator that loading is complete
      const textboxes = screen.queryAllByRole('textbox');
      const checkboxes = screen.queryAllByRole('checkbox');
      const comboboxes = screen.queryAllByRole('combobox');
      const hasInputs = textboxes.length + checkboxes.length + comboboxes.length > 0;
      
      // Also check for common section headings as a secondary indicator
      // Using queryAllByText to avoid errors when multiple elements match
      const formIndicators = [
        /SMTP Configuration/i,
        /Authentication/i,
        /Email Settings/i,
        /SSL\/TLS Options/i,
        /Current Settings/i,
        /Configuration/i,
      ];
      
      const hasFormSection = formIndicators.some(pattern => 
        screen.queryAllByText(pattern).length > 0
      );
      
      expect(hasInputs || hasFormSection).toBe(true);
    },
    { timeout } as WaitForOptions
  );

  // If form section title provided, wait for it specifically
  if (formSectionTitle) {
    await waitFor(
      () => {
        expect(screen.queryAllByText(formSectionTitle).length).toBeGreaterThan(0);
      },
      { timeout: Math.min(timeout, 2000) } as WaitForOptions
    );
  }
}

/**
 * Waits for a specific label to appear in the form.
 * Combines waitForFormToLoad with label-specific waiting.
 *
 * @example
 * ```typescript
 * it('has SMTP Host input', async () => {
 *   render(<EmailPage />, { wrapper: TestWrapper });
 *   await waitForLabel('SMTP Host');
 *   expect(screen.getByLabelText('SMTP Host')).toBeInTheDocument();
 * });
 * ```
 */
export async function waitForLabel(
  labelText: string | RegExp,
  timeout = 3000
): Promise<void> {
  await waitForFormToLoad({ timeout });
  await waitFor(
    () => {
      const label = typeof labelText === 'string'
        ? screen.getByLabelText(labelText)
        : screen.getByLabelText(labelText);
      expect(label).toBeInTheDocument();
    },
    { timeout } as WaitForOptions
  );
}

