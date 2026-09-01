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
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { RepositoriesPage } from '../RepositoriesPage';
import { useRepositoriesApi } from '../useRepositoriesApi';
import { ToastProvider } from '../../../../../shared/Toast';

jest.mock('@uirouter/react', () => ({
  useRouter: () => ({
    stateService: {
      go: jest.fn(),
      href: jest.fn(() => '#preview/admin/iq/hosted-repos-eval'),
    },
  }),
  useCurrentStateAndParams: () => ({ state: null, params: {} }),
}));

jest.mock('../useRepositoriesApi');
const mockUseRepositoriesApi = useRepositoriesApi as jest.MockedFunction<typeof useRepositoriesApi>;

jest.mock('../../../../../../../interface/ExtJS', () => ({
  ExtJS: {
    checkPermission: jest.fn().mockReturnValue(true),
    showSuccessMessage: jest.fn(),
    showErrorMessage: jest.fn(),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn((key: string) => {
        if (key === 'clm') return { enabled: true, hasFirewall: true };
        return undefined;
      }),
    }),
    // RepositoryForm reads permissions through the provider-independent ExtJS.usePermission
    // (NEXUS-54212); delegate to the getter so tests keep driving behavior via checkPermission.
    usePermission: jest.fn((getValue: () => boolean) => getValue()),
    useUser: jest.fn(() => ({ id: 'admin' })),
  },
}));

// mockDirty is a test-only knob that lets each test drive the mocked
// RepositoryForm's dirty state (NEXUS-54349). Undefined → pristine.
let mockDirty: boolean | undefined;

jest.mock('../RepositoryForm', () => ({
  RepositoryForm: ({ onSave, onSubmitRef, onCanAdvanceChange, onDirtyChange }: any) => {
    const { useEffect } = require('react');

    useEffect(() => {
      if (onCanAdvanceChange) onCanAdvanceChange(true);
    }, [onCanAdvanceChange]);

    useEffect(() => {
      if (onDirtyChange) onDirtyChange(!!mockDirty);
    }, [onDirtyChange]);

    useEffect(() => {
      if (onSubmitRef) {
        onSubmitRef.current = () => {
          onSave({ name: 'test-proxy', format: 'maven2', type: 'proxy' });
        };
      }
    }, [onSubmitRef, onSave]);

    return <div data-testid="mock-repository-form">Mock Repository Form</div>;
  },
}));

const mockApiHook = {
  loading: false,
  error: null,
  setError: jest.fn(),
  fetchRepositories: jest.fn().mockResolvedValue([]),
  fetchRepository: jest.fn().mockResolvedValue(null),
  createRepository: jest.fn().mockResolvedValue(undefined),
  updateRepository: jest.fn().mockResolvedValue(undefined),
  deleteRepository: jest.fn().mockResolvedValue(undefined),
  invalidateCache: jest.fn().mockResolvedValue(undefined),
  rebuildIndex: jest.fn().mockResolvedValue('Index rebuild started'),
  fetchRecipes: jest.fn().mockResolvedValue([]),
  fetchBlobStores: jest.fn().mockResolvedValue([{ name: 'default' }]),
  fetchRepositoryReferences: jest.fn().mockResolvedValue([]),
  fetchRoutingRules: jest.fn().mockResolvedValue([]),
  fetchCleanupPolicies: jest.fn().mockResolvedValue([]),
  fetchHealthCheckStatus: jest.fn().mockResolvedValue({}),
  fetchHealthCheckCapabilityEnabled: jest.fn().mockResolvedValue(true),
  enableHealthCheck: jest.fn().mockResolvedValue(undefined),
  disableHealthCheck: jest.fn().mockResolvedValue(undefined),
};

const renderWithTheme = (component: React.ReactElement) => {
  return render(
    <Theme>
      <ToastProvider>{component}</ToastProvider>
    </Theme>
  );
};

describe('RepositoriesPage wizard firewall step', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    window.location.hash = '#preview/admin/repository/repositories/create/maven2/proxy';
    mockUseRepositoriesApi.mockReturnValue(mockApiHook);
  });

  it('preserves firewall choice when navigating back from step 3 and returning', async () => {
    renderWithTheme(<RepositoriesPage />);

    await waitFor(() => {
      expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument();
    });

    const continueButton = screen.getByRole('button', { name: /continue/i });
    await userEvent.click(continueButton);

    await waitFor(() => {
      expect(screen.getByText('Enable Repository Firewall')).toBeInTheDocument();
    });

    const quarantineButton = screen.getByRole('button', { name: 'Quarantine' });
    await userEvent.click(quarantineButton);

    await waitFor(() => {
      expect(screen.getByText('Firewall will be enabled in Quarantine mode')).toBeInTheDocument();
    });

    const backButton = screen.getByTestId('wizard-back');
    await userEvent.click(backButton);

    await waitFor(() => {
      expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument();
    });

    const continueAgain = screen.getByRole('button', { name: /continue/i });
    await userEvent.click(continueAgain);

    await waitFor(() => {
      expect(screen.getByText('Firewall will be enabled in Quarantine mode')).toBeInTheDocument();
    });

    const quarantineBtn = screen.getByRole('button', { name: 'Quarantine' });
    expect(quarantineBtn).toHaveAttribute('aria-pressed', 'true');
  });

  it('preserves audit choice when navigating back from step 3 and returning', async () => {
    renderWithTheme(<RepositoriesPage />);

    await waitFor(() => {
      expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument();
    });

    const continueButton = screen.getByRole('button', { name: /continue/i });
    await userEvent.click(continueButton);

    await waitFor(() => {
      expect(screen.getByText('Enable Repository Firewall')).toBeInTheDocument();
    });

    const auditButton = screen.getByRole('button', { name: 'Audit' });
    await userEvent.click(auditButton);

    const confirmAuditButton = await screen.findByRole('button', { name: /Enable Audit Anyway/i });
    await userEvent.click(confirmAuditButton);

    await waitFor(() => {
      expect(screen.getByText('Firewall will be enabled in Audit mode')).toBeInTheDocument();
    });

    const backButton = screen.getByTestId('wizard-back');
    await userEvent.click(backButton);

    await waitFor(() => {
      expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument();
    });

    const continueAgain = screen.getByRole('button', { name: /continue/i });
    await userEvent.click(continueAgain);

    await waitFor(() => {
      expect(screen.getByText('Firewall will be enabled in Audit mode')).toBeInTheDocument();
    });

    const auditBtn = screen.getByRole('button', { name: 'Audit' });
    expect(auditBtn).toHaveAttribute('aria-pressed', 'true');
  });
});

describe('RepositoriesPage wizard cancel on pristine config step (NEXUS-54349)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockDirty = undefined;
    window.location.hash = '#preview/admin/repository/repositories/create/maven2/hosted';
    mockUseRepositoriesApi.mockReturnValue(mockApiHook);
  });

  it('navigates away without opening the "Unsaved Changes" dialog when Cancel is clicked on a pristine form', async () => {
    mockDirty = false;
    renderWithTheme(<RepositoriesPage />);

    await waitFor(() => {
      expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByTestId('form-cancel'));

    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: /unsaved changes/i })).not.toBeInTheDocument();
  });

  it('opens the "Unsaved Changes" dialog when Cancel is clicked on a dirty form', async () => {
    mockDirty = true;
    renderWithTheme(<RepositoriesPage />);

    await waitFor(() => {
      expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByTestId('form-cancel'));

    expect(await screen.findByRole('alertdialog')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /leave/i })).toBeInTheDocument();
  });
});

describe('RepositoriesPage wizard HC disabled-by-default', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    window.location.hash = '#preview/admin/repository/repositories/create/maven2/proxy';
    mockUseRepositoriesApi.mockReturnValue(mockApiHook);
  });

  it('calls disableHealthCheck after creating a proxy repo when user did not enable HC', async () => {
    renderWithTheme(<RepositoriesPage />);

    // Step 2: form
    await waitFor(() => expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument());
    await userEvent.click(screen.getByRole('button', { name: /continue/i }));

    // Step 3: firewall — advance without choosing firewall
    await waitFor(() => expect(screen.getByText('Enable Repository Firewall')).toBeInTheDocument());
    await userEvent.click(screen.getByRole('button', { name: /continue/i }));

    // Step 4: RHC — click "Create Repository" without enabling HC
    await waitFor(() => expect(screen.getByText('Enable Repository Health Check')).toBeInTheDocument());
    await userEvent.click(screen.getByRole('button', { name: /Create Repository/i }));

    await waitFor(() => {
      expect(mockApiHook.createRepository).toHaveBeenCalled();
      expect(mockApiHook.disableHealthCheck).toHaveBeenCalledWith('test-proxy');
    });
    expect(mockApiHook.enableHealthCheck).not.toHaveBeenCalled();
  });
});
