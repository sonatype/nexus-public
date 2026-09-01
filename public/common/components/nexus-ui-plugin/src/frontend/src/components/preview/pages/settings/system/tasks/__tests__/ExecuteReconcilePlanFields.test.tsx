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
import { render, screen, waitFor, within } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { DynamicFormFields } from '../TaskTypeSelector';
import { restTemplateToTaskType, RestTaskTemplate } from '../taskTransformers';
import { restClient } from '../../../../../../../interface/api';
import { ExtJS } from '../../../../../../../interface/ExtJS';

jest.mock('../../../../../../../interface/api', () => ({ restClient: { get: jest.fn() } }));
jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: { state: jest.fn(() => ({ getValue: jest.fn(() => false) })) },
}));

const mockGet = restClient.get as jest.Mock;
const mockState = ExtJS.state as jest.Mock;
const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

// taskScope intentionally '' (unset) so the default-scope resolution is exercised.
const SELF_HOSTED: Record<string, string> = {
  topAlertBanner: '', planOptionsLabelId: '', planInformationLabelId: '', planInformation: '',
  blobstoreName: '(All Blob Stores)', repositoryName: '', taskScope: '',
  name: 'Repair - Execute Data Repair Plan', reconcileStartDate: '', reconcileEndDate: '',
};
// Cloud: no blobstoreName, no name template.
const CLOUD: Record<string, string> = {
  topAlertBanner: '', planOptionsLabelId: '', planInformationLabelId: '', planInformation: '',
  repositoryName: '', taskScope: '', reconcileStartDate: '', reconcileEndDate: '',
};

const makeTaskType = (properties: Record<string, string>) =>
  restTemplateToTaskType({
    type: 'blobstore.executeReconciliationPlan',
    name: 'Repair - Execute Data Repair Plan',
    enabled: true,
    notificationCondition: 'FAILURE',
    properties,
  } as unknown as RestTaskTemplate);

function Harness({ initial }: { initial: Record<string, string> }) {
  const [values, setValues] = useState<Record<string, string>>(initial);
  return (
    <DynamicFormFields
      taskType={makeTaskType(initial)}
      values={values}
      onChange={(id, v) => setValues((prev) => ({ ...prev, [id]: v }))}
    />
  );
}

// Route the reference-data fetches the form fires: /v1/plan (widget), repositories, blobstores, details.
const routeReferenceData = (plan: unknown = { items: [], continuationToken: null }) =>
  mockGet.mockImplementation((url: string) => {
    if (url.startsWith('/service/rest/v1/plan')) return Promise.resolve(plan);
    if (url.includes('/repositories/details')) return Promise.resolve([{ name: 'repo-1', blobStoreName: 'default' }]);
    if (url.includes('/v1/blobstores')) return Promise.resolve([{ name: 'default' }]);
    if (url.includes('/v1/repositories')) return Promise.resolve([{ name: 'repo-1', format: 'raw', type: 'hosted' }]);
    return Promise.resolve([]);
  });

describe('Execute Data Repair Plan — scope default', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    routeReferenceData();
    mockState.mockReturnValue({ getValue: jest.fn(() => false) }); // self-hosted
  });

  it('defaults scope to dates: start/end date inputs visible when taskScope is unset', async () => {
    renderWithTheme(<Harness initial={SELF_HOSTED} />);
    expect(await screen.findByTestId('input-reconcileStartDate')).toBeInTheDocument();
    expect(screen.getByTestId('input-reconcileEndDate')).toBeInTheDocument();
  });
});

describe('Execute Data Repair Plan — full form (self-hosted)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    routeReferenceData();
    mockState.mockReturnValue({ getValue: jest.fn(() => false) });
  });

  it('renders the top alert banner copy', async () => {
    renderWithTheme(<Harness initial={SELF_HOSTED} />);
    expect(await screen.findByText(/executes recovery plans created using/i)).toBeInTheDocument();
  });

  it('renders both static-info section headers', async () => {
    renderWithTheme(<Harness initial={SELF_HOSTED} />);
    expect(await screen.findByText('Execution Information')).toBeInTheDocument();
    expect(screen.getByText('Plan Information')).toBeInTheDocument();
  });

  it('renders the plan-information widget labels with N/A when no active plans', async () => {
    renderWithTheme(<Harness initial={SELF_HOSTED} />);
    expect(await screen.findByText('Plans')).toBeInTheDocument();
    await waitFor(() => expect(screen.getAllByText('N/A').length).toBe(5));
  });

  it('does not render an editable name input', async () => {
    renderWithTheme(<Harness initial={SELF_HOSTED} />);
    await screen.findByText('Plans');
    expect(screen.queryByTestId('input-name')).not.toBeInTheDocument();
  });

  it('renders the blob store as a read-only Selected list (no Available column / transfer)', async () => {
    renderWithTheme(<Harness initial={{ ...SELF_HOSTED, blobstoreName: 'default' }} />);
    const blobStore = await screen.findByTestId('input-blobstoreName');
    expect(within(blobStore).getByText('default')).toBeInTheDocument();
    expect(screen.queryByText('Available')).not.toBeInTheDocument();
  });

  it('renders taskScope and date inputs read-only (disabled)', async () => {
    renderWithTheme(<Harness initial={SELF_HOSTED} />);
    expect(await screen.findByTestId('input-reconcileStartDate')).toBeDisabled();
    expect(screen.getByTestId('input-reconcileEndDate')).toBeDisabled();
    expect(screen.getByTestId('input-taskScope-dates')).toBeDisabled();
  });

  it('shows no date inputs when scope is explicitly Duration (Execute declares no duration fields)', async () => {
    renderWithTheme(<Harness initial={{ ...SELF_HOSTED, taskScope: 'duration' }} />);
    await screen.findByText('Plans');
    expect(screen.queryByTestId('input-reconcileStartDate')).not.toBeInTheDocument();
    expect(screen.queryByTestId('input-reconcileEndDate')).not.toBeInTheDocument();
    expect(screen.queryByTestId('input-sinceDays')).not.toBeInTheDocument();
  });
});

describe('Execute Data Repair Plan — full form (cloud)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    routeReferenceData();
    mockState.mockReturnValue({ getValue: jest.fn((k: string) => k === 'isCloud') });
  });

  it('omits the blob store field and shows the "Plan Options" header', async () => {
    renderWithTheme(<Harness initial={CLOUD} />);
    expect(await screen.findByText('Plan Options')).toBeInTheDocument();
    expect(screen.queryByTestId('input-blobstoreName')).not.toBeInTheDocument();
    expect(screen.queryByTestId('input-name')).not.toBeInTheDocument();
  });
});
