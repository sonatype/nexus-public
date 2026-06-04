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
import {fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ExtJS} from '@sonatype/nexus-ui-plugin';

import RawQueryParamsConfiguration from './RawQueryParamsConfiguration';
import UIStrings from '../../../../../constants/UIStrings';

jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ...jest.requireActual('@sonatype/nexus-ui-plugin'),
  ExtJS: {
    state: jest.fn()
  }
}));

const {RAW: {QUERY_PARAMS}} = UIStrings.REPOSITORIES.EDITOR;

describe('RawQueryParamsConfiguration', () => {
  const mockSend = jest.fn();

  const createParentMachine = (forwardQueryParameters = false, excludedQueryParameters = []) => [
    {
      context: {
        data: {
          raw: {
            forwardQueryParameters,
            excludedQueryParameters,
            newExcludedParameter: ''
          }
        },
        pristineData: {
          raw: {
            forwardQueryParameters: false,
            excludedQueryParameters: []
          }
        }
      }
    },
    mockSend
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue(true)
    });
  });

  it('renders the fieldset with caption and sublabel', () => {
    render(<RawQueryParamsConfiguration parentMachine={createParentMachine()} />);

    expect(screen.getByText(QUERY_PARAMS.CAPTION)).toBeInTheDocument();
    expect(screen.getByText(QUERY_PARAMS.SUBLABEL)).toBeInTheDocument();
  });

  it('renders the disabled description when forwarding is off', () => {
    render(<RawQueryParamsConfiguration parentMachine={createParentMachine(false)} />);

    expect(screen.getByText(QUERY_PARAMS.DESCRIPTION)).toBeInTheDocument();
    expect(screen.queryByText(QUERY_PARAMS.DESCRIPTION_ENABLED)).not.toBeInTheDocument();
  });

  it('renders the enabled description when forwarding is on', () => {
    render(<RawQueryParamsConfiguration parentMachine={createParentMachine(true)} />);

    expect(screen.getByText(QUERY_PARAMS.DESCRIPTION_ENABLED)).toBeInTheDocument();
    expect(screen.queryByText(QUERY_PARAMS.DESCRIPTION)).not.toBeInTheDocument();
  });

  it('renders usage examples when forwarding is enabled', () => {
    render(<RawQueryParamsConfiguration parentMachine={createParentMachine(true)} />);

    expect(screen.getByText(QUERY_PARAMS.EXAMPLES_TITLE)).toBeInTheDocument();
    QUERY_PARAMS.EXAMPLES.forEach(example => {
      expect(screen.getByText(example)).toBeInTheDocument();
    });
  });

  it('renders common use cases when forwarding is enabled', () => {
    render(<RawQueryParamsConfiguration parentMachine={createParentMachine(true)} />);

    expect(screen.getByText(QUERY_PARAMS.USE_CASES_TITLE)).toBeInTheDocument();
    QUERY_PARAMS.USE_CASES.forEach(useCase => {
      expect(screen.getByText(useCase)).toBeInTheDocument();
    });
  });

  it('does not render examples, warnings, or exclusions when forwarding is disabled', () => {
    render(<RawQueryParamsConfiguration parentMachine={createParentMachine(false)} />);

    expect(screen.queryByText(QUERY_PARAMS.EXAMPLES_TITLE)).not.toBeInTheDocument();
    expect(screen.queryByText(QUERY_PARAMS.CACHING_WARNING_TITLE)).not.toBeInTheDocument();
    expect(screen.queryByText(QUERY_PARAMS.EXCLUSION_LABEL)).not.toBeInTheDocument();
  });

  it('renders caching warning when forwarding is enabled', () => {
    render(<RawQueryParamsConfiguration parentMachine={createParentMachine(true)} />);

    expect(screen.getByText(QUERY_PARAMS.CACHING_WARNING_TITLE)).toBeInTheDocument();
    expect(screen.getByText(QUERY_PARAMS.CACHING_WARNING_CONTENT)).toBeInTheDocument();
  });

  it('renders exclusion fieldset when forwarding is enabled', () => {
    render(<RawQueryParamsConfiguration parentMachine={createParentMachine(true)} />);

    expect(screen.getByText(QUERY_PARAMS.EXCLUSION_LABEL)).toBeInTheDocument();
    expect(screen.getByText(QUERY_PARAMS.EXCLUSION_SUBLABEL)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(QUERY_PARAMS.EXCLUSION_PLACEHOLDER)).toBeInTheDocument();
  });

  describe('checkbox toggle', () => {
    it('dispatches UPDATE to enable forwarding when checkbox is clicked', () => {
      render(<RawQueryParamsConfiguration parentMachine={createParentMachine(false)} />);

      userEvent.click(screen.getByRole('checkbox'));

      expect(mockSend).toHaveBeenCalledWith({
        type: 'UPDATE',
        name: 'raw.forwardQueryParameters',
        value: true
      });
    });

    it('dispatches UPDATE to disable forwarding when checkbox is unchecked', () => {
      render(<RawQueryParamsConfiguration parentMachine={createParentMachine(true)} />);

      userEvent.click(screen.getByRole('checkbox'));

      expect(mockSend).toHaveBeenCalledWith({
        type: 'UPDATE',
        name: 'raw.forwardQueryParameters',
        value: false
      });
    });
  });

  describe('exclusion management', () => {
    it('adds an exclusion when the add button is clicked', () => {
      render(<RawQueryParamsConfiguration parentMachine={createParentMachine(true)} />);

      const input = screen.getByPlaceholderText(QUERY_PARAMS.EXCLUSION_PLACEHOLDER);
      fireEvent.change(input, {target: {value: 'api_key'}});

      const addButton = screen.getAllByRole('button').find(btn =>
        btn.querySelector('.fa-plus-circle, [data-icon="plus-circle"]') ||
        btn.classList.contains('nx-btn--icon-only')
      );
      userEvent.click(addButton);

      expect(mockSend).toHaveBeenCalledWith({
        type: 'UPDATE',
        name: 'raw.excludedQueryParameters',
        value: ['api_key']
      });
    });

    it('adds an exclusion when Enter key is pressed', () => {
      render(<RawQueryParamsConfiguration parentMachine={createParentMachine(true)} />);

      const input = screen.getByPlaceholderText(QUERY_PARAMS.EXCLUSION_PLACEHOLDER);
      fireEvent.change(input, {target: {value: 'session_id'}});
      fireEvent.keyDown(input, {key: 'Enter', code: 'Enter'});

      expect(mockSend).toHaveBeenCalledWith({
        type: 'UPDATE',
        name: 'raw.excludedQueryParameters',
        value: ['session_id']
      });
    });

    it('trims whitespace from exclusion input', () => {
      render(<RawQueryParamsConfiguration parentMachine={createParentMachine(true)} />);

      const input = screen.getByPlaceholderText(QUERY_PARAMS.EXCLUSION_PLACEHOLDER);
      fireEvent.change(input, {target: {value: '  api_key  '}});
      fireEvent.keyDown(input, {key: 'Enter', code: 'Enter'});

      expect(mockSend).toHaveBeenCalledWith({
        type: 'UPDATE',
        name: 'raw.excludedQueryParameters',
        value: ['api_key']
      });
    });

    it('does not add empty exclusion', () => {
      render(<RawQueryParamsConfiguration parentMachine={createParentMachine(true)} />);

      const input = screen.getByPlaceholderText(QUERY_PARAMS.EXCLUSION_PLACEHOLDER);
      fireEvent.change(input, {target: {value: '   '}});
      fireEvent.keyDown(input, {key: 'Enter', code: 'Enter'});

      expect(mockSend).not.toHaveBeenCalled();
    });

    it('does not add duplicate exclusion (case-insensitive)', () => {
      render(<RawQueryParamsConfiguration parentMachine={createParentMachine(true, ['api_key'])} />);

      const input = screen.getByPlaceholderText(QUERY_PARAMS.EXCLUSION_PLACEHOLDER);
      fireEvent.change(input, {target: {value: 'API_KEY'}});
      fireEvent.keyDown(input, {key: 'Enter', code: 'Enter'});

      expect(mockSend).not.toHaveBeenCalled();
    });

    it('renders existing exclusions in a list', () => {
      render(<RawQueryParamsConfiguration parentMachine={createParentMachine(true, ['api_key', 'token'])} />);

      expect(screen.getByText('api_key')).toBeInTheDocument();
      expect(screen.getByText('token')).toBeInTheDocument();
    });

    it('removes an exclusion when the trash button is clicked', () => {
      render(<RawQueryParamsConfiguration parentMachine={createParentMachine(true, ['api_key', 'token'])} />);

      const exclusionList = document.querySelector('.nxrm-query-params-exclusion-list');
      const removeButtons = exclusionList.querySelectorAll('.nx-btn--icon-only');
      userEvent.click(removeButtons[0]);

      expect(mockSend).toHaveBeenCalledWith({
        type: 'UPDATE',
        name: 'raw.excludedQueryParameters',
        value: ['token']
      });
    });

    it('removes the correct exclusion when there are multiple', () => {
      render(<RawQueryParamsConfiguration parentMachine={createParentMachine(true, ['a', 'b', 'c'])} />);

      const exclusionList = document.querySelector('.nxrm-query-params-exclusion-list');
      const removeButtons = exclusionList.querySelectorAll('.nx-btn--icon-only');
      userEvent.click(removeButtons[1]);

      expect(mockSend).toHaveBeenCalledWith({
        type: 'UPDATE',
        name: 'raw.excludedQueryParameters',
        value: ['a', 'c']
      });
    });

    it('does not render exclusion list when no exclusions exist', () => {
      render(<RawQueryParamsConfiguration parentMachine={createParentMachine(true, [])} />);

      expect(document.querySelector('.nxrm-query-params-exclusion-list')).not.toBeInTheDocument();
    });
  });
});
