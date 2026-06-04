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
import { render, screen, fireEvent, within } from '@testing-library/react';
import '@testing-library/jest-dom';
import { SettingsTransferList } from '../SettingsTransferList';

// @radix-ui/themes is in transformIgnorePatterns — no mock needed
// Mock lucide-react
jest.mock('lucide-react', () => ({
  Search: () => <span data-testid="search-icon">🔍</span>,
  ChevronRight: () => <span>→</span>,
  ChevronLeft: () => <span>←</span>,
  ChevronsRight: () => <span>⇒</span>,
  ChevronsLeft: () => <span>⇐</span>,
}));

describe('SettingsTransferList', () => {
  const allItems = [
    { id: '1', name: 'Item One' },
    { id: '2', name: 'Item Two' },
    { id: '3', name: 'Item Three' },
    { id: '4', name: 'Item Four' },
  ];

  const defaultProps = {
    name: 'test-transfer',
    availableItems: allItems,
    selectedItems: [],
    onChange: jest.fn(),
    getItemId: (item) => item.id,
    getItemLabel: (item) => item.name,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders with available items', () => {
    render(<SettingsTransferList {...defaultProps} />);
    
    expect(screen.getByText('Item One')).toBeInTheDocument();
    expect(screen.getByText('Item Two')).toBeInTheDocument();
    expect(screen.getByText('Item Three')).toBeInTheDocument();
    expect(screen.getByText('Item Four')).toBeInTheDocument();
  });

  it('renders label when provided', () => {
    render(<SettingsTransferList {...defaultProps} label="Assign Roles" />);
    
    expect(screen.getByText('Assign Roles')).toBeInTheDocument();
  });

  it('renders custom available and selected labels', () => {
    render(
      <SettingsTransferList 
        {...defaultProps} 
        availableLabel="All Roles"
        selectedLabel="Granted Roles"
      />
    );
    
    expect(screen.getByText('All Roles')).toBeInTheDocument();
    expect(screen.getByText('Granted Roles')).toBeInTheDocument();
  });

  it('shows selected items in the right panel', () => {
    const selectedItems = [allItems[0], allItems[2]];
    render(
      <SettingsTransferList 
        {...defaultProps} 
        selectedItems={selectedItems}
      />
    );
    
    // Get the two listboxes (available and selected)
    const listboxes = screen.getAllByRole('listbox');
    const selectedPanel = listboxes[1];
    
    expect(within(selectedPanel).getByText('Item One')).toBeInTheDocument();
    expect(within(selectedPanel).getByText('Item Three')).toBeInTheDocument();
  });

  it('excludes selected items from available panel', () => {
    const selectedItems = [allItems[0]];
    render(
      <SettingsTransferList 
        {...defaultProps} 
        selectedItems={selectedItems}
      />
    );
    
    const listboxes = screen.getAllByRole('listbox');
    const availablePanel = listboxes[0];
    
    expect(within(availablePanel).queryByText('Item One')).not.toBeInTheDocument();
    expect(within(availablePanel).getByText('Item Two')).toBeInTheDocument();
  });

  it('filters available items by search', () => {
    render(<SettingsTransferList {...defaultProps} />);
    
    const searchInputs = screen.getAllByPlaceholderText('Filter...');
    fireEvent.change(searchInputs[0], { target: { value: 'Two' } });
    
    const listboxes = screen.getAllByRole('listbox');
    const availablePanel = listboxes[0];
    
    expect(within(availablePanel).getByText('Item Two')).toBeInTheDocument();
    expect(within(availablePanel).queryByText('Item One')).not.toBeInTheDocument();
  });

  it('filters selected items by search', () => {
    const selectedItems = [allItems[0], allItems[1]];
    render(
      <SettingsTransferList 
        {...defaultProps} 
        selectedItems={selectedItems}
      />
    );
    
    const searchInputs = screen.getAllByPlaceholderText('Filter...');
    fireEvent.change(searchInputs[1], { target: { value: 'One' } });
    
    const listboxes = screen.getAllByRole('listbox');
    const selectedPanel = listboxes[1];
    
    expect(within(selectedPanel).getByText('Item One')).toBeInTheDocument();
    expect(within(selectedPanel).queryByText('Item Two')).not.toBeInTheDocument();
  });

  it('moves item to selected on double-click', () => {
    const onChange = jest.fn();
    render(<SettingsTransferList {...defaultProps} onChange={onChange} />);
    
    fireEvent.doubleClick(screen.getByText('Item One'));
    
    expect(onChange).toHaveBeenCalledWith([allItems[0]]);
  });

  it('removes item from selected on double-click', () => {
    const onChange = jest.fn();
    const selectedItems = [allItems[0], allItems[1]];
    render(
      <SettingsTransferList 
        {...defaultProps} 
        selectedItems={selectedItems}
        onChange={onChange}
      />
    );
    
    const listboxes = screen.getAllByRole('listbox');
    const selectedPanel = listboxes[1];
    
    fireEvent.doubleClick(within(selectedPanel).getByText('Item One'));
    
    expect(onChange).toHaveBeenCalledWith([allItems[1]]);
  });

  it('selects item on click', () => {
    render(<SettingsTransferList {...defaultProps} />);
    
    const item = screen.getByText('Item One');
    fireEvent.click(item);
    
    expect(item.closest('[role="option"]')).toHaveClass('settings-transfer-list__item--selected');
  });

  it('moves selected items to right with arrow button', () => {
    const onChange = jest.fn();
    render(<SettingsTransferList {...defaultProps} onChange={onChange} />);
    
    // Select an item
    fireEvent.click(screen.getByText('Item One'));
    
    // Click move right button
    fireEvent.click(screen.getByRole('button', { name: 'Move selected to right' }));
    
    expect(onChange).toHaveBeenCalledWith([allItems[0]]);
  });

  it('moves all items to selected with double arrow button', () => {
    const onChange = jest.fn();
    render(<SettingsTransferList {...defaultProps} onChange={onChange} />);
    
    fireEvent.click(screen.getByRole('button', { name: 'Move all to selected' }));
    
    expect(onChange).toHaveBeenCalledWith(allItems);
  });

  it('removes selected items with left arrow button', () => {
    const onChange = jest.fn();
    const selectedItems = [allItems[0], allItems[1]];
    render(
      <SettingsTransferList 
        {...defaultProps} 
        selectedItems={selectedItems}
        onChange={onChange}
      />
    );
    
    // Click on item in selected panel
    const listboxes = screen.getAllByRole('listbox');
    const selectedPanel = listboxes[1];
    fireEvent.click(within(selectedPanel).getByText('Item One'));
    
    // Click move left button
    fireEvent.click(screen.getByRole('button', { name: 'Move selected to left' }));
    
    expect(onChange).toHaveBeenCalledWith([allItems[1]]);
  });

  it('removes all selected items with double left arrow', () => {
    const onChange = jest.fn();
    const selectedItems = [allItems[0], allItems[1]];
    render(
      <SettingsTransferList 
        {...defaultProps} 
        selectedItems={selectedItems}
        onChange={onChange}
      />
    );
    
    fireEvent.click(screen.getByRole('button', { name: 'Move all to available' }));
    
    expect(onChange).toHaveBeenCalledWith([]);
  });

  it('shows empty message when no available items', () => {
    render(
      <SettingsTransferList 
        {...defaultProps} 
        availableItems={[]}
      />
    );
    
    expect(screen.getByText('No items available')).toBeInTheDocument();
  });

  it('shows empty message when no selected items', () => {
    render(<SettingsTransferList {...defaultProps} />);
    
    expect(screen.getByText('No items selected')).toBeInTheDocument();
  });

  it('shows no matches message when search has no results', () => {
    render(<SettingsTransferList {...defaultProps} />);
    
    const searchInputs = screen.getAllByPlaceholderText('Filter...');
    fireEvent.change(searchInputs[0], { target: { value: 'xyz' } });
    
    expect(screen.getByText('No matches')).toBeInTheDocument();
  });

  it('displays item counts', () => {
    const selectedItems = [allItems[0]];
    render(
      <SettingsTransferList 
        {...defaultProps} 
        selectedItems={selectedItems}
      />
    );
    
    expect(screen.getByText('3 items')).toBeInTheDocument(); // Available (4-1)
    expect(screen.getByText('1 item')).toBeInTheDocument(); // Selected (singular)
  });

  it('disables component when disabled is true', () => {
    const { container } = render(<SettingsTransferList {...defaultProps} disabled />);
    
    expect(container.firstChild).toHaveClass('settings-transfer-list--disabled');
  });

  it('renders help text when provided', () => {
    render(<SettingsTransferList {...defaultProps} helpText="Use Ctrl+Click for multi-select" />);
    
    expect(screen.getByText('Use Ctrl+Click for multi-select')).toBeInTheDocument();
  });

  it('applies custom className', () => {
    const { container } = render(<SettingsTransferList {...defaultProps} className="custom" />);
    
    expect(container.firstChild).toHaveClass('custom');
  });

  it('supports multi-select with Ctrl+Click', () => {
    render(<SettingsTransferList {...defaultProps} />);
    
    fireEvent.click(screen.getByText('Item One'));
    fireEvent.click(screen.getByText('Item Two'), { ctrlKey: true });
    
    expect(screen.getByText('Item One').closest('[role="option"]')).toHaveClass('settings-transfer-list__item--selected');
    expect(screen.getByText('Item Two').closest('[role="option"]')).toHaveClass('settings-transfer-list__item--selected');
  });

  describe('testId prop', () => {
    it('sets data-testid on root when testId provided', () => {
      render(<SettingsTransferList {...defaultProps} testId="user-roles" />);
      // Root data-testid is on the Radix Box; verify by checking child elements use the testId prefix
      expect(screen.getByTestId('user-roles-available-search')).toBeInTheDocument();
      expect(screen.getByTestId('user-roles-selected-search')).toBeInTheDocument();
    });

    it('sets data-testid on available search input', () => {
      render(<SettingsTransferList {...defaultProps} testId="user-roles" />);
      expect(screen.getByTestId('user-roles-available-search')).toBeInTheDocument();
    });

    it('sets data-testid on selected search input', () => {
      render(<SettingsTransferList {...defaultProps} testId="user-roles" />);
      expect(screen.getByTestId('user-roles-selected-search')).toBeInTheDocument();
    });

    it('sets data-testid on available list container', () => {
      render(<SettingsTransferList {...defaultProps} testId="user-roles" />);
      expect(screen.getByTestId('user-roles-available-list')).toBeInTheDocument();
    });

    it('sets data-testid on selected list container', () => {
      render(<SettingsTransferList {...defaultProps} testId="user-roles" />);
      expect(screen.getByTestId('user-roles-selected-list')).toBeInTheDocument();
    });

    it('sets data-testid on each available item with sanitized id', () => {
      render(<SettingsTransferList {...defaultProps} testId="user-roles" />);
      expect(screen.getByTestId('user-roles-available-item-1')).toBeInTheDocument();
      expect(screen.getByTestId('user-roles-available-item-2')).toBeInTheDocument();
    });

    it('sets data-testid on selected items with sanitized id', () => {
      render(
        <SettingsTransferList
          {...defaultProps}
          testId="user-roles"
          selectedItems={[allItems[0]]}
        />
      );
      expect(screen.getByTestId('user-roles-selected-item-1')).toBeInTheDocument();
    });

    it('sanitizes special characters in item ids for testid', () => {
      const specialItems = [{ id: 'nx:all/*', name: 'All Privileges' }];
      render(
        <SettingsTransferList
          {...defaultProps}
          availableItems={specialItems}
          testId="privs"
          getItemId={(item) => item.id}
        />
      );
      expect(screen.getByTestId('privs-available-item-nx-all--')).toBeInTheDocument();
    });

    it('does not set testId attributes when testId not provided', () => {
      const { container } = render(<SettingsTransferList {...defaultProps} />);
      expect(container.firstChild).not.toHaveAttribute('data-testid');
    });
  });
});

