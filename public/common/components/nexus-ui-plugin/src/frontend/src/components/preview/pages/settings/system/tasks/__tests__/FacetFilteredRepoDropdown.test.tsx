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

/**
 * NEXUS-53741 — end-to-end regression guard for facet-filtered task repository dropdowns in
 * the Preview UI (e.g. "Repository - Delete unused components", `repository.purge-unused`).
 *
 * The full flow under test:
 *   1. restTemplateToTaskType passes the descriptor's `storeFilters` ({facets: ...}) through
 *      verbatim onto the repositoryName form field (NEXUS-53357).
 *   2. DynamicFormFields detects the server-only `facets` filter and fetches the pre-filtered
 *      repository list from /service/rest/internal/ui/repositories?facets=...
 *   3. DynamicFormField renders those options WITHOUT dropping them against the same
 *      server-only `facets` storeFilter (the bug: applyStoreFilters emptied the dropdown).
 */

import React, { useState } from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { DynamicFormFields } from '../TaskTypeSelector';
import { restTemplateToTaskType, RestTaskTemplate } from '../taskTransformers';
import { restClient } from '../../../../../../../interface/api';
import { ExtJS } from '../../../../../../../interface/ExtJS';
import { DYNAMIC_FORM_FIELDS } from '../TaskStrings';

// Render the real shared/form components; mock only the REST client and the ExtJS state bridge.
jest.mock('../../../../../../../interface/api', () => ({
  restClient: { get: jest.fn() },
}));

jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: { state: jest.fn(() => ({ getValue: jest.fn(() => false) })) },
}));

const mockGet = restClient.get as jest.Mock;
const mockState = ExtJS.state as jest.Mock;

const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

const PURGE_UNUSED_FACET = 'org.sonatype.nexus.repository.purge.PurgeUnusedFacet';

// A purge-unused template exactly as the backend now publishes it (NEXUS-53357): formFields with
// per-field metadata, including the server-only `facets` storeFilter on repositoryName.
const makePurgeUnusedTaskType = () =>
  restTemplateToTaskType({
    type: 'repository.purge-unused',
    name: 'Repository - Delete unused components',
    enabled: true,
    notificationCondition: 'FAILURE',
    properties: { repositoryName: '', lastUsed: '1' },
    formFields: [
      {
        id: 'repositoryName',
        type: 'repo',
        label: 'Repository',
        required: true,
        storeApi: 'coreui_Repository.readReferencesAddingEntryForAll',
        storeFilters: { facets: PURGE_UNUSED_FACET },
      },
      { id: 'lastUsed', type: 'number', label: 'Last used', required: true },
    ],
  } as RestTaskTemplate);

/** Controlled wrapper so onChange edits persist. */
function Harness({ taskType }: { taskType: ReturnType<typeof makePurgeUnusedTaskType> }) {
  const [values, setValues] = useState<Record<string, string>>({ repositoryName: '', lastUsed: '1' });
  return (
    <DynamicFormFields
      taskType={taskType}
      values={values}
      onChange={(id, v) => setValues((prev) => ({ ...prev, [id]: v }))}
    />
  );
}

// The unfiltered /v1/repositories list carries every repo (name/format/type). Only the two proxies
// below carry PurgeUnusedFacet, so the internal endpoint returns just those — mirroring the server.
const ALL_REPOSITORIES = [
  { name: 'maven-releases', format: 'maven2', type: 'hosted' },
  { name: 'maven-central', format: 'maven2', type: 'proxy' },
  { name: 'npm-proxy', format: 'npm', type: 'proxy' },
];
const FACET_FILTERED_REPOSITORIES = [
  { id: 'maven-central', name: 'maven-central' },
  { id: 'npm-proxy', name: 'npm-proxy' },
];

// The internal endpoint prepends the "(All Repositories)" entry when withAll=true; this helper
// simulates that so the mock mirrors the real server response the frontend now renders verbatim.
const ALL_ENTRY = {
  id: DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_VALUE,
  name: DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_LABEL,
};
const withAllEntry = (url: string, list: { id: string; name: string }[]) =>
  url.includes('withAll=true') ? [ALL_ENTRY, ...list] : list;

beforeEach(() => {
  jest.clearAllMocks();
  mockState.mockReturnValue({ getValue: jest.fn(() => false) }); // self-hosted
  mockGet.mockImplementation((url: string) => {
    if (url.includes('/internal/ui/repositories')) return Promise.resolve(withAllEntry(url, FACET_FILTERED_REPOSITORIES));
    if (url.includes('/v1/repositories')) return Promise.resolve(ALL_REPOSITORIES);
    if (url.includes('/v1/blobstores')) return Promise.resolve([]);
    return Promise.resolve([]);
  });
});

describe('facet-filtered repository dropdown (NEXUS-53741, repository.purge-unused)', () => {
  it('fetches the pre-filtered repository list from the internal endpoint with the facets param', async () => {
    renderWithTheme(<Harness taskType={makePurgeUnusedTaskType()} />);

    await waitFor(() =>
      expect(mockGet).toHaveBeenCalledWith(
        expect.stringContaining(`/service/rest/internal/ui/repositories?facets=${encodeURIComponent(PURGE_UNUSED_FACET)}`)
      )
    );
  });

  it('populates the dropdown with the server-filtered repositories (not empty)', async () => {
    renderWithTheme(<Harness taskType={makePurgeUnusedTaskType()} />);

    // Wait for the server-filtered fetch to resolve, then open the combobox.
    await waitFor(() =>
      expect(mockGet).toHaveBeenCalledWith(expect.stringContaining('/internal/ui/repositories'))
    );
    fireEvent.focus(screen.getByTestId('combobox-repositoryName'));

    expect(await screen.findByRole('option', { name: 'maven-central' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'npm-proxy' })).toBeInTheDocument();
  });

  it('offers the "(All Repositories)" entry (descriptor includeAnEntryForAllRepositories)', async () => {
    renderWithTheme(<Harness taskType={makePurgeUnusedTaskType()} />);

    await waitFor(() =>
      expect(mockGet).toHaveBeenCalledWith(expect.stringContaining('/internal/ui/repositories'))
    );
    fireEvent.focus(screen.getByTestId('combobox-repositoryName'));

    expect(
      await screen.findByRole('option', { name: DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_LABEL })
    ).toBeInTheDocument();
  });

  it('excludes repositories that do not carry the facet (server already filtered them out)', async () => {
    renderWithTheme(<Harness taskType={makePurgeUnusedTaskType()} />);

    await waitFor(() =>
      expect(mockGet).toHaveBeenCalledWith(expect.stringContaining('/internal/ui/repositories'))
    );
    fireEvent.focus(screen.getByTestId('combobox-repositoryName'));

    // maven-central is present, so the menu is open and populated…
    expect(await screen.findByRole('option', { name: 'maven-central' })).toBeInTheDocument();
    // …but maven-releases (hosted, no PurgeUnusedFacet) was never returned by the endpoint.
    expect(screen.queryByRole('option', { name: 'maven-releases' })).not.toBeInTheDocument();
  });
});

// A facet task that is NOT listed in the static TASK_TYPE_REPO_FILTERS map. The server-side
// filtering must still fire, driven purely by the backend descriptor's `storeFilters` — otherwise
// this dropdown would fall back to the unfiltered repo list (showing non-npm repos), diverging
// from Classic. Guards against the "generic fix affects other task types" concern (NEXUS-53741).
const NPM_SEARCH_FACET = 'com.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchFacet';

const makeReindexNpmTaskType = () =>
  restTemplateToTaskType({
    type: 'repository.npm.reindex',
    name: 'Repair - Reindex npm repository',
    enabled: true,
    notificationCondition: 'FAILURE',
    properties: { repositoryName: '' },
    formFields: [
      {
        id: 'repositoryName',
        type: 'repo',
        label: 'Repository',
        required: true,
        storeApi: 'coreui_Repository.readReferencesAddingEntryForAll',
        storeFilters: { facets: NPM_SEARCH_FACET },
      },
    ],
  } as RestTaskTemplate);

describe('facet task absent from TASK_TYPE_REPO_FILTERS (NEXUS-53741, repository.npm.reindex)', () => {
  const NPM_REPOSITORIES = [
    { id: 'npm-hosted', name: 'npm-hosted' },
    { id: 'npm-proxy', name: 'npm-proxy' },
  ];

  beforeEach(() => {
    mockGet.mockImplementation((url: string) => {
      if (url.includes('/internal/ui/repositories')) return Promise.resolve(withAllEntry(url, NPM_REPOSITORIES));
      if (url.includes('/v1/repositories')) return Promise.resolve(ALL_REPOSITORIES);
      if (url.includes('/v1/blobstores')) return Promise.resolve([]);
      return Promise.resolve([]);
    });
  });

  it('still fetches the server-filtered list from the internal endpoint (facets from storeFilters)', async () => {
    renderWithTheme(<Harness taskType={makeReindexNpmTaskType()} />);

    await waitFor(() =>
      expect(mockGet).toHaveBeenCalledWith(
        expect.stringContaining(`/service/rest/internal/ui/repositories?facets=${encodeURIComponent(NPM_SEARCH_FACET)}`)
      )
    );
  });

  it('shows the npm repositories (not the unfiltered repo list) even without a static-map entry', async () => {
    renderWithTheme(<Harness taskType={makeReindexNpmTaskType()} />);

    await waitFor(() =>
      expect(mockGet).toHaveBeenCalledWith(expect.stringContaining('/internal/ui/repositories'))
    );
    fireEvent.focus(screen.getByTestId('combobox-repositoryName'));

    expect(await screen.findByRole('option', { name: 'npm-hosted' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'npm-proxy' })).toBeInTheDocument();
    // maven-releases is in the unfiltered /v1/repositories list but not the facet-filtered result,
    // so it must NOT leak into the dropdown (the pre-fix behaviour would have shown all repos).
    expect(screen.queryByRole('option', { name: 'maven-releases' })).not.toBeInTheDocument();
  });

  it('offers the "(All Repositories)" entry (storeApi=readReferencesAddingEntryForAll)', async () => {
    renderWithTheme(<Harness taskType={makeReindexNpmTaskType()} />);

    await waitFor(() =>
      expect(mockGet).toHaveBeenCalledWith(expect.stringContaining('/internal/ui/repositories'))
    );
    fireEvent.focus(screen.getByTestId('combobox-repositoryName'));

    expect(
      await screen.findByRole('option', { name: DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_LABEL })
    ).toBeInTheDocument();
  });
});

describe('descriptor-driven repository query per pattern (NEXUS-53741)', () => {
  // Capture every internal-endpoint URL requested so we can assert the derived query params.
  const calledUrls: string[] = [];
  beforeEach(() => {
    calledUrls.length = 0;
    mockGet.mockImplementation((url: string) => {
      if (url.includes('/internal/ui/repositories')) {
        calledUrls.push(url);
        return Promise.resolve([{ id: 'r1', name: 'r1' }, { id: 'r2', name: 'r2' }]);
      }
      if (url.includes('/v1/repositories')) return Promise.resolve(ALL_REPOSITORIES);
      if (url.includes('/v1/blobstores')) return Promise.resolve([]);
      return Promise.resolve([]);
    });
  });

  const makeTask = (typeId: string, storeApi: string, storeFilters?: Record<string, string>) =>
    restTemplateToTaskType({
      type: typeId, name: typeId, enabled: true, notificationCondition: 'FAILURE',
      properties: { repositoryName: '' },
      formFields: [{ id: 'repositoryName', type: 'repo', label: 'Repository', required: true, storeApi, storeFilters }],
    } as RestTaskTemplate);

  const firstQuery = async (): Promise<URLSearchParams> => {
    await waitFor(() => expect(calledUrls.length).toBeGreaterThan(0));
    return new URL(calledUrls[0], 'http://x').searchParams;
  };

  it('format+type (alpine): sends format & type, withAll', async () => {
    renderWithTheme(<Harness taskType={makeTask('repository.alpine.rebuild.metadata',
      'coreui_Repository.readReferencesAddingEntryForAll', { format: 'alpine', type: 'hosted,proxy' })} />);
    const q = await firstQuery();
    expect(q.get('format')).toBe('alpine');
    expect(q.get('type')).toBe('hosted,proxy');
    expect(q.get('withAll')).toBe('true');
  });

  it('facets (docker gc): sends facets, withAll, renders returned repos', async () => {
    renderWithTheme(<Harness taskType={makeTask('repository.docker.gc',
      'coreui_Repository.readReferencesAddingEntryForAll',
      { facets: 'com.sonatype.nexus.repository.docker.DockerGCFacet' })} />);
    const q = await firstQuery();
    expect(q.get('facets')).toBe('com.sonatype.nexus.repository.docker.DockerGCFacet');
    expect(q.get('withAll')).toBe('true');
    fireEvent.focus(screen.getByTestId('combobox-repositoryName'));
    expect(await screen.findByRole('option', { name: 'r1' })).toBeInTheDocument();
  });

  it('type-exclude, no All (plain readReferences): sends type=!group, no withAll', async () => {
    renderWithTheme(<Harness taskType={makeTask('some.repo.task',
      'coreui_Repository.readReferences', { type: '!group' })} />);
    const q = await firstQuery();
    expect(q.get('type')).toBe('!group');
    expect(q.get('withAll')).toBeNull();
  });

  it('no filter (move task): calls the endpoint with no filter params', async () => {
    renderWithTheme(<Harness taskType={makeTask('repository.move', 'coreui_Repository.readReferences', undefined)} />);
    const q = await firstQuery();
    expect(q.get('facets')).toBeNull();
    expect(q.get('format')).toBeNull();
    expect(q.get('withAll')).toBeNull();
  });
});
