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

import React, { useState, useCallback, useMemo, useEffect, useRef } from 'react';
import { Box } from '@radix-ui/themes';
import { WizardForm } from '../../../../shared/form';
import { BlobStoreTypeSelector, type BlobStoreTypeId } from './BlobStoreTypeSelector';
import { BlobStoreWizardStepBasic } from './BlobStoreWizardStepBasic';
import { BlobStoreWizardStepCredentials } from './BlobStoreWizardStepCredentials';
import { BlobStoreWizardStepAdvanced } from './BlobStoreWizardStepAdvanced';
import { useBlobStore, useBlobStoreTypes } from './useBlobStores';
import { clearDirtyState, useToast, PageHeader } from '../../../../shared';
import type { BlobStoreFormData } from './types';
import { BLOB_STORE_TYPE_IDS } from './blobStoreFormMachine';

const BASE_PATH = 'preview/admin/repository/blobstores';

const WIZARD_STEPS = [
  { id: 'type', label: 'Select Type' },
  { id: 'basic', label: 'Basic Config' },
  { id: 'credentials', label: 'Credentials' },
  { id: 'advanced', label: 'Advanced' },
];

function navigateTo(path: string) {
  window.location.hash = path;
}

function buildDefaultsForType(type: string): Partial<BlobStoreFormData> {
  const t = type.toLowerCase();
  const base: Partial<BlobStoreFormData> = {
    name: '',
    type,
    softQuota: { enabled: false },
    path: '',
    bucketConfiguration: {},
    members: [],
    fillPolicy: '',
  };

  if (t === BLOB_STORE_TYPE_IDS.S3) {
    base.bucketConfiguration = {
      bucket: { region: 'DEFAULT', name: '', prefix: '' },
      bucketSecurity: {},
      encryption: null,
      advancedBucketConnection: {},
      failoverBuckets: [],
    };
  } else if (t === BLOB_STORE_TYPE_IDS.AZURE) {
    base.bucketConfiguration = {
      accountName: '',
      containerName: '',
      authentication: { authenticationMethod: 'ENVIRONMENTVARIABLE' },
    };
  } else if (t === BLOB_STORE_TYPE_IDS.GOOGLE) {
    base.bucketConfiguration = {
      bucket: { name: '', prefix: '' },
      bucketSecurity: { authenticationMethod: 'applicationDefault' },
      encryption: { encryptionType: 'default' },
    };
  }
  return base;
}

export interface BlobStoreWizardCreateProps {
  onBack: () => void;
}

export function BlobStoreWizardCreate({ onBack }: BlobStoreWizardCreateProps) {
  const [step, setStep] = useState(0);
  const [selectedType, setSelectedType] = useState<BlobStoreTypeId | null>(null);
  const [formData, setFormData] = useState<BlobStoreFormData>({
    name: '',
    softQuota: { enabled: false },
  });
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const pathTouched = useRef(false);
  const touchedFields = useRef<Set<string>>(new Set());

  const toast = useToast();
  const { save } = useBlobStore(undefined, selectedType || undefined);
  const { types, quotaTypes, loading: typesLoading } = useBlobStoreTypes();

  const updateField = useCallback((field: string, value: unknown) => {
    touchedFields.current.add(field);
    setFormData((prev) => {
      const next = { ...prev, [field]: value };
      if (field === 'name' && !pathTouched.current) {
        const fileType = types.find((t) => t.id.toLowerCase() === BLOB_STORE_TYPE_IDS.FILE);
        const pathField = fileType?.fields?.find((f) => f.id === 'path');
        const template = pathField?.attributes?.tokenReplacement;
        if (template && selectedType?.toLowerCase() === BLOB_STORE_TYPE_IDS.FILE) {
          next.path = template.replace(/\$\{name\}/g, value as string);
        }
      }
      return next;
    });
    setValidationErrors((prev) => {
      const next = { ...prev };
      delete next[field];
      Object.keys(next).forEach((k) => {
        if (k.startsWith(`${field}.`)) delete next[k];
      });
      return next;
    });
  }, [types, selectedType]);

  const updateNested = useCallback((path: string, value: unknown) => {
    touchedFields.current.add(path);
    if (path === 'path') {
      pathTouched.current = true;
    }
    setFormData((prev) => {
      const parts = path.split('.');
      const result = { ...prev };
      let cur: Record<string, unknown> = result;
      for (let i = 0; i < parts.length - 1; i++) {
        const p = parts[i];
        cur[p] = { ...((cur[p] as Record<string, unknown>) || {}) };
        cur = cur[p] as Record<string, unknown>;
      }
      cur[parts[parts.length - 1]] = value;
      return result;
    });
    setValidationErrors((prev) => {
      const next = { ...prev };
      delete next[path];
      return next;
    });
  }, []);

  const handleTypeSelect = useCallback((type: BlobStoreTypeId) => {
    setSelectedType(type);
    pathTouched.current = false;
    touchedFields.current = new Set();
    const defaults = buildDefaultsForType(type);
    setFormData((prev) => ({ ...prev, ...defaults, type }));
    setValidationErrors({});
  }, []);

  const validateStep1Pure = useCallback(
    (d: BlobStoreFormData, t: BlobStoreTypeId | null): Record<string, string> => {
      const err: Record<string, string> = {};
      if (!d.name?.trim()) err.name = 'Name is required';
      else if (!/^[a-zA-Z0-9_-]+$/.test(d.name)) {
        err.name = 'Name can only contain letters, numbers, underscores, and hyphens';
      }
      const tl = t?.toLowerCase();
      if (tl === BLOB_STORE_TYPE_IDS.FILE) {
        const path = d.path?.trim() ?? '';
        if (!path) {
          err.path = 'Path is required';
        } else if (path === '/' || path === '\\') {
          err.path = 'Path cannot be the root directory';
        } else if (/\.\./.test(path)) {
          err.path = 'Path cannot contain ".." (parent directory references)';
        } else if (/^[/\\]$/.test(path) || /^[A-Z]:[/\\]?$/i.test(path)) {
          err.path = 'Path must specify a directory name, not just a drive or root';
        } else if (/[*?"<>|]/.test(path) || path.includes('\0')) {
          err.path = 'Path cannot contain invalid characters (* ? " < > |)';
        } else {
          const segments = path.split(/[/\\]+/).filter(Boolean);
          const tooLong = segments.find((s) => s.length > 255);
          if (tooLong) {
            err.path =
              'Each folder name in the path must be 255 characters or fewer';
          }
        }
      }
      if (tl === BLOB_STORE_TYPE_IDS.S3) {
        const b = (d.bucketConfiguration as Record<string, unknown>)?.bucket as Record<string, string>;
        const bucketName = b?.name?.trim() ?? '';
        if (!bucketName) {
          err['bucketConfiguration.bucket.name'] = 'Bucket name is required';
        } else if (bucketName.length < 3 || bucketName.length > 63) {
          err['bucketConfiguration.bucket.name'] = 'Bucket name must be between 3 and 63 characters';
        }
      }
      if (tl === BLOB_STORE_TYPE_IDS.AZURE) {
        const c = d.bucketConfiguration as Record<string, string>;
        // Azure storage account names: 3-24 characters (per Azure naming rules).
        const accountName = c?.accountName?.trim() ?? '';
        if (!accountName) {
          err['bucketConfiguration.accountName'] = 'Account name is required';
        } else if (accountName.length < 3 || accountName.length > 24) {
          err['bucketConfiguration.accountName'] = 'Account name must be between 3 and 24 characters';
        }
        // Azure container names: 3-63 characters (per Azure naming rules).
        const containerName = c?.containerName?.trim() ?? '';
        if (!containerName) {
          err['bucketConfiguration.containerName'] = 'Container name is required';
        } else if (containerName.length < 3 || containerName.length > 63) {
          err['bucketConfiguration.containerName'] = 'Container name must be between 3 and 63 characters';
        }
      }
      if (tl === BLOB_STORE_TYPE_IDS.GOOGLE) {
        const b = (d.bucketConfiguration as Record<string, unknown>)?.bucket as Record<string, string>;
        const bucketName = b?.name?.trim() ?? '';
        if (!bucketName) {
          err['bucketConfiguration.bucket.name'] = 'Bucket name is required';
        } else if (bucketName.length < 3 || bucketName.length > 63) {
          err['bucketConfiguration.bucket.name'] = 'Bucket name must be between 3 and 63 characters';
        }
      }
      if (tl === BLOB_STORE_TYPE_IDS.GROUP) {
        if (!d.members?.length) err.members = 'At least one member is required';
        if (!d.fillPolicy?.trim()) err.fillPolicy = 'Fill policy is required';
      }
      return err;
    },
    []
  );

  const validateStep3Pure = useCallback((d: BlobStoreFormData): Record<string, string> => {
    const err: Record<string, string> = {};
    const sq = d.softQuota;
    if (sq?.enabled) {
      if (!sq.type) err['softQuota.type'] = 'Constraint type is required';
      const limit = sq.limit;
      if (limit === undefined || limit === null || String(limit) === '' || limit < 1) {
        err['softQuota.limit'] = 'Limit must be a positive number (MB)';
      }
    }
    return err;
  }, []);

  const runValidateStep1 = useCallback(() => {
    const err = validateStep1Pure(formData, selectedType);
    setValidationErrors(err);
    return Object.keys(err).length === 0;
  }, [formData, selectedType, validateStep1Pure]);

  const runValidateStep3 = useCallback(() => {
    const err = validateStep3Pure(formData);
    setValidationErrors((prev) => ({ ...prev, ...err }));
    return Object.keys(err).length === 0;
  }, [formData, validateStep3Pure]);

  // Surface Basic step validation errors inline as the user types touched fields
  useEffect(() => {
    if (step === 1) {
      const allErrors = validateStep1Pure(formData, selectedType);
      setValidationErrors((prev) => {
        const next = { ...prev };
        // Only show errors for fields the user has interacted with
        for (const [key, msg] of Object.entries(allErrors)) {
          if (touchedFields.current.has(key)) {
            next[key] = msg;
          }
        }
        // Clear errors for fields that are now valid
        for (const key of Object.keys(next)) {
          if (!(key in allErrors)) {
            delete next[key];
          }
        }
        return next;
      });
    }
  }, [step, formData, selectedType, validateStep1Pure]);

  // Surface Advanced step validation errors so user sees what to fix when button is disabled
  useEffect(() => {
    if (step === 3) {
      const err = validateStep3Pure(formData);
      setValidationErrors((prev) => ({ ...prev, ...err }));
    }
  }, [step, formData, validateStep3Pure]);

  const canAdvance = useMemo(() => {
    if (step === 0) return selectedType != null;
    if (step === 1) return Object.keys(validateStep1Pure(formData, selectedType)).length === 0;
    if (step === 2) return true;
    if (step === 3) return Object.keys(validateStep3Pure(formData)).length === 0;
    return true;
  }, [step, formData, selectedType, validateStep1Pure, validateStep3Pure]);

  const handleStepChange = useCallback(
    (newStep: number) => {
      if (newStep > step) {
        if (step === 1 && !runValidateStep1()) return;
        if (step === 3 && !runValidateStep3()) return;
      }
      setStep(newStep);
    },
    [step, runValidateStep1, runValidateStep3]
  );

  const handleComplete = useCallback(async () => {
    if (!runValidateStep1() || !runValidateStep3()) return;

    const payload: BlobStoreFormData = {
      ...formData,
      type: selectedType || formData.type || '',
    };
    if (!payload.type) {
      toast.error('Blob store type is required');
      return;
    }

    try {
      clearDirtyState('blob-store-wizard-create');
      await save(payload);
      toast.success(`Blob store "${payload.name}" created successfully`);
      navigateTo(BASE_PATH);
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Failed to create blob store');
      throw e;
    }
  }, [formData, selectedType, save, runValidateStep1, runValidateStep3, toast]);

  const handleCancel = useCallback(() => {
    navigateTo(BASE_PATH);
    onBack();
  }, [onBack]);

  if (typesLoading) {
    return (
      <Box p="8">
        <PageHeader
          breadcrumbs={[
            { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
            { label: 'Blob Stores', onClick: onBack },
            { label: 'Create' },
          ]}
        />
        <p>Loading blob store types...</p>
      </Box>
    );
  }

  const stepContent = (
    <>
      {step === 0 && (
        <BlobStoreTypeSelector
          selectedType={selectedType}
          onSelect={handleTypeSelect}
        />
      )}
      {step === 1 && selectedType && (
        <BlobStoreWizardStepBasic
          data={formData}
          selectedType={selectedType}
          onChange={updateNested}
          updateField={updateField}
          validationErrors={validationErrors}
        />
      )}
      {step === 2 && selectedType && (
        <BlobStoreWizardStepCredentials
          data={formData}
          selectedType={selectedType}
          onChange={updateNested}
        />
      )}
      {step === 3 && selectedType && (
        <BlobStoreWizardStepAdvanced
          data={formData}
          selectedType={selectedType}
          onChange={updateNested}
          updateField={updateField}
          quotaTypes={quotaTypes}
          validationErrors={validationErrors}
        />
      )}
    </>
  );

  return (
    <Box className="blob-store-wizard-create" data-testid="blob-store-wizard-create">
      <PageHeader
        breadcrumbs={[
          { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
          { label: 'Blob Stores', onClick: onBack },
          { label: 'Create' },
        ]}
      />
      <WizardForm
        steps={WIZARD_STEPS}
        currentStep={step}
        onStepChange={handleStepChange}
        onComplete={handleComplete}
        onCancel={handleCancel}
        completeLabel="Create Blob Store"
        title="Create Blob Store"
        description="Configure storage for your repositories"
        canAdvance={canAdvance}
        dirty={step > 0 || !!formData.name}
        noDirtyTracking={step < 1}
        testId="blob-store-wizard"
      >
        {stepContent}
      </WizardForm>
    </Box>
  );
}
