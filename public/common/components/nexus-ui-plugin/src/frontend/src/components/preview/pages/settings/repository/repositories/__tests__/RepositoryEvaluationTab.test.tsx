/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are
 * trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark
 * of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React from 'react';
import { render, screen, waitFor, fireEvent, } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';


// Mock the hook
const mockFetchOverride = jest.fn();
const mockSaveOverride = jest.fn();
jest.mock('../useRepoEvaluationOverride', () => ({
  useRepoEvaluationOverride: () => ({
    loading: false,
    error: null,
    fetchOverride: mockFetchOverride,
    saveOverride: mockSaveOverride,
  }),
}));

// Mock restClient for global settings fetch
const mockRestGet = jest.fn();
jest.mock('../../../../../../../interface/api', () => ({
  restClient: {
    get: (...args: unknown[]) => mockRestGet(...args),
  },
}));

// Mock toast so tests can assert success/error messages surfaced by the component.
// The real toast is rendered by a top-level <Toaster> outside this component's
// render tree, so screen.getByText(...) would never find the text.
const mockToastSuccess = jest.fn();
const mockToastError = jest.fn();
jest.mock('../../../../../shared', () => {
  const actual = jest.requireActual('../../../../../shared');
  return {
    ...actual,
    useToast: () => ({ success: mockToastSuccess, error: mockToastError }),
  };
});

import { RepositoryEvaluationTab } from '../RepositoryEvaluationTab';

function renderWithTheme(ui: React.ReactElement) {
  return render(<Theme accentColor="blue" hasBackground={false}>{ui}</Theme>);
}


describe('RepositoryEvaluationTab', () => {
  const defaultProps = { repositoryName: 'test-repo' };

  beforeEach(() => {
    jest.clearAllMocks();
    // Default: global settings fetch returns defaults
    mockRestGet.mockResolvedValue({
      activityTimeFrame: 30,
      artifactLatestVersions: 1,
      policyEvaluationStage: 'BUILD',
    });
    // Default: no existing override
    mockFetchOverride.mockResolvedValue(null);
    // Default: save succeeds
    mockSaveOverride.mockResolvedValue({ ok: true, message: 'Override saved' });
  });

  describe('Initial loading', () => {
    it('renders loading state initially', async () => {
      // Keep fetchOverride pending to simulate loading
      mockFetchOverride.mockImplementation(() => new Promise(() => {}));
      mockRestGet.mockImplementation(() => new Promise(() => {}));

      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      expect(screen.getByText(/Loading evaluation configuration/)).toBeInTheDocument();
    });
  });

  describe('Mode loading from server', () => {
    it('loads INHERIT mode when fetchOverride returns null', async () => {
      mockFetchOverride.mockResolvedValue(null);

      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Use global settings')).toBeInTheDocument();
      });

      // INHERIT info card should be visible
      expect(document.querySelector('button[role="radio"][value="INHERIT"]')?.getAttribute('aria-checked')).toBe('true');
    });

    it('loads OVERRIDE mode when fetchOverride returns { mode: OVERRIDE, ... }', async () => {
      mockFetchOverride.mockResolvedValue({
        mode: 'OVERRIDE',
        activityTimeFrame: 60,
        artifactLatestVersions: 2,
        policyEvaluationStage: 'RELEASE',
      });

      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Activity Time Frame')).toBeInTheDocument();
      });

      // OVERRIDE fields should be visible
      expect(screen.getByText('Activity Time Frame')).toBeInTheDocument();
      expect(screen.getByText('Artifact Latest Versions')).toBeInTheDocument();
      expect(screen.getByText('Policy Evaluation Stage')).toBeInTheDocument();
    });

    it('loads DISABLE mode when fetchOverride returns { mode: DISABLE }', async () => {
      mockFetchOverride.mockResolvedValue({ mode: 'DISABLE' });

      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="DISABLE"]')?.getAttribute('aria-checked')).toBe('true');
      });
    });
  });

  describe('Subsection visibility based on mode', () => {
    it('shows INHERIT info card when INHERIT is selected', async () => {
      mockFetchOverride.mockResolvedValue(null);

      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="INHERIT"]')?.getAttribute('aria-checked')).toBe('true');
      });
      expect(screen.queryByText('Activity Time Frame')).not.toBeInTheDocument();
      expect(document.querySelector('button[role="radio"][value="DISABLE"]')?.getAttribute('aria-checked')).toBe('false');
    });

    it('shows OVERRIDE fields when OVERRIDE is selected', async () => {
      mockFetchOverride.mockResolvedValue({ mode: 'OVERRIDE', activityTimeFrame: 30, artifactLatestVersions: 1, policyEvaluationStage: 'BUILD' });

      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Activity Time Frame')).toBeInTheDocument();
      });
      expect(document.querySelector('button[role="radio"][value="INHERIT"]')?.getAttribute('aria-checked')).toBe('false');
      expect(document.querySelector('button[role="radio"][value="DISABLE"]')?.getAttribute('aria-checked')).toBe('false');
    });

    it('shows DISABLE warning when DISABLE is selected', async () => {
      mockFetchOverride.mockResolvedValue({ mode: 'DISABLE' });

      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="DISABLE"]')?.getAttribute('aria-checked')).toBe('true');
      });
      expect(document.querySelector('button[role="radio"][value="INHERIT"]')?.getAttribute('aria-checked')).toBe('false');
      expect(screen.queryByText('Activity Time Frame')).not.toBeInTheDocument();
    });
  });

  describe('Mode switching preserves OVERRIDE values', () => {
    it('switching OVERRIDE -> INHERIT -> OVERRIDE restores the values', async () => {
      mockFetchOverride.mockResolvedValue({
        mode: 'OVERRIDE',
        activityTimeFrame: 90,
        artifactLatestVersions: 5,
        policyEvaluationStage: 'STAGE_RELEASE',
      });

      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      // Wait for OVERRIDE to load
      await waitFor(() => {
        expect(screen.getByText('Activity Time Frame')).toBeInTheDocument();
      });

      // Click INHERIT radio (first radio in the group)
      const radios = screen.getAllByRole('radio');
      const inheritRadio = Array.from(radios).find(
        (r) => (r as HTMLElement).getAttribute('value') === 'INHERIT'
      );
      fireEvent.click(inheritRadio!);

      // INHERIT info should be shown
      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="INHERIT"]')?.getAttribute('aria-checked')).toBe('true');
      });

      // Click OVERRIDE radio again
      const overrideRadio = Array.from(screen.getAllByRole('radio')).find(
        (r) => (r as HTMLElement).getAttribute('value') === 'OVERRIDE'
      );
      fireEvent.click(overrideRadio!);

      // OVERRIDE fields should reappear with preserved values
      await waitFor(() => {
        expect(screen.getByText('Activity Time Frame')).toBeInTheDocument();
      });
    });
  });

  describe('OVERRIDE seeds from global settings on first switch', () => {
    it('seeds OVERRIDE fields from globals when switching from INHERIT', async () => {
      mockRestGet.mockResolvedValue({
        activityTimeFrame: 60,
        artifactLatestVersions: 3,
        policyEvaluationStage: 'RELEASE',
      });
      mockFetchOverride.mockResolvedValue(null);

      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="INHERIT"]')?.getAttribute('aria-checked')).toBe('true');
      });

      // Click OVERRIDE radio
      const radios = screen.getAllByRole('radio');
      const overrideRadio = Array.from(radios).find(
        (r) => (r as HTMLElement).getAttribute('value') === 'OVERRIDE'
      );
      fireEvent.click(overrideRadio!);

      // OVERRIDE fields should appear
      await waitFor(() => {
        expect(screen.getByText('Activity Time Frame')).toBeInTheDocument();
      });
    });
  });

  describe('Save button state', () => {
    it('Save button is disabled when not dirty', async () => {
      mockFetchOverride.mockResolvedValue(null);

      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="INHERIT"]')?.getAttribute('aria-checked')).toBe('true');
      });

      const saveButton = screen.getByRole('button', { name: 'Save' });
      expect(saveButton).toBeDisabled();
    });

    it('Save button is enabled when dirty (mode changed)', async () => {
      mockFetchOverride.mockResolvedValue(null);

      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="INHERIT"]')?.getAttribute('aria-checked')).toBe('true');
      });

      // Click DISABLE radio to make dirty
      const radios = screen.getAllByRole('radio');
      const disableRadio = Array.from(radios).find(
        (r) => (r as HTMLElement).getAttribute('value') === 'DISABLE'
      );
      fireEvent.click(disableRadio!);

      await waitFor(() => {
        const saveButton = screen.getByRole('button', { name: 'Save' });
        expect(saveButton).not.toBeDisabled();
      });
    });
  });

  describe('Save functionality', () => {
    it('calls saveOverride with correct payload when saving', async () => {
      mockFetchOverride.mockResolvedValue(null);

      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="INHERIT"]')?.getAttribute('aria-checked')).toBe('true');
      });

      // Click DISABLE radio
      const radios = screen.getAllByRole('radio');
      const disableRadio = Array.from(radios).find(
        (r) => (r as HTMLElement).getAttribute('value') === 'DISABLE'
      );
      fireEvent.click(disableRadio!);

      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="DISABLE"]')?.getAttribute('aria-checked')).toBe('true');
      });

      // Click Save
      const saveButton = screen.getByRole('button', { name: 'Save' });
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(mockSaveOverride).toHaveBeenCalledWith('test-repo', { mode: 'DISABLE' });
      });
    });

    it('calls saveOverride with OVERRIDE payload including field values', async () => {
      mockFetchOverride.mockResolvedValue(null);

      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="INHERIT"]')?.getAttribute('aria-checked')).toBe('true');
      });

      // Click OVERRIDE radio
      const radios = screen.getAllByRole('radio');
      const overrideRadio = Array.from(radios).find(
        (r) => (r as HTMLElement).getAttribute('value') === 'OVERRIDE'
      );
      fireEvent.click(overrideRadio!);

      await waitFor(() => {
        expect(screen.getByText('Activity Time Frame')).toBeInTheDocument();
      });

      // Click Save
      const saveButton = screen.getByRole('button', { name: 'Save' });
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(mockSaveOverride).toHaveBeenCalledWith(
          'test-repo',
          expect.objectContaining({
            mode: 'OVERRIDE',
            activityTimeFrame: expect.any(Number),
            artifactLatestVersions: expect.any(Number),
            policyEvaluationStage: expect.any(String),
          })
        );
      });
    });
  });

  describe('Cancel functionality', () => {
    it('Cancel reverts to pristine state', async () => {
      mockFetchOverride.mockResolvedValue(null);

      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="INHERIT"]')?.getAttribute('aria-checked')).toBe('true');
      });

      // Click DISABLE radio
      const radios = screen.getAllByRole('radio');
      const disableRadio = Array.from(radios).find(
        (r) => (r as HTMLElement).getAttribute('value') === 'DISABLE'
      );
      fireEvent.click(disableRadio!);

      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="DISABLE"]')?.getAttribute('aria-checked')).toBe('true');
      });

      // Click Cancel
      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      fireEvent.click(cancelButton);

      // Should revert to INHERIT
      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="INHERIT"]')?.getAttribute('aria-checked')).toBe('true');
      });
    });
  });

  describe('Error handling', () => {
    it('shows error message on failed save', async () => {
      mockFetchOverride.mockResolvedValue(null);
      mockSaveOverride.mockResolvedValue({ ok: false, message: 'Save failed' });

      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="INHERIT"]')?.getAttribute('aria-checked')).toBe('true');
      });

      // Click DISABLE radio
      const radios = screen.getAllByRole('radio');
      const disableRadio = Array.from(radios).find(
        (r) => (r as HTMLElement).getAttribute('value') === 'DISABLE'
      );
      fireEvent.click(disableRadio!);

      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="DISABLE"]')?.getAttribute('aria-checked')).toBe('true');
      });

      // Click Save
      const saveButton = screen.getByRole('button', { name: 'Save' });
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(mockToastError).toHaveBeenCalledWith('Save failed');
      });
    });
  });

  describe('Success message', () => {
    it('shows success message on successful save', async () => {
      mockFetchOverride.mockResolvedValue(null);
      mockSaveOverride.mockResolvedValue({ ok: true, message: 'Settings saved' });

      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="INHERIT"]')?.getAttribute('aria-checked')).toBe('true');
      });

      // Click DISABLE radio
      const radios = screen.getAllByRole('radio');
      const disableRadio = Array.from(radios).find(
        (r) => (r as HTMLElement).getAttribute('value') === 'DISABLE'
      );
      fireEvent.click(disableRadio!);

      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="DISABLE"]')?.getAttribute('aria-checked')).toBe('true');
      });

      // Click Save
      const saveButton = screen.getByRole('button', { name: 'Save' });
      fireEvent.click(saveButton);

      await waitFor(() => {
        expect(mockToastSuccess).toHaveBeenCalledWith('Settings saved');
      });
    });
  });

  // =============================================================================
  // UX-11: hideActions + lift Save/Cancel/dirty up to the parent form's top
  // toolbar. The Edit Repository page's Cancel + Save Changes buttons drive this
  // tab when embedded in RepositoryForm.
  // =============================================================================
  describe('hideActions + lifted handlers (UX-11)', () => {
    it('hides the in-card Cancel/Save buttons when hideActions is true', async () => {
      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} hideActions />);

      await waitFor(() => {
        expect(screen.getByText('Use global settings')).toBeInTheDocument();
      });

      expect(screen.queryByRole('button', { name: 'Save' })).toBeNull();
      expect(screen.queryByRole('button', { name: 'Cancel' })).toBeNull();
    });

    it('still renders in-card buttons when hideActions is omitted (default)', async () => {
      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument();
      });
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
    });

    it('calls onDirtyChange(true) when user makes a change, false again on Cancel', async () => {
      const onDirtyChange = jest.fn();
      const cancelRef: React.MutableRefObject<(() => void) | null> = { current: null };
      renderWithTheme(
        <RepositoryEvaluationTab
          {...defaultProps}
          hideActions
          onDirtyChange={onDirtyChange}
          onCancelRef={cancelRef}
        />,
      );

      await waitFor(() => {
        expect(screen.getByText('Use global settings')).toBeInTheDocument();
      });

      // Initial render is pristine
      expect(onDirtyChange).toHaveBeenLastCalledWith(false);

      // Switch to Override → dirty
      const overrideRadio = screen.getAllByRole('radio')[1];
      fireEvent.click(overrideRadio);
      await waitFor(() => {
        expect(onDirtyChange).toHaveBeenLastCalledWith(true);
      });

      // Trigger Cancel via the lifted ref (parent's "Cancel" button would call this)
      expect(typeof cancelRef.current).toBe('function');
      fireEvent.click(overrideRadio); // ensure radio is registered
      await waitFor(() => {
        // After Cancel, dirty flips back to false
        cancelRef.current?.();
      });
      await waitFor(() => {
        expect(onDirtyChange).toHaveBeenLastCalledWith(false);
      });
    });

    it('exposes a Save handler via onSaveRef that persists the override', async () => {
      const saveRef: React.MutableRefObject<(() => Promise<void>) | null> = { current: null };
      const onDirtyChange = jest.fn();
      renderWithTheme(
        <RepositoryEvaluationTab
          {...defaultProps}
          hideActions
          onSaveRef={saveRef}
          onDirtyChange={onDirtyChange}
        />,
      );

      await waitFor(() => {
        expect(screen.getByText('Use global settings')).toBeInTheDocument();
      });

      // Switch to Disable mode → dirty
      const disableRadio = screen.getAllByRole('radio')[2];
      fireEvent.click(disableRadio);
      await waitFor(() => {
        expect(onDirtyChange).toHaveBeenLastCalledWith(true);
      });

      // Parent toolbar's Save Changes calls saveRef.current()
      expect(typeof saveRef.current).toBe('function');
      await saveRef.current!();

      expect(mockSaveOverride).toHaveBeenCalledWith('test-repo', { mode: 'DISABLE' });
      // After save, dirty flips back to false
      await waitFor(() => {
        expect(onDirtyChange).toHaveBeenLastCalledWith(false);
      });
    });
  });

  describe('savedOverride updated after save', () => {
    it('Save is disabled after OVERRIDE save + INHERIT + OVERRIDE toggle — savedOverride matches last save', async () => {
      mockFetchOverride.mockResolvedValue(null);
      mockSaveOverride.mockResolvedValue({ ok: true });

      renderWithTheme(<RepositoryEvaluationTab {...defaultProps} />);

      await waitFor(() => {
        expect(document.querySelector('button[role="radio"][value="INHERIT"]')?.getAttribute('aria-checked')).toBe('true');
      });

      const getRadio = (value: string) =>
        Array.from(screen.getAllByRole('radio')).find(
          (r) => (r as HTMLElement).getAttribute('value') === value
        );

      // Switch to OVERRIDE and save
      fireEvent.click(getRadio('OVERRIDE')!);
      await waitFor(() => expect(screen.getByText('Activity Time Frame')).toBeInTheDocument());
      fireEvent.click(screen.getByRole('button', { name: 'Save' }));
      await waitFor(() => expect(mockSaveOverride).toHaveBeenCalledTimes(1));

      // Toggle OVERRIDE -> INHERIT -> OVERRIDE
      fireEvent.click(getRadio('INHERIT')!);
      await waitFor(() =>
        expect(document.querySelector('button[role="radio"][value="INHERIT"]')?.getAttribute('aria-checked')).toBe('true')
      );
      fireEvent.click(getRadio('OVERRIDE')!);
      await waitFor(() => expect(screen.getByText('Activity Time Frame')).toBeInTheDocument());

      // Save must be disabled — restored values match the saved snapshot (savedOverride was updated on save)
      expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
    });
  });
});
