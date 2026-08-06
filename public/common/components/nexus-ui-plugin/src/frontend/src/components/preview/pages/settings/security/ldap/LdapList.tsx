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
  ChevronUp,
  ChevronDown,
} from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { SettingsButton } from '../../../../shared/form';
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
  const [isReordering, setIsReordering] = useState(false);

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
    return localServers.filter((server, index) => {
      const url = (server.url || `${server.protocol}://${server.host}:${server.port}`).toLowerCase();
      // Match the order number as displayed (live position after drag-reorder), not the stale
      // server.order from the API — the Order column shows index + 1, so filter on the same value.
      return (
        server.name.toLowerCase().includes(search) ||
        server.host.toLowerCase().includes(search) ||
        url.includes(search) ||
        String(index + 1).includes(search)
      );
    });
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

  const handleDrop = useCallback(async (e: React.DragEvent, dropIndex: number) => {
    e.preventDefault();
    if (draggedIndex === null || draggedIndex === dropIndex || isReordering) {
      setDraggedIndex(null);
      return;
    }

    const newServers = [...localServers];
    const [removed] = newServers.splice(draggedIndex, 1);
    newServers.splice(dropIndex, 0, removed);
    const previous = localServers;
    setLocalServers(newServers);
    // Drag indicator cleared before await; row order reverts on API failure.
    setDraggedIndex(null);
    setIsReordering(true);

    const serverNames = newServers.map((s) => s.name).filter(Boolean);
    try {
      await onReorder(serverNames);
    } catch {
      setLocalServers(previous);
    } finally {
      setIsReordering(false);
    }
  }, [draggedIndex, localServers, onReorder, isReordering]);

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

  // Change order modal handlers
  const handleOpenOrderModal = useCallback(() => {
    setOrderServers([...localServers]);
    setShowOrderModal(true);
  }, [localServers]);

  const handleOrderDragStart = useCallback((e: React.DragEvent, index: number) => {
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('text/plain', index.toString());
  }, []);

  const moveOrderItem = useCallback((fromIndex: number, toIndex: number) => {
    setOrderServers((prev) => {
      if (toIndex < 0 || toIndex >= prev.length) return prev;
      const newServers = [...prev];
      const [removed] = newServers.splice(fromIndex, 1);
      newServers.splice(toIndex, 0, removed);
      return newServers;
    });
  }, []);

  const handleOrderDrop = useCallback((e: React.DragEvent, dropIndex: number) => {
    e.preventDefault();
    const dragIndex = parseInt(e.dataTransfer.getData('text/plain'), 10);
    if (dragIndex === dropIndex) return;
    moveOrderItem(dragIndex, dropIndex);
  }, [moveOrderItem]);

  const handleSaveOrder = useCallback(async () => {
    const serverNames = orderServers.map((s) => s.name).filter(Boolean);
    const previous = localServers;
    setLocalServers(orderServers);
    setShowOrderModal(false);
    setIsReordering(true);
    try {
      await onReorder(serverNames);
    } catch {
      // Reopen the modal so the user can retry. The error reason is surfaced
      // via the page-level banner (useLdapApi sets error state; LdapPage
      // renders it when viewMode === 'list').
      setLocalServers(previous);
      setShowOrderModal(true);
    } finally {
      setIsReordering(false);
    }
  }, [orderServers, localServers, onReorder]);

  return (
    <Box className="ldap-list">
      {/* Toolbar */}
      <Flex justify="between" align="center" gap="3" className="ldap-list__toolbar">
        <Box className="ldap-list__search">
          <Search size={16} className="ldap-list__search-icon" aria-hidden="true" />
          <input
            type="text"
            placeholder="Filter by name or URL..."
            aria-label="Filter servers"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            className="ldap-list__search-input"
            data-testid="ldap-search-input"
            data-analytics-id="nxrm-ldap-list-filter"
          />
        </Box>
        <Flex gap="2">
          {canUpdate && (
            <SettingsButton
              variant="secondary"
              onClick={handleOpenOrderModal}
              disabled={loading || isReordering || servers.length <= 1}
              title="Change server order"
              data-testid="ldap-change-order-button"
              icon={ArrowUpDown}
            >
              Change Order
            </SettingsButton>
          )}
          {canDelete && (
            <SettingsButton
              variant="secondary"
              onClick={onClearCache}
              disabled={loading || servers.length === 0}
              title="Clear LDAP cache"
              data-testid="ldap-clear-cache-button"
              data-analytics-id="nxrm-ldap-list-clear-cache"
              icon={RefreshCw}
            >
              Clear Cache
            </SettingsButton>
          )}
        </Flex>
      </Flex>

      {/* Loading State */}
      {loading && servers.length === 0 && (
        <Flex role="status" aria-live="polite" align="center" justify="center" className="ldap-list__loading">
          <Loader2 size={24} className="ldap-list__spinner" aria-hidden="true" />
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
              {filteredServers.map((server) => {
                // Resolve the drag index against localServers by identity, not the filtered map
                // position. When a filter is active the two arrays have different index spaces, so
                // splicing localServers by a filtered index silently corrupts the submitted order.
                const actualIndex = localServers.indexOf(server);
                return (
                <tr
                  key={server.id}
                  draggable={canUpdate && !isReordering}
                  onDragStart={(e) => handleDragStart(e, actualIndex)}
                  onDragOver={handleDragOver}
                  onDrop={(e) => handleDrop(e, actualIndex)}
                  onDragEnd={handleDragEnd}
                  onClick={() => handleRowClick(server)}
                  data-analytics-id="nxrm-ldap-list-select-server"
                  className={`ldap-list__row ${draggedIndex === actualIndex ? 'ldap-list__row--dragging' : ''}`}
                >
                  <td className="ldap-list__td ldap-list__td--order">
                    <Flex align="center" gap="2">
                      {canUpdate && (
                        <GripVertical size={14} className="ldap-list__drag-handle" aria-hidden="true" />
                      )}
                      <span className="ldap-list__order-number">{localServers.indexOf(server) + 1}</span>
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
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleRowClick(server);
                        }}
                        className="ldap-list__action-button ldap-list__action-button--edit"
                        title="Edit server"
                        aria-label={`Edit LDAP server ${server.name}`}
                        data-analytics-id="nxrm-ldap-list-edit-server"
                      >
                        <Pencil size={16} aria-hidden="true" />
                      </button>
                      {canDelete && (
                        <button
                          type="button"
                          onClick={(e) => handleDeleteClick(e, server)}
                          className="ldap-list__action-button ldap-list__action-button--delete"
                          title="Delete server"
                          aria-label={`Delete ${server.name}`}
                          data-analytics-id="nxrm-ldap-list-delete-server"
                        >
                          <Trash2 size={16} aria-hidden="true" />
                        </button>
                      )}
                    </Flex>
                  </td>
                </tr>
                );
              })}
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
          <Info size={16} aria-hidden="true" />
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
            <ExternalLink size={12} aria-hidden="true" />
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
        analyticsId="nxrm-ldap-list-delete-confirm"
      />

      {/* Change Order Modal */}
      <Dialog.Root open={showOrderModal} onOpenChange={setShowOrderModal}>
        <Dialog.Portal>
          <Dialog.Overlay className="ldap-list__modal-overlay" />
          <Dialog.Content className="ldap-list__modal ldap-list__modal--order" data-testid="ldap-order-modal">
            <Dialog.Title className="ldap-list__modal-title">
              <Flex align="center" gap="2">
                <ArrowUpDown size={20} aria-hidden="true" />
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
                    <GripVertical size={16} className="ldap-list__drag-handle" aria-hidden="true" />
                    <Text weight="medium">{index + 1}.</Text>
                    <Text>{server.name}</Text>
                    <Flex className="ldap-list__order-item-move-buttons" gap="1">
                      <button
                        type="button"
                        className="ldap-list__order-move-button"
                        aria-label={`Move ${server.name} up`}
                        disabled={index === 0}
                        onClick={() => moveOrderItem(index, index - 1)}
                      >
                        <ChevronUp size={14} aria-hidden="true" />
                      </button>
                      <button
                        type="button"
                        className="ldap-list__order-move-button"
                        aria-label={`Move ${server.name} down`}
                        disabled={index === orderServers.length - 1}
                        onClick={() => moveOrderItem(index, index + 1)}
                      >
                        <ChevronDown size={14} aria-hidden="true" />
                      </button>
                    </Flex>
                  </Flex>
                </Box>
              ))}
            </Box>
            <Flex gap="2" justify="end" className="ldap-list__modal-actions">
              <SettingsButton
                variant="secondary"
                onClick={() => setShowOrderModal(false)}
                data-testid="ldap-order-cancel"
                data-analytics-id="nxrm-ldap-order-cancel"
              >
                Cancel
              </SettingsButton>
              <SettingsButton
                variant="primary"
                onClick={handleSaveOrder}
                disabled={isReordering}
                data-testid="ldap-order-save"
                data-analytics-id="nxrm-ldap-order-save"
              >
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
