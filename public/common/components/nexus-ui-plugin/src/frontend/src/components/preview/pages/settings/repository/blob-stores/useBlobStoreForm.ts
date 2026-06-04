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

import { useMemo } from 'react';
import { useForm } from '../../../../../../interface/form';
import { useToast } from '../../../../shared';
import { createBlobStoreFormMachine, BlobStoreFormData } from './blobStoreFormMachine';
import type { BlobStoreTypeDescriptor as BlobStoreTypeInfo, QuotaType } from './types';

export interface UseBlobStoreFormOptions {
  /** Name of the blob store to edit (undefined for create mode). */
  blobStoreName?: string;
  /** Type ID of the blob store (for edit mode URL construction). */
  blobStoreType?: string;
  /** Callback to save/create a blob store. */
  saveBlobStore: (data: BlobStoreFormData) => Promise<void>;
  /** Callback to update an existing blob store. */
  updateBlobStore: (data: BlobStoreFormData) => Promise<void>;
  /** Callback to delete a blob store. */
  deleteBlobStore?: (name: string) => Promise<void>;
  /** Callback when form is cancelled / navigation back. */
  onCancel: () => void;
}

export interface UseBlobStoreFormReturn {
  /** Form state and helpers from useForm(). */
  form: ReturnType<typeof useForm>;
  /** Whether this is a create (new) form. */
  isCreate: boolean;
  /** Loaded blob store types for the Type dropdown. */
  blobStoreTypes: BlobStoreTypeInfo[];
  /** Loaded quota types for the Soft Quota dropdown. */
  quotaTypes: QuotaType[];
  /** Blob store usage counts (repositories / child blob stores referencing this one). */
  usage: { blobStoreUsage: number; repositoryUsage: number };
}

/**
 * Custom hook for managing the Blob Store form state and logic.
 *
 * Uses XState form machine for state management with automatic dirty tracking
 * and unsaved changes warnings. The machine loads reference data (types, quota
 * types) and, in edit mode, the existing blob store configuration.
 *
 * The save service is injected via options so the caller controls persistence.
 *
 * @example
 * ```tsx
 * const { form, isCreate, blobStoreTypes, quotaTypes, usage } = useBlobStoreForm({
 *   blobStoreName: params.name,
 *   blobStoreType: params.type,
 *   saveBlobStore,
 *   updateBlobStore,
 *   deleteBlobStore,
 *   onCancel: handleBack,
 * });
 * ```
 */
export function useBlobStoreForm({
  blobStoreName,
  blobStoreType,
  saveBlobStore,
  updateBlobStore,
  deleteBlobStore,
  onCancel,
}: UseBlobStoreFormOptions): UseBlobStoreFormReturn {
  const toast = useToast();
  const isCreate = !blobStoreName;

  // Create the form machine - memoized based on blobStoreName and blobStoreType
  const machine = useMemo(
    () => createBlobStoreFormMachine(blobStoreName, blobStoreType),
    [blobStoreName, blobStoreType]
  );

  // Use the form machine with action/service overrides
  const form = useForm(machine, {
    actions: {
      onCancel: onCancel,
    },
    services: {
      save: async (ctx: { data: BlobStoreFormData }) => {
        try {
          if (isCreate) {
            await saveBlobStore(ctx.data);
            toast.success(`Blob store "${ctx.data.name}" created successfully`);
          } else {
            await updateBlobStore(ctx.data);
            toast.success(`Blob store "${ctx.data.name}" updated successfully`);
          }
          onCancel();
        } catch (err) {
          toast.error(err instanceof Error ? err.message : 'Operation failed');
          throw err;
        }
      },
      ...(deleteBlobStore && {
        delete: async (ctx: { data: BlobStoreFormData }) => {
          await deleteBlobStore(ctx.data.name);
          toast.success(`Blob store "${ctx.data.name}" deleted successfully`);
        },
      }),
    },
  });

  // Access extended context for reference data
  const context = (form.state as {
    context: {
      blobStoreTypes: BlobStoreTypeInfo[];
      quotaTypes: QuotaType[];
      usage: { blobStoreUsage: number; repositoryUsage: number };
    };
  }).context;

  return {
    form,
    isCreate,
    blobStoreTypes: context.blobStoreTypes || [],
    quotaTypes: context.quotaTypes || [],
    usage: context.usage || { blobStoreUsage: 0, repositoryUsage: 0 },
  };
}
