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
import { act, render, screen, waitFor } from '@testing-library/react';
import UnsavedChangesModal from './UnsavedChangesModal';
import * as unsavedChangesDialog from '../../../router/unsavedChangesDialog';

describe('UnsavedChangesModal', () => {
  const selectors = {
    cancelButton: () => screen.queryByRole('button', { name: 'Cancel' }),
    continueButton: () => screen.queryByRole('button', { name: 'Continue' }),
    modalTitle: () => screen.queryByRole('heading', { name: 'Unsaved Changes' }),
    modalContent: () => screen.queryByText('You have unsaved changes. Continuing will discard them.')
  };

  let handleCancelSpy;
  let handleContinueSpy;

  beforeEach(() => {
    handleCancelSpy = jest.spyOn(unsavedChangesDialog, 'handleCancel');
    handleContinueSpy = jest.spyOn(unsavedChangesDialog, 'handleContinue');
  });

  afterEach(() => {
    // Clear the module-level dialog singleton (setVisible reference + any
    // unresolved resolveFn) between cases so a leaked prompt from one test
    // can't leave the next test starting with visible=true. Wrapped in act()
    // because resetDialogState() calls setVisible(false) on the still-mounted
    // component (RTL auto-cleanup fires after this afterEach hook).
    act(() => {
      unsavedChangesDialog.resetDialogState();
    });
  });

  function renderComponent() {
    return render(<UnsavedChangesModal />);
  }

  it('should not render the modal when not visible', () => {
    renderComponent();

    expect(selectors.modalTitle()).not.toBeInTheDocument();
    expect(selectors.modalContent()).not.toBeInTheDocument();
  });

  it('should render the modal when visible', async () => {
    renderComponent();

    act(() => {
      unsavedChangesDialog.showUnsavedChangesModal();
    });

    await waitFor(() => {
      expect(selectors.modalTitle()).toBeInTheDocument();
      expect(selectors.modalContent()).toBeInTheDocument();
    });
  });

  it('should call handleCancel when cancel button is clicked', async () => {
    renderComponent();

    act(() => {
      unsavedChangesDialog.showUnsavedChangesModal();
    });

    await waitFor(() => {
      expect(selectors.cancelButton()).toBeInTheDocument();
      expect(selectors.continueButton()).toBeInTheDocument();
    });

    act(() => {
      selectors.cancelButton().click();
    });

    expect(handleCancelSpy).toHaveBeenCalled();
  });

  it('should call handleContinue when continue button is clicked', async () => {
    renderComponent();

    act(() => {
      unsavedChangesDialog.showUnsavedChangesModal();
    });

    await waitFor(() => {
      expect(selectors.cancelButton()).toBeInTheDocument();
      expect(selectors.continueButton()).toBeInTheDocument();
    });

    act(() => {
      selectors.continueButton().click();
    });

    expect(handleContinueSpy).toHaveBeenCalled();
  });

  // App.test.jsx now exercises the "hide after click" path against a portal-free
  // mock (NEXUS-53515), so these two cases keep the real NxModal-backed removal
  // path covered here. Without them, dropping setVisible(false) from either
  // handler in unsavedChangesDialog.js would silently pass every test.
  it('should hide the modal after the cancel button is clicked', async () => {
    renderComponent();

    act(() => {
      unsavedChangesDialog.showUnsavedChangesModal();
    });

    await waitFor(() => {
      expect(selectors.cancelButton()).toBeInTheDocument();
    });

    act(() => {
      selectors.cancelButton().click();
    });

    await waitFor(() => {
      expect(selectors.modalTitle()).not.toBeInTheDocument();
    });
    expect(selectors.modalContent()).not.toBeInTheDocument();
    expect(selectors.cancelButton()).not.toBeInTheDocument();
  });

  it('should hide the modal after the continue button is clicked', async () => {
    renderComponent();

    act(() => {
      unsavedChangesDialog.showUnsavedChangesModal();
    });

    await waitFor(() => {
      expect(selectors.continueButton()).toBeInTheDocument();
    });

    act(() => {
      selectors.continueButton().click();
    });

    await waitFor(() => {
      expect(selectors.modalTitle()).not.toBeInTheDocument();
    });
    expect(selectors.modalContent()).not.toBeInTheDocument();
    expect(selectors.continueButton()).not.toBeInTheDocument();
  });
});
