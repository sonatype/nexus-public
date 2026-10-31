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
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { DynamicFormFields } from '../TaskTypeSelector';
import { restTemplateToTaskType, RestTaskTemplate } from '../taskTransformers';
import { restClient } from '../../../../../../../interface/api';
import { ExtJS } from '../../../../../../../interface/ExtJS';

// Render real shared/form components (SettingsAlert, SettingsTransferList, etc.); mock only the
// REST client and the ExtJS state bridge so we can drive edition + reference data.
jest.mock('../../../../../../../interface/api', () => ({
  restClient: { get: jest.fn() },
}));

jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: { state: jest.fn(() => ({ getValue: jest.fn(() => false) })) },
}));

const mockGet = restClient.get as jest.Mock;
const mockState = ExtJS.state as jest.Mock;

const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

const SELF_HOSTED_PROPERTIES: Record<string, string> = {
  topAlertBanner: '',
  bottomAlertBanner: '',
  onlyNotify: 'true',
  blobstoreName: '(All Blob Stores)',
  repositoryName: '',
  taskScope: 'duration',
  name: 'Repair - Data Repair Plan',
  sinceDays: '',
  sinceHours: '',
  sinceMinutes: '30',
  reconcileStartDate: '',
  reconcileEndDate: '',
};

// Note: there is no Cloud variant of this form. blobstore.planReconciliation is self-hosted only
// (@ConditionalOnEdition(pro, community) — cloud=true was reverted in NEXUS-47948), so the task is
// absent from /v1/tasks/templates in Cloud and never reaches this renderer there.

const makeTaskType = (properties: Record<string, string>) =>
  restTemplateToTaskType({
    type: 'blobstore.planReconciliation',
    name: 'Repair - Data Repair Plan',
    enabled: true,
    notificationCondition: 'FAILURE',
    properties,
  } as RestTaskTemplate);

/** Controlled wrapper so onChange edits persist (needed to assert value preservation). */
function Harness({ taskType, initial }: { taskType: ReturnType<typeof makeTaskType>; initial: Record<string, string> }) {
  const [values, setValues] = useState<Record<string, string>>(initial);
  return (
    <DynamicFormFields
      taskType={taskType}
      values={values}
      onChange={(id, v) => setValues((prev) => ({ ...prev, [id]: v }))}
    />
  );
}

const setReferenceData = ({
  blobstores = ['default', 'other'],
  details = [
    { name: 'repo-default', blobStoreName: 'default' },
    { name: 'repo-other', blobStoreName: 'other' },
  ],
}: {
  blobstores?: string[];
  details?: { name: string; blobStoreName: string }[];
} = {}) => {
  mockGet.mockImplementation((url: string) => {
    if (url.includes('/repositories/details')) return Promise.resolve(details);
    if (url.includes('/v1/blobstores')) return Promise.resolve(blobstores.map((name) => ({ name })));
    if (url.includes('/v1/repositories')) {
      return Promise.resolve(details.map((d) => ({ name: d.name, format: 'raw', type: 'hosted' })));
    }
    return Promise.resolve([]);
  });
};

describe('Data Repair Plan Configure step (DynamicFormFields, blobstore.planReconciliation)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockState.mockReturnValue({ getValue: jest.fn(() => false) }); // self-hosted by default
    setReferenceData();
  });

  describe('banners', () => {
    it('renders the top and bottom alert banners', async () => {
      renderWithTheme(<Harness taskType={makeTaskType(SELF_HOSTED_PROPERTIES)} initial={SELF_HOSTED_PROPERTIES} />);

      expect(await screen.findByText(/This task generates recovery plans/)).toBeInTheDocument();
      expect(
        screen.getByText('Tasks do not run automatically after creation. You must manually run the task after saving.')
      ).toBeInTheDocument();
    });
  });

  describe('onlyNotify checkbox', () => {
    it('renders the custom label/help and reflects the checked default', async () => {
      renderWithTheme(<Harness taskType={makeTaskType(SELF_HOSTED_PROPERTIES)} initial={SELF_HOSTED_PROPERTIES} />);

      expect(await screen.findByText('Keep database records when blob is missing:')).toBeInTheDocument();
      expect(screen.getByText(/do not remove database records for missing blobs/)).toBeInTheDocument();
      expect(screen.getByTestId('input-onlyNotify')).toBeChecked();
    });
  });

  describe('task scope radio group', () => {
    it('renders an accessible radio group with Duration and Start/End Dates', async () => {
      renderWithTheme(<Harness taskType={makeTaskType(SELF_HOSTED_PROPERTIES)} initial={SELF_HOSTED_PROPERTIES} />);

      expect(await screen.findByRole('radiogroup')).toBeInTheDocument();
      expect(screen.getByText('Duration')).toBeInTheDocument();
      expect(screen.getByText('Start/End Dates')).toBeInTheDocument();
    });

    it('shows duration fields and hides date fields when scope=duration (default)', async () => {
      renderWithTheme(<Harness taskType={makeTaskType(SELF_HOSTED_PROPERTIES)} initial={SELF_HOSTED_PROPERTIES} />);

      expect(await screen.findByTestId('input-sinceDays')).toBeInTheDocument();
      expect(screen.getByTestId('input-sinceHours')).toBeInTheDocument();
      expect(screen.getByTestId('input-sinceMinutes')).toBeInTheDocument();
      expect(screen.queryByTestId('input-reconcileStartDate')).not.toBeInTheDocument();
      expect(screen.queryByTestId('input-reconcileEndDate')).not.toBeInTheDocument();
    });

    it('swaps to date fields when Start/End Dates is selected', async () => {
      renderWithTheme(<Harness taskType={makeTaskType(SELF_HOSTED_PROPERTIES)} initial={SELF_HOSTED_PROPERTIES} />);

      fireEvent.click(await screen.findByTestId('input-taskScope-dates'));

      expect(screen.getByTestId('input-reconcileStartDate')).toBeInTheDocument();
      expect(screen.getByTestId('input-reconcileEndDate')).toBeInTheDocument();
      expect(screen.queryByTestId('input-sinceDays')).not.toBeInTheDocument();
    });

    it('preserves user-entered duration values when toggling scope away and back', async () => {
      renderWithTheme(<Harness taskType={makeTaskType(SELF_HOSTED_PROPERTIES)} initial={SELF_HOSTED_PROPERTIES} />);

      fireEvent.change(await screen.findByTestId('input-sinceDays'), { target: { value: '7' } });
      expect(screen.getByTestId('input-sinceDays')).toHaveValue(7);

      fireEvent.click(screen.getByTestId('input-taskScope-dates'));
      expect(screen.queryByTestId('input-sinceDays')).not.toBeInTheDocument();

      fireEvent.click(screen.getByTestId('input-taskScope-duration'));
      // The value re-appears: it was held in form state, not cleared, while hidden.
      expect(screen.getByTestId('input-sinceDays')).toHaveValue(7);
    });
  });

  describe('date field round-trip', () => {
    it('shows the stored m/d/Y value as a YYYY-MM-DD date input', async () => {
      const initial = { ...SELF_HOSTED_PROPERTIES, taskScope: 'dates', reconcileStartDate: '06/24/2026' };
      renderWithTheme(<Harness taskType={makeTaskType(initial)} initial={initial} />);

      expect(await screen.findByTestId('input-reconcileStartDate')).toHaveValue('2026-06-24');
    });

    it('stores a picked date back as m/d/Y', async () => {
      const initial = { ...SELF_HOSTED_PROPERTIES, taskScope: 'dates' };
      renderWithTheme(<Harness taskType={makeTaskType(initial)} initial={initial} />);

      fireEvent.change(await screen.findByTestId('input-reconcileEndDate'), { target: { value: '2026-06-25' } });
      expect(screen.getByTestId('input-reconcileEndDate')).toHaveValue('2026-06-25');
    });
  });

  describe('blob store selector (self-hosted)', () => {
    it('renders the blob store transfer list with the available blob stores', async () => {
      renderWithTheme(<Harness taskType={makeTaskType(SELF_HOSTED_PROPERTIES)} initial={SELF_HOSTED_PROPERTIES} />);

      const blobList = await screen.findByTestId('input-blobstoreName');
      await waitFor(() => {
        expect(within(blobList).getByText('default')).toBeInTheDocument();
        expect(within(blobList).getByText('other')).toBeInTheDocument();
      });
    });

    it('shows "All Blob Stores selected" in the Selected column when nothing is explicitly selected', async () => {
      // The sentinel "(All Blob Stores)" / empty selection must read as all-selected, not as an
      // empty "No items selected" state that looks like nothing will be processed.
      renderWithTheme(<Harness taskType={makeTaskType(SELF_HOSTED_PROPERTIES)} initial={SELF_HOSTED_PROPERTIES} />);

      const blobList = await screen.findByTestId('input-blobstoreName');
      expect(within(blobList).getByText('All Blob Stores selected')).toBeInTheDocument();
      expect(within(blobList).queryByText('No items selected')).not.toBeInTheDocument();
    });

    it('keeps the normal empty state for the repository transfer list (scoped to blob store)', async () => {
      // repositoryName has no "empty = all" copy, so it must still read "No items selected".
      renderWithTheme(<Harness taskType={makeTaskType(SELF_HOSTED_PROPERTIES)} initial={SELF_HOSTED_PROPERTIES} />);

      const repoList = await screen.findByTestId('input-repositoryName');
      expect(within(repoList).getByText('No items selected')).toBeInTheDocument();
      expect(within(repoList).queryByText('All Blob Stores selected')).not.toBeInTheDocument();
    });
  });

  describe('repository selector filtered by selected blob store', () => {
    it('limits Available repositories to those assigned to the selected blob store', async () => {
      const initial = { ...SELF_HOSTED_PROPERTIES, blobstoreName: 'default' };
      renderWithTheme(<Harness taskType={makeTaskType(initial)} initial={initial} />);

      const repoAvailable = await screen.findByTestId('input-repositoryName-available-list');
      await waitFor(() => {
        expect(within(repoAvailable).getByText('repo-default')).toBeInTheDocument();
      });
      // repo-other is assigned to the 'other' blob store, which is not selected.
      expect(within(repoAvailable).queryByText('repo-other')).not.toBeInTheDocument();
    });

    it('shows all repositories when no blob store is selected (sentinel)', async () => {
      renderWithTheme(<Harness taskType={makeTaskType(SELF_HOSTED_PROPERTIES)} initial={SELF_HOSTED_PROPERTIES} />);

      const repoAvailable = await screen.findByTestId('input-repositoryName-available-list');
      await waitFor(() => {
        expect(within(repoAvailable).getByText('repo-default')).toBeInTheDocument();
        expect(within(repoAvailable).getByText('repo-other')).toBeInTheDocument();
      });
    });
  });
});
