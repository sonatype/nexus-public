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

import { useEffect, useLayoutEffect, useRef } from 'react';

// Extend Window interface to include the dirty array used by UI-Router
declare global {
  interface Window {
    dirty?: string[];
  }
}

// Initialize window.dirty immediately (not in an effect)
if (typeof window !== 'undefined' && !window.dirty) {
  window.dirty = [];
}

/**
 * Hook to warn users about unsaved changes when navigating away.
 *
 * Handles:
 * - Browser refresh/close (beforeunload event)
 * - UI-Router navigation (via window.dirty array checked by router)
 *
 * @param isDirty - Whether the form has unsaved changes
 * @param formId - Unique identifier for this form (used to track multiple forms)
 *
 * @example
 * ```tsx
 * function MyForm() {
 *   const [formData, setFormData] = useState(initialData);
 *   const isDirty = formData !== initialData;
 *
 *   useUnsavedChangesWarning(isDirty, 'my-form');
 *
 *   return <form>...</form>;
 * }
 * ```
 *
 * @example
 * ```tsx
 * // With react-hook-form
 * function MyForm() {
 *   const { formState: { isDirty } } = useForm();
 *
 *   useUnsavedChangesWarning(isDirty, 'settings-form');
 *
 *   return <form>...</form>;
 * }
 * ```
 */
export function useUnsavedChangesWarning(isDirty: boolean, formId: string): void {
  // Track if we've registered this form
  const isRegisteredRef = useRef(false);

  // Use useLayoutEffect for synchronous updates to window.dirty
  // This ensures the dirty state is set BEFORE any click handlers or
  // router transitions can check it
  useLayoutEffect(() => {
    if (typeof window === 'undefined') return;

    // Initialize if needed
    if (!window.dirty) {
      window.dirty = [];
    }

    if (isDirty) {
      // Register this form as dirty
      if (!window.dirty.includes(formId)) {
        window.dirty.push(formId);
        console.debug(`[UnsavedChanges] Added "${formId}" to dirty list:`, window.dirty);
      }
      isRegisteredRef.current = true;
    } else if (isRegisteredRef.current) {
      // Unregister this form
      const index = window.dirty.indexOf(formId);
      if (index > -1) {
        window.dirty.splice(index, 1);
        console.debug(`[UnsavedChanges] Removed "${formId}" from dirty list:`, window.dirty);
      }
      isRegisteredRef.current = false;
    }
  }, [isDirty, formId]);

  // Handle beforeunload event (browser refresh/close).
  // Checks window.dirty (not the closure isDirty) so that clearDirtyState()
  // can suppress the dialog for programmatic reloads after save.
  useEffect(() => {
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      if (window.dirty && window.dirty.includes(formId)) {
        event.preventDefault();
        event.returnValue = '';
        return '';
      }
    };

    if (isDirty) {
      window.addEventListener('beforeunload', handleBeforeUnload);
    }

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, [isDirty, formId]);

  // Cleanup on unmount - remove this form from the dirty list.
  // Safe because the router's onBefore hook checks window.dirty and awaits
  // user confirmation BEFORE the transition completes and the component unmounts.
  // By the time this cleanup fires, the router has already read window.dirty.
  useEffect(() => {
    return () => {
      if (typeof window !== 'undefined' && window.dirty) {
        const index = window.dirty.indexOf(formId);
        if (index > -1) {
          window.dirty.splice(index, 1);
        }
      }
      isRegisteredRef.current = false;
    };
  }, [formId]);
}

/**
 * Programmatically clear dirty state for a form.
 * Call this after a successful save to prevent the warning from showing.
 *
 * @param formId - The form ID to clear
 *
 * @example
 * ```tsx
 * const handleSave = async () => {
 *   await saveData();
 *   clearDirtyState('my-form');
 * };
 * ```
 */
export function clearDirtyState(formId: string): void {
  if (typeof window !== 'undefined' && window.dirty) {
    const index = window.dirty.indexOf(formId);
    if (index > -1) {
      window.dirty.splice(index, 1);
      console.debug(`[UnsavedChanges] Cleared "${formId}" from dirty list:`, window.dirty);
    }
  }
}

/**
 * Check if any forms have unsaved changes.
 *
 * @returns true if any form is dirty
 */
export function hasUnsavedChanges(): boolean {
  return typeof window !== 'undefined' && window.dirty && window.dirty.length > 0;
}

export default useUnsavedChangesWarning;
