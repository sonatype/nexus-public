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
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { RepositoriesPage } from '../RepositoriesPage';
import { useRepositoriesApi } from '../useRepositoriesApi';
import { ToastProvider } from '../../../../../shared/Toast';
import {
  enableFirewallAudit,
  enableFirewallPccs,
  enableFirewallQuarantine,
} from '../../../../../shared/security/useFirewallEnable';

// Keep the real fetch/status helpers the Firewall step depends on; spy only on the three
// enable calls so tests can assert they are not fired for unsupported formats.
jest.mock('../../../../../shared/security/useFirewallEnable', () => ({
  ...jest.requireActual('../../../../../shared/security/useFirewallEnable'),
  enableFirewallAudit: jest.fn().mockResolvedValue(undefined),
  enableFirewallPccs: jest.fn().mockResolvedValue(undefined),
  enableFirewallQuarantine: jest.fn().mockResolvedValue(undefined),
}));

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
    isLicensedEdition: jest.fn().mockReturnValue(false),
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
    useState: jest.fn((getValue: () => unknown) => getValue()),
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
          // The format here is hardcoded to 'maven2' regardless of the URL under test.
          // This is intentional: RepositoriesPage.tsx reads the format for Firewall gating
          // from selectedRecipe (parsed from the URL route), NOT from the submitted form data,
          // so the value passed to onSave does not affect firewall-step visibility decisions.
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

// NEXUS: the Configure step must be the FINAL step for hosted and group repositories,
// so the wizard's submit button reads "Create Repository" rather than "Continue" and the
// progress indicator does not promise Firewall/RHC steps that never arrive. Those two
// steps are proxy-only.
describe('RepositoriesPage wizard final step label', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUseRepositoriesApi.mockReturnValue(mockApiHook);
  });

  it.each([
    ['hosted', 'terraform'],
    ['group', 'terraform'],
    ['hosted', 'maven2'],
    ['group', 'maven2'],
  ])('shows "Create Repository" on Configure for a %s %s repository', async (type, format) => {
    window.location.hash = `#preview/admin/repository/repositories/create/${format}/${type}`;
    renderWithTheme(<RepositoriesPage />);

    await waitFor(() => expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument());

    expect(screen.getByRole('button', { name: /Create Repository/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /^Continue$/i })).not.toBeInTheDocument();
  });

  it.each([
    ['maven2', 'a firewall-supported format'],
    ['terraform', 'a firewall-unsupported format'],
  ])('still shows "Continue" on Configure for a proxy on %s (%s)', async (format) => {
    window.location.hash = `#preview/admin/repository/repositories/create/${format}/proxy`;
    renderWithTheme(<RepositoriesPage />);

    await waitFor(() => expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument());

    expect(screen.getByRole('button', { name: /^Continue$/i })).toBeInTheDocument();
  });

  // The progress indicator renders steps.length - 1 dividers, so the divider count is a
  // direct proxy for how many steps the wizard is advertising. Hosted/group must advertise
  // 3 (Format/Type/Configure) — not 5 — so the bar does not promise steps that never arrive.
  it.each([
    ['hosted', 2],
    ['group', 2],
  ])('advertises only 3 steps for a %s repository', async (type, expectedDividers) => {
    window.location.hash = `#preview/admin/repository/repositories/create/terraform/${type}`;
    const { container } = renderWithTheme(<RepositoriesPage />);

    await waitFor(() => expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument());

    expect(container.querySelectorAll('.wizard-form__divider')).toHaveLength(expectedDividers);
  });

  it('advertises 5 steps for a proxy on a firewall-supported format', async () => {
    window.location.hash = '#preview/admin/repository/repositories/create/maven2/proxy';
    const { container } = renderWithTheme(<RepositoriesPage />);

    await waitFor(() => expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument());

    expect(container.querySelectorAll('.wizard-form__divider')).toHaveLength(4);
  });

  it('advertises 4 steps for a proxy on a firewall-unsupported format', async () => {
    window.location.hash = '#preview/admin/repository/repositories/create/terraform/proxy';
    const { container } = renderWithTheme(<RepositoriesPage />);

    await waitFor(() => expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument());

    expect(container.querySelectorAll('.wizard-form__divider')).toHaveLength(3);
  });

  it('creates the repository directly from Configure for a hosted repository', async () => {
    window.location.hash = '#preview/admin/repository/repositories/create/terraform/hosted';
    renderWithTheme(<RepositoriesPage />);

    await waitFor(() => expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument());
    await userEvent.click(screen.getByRole('button', { name: /Create Repository/i }));

    await waitFor(() => expect(mockApiHook.createRepository).toHaveBeenCalled());
    // No intermediate Firewall/RHC panel for hosted.
    expect(screen.queryByText('Enable Repository Firewall')).not.toBeInTheDocument();
    expect(screen.queryByText('Enable Repository Health Check')).not.toBeInTheDocument();
  });
});

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

  // NEXUS-54536: the wizard must not offer "Enable Repository Firewall" for a proxy format
  // Firewall does not support (apt, helm, terraform, gitlfs, alpine, ansiblegalaxy, oci, pub).
  it('skips the firewall step and goes straight to Enable RHC for a format Firewall does not support (apt)', async () => {
    window.location.hash = '#preview/admin/repository/repositories/create/apt/proxy';
    renderWithTheme(<RepositoriesPage />);

    await waitFor(() => {
      expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument();
    });

    const continueButton = screen.getByRole('button', { name: /continue/i });
    await userEvent.click(continueButton);

    await waitFor(() => {
      expect(screen.getByText('Enable Repository Health Check')).toBeInTheDocument();
    });

    expect(screen.queryByText('Enable Repository Firewall')).not.toBeInTheDocument();
  });

  // NEXUS-54536 Fix 2: clicking Back from the RHC step on an unsupported-format proxy must
  // NOT re-introduce the Firewall step — it should return to Configure (mock form visible).
  it('does not re-show the firewall step when navigating Back from RHC for an unsupported format (apt)', async () => {
    window.location.hash = '#preview/admin/repository/repositories/create/apt/proxy';
    renderWithTheme(<RepositoriesPage />);

    await waitFor(() => {
      expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument();
    });

    // Advance to the RHC step (firewall is skipped for apt)
    const continueButton = screen.getByRole('button', { name: /continue/i });
    await userEvent.click(continueButton);

    await waitFor(() => {
      expect(screen.getByText('Enable Repository Health Check')).toBeInTheDocument();
    });
    expect(screen.queryByText('Enable Repository Firewall')).not.toBeInTheDocument();

    // Click Back from the RHC step
    const backButton = screen.getByTestId('wizard-back');
    await userEvent.click(backButton);

    // Should return to Configure (mock form) — Firewall must NOT appear
    await waitFor(() => {
      expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument();
    });
    expect(screen.queryByText('Enable Repository Firewall')).not.toBeInTheDocument();
  });

  // Guards against an implementer special-casing 'apt' or 'maven2' instead of using the real
  // firewall-supported-format predicate: npm is supported and is neither of those two formats.
  it('still shows the firewall step for another format Firewall supports (npm)', async () => {
    window.location.hash = '#preview/admin/repository/repositories/create/npm/proxy';
    renderWithTheme(<RepositoriesPage />);

    await waitFor(() => {
      expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument();
    });

    const continueButton = screen.getByRole('button', { name: /continue/i });
    await userEvent.click(continueButton);

    await waitFor(() => {
      expect(screen.getByText('Enable Repository Firewall')).toBeInTheDocument();
    });

    // Advance past the firewall step (without choosing a level) to verify the forward
    // flow continues correctly to the RHC step.
    await userEvent.click(screen.getByRole('button', { name: /continue/i }));

    await waitFor(() => {
      expect(screen.getByText('Enable Repository Health Check')).toBeInTheDocument();
    });
  });

  // NEXUS-54536: a Firewall level chosen for a supported format must not leak onto a
  // format Firewall does not support. Reachable path: choose Quarantine for an npm proxy,
  // go Back out of the Firewall step, then select apt/proxy — the wizard skips the Firewall
  // step for apt, so the stale choice is never displayed nor overwritten, and the create
  // handler would still call enableFirewallQuarantine on an apt repository.
  it('does not enable Firewall for an unsupported format after a level was chosen for a supported one', async () => {
    window.location.hash = '#preview/admin/repository/repositories/create/npm/proxy';
    renderWithTheme(<RepositoriesPage />);

    await waitFor(() => expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument());
    await userEvent.click(screen.getByRole('button', { name: /continue/i }));

    await waitFor(() => expect(screen.getByText('Enable Repository Firewall')).toBeInTheDocument());
    await userEvent.click(screen.getByRole('button', { name: 'Quarantine' }));
    await waitFor(() =>
      expect(screen.getByText('Firewall will be enabled in Quarantine mode')).toBeInTheDocument()
    );

    // Back out of the Firewall step, then land on apt/proxy. Going Back to the format/type
    // selector and re-picking there produces the same route change, which is what the
    // hashchange handler sees.
    await userEvent.click(screen.getByTestId('wizard-back'));
    await waitFor(() => expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument());

    await act(async () => {
      window.location.hash = '#preview/admin/repository/repositories/create/apt/proxy';
      window.dispatchEvent(new Event('hashchange'));
    });

    // apt skips the Firewall step: Configure → Enable RHC.
    await waitFor(() => expect(screen.getByTestId('mock-repository-form')).toBeInTheDocument());
    await userEvent.click(screen.getByRole('button', { name: /continue/i }));

    await waitFor(() =>
      expect(screen.getByText('Enable Repository Health Check')).toBeInTheDocument()
    );
    expect(screen.queryByText('Enable Repository Firewall')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /Create Repository/i }));

    await waitFor(() => expect(mockApiHook.createRepository).toHaveBeenCalled());
    expect(enableFirewallQuarantine).not.toHaveBeenCalled();
    expect(enableFirewallAudit).not.toHaveBeenCalled();
    expect(enableFirewallPccs).not.toHaveBeenCalled();
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
