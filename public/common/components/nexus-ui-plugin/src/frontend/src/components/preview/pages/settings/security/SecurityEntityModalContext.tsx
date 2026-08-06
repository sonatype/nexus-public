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

import React, { createContext, useContext, useState, useCallback, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { Box, Flex, IconButton, Text, Tooltip } from '@radix-ui/themes';
import { X } from 'lucide-react';

import { RoleProfilePage } from './roles/RoleProfilePage';
import { UserProfilePage } from './users/UserProfilePage';
import { PrivilegeProfilePage } from './privileges/PrivilegeProfilePage';

import './SecurityEntityModal.scss';

export type SecurityEntityType = 'role' | 'user' | 'privilege';

interface SecurityEntityModalState {
  open: boolean;
  entityType: SecurityEntityType | null;
  entityId: string | null;
  entitySource: string | null;
}

interface SecurityEntityModalContextValue {
  openEntity: (type: SecurityEntityType, id: string, source?: string) => void;
  closeModal: () => void;
  isEmbedMode: boolean;
}

const defaultState: SecurityEntityModalState = {
  open: false,
  entityType: null,
  entityId: null,
  entitySource: null,
};

const SecurityEntityModalContext = createContext<SecurityEntityModalContextValue | null>(null);

export function useSecurityEntityModal(): SecurityEntityModalContextValue {
  const ctx = useContext(SecurityEntityModalContext);
  if (!ctx) {
    return {
      openEntity: () => {},
      closeModal: () => {},
      isEmbedMode: false,
    };
  }
  return ctx;
}

export function SecurityEntityModalProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<SecurityEntityModalState>(defaultState);

  const openEntity = useCallback((type: SecurityEntityType, id: string, source?: string) => {
    setState({
      open: true,
      entityType: type,
      entityId: id,
      entitySource: source ?? null,
    });
  }, []);

  const closeModal = useCallback(() => {
    setState(defaultState);
  }, []);

  const value: SecurityEntityModalContextValue = {
    openEntity,
    closeModal,
    isEmbedMode: false,
  };

  const modalTitle = useMemo(() => {
    if (!(state.entityType && state.entityId)) return '';
    const typeLabel =
      state.entityType === 'role'
        ? 'Role'
        : state.entityType === 'user'
          ? 'User'
          : 'Privilege';
    const source =
      state.entityType === 'user' && state.entitySource
        ? ` (${state.entitySource})`
        : '';
    return `${typeLabel}: ${state.entityId}${source}`;
  }, [state.entityType, state.entityId, state.entitySource]);

  const modalContent = state.open && state.entityType && state.entityId && (
    <Box className="security-entity-modal" data-testid="security-entity-modal">
      <Flex className="security-entity-modal__header">
        <Text as="h2" size="4" weight="bold" className="security-entity-modal__title">
          {modalTitle}
        </Text>
        <Tooltip content="Close">
          <IconButton
            variant="ghost"
            size="2"
            onClick={closeModal}
            aria-label="Close"
            data-testid="security-entity-modal-close"
          >
            <X size={16} />
          </IconButton>
        </Tooltip>
      </Flex>
      <Box p="4" className="security-entity-modal__content">
          {state.entityType === 'role' && (
            <RoleProfilePage
              roleName={state.entityId}
              onBack={closeModal}
              embedMode
            />
          )}
          {state.entityType === 'user' && (
            <UserProfilePage
              userId={state.entityId}
              userSource={state.entitySource || 'default'}
              onBack={closeModal}
              embedMode
            />
          )}
          {state.entityType === 'privilege' && (
            <PrivilegeProfilePage
              privilegeId={state.entityId}
              onBack={closeModal}
              embedMode
            />
          )}
      </Box>
    </Box>
  );

  return (
    <SecurityEntityModalContext.Provider value={value}>
      {children}
      {modalContent && createPortal(modalContent, document.body)}
    </SecurityEntityModalContext.Provider>
  );
}
