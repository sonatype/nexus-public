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
import { render, screen, fireEvent } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';
import PreviewUiSettingsPage from '../PreviewUiSettingsPage';
import { ExtJS } from '../../../../../../../interface/ExtJS';

jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
    state: jest.fn().mockReturnValue({ getValue: jest.fn().mockReturnValue(false) }),
  },
}));

jest.mock('../../../../../shared', () => ({
  useToast: () => ({ success: jest.fn(), error: jest.fn() }),
  PageHeader: ({ title, description }: { title: string; description?: string }) => (
    <div data-testid="page-header"><h1>{title}</h1>{description && <p>{description}</p>}</div>
  ),
  useUnsavedChangesWarning: jest.fn(),
  clearDirtyState: jest.fn(),
}));

jest.mock('../../../../../shared/form', () => ({
  SettingsButton: ({ children, testId, disabled, type, onClick }: { children: React.ReactNode; testId?: string; disabled?: boolean; type?: string; onClick?: () => void }) => (
    <button data-testid={testId} disabled={disabled} type={(type as any) || 'button'} onClick={onClick}>
      {children}
    </button>
  ),
}));

let mockIsPristine = true;
let mockIsLoading = false;
let mockIsSaving = false;
let mockSaveError: string | null = null;
let mockHasValidationErrors = false;
let mockValidationErrors: Record<string, string | null> = {};
let mockData = {
  anonymousEnabled: false,
  loggedInEnabled: true,
  defaultToPreviewUi: false,
  disableLegacyUi: false,
  disableSwitchFeedback: false,
};
const mockSubmit = jest.fn();
const mockReset = jest.fn();
const mockCheckboxOnChange = jest.fn();

jest.mock('../usePreviewUiSettingsForm', () => ({
  usePreviewUiSettingsForm: () => ({
    isLoading: mockIsLoading,
    isPristine: mockIsPristine,
    isSaving: mockIsSaving,
    saveError: mockSaveError,
    hasValidationErrors: mockHasValidationErrors,
    validationErrors: mockValidationErrors,
    data: mockData,
    submit: mockSubmit,
    reset: mockReset,
    checkbox: (field: string) => ({
      checked: (mockData as Record<string, boolean>)[field] ?? false,
      onChange: mockCheckboxOnChange,
    }),
  }),
}));

describe('PreviewUiSettingsPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockIsPristine = true;
    mockIsLoading = false;
    mockIsSaving = false;
    mockSaveError = null;
    mockHasValidationErrors = false;
    mockValidationErrors = {};
    mockData = {
      anonymousEnabled: false,
      loggedInEnabled: true,
      defaultToPreviewUi: false,
      disableLegacyUi: false,
      disableSwitchFeedback: false,
    };
  });

  function renderPage() {
    return render(<Theme><PreviewUiSettingsPage /></Theme>);
  }

  it('renders all settings sections when loaded', () => {
    renderPage();

    expect(screen.getByText('Nexus One UI Settings')).toBeInTheDocument();
    expect(screen.getByText('Anonymous Users')).toBeInTheDocument();
    expect(screen.getByTestId('preview-ui-switch-anonymous')).toBeInTheDocument();
    expect(screen.getByText('Logged-in Users')).toBeInTheDocument();
    expect(screen.getByText('Default to Nexus One UI')).toBeInTheDocument();
    expect(screen.getByText('Disable Switch Feedback')).toBeInTheDocument();
    expect(screen.queryByText('Disable Classic UI')).not.toBeInTheDocument();
  });

  it('shows loading state', () => {
    mockIsLoading = true;
    renderPage();
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('disables save button when form is pristine', () => {
    renderPage();
    expect(screen.getByTestId('form-submit')).toBeDisabled();
  });

  it('enables save button when form has changes', () => {
    mockIsPristine = false;
    renderPage();
    expect(screen.getByTestId('form-submit')).not.toBeDisabled();
  });

  it('calls checkbox onChange when switch is clicked', () => {
    renderPage();
    const defaultSwitch = screen.getByTestId('preview-ui-switch-default-preview');
    fireEvent.click(defaultSwitch);
    expect(mockCheckboxOnChange).toHaveBeenCalled();
  });

  it('calls submit when save is clicked', () => {
    mockIsPristine = false;
    renderPage();
    fireEvent.click(screen.getByTestId('form-submit'));
    expect(mockSubmit).toHaveBeenCalled();
  });

  it('hides the Disable Classic UI card', () => {
    renderPage();
    expect(screen.queryByText('Disable Classic UI')).not.toBeInTheDocument();
    expect(screen.queryByText(/CAUTION: When enabled, all users lose access to the Classic UI immediately/i)).not.toBeInTheDocument();
  });

  it('shows discard confirmation when discarding dirty form', () => {
    mockIsPristine = false;
    renderPage();
    fireEvent.click(screen.getByTestId('form-cancel'));
    expect(screen.getByText('Unsaved Changes')).toBeInTheDocument();
  });

  it('calls reset after confirming discard', () => {
    mockIsPristine = false;
    renderPage();
    fireEvent.click(screen.getByTestId('form-cancel'));
    const leaveButton = screen.getByRole('button', { name: /leave/i });
    fireEvent.click(leaveButton);
    expect(mockReset).toHaveBeenCalled();
  });

  it('calls reset immediately when discarding a pristine form with a save error', () => {
    mockSaveError = 'Network error';
    renderPage();

    fireEvent.click(screen.getByTestId('form-cancel'));

    expect(mockReset).toHaveBeenCalled();
    expect(screen.queryByText('Unsaved Changes')).not.toBeInTheDocument();
  });

  it('hides Save/Discard and disables switches when user lacks permission', () => {
    (ExtJS.checkPermission as jest.Mock).mockReturnValue(false);
    renderPage();

    expect(screen.queryByTestId('form-submit')).not.toBeInTheDocument();
    expect(screen.queryByTestId('form-cancel')).not.toBeInTheDocument();

    const switches = screen.getAllByRole('switch');
    switches.forEach((s) => expect(s).toBeDisabled());

    (ExtJS.checkPermission as jest.Mock).mockReturnValue(true);
  });

  it('does not disable access or rollout cards based on disableLegacyUi state', () => {
    mockData.disableLegacyUi = true;
    renderPage();

    const anonymousSwitch = screen.getByTestId('preview-ui-switch-anonymous');
    expect(anonymousSwitch).not.toBeDisabled();

    const loggedInSwitch = screen.getByTestId('preview-ui-switch-logged-in');
    expect(loggedInSwitch).not.toBeDisabled();

    const defaultSwitch = screen.getByTestId('preview-ui-switch-default-preview');
    expect(defaultSwitch).not.toBeDisabled();

    const disableFeedbackSwitch = screen.getByTestId('preview-ui-switch-disable-feedback');
    expect(disableFeedbackSwitch).not.toBeDisabled();
  });

  it('hides Anonymous Users switch in Cloud deployment', () => {
    (ExtJS.state as jest.Mock).mockReturnValue({ getValue: jest.fn().mockReturnValue(true) });
    renderPage();

    expect(screen.queryByTestId('preview-ui-switch-anonymous')).not.toBeInTheDocument();
    expect(screen.queryByText('Anonymous Users')).not.toBeInTheDocument();
    expect(screen.getByTestId('preview-ui-switch-logged-in')).toBeInTheDocument();
  });

  it('shouldDisableSaveButtonWhenValidationErrorsExist', () => {
    mockIsPristine = false;
    mockHasValidationErrors = true;
    renderPage();

    expect(screen.getByTestId('form-submit')).toBeDisabled();
  });

  it('shouldShowLockoutErrorMessageWhenValidationErrorsExist', () => {
    mockIsPristine = false;
    mockHasValidationErrors = true;
    mockValidationErrors = { disableLegacyUi: 'Cannot disable Classic UI when both Anonymous and Logged-in access to Nexus One UI are disabled. At least one UI access method must remain enabled to prevent lockout.' };
    renderPage();

    expect(screen.getByText(/Cannot disable Classic UI when both Anonymous and Logged-in access/)).toBeInTheDocument();
  });

  it('shouldNotShowLockoutErrorWhenNoValidationErrors', () => {
    mockIsPristine = false;
    mockHasValidationErrors = false;
    renderPage();

    expect(screen.queryByText(/Cannot disable Classic UI when both Anonymous and Logged-in access/)).not.toBeInTheDocument();
  });

  it('shouldShowSaveErrorWhenNoValidationErrors', () => {
    mockSaveError = 'Network error';
    mockHasValidationErrors = false;
    renderPage();

    expect(screen.getByText('Network error')).toBeInTheDocument();
  });

  it('shouldShowValidationErrorInsteadOfSaveErrorWhenBothExist', () => {
    mockIsPristine = false;
    mockSaveError = 'Network error';
    mockHasValidationErrors = true;
    mockValidationErrors = { disableLegacyUi: 'Cannot disable Classic UI when both Anonymous and Logged-in access to Nexus One UI are disabled. At least one UI access method must remain enabled to prevent lockout.' };
    renderPage();

    expect(screen.getByText(/Cannot disable Classic UI when both Anonymous and Logged-in access/)).toBeInTheDocument();
    expect(screen.queryByText('Network error')).not.toBeInTheDocument();
  });
});
