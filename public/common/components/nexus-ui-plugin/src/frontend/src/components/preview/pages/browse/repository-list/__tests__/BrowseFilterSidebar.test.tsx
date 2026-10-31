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
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

import { BrowseFilterSidebar } from '../BrowseFilterSidebar';

/*
 * Radix Select relies on pointer-capture and scroll APIs that jsdom does not implement.
 */
beforeAll(() => {
  Element.prototype.hasPointerCapture = jest.fn(() => false);
  Element.prototype.setPointerCapture = jest.fn();
  Element.prototype.releasePointerCapture = jest.fn();
  Element.prototype.scrollIntoView = jest.fn();
});

const formatOptions = [{ value: 'maven2', label: 'Maven', count: 4 }];
const typeOptions = [{ value: 'proxy', label: 'proxy', count: 2 }];
const statusOptions = [{ value: 'online', label: 'Online', count: 4 }];

function renderSidebar(props: Record<string, unknown> = {}) {
  const onSortChange = jest.fn();
  const result = render(
    <Theme>
      <BrowseFilterSidebar
        formatOptions={formatOptions}
        typeOptions={typeOptions}
        statusOptions={statusOptions}
        selectedFormats={[]}
        selectedTypes={[]}
        selectedStatuses={[]}
        selectedProtection={[]}
        selectedHealthCheck={[]}
        sortField="name"
        sortDirection="asc"
        onFormatsChange={jest.fn()}
        onTypesChange={jest.fn()}
        onStatusesChange={jest.fn()}
        onSortChange={onSortChange}
        onResetFilters={jest.fn()}
        {...props}
      />
    </Theme>
  );
  return { ...result, onSortChange };
}

async function openSortFieldSelect() {
  await userEvent.click(screen.getAllByRole('combobox')[0]);
  return screen.getByRole('listbox');
}

describe('BrowseFilterSidebar', () => {
  describe('sort control on the browse repository list', () => {
    it('shouldOfferSortingOnNameTypeAndFormat', async () => {
      renderSidebar({ sortableFields: ['name', 'type', 'format'] });

      const listbox = await openSortFieldSelect();

      ['Name', 'Type', 'Format'].forEach((label) => {
        expect(screen.getByRole('option', { name: label })).toBeInTheDocument();
      });
      expect(listbox).toBeInTheDocument();
    });

    it('shouldNotOfferSortingOnStatus', async () => {
      renderSidebar({ sortableFields: ['name', 'type', 'format'] });

      await openSortFieldSelect();

      expect(screen.queryByRole('option', { name: 'Status' })).not.toBeInTheDocument();
    });

    it('shouldReportSortChangesForRetainedFields', async () => {
      const { onSortChange } = renderSidebar({ sortableFields: ['name', 'type', 'format'] });

      await openSortFieldSelect();
      await userEvent.click(screen.getByRole('option', { name: 'Type' }));

      expect(onSortChange).toHaveBeenCalledWith('type', 'asc');
    });

    it('shouldUseTheSameFormatLabelInTheSortControlAndTheFormatFilter', async () => {
      renderSidebar({ sortableFields: ['name', 'type', 'format'] });

      expect(screen.getByText('Format')).toBeInTheDocument();
      expect(screen.queryByText('Ecosystem')).not.toBeInTheDocument();

      await openSortFieldSelect();

      expect(screen.getByRole('option', { name: 'Format' })).toBeInTheDocument();
    });
  });

  describe('sort control for consumers that do not restrict the sortable fields', () => {
    it('shouldKeepOfferingEveryFieldIncludingStatus', async () => {
      renderSidebar();

      await openSortFieldSelect();

      ['Name', 'Type', 'Format', 'Status'].forEach((label) => {
        expect(screen.getByRole('option', { name: label })).toBeInTheDocument();
      });
    });
  });
});
