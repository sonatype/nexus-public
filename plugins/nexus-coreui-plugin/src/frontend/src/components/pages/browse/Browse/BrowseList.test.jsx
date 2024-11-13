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
import {interpret} from 'xstate';
import axios from 'axios';
import {render, screen, waitForElementToBeRemoved, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import TestUtils from '@sonatype/nexus-ui-plugin/src/frontend/src/interface/TestUtils';
import {ascend, descend, pick, prop, sort} from 'ramda';
import {when} from 'jest-when';

import RepositoriesContextProvider from '../../admin/Repositories/RepositoriesContextProvider';
import RepositoriesListMachine from '../../admin/Repositories/RepositoriesListMachine';
import {canReadFirewallStatus, canUpdateHealthCheck} from '../../admin/Repositories/IQServerColumns/IQServerHelpers';
import BrowseList from './BrowseList';
import UIStrings from '../../../../constants/UIStrings';
import {
  FIELDS,
  READ_FIREWALL_STATUS_DATA,
  READ_FIREWALL_STATUS_DATA_WITH_MESSAGE,
  READ_HEALTH_CHECK_DATA,
  REPOS,
  ROW_INDICES
} from './BrowseList.testdata';

jest.mock('axios', () => ({
  post: jest.fn()
}));

jest.mock('../../admin/Repositories/IQServerColumns/IQServerHelpers', () => ({
  isIqServerEnabled: jest.fn().mockReturnValue(true),
  canReadHealthCheck: jest.fn().mockReturnValue(true),
  canUpdateHealthCheck: jest.fn().mockReturnValue(true),
  canReadFirewallStatus: jest.fn().mockReturnValue(true),
  canReadHealthCheckSummary: jest.fn().mockReturnValue(true),
  canReadHealthCheckDetail: jest.fn().mockReturnValue(true)
}));

describe('BrowseList', function() {
  const {COLUMNS} = UIStrings.BROWSE.LIST;
  const {
    ANALYZE_BUTTON,
    LOADING_ERROR,
    NOT_AVAILABLE_TOOLTIP_HC,
    NOT_AVAILABLE_TOOLTIP_FS,
  } = UIStrings.REPOSITORIES.LIST.HEALTH_CHECK;

  const selectors = {
    ...TestUtils.selectors,
    ...TestUtils.tableSelectors,
    getEmptyMessage: () => screen.getByText('There are no repositories available'),
    getFilterInput: () => screen.getByPlaceholderText('Filter by name'),
    healthCheck: {
      columnHeader: () => screen.queryByRole('columnheader', {name: COLUMNS.HEALTH_CHECK}),
      cell: (rowIdx) => screen.getAllByRole('row')[rowIdx].cells[5],
    },
    iqPolicyViolations: {
      columnHeader: () => screen.queryByRole('columnheader', {name: COLUMNS.IQ_POLICY_VIOLATIONS}),
      cell: (rowIdx) => screen.getAllByRole('row')[rowIdx].cells[6],
    },
  };

  async function renderView(data) {
    const service = interpret(RepositoriesListMachine.withConfig({
      services: {
        fetchData: () => Promise.resolve(data)
      }
    })).start();

    render(
      <RepositoriesContextProvider service={service}>
        <BrowseList />
      </RepositoriesContextProvider>
    );
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
  };

  function getStatusText(v) {
    const online = v.online ? 'Online' : 'Offline';
    const desc = v.description ? ` - ${v.description}` : '';
    return online + desc;
  };

  const sortRepos = (field, order = ascend) => sort(order(prop(field)), REPOS);

  const sortReposWithStatus = (order = ascend) => { 
    const dir = order === ascend ? 1 : -1;
    return REPOS.slice().sort((a, b) => JSON.stringify(a) > JSON.stringify(b) ? dir : -dir)
  };

  it('renders the resolved empty text', async function() {
    await renderView({data:[]})

    expect(selectors.getEmptyMessage()).toBeInTheDocument();
  });

  it('renders the error message', async function() {
    const service = interpret(RepositoriesListMachine.withConfig({
      services: {
        fetchData: () => Promise.reject({message: 'Error'})
      }
    })).start();

    render(
      <RepositoriesContextProvider service={service}>
        <BrowseList />
      </RepositoriesContextProvider>
    );
    await waitForElementToBeRemoved(selectors.queryLoadingMask());
    const error = selectors.tableAlert();

    expect(error).toBeInTheDocument();
    expect(error).toHaveTextContent('Error');
  });

  it('renders the resolved data', async function() {
    await renderView({data:REPOS});

    TestUtils.expectTableHeaders(Object.values(COLUMNS));

    const rows = selectors.rows();
    const keys = Object.values(FIELDS);

    expect(rows).toHaveLength(REPOS.length);

    REPOS.forEach((item, idx) => {
      const row = rows[idx];

      keys.forEach(key => expect(item.hasOwnProperty(key)).toBeTruthy());

      Object.values(pick(keys, item)).forEach((value, index)  => {
        index === 3 
        ? expect(row.cells[index]).toHaveTextContent(getStatusText(value))
        : expect(row.cells[index]).toHaveTextContent(value);
      });
    });
  });

  it('renders copy button in each row with tooltips of "Copy URL to Clipboard" when hovering',
    async function() {
      await renderView({data: REPOS});
      const rows = selectors.rows();

      for (const row of rows) {
        const copyBtn = within(row).getAllByRole('button')[0];
        expect(copyBtn).toBeInTheDocument();
        await TestUtils.expectToSeeTooltipOnHover(copyBtn, 'Copy URL to Clipboard');
      }
  });

  it('calls onCopyUrl when copy button is clicked', async function () {
    const onCopyUrl = jest.fn((event) => event.stopPropagation());
    const service = interpret(RepositoriesListMachine.withConfig({
      services: {
        fetchData: () => Promise.resolve({data: REPOS})
      }
    })).start();

    render(
      <RepositoriesContextProvider service={service}>
        <BrowseList copyUrl={onCopyUrl}/>
      </RepositoriesContextProvider>
    );
    await waitForElementToBeRemoved(selectors.queryLoadingMask());

    const copyBtn = within(selectors.rows()[1]).getAllByRole('button')[0];
    userEvent.click(copyBtn);

    expect(onCopyUrl).toBeCalled();
  });

  it('renders a "Filter" text input', async function() {
    await renderView({data:REPOS});

    expect(selectors.getFilterInput()).toBeInTheDocument();
  });

  it('filters by the text value when the user types into the filter', async function() {
    await renderView({data:REPOS});

    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, '', REPOS.length);
    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, 'maven', 4);
    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, 'proxy', 1);
  });

  it('unfilters when the clear button is pressed', async function() {
    await renderView({data:REPOS});

    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, '',  REPOS.length);
    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, 'maven', 4);

    const clearBtn = await screen.findByRole('button', {name: 'Clear filter'});

    userEvent.click(clearBtn);
    expect(selectors.rows()).toHaveLength(7);
  });

  it('unfilters when the ESC key is pressed', async function() {
    await renderView({data:REPOS});

    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, '',  REPOS.length);
    await TestUtils.expectProperFilteredItemsCount(selectors.getFilterInput, 'maven', 4);

    userEvent.type(selectors.getFilterInput(), '{esc}');
    expect(selectors.rows()).toHaveLength(7);
  });

  it('sorts the rows by name', async function() {
    await renderView({data:REPOS});

    const nameHeader = selectors.headerCell(COLUMNS.NAME);

    TestUtils.expectProperRowsOrder(REPOS, FIELDS.NAME);

    userEvent.click(nameHeader);
    let sortedRepos = sortRepos(FIELDS.NAME, descend);
    TestUtils.expectProperRowsOrder(sortedRepos, FIELDS.NAME);

    userEvent.click(nameHeader);
    sortedRepos = sortRepos(FIELDS.NAME);
    TestUtils.expectProperRowsOrder(sortedRepos, FIELDS.NAME);
  });

  it('sorts the rows by type', async function() {
    await renderView({data:REPOS});

    const nameHeader = selectors.headerCell(COLUMNS.TYPE);

    TestUtils.expectProperRowsOrder(REPOS, FIELDS.NAME);

    userEvent.click(nameHeader);
    let sortedRepos = sortRepos(FIELDS.TYPE);
    TestUtils.expectProperRowsOrder(sortedRepos, FIELDS.NAME);

    userEvent.click(nameHeader);
    sortedRepos = sortRepos(FIELDS.TYPE, descend);
    TestUtils.expectProperRowsOrder(sortedRepos, FIELDS.NAME);
  });

  it('sorts the rows by format', async function() {
    await renderView({data:REPOS});

    const nameHeader = selectors.headerCell(COLUMNS.FORMAT);

    TestUtils.expectProperRowsOrder(REPOS, FIELDS.NAME);

    userEvent.click(nameHeader);
    let sortedRepos = sortRepos(FIELDS.FORMAT);
    TestUtils.expectProperRowsOrder(sortedRepos, FIELDS.NAME);

    userEvent.click(nameHeader);
    sortedRepos = sortRepos(FIELDS.FORMAT, descend);
    TestUtils.expectProperRowsOrder(sortedRepos, FIELDS.NAME);
  });

  it('sorts the rows by status', async function() {
    await renderView({data:REPOS});

    const nameHeader = selectors.headerCell(COLUMNS.STATUS);

    TestUtils.expectProperRowsOrder(REPOS, FIELDS.NAME);

    userEvent.click(nameHeader);
    let sortedRepos = sortReposWithStatus();
    TestUtils.expectProperRowsOrder(sortedRepos, FIELDS.NAME);

    userEvent.click(nameHeader);
    sortedRepos = sortReposWithStatus(descend);
    TestUtils.expectProperRowsOrder(sortedRepos, FIELDS.NAME);
  });

  it('renders tooltips of sorting direction when hovering', async function() {
    await renderView({data:REPOS});

    const headerBtn = within(selectors.headerCell(COLUMNS.TYPE)).getByRole('button');

    await TestUtils.expectToSeeTooltipOnHover(headerBtn, 'Type unsorted');

    userEvent.click(headerBtn);
    await TestUtils.expectToSeeTooltipOnHover(headerBtn, 'Type ascending');

    userEvent.click(headerBtn);
    await TestUtils.expectToSeeTooltipOnHover(headerBtn, 'Type descending');
  });

  describe('Health Check Column', function() {
    beforeEach(() => {
      canUpdateHealthCheck.mockReturnValue(true);
      when(axios.post).calledWith(
        'service/extdirect',
        expect.objectContaining({action: 'healthcheck_Status', method: 'read'})
      ).mockResolvedValue({data: TestUtils.makeExtResult(READ_HEALTH_CHECK_DATA)});
    });

    it('does not display health-check column if user has no update permissions', async function() {
      canUpdateHealthCheck.mockReturnValue(false);
      await renderView({data:REPOS});

      expect(axios.post).not.toHaveBeenCalledWith(
        'service/extdirect',
        expect.objectContaining({action: 'healthcheck_Status', method: 'read'})
      );
      expect(selectors.healthCheck.columnHeader()).not.toBeInTheDocument();
    });

    it('renders analyze button when repository supports health check', async function() {
      await renderView({data:REPOS});

      expect(selectors.healthCheck.cell(ROW_INDICES.MAVEN_CENTRAL)).toHaveTextContent(ANALYZE_BUTTON);
      expect(selectors.healthCheck.cell(ROW_INDICES.NUGET_ORG_PROXY)).toHaveTextContent(ANALYZE_BUTTON);
    });

    it('renders an icon with tooltips on hover when repository does not support health check',
      async function() {
        await renderView({data:REPOS});

        const icon = within(selectors.healthCheck.cell(ROW_INDICES.MAVEN_PUBLIC)).getByRole('img', {hidden: true});
        expect(icon).toBeInTheDocument();
        await TestUtils.expectToSeeTooltipOnHover(icon, NOT_AVAILABLE_TOOLTIP_HC);
    });

    it('renders an error message on API call error', async function() {
      when(axios.post).calledWith(
        'service/extdirect',
        expect.objectContaining({action: 'healthcheck_Status', method: 'read'})
      ).mockResolvedValue('error');
      await renderView({data:REPOS});

      const cell = selectors.healthCheck.cell(ROW_INDICES.MAVEN_CENTRAL);
      expect(cell).toHaveTextContent(LOADING_ERROR);
    });
  });

  describe('Firewall Report Column', function() {
    beforeEach(() => {
      canReadFirewallStatus.mockReturnValue(true);
      when(axios.post).calledWith(
          'service/extdirect',
          expect.objectContaining({action: 'firewall_RepositoryStatus', method: 'read'})
      ).mockResolvedValue({data: TestUtils.makeExtResult(READ_FIREWALL_STATUS_DATA)});
    });

    it('does not display Firewall Report column if user has no read permissions',
        async function() {
          canReadFirewallStatus.mockReturnValue(false);
          await renderView({data: REPOS});

          expect(axios.post).not.toHaveBeenCalledWith(
              'service/extdirect',
              expect.objectContaining({action: 'firewall_RepositoryStatus', method: 'read'})
          );
          expect(selectors.iqPolicyViolations.columnHeader()).not.toBeInTheDocument();
        });

    it('renders an icon with tooltips on hover when firewall status is unavailable ',
      async function() {
        await renderView({data:REPOS});

        const icon = within(selectors.iqPolicyViolations.cell(ROW_INDICES.MAVEN_PUBLIC)).getByRole('img', {hidden: true});
        expect(icon).toBeInTheDocument();
        await TestUtils.expectToSeeTooltipOnHover(icon, NOT_AVAILABLE_TOOLTIP_FS);
    });

    it('renders firewall status counters with tooltips on hover', async function() {
      await renderView({data:REPOS});

      const criticalCounter = await within(selectors.iqPolicyViolations.cell(ROW_INDICES.MAVEN_CENTRAL)).findByTitle('Critical');
      const severeCounter = await within(selectors.iqPolicyViolations.cell(ROW_INDICES.MAVEN_CENTRAL)).findByTitle('Severe');
      const moderateCounter = await within(selectors.iqPolicyViolations.cell(ROW_INDICES.MAVEN_CENTRAL)).findByTitle('Moderate');

      expect(criticalCounter).toBeInTheDocument();
      expect(severeCounter).toBeInTheDocument();
      expect(moderateCounter).toBeInTheDocument();
      await TestUtils.expectToSeeTooltipOnHover(criticalCounter, 'Critical');
      await TestUtils.expectToSeeTooltipOnHover(severeCounter, 'Severe');
      await TestUtils.expectToSeeTooltipOnHover(moderateCounter, 'Moderate');
    });

    it('renders an icon and quarantined number with tooltips on hover', async function() {
      await renderView({data:REPOS});

      const icon = await within(selectors.iqPolicyViolations.cell(ROW_INDICES.MAVEN_CENTRAL)).findByTitle('Quarantined');
      expect(icon).toBeInTheDocument();
      expect(icon).toHaveTextContent(READ_FIREWALL_STATUS_DATA[0].quarantinedComponentCount);
      await TestUtils.expectToSeeTooltipOnHover(icon, 'Quarantined');
    });

    it('renders a link', async function() {
      await renderView({data:REPOS});
      const link = within(selectors.iqPolicyViolations.cell(ROW_INDICES.MAVEN_CENTRAL)).getByRole('link');

      expect(link).toBeInTheDocument();
    });

    it('renders an error message on API call error', async function() {
      when(axios.post).calledWith(
        'service/extdirect',
        expect.objectContaining({action: 'firewall_RepositoryStatus', method: 'read'})
      ).mockResolvedValue('error');
      await renderView({data:REPOS});

      const cell = selectors.iqPolicyViolations.cell(ROW_INDICES.MAVEN_CENTRAL);
      expect(cell).toHaveTextContent(LOADING_ERROR);
    });

    it('renders ExtDirect messages', async function() {
      when(axios.post).calledWith(
        'service/extdirect',
        expect.objectContaining({ action: 'firewall_RepositoryStatus', method: 'read' })
      ).mockResolvedValue({data: TestUtils.makeExtResult(READ_FIREWALL_STATUS_DATA_WITH_MESSAGE)});
      await renderView({data:REPOS});

      const cell1 = selectors.iqPolicyViolations.cell(ROW_INDICES.MAVEN_CENTRAL);
      const cell2 = selectors.iqPolicyViolations.cell(ROW_INDICES.NUGET_ORG_PROXY);

      expect(cell1).toHaveTextContent(READ_FIREWALL_STATUS_DATA_WITH_MESSAGE[0].message);
      expect(cell2).toHaveTextContent(READ_FIREWALL_STATUS_DATA_WITH_MESSAGE[1].errorMessage);
    });
  });
});
