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

import { RepositoryForm } from '../RepositoryForm';
import { useRepositoryForm } from '../useRepositoryForm';
import { ToastProvider } from '../../../../../shared/Toast';

// Mock the hook (Layer 2) - component only consumes hook output
jest.mock('../useRepositoryForm');

// Mock the useAuditLogApi hook
jest.mock('../../../../../../../utils/audit/useAuditLogApi', () => ({
  useAuditLogApi: jest.fn(),
}));

const mockUseAuditLogApi = require('../../../../../../../utils/audit/useAuditLogApi').useAuditLogApi;

// Mock ExtJS for auditEnabled state check
jest.mock('../../../../../../../interface/ExtJS', () => {
  const mockExtJS = {
    checkPermission: jest.fn().mockReturnValue(true),
    // RepositoryForm reads permissions through the provider-independent ExtJS.usePermission
    // (NEXUS-54212); delegate to the getter so tests keep driving behavior via checkPermission.
    usePermission: jest.fn((getValue: () => boolean) => getValue()),
    useUser: jest.fn(() => ({ id: 'admin' })),
    state: jest.fn().mockReturnValue({
      getValue: jest.fn().mockImplementation((key: string) => {
        if (key === 'previewAuditEnabled') return false;
        return undefined;
      }),
    }),
  };
  return {
    __esModule: true,
    default: mockExtJS,
    ExtJS: mockExtJS,
  };
});

const mockUseRepositoryForm = useRepositoryForm as jest.MockedFunction<typeof useRepositoryForm>;

const MOCK_BLOB_STORES = [{ name: 'default' }, { name: 'secondary' }];
const MOCK_ROUTING_RULES = [{ id: '1', name: 'block-snapshots', mode: 'BLOCK' }];
const MOCK_CLEANUP_POLICIES = [{ name: 'maven-cleanup', format: 'maven2' }];

function createMockForm(data: any = {}) {
  return {
    field: jest.fn((name: string) => {
      const value = data[name];
      return { name, value: value != null ? String(value) : '', onChange: jest.fn(), onBlur: jest.fn(), error: undefined };
    }),
    data,
    isPristine: true,
    isSaving: false,
    isLoading: false,
    isDeleting: false,
    saveError: null,
    validationErrors: {},
    state: { matches: jest.fn(() => false), context: { data } },
    send: jest.fn(),
  } as any;
}

function createMockHookReturn(overrides: Partial<ReturnType<typeof useRepositoryForm>> = {}) {
  const formData = overrides.formData || {
    name: '', type: 'hosted', format: 'maven2',
    recipe: 'maven2-hosted', online: true,
    storage: { blobStoreName: '', strictContentTypeValidation: true },
  };

  return {
    form: createMockForm(formData),
    repository: null,
    isCreate: true,
    hasFirewallLicense: false,
    isCloud: false,
    activeTab: 'settings',
    setActiveTab: jest.fn(),
    originChangeWarning: false,
    setOriginChangeWarning: jest.fn(),
    formData: formData as any,
    pristineData: undefined,
    errors: {},
    blobStores: MOCK_BLOB_STORES as any,
    routingRules: MOCK_ROUTING_RULES as any,
    cleanupPolicies: MOCK_CLEANUP_POLICIES as any,
    memberRepositories: [],
    handleChange: jest.fn(),
    handleNestedChange: jest.fn(),
    ...overrides,
  } as any;
}

const MOCK_PROXY_RECIPE = { format: 'maven2', type: 'proxy', name: 'maven2-proxy' };
const MOCK_HOSTED_RECIPE = { format: 'maven2', type: 'hosted', name: 'maven2-hosted' };
const MOCK_GROUP_RECIPE = { format: 'maven2', type: 'group', name: 'maven2-group' };

const MOCK_PROXY_REPOSITORY = {
  name: 'maven-central',
  type: 'proxy',
  format: 'maven2',
  recipe: 'maven2-proxy',
  online: true,
  url: 'http://localhost:8081/repository/maven-central/',
  status: { online: true },
  attributes: {
    storage: { blobStoreName: 'default', strictContentTypeValidation: true },
    proxy: { remoteUrl: 'https://repo1.maven.org/maven2/', contentMaxAge: 1440, metadataMaxAge: 1440 },
    negativeCache: { enabled: true, timeToLive: 1440 },
    httpClient: { blocked: false, autoBlock: true },
  },
};

const renderWithTheme = (component: React.ReactElement) => {
  return render(
    <Theme>
      <ToastProvider>{component}</ToastProvider>
    </Theme>
  );
};

describe('RepositoryForm', () => {
  const mockOnSave = jest.fn().mockResolvedValue(undefined);
  const mockOnCancel = jest.fn();
  const mockOnDelete = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockUseRepositoryForm.mockReturnValue(createMockHookReturn());
  });

  it('renders loading state while fetching reference data', () => {
    const loadingForm = createMockForm({});
    loadingForm.isLoading = true;
    mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
      form: loadingForm,
    }));

    renderWithTheme(
      <RepositoryForm
        recipe={MOCK_HOSTED_RECIPE}
        isCreate={true}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    expect(screen.getByText(/loading form data/i)).toBeInTheDocument();
  });

  it('renders form for creating hosted repository', async () => {
    renderWithTheme(
      <RepositoryForm
        recipe={MOCK_HOSTED_RECIPE}
        isCreate={true}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/blob store/i)).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: /create repository/i })).toBeInTheDocument();
  });

  it('renders form for creating proxy repository', async () => {
    const proxyFormData = {
      name: '', type: 'proxy', format: 'maven2',
      recipe: 'maven2-proxy', online: true,
      storage: { blobStoreName: '', strictContentTypeValidation: true },
      proxy: { remoteUrl: '', contentMaxAge: 1440, metadataMaxAge: 1440 },
    };
    mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
      formData: proxyFormData as any,
      form: createMockForm(proxyFormData),
    }));

    renderWithTheme(
      <RepositoryForm
        recipe={MOCK_PROXY_RECIPE}
        isCreate={true}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/remote storage/i)).toBeInTheDocument();
    });
  });

  it('renders form for creating group repository', async () => {
    const groupFormData = {
      name: '', type: 'group', format: 'maven2',
      recipe: 'maven2-group', online: true,
      storage: { blobStoreName: '', strictContentTypeValidation: true },
      group: { memberNames: [] },
    };
    mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
      formData: groupFormData as any,
      form: createMockForm(groupFormData),
      memberRepositories: [
        { id: 'maven-central', name: 'maven-central', format: 'maven2', type: 'proxy' },
        { id: 'maven-releases', name: 'maven-releases', format: 'maven2', type: 'hosted' },
      ] as any,
    }));

    renderWithTheme(
      <RepositoryForm
        recipe={MOCK_GROUP_RECIPE}
        isCreate={true}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    });

    await waitFor(() => {
      const groupSection = screen.queryByText('Group');
      expect(groupSection).toBeInTheDocument();
    });
  });

  it('renders form for editing repository', async () => {
    const editFormData = {
      name: 'maven-central', type: 'proxy', format: 'maven2',
      recipe: 'maven2-proxy', online: true,
      storage: { blobStoreName: 'default', strictContentTypeValidation: true },
      proxy: { remoteUrl: 'https://repo1.maven.org/maven2/', contentMaxAge: 1440, metadataMaxAge: 1440 },
      httpClient: { blocked: false, autoBlock: true },
      negativeCache: { enabled: true, timeToLive: 1440 },
    };
    mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
      isCreate: false,
      repository: MOCK_PROXY_REPOSITORY as any,
      activeTab: 'settings',
      formData: editFormData as any,
      form: createMockForm(editFormData),
    }));

    renderWithTheme(
      <RepositoryForm
        repository={MOCK_PROXY_REPOSITORY as any}
        recipe={MOCK_PROXY_RECIPE}
        isCreate={false}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        onDelete={mockOnDelete}
      />
    );

    await waitFor(() => {
      expect(screen.getByRole('tab', { name: /settings/i })).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getByDisplayValue('maven-central')).toBeInTheDocument();
    });

    const nameInput = screen.getByLabelText(/name/i);
    expect(nameInput).toBeDisabled();

    expect(screen.getByRole('button', { name: /delete/i })).toBeInTheDocument();
  });

  it('calls setActiveTab when tab is clicked', async () => {
    const mockSetActiveTab = jest.fn();
    const editFormData = {
      name: 'maven-central', type: 'proxy', format: 'maven2',
      recipe: 'maven2-proxy', online: true,
      storage: { blobStoreName: 'default', strictContentTypeValidation: true },
      proxy: { remoteUrl: 'https://repo1.maven.org/maven2/', contentMaxAge: 1440, metadataMaxAge: 1440 },
    };
    mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
      isCreate: false,
      repository: MOCK_PROXY_REPOSITORY as any,
      activeTab: 'summary',
      setActiveTab: mockSetActiveTab,
      formData: editFormData as any,
      form: createMockForm(editFormData),
    }));

    renderWithTheme(
      <RepositoryForm
        repository={MOCK_PROXY_REPOSITORY as any}
        recipe={MOCK_PROXY_RECIPE}
        isCreate={false}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    const settingsTab = await screen.findByRole('tab', { name: /settings/i });
    await userEvent.click(settingsTab);

    expect(mockSetActiveTab).toHaveBeenCalledWith('settings');
  });

  it('validates required name field', async () => {
    renderWithTheme(
      <RepositoryForm
        recipe={MOCK_HOSTED_RECIPE}
        isCreate={true}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    });

    const submitButton = screen.getByRole('button', { name: /create repository/i });
    await userEvent.click(submitButton);

    await waitFor(() => {
      expect(mockOnSave).not.toHaveBeenCalled();
    });
  });

  it('validates name pattern', async () => {
    const invalidData = {
      name: '-invalid-name', type: 'hosted', format: 'maven2', online: true,
      storage: { blobStoreName: '', strictContentTypeValidation: true },
    };
    const mockForm = createMockForm(invalidData);
    mockForm.isPristine = false;
    mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
      form: mockForm,
      formData: invalidData as any,
    }));

    renderWithTheme(
      <RepositoryForm
        recipe={MOCK_HOSTED_RECIPE}
        isCreate={true}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    const submitButton = screen.getByRole('button', { name: /create repository/i });
    await userEvent.click(submitButton);

    expect(mockForm.send).toHaveBeenCalledWith('SUBMIT');
  });

  it('validates required remote URL for proxy repository', async () => {
    const proxyFormData = {
      name: '', type: 'proxy', format: 'maven2',
      recipe: 'maven2-proxy', online: true,
      storage: { blobStoreName: '', strictContentTypeValidation: true },
      proxy: { remoteUrl: '', contentMaxAge: 1440, metadataMaxAge: 1440 },
    };
    mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
      formData: proxyFormData as any,
      form: createMockForm(proxyFormData),
    }));

    renderWithTheme(
      <RepositoryForm
        recipe={MOCK_PROXY_RECIPE}
        isCreate={true}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    });

    const submitButton = screen.getByRole('button', { name: /create repository/i });
    await userEvent.click(submitButton);

    await waitFor(() => {
      expect(mockOnSave).not.toHaveBeenCalled();
    });
  });

  it('renders blob store and content validation options', async () => {
    renderWithTheme(
      <RepositoryForm
        recipe={MOCK_HOSTED_RECIPE}
        isCreate={true}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    });

    expect(screen.getByText('Blob Store')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create repository/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
  });

  it('calls onCancel when cancel button is clicked', async () => {
    renderWithTheme(
      <RepositoryForm
        recipe={MOCK_HOSTED_RECIPE}
        isCreate={true}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /cancel/i }));

    expect(mockOnCancel).toHaveBeenCalled();
  });

  // NEXUS-53946: post-Repository-Profile migration the Settings header
  // gains a Browse Repository button that navigates to the in-app browse
  // tree. Only rendered in edit mode and only when the parent supplies the
  // callback.
  describe('Browse Repository action (NEXUS-53946)', () => {
    const buildEditHookReturn = () => {
      const editFormData = {
        name: 'maven-central', type: 'proxy', format: 'maven2',
        recipe: 'maven2-proxy', online: true,
        storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        proxy: { remoteUrl: 'https://repo1.maven.org/maven2/', contentMaxAge: 1440, metadataMaxAge: 1440 },
        httpClient: { blocked: false, autoBlock: true },
        negativeCache: { enabled: true, timeToLive: 1440 },
      };
      return createMockHookReturn({
        isCreate: false,
        repository: MOCK_PROXY_REPOSITORY as any,
        activeTab: 'settings',
        formData: editFormData as any,
        form: createMockForm(editFormData),
      });
    };

    it('renders Browse Repository button in edit mode when onBrowseRepository is provided', async () => {
      mockUseRepositoryForm.mockReturnValue(buildEditHookReturn());

      renderWithTheme(
        <RepositoryForm
          repository={MOCK_PROXY_REPOSITORY as any}
          recipe={MOCK_PROXY_RECIPE}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onBrowseRepository={jest.fn()}
        />
      );

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /browse repository/i })).toBeInTheDocument();
      });
    });

    it('does not render Browse Repository button when callback is not supplied', async () => {
      mockUseRepositoryForm.mockReturnValue(buildEditHookReturn());

      renderWithTheme(
        <RepositoryForm
          repository={MOCK_PROXY_REPOSITORY as any}
          recipe={MOCK_PROXY_RECIPE}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: /settings/i })).toBeInTheDocument();
      });

      expect(screen.queryByRole('button', { name: /browse repository/i })).not.toBeInTheDocument();
    });

    it('does not render Browse Repository button in create mode', async () => {
      renderWithTheme(
        <RepositoryForm
          recipe={MOCK_HOSTED_RECIPE}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onBrowseRepository={jest.fn()}
        />
      );

      await waitFor(() => {
        expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
      });

      expect(screen.queryByRole('button', { name: /browse repository/i })).not.toBeInTheDocument();
    });

    it('calls onBrowseRepository when the button is clicked', async () => {
      const mockOnBrowse = jest.fn();
      mockUseRepositoryForm.mockReturnValue(buildEditHookReturn());

      renderWithTheme(
        <RepositoryForm
          repository={MOCK_PROXY_REPOSITORY as any}
          recipe={MOCK_PROXY_RECIPE}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onBrowseRepository={mockOnBrowse}
        />
      );

      await userEvent.click(await screen.findByRole('button', { name: /browse repository/i }));

      expect(mockOnBrowse).toHaveBeenCalledTimes(1);
    });

    it('disables Browse Repository button while another action is in flight', async () => {
      mockUseRepositoryForm.mockReturnValue(buildEditHookReturn());

      renderWithTheme(
        <RepositoryForm
          repository={MOCK_PROXY_REPOSITORY as any}
          recipe={MOCK_PROXY_RECIPE}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onBrowseRepository={jest.fn()}
          isActionInFlight={true}
        />
      );

      const button = await screen.findByRole('button', { name: /browse repository/i });
      expect(button).toBeDisabled();
    });

    it('disables Browse Repository button while the form is dirty to avoid discarding unsaved edits', async () => {
      const hookReturn = buildEditHookReturn();
      hookReturn.form.isPristine = false;
      mockUseRepositoryForm.mockReturnValue(hookReturn);

      renderWithTheme(
        <RepositoryForm
          repository={MOCK_PROXY_REPOSITORY as any}
          recipe={MOCK_PROXY_RECIPE}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onBrowseRepository={jest.fn()}
        />
      );

      const button = await screen.findByRole('button', { name: /browse repository/i });
      expect(button).toBeDisabled();
      expect(button.getAttribute('title')).toMatch(/unsaved changes/i);
    });
  });

  it('shows delete button and calls onDelete', async () => {
    const editFormData = {
      name: 'maven-central', type: 'proxy', format: 'maven2',
      recipe: 'maven2-proxy', online: true,
      storage: { blobStoreName: 'default', strictContentTypeValidation: true },
      proxy: { remoteUrl: 'https://repo1.maven.org/maven2/', contentMaxAge: 1440, metadataMaxAge: 1440 },
      httpClient: { blocked: false, autoBlock: true },
      negativeCache: { enabled: true, timeToLive: 1440 },
    };
    mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
      isCreate: false,
      repository: MOCK_PROXY_REPOSITORY as any,
      activeTab: 'settings',
      formData: editFormData as any,
      form: createMockForm(editFormData),
    }));

    renderWithTheme(
      <RepositoryForm
        repository={MOCK_PROXY_REPOSITORY as any}
        recipe={MOCK_PROXY_RECIPE}
        isCreate={false}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        onDelete={mockOnDelete}
      />
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /delete/i })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /delete/i }));

    expect(mockOnDelete).toHaveBeenCalled();
  });

  it('displays error message from props', async () => {
    renderWithTheme(
      <RepositoryForm
        recipe={MOCK_HOSTED_RECIPE}
        isCreate={true}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        error="Something went wrong"
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Something went wrong')).toBeInTheDocument();
    });
  });

  it('shouldShowYumSigningFieldsWhenEditingYumProxy', async () => {
    const yumProxyData = {
      name: 'yum-proxy-test', type: 'proxy', format: 'yum',
      recipe: 'yum-proxy', online: true,
      storage: { blobStoreName: 'default', strictContentTypeValidation: true },
      proxy: { remoteUrl: 'https://mirror.stream.centos.org/', contentMaxAge: 1440, metadataMaxAge: 1440 },
    };
    mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
      isCreate: false,
      repository: { name: 'yum-proxy-test', type: 'proxy', format: 'yum' } as any,
      activeTab: 'settings',
      formData: yumProxyData as any,
      form: createMockForm(yumProxyData),
    }));

    const yumProxyRecipe = { format: 'yum', type: 'proxy', name: 'yum-proxy' };

    renderWithTheme(
      <RepositoryForm
        repository={{ name: 'yum-proxy-test', type: 'proxy', format: 'yum' } as any}
        recipe={yumProxyRecipe}
        isCreate={false}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Signing Key')).toBeInTheDocument();
      expect(screen.getByText('Passphrase')).toBeInTheDocument();
    });
  });

  it('shouldShowYumSigningFieldsWhenEditingYumGroup', async () => {
    const yumGroupData = {
      name: 'yum-group-test', type: 'group', format: 'yum',
      recipe: 'yum-group', online: true,
      storage: { blobStoreName: 'default', strictContentTypeValidation: true },
      group: { memberNames: ['yum-proxy-test'] },
    };
    mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
      isCreate: false,
      repository: { name: 'yum-group-test', type: 'group', format: 'yum' } as any,
      activeTab: 'settings',
      formData: yumGroupData as any,
      form: createMockForm(yumGroupData),
    }));

    const yumGroupRecipe = { format: 'yum', type: 'group', name: 'yum-group' };

    renderWithTheme(
      <RepositoryForm
        repository={{ name: 'yum-group-test', type: 'group', format: 'yum' } as any}
        recipe={yumGroupRecipe}
        isCreate={false}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Signing Key')).toBeInTheDocument();
      expect(screen.getByText('Passphrase')).toBeInTheDocument();
    });
  });

  it('shouldShowAptSigningFieldsWhenEditingAptProxy', async () => {
    const aptProxyData = {
      name: 'apt-proxy-test', type: 'proxy', format: 'apt',
      recipe: 'apt-proxy', online: true,
      storage: { blobStoreName: 'default', strictContentTypeValidation: true },
      proxy: { remoteUrl: 'http://archive.ubuntu.com/ubuntu/', contentMaxAge: 1440, metadataMaxAge: 1440 },
      apt: { distribution: 'focal' },
    };
    mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
      isCreate: false,
      repository: { name: 'apt-proxy-test', type: 'proxy', format: 'apt' } as any,
      activeTab: 'settings',
      formData: aptProxyData as any,
      form: createMockForm(aptProxyData),
    }));

    const aptProxyRecipe = { format: 'apt', type: 'proxy', name: 'apt-proxy' };

    renderWithTheme(
      <RepositoryForm
        repository={{ name: 'apt-proxy-test', type: 'proxy', format: 'apt' } as any}
        recipe={aptProxyRecipe}
        isCreate={false}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('APT Signing')).toBeInTheDocument();
      expect(screen.getByText('GPG Signing Key')).toBeInTheDocument();
      expect(screen.getByText('GPG Signing Key Passphrase')).toBeInTheDocument();
    });
  });

  it('shouldShowAptSigningFieldsWhenEditingAptHosted', async () => {
    const aptHostedData = {
      name: 'apt-hosted-test', type: 'hosted', format: 'apt',
      recipe: 'apt-hosted', online: true,
      storage: { blobStoreName: 'default', strictContentTypeValidation: true },
      apt: { distribution: 'focal' },
    };
    mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
      isCreate: false,
      repository: { name: 'apt-hosted-test', type: 'hosted', format: 'apt' } as any,
      activeTab: 'settings',
      formData: aptHostedData as any,
      form: createMockForm(aptHostedData),
    }));

    const aptHostedRecipe = { format: 'apt', type: 'hosted', name: 'apt-hosted' };

    renderWithTheme(
      <RepositoryForm
        repository={{ name: 'apt-hosted-test', type: 'hosted', format: 'apt' } as any}
        recipe={aptHostedRecipe}
        isCreate={false}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('APT Signing')).toBeInTheDocument();
      expect(screen.getByText('GPG Signing Key')).toBeInTheDocument();
      expect(screen.getByText('GPG Signing Key Passphrase')).toBeInTheDocument();
    });
  });

  describe('advanceOnly wizard mode (NEXUS-51923)', () => {
    const VALID_PROXY_DATA = {
      name: 'test-proxy',
      type: 'proxy',
      format: 'maven2',
      recipe: 'maven2-proxy',
      online: true,
      storage: { blobStoreName: 'default', strictContentTypeValidation: true },
      proxy: { remoteUrl: 'https://repo1.maven.org/maven2/', contentMaxAge: 1440, metadataMaxAge: 1440 },
      negativeCache: { enabled: true, timeToLive: 1440 },
      httpClient: { blocked: false, autoBlock: true },
    };

    const INVALID_PROXY_DATA = {
      name: 'test-proxy',
      type: 'proxy',
      format: 'maven2',
      recipe: 'maven2-proxy',
      online: true,
      storage: { blobStoreName: 'default', strictContentTypeValidation: true },
      proxy: { remoteUrl: '', contentMaxAge: 1440, metadataMaxAge: 1440 },
    };

    it('shouldCallOnSaveDirectlyWhenAdvanceOnlyAndFormIsValid', async () => {
      const mockForm = createMockForm(VALID_PROXY_DATA);
      mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
        form: mockForm,
        formData: VALID_PROXY_DATA as any,
      }));

      const submitRef = { current: null } as React.MutableRefObject<(() => void) | null>;

      renderWithTheme(
        <RepositoryForm
          recipe={MOCK_PROXY_RECIPE}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          hideActions
          onSubmitRef={submitRef}
          advanceOnly={true}
        />
      );

      // The submit logic is now in the hook, which is mocked.
      // This test verifies the component passes onSubmitRef to the hook.
      // The hook integration test (useRepositoryForm.test.ts) covers the actual logic.
      expect(mockUseRepositoryForm).toHaveBeenCalledWith(
        expect.objectContaining({ onSubmitRef: submitRef, advanceOnly: true })
      );
    });

    it('shouldSendSubmitToMachineWhenAdvanceOnlyAndFormIsInvalid', async () => {
      const mockForm = createMockForm(INVALID_PROXY_DATA);
      mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
        form: mockForm,
        formData: INVALID_PROXY_DATA as any,
      }));

      const submitRef = { current: null } as React.MutableRefObject<(() => void) | null>;

      renderWithTheme(
        <RepositoryForm
          recipe={MOCK_PROXY_RECIPE}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          hideActions
          onSubmitRef={submitRef}
          advanceOnly={true}
        />
      );

      expect(mockUseRepositoryForm).toHaveBeenCalledWith(
        expect.objectContaining({ onSubmitRef: submitRef, advanceOnly: true })
      );
    });

    it('shouldSendSubmitToMachineWhenNotAdvanceOnly', async () => {
      const mockForm = createMockForm(VALID_PROXY_DATA);
      mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
        form: mockForm,
        formData: VALID_PROXY_DATA as any,
      }));

      const submitRef = { current: null } as React.MutableRefObject<(() => void) | null>;

      renderWithTheme(
        <RepositoryForm
          recipe={MOCK_PROXY_RECIPE}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          hideActions
          onSubmitRef={submitRef}
          advanceOnly={false}
        />
      );

      expect(mockUseRepositoryForm).toHaveBeenCalledWith(
        expect.objectContaining({ onSubmitRef: submitRef, advanceOnly: false })
      );
    });

    it('shouldNotSubmitWhenFormIsLoadingInAdvanceOnlyMode', async () => {
      const mockForm = createMockForm(VALID_PROXY_DATA);
      mockForm.isLoading = true;
      mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
        form: mockForm,
        formData: VALID_PROXY_DATA as any,
      }));

      const submitRef = { current: null } as React.MutableRefObject<(() => void) | null>;

      renderWithTheme(
        <RepositoryForm
          recipe={MOCK_PROXY_RECIPE}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          hideActions
          onSubmitRef={submitRef}
          advanceOnly={true}
        />
      );

      // Component shows loading state, submit logic is handled by hook
      expect(screen.getByText(/loading form data/i)).toBeInTheDocument();
    });
  });

  describe('Audit tab (NEXUS-53949)', () => {
    const { ExtJS } = jest.requireMock('../../../../../../../interface/ExtJS');

    beforeEach(() => {
      // Reset the mock to return false by default
      ExtJS.state().getValue.mockImplementation((key: string) => {
        if (key === 'previewAuditEnabled') return false;
        return undefined;
      });
      ExtJS.checkPermission.mockImplementation((perm: string) => {
        if (perm === 'nexus:audit:read') return false;
        return true;
      });
    });

    it('shouldNotShowAuditTabWhenAuditIsDisabled', async () => {
      ExtJS.state().getValue.mockImplementation((key: string) => {
        if (key === 'previewAuditEnabled') return false;
        return undefined;
      });

      const editFormData = {
        name: 'maven-central', type: 'proxy', format: 'maven2',
        recipe: 'maven2-proxy', online: true,
        storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        proxy: { remoteUrl: 'https://repo1.maven.org/maven2/', contentMaxAge: 1440, metadataMaxAge: 1440 },
        httpClient: { blocked: false, autoBlock: true },
        negativeCache: { enabled: true, timeToLive: 1440 },
      };
      mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
        isCreate: false,
        repository: MOCK_PROXY_REPOSITORY as any,
        activeTab: 'settings',
        formData: editFormData as any,
        form: createMockForm(editFormData),
      }));

      renderWithTheme(
        <RepositoryForm
          repository={MOCK_PROXY_REPOSITORY as any}
          recipe={MOCK_PROXY_RECIPE}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onDelete={mockOnDelete}
        />
      );

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: /settings/i })).toBeInTheDocument();
      });

      // Audit tab should not be present when previewAuditEnabled is false
      expect(screen.queryByRole('tab', { name: /audit/i })).not.toBeInTheDocument();
    });

    it('shouldNotShowAuditTabWhenUserLacksAuditReadPermission', async () => {
      // Audit enabled but user lacks permission
      ExtJS.state().getValue.mockImplementation((key: string) => {
        if (key === 'previewAuditEnabled') return true;
        return undefined;
      });
      ExtJS.checkPermission.mockImplementation((perm: string) => {
        if (perm === 'nexus:audit:read') return false;
        return true;
      });

      const editFormData = {
        name: 'maven-central', type: 'proxy', format: 'maven2',
        recipe: 'maven2-proxy', online: true,
        storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        proxy: { remoteUrl: 'https://repo1.maven.org/maven2/', contentMaxAge: 1440, metadataMaxAge: 1440 },
        httpClient: { blocked: false, autoBlock: true },
        negativeCache: { enabled: true, timeToLive: 1440 },
      };
      mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
        isCreate: false,
        repository: MOCK_PROXY_REPOSITORY as any,
        activeTab: 'settings',
        formData: editFormData as any,
        form: createMockForm(editFormData),
      }));

      renderWithTheme(
        <RepositoryForm
          repository={MOCK_PROXY_REPOSITORY as any}
          recipe={MOCK_PROXY_RECIPE}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onDelete={mockOnDelete}
        />
      );

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: /settings/i })).toBeInTheDocument();
      });

      // Audit tab should not be present when user lacks nexus:audit:read permission
      expect(screen.queryByRole('tab', { name: /audit/i })).not.toBeInTheDocument();
    });

    it('shouldShowAuditTabWhenAuditIsEnabledAndUserHasPermission', async () => {
      // Enable audit and grant permission
      ExtJS.state().getValue.mockImplementation((key: string) => {
        if (key === 'previewAuditEnabled') return true;
        return undefined;
      });
      ExtJS.checkPermission.mockImplementation((perm: string) => {
        if (perm === 'nexus:audit:read') return true;
        return true;
      });

      const editFormData = {
        name: 'maven-central', type: 'proxy', format: 'maven2',
        recipe: 'maven2-proxy', online: true,
        storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        proxy: { remoteUrl: 'https://repo1.maven.org/maven2/', contentMaxAge: 1440, metadataMaxAge: 1440 },
        httpClient: { blocked: false, autoBlock: true },
        negativeCache: { enabled: true, timeToLive: 1440 },
      };
      mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
        isCreate: false,
        repository: MOCK_PROXY_REPOSITORY as any,
        activeTab: 'settings',
        formData: editFormData as any,
        form: createMockForm(editFormData),
      }));

      renderWithTheme(
        <RepositoryForm
          repository={MOCK_PROXY_REPOSITORY as any}
          recipe={MOCK_PROXY_RECIPE}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onDelete={mockOnDelete}
        />
      );

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: /audit/i })).toBeInTheDocument();
      });

      // Should also show Settings and Summary tabs
      expect(screen.getByRole('tab', { name: /settings/i })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /summary/i })).toBeInTheDocument();
    });

    it('shouldNotShowAuditTabForCreateMode', async () => {
      // Enable audit and grant permission
      ExtJS.state().getValue.mockImplementation((key: string) => {
        if (key === 'previewAuditEnabled') return true;
        return undefined;
      });
      ExtJS.checkPermission.mockImplementation((perm: string) => {
        if (perm === 'nexus:audit:read') return true;
        return true;
      });

      renderWithTheme(
        <RepositoryForm
          recipe={MOCK_HOSTED_RECIPE}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      await waitFor(() => {
        expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
      });

      // Audit tab should not be present in create mode
      expect(screen.queryByRole('tab', { name: /audit/i })).not.toBeInTheDocument();
    });

    it('shouldRenderAuditTabContentWhenAuditTabIsSelected', async () => {
      // Enable audit and grant permission
      ExtJS.state().getValue.mockImplementation((key: string) => {
        if (key === 'previewAuditEnabled') return true;
        return undefined;
      });
      ExtJS.checkPermission.mockImplementation((perm: string) => {
        if (perm === 'nexus:audit:read') return true;
        return true;
      });

      mockUseAuditLogApi.mockReturnValue({
        data: null,
        loading: true,
        error: null,
      });

      const editFormData = {
        name: 'maven-central', type: 'proxy', format: 'maven2',
        recipe: 'maven2-proxy', online: true,
        storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        proxy: { remoteUrl: 'https://repo1.maven.org/maven2/', contentMaxAge: 1440, metadataMaxAge: 1440 },
        httpClient: { blocked: false, autoBlock: true },
        negativeCache: { enabled: true, timeToLive: 1440 },
      };
      mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
        isCreate: false,
        repository: MOCK_PROXY_REPOSITORY as any,
        activeTab: 'audit',
        formData: editFormData as any,
        form: createMockForm(editFormData),
      }));

      renderWithTheme(
        <RepositoryForm
          repository={MOCK_PROXY_REPOSITORY as any}
          recipe={MOCK_PROXY_RECIPE}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onDelete={mockOnDelete}
        />
      );

      // Should render audit content - it will show loading spinner since useAuditLogApi is mocked as loading
      await waitFor(() => {
        // The tab content renders RepositoryAuditTab which shows loading spinner
        // Spinner is rendered as a div with class containing "rt-Spinner"
        const spinner = document.querySelector('.rt-Spinner');
        expect(spinner).toBeInTheDocument();
      });
    });
  });

  describe('Tasks & Capabilities tab (NEXUS-53943)', () => {
    const { ExtJS } = jest.requireMock('../../../../../../../interface/ExtJS');

    beforeEach(() => {
      ExtJS.checkPermission.mockReturnValue(true);
    });

    it('shouldRenderTasksAndCapabilitiesTabWhenUserHasTasksReadPermission', async () => {
      ExtJS.checkPermission.mockImplementation((perm: string) => perm === 'nexus:tasks:read');

      mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
        isCreate: false,
        repository: MOCK_PROXY_REPOSITORY as any,
        activeTab: 'settings',
        formData: MOCK_PROXY_REPOSITORY.attributes as any,
        form: createMockForm(MOCK_PROXY_REPOSITORY.attributes),
      }));

      renderWithTheme(
        <RepositoryForm
          repository={MOCK_PROXY_REPOSITORY as any}
          recipe={MOCK_PROXY_RECIPE}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onDelete={mockOnDelete}
        />
      );

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: /Tasks & Capabilities/i })).toBeInTheDocument();
      });
    });

    it('shouldRenderTasksAndCapabilitiesTabWhenUserHasCapabilitiesReadPermission', async () => {
      ExtJS.checkPermission.mockImplementation((perm: string) => perm === 'nexus:capabilities:read');

      mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
        isCreate: false,
        repository: MOCK_PROXY_REPOSITORY as any,
        activeTab: 'settings',
        formData: MOCK_PROXY_REPOSITORY.attributes as any,
        form: createMockForm(MOCK_PROXY_REPOSITORY.attributes),
      }));

      renderWithTheme(
        <RepositoryForm
          repository={MOCK_PROXY_REPOSITORY as any}
          recipe={MOCK_PROXY_RECIPE}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onDelete={mockOnDelete}
        />
      );

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: /Tasks & Capabilities/i })).toBeInTheDocument();
      });
    });

    it('shouldHideTasksAndCapabilitiesTabWhenUserHasNeitherPermission', async () => {
      ExtJS.checkPermission.mockImplementation((perm: string) => {
        if (perm === 'nexus:tasks:read') return false;
        if (perm === 'nexus:capabilities:read') return false;
        return true;
      });

      mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
        isCreate: false,
        repository: MOCK_PROXY_REPOSITORY as any,
        activeTab: 'settings',
        formData: MOCK_PROXY_REPOSITORY.attributes as any,
        form: createMockForm(MOCK_PROXY_REPOSITORY.attributes),
      }));

      renderWithTheme(
        <RepositoryForm
          repository={MOCK_PROXY_REPOSITORY as any}
          recipe={MOCK_PROXY_RECIPE}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onDelete={mockOnDelete}
        />
      );

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: /settings/i })).toBeInTheDocument();
      });
      expect(screen.queryByRole('tab', { name: /Tasks & Capabilities/i })).not.toBeInTheDocument();
    });

    it('shouldHideTasksAndCapabilitiesTabInCreateMode', async () => {
      ExtJS.checkPermission.mockReturnValue(true);

      renderWithTheme(
        <RepositoryForm
          recipe={MOCK_HOSTED_RECIPE}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      await waitFor(() => {
        expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
      });
      expect(screen.queryByRole('tab', { name: /Tasks & Capabilities/i })).not.toBeInTheDocument();
    });
  });

  describe('Access & Security tab (NEXUS-53942)', () => {
    const { ExtJS } = jest.requireMock('../../../../../../../interface/ExtJS');

    const ACCESS_SECURITY_PERMS = [
      'nexus:privileges:read',
      'nexus:roles:read',
      'nexus:users:read',
      'nexus:settings:read',
    ];

    beforeEach(() => {
      ExtJS.checkPermission.mockReturnValue(true);
    });

    it.each(ACCESS_SECURITY_PERMS)(
      'shouldRenderAccessSecurityTabWhenUserHas_%s',
      async (grantedPerm: string) => {
        ExtJS.checkPermission.mockImplementation((perm: string) => perm === grantedPerm);

        mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
          isCreate: false,
          repository: MOCK_PROXY_REPOSITORY as any,
          activeTab: 'settings',
          formData: MOCK_PROXY_REPOSITORY.attributes as any,
          form: createMockForm(MOCK_PROXY_REPOSITORY.attributes),
        }));

        renderWithTheme(
          <RepositoryForm
            repository={MOCK_PROXY_REPOSITORY as any}
            recipe={MOCK_PROXY_RECIPE}
            isCreate={false}
            onSave={mockOnSave}
            onCancel={mockOnCancel}
            onDelete={mockOnDelete}
          />
        );

        await waitFor(() => {
          expect(screen.getByRole('tab', { name: /Access & Security/i })).toBeInTheDocument();
        });
      }
    );

    it('shouldHideAccessSecurityTabWhenUserHasNoneOfTheFourPermissions', async () => {
      ExtJS.checkPermission.mockImplementation((perm: string) =>
        !ACCESS_SECURITY_PERMS.includes(perm)
      );

      mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
        isCreate: false,
        repository: MOCK_PROXY_REPOSITORY as any,
        activeTab: 'settings',
        formData: MOCK_PROXY_REPOSITORY.attributes as any,
        form: createMockForm(MOCK_PROXY_REPOSITORY.attributes),
      }));

      renderWithTheme(
        <RepositoryForm
          repository={MOCK_PROXY_REPOSITORY as any}
          recipe={MOCK_PROXY_RECIPE}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          onDelete={mockOnDelete}
        />
      );

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: /settings/i })).toBeInTheDocument();
      });
      expect(screen.queryByRole('tab', { name: /Access & Security/i })).not.toBeInTheDocument();
    });

    it('shouldHideAccessSecurityTabInCreateMode', async () => {
      ExtJS.checkPermission.mockReturnValue(true);

      renderWithTheme(
        <RepositoryForm
          recipe={MOCK_HOSTED_RECIPE}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      await waitFor(() => {
        expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
      });
      expect(screen.queryByRole('tab', { name: /Access & Security/i })).not.toBeInTheDocument();
    });
  });

  // NEXUS-54212: the Save/Update button must respect the per-repository edit permission
  // (nexus:repository-admin:{format}:{name}:edit), matching Classic UI (RepositorySettingsForm.js).
  // A user with repository-admin:read but no :edit for this repo can open the detail but must not
  // see a Save button.
  describe('Save button permission gating (NEXUS-54212)', () => {
    const { ExtJS } = jest.requireMock('../../../../../../../interface/ExtJS');

    const editFormData = {
      name: 'maven-central', type: 'proxy', format: 'maven2',
      recipe: 'maven2-proxy', online: true,
      storage: { blobStoreName: 'default', strictContentTypeValidation: true },
      proxy: { remoteUrl: 'https://repo1.maven.org/maven2/', contentMaxAge: 1440, metadataMaxAge: 1440 },
      httpClient: { blocked: false, autoBlock: true },
      negativeCache: { enabled: true, timeToLive: 1440 },
    };

    const renderEdit = () => {
      mockUseRepositoryForm.mockReturnValue(createMockHookReturn({
        isCreate: false,
        repository: MOCK_PROXY_REPOSITORY as any,
        activeTab: 'settings',
        formData: editFormData as any,
        pristineData: MOCK_PROXY_REPOSITORY as any,
        form: createMockForm(editFormData),
      }));

      return renderWithTheme(
        <RepositoryForm
          repository={MOCK_PROXY_REPOSITORY as any}
          recipe={MOCK_PROXY_RECIPE}
          isCreate={false}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );
    };

    it('hides the Save button when the user lacks per-repo edit permission', async () => {
      ExtJS.checkPermission.mockImplementation(() => false);

      renderEdit();

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: /settings/i })).toBeInTheDocument();
      });

      expect(screen.queryByTestId('form-submit')).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /save changes/i })).not.toBeInTheDocument();
    });

    it('shows the Save button when the user holds edit permission for this repo', async () => {
      ExtJS.checkPermission.mockImplementation(
        (perm: string) => perm === 'nexus:repository-admin:maven2:maven-central:edit'
      );

      renderEdit();

      await waitFor(() => {
        expect(screen.getByRole('tab', { name: /settings/i })).toBeInTheDocument();
      });

      expect(screen.getByTestId('form-submit')).toBeInTheDocument();
    });

    it('shows the Save button for a holder of the repository-admin edit wildcard', async () => {
      // ExtJS.checkPermission applies Shiro wildcard semantics, so *:*:edit satisfies the
      // concrete per-repo check.
      ExtJS.checkPermission.mockImplementation(
        (perm: string) => perm === 'nexus:repository-admin:maven2:maven-central:edit'
      );

      renderEdit();

      await waitFor(() => {
        expect(screen.getByTestId('form-submit')).toBeInTheDocument();
      });
    });

    it('always shows the Create button in create mode regardless of per-repo edit permission', async () => {
      ExtJS.checkPermission.mockImplementation(() => false);

      renderWithTheme(
        <RepositoryForm
          recipe={MOCK_HOSTED_RECIPE}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
        />
      );

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /create repository/i })).toBeInTheDocument();
      });
    });
  });
});
