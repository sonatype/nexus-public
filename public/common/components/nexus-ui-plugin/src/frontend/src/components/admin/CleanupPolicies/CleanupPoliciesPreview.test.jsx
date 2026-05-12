/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React from 'react';
import {render, screen, fireEvent} from '@testing-library/react';
import {useMachine} from '@xstate/react';
import CleanupPoliciesPreview from './CleanupPoliciesPreview';

// Mock the useMachine hook
jest.mock('@xstate/react');

// Mock NxWarningAlert component
jest.mock('@sonatype/react-shared-components', () => ({
  ...jest.requireActual('@sonatype/react-shared-components'),
  NxWarningAlert: ({onClose, children}) => (
    <div data-testid="nx-warning-alert">
      <button data-testid="close-button" onClick={onClose}>X</button>
      {children}
    </div>
  ),
  NxButton: ({onClick, children, className}) => (
    <button onClick={onClick} className={className}>{children}</button>
  ),
  NxFilterInput: () => <input />,
  NxFormSelect: ({onChange, value, children}) => (
    <select onChange={(e) => onChange(e.target.value)} value={value}>
      {children}
    </select>
  ),
  NxLoadWrapper: ({children}) => <>{children()}</>,
  NxTable: ({children}) => <table>{children}</table>,
  NxTableBody: ({children}) => <tbody>{children}</tbody>,
  NxTableCell: ({children}) => <td>{children}</td>,
  NxTableHead: ({children}) => <thead>{children}</thead>,
  NxTableRow: ({children}) => <tr>{children}</tr>,
  NxTooltip: ({children}) => <>{children}</>
}));

describe('CleanupPoliciesPreview', () => {
  const mockPolicyData = {
    format: 'maven2',
    criteriaLastBlobUpdated: '10'
  };

  const mockSendToForm = jest.fn();
  const mockSendToList = jest.fn();

  const defaultFormState = {
    context: {
      error: null,
      repository: '',
      repositories: [{id: 'repo1', name: 'Repo 1'}]
    },
    matches: jest.fn().mockReturnValue(false)
  };

  const defaultListState = {
    context: {
      error: null,
      data: [{name: 'comp1', group: 'group1', version: '1.0'}],
      total: 100,
      filter: '',
      isAlertShown: true
    },
    matches: jest.fn().mockReturnValue(false)
  };

  beforeEach(() => {
    jest.clearAllMocks();
    useMachine
      .mockReturnValueOnce([defaultFormState, mockSendToForm])
      .mockReturnValueOnce([defaultListState, mockSendToList]);
  });

  it('should send HIDE_ALERT to list machine when alert close button is clicked', async () => {
    render(<CleanupPoliciesPreview policyData={mockPolicyData} />);

    // Verify the alert is shown
    expect(screen.getByTestId('nx-warning-alert')).toBeInTheDocument();

    // Click the close button
    fireEvent.click(screen.getByTestId('close-button'));

    // Verify HIDE_ALERT was sent to the list machine, not the form machine
    expect(mockSendToList).toHaveBeenCalledWith({type: 'HIDE_ALERT'});
    expect(mockSendToForm).not.toHaveBeenCalledWith({type: 'HIDE_ALERT'});
  });

  it('should not show alert when isAlertShown is false', () => {
    const listStateWithoutAlert = {
      ...defaultListState,
      context: {
        ...defaultListState.context,
        isAlertShown: false
      }
    };

    // Clear previous mock implementations and set new ones
    useMachine.mockReset();
    useMachine
      .mockReturnValueOnce([defaultFormState, mockSendToForm])
      .mockReturnValueOnce([listStateWithoutAlert, mockSendToList]);

    render(<CleanupPoliciesPreview policyData={mockPolicyData} />);

    expect(screen.queryByTestId('nx-warning-alert')).not.toBeInTheDocument();
  });
});
