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
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';

import { RepositoryForm } from '../RepositoryForm';
import { useRepositoriesApi } from '../useRepositoriesApi';
import { useRepositoryForm } from '../useRepositoryForm';
import { ToastProvider } from '../../../../../shared/Toast';

// Mock hooks
jest.mock('../useRepositoriesApi');
jest.mock('../useRepositoryForm');

const mockUseRepositoriesApi = useRepositoriesApi as jest.MockedFunction<typeof useRepositoriesApi>;
const mockUseRepositoryForm = useRepositoryForm as jest.MockedFunction<typeof useRepositoryForm>;

function createMockRepoForm(data: any = {}, refs: any = {}) {
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
    state: { matches: jest.fn(() => false), context: { data, ...refs } },
    send: jest.fn(),
  } as any;
}

const mockBlobStores = [{ name: 'default' }, { name: 'secondary' }];
const mockRoutingRules = [{ id: '1', name: 'block-snapshots', mode: 'BLOCK' }];
const mockCleanupPolicies = [{ name: 'maven-cleanup', format: 'maven2' }];

const mockApiHook = {
  loading: false,
  error: null,
  setError: jest.fn(),
  fetchBlobStores: jest.fn().mockResolvedValue(mockBlobStores),
  fetchRoutingRules: jest.fn().mockResolvedValue(mockRoutingRules),
  fetchCleanupPolicies: jest.fn().mockResolvedValue(mockCleanupPolicies),
  fetchRepositoryReferences: jest.fn().mockResolvedValue([]),
};

const mockProxyRecipe = { format: 'maven2', type: 'proxy', name: 'maven2-proxy' };
const mockHostedRecipe = { format: 'maven2', type: 'hosted', name: 'maven2-hosted' };
const mockGroupRecipe = { format: 'maven2', type: 'group', name: 'maven2-group' };

const mockProxyRepository = {
  name: 'maven-central',
  type: 'proxy',
  format: 'maven2',
  recipe: 'maven2-proxy',
  online: true,
  url: 'http://localhost:8081/repository/maven-central/',
  status: { online: true },
  attributes: {
    storage: {
      blobStoreName: 'default',
      strictContentTypeValidation: true,
    },
    proxy: {
      remoteUrl: 'https://repo1.maven.org/maven2/',
      contentMaxAge: 1440,
      metadataMaxAge: 1440,
    },
    negativeCache: {
      enabled: true,
      timeToLive: 1440,
    },
    httpClient: {
      blocked: false,
      autoBlock: true,
    },
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
    mockUseRepositoriesApi.mockReturnValue(mockApiHook as any);
    mockUseRepositoryForm.mockImplementation(({ repository, format, repositoryType }: any) => {
      const formData = repository ? {
        name: repository.name, type: repository.type, format: repository.format,
        recipe: repository.recipe || `${repository.format}-${repository.type}`,
        online: repository.online ?? true, storage: repository.attributes?.storage || { blobStoreName: 'default', strictContentTypeValidation: true },
        proxy: repository.attributes?.proxy, httpClient: repository.attributes?.httpClient,
        negativeCache: repository.attributes?.negativeCache, group: repository.attributes?.group,
        cleanup: repository.attributes?.cleanup, maven: repository.attributes?.maven,
        docker: repository.attributes?.docker,
      } : {
        name: '', type: repositoryType || 'hosted', format: format || 'maven2',
        recipe: `${format || 'maven2'}-${repositoryType || 'hosted'}`,
        online: true, storage: { blobStoreName: '', strictContentTypeValidation: true },
      };
      return {
        form: createMockRepoForm(formData, {
          blobStores: mockBlobStores, routingRules: mockRoutingRules,
          cleanupPolicies: mockCleanupPolicies, memberOptions: [],
        }),
        repository: repository || null,
        isCreate: !repository,
      } as any;
    });
  });

  it('renders loading state while fetching reference data', () => {
    const loadingForm = createMockRepoForm({}, { blobStores: [], routingRules: [], cleanupPolicies: [], memberOptions: [] });
    loadingForm.isLoading = true;
    mockUseRepositoryForm.mockReturnValue({
      form: loadingForm,
      repository: null,
      isCreate: true,
    } as any);

    renderWithTheme(
      <RepositoryForm
        recipe={mockHostedRecipe}
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
        recipe={mockHostedRecipe}
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
    renderWithTheme(
      <RepositoryForm
        recipe={mockProxyRecipe}
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
    mockUseRepositoriesApi.mockReturnValue({
      ...mockApiHook,
      fetchRepositoryReferences: jest.fn().mockResolvedValue([
        { name: 'maven-central', type: 'proxy' },
        { name: 'maven-releases', type: 'hosted' },
      ]),
    } as any);

    renderWithTheme(
      <RepositoryForm
        recipe={mockGroupRecipe}
        isCreate={true}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    });

    // Group repositories have a Group section (rendered by GroupFacet)
    // Wait for the Group section title to appear
    await waitFor(() => {
      const groupSection = screen.queryByText('Group');
      expect(groupSection).toBeInTheDocument();
    });
  });

  it('renders form for editing repository', async () => {
    renderWithTheme(
      <RepositoryForm
        repository={mockProxyRepository as any}
        recipe={mockProxyRecipe}
        isCreate={false}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
        onDelete={mockOnDelete}
      />
    );

    await waitFor(() => {
      expect(screen.getByRole('tab', { name: /settings/i })).toBeInTheDocument();
    });

    const settingsTab = screen.getByRole('tab', { name: /settings/i });
    await userEvent.click(settingsTab);

    await waitFor(() => {
      expect(screen.getByDisplayValue('maven-central')).toBeInTheDocument();
    });

    // Name should be disabled in edit mode
    const nameInput = screen.getByLabelText(/name/i);
    expect(nameInput).toBeDisabled();

    // Delete button should be present
    expect(screen.getByRole('button', { name: /delete/i })).toBeInTheDocument();
  });

  it('validates required name field', async () => {
    renderWithTheme(
      <RepositoryForm
        recipe={mockHostedRecipe}
        isCreate={true}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    });

    // Submit form without filling name
    const submitButton = screen.getByRole('button', { name: /create repository/i });
    await userEvent.click(submitButton);

    // Since the form validates on submit, it should prevent onSave from being called
    // when required fields are empty
    await waitFor(() => {
      expect(mockOnSave).not.toHaveBeenCalled();
    });
  });

  it('validates name pattern', async () => {
    // With XState, validation happens in the machine when SUBMIT is sent
    const mockForm = createMockRepoForm(
      { name: '-invalid-name', type: 'hosted', format: 'maven2', online: true, storage: { blobStoreName: '', strictContentTypeValidation: true } },
      { blobStores: mockBlobStores, routingRules: mockRoutingRules, cleanupPolicies: mockCleanupPolicies, memberOptions: [] }
    );
    mockForm.isPristine = false;
    mockUseRepositoryForm.mockReturnValue({ form: mockForm, repository: null, isCreate: true } as any);

    renderWithTheme(
      <RepositoryForm
        recipe={mockHostedRecipe}
        isCreate={true}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    const submitButton = screen.getByRole('button', { name: /create repository/i });
    await userEvent.click(submitButton);

    // Machine handles validation - verify submit event was sent
    expect(mockForm.send).toHaveBeenCalledWith('SUBMIT');
  });

  it('validates required remote URL for proxy repository', async () => {
    renderWithTheme(
      <RepositoryForm
        recipe={mockProxyRecipe}
        isCreate={true}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    });

    // Fill name but not remote URL
    const nameInput = screen.getByLabelText(/name/i);
    await userEvent.type(nameInput, 'test-proxy');

    // Note: We can't easily interact with Radix UI Select components in tests
    // Instead, test that validation happens by clicking submit without all required fields

    const submitButton = screen.getByRole('button', { name: /create repository/i });
    await userEvent.click(submitButton);

    // Form should show validation errors (either for remote URL or blob store)
    await waitFor(() => {
      // Check that save was not called because of validation errors
      expect(mockOnSave).not.toHaveBeenCalled();
    });
  });

  it('renders blob store and content validation options', async () => {
    // Since Radix UI Select components are difficult to test with userEvent.selectOptions,
    // we verify the form renders the expected elements instead of full form submission
    renderWithTheme(
      <RepositoryForm
        recipe={mockHostedRecipe}
        isCreate={true}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    await waitFor(() => {
      expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    });

    // Verify blob store label is present
    expect(screen.getByText('Blob Store')).toBeInTheDocument();

    // Verify the form has expected structure
    expect(screen.getByRole('button', { name: /create repository/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
  });

  it('calls onCancel when cancel button is clicked', async () => {
    renderWithTheme(
      <RepositoryForm
        recipe={mockHostedRecipe}
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

  it('shows delete button and calls onDelete', async () => {
    renderWithTheme(
      <RepositoryForm
        repository={mockProxyRepository as any}
        recipe={mockProxyRecipe}
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

    // Delete button now calls onDelete directly (machine handles confirmation)
    expect(mockOnDelete).toHaveBeenCalled();
  });

  it('displays error message from props', async () => {
    renderWithTheme(
      <RepositoryForm
        recipe={mockHostedRecipe}
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
    const yumProxyRepo = {
      name: 'yum-proxy-test',
      type: 'proxy',
      format: 'yum',
      recipe: 'yum-proxy',
      online: true,
      url: 'http://localhost:8081/repository/yum-proxy-test/',
      status: { online: true },
      attributes: {
        storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        proxy: { remoteUrl: 'http://mirror.centos.org/centos/', contentMaxAge: 1440, metadataMaxAge: 1440 },
      },
    };
    const yumProxyRecipe = { format: 'yum', type: 'proxy', name: 'yum-proxy' };

    renderWithTheme(
      <RepositoryForm
        repository={yumProxyRepo as any}
        recipe={yumProxyRecipe}
        isCreate={false}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    // Click the Settings tab to reveal the form content
    const settingsTab = await screen.findByRole('tab', { name: /settings/i });
    await userEvent.click(settingsTab);

    // Verify signing fields are present
    await waitFor(() => {
      expect(screen.getByText('Signing Key')).toBeInTheDocument();
      expect(screen.getByText('Passphrase')).toBeInTheDocument();
    });
  });

  it('shouldShowYumSigningFieldsWhenEditingYumGroup', async () => {
    const yumGroupRepo = {
      name: 'yum-group-test',
      type: 'group',
      format: 'yum',
      recipe: 'yum-group',
      online: true,
      url: 'http://localhost:8081/repository/yum-group-test/',
      status: { online: true },
      attributes: {
        storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        group: { memberNames: ['yum-proxy-test'] },
      },
    };
    const yumGroupRecipe = { format: 'yum', type: 'group', name: 'yum-group' };

    renderWithTheme(
      <RepositoryForm
        repository={yumGroupRepo as any}
        recipe={yumGroupRecipe}
        isCreate={false}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    const settingsTab = await screen.findByRole('tab', { name: /settings/i });
    await userEvent.click(settingsTab);

    await waitFor(() => {
      expect(screen.getByText('Signing Key')).toBeInTheDocument();
      expect(screen.getByText('Passphrase')).toBeInTheDocument();
    });
  });

  it('shouldShowAptSigningFieldsWhenEditingAptProxy', async () => {
    const aptProxyRepo = {
      name: 'apt-proxy-test',
      type: 'proxy',
      format: 'apt',
      recipe: 'apt-proxy',
      online: true,
      url: 'http://localhost:8081/repository/apt-proxy-test/',
      status: { online: true },
      attributes: {
        storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        proxy: { remoteUrl: 'http://archive.ubuntu.com/ubuntu/', contentMaxAge: 1440, metadataMaxAge: 1440 },
        apt: { distribution: 'focal' },
      },
    };
    const aptProxyRecipe = { format: 'apt', type: 'proxy', name: 'apt-proxy' };

    renderWithTheme(
      <RepositoryForm
        repository={aptProxyRepo as any}
        recipe={aptProxyRecipe}
        isCreate={false}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    const settingsTab = await screen.findByRole('tab', { name: /settings/i });
    await userEvent.click(settingsTab);

    await waitFor(() => {
      expect(screen.getByText('APT Signing')).toBeInTheDocument();
      expect(screen.getByText('GPG Signing Key')).toBeInTheDocument();
      expect(screen.getByText('GPG Signing Key Passphrase')).toBeInTheDocument();
    });
  });

  it('shouldShowAptSigningFieldsWhenEditingAptHosted', async () => {
    const aptHostedRepo = {
      name: 'apt-hosted-test',
      type: 'hosted',
      format: 'apt',
      recipe: 'apt-hosted',
      online: true,
      url: 'http://localhost:8081/repository/apt-hosted-test/',
      status: { online: true },
      attributes: {
        storage: { blobStoreName: 'default', strictContentTypeValidation: true },
        apt: { distribution: 'focal' },
      },
    };
    const aptHostedRecipe = { format: 'apt', type: 'hosted', name: 'apt-hosted' };

    renderWithTheme(
      <RepositoryForm
        repository={aptHostedRepo as any}
        recipe={aptHostedRecipe}
        isCreate={false}
        onSave={mockOnSave}
        onCancel={mockOnCancel}
      />
    );

    const settingsTab = await screen.findByRole('tab', { name: /settings/i });
    await userEvent.click(settingsTab);

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
      const mockForm = createMockRepoForm(VALID_PROXY_DATA, {
        blobStores: mockBlobStores, routingRules: mockRoutingRules,
        cleanupPolicies: mockCleanupPolicies, memberOptions: [],
      });
      mockUseRepositoryForm.mockReturnValue({ form: mockForm, repository: null, isCreate: true } as any);

      const submitRef = { current: null } as React.MutableRefObject<(() => void) | null>;

      renderWithTheme(
        <RepositoryForm
          recipe={mockProxyRecipe}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          hideActions
          onSubmitRef={submitRef}
          advanceOnly={true}
        />
      );

      // Wait for the ref to be populated
      await waitFor(() => {
        expect(submitRef.current).not.toBeNull();
      });

      // Trigger the submit via ref (simulates wizard Continue click)
      submitRef.current!();

      // onSave should be called directly with form data
      expect(mockOnSave).toHaveBeenCalledWith(VALID_PROXY_DATA);
      // Machine SUBMIT should NOT be sent (avoids terminal saved state)
      expect(mockForm.send).not.toHaveBeenCalledWith('SUBMIT');
    });

    it('shouldSendSubmitToMachineWhenAdvanceOnlyAndFormIsInvalid', async () => {
      const mockForm = createMockRepoForm(INVALID_PROXY_DATA, {
        blobStores: mockBlobStores, routingRules: mockRoutingRules,
        cleanupPolicies: mockCleanupPolicies, memberOptions: [],
      });
      mockUseRepositoryForm.mockReturnValue({ form: mockForm, repository: null, isCreate: true } as any);

      const submitRef = { current: null } as React.MutableRefObject<(() => void) | null>;

      renderWithTheme(
        <RepositoryForm
          recipe={mockProxyRecipe}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          hideActions
          onSubmitRef={submitRef}
          advanceOnly={true}
        />
      );

      await waitFor(() => {
        expect(submitRef.current).not.toBeNull();
      });

      submitRef.current!();

      // onSave should NOT be called (form is invalid)
      expect(mockOnSave).not.toHaveBeenCalled();
      // Machine SUBMIT should be sent to trigger validation error display
      expect(mockForm.send).toHaveBeenCalledWith('SUBMIT');
    });

    it('shouldSendSubmitToMachineWhenNotAdvanceOnly', async () => {
      const mockForm = createMockRepoForm(VALID_PROXY_DATA, {
        blobStores: mockBlobStores, routingRules: mockRoutingRules,
        cleanupPolicies: mockCleanupPolicies, memberOptions: [],
      });
      mockUseRepositoryForm.mockReturnValue({ form: mockForm, repository: null, isCreate: true } as any);

      const submitRef = { current: null } as React.MutableRefObject<(() => void) | null>;

      renderWithTheme(
        <RepositoryForm
          recipe={mockProxyRecipe}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          hideActions
          onSubmitRef={submitRef}
          advanceOnly={false}
        />
      );

      await waitFor(() => {
        expect(submitRef.current).not.toBeNull();
      });

      submitRef.current!();

      // Normal flow: machine handles submission
      expect(mockForm.send).toHaveBeenCalledWith('SUBMIT');
      // onSave should NOT be called directly (machine handles it)
      expect(mockOnSave).not.toHaveBeenCalled();
    });

    it('shouldNotSubmitWhenFormIsLoadingInAdvanceOnlyMode', async () => {
      const mockForm = createMockRepoForm(VALID_PROXY_DATA, {
        blobStores: mockBlobStores, routingRules: mockRoutingRules,
        cleanupPolicies: mockCleanupPolicies, memberOptions: [],
      });
      mockForm.isLoading = true;
      mockUseRepositoryForm.mockReturnValue({ form: mockForm, repository: null, isCreate: true } as any);

      const submitRef = { current: null } as React.MutableRefObject<(() => void) | null>;

      renderWithTheme(
        <RepositoryForm
          recipe={mockProxyRecipe}
          isCreate={true}
          onSave={mockOnSave}
          onCancel={mockOnCancel}
          hideActions
          onSubmitRef={submitRef}
          advanceOnly={true}
        />
      );

      // The ref is populated but the guard inside prevents action when loading
      await waitFor(() => {
        expect(submitRef.current).not.toBeNull();
      });

      submitRef.current!();

      // Neither onSave nor form.send should be called when form is loading
      expect(mockOnSave).not.toHaveBeenCalled();
      expect(mockForm.send).not.toHaveBeenCalled();
    });
  });
});

