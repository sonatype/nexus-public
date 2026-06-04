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

import { useState, useCallback, useEffect, useRef } from 'react';
import { NEXUS_SESSION_EXPIRED_EVENT } from '../../../../interface/api/sessionExpiryBroadcast';

interface SessionExpiryState {
  isExpired: boolean;
  message: string;
}

export interface UseSessionExpiryReturn {
  /** Whether the session has expired */
  isExpired: boolean;
  /** Message to display in the modal */
  message: string;
  /** Show the session expiry modal */
  showExpiryModal: (message?: string) => void;
  /** Hide the session expiry modal */
  hideExpiryModal: () => void;
  /** Check if an error is a session expiry error */
  checkSessionError: (error: unknown) => boolean;
}

/**
 * Hook for managing session expiry state.
 *
 * Usage:
 * ```tsx
 * const { isExpired, message, showExpiryModal, hideExpiryModal } = useSessionExpiry();
 *
 * return (
 *   <>
 *     <YourApp />
 *     <SessionExpiryModal isOpen={isExpired} onClose={hideExpiryModal} message={message} />
 *   </>
 * );
 * ```
 */
export function useSessionExpiry(): UseSessionExpiryReturn {
  const [state, setState] = useState<SessionExpiryState>({
    isExpired: false,
    message: 'Your session has expired. Please sign in again to continue.',
  });

  // Ref to track if we've already shown the modal (to prevent multiple popups)
  const hasShownRef = useRef(false);

  const showExpiryModal = useCallback((message?: string) => {
    if (!hasShownRef.current) {
      hasShownRef.current = true;
      setState({
        isExpired: true,
        message: message || 'Your session has expired. Please sign in again to continue.',
      });
    }
  }, []);

  const hideExpiryModal = useCallback(() => {
    setState((prev) => ({ ...prev, isExpired: false }));
    // Reset the ref when modal is closed so we can show it again if needed
    hasShownRef.current = false;
  }, []);

  const checkSessionError = useCallback((error: unknown): boolean => {
    // Check if error is a 401 Unauthorized
    if (error && typeof error === 'object') {
      const err = error as { status?: number; response?: { status?: number } };
      const status = err.status || err.response?.status;

      if (status === 401) {
        showExpiryModal();
        return true;
      }
    }
    return false;
  }, [showExpiryModal]);

  // Axios 401 → CustomEvent from rest-client interceptors
  useEffect(() => {
    const handleRestUnauthorized = () => {
      showExpiryModal();
    };
    window.addEventListener(NEXUS_SESSION_EXPIRED_EVENT, handleRestUnauthorized);
    return () => window.removeEventListener(NEXUS_SESSION_EXPIRED_EVENT, handleRestUnauthorized);
  }, [showExpiryModal]);

  // Optional: uncaught errors that carry 401-shaped payloads
  useEffect(() => {
    const handleUnauthorized = (event: ErrorEvent) => {
      checkSessionError(event.error);
    };

    window.addEventListener('error', handleUnauthorized);
    return () => window.removeEventListener('error', handleUnauthorized);
  }, [checkSessionError]);

  return {
    isExpired: state.isExpired,
    message: state.message,
    showExpiryModal,
    hideExpiryModal,
    checkSessionError,
  };
}

export default useSessionExpiry;
