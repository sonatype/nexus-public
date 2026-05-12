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
import '@testing-library/jest-dom';

import PreviewUiSettings from '../PreviewUiSettings';
import * as usePreviewUiSettingsFormModule from '../../../../super/settings/system/preview-ui/usePreviewUiSettingsForm';

jest.mock('../../../../super/settings/system/preview-ui/usePreviewUiSettingsForm');

const mockedUseForm = usePreviewUiSettingsFormModule.usePreviewUiSettingsForm;

// Provide UIStrings.SETTINGS so that constants/UIStrings.jsx (which re-exports from this module) works
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ContentBody: ({ children, className }) => <div className={className}>{children}</div>,
  Page: ({ children }) => <div>{children}</div>,
  PageHeader: ({ children }) => <header>{children}</header>,
  PageTitle: ({ text }) => <h1>{text}</h1>,
  Section: ({ children }) => <section>{children}</section>,
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
  },
  FormUtils: {
    discardTooltip: jest.fn(() => ''),
  },
  // UIStrings is re-exported by constants/UIStrings.jsx — provide minimum needed keys
  UIStrings: {
    SETTINGS: {
      DISCARD_BUTTON_LABEL: 'Discard',
      SAVE_BUTTON_LABEL: 'Save',
    },
  },
  APIConstants: {
    REST: {
      INTERNAL: {
        PREVIEW_UI_SETTINGS: '/service/rest/internal/ui/preview-ui-settings',
      },
    },
  },
}));

jest.mock('@sonatype/react-shared-components', () => ({
  NxLoadWrapper: ({ loading, error, children }) => {
    if (loading) return <div>Loading...</div>;
    if (error) return <div>Error: {error}</div>;
    return <div>{children}</div>;
  },
  NxStatefulForm: ({ onSubmit, loading, submitError, additionalFooterBtns, children }) => (
    <form data-loading={loading}>
      {submitError && <div role="alert">{submitError}</div>}
      {typeof children === 'function' ? children() : children}
      {additionalFooterBtns}
      <button type="button" disabled={!!loading} onClick={() => onSubmit()}>Save</button>
    </form>
  ),
  NxCheckbox: ({ checkboxId, isChecked, onChange, disabled, children }) => (
    <label>
      <input
        type="checkbox"
        id={checkboxId}
        checked={isChecked}
        onChange={onChange}
        disabled={disabled}
        data-testid={checkboxId}
      />
      {children}
    </label>
  ),
  NxButton: ({ children, onClick, className, type }) => (
    <button type={type} onClick={onClick} className={className}>{children}</button>
  ),
  NxFieldset: ({ label, children }) => (
    <fieldset>
      <legend>{label}</legend>
      {children}
    </fieldset>
  ),
  NxTooltip: ({ children }) => <>{children}</>,
}));

const DEFAULT_SETTINGS = {
  anonymousEnabled: false,
  loggedInEnabled: true,
  defaultToPreviewUi: false,
  disableLegacyUi: false,
  disableSwitchFeedback: false,
};

function createFormMock(data = DEFAULT_SETTINGS, overrides = {}) {
  const checkboxOnChange = jest.fn();
  return {
    data,
    isPristine: true,
    isLoading: false,
    isSaving: false,
    loadError: null,
    saveError: null,
    checkbox: jest.fn((name) => ({
      checked: Boolean(data[name]),
      onChange: checkboxOnChange,
    })),
    submit: jest.fn(),
    reset: jest.fn(),
    field: jest.fn(),
    send: jest.fn(),
    ...overrides,
  };
}

function renderPage() {
  return render(<PreviewUiSettings />);
}

describe('PreviewUiSettings (heritage)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    const { ExtJS } = require('@sonatype/nexus-ui-plugin');
    ExtJS.checkPermission.mockReturnValue(true);
    mockedUseForm.mockReturnValue(createFormMock());
  });

  it('shows loading state when form is loading', () => {
    mockedUseForm.mockReturnValue(createFormMock(DEFAULT_SETTINGS, { isLoading: true }));
    renderPage();
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('shows load error with retry option', () => {
    mockedUseForm.mockReturnValue(createFormMock(DEFAULT_SETTINGS, { loadError: 'Failed to load settings' }));
    renderPage();
    expect(screen.getByText(/Failed to load settings/)).toBeInTheDocument();
  });

  it('renders page title', () => {
    renderPage();
    expect(screen.getByText('Nexus One UI')).toBeInTheDocument();
  });

  it('renders all fieldsets', () => {
    renderPage();
    expect(screen.getByText('Anonymous Users')).toBeInTheDocument();
    expect(screen.getByText('Logged-in Users')).toBeInTheDocument();
    expect(screen.getByText('Rollout Control')).toBeInTheDocument();
  });

  it('Discard button has disabled class when form is pristine', () => {
    renderPage();
    expect(screen.getByRole('button', { name: /discard/i })).toHaveClass('disabled');
  });

  it('Discard button does not have disabled class when form is dirty', () => {
    mockedUseForm.mockReturnValue(createFormMock(DEFAULT_SETTINGS, { isPristine: false }));
    renderPage();
    expect(screen.getByRole('button', { name: /discard/i })).not.toHaveClass('disabled');
  });

  it('shows save error when saveError is set', () => {
    mockedUseForm.mockReturnValue(createFormMock(DEFAULT_SETTINGS, { saveError: 'Failed to save' }));
    renderPage();
    expect(screen.getByText('Failed to save')).toBeInTheDocument();
  });

  it('calls form.submit() when Save is clicked', () => {
    const mockSubmit = jest.fn();
    mockedUseForm.mockReturnValue(createFormMock(DEFAULT_SETTINGS, { isPristine: false, submit: mockSubmit }));
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /save/i }));
    expect(mockSubmit).toHaveBeenCalledTimes(1);
  });

  it('calls form.reset() when Discard is clicked', () => {
    const mockReset = jest.fn();
    mockedUseForm.mockReturnValue(createFormMock(DEFAULT_SETTINGS, { isPristine: false, reset: mockReset }));
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /discard/i }));
    expect(mockReset).toHaveBeenCalledTimes(1);
  });

  it('calls form.checkbox for each field', () => {
    const mock = createFormMock();
    mockedUseForm.mockReturnValue(mock);
    renderPage();
    expect(mock.checkbox).toHaveBeenCalledWith('anonymousEnabled');
    expect(mock.checkbox).toHaveBeenCalledWith('loggedInEnabled');
    expect(mock.checkbox).toHaveBeenCalledWith('defaultToPreviewUi');
    expect(mock.checkbox).toHaveBeenCalledWith('disableSwitchFeedback');
  });

  it('disables all checkboxes when user lacks update permission', () => {
    const { ExtJS } = require('@sonatype/nexus-ui-plugin');
    ExtJS.checkPermission.mockReturnValue(false);
    renderPage();
    expect(screen.getByTestId('anonymousEnabled')).toBeDisabled();
    expect(screen.getByTestId('loggedInEnabled')).toBeDisabled();
    expect(screen.getByTestId('defaultToPreviewUi')).toBeDisabled();
    expect(screen.getByTestId('disableSwitchFeedback')).toBeDisabled();
  });

  it('renders the switch feedback checkbox', () => {
    renderPage();
    expect(screen.getByTestId('disableSwitchFeedback')).toBeInTheDocument();
    expect(screen.getByText(/Hide the feedback prompt and prevent Nexus One UI from sending feedback/i))
        .toBeInTheDocument();
  });
});
