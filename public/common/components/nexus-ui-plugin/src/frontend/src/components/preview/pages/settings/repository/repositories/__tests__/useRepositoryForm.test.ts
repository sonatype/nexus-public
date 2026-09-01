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

import { renderHook, act } from '@testing-library/react';

import { useRepositoryForm } from '../useRepositoryForm';

// Mock dependencies
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

const mockToastSuccess = jest.fn();
const mockToastError = jest.fn();

// Mock useForm to isolate hook logic from XState machine internals
const mockSend = jest.fn();
const mockFormData = {
  name: 'test-repo',
  type: 'proxy',
  format: 'maven2',
  recipe: 'maven2-proxy',
  online: true,
  storage: { blobStoreName: 'default', strictContentTypeValidation: true },
  proxy: { remoteUrl: 'https://repo1.maven.org/maven2/', contentMaxAge: 1440, metadataMaxAge: 1440 },
};

const mockFormReturn = {
  data: mockFormData,
  isPristine: true,
  isSaving: false,
  isLoading: false,
  isDeleting: false,
  saveError: null,
  validationErrors: {},
  field: jest.fn((name: string) => ({
    name,
    value: (mockFormData as any)[name] ?? '',
    onChange: jest.fn(),
    onBlur: jest.fn(),
    error: undefined,
  })),
  state: {
    matches: jest.fn(() => false),
    context: {
      data: mockFormData,
      pristineData: mockFormData,
      blobStores: [{ name: 'default' }],
      routingRules: [{ id: '1', name: 'block-snapshots' }],
      cleanupPolicies: [{ name: 'maven-cleanup', format: 'maven2' }],
      memberRepositories: [{ id: 'central', name: 'central', format: 'maven2', type: 'proxy' }],
      repository: null,
    },
  },
  send: mockSend,
  submit: jest.fn(),
  reset: jest.fn(),
};

jest.mock('../../../../../../../interface/form', () => ({
  useForm: () => mockFormReturn,
  createFormMachine: jest.fn(),
}));

jest.mock('../repositoryFormMachine', () => ({
  createRepositoryFormMachine: jest.fn(() => ({ id: 'test-machine' })),
  validateRepository: jest.fn((data: any) => {
    const errors: Record<string, string> = {};
    if (!data.name?.trim()) errors.name = 'Name is required';
    if (data.type === 'proxy' && !data.proxy?.remoteUrl?.trim()) {
      errors['proxy.remoteUrl'] = 'Remote URL is required';
    }
    return errors;
  }),
}));

// Mock shared hooks
const mockUseIsCloud = jest.fn(() => false);
const mockUseHasFirewallLicense = jest.fn(() => true);

jest.mock('../../../../../shared', () => ({
  useToast: () => ({ success: mockToastSuccess, error: mockToastError }),
  useIsCloud: () => mockUseIsCloud(),
  useHasFirewallLicense: () => mockUseHasFirewallLicense(),
}));

// useRepositoryForm uses UIRouter's useCurrentStateAndParams to read the
// `?tab=` query param from the URL. The renderHook test harness doesn't mount a
// <UIRouter> wrapper, so stub the hook to return empty params.
jest.mock('@uirouter/react', () => ({
  useCurrentStateAndParams: () => ({ state: null, params: {} }),
}));

const DEFAULT_OPTIONS = {
  format: 'maven2',
  repositoryType: 'proxy' as const,
  onCancel: jest.fn(),
  onSave: jest.fn().mockResolvedValue(undefined),
};

describe('useRepositoryForm', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('derived state', () => {
    it('shouldExposeIsCloudFromExtJSState', () => {
      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      expect(result.current.isCloud).toBe(false);
    });

    it('shouldExposeHasFirewallLicenseFromExtJSState', () => {
      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      expect(result.current.hasFirewallLicense).toBe(true);
    });

    it('shouldExposeFormDataFromMachineContext', () => {
      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      expect(result.current.formData).toEqual(mockFormData);
    });

    it('shouldExposeBlobStoresFromMachineContext', () => {
      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      expect(result.current.blobStores).toEqual([{ name: 'default' }]);
    });

    it('shouldExposeRoutingRulesFromMachineContext', () => {
      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      expect(result.current.routingRules).toEqual([{ id: '1', name: 'block-snapshots' }]);
    });

    it('shouldExposeCleanupPoliciesFromMachineContext', () => {
      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      expect(result.current.cleanupPolicies).toEqual([{ name: 'maven-cleanup', format: 'maven2' }]);
    });

    it('shouldExposeMemberOptionsFromMachineContext', () => {
      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      expect(result.current.memberRepositories).toEqual([
        { id: 'central', name: 'central', format: 'maven2', type: 'proxy' },
      ]);
    });

    it('shouldDetermineIsCreateWhenNoRepositoryName', () => {
      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      expect(result.current.isCreate).toBe(true);
    });

    it('shouldDetermineIsCreateFalseWhenRepositoryNameProvided', () => {
      const { result } = renderHook(() =>
        useRepositoryForm({ ...DEFAULT_OPTIONS, repositoryName: 'existing-repo' })
      );

      expect(result.current.isCreate).toBe(false);
    });
  });

  describe('UI state management', () => {
    it('shouldInitializeActiveTabToSummaryForCreateMode', () => {
      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      expect(result.current.activeTab).toBe('summary');
    });

    it('shouldInitializeActiveTabToSettingsForEditMode', () => {
      const { result } = renderHook(() =>
        useRepositoryForm({ ...DEFAULT_OPTIONS, repositoryName: 'existing-repo' })
      );

      expect(result.current.activeTab).toBe('settings');
    });

    it('shouldUpdateActiveTabWhenSetActiveTabCalled', () => {
      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      act(() => {
        result.current.setActiveTab('firewall');
      });

      expect(result.current.activeTab).toBe('firewall');
    });

    it('shouldInitializeOriginChangeWarningToFalse', () => {
      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      expect(result.current.originChangeWarning).toBe(false);
    });

    it('shouldUpdateOriginChangeWarningWhenSetterCalled', () => {
      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      act(() => {
        result.current.setOriginChangeWarning(true);
      });

      expect(result.current.originChangeWarning).toBe(true);
    });
  });

  describe('bridge functions', () => {
    it('shouldDispatchUpdateEventForEachFieldInHandleChange', () => {
      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      act(() => {
        result.current.handleChange({ online: false, name: 'new-name' });
      });

      expect(mockSend).toHaveBeenCalledWith({ type: 'UPDATE', name: 'online', value: false });
      expect(mockSend).toHaveBeenCalledWith({ type: 'UPDATE', name: 'name', value: 'new-name' });
    });

    it('shouldMergeNestedUpdatesInHandleNestedChange', () => {
      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      act(() => {
        result.current.handleNestedChange('proxy', { contentMaxAge: 720 });
      });

      expect(mockSend).toHaveBeenCalledWith({
        type: 'UPDATE',
        name: 'proxy',
        value: { remoteUrl: 'https://repo1.maven.org/maven2/', contentMaxAge: 720, metadataMaxAge: 1440 },
      });
    });
  });

  describe('wizard integration', () => {
    it('shouldPopulateOnSubmitRefWhenProvided', () => {
      const submitRef = { current: null } as React.MutableRefObject<(() => void) | null>;

      renderHook(() =>
        useRepositoryForm({ ...DEFAULT_OPTIONS, onSubmitRef: submitRef })
      );

      expect(submitRef.current).not.toBeNull();
      expect(typeof submitRef.current).toBe('function');
    });

    it('shouldCleanupOnSubmitRefOnUnmount', () => {
      const submitRef = { current: null } as React.MutableRefObject<(() => void) | null>;

      const { unmount } = renderHook(() =>
        useRepositoryForm({ ...DEFAULT_OPTIONS, onSubmitRef: submitRef })
      );

      expect(submitRef.current).not.toBeNull();

      unmount();

      expect(submitRef.current).toBeNull();
    });

    it('shouldCallOnSaveDirectlyInAdvanceOnlyModeWhenFormIsValid', () => {
      const submitRef = { current: null } as React.MutableRefObject<(() => void) | null>;
      const onSave = jest.fn().mockResolvedValue(undefined);

      renderHook(() =>
        useRepositoryForm({
          ...DEFAULT_OPTIONS,
          onSubmitRef: submitRef,
          advanceOnly: true,
          onSave,
        })
      );

      act(() => {
        submitRef.current!();
      });

      expect(onSave).toHaveBeenCalledWith(mockFormData);
      expect(mockSend).not.toHaveBeenCalledWith('SUBMIT');
    });

    it('shouldSendSubmitToMachineInAdvanceOnlyModeWhenFormIsInvalid', () => {
      const submitRef = { current: null } as React.MutableRefObject<(() => void) | null>;
      const onSave = jest.fn().mockResolvedValue(undefined);

      // Override mockFormReturn data to simulate invalid form
      const originalData = mockFormReturn.data;
      mockFormReturn.data = {
        ...mockFormData,
        proxy: { remoteUrl: '', contentMaxAge: 1440, metadataMaxAge: 1440 },
      } as any;

      renderHook(() =>
        useRepositoryForm({
          ...DEFAULT_OPTIONS,
          onSubmitRef: submitRef,
          advanceOnly: true,
          onSave,
        })
      );

      act(() => {
        submitRef.current!();
      });

      expect(onSave).not.toHaveBeenCalled();
      expect(mockSend).toHaveBeenCalledWith('SUBMIT');

      // Restore
      mockFormReturn.data = originalData;
    });

    it('shouldNotSubmitWhenFormIsLoading', () => {
      const submitRef = { current: null } as React.MutableRefObject<(() => void) | null>;
      const onSave = jest.fn().mockResolvedValue(undefined);

      mockFormReturn.isLoading = true;

      renderHook(() =>
        useRepositoryForm({
          ...DEFAULT_OPTIONS,
          onSubmitRef: submitRef,
          advanceOnly: true,
          onSave,
        })
      );

      act(() => {
        submitRef.current!();
      });

      expect(onSave).not.toHaveBeenCalled();
      expect(mockSend).not.toHaveBeenCalled();

      mockFormReturn.isLoading = false;
    });

    it('shouldSendSubmitToMachineWhenNotAdvanceOnly', () => {
      const submitRef = { current: null } as React.MutableRefObject<(() => void) | null>;

      renderHook(() =>
        useRepositoryForm({
          ...DEFAULT_OPTIONS,
          onSubmitRef: submitRef,
          advanceOnly: false,
        })
      );

      act(() => {
        submitRef.current!();
      });

      expect(mockSend).toHaveBeenCalledWith('SUBMIT');
    });
  });

  describe('shared hook integration', () => {
    it('shouldReturnFalseForHasFirewallLicenseWhenHookReturnsFalse', () => {
      mockUseHasFirewallLicense.mockReturnValueOnce(false);

      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      expect(result.current.hasFirewallLicense).toBe(false);
    });

    it('shouldReturnFalseForIsCloudWhenHookReturnsFalse', () => {
      mockUseIsCloud.mockReturnValueOnce(false);

      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      expect(result.current.isCloud).toBe(false);
    });

    it('shouldReturnTrueForIsCloudWhenHookReturnsTrue', () => {
      mockUseIsCloud.mockReturnValueOnce(true);

      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      expect(result.current.isCloud).toBe(true);
    });
  });

  describe('onCanAdvanceChange effect', () => {
    it('shouldReportFalseWhenFormIsLoading', () => {
      const onCanAdvanceChange = jest.fn();
      mockFormReturn.isLoading = true;

      renderHook(() =>
        useRepositoryForm({ ...DEFAULT_OPTIONS, onCanAdvanceChange })
      );

      expect(onCanAdvanceChange).toHaveBeenCalledWith(false);

      mockFormReturn.isLoading = false;
    });

    it('shouldReportTrueWhenFormDataIsValid', () => {
      const onCanAdvanceChange = jest.fn();

      renderHook(() =>
        useRepositoryForm({ ...DEFAULT_OPTIONS, onCanAdvanceChange })
      );

      expect(onCanAdvanceChange).toHaveBeenCalledWith(true);
    });

    it('shouldReportFalseWhenFormDataIsInvalid', () => {
      const onCanAdvanceChange = jest.fn();
      const originalData = mockFormReturn.data;
      mockFormReturn.data = { ...mockFormData, name: '' } as any;

      renderHook(() =>
        useRepositoryForm({ ...DEFAULT_OPTIONS, onCanAdvanceChange })
      );

      expect(onCanAdvanceChange).toHaveBeenCalledWith(false);

      mockFormReturn.data = originalData;
    });

    it('shouldNotCallOnCanAdvanceChangeWhenNotProvided', () => {
      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      expect(result.current).toBeDefined();
    });
  });

  describe('onDirtyChange effect (NEXUS-54349)', () => {
    it('shouldReportFalseWhenFormIsPristine', () => {
      const onDirtyChange = jest.fn();
      mockFormReturn.isPristine = true;

      renderHook(() =>
        useRepositoryForm({ ...DEFAULT_OPTIONS, onDirtyChange })
      );

      expect(onDirtyChange).toHaveBeenCalledWith(false);
    });

    it('shouldReportTrueWhenFormIsDirty', () => {
      const onDirtyChange = jest.fn();
      mockFormReturn.isPristine = false;

      renderHook(() =>
        useRepositoryForm({ ...DEFAULT_OPTIONS, onDirtyChange })
      );

      expect(onDirtyChange).toHaveBeenCalledWith(true);

      mockFormReturn.isPristine = true;
    });

    it('shouldNotCallOnDirtyChangeWhenNotProvided', () => {
      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      expect(result.current).toBeDefined();
    });
  });

  describe('handleNestedChange with undefined key', () => {
    it('shouldFallbackToEmptyObjectWhenNestedKeyIsUndefined', () => {
      const originalData = mockFormReturn.data;
      mockFormReturn.data = { ...mockFormData, cleanup: undefined } as any;

      const { result } = renderHook(() => useRepositoryForm(DEFAULT_OPTIONS));

      act(() => {
        result.current.handleNestedChange('cleanup' as any, { policyNames: ['maven-cleanup'] } as any);
      });

      expect(mockSend).toHaveBeenCalledWith({
        type: 'UPDATE',
        name: 'cleanup',
        value: { policyNames: ['maven-cleanup'] },
      });

      mockFormReturn.data = originalData;
    });
  });
});
