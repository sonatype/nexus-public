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

import React, { createContext, useContext, useState, useCallback, useRef, useMemo } from 'react';
import * as ToastPrimitive from '@radix-ui/react-toast';
import { CheckCircle2, XCircle, AlertCircle, Info, X } from 'lucide-react';
import './Toast.scss';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastMessage {
  id: string;
  type: ToastType;
  title: string;
  description?: string;
  duration?: number;
}

interface ToastContextValue {
  showToast: (type: ToastType, title: string, description?: string, duration?: number) => void;
  success: (title: string, description?: string) => void;
  error: (title: string, description?: string) => void;
  warning: (title: string, description?: string) => void;
  info: (title: string, description?: string) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

const TOAST_ICONS: Record<ToastType, React.ReactNode> = {
  success: <CheckCircle2 size={20} />,
  error: <XCircle size={20} />,
  warning: <AlertCircle size={20} />,
  info: <Info size={20} />,
};

const DEFAULT_DURATIONS: Record<ToastType, number> = {
  success: 4000,
  error: 6000,
  warning: 5000,
  info: 4000,
};

/**
 * Toast component that renders a single toast message
 */
const TOAST_INLINE_STYLES: Record<ToastType, React.CSSProperties> = {
  success: { backgroundColor: '#00bb6c', border: '1px solid #009955', color: '#fff' },
  error: { backgroundColor: '#dc2626', border: '1px solid #b91c1c', color: '#fff' },
  warning: { backgroundColor: '#f59e0b', border: '1px solid #d97706', color: '#1c1917' },
  info: { backgroundColor: '#2563eb', border: '1px solid #1d4ed8', color: '#fff' },
};

function Toast({ toast, onOpenChange }: { toast: ToastMessage; onOpenChange: (open: boolean) => void }) {
  const textColor = toast.type === 'warning' ? '#1c1917' : '#ffffff';

  return (
    <ToastPrimitive.Root
      className={`toast toast--${toast.type}`}
      style={TOAST_INLINE_STYLES[toast.type]}
      open={true}
      onOpenChange={onOpenChange}
      duration={toast.duration || DEFAULT_DURATIONS[toast.type]}
      data-testid={`toast-${toast.type}`}
    >
      <div className="toast__icon" style={{ color: textColor }}>{TOAST_ICONS[toast.type]}</div>
      <div className="toast__content">
        <ToastPrimitive.Title className="toast__title" style={{ color: textColor }}>{toast.title}</ToastPrimitive.Title>
        {toast.description && (
          <ToastPrimitive.Description className="toast__description">
            {toast.description}
          </ToastPrimitive.Description>
        )}
      </div>
      <ToastPrimitive.Close className="toast__close" aria-label="Close">
        <X size={16} />
      </ToastPrimitive.Close>
    </ToastPrimitive.Root>
  );
}

/**
 * ToastProvider - Wrap your app or page with this to enable toast notifications
 *
 * Usage:
 * ```tsx
 * <ToastProvider>
 *   <YourPage />
 * </ToastProvider>
 * ```
 *
 * Then in your component:
 * ```tsx
 * const { success, error } = useToast();
 * success('Saved successfully!');
 * error('Failed to save', 'Please check your connection');
 * ```
 */
export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);
  const toastIdRef = useRef(0);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const showToast = useCallback(
    (type: ToastType, title: string, description?: string, duration?: number) => {
      const id = `toast-${++toastIdRef.current}`;
      const newToast: ToastMessage = { id, type, title, description, duration };
      setToasts((prev) => [...prev, newToast]);
    },
    []
  );

  const success = useCallback(
    (title: string, description?: string) => showToast('success', title, description),
    [showToast]
  );

  const error = useCallback(
    (title: string, description?: string) => showToast('error', title, description),
    [showToast]
  );

  const warning = useCallback(
    (title: string, description?: string) => showToast('warning', title, description),
    [showToast]
  );

  const info = useCallback(
    (title: string, description?: string) => showToast('info', title, description),
    [showToast]
  );

  const contextValue = useMemo<ToastContextValue>(() => ({
    showToast,
    success,
    error,
    warning,
    info,
  }), [showToast, success, error, warning, info]);

  // Register on window SYNCHRONOUSLY for cross-module access (adapter useToast bridge)
  // Must be synchronous (not useEffect) so it's available before child components render
  if (typeof window !== 'undefined') {
    (window as any).__nexusToast = contextValue;
  }

  return (
    <ToastContext.Provider value={contextValue}>
      <ToastPrimitive.Provider swipeDirection="right">
        {children}
        {toasts.map((toast) => (
          <Toast
            key={toast.id}
            toast={toast}
            onOpenChange={(open) => {
              if (!open) removeToast(toast.id);
            }}
          />
        ))}
        <ToastPrimitive.Viewport className="toast-viewport" />
      </ToastPrimitive.Provider>
    </ToastContext.Provider>
  );
}

/**
 * Hook to access toast functions
 *
 * @returns Toast context with success, error, warning, info methods
 * @throws Error if used outside ToastProvider
 *
 * @example
 * ```tsx
 * const { success, error } = useToast();
 *
 * const handleSave = async () => {
 *   try {
 *     await save();
 *     success('Settings saved successfully');
 *   } catch (err) {
 *     error('Failed to save settings', err.message);
 *   }
 * };
 * ```
 */
// No-op fallback when ToastProvider is not in the tree (e.g., cloud bundle
// renders Preview UI components without the coreui App.jsx wrapper).
const noopToast: ToastContextValue = {
  showToast: () => {},
  success: () => {},
  error: () => {},
  warning: () => {},
  info: () => {},
};

export function useToast(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) {
    // Check window-level toast bridge (used by cross-module adapters)
    if (typeof window !== 'undefined' && (window as any).__nexusToast) {
      return (window as any).__nexusToast;
    }
    // Return no-op instead of crashing -- toast is non-critical UI feedback
    return noopToast;
  }
  return context;
}

export default ToastProvider;
