/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React from 'react';
import { Box, Flex, Heading, IconButton, Button } from '@radix-ui/themes';
import { X, RefreshCw } from 'lucide-react';

import './MobileFilterDrawer.scss';

export interface MobileFilterDrawerProps {
  /** Whether the drawer is open */
  isOpen: boolean;
  /** Callback when drawer should close */
  onClose: () => void;
  /** Title shown in drawer header */
  title: string;
  /** Optional callback for clear-all button in header */
  onClearAll?: () => void;
  /** Filter content (e.g. SearchSidebar) */
  children: React.ReactNode;
}

/**
 * Mobile filter drawer - slides in from the right on mobile when sidebar is hidden.
 * Matches ux-lab v1/components MobileFilterDrawer: 85vw/360px, Reset filters + RefreshCw.
 */
export function MobileFilterDrawer({
  isOpen,
  onClose,
  title,
  onClearAll,
  children,
}: MobileFilterDrawerProps): JSX.Element {
  if (!isOpen) return null;

  return (
    <Box>
      {/* Backdrop */}
      <div
        onClick={onClose}
        style={{
          position: 'fixed',
          inset: 0,
          backgroundColor: 'transparent',
          zIndex: 200,
        }}
      />

      {/* Drawer - matches ux-lab dimensions and structure */}
      <div
        className="mobile-filter-drawer"
        role="dialog"
        aria-modal="true"
        aria-labelledby="mobile-filter-drawer-title"
        style={{
          position: 'fixed',
          top: 0,
          bottom: 0,
          right: 0,
          width: '85vw',
          maxWidth: '360px',
          backgroundColor: 'var(--color-panel-solid)',
          borderLeft: '1px solid var(--gray-6)',
          zIndex: 201,
          display: 'flex',
          flexDirection: 'column',
          boxShadow: 'var(--shadow-3)',
        }}
      >
        <Flex
          align="center"
          justify="between"
          px="4"
          py="3"
          style={{ borderBottom: '1px solid var(--gray-6)' }}
        >
          <Heading id="mobile-filter-drawer-title" size="4">
            {title}
          </Heading>
          <Flex align="center" gap="3">
            {onClearAll && (
              <Button
                variant="outline"
                color="gray"
                size="2"
                onClick={onClearAll}
              >
                <RefreshCw size={12} />
                Reset filters
              </Button>
            )}
            <IconButton
              variant="ghost"
              size="2"
              color="gray"
              onClick={onClose}
              aria-label="Close"
            >
              <X size={16} />
            </IconButton>
          </Flex>
        </Flex>

        <Box px="4" py="3" style={{ overflowY: 'auto', flex: 1 }}>
          {children}
        </Box>
      </div>
    </Box>
  );
}

export default MobileFilterDrawer;
