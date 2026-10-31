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

import React, { useState } from 'react';
import { act, fireEvent, render, screen } from '@testing-library/react';

import { TextFilter } from '../TextFilter';

/**
 * The debounce window this component is expected to honour. It is asserted as a value rather
 * than imported so the test pins the intended user-facing behaviour, not whatever the module
 * happens to declare.
 */
const DEBOUNCE_MS = 300;

/**
 * `TextFilter` is a controlled component: it reports keystrokes through `onChange` and expects the
 * parent to feed the new value back. This harness supplies that parent so typing behaves as it
 * does in the sidebar.
 */
function Harness({ onSearch, debounceMs }: { onSearch?: () => void; debounceMs?: number }): JSX.Element {
  const [value, setValue] = useState('');
  return (
    <TextFilter
      id="name"
      label="Name"
      value={value}
      onChange={(_id, next) => setValue(next)}
      onSearch={onSearch}
      debounceMs={debounceMs}
    />
  );
}

function advance(ms: number): void {
  act(() => {
    jest.advanceTimersByTime(ms);
  });
}

function type(input: HTMLElement, text: string): void {
  for (const char of text) {
    act(() => {
      fireEvent.change(input, { target: { value: (input as HTMLInputElement).value + char } });
    });
  }
}

describe('TextFilter', () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  const getInput = () => screen.getByLabelText('Name');

  it('reflects every keystroke immediately so typing stays responsive', () => {
    const onChange = jest.fn();
    render(<TextFilter id="name" label="Name" value="" onChange={onChange} />);

    fireEvent.change(getInput(), { target: { value: 'n' } });

    expect(onChange).toHaveBeenCalledWith('name', 'n');
  });

  it('does not search while the user is still typing', () => {
    const onSearch = jest.fn();
    render(<Harness onSearch={onSearch} />);

    type(getInput(), 'nexus');
    advance(DEBOUNCE_MS - 1);

    expect(onSearch).not.toHaveBeenCalled();
  });

  it('searches once, after the user stops typing', () => {
    const onSearch = jest.fn();
    render(<Harness onSearch={onSearch} />);

    type(getInput(), 'nexus');
    advance(DEBOUNCE_MS);

    expect(onSearch).toHaveBeenCalledTimes(1);
    expect(getInput()).toHaveValue('nexus');
  });

  it('does not issue a search per keystroke when typing is paced under the debounce window', () => {
    const onSearch = jest.fn();
    render(<Harness onSearch={onSearch} />);

    const input = getInput();
    for (const char of 'nexus') {
      type(input, char);
      advance(DEBOUNCE_MS - 100);
    }
    expect(onSearch).not.toHaveBeenCalled();

    advance(DEBOUNCE_MS);
    expect(onSearch).toHaveBeenCalledTimes(1);
  });

  it('searches immediately on Enter, without also firing the pending debounce', () => {
    const onSearch = jest.fn();
    render(<Harness onSearch={onSearch} />);

    const input = getInput();
    type(input, 'nexus');
    act(() => {
      fireEvent.keyDown(input, { key: 'Enter' });
    });

    expect(onSearch).toHaveBeenCalledTimes(1);

    // The keystroke's pending timer was cancelled, so no second search follows.
    advance(DEBOUNCE_MS * 2);
    expect(onSearch).toHaveBeenCalledTimes(1);
  });

  it('clears the value and searches when the clear button is used', () => {
    const onSearch = jest.fn();
    render(<Harness onSearch={onSearch} />);

    const input = getInput();
    type(input, 'nexus');
    act(() => {
      fireEvent.click(screen.getByRole('button', { name: 'Clear Name filter' }));
    });

    expect(input).toHaveValue('');

    // Clearing searches at once rather than waiting out the debounce window.
    advance(0);
    expect(onSearch).toHaveBeenCalledTimes(1);

    // The cancelled typing timer must not produce a second search.
    advance(DEBOUNCE_MS * 2);
    expect(onSearch).toHaveBeenCalledTimes(1);
  });

  it('clears the value on Escape', () => {
    render(<Harness />);

    const input = getInput();
    type(input, 'nexus');
    act(() => {
      fireEvent.keyDown(input, { key: 'Escape' });
    });

    expect(input).toHaveValue('');
  });

  it('does not search after unmount when a debounce is pending', () => {
    const onSearch = jest.fn();
    const { unmount } = render(<Harness onSearch={onSearch} />);

    type(getInput(), 'nexus');
    unmount();
    advance(DEBOUNCE_MS * 2);

    expect(onSearch).not.toHaveBeenCalled();
  });

  it('honours a caller-supplied debounceMs prop instead of the default', () => {
    const onSearch = jest.fn();
    const customDebounce = 500;
    render(<Harness onSearch={onSearch} debounceMs={customDebounce} />);

    type(getInput(), 'nexus');
    advance(DEBOUNCE_MS);
    expect(onSearch).not.toHaveBeenCalled();

    advance(customDebounce - DEBOUNCE_MS);
    expect(onSearch).toHaveBeenCalledTimes(1);
  });
});
