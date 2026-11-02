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
 * NEXUS-55104: end-to-end test through the REAL form machine and REAL
 * useRepositoryForm hook — only the network boundary (restClient,
 * useRepositoriesApi's create/update) is mocked. Every other RepositoryForm
 * test in this directory mocks useRepositoryForm (RepositoryForm.test.tsx) or
 * mocks useForm itself (useRepositoryForm.test.ts), so a bug in the actual
 * UPDATE-event -> machine assign() -> re-render -> SUBMIT chain would pass
 * every existing test while failing for a real user clicking the real
 * checkbox. This file exists to catch exactly that class of bug.
 */

import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { RepositoryForm } from '../RepositoryForm';
import { ToastProvider } from '../../../../../shared/Toast';
import { Recipe } from '../types';

// Radix Select relies on pointer-capture APIs jsdom doesn't implement; every
// existing test that exercises a Radix-backed dropdown in this codebase either
// mocks the component out or drives state directly rather than clicking
// through the real popover. Stub the two APIs Radix calls during open/select
// so a genuine trigger-click + item-click sequence works here.
beforeAll(() => {
  if (!('hasPointerCapture' in HTMLElement.prototype)) {
    (HTMLElement.prototype as any).hasPointerCapture = () => false;
  }
  if (!('scrollIntoView' in HTMLElement.prototype)) {
    (HTMLElement.prototype as any).scrollIntoView = () => {};
  }
});

const mockCreateRepository = jest.fn().mockResolvedValue(undefined);
const mockUpdateRepository = jest.fn().mockResolvedValue(undefined);

jest.mock('../useRepositoriesApi', () => ({
  useRepositoriesApi: () => ({
    createRepository: mockCreateRepository,
    updateRepository: mockUpdateRepository,
    loading: false,
    error: null,
  }),
}));

// repositoryFormMachine's `load` service hits restClient.get directly (blob
// stores, routing rules, cleanup policies, recipes) rather than going through
// useRepositoriesApi. Every call resolves to [] so the machine reaches
// 'editing' quickly; none of these lists matter for this test.
jest.mock('../../../../../../../interface/api', () => {
  const actual = jest.requireActual('../../../../../../../interface/api');
  return {
    ...actual,
    restClient: {
      get: jest.fn((url: string) => {
        if (url === actual.ENDPOINTS.BLOBSTORES) {
          return Promise.resolve([{ name: 'default' }]);
        }
        return Promise.resolve([]);
      }),
      post: jest.fn(),
      put: jest.fn(),
      delete: jest.fn(),
    },
  };
});

jest.mock('../../../../../../../interface/ExtJS', () => {
  const mockExtJS = {
    checkPermission: jest.fn().mockReturnValue(true),
    usePermission: jest.fn((getValue: () => boolean) => getValue()),
    useUser: jest.fn(() => ({ id: 'admin' })),
    useState: jest.fn((getValue: () => any) => getValue()),
    isEnterpriseEdition: jest.fn().mockReturnValue(false),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockImplementation((key: string) => {
        if (key === 'isCloud') return false;
        if (key === 'previewAuditEnabled') return false;
        return undefined;
      }),
    }),
  };
  return { __esModule: true, default: mockExtJS, ExtJS: mockExtJS };
});

jest.mock('../../../../../../../utils/audit/useAuditLogApi', () => ({
  useAuditLogApi: jest.fn(),
}));

let mockRouterParams: Record<string, any> = {};
jest.mock('@uirouter/react', () => ({
  useCurrentStateAndParams: () => ({ state: null, params: mockRouterParams }),
}));

const renderWithProviders = (component: React.ReactElement) =>
  render(
    <Theme>
      <ToastProvider>{component}</ToastProvider>
    </Theme>
  );

const CARGO_PROXY_RECIPE: Recipe = { format: 'cargo', type: 'proxy', name: 'cargo-proxy' };

const CARGO_CHECKBOX_NAME = /Restrict repository content to authenticated users/i;

async function waitForFormLoaded() {
  await waitFor(() =>
    expect(screen.queryByText(/Loading form data/i)).not.toBeInTheDocument()
  );
}

async function goToSettingsTab() {
  const settingsTab = await screen.findByRole('tab', { name: /Settings/i });
  await userEvent.click(settingsTab);
}

// Radix Select is not a native <select>; userEvent.selectOptions doesn't apply.
// Click the combobox trigger to open the popover, then click the matching
// option by its accessible name.
async function selectBlobStore(name: string) {
  await userEvent.click(screen.getByLabelText(/Blob Store/i));
  await userEvent.click(await screen.findByRole('option', { name }));
}

describe('RepositoryForm + CargoFacet (real machine, NEXUS-55104)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockRouterParams = {};
    mockCreateRepository.mockResolvedValue(undefined);
    mockUpdateRepository.mockResolvedValue(undefined);
  });

  // TODO NEXUS-55881: skipped because it is flaky in CI - it types ~40 characters one keystroke at a time through the real
  // form machine and takes 6-11s against the 10s jest timeout, so it intermittently fails with "Exceeded timeout of
  // 10000 ms" when the agent is under load. Re-enable once the typing is replaced (userEvent.paste or
  // userEvent.setup({ delay: null })). The test below does the same typing and is equally close to the limit.
  it.skip('sends cargo.requireAuthentication=true on create when the checkbox is checked before save', async () => {
    renderWithProviders(
      <RepositoryForm
        repository={null}
        recipe={CARGO_PROXY_RECIPE}
        isCreate
        onSave={jest.fn().mockResolvedValue(undefined)}
        onCancel={jest.fn()}
      />
    );

    await waitForFormLoaded();

    // Required fields so validation lets SUBMIT through to the save service.
    const nameInput = screen.getByLabelText(/^Name/i);
    await userEvent.type(nameInput, 'cargo-proxy-test');

    const remoteUrlInput = screen.getByLabelText(/Remote Storage/i);
    await userEvent.type(remoteUrlInput, 'https://index.crates.io');

    await selectBlobStore('default');

    const checkbox = screen.getByRole('checkbox', { name: CARGO_CHECKBOX_NAME });
    expect(checkbox).not.toBeChecked();

    await userEvent.click(checkbox);
    expect(checkbox).toBeChecked();

    await userEvent.click(screen.getByTestId('form-submit'));

    await waitFor(() => expect(mockCreateRepository).toHaveBeenCalled());

    const savedData = mockCreateRepository.mock.calls[0][0];
    expect(savedData.cargo).toEqual({ requireAuthentication: true });
  });

  it('sends cargo.requireAuthentication=false on create when the checkbox is left unchecked', async () => {
    renderWithProviders(
      <RepositoryForm
        repository={null}
        recipe={CARGO_PROXY_RECIPE}
        isCreate
        onSave={jest.fn().mockResolvedValue(undefined)}
        onCancel={jest.fn()}
      />
    );

    await waitForFormLoaded();

    await userEvent.type(screen.getByLabelText(/^Name/i), 'cargo-proxy-test');
    await userEvent.type(screen.getByLabelText(/Remote Storage/i), 'https://index.crates.io');
    await selectBlobStore('default');

    const checkbox = screen.getByRole('checkbox', { name: CARGO_CHECKBOX_NAME });
    expect(checkbox).not.toBeChecked();

    await userEvent.click(screen.getByTestId('form-submit'));

    await waitFor(() => expect(mockCreateRepository).toHaveBeenCalled());

    const savedData = mockCreateRepository.mock.calls[0][0];
    expect(savedData.cargo).toEqual({ requireAuthentication: false });
  });

  it('sends cargo.requireAuthentication=false on edit when an initially-checked box is unchecked before save', async () => {
    const existingRepo = {
      name: 'cargo-api-test-proxy',
      type: 'proxy',
      format: 'cargo',
      recipe: 'cargo-proxy',
      online: true,
      url: 'http://localhost:8081/repository/cargo-api-test-proxy/',
      status: { online: true },
      attributes: {
        storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        proxy: { remoteUrl: 'https://index.crates.io', contentMaxAge: 1440, metadataMaxAge: 1440 },
        negativeCache: { enabled: true, timeToLive: 1440 },
        httpClient: { blocked: false, autoBlock: true },
        cargo: { requireAuthentication: true },
      },
    } as any;

    renderWithProviders(
      <RepositoryForm
        repository={existingRepo}
        recipe={CARGO_PROXY_RECIPE}
        isCreate={false}
        onSave={jest.fn().mockResolvedValue(undefined)}
        onCancel={jest.fn()}
      />
    );

    await waitForFormLoaded();
    await goToSettingsTab();

    const checkbox = await screen.findByRole('checkbox', { name: CARGO_CHECKBOX_NAME });
    await waitFor(() => expect(checkbox).toBeChecked());

    await userEvent.click(checkbox);
    expect(checkbox).not.toBeChecked();

    await userEvent.click(screen.getByTestId('form-submit'));

    await waitFor(() => expect(mockUpdateRepository).toHaveBeenCalled());

    const [savedName, savedData] = mockUpdateRepository.mock.calls[0];
    expect(savedName).toBe('cargo-api-test-proxy');
    expect(savedData.cargo).toEqual({ requireAuthentication: false });
  });
});
