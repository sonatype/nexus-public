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

import { SettingsSelectionModal } from '../SettingsSelectionModal';
import { useSettingsSelectionModal } from '../useSettingsSelectionModal';
import type { SettingsSelectionItem } from '../settingsSelectionModalMachine';

const ITEMS: SettingsSelectionItem[] = [
  { id: 'a', name: 'Alpha', description: 'First entry' },
  { id: 'b', name: 'Bravo', description: 'Second entry' },
  { id: 'c', name: 'Charlie', description: 'Third entry' },
];

function TestHarness({
  onConfirm,
  initialSelection = [],
  items = ITEMS,
}: {
  onConfirm: (selection: string[]) => void;
  initialSelection?: string[];
  items?: SettingsSelectionItem[];
}) {
  const picker = useSettingsSelectionModal({ availableItems: items, onConfirm });

  React.useEffect(() => {
    picker.open(initialSelection);
    // Open once on mount; we don't want to re-open on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <Theme>
      <SettingsSelectionModal
        picker={picker}
        title="Privileges Selection"
        emptyMessage="No items match your filter"
      />
    </Theme>
  );
}

describe('SettingsSelectionModal', () => {
  it('renders title, columns, filter, counter, and pagination', () => {
    render(<TestHarness onConfirm={jest.fn()} />);
    expect(screen.getByText('Privileges Selection')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Filter')).toBeInTheDocument();
    expect(screen.getByText(/Use \* as a wildcard\./)).toBeInTheDocument();
    // Three sortable columns
    expect(screen.getByTestId('selection-modal-sort-select')).toBeInTheDocument();
    expect(screen.getByTestId('selection-modal-sort-name')).toBeInTheDocument();
    expect(screen.getByTestId('selection-modal-sort-description')).toBeInTheDocument();
    expect(screen.getByTestId('selection-modal-counter')).toHaveTextContent('0 Selected');
    expect(screen.getByTestId('selection-modal-cancel')).toBeInTheDocument();
    expect(screen.getByTestId('selection-modal-confirm')).toBeInTheDocument();
  });

  it('reflects the initial selection in the counter and checkboxes', () => {
    render(<TestHarness onConfirm={jest.fn()} initialSelection={['a']} />);
    expect(screen.getByTestId('selection-modal-counter')).toHaveTextContent('1 Selected');
    expect(screen.getByTestId('selection-modal-checkbox-a')).toBeChecked();
    expect(screen.getByTestId('selection-modal-checkbox-b')).not.toBeChecked();
  });

  it('toggles an item and updates the counter', () => {
    render(<TestHarness onConfirm={jest.fn()} />);
    const cb = screen.getByTestId('selection-modal-checkbox-b');
    fireEvent.click(cb);
    expect(cb).toBeChecked();
    expect(screen.getByTestId('selection-modal-counter')).toHaveTextContent('1 Selected');
  });

  it('confirm invokes onConfirm with the temp selection', () => {
    const onConfirm = jest.fn();
    render(<TestHarness onConfirm={onConfirm} />);
    fireEvent.click(screen.getByTestId('selection-modal-checkbox-a'));
    fireEvent.click(screen.getByTestId('selection-modal-checkbox-c'));
    fireEvent.click(screen.getByTestId('selection-modal-confirm'));
    expect(onConfirm).toHaveBeenCalledWith(['a', 'c']);
  });

  it('cancel does not invoke onConfirm', () => {
    const onConfirm = jest.fn();
    render(<TestHarness onConfirm={onConfirm} />);
    fireEvent.click(screen.getByTestId('selection-modal-checkbox-a'));
    fireEvent.click(screen.getByTestId('selection-modal-cancel'));
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('filters rows by name and description', () => {
    render(<TestHarness onConfirm={jest.fn()} />);
    fireEvent.change(screen.getByPlaceholderText('Filter'), {
      target: { value: 'Second' },
    });
    expect(screen.queryByTestId('selection-modal-checkbox-a')).not.toBeInTheDocument();
    expect(screen.getByTestId('selection-modal-checkbox-b')).toBeInTheDocument();
    expect(screen.queryByTestId('selection-modal-checkbox-c')).not.toBeInTheDocument();
  });

  it('supports wildcard filter with *', () => {
    render(<TestHarness onConfirm={jest.fn()} />);
    fireEvent.change(screen.getByPlaceholderText('Filter'), {
      target: { value: 'B*o' }, // matches Bravo (name), not Alpha or Charlie
    });
    expect(screen.queryByTestId('selection-modal-checkbox-a')).not.toBeInTheDocument();
    expect(screen.getByTestId('selection-modal-checkbox-b')).toBeInTheDocument();
    expect(screen.queryByTestId('selection-modal-checkbox-c')).not.toBeInTheDocument();
  });

  it('renders an empty-state message when the filter matches nothing', () => {
    render(<TestHarness onConfirm={jest.fn()} />);
    fireEvent.change(screen.getByPlaceholderText('Filter'), {
      target: { value: 'zzz-nomatch' },
    });
    expect(screen.getByTestId('selection-modal-empty')).toHaveTextContent(
      'No items match your filter',
    );
  });

  it('hides the description column when showDescriptionColumn is false', () => {
    const onConfirm = jest.fn();
    function Harness() {
      const picker = useSettingsSelectionModal({ availableItems: ITEMS, onConfirm });
      React.useEffect(() => {
        picker.open([]);
        // eslint-disable-next-line react-hooks/exhaustive-deps
      }, []);
      return (
        <Theme>
          <SettingsSelectionModal
            picker={picker}
            title="Roles Selection"
            showDescriptionColumn={false}
          />
        </Theme>
      );
    }
    render(<Harness />);
    expect(screen.queryByTestId('selection-modal-sort-description')).not.toBeInTheDocument();
  });

  it('cycles sort direction when the Name column header is clicked (asc → desc → asc)', () => {
    render(<TestHarness onConfirm={jest.fn()} />);
    const nameHeader = screen.getByTestId('selection-modal-sort-name');
    // Initial: asc. Click → desc.
    fireEvent.click(nameHeader);
    // Verify a click flipped direction: rely on aria-sort attribute of th
    expect(nameHeader.closest('th')).toHaveAttribute('aria-sort', 'descending');
    fireEvent.click(nameHeader);
    expect(nameHeader.closest('th')).toHaveAttribute('aria-sort', 'ascending');
  });
});
