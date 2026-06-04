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
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { DockerSearchInput } from '../DockerSearchInput';

jest.mock('../mockData', () => ({
  ...jest.requireActual('../mockData'),
  mockSuggestApi: jest.fn(),
}));

import { mockSuggestApi } from '../mockData';
const mockSuggest = mockSuggestApi as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();
  jest.useFakeTimers();
  mockSuggest.mockResolvedValue({ suggestions: [] });
});

afterEach(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});

describe('DockerSearchInput', () => {
  it('renders with placeholder', () => {
    render(
      <DockerSearchInput
        value=""
        onChange={jest.fn()}
        onSearch={jest.fn()}
        placeholder="Search Docker images..."
      />
    );

    expect(screen.getByRole('combobox', { name: /search docker images/i })).toBeInTheDocument();
  });

  it('calls onChange when typing', async () => {
    const onChange = jest.fn();
    render(
      <DockerSearchInput value="" onChange={onChange} onSearch={jest.fn()} />
    );

    const input = screen.getByRole('combobox');
    fireEvent.change(input, { target: { value: 'nginx' } });

    expect(onChange).toHaveBeenCalledWith('nginx');
  });

  it('calls onSearch when Enter pressed with no suggestions showing', () => {
    const onSearch = jest.fn();
    render(
      <DockerSearchInput value="nginx" onChange={jest.fn()} onSearch={onSearch} />
    );

    const input = screen.getByRole('combobox');
    fireEvent.keyDown(input, { key: 'Enter' });

    expect(onSearch).toHaveBeenCalledWith('nginx');
  });

  it('does not fetch suggestions when value length < 2', () => {
    render(
      <DockerSearchInput value="n" onChange={jest.fn()} onSearch={jest.fn()} />
    );

    act(() => {
      jest.runAllTimers();
    });

    expect(mockSuggest).not.toHaveBeenCalled();
  });

  it('fetches suggestions when value length >= 2', async () => {
    const suggestions = [
      { id: 'nginx', displayText: 'nginx' },
      { id: 'nginx-alpine', displayText: 'nginx-alpine' },
    ];
    mockSuggest.mockResolvedValue({ suggestions });

    render(
      <DockerSearchInput value="ng" onChange={jest.fn()} onSearch={jest.fn()} />
    );

    await act(async () => {
      jest.runAllTimers();
      await Promise.resolve();
    });

    await waitFor(() => {
      expect(screen.getByText('nginx')).toBeInTheDocument();
    });
  });

  it('calls onSuggestionSelect when suggestion clicked', async () => {
    const suggestions = [{ id: 'nginx-id', displayText: 'nginx' }];
    mockSuggest.mockResolvedValue({ suggestions });
    const onSuggestionSelect = jest.fn();

    render(
      <DockerSearchInput
        value="ng"
        onChange={jest.fn()}
        onSearch={jest.fn()}
        onSuggestionSelect={onSuggestionSelect}
      />
    );

    await act(async () => {
      jest.runAllTimers();
      await Promise.resolve();
    });

    await waitFor(() => screen.getByText('nginx'));
    fireEvent.click(screen.getByText('nginx'));

    expect(onSuggestionSelect).toHaveBeenCalledWith('nginx-id');
  });

  it('calls onChange and onSearch when suggestion clicked without onSuggestionSelect', async () => {
    const suggestions = [{ id: 'nginx-id', displayText: 'nginx' }];
    mockSuggest.mockResolvedValue({ suggestions });
    const onChange = jest.fn();
    const onSearch = jest.fn();

    render(
      <DockerSearchInput value="ng" onChange={onChange} onSearch={onSearch} />
    );

    await act(async () => {
      jest.runAllTimers();
      await Promise.resolve();
    });

    await waitFor(() => screen.getByText('nginx'));
    fireEvent.click(screen.getByText('nginx'));

    expect(onChange).toHaveBeenCalledWith('nginx');
    expect(onSearch).toHaveBeenCalledWith('nginx');
  });

  it('is disabled when disabled prop is true', () => {
    render(
      <DockerSearchInput value="" onChange={jest.fn()} onSearch={jest.fn()} disabled />
    );

    expect(screen.getByRole('combobox')).toBeDisabled();
  });
});
