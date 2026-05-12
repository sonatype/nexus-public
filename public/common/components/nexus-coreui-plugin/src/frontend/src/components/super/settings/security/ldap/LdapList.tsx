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

import React, { useState, useCallback, useMemo } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import * as Dialog from '@radix-ui/react-dialog';
import {
  GripVertical,
  Pencil,
  Search,
  Loader2,
  Info,
  ExternalLink,
  Trash2,
  RefreshCw,
  ArrowUpDown,
} from 'lucide-react';
import { ExtJS } from '@sonatype/nexus-ui-plugin';

import { SettingsButton } from '../../../shared/form';
import { ConfirmDialog } from '../shared/ConfirmDialog';
import { LdapServer, LdapListProps } from './types';

import './LdapList.scss';

/**
 * LdapList - Displays LDAP servers in a reorderable list
 */
export function LdapList({
  servers,
  onSelect,
  onCreate,
  onReorder,
  onDelete,
  onClearCache,
  loading = false,
}: LdapListProps) {
  const [filter, setFilter] = useState('');
  const [draggedIndex, setDraggedIndex] = useState<number | null>(null);
  const [localServers, setLocalServers] = useState<LdapServer[]>(servers);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [serverToDelete, setServerToDelete] = useState<LdapServer | null>(null);
  const [showOrderModal, setShowOrderModal] = useState(false);
  const [orderServers, setOrderServers] = useState<LdapServer[]>([]);

  const canCreate = ExtJS.checkPermission('nexus:ldap:create');
  const canUpdate = ExtJS.checkPermission('nexus:ldap:update');
  const canDelete = ExtJS.checkPermission('nexus:ldap:delete');

  // Update local servers when prop changes
  React.useEffect(() => {
    setLocalServers(servers);
  }, [servers]);

  // Filtered servers
  const filteredServers = useMemo(() => {
    if (!filter) return localServers;
    const search = filter.toLowerCase();
    return localServers.filter((server) => 
      server.name.toLowerCase().includes(search) ||
      server.host.toLowerCase().includes(search)
    );
  }, [localServers, filter]);

  // Drag handlers
  const handleDragStart = useCallback((e: React.DragEvent, index: number) => {
    setDraggedIndex(index);
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', index.toString());
  }, []);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  }, []);

  const handleDrop = useCallback((e: React.DragEvent, dropIndex: number) => {
    e.preventDefault();
    if (draggedIndex === null || draggedIndex === dropIndex) {
      setDraggedIndex(null);
      return;
    }

    const newServers = [...localServers];
    const [removed] = newServers.splice(draggedIndex, 1);
    newServers.splice(dropIndex, 0, removed);
    setLocalServers(newServers);

    // Notify parent of new order
    const serverIds = newServers.map((s) => s.id!).filter(Boolean);
    onReorder(serverIds);

    setDraggedIndex(null);
  }, [draggedIndex, localServers, onReorder]);

  const handleDragEnd = useCallback(() => {
    setDraggedIndex(null);
  }, []);

  const handleRowClick = useCallback((server: LdapServer) => {
    onSelect(server);
  }, [onSelect]);

  const handleDeleteClick = useCallback((e: React.MouseEvent, server: LdapServer) => {
    e.stopPropagation();
    setServerToDelete(server);
    setShowDeleteModal(true);
  }, []);

  const handleDeleteConfirm = useCallback(() => {
    if (serverToDelete) {
      onDelete(serverToDelete);
      setShowDeleteModal(false);
      setServerToDelete(null);
    }
  }, [serverToDelete, onDelete]);

  const handleDeleteCancel = useCallback(() => {
    setShowDeleteModal(false);
    setServerToDelete(null);
  }, []);

  // Change order modal handlers
  const handleOpenOrderModal = useCallback(() => {
    setOrderServers([...localServers]);
    setShowOrderModal(true);
  }, [localServers]);

  const handleOrderDragStart = useCallback((e: React.DragEvent, index: number) => {
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', index.toString());
  }, []);

  const handleOrderDrop = useCallback((e: React.DragEvent, dropIndex: number) => {
    e.preventDefault();
    const dragIndex = parseInt(e.dataTransfer.getData('text/plain'), 10);
    if (dragIndex === dropIndex) return;

    setOrderServers((prev) => {
      const newServers = [...prev];
      const [removed] = newServers.splice(dragIndex, 1);
      newServers.splice(dropIndex, 0, removed);
      return newServers;
    });
  }, []);

  const handleSaveOrder = useCallback(() => {
    const serverIds = orderServers.map((s) => s.id!).filter(Boolean);
    onReorder(serverIds);
    setLocalServers(orderServers);
    setShowOrderModal(false);
  }, [orderServers, onReorder]);

  return (
    <Box className="ldap-list">
      {/* Toolbar */}
      <Flex justify="between" align="center" gap="3" className="ldap-list__toolbar">
        <Box className="ldap-list__search">
          <Search size={16} className="ldap-list__search-icon" />
          <input
            type="text"
            placeholder="Filter servers..."
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="ldap-list__search-input"
            data-testid="ldap-search-input"
          />
        </Box>
        <Flex gap="2">
          {canUpdate && servers.length > 1 && (
            <SettingsButton
              variant="secondary"
              onClick={handleOpenOrderModal}
              disabled={loading}
              title="Change server order"
              data-testid="ldap-change-order-button"
              icon={ArrowUpDown}
            >
              Change Order
            </SettingsButton>
          )}
          {canUpdate && (
            <SettingsButton
              variant="secondary"
              onClick={onClearCache}
              disabled={loading || servers.length === 0}
              title="Clear LDAP cache"
              data-testid="ldap-clear-cache-button"
              icon={RefreshCw}
            >
              Clear Cache
            </SettingsButton>
          )}
        </Flex>
      </Flex>

      {/* Loading State */}
      {loading && servers.length === 0 && (
        <Flex align="center" justify="center" className="ldap-list__loading">
          <Loader2 size={24} className="ldap-list__spinner" />
          <Text size="2">Loading LDAP servers...</Text>
        </Flex>
      )}

      {/* Empty State */}
      {!loading && servers.length === 0 && (
        <Box className="ldap-list__empty">
          <Text size="2">No LDAP servers configured</Text>
          {canCreate && (
            <Text size="2" className="ldap-list__empty-hint">
              Click "Create LDAP Server" to add one
            </Text>
          )}
        </Box>
      )}

      {/* Server List */}
      {servers.length > 0 && (
        <Box className="ldap-list__table-wrapper">
          <table className="ldap-list__table">
            <thead>
              <tr>
                <th className="ldap-list__th ldap-list__th--order">Order</th>
                <th className="ldap-list__th">Name</th>
                <th className="ldap-list__th">URL</th>
                <th className="ldap-list__th ldap-list__th--actions"></th>
              </tr>
            </thead>
            <tbody>
              {filteredServers.map((server, index) => (
                <tr
                  key={server.id}
                  draggable={canUpdate}
                  onDragStart={(e) => handleDragStart(e, index)}
                  onDragOver={handleDragOver}
                  onDrop={(e) => handleDrop(e, index)}
                  onDragEnd={handleDragEnd}
                  onClick={() => handleRowClick(server)}
                  className={`ldap-list__row ${draggedIndex === index ? 'ldap-list__row--dragging' : ''}`}
                >
                  <td className="ldap-list__td ldap-list__td--order">
                    <Flex align="center" gap="2">
                      {canUpdate && (
                        <GripVertical size={14} className="ldap-list__drag-handle" />
                      )}
                      <span className="ldap-list__order-number">{server.order || index + 1}</span>
                    </Flex>
                  </td>
                  <td className="ldap-list__td ldap-list__td--name">
                    <Text weight="medium">{server.name}</Text>
                  </td>
                  <td className="ldap-list__td ldap-list__td--url">
                    {server.url || `${server.protocol}://${server.host}:${server.port}`}
                  </td>
                  <td className="ldap-list__td ldap-list__td--actions">
                    <Flex align="center" gap="2">
                      {canDelete && (
                        <button
                          type="button"
                          onClick={(e) => handleDeleteClick(e, server)}
                          className="ldap-list__action-button ldap-list__action-button--delete"
                          title="Delete server"
                          aria-label={`Delete ${server.name}`}
                        >
                          <Trash2 size={16} />
                        </button>
                      )}
                      <Pencil size={16} className="ldap-list__row-edit-icon" />
                    </Flex>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Box>
      )}

      {/* No matches */}
      {servers.length > 0 && filteredServers.length === 0 && (
        <Box className="ldap-list__no-matches">
          <Text size="2">No servers match your filter</Text>
        </Box>
      )}

      {/* Help Section */}
      <Box className="ldap-list__help">
        <Flex align="center" gap="2" className="ldap-list__help-header">
          <Info size={16} />
          <Text size="2" weight="medium">About LDAP Servers</Text>
        </Flex>
        <Text size="2" className="ldap-list__help-text">
          LDAP servers are used for user authentication and authorization. The order determines
          which server is tried first. Drag servers to reorder them.
        </Text>
        <Text size="2" className="ldap-list__help-text">
          See our{' '}
          <a
            href="http://links.sonatype.com/products/nxrm3/docs/ldap"
            target="_blank"
            rel="noopener noreferrer"
            className="ldap-list__help-link"
          >
            documentation
            <ExternalLink size={12} />
          </a>
          {' '}for more information.
        </Text>
      </Box>

      {/* Delete Confirmation Modal */}
      <ConfirmDialog
        open={showDeleteModal}
        testId="delete-ldap-server-dialog"
        onOpenChange={setShowDeleteModal}
        title="Delete LDAP Server"
        message={`Are you sure you want to delete the LDAP server "${serverToDelete?.name}"? Users authenticated through this server will no longer be able to log in.`}
        confirmLabel="Delete Server"
        cancelLabel="Cancel"
        variant="danger"
        onConfirm={handleDeleteConfirm}
      />

      {/* Change Order Modal */}
      <Dialog.Root open={showOrderModal} onOpenChange={setShowOrderModal}>
        <Dialog.Portal>
          <Dialog.Overlay className="ldap-list__modal-overlay" />
          <Dialog.Content className="ldap-list__modal ldap-list__modal--order" data-testid="ldap-order-modal">
            <Dialog.Title className="ldap-list__modal-title">
              <Flex align="center" gap="2">
                <ArrowUpDown size={20} />
                Change Server Order
              </Flex>
            </Dialog.Title>
            <Dialog.Description className="ldap-list__modal-description">
              LDAP servers are queried in order during authentication. Drag servers to reorder them.
            </Dialog.Description>
            <Box className="ldap-list__order-list">
              {orderServers.map((server, index) => (
                <Box
                  key={server.id}
                  draggable
                  onDragStart={(e) => handleOrderDragStart(e, index)}
                  onDragOver={(e) => e.preventDefault()}
                  onDrop={(e) => handleOrderDrop(e, index)}
                  className="ldap-list__order-item"
                >
                  <Flex align="center" gap="2">
                    <GripVertical size={16} className="ldap-list__drag-handle" />
                    <Text weight="medium">{index + 1}.</Text>
                    <Text>{server.name}</Text>
                  </Flex>
                </Box>
              ))}
            </Box>
            <Flex gap="2" justify="end" className="ldap-list__modal-actions">
              <SettingsButton variant="secondary" onClick={() => setShowOrderModal(false)}>
                Cancel
              </SettingsButton>
              <SettingsButton variant="primary" onClick={handleSaveOrder} data-testid="ldap-order-save">
                Save Order
              </SettingsButton>
            </Flex>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>
    </Box>
  );
}

export default LdapList;



