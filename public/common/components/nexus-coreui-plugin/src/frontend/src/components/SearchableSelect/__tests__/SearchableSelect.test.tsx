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
import {render, screen, fireEvent} from '@testing-library/react';
import {SearchableSelect} from '../SearchableSelect';

const OPTIONS = [
  {value: 'maven', label: 'Maven'},
  {value: 'npm', label: 'NPM'},
  {value: 'docker', label: 'Docker'},
  {value: 'pypi', label: 'PyPI'},
];

function renderSelect(overrides = {}) {
  const onChange = jest.fn();
  render(<SearchableSelect options={OPTIONS} value="" onChange={onChange} {...overrides} />);
  return {onChange};
}

describe('SearchableSelect', () => {
  describe('rendering', () => {
    it('shows placeholder when no value is selected', () => {
      renderSelect({placeholder: 'Pick a format'});
      expect(screen.getByText('Pick a format')).toBeInTheDocument();
    });

    it('shows the selected option label', () => {
      renderSelect({value: 'maven'});
      expect(screen.getByText('Maven')).toBeInTheDocument();
    });

    it('renders a trigger button', () => {
      renderSelect();
      expect(screen.getByRole('button')).toBeInTheDocument();
    });

    it('shows a clear button when a value is set', () => {
      renderSelect({value: 'npm'});
      expect(screen.getByRole('button', {name: /clear/i})).toBeInTheDocument();
    });

    it('does not show a clear button when value is empty', () => {
      renderSelect({value: ''});
      expect(screen.queryByRole('button', {name: /clear/i})).not.toBeInTheDocument();
    });

    it('renders option icons when provided', () => {
      const opts = [{value: 'a', label: 'Alpha', icon: <span data-testid="icon-a">A</span>}];
      renderSelect({options: opts, value: 'a'});
      expect(screen.getByTestId('icon-a')).toBeInTheDocument();
    });
  });

  describe('open / close', () => {
    it('opens dropdown on trigger click', () => {
      renderSelect();
      fireEvent.click(screen.getByRole('button'));
      expect(screen.getByRole('listbox')).toBeInTheDocument();
    });

    it('lists all options when opened', () => {
      renderSelect();
      fireEvent.click(screen.getByRole('button'));
      OPTIONS.forEach(opt => expect(screen.getByText(opt.label)).toBeInTheDocument());
    });

    it('closes on a second trigger click', () => {
      renderSelect();
      const btn = screen.getByRole('button');
      fireEvent.click(btn);
      fireEvent.click(btn);
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    });

    it('closes when clicking outside', () => {
      renderSelect();
      fireEvent.click(screen.getByRole('button'));
      fireEvent.mouseDown(document.body);
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    });

    it('shows allOptionLabel as the first option in the list', () => {
      renderSelect({allOptionLabel: 'All Formats'});
      fireEvent.click(screen.getAllByRole('button')[0]);
      const options = screen.getAllByRole('option');
      expect(options[0]).toHaveTextContent('All Formats');
    });
  });

  describe('search / filter', () => {
    it('filters options by label', () => {
      renderSelect();
      fireEvent.click(screen.getByRole('button'));
      fireEvent.change(screen.getByPlaceholderText('Search...'), {target: {value: 'doc'}});
      expect(screen.getByText('Docker')).toBeInTheDocument();
      expect(screen.queryByText('Maven')).not.toBeInTheDocument();
    });

    it('shows a no-matches message when no options match', () => {
      renderSelect();
      fireEvent.click(screen.getByRole('button'));
      fireEvent.change(screen.getByPlaceholderText('Search...'), {target: {value: 'zzz'}});
      expect(screen.getByText('No matches found')).toBeInTheDocument();
    });

    it('respects a custom searchPlaceholder', () => {
      renderSelect({searchPlaceholder: 'Filter...'});
      fireEvent.click(screen.getByRole('button'));
      expect(screen.getByPlaceholderText('Filter...')).toBeInTheDocument();
    });

    it('filters by value as well as label', () => {
      renderSelect();
      fireEvent.click(screen.getByRole('button'));
      fireEvent.change(screen.getByPlaceholderText('Search...'), {target: {value: 'npm'}});
      expect(screen.getByText('NPM')).toBeInTheDocument();
    });
  });

  describe('selection', () => {
    it('calls onChange when an option is clicked', () => {
      const {onChange} = renderSelect();
      fireEvent.click(screen.getByRole('button'));
      fireEvent.click(screen.getByText('NPM'));
      expect(onChange).toHaveBeenCalledWith('npm');
    });

    it('closes the dropdown after selection', () => {
      renderSelect();
      fireEvent.click(screen.getByRole('button'));
      fireEvent.click(screen.getByText('Maven'));
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    });

    it('marks the currently selected option with aria-selected', () => {
      renderSelect({value: 'docker'});
      fireEvent.click(screen.getAllByRole('button')[0]);
      expect(screen.getByRole('option', {name: /docker/i})).toHaveAttribute('aria-selected', 'true');
    });

    it('highlights an option on mouse enter', () => {
      renderSelect();
      fireEvent.click(screen.getByRole('button'));
      const opts = screen.getAllByRole('option');
      fireEvent.mouseEnter(opts[1]);
      expect(opts[1]).toHaveClass('searchable-select__option--highlighted');
    });
  });

  describe('clear button', () => {
    it('calls onChange with empty string on clear click', () => {
      const {onChange} = renderSelect({value: 'maven'});
      fireEvent.click(screen.getByRole('button', {name: /clear/i}));
      expect(onChange).toHaveBeenCalledWith('');
    });

    it('does not open the dropdown when clear is clicked', () => {
      renderSelect({value: 'maven'});
      fireEvent.click(screen.getByRole('button', {name: /clear/i}));
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    });
  });

  describe('keyboard navigation', () => {
    function getContainer() {
      return screen.getByRole('button').parentElement!;
    }

    it('opens the dropdown on Enter key', () => {
      renderSelect();
      fireEvent.keyDown(getContainer(), {key: 'Enter'});
      expect(screen.getByRole('listbox')).toBeInTheDocument();
    });

    it('closes the dropdown on Escape key', () => {
      renderSelect();
      fireEvent.keyDown(getContainer(), {key: 'Enter'});
      fireEvent.keyDown(getContainer(), {key: 'Escape'});
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    });

    it('highlights the first option on ArrowDown', () => {
      renderSelect();
      fireEvent.keyDown(getContainer(), {key: 'Enter'});
      fireEvent.keyDown(getContainer(), {key: 'ArrowDown'});
      expect(screen.getAllByRole('option')[0]).toHaveClass('searchable-select__option--highlighted');
    });

    it('moves highlight up on ArrowUp', () => {
      renderSelect();
      fireEvent.keyDown(getContainer(), {key: 'Enter'});
      fireEvent.keyDown(getContainer(), {key: 'ArrowDown'});
      fireEvent.keyDown(getContainer(), {key: 'ArrowDown'});
      fireEvent.keyDown(getContainer(), {key: 'ArrowUp'});
      expect(screen.getByRole('listbox')).toBeInTheDocument();
    });

    it('selects the highlighted option on Enter', () => {
      const {onChange} = renderSelect();
      fireEvent.keyDown(getContainer(), {key: 'Enter'});
      fireEvent.keyDown(getContainer(), {key: 'ArrowDown'});
      fireEvent.keyDown(getContainer(), {key: 'Enter'});
      expect(onChange).toHaveBeenCalledWith(OPTIONS[0].value);
    });

    it('closes the dropdown on Tab key', () => {
      renderSelect();
      fireEvent.keyDown(getContainer(), {key: 'Enter'});
      fireEvent.keyDown(getContainer(), {key: 'Tab'});
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    });

    it('opens dropdown on ArrowDown when closed', () => {
      renderSelect();
      fireEvent.keyDown(getContainer(), {key: 'ArrowDown'});
      expect(screen.getByRole('listbox')).toBeInTheDocument();
    });
  });

  describe('disabled state', () => {
    it('disables the trigger button when disabled prop is true', () => {
      renderSelect({disabled: true});
      expect(screen.getByRole('button')).toBeDisabled();
    });

    it('does not open the dropdown when disabled', () => {
      renderSelect({disabled: true});
      fireEvent.click(screen.getByRole('button'));
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    });

    it('does not respond to keyboard when disabled', () => {
      renderSelect({disabled: true});
      const container = screen.getByRole('button').parentElement!;
      fireEvent.keyDown(container, {key: 'Enter'});
      expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    });
  });
});
