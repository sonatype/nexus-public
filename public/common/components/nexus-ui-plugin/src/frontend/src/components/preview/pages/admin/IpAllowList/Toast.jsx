/*
 * Sonatype Nexus (TM) Professional Version.
 * Copyright (c) 2008-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */

import React from 'react';
import * as Toast from '@radix-ui/react-toast';

/**
 * Toast notification component
 * Displays success/error/warning messages in a toast at top-right corner
 *
 * @param {boolean} open - Whether the toast is visible
 * @param {function} onOpenChange - Callback when visibility changes
 * @param {string} message - Message to display
 * @param {string} type - Toast type: 'success', 'error', or 'warning' (default: 'success')
 */
export function ToastNotification({ open, onOpenChange, message, type = 'success' }) {
  // Determine background color based on type
  const getBackgroundColor = () => {
    switch (type) {
      case 'error':
        return 'var(--red-9)';
      case 'warning':
        return 'var(--orange-9)';
      case 'success':
      default:
        return 'var(--green-9)';
    }
  };

  return (
    <Toast.Root
      className="ToastRoot"
      open={open}
      onOpenChange={onOpenChange}
      style={{
        backgroundColor: getBackgroundColor(),
        color: 'white',
        borderRadius: '6px',
        padding: '12px 16px',
        fontSize: '14px',
        fontWeight: '500',
        boxShadow:
          'hsl(206 22% 7% / 35%) 0px 10px 38px -10px, hsl(206 22% 7% / 20%) 0px 10px 20px -15px',
      }}
    >
      <Toast.Title>{message}</Toast.Title>
    </Toast.Root>
  );
}

/**
 * Toast viewport container
 * Should be rendered once at the root level of the component
 */
export function ToastViewport() {
  return (
    <Toast.Viewport
      className="ToastViewport"
      style={{
        position: 'fixed',
        top: '0',
        right: '0',
        display: 'flex',
        flexDirection: 'column',
        padding: '25px',
        gap: '10px',
        width: 'auto',
        maxWidth: '100vw',
        margin: '0',
        listStyle: 'none',
        zIndex: '2147483647',
        outline: 'none',
      }}
    />
  );
}
