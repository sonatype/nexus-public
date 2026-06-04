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
import { render, screen, fireEvent, within, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import '@testing-library/jest-dom';
import { SettingsCombobox } from '../SettingsCombobox';

const singleOptions = [
  { value: 'users', label: 'Users' },
  { value: 'roles', label: 'Roles' },
  { value: 'privileges', label: 'Privileges' },
];

const multiOptions = [
  { value: 'nx-repository-view-maven-read', label: 'nx-repository-view-maven-read', description: 'All permissions for maven repository views' },
  { value: 'nx-repository-view-npm-read', label: 'nx-repository-view-npm-read', description: 'All permissions for npm repository views' },
  { value: 'nx-repository-admin-maven-edit', label: 'nx-repository-admin-maven-edit', description: 'Admin permissions for maven repositories' },
  { value: 'nx-application-users-all', label: 'nx-application-users-all', description: 'All application user permissions' },
  { value: 'nx-script-run', label: 'nx-script-run', description: 'Permission to run scripts' },
];

const groupByFn = (opt: { value: string }) => {
  if (opt.value.startsWith('nx-repository-view')) return 'Repository View';
  if (opt.value.startsWith('nx-repository-admin')) return 'Repository Admin';
  if (opt.value.startsWith('nx-application')) return 'Application';
  return 'Other';
};

describe('SettingsCombobox', () => {
  describe('Single mode', () => {
    it('renders with label and input', () => {
      render(
        <SettingsCombobox name="domain" label="Domain" options={singleOptions} />
      );

      expect(screen.getByLabelText('Domain')).toBeInTheDocument();
      expect(screen.getByTestId('combobox-domain')).toBeInTheDocument();
    });

    it('shows options when input is focused', () => {
      render(
        <SettingsCombobox name="domain" label="Domain" options={singleOptions} />
      );

      fireEvent.focus(screen.getByTestId('combobox-domain'));

      expect(screen.getByRole('listbox')).toBeInTheDocument();
      expect(screen.getByText('Users')).toBeInTheDocument();
      expect(screen.getByText('Roles')).toBeInTheDocument();
    });

    it('filters options based on input text', async () => {
      render(
        <SettingsCombobox name="domain" label="Domain" options={singleOptions} />
      );

      const input = screen.getByTestId('combobox-domain');
      fireEvent.focus(input);
      fireEvent.change(input, { target: { value: 'rol' } });

      // Wait longer for React to update the filtered options
      await waitFor(
        () => {
          const listbox = screen.getByRole('listbox');
          const options = within(listbox).getAllByRole('option');
          expect(options).toHaveLength(1);
          expect(options[0].textContent).toContain('Roles');
        },
        { timeout: 3000 }
      );
    });

    it('calls onChange when option is selected', () => {
      const onChange = jest.fn();
      render(
        <SettingsCombobox name="domain" label="Domain" options={singleOptions} onChange={onChange} />
      );

      fireEvent.focus(screen.getByTestId('combobox-domain'));
      fireEvent.click(screen.getByText('Roles'));

      expect(onChange).toHaveBeenCalledWith('roles');
    });

    it('shows error state', () => {
      render(
        <SettingsCombobox name="domain" label="Domain" options={singleOptions} error="Required" />
      );

      expect(screen.getByText('Required')).toBeInTheDocument();
    });

    it('shows help text', () => {
      render(
        <SettingsCombobox name="domain" label="Domain" options={singleOptions} helpText="Pick a domain" />
      );

      expect(screen.getByText('Pick a domain')).toBeInTheDocument();
    });
  });

  describe('Multiple mode', () => {
    it('renders with multiple mode and no chips when empty', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={[]}
          onMultiChange={jest.fn()}
          options={multiOptions}
        />
      );

      expect(screen.getByTestId('combobox-privs')).toBeInTheDocument();
      expect(screen.queryByTestId('combobox-chips-privs')).not.toBeInTheDocument();
    });

    it('renders chips for selected values', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={['nx-application-users-all', 'nx-script-run']}
          onMultiChange={jest.fn()}
          options={multiOptions}
        />
      );

      const chips = screen.getByTestId('combobox-chips-privs');
      expect(within(chips).getByText('nx-application-users-all')).toBeInTheDocument();
      expect(within(chips).getByText('nx-script-run')).toBeInTheDocument();
    });

    it('shows selected count in label', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={['nx-application-users-all', 'nx-script-run']}
          onMultiChange={jest.fn()}
          options={multiOptions}
        />
      );

      expect(screen.getByText('2 selected')).toBeInTheDocument();
    });

    it('calls onMultiChange to add when clicking an unselected option', () => {
      const onMultiChange = jest.fn();
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={['nx-script-run']}
          onMultiChange={onMultiChange}
          options={multiOptions}
        />
      );

      fireEvent.focus(screen.getByTestId('combobox-privs'));
      fireEvent.click(screen.getByText('nx-application-users-all'));

      expect(onMultiChange).toHaveBeenCalledWith(['nx-script-run', 'nx-application-users-all']);
    });

    it('calls onMultiChange to remove when clicking a chip remove button', () => {
      const onMultiChange = jest.fn();
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={['nx-application-users-all', 'nx-script-run']}
          onMultiChange={onMultiChange}
          options={multiOptions}
        />
      );

      const removeButtons = screen.getAllByRole('button', { name: /Remove/i });
      fireEvent.click(removeButtons[0]);

      expect(onMultiChange).toHaveBeenCalledWith(['nx-script-run']);
    });

    it('shows all options in dropdown with selected items checked', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={['nx-script-run']}
          onMultiChange={jest.fn()}
          options={multiOptions}
        />
      );

      fireEvent.focus(screen.getByTestId('combobox-privs'));

      const listbox = screen.getByRole('listbox');
      // Selected options ARE shown in dropdown (with checkmark)
      expect(within(listbox).getByText('nx-script-run')).toBeInTheDocument();
      // Unselected options are also shown
      expect(within(listbox).getByText('nx-application-users-all')).toBeInTheDocument();
    });

    it('filters dropdown by search text', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={[]}
          onMultiChange={jest.fn()}
          options={multiOptions}
        />
      );

      const input = screen.getByTestId('combobox-privs');
      fireEvent.focus(input);
      fireEvent.change(input, { target: { value: 'maven' } });

      const listbox = screen.getByRole('listbox');
      const options = within(listbox).getAllByRole('option');
      expect(options).toHaveLength(2);
      expect(options[0].textContent).toContain('maven');
      expect(within(listbox).queryByText('nx-script-run')).not.toBeInTheDocument();
    });

    it('removes last chip on Backspace when input is empty', () => {
      const onMultiChange = jest.fn();
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={['nx-application-users-all', 'nx-script-run']}
          onMultiChange={onMultiChange}
          options={multiOptions}
        />
      );

      const input = screen.getByTestId('combobox-privs');
      fireEvent.keyDown(input, { key: 'Backspace' });

      expect(onMultiChange).toHaveBeenCalledWith(['nx-application-users-all']);
    });

    it('renders option descriptions as secondary text in dropdown', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={[]}
          onMultiChange={jest.fn()}
          options={multiOptions}
        />
      );

      fireEvent.focus(screen.getByTestId('combobox-privs'));

      const descs = document.querySelectorAll('.settings-combobox__option-desc');
      expect(descs.length).toBe(5);
      expect(descs[0].textContent).toBe('All permissions for maven repository views');
    });

    it('highlights description text when searching', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={[]}
          onMultiChange={jest.fn()}
          options={multiOptions}
        />
      );

      const input = screen.getByTestId('combobox-privs');
      fireEvent.focus(input);
      fireEvent.change(input, { target: { value: 'maven' } });

      const descMarks = document.querySelectorAll('.settings-combobox__option-desc mark');
      expect(descMarks.length).toBeGreaterThan(0);
      expect(descMarks[0].textContent).toBe('maven');
    });

    it('shows overflow count when chips exceed chipLimit', () => {
      const manyValues = Array.from({ length: 10 }, (_, i) => `priv-${i}`);
      const manyOptions = manyValues.map((v) => ({ value: v, label: v }));

      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={manyValues}
          onMultiChange={jest.fn()}
          options={manyOptions}
          chipLimit={3}
        />
      );

      expect(screen.getByText('+7 more')).toBeInTheDocument();
    });
  });

  describe('Grouped mode', () => {
    it('renders group headers with counts when groupBy is provided', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={[]}
          onMultiChange={jest.fn()}
          options={multiOptions}
          groupBy={groupByFn}
        />
      );

      fireEvent.focus(screen.getByTestId('combobox-privs'));

      expect(screen.getByText(/Repository View \(2\)/)).toBeInTheDocument();
      expect(screen.getByText(/Repository Admin \(1\)/)).toBeInTheDocument();
      expect(screen.getByText(/Application \(1\)/)).toBeInTheDocument();
      expect(screen.getByText(/Other \(1\)/)).toBeInTheDocument();
    });

    it('groups options under correct headers', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={[]}
          onMultiChange={jest.fn()}
          options={multiOptions}
          groupBy={groupByFn}
        />
      );

      fireEvent.focus(screen.getByTestId('combobox-privs'));

      const listbox = screen.getByRole('listbox');
      const items = within(listbox).getAllByRole('option');
      expect(items.length).toBe(5);
    });
  });

  describe('Match highlighting', () => {
    it('renders <mark> around matching text in search results', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={[]}
          onMultiChange={jest.fn()}
          options={multiOptions}
        />
      );

      const input = screen.getByTestId('combobox-privs');
      fireEvent.focus(input);
      fireEvent.change(input, { target: { value: 'maven' } });

      const marks = document.querySelectorAll('mark.settings-combobox__match');
      expect(marks.length).toBeGreaterThan(0);
      expect(marks[0].textContent).toBe('maven');
    });
  });

  describe('Match counter', () => {
    it('shows "X of Y matching" counter in dropdown', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={[]}
          onMultiChange={jest.fn()}
          options={multiOptions}
        />
      );

      fireEvent.focus(screen.getByTestId('combobox-privs'));

      expect(screen.getByRole('status')).toBeInTheDocument();
      // All options shown, none selected
      expect(screen.getByText(/5 of 5 matching/)).toBeInTheDocument();
    });

    it('shows selected count in counter when items are selected', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={['nx-script-run']}
          onMultiChange={jest.fn()}
          options={multiOptions}
        />
      );

      fireEvent.focus(screen.getByTestId('combobox-privs'));

      // All 5 options shown (including the selected one)
      // Counter shows "X of Y matching · Z selected"
      expect(screen.getByText(/5 of 5 matching · 1 selected/)).toBeInTheDocument();
    });
  });

  describe('Empty state', () => {
    it('shows empty state when no options match search', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={[]}
          onMultiChange={jest.fn()}
          options={multiOptions}
        />
      );

      const input = screen.getByTestId('combobox-privs');
      fireEvent.focus(input);
      fireEvent.change(input, { target: { value: 'zzzznonexistent' } });

      expect(screen.getByText(/No results matching/)).toBeInTheDocument();
    });
  });

  describe('Chip truncation with tooltip', () => {
    it('truncates long chip labels with ellipsis', () => {
      const longOption = {
        value: 'nx-repository-view-maven2-central-super-long-name-here',
        label: 'nx-repository-view-maven2-central-super-long-name-here',
      };

      const { container } = render(
        <Theme>
          <SettingsCombobox
            name="privs"
            label="Privileges"
            multiple
            selectedValues={[longOption.value]}
            onMultiChange={jest.fn()}
            options={[...multiOptions, longOption]}
          />
        </Theme>
      );

      const chipLabel = container.querySelector('.settings-combobox__chip-label');
      expect(chipLabel?.textContent).toContain('...');
      expect(chipLabel?.textContent?.length).toBeLessThan(longOption.label.length);
    });

    it('does not truncate short chip labels', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={['nx-script-run']}
          onMultiChange={jest.fn()}
          options={multiOptions}
        />
      );

      const chips = screen.getByTestId('combobox-chips-privs');
      expect(chips.textContent).toContain('nx-script-run');
      expect(chips.textContent).not.toContain('...');
    });
  });

  describe('Group collapse', () => {
    it('collapses group when header is clicked', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={[]}
          onMultiChange={jest.fn()}
          options={multiOptions}
          groupBy={groupByFn}
        />
      );

      fireEvent.focus(screen.getByTestId('combobox-privs'));

      const listbox = screen.getByRole('listbox');
      expect(within(listbox).getByText('nx-repository-view-maven-read')).toBeInTheDocument();

      fireEvent.click(screen.getByText(/Repository View \(2\)/));

      expect(within(listbox).queryByText('nx-repository-view-maven-read')).not.toBeInTheDocument();
    });

    it('expands group when collapsed header is clicked again', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={[]}
          onMultiChange={jest.fn()}
          options={multiOptions}
          groupBy={groupByFn}
        />
      );

      fireEvent.focus(screen.getByTestId('combobox-privs'));

      fireEvent.click(screen.getByText(/Repository View \(2\)/));
      const listbox = screen.getByRole('listbox');
      expect(within(listbox).queryByText('nx-repository-view-maven-read')).not.toBeInTheDocument();

      fireEvent.click(screen.getByText(/Repository View \(2\)/));
      expect(within(listbox).getByText('nx-repository-view-maven-read')).toBeInTheDocument();
    });
  });

  describe('Option descriptions', () => {
    const optionsWithDesc = [
      { value: 'nx-all', label: 'nx-all', description: 'All permissions' },
      { value: 'nx-search-read', label: 'nx-search-read', description: 'Browse and search repositories' },
    ];

    it('renders description text below option label', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={[]}
          onMultiChange={jest.fn()}
          options={optionsWithDesc}
        />
      );

      fireEvent.focus(screen.getByTestId('combobox-privs'));

      const listbox = screen.getByRole('listbox');
      expect(within(listbox).getByText('All permissions')).toBeInTheDocument();
      expect(within(listbox).getByText('Browse and search repositories')).toBeInTheDocument();
    });

    it('filters by description text', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={[]}
          onMultiChange={jest.fn()}
          options={optionsWithDesc}
        />
      );

      const input = screen.getByTestId('combobox-privs');
      fireEvent.focus(input);
      fireEvent.change(input, { target: { value: 'Browse' } });

      const listbox = screen.getByRole('listbox');
      const options = within(listbox).getAllByRole('option');
      expect(options).toHaveLength(1);
      expect(options[0].textContent).toContain('nx-search-read');
    });
  });

  describe('Disabled state', () => {
    it('disables input when disabled prop is true', () => {
      render(
        <SettingsCombobox name="domain" label="Domain" options={singleOptions} disabled />
      );

      expect(screen.getByTestId('combobox-domain')).toBeDisabled();
    });

    it('does not show chip remove buttons when disabled in multi mode', () => {
      render(
        <SettingsCombobox
          name="privs"
          label="Privileges"
          multiple
          selectedValues={['nx-script-run']}
          onMultiChange={jest.fn()}
          options={multiOptions}
          disabled
        />
      );

      expect(screen.queryByRole('button', { name: /Remove/i })).not.toBeInTheDocument();
    });
  });
});
