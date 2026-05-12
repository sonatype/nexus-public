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

import React, { useState, useEffect, useCallback } from 'react';
import { Box, Flex, Text, Badge, Tabs, ScrollArea } from '@radix-ui/themes';
import {
  Loader2,
  AlertCircle,
  CheckCircle,
  XCircle,
  Circle,
  Power,
  PowerOff,
  Trash2,
  Info,
  Settings,
  FileText,
} from 'lucide-react';
import DOMPurify from 'dompurify';
import { SettingsButton, SettingsAlert, SettingsFormSection, SettingsTextArea } from '../../../shared/form';
import { ExtJS } from '@sonatype/nexus-ui-plugin';
import {
  Capability,
  CapabilityType,
  CapabilityFormData,
  getStateColor,
  getStateName,
} from './types';
import { DynamicCapabilityForm } from './DynamicCapabilityForm';
import { useCapabilitiesApi } from './useCapabilitiesApi';

import './CapabilityDetail.scss';

interface CapabilityDetailProps {
  capability: Capability;
  onSave: (data: CapabilityFormData) => Promise<void>;
  onDelete: () => void;
  onEnable: () => void;
  onDisable: () => void;
  onBack: () => void;
  loading?: boolean;
  error?: string;
}

/**
 * State icon component
 */
function StateIcon({ state }: { state: string }) {
  const color = getStateColor(state as 'active' | 'disabled' | 'error' | 'passive');
  const iconProps = { size: 18, style: { color } };

  switch (state) {
    case 'active':
      return <CheckCircle {...iconProps} />;
    case 'disabled':
      return <Circle {...iconProps} />;
    case 'error':
      return <XCircle {...iconProps} />;
    case 'passive':
      return <AlertCircle {...iconProps} />;
    default:
      return <Circle {...iconProps} />;
  }
}

/**
 * CapabilityDetail - View and edit capability details
 */
export function CapabilityDetail({
  capability,
  onSave,
  onDelete,
  onEnable,
  onDisable,
  onBack,
  loading = false,
  error,
}: CapabilityDetailProps) {
  const { fetchCapabilityTypes } = useCapabilitiesApi();
  const [capabilityType, setCapabilityType] = useState<CapabilityType | null>(null);
  const [loadingType, setLoadingType] = useState(true);
  const [activeTab, setActiveTab] = useState('summary');
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [disableConfirmOpen, setDisableConfirmOpen] = useState(false);

  const canUpdate = ExtJS.checkPermission('nexus:capabilities:update');
  const canDelete = ExtJS.checkPermission('nexus:capabilities:delete');

  // Load capability type to get form fields
  useEffect(() => {
    const loadType = async () => {
      setLoadingType(true);
      try {
        const types = await fetchCapabilityTypes();
        const type = types.find((t) => t.id === capability.typeId);
        setCapabilityType(type || null);
      } catch (err) {
        console.error('Failed to load capability type:', err);
      } finally {
        setLoadingType(false);
      }
    };

    loadType();
  }, [capability.typeId, fetchCapabilityTypes]);

  // Handle delete confirmation
  const handleDelete = useCallback(() => {
    if (capability.deleteWarningMessage) {
      setDeleteConfirmOpen(true);
    } else {
      onDelete();
    }
  }, [capability.deleteWarningMessage, onDelete]);

  // Handle disable confirmation
  const handleDisable = useCallback(() => {
    if (capability.disableWarningMessage) {
      setDisableConfirmOpen(true);
    } else {
      onDisable();
    }
  }, [capability.disableWarningMessage, onDisable]);

  // Render summary tab
  const renderSummary = () => (
    <Box className="capability-detail__summary">
      {/* State Warning */}
      {capability.enabled && !capability.active && capability.stateDescription && (
        <SettingsAlert type="warning">{capability.stateDescription}</SettingsAlert>
      )}

      {/* Info Table */}
      <SettingsFormSection title="Summary">
        <Box className="capability-detail__info">
          <Flex className="capability-detail__info-row">
            <Text className="capability-detail__info-label">Type</Text>
            <Text className="capability-detail__info-value">{capability.typeName}</Text>
          </Flex>
          <Flex className="capability-detail__info-row">
            <Text className="capability-detail__info-label">Description</Text>
            <Text className="capability-detail__info-value">{capability.description || '-'}</Text>
          </Flex>
          <Flex className="capability-detail__info-row">
            <Text className="capability-detail__info-label">State</Text>
            <Flex align="center" gap="2">
              <StateIcon state={capability.state} />
              <Badge
                color={
                  capability.state === 'active'
                    ? 'green'
                    : capability.state === 'error'
                    ? 'red'
                    : capability.state === 'passive'
                    ? 'orange'
                    : 'gray'
                }
                variant="soft"
              >
                {getStateName(capability.state as 'active' | 'disabled' | 'error' | 'passive')}
              </Badge>
            </Flex>
          </Flex>
          {/* Dynamic Tags */}
          {capability.tags &&
            Object.entries(capability.tags).map(([key, value]) => (
              <Flex key={key} className="capability-detail__info-row">
                <Text className="capability-detail__info-label">{key}</Text>
                <Text className="capability-detail__info-value">{value}</Text>
              </Flex>
            ))}
        </Box>
      </SettingsFormSection>

      {/* Notes */}
      <SettingsFormSection title="Notes">
        <Text className="capability-detail__notes">{capability.notes || 'No notes'}</Text>
      </SettingsFormSection>
    </Box>
  );

  // Render settings tab
  const renderSettings = () => {
    if (loadingType) {
      return (
        <Flex align="center" justify="center" className="capability-detail__loading">
          <Loader2 size={24} className="capability-detail__spinner" />
          <Text size="2">Loading settings...</Text>
        </Flex>
      );
    }

    if (!capabilityType) {
      return (
        <Box className="capability-detail__error">
          <AlertCircle size={20} />
          <Text size="2">Unable to load capability type configuration</Text>
        </Box>
      );
    }

    return (
      <DynamicCapabilityForm
        capability={capability}
        capabilityType={capabilityType}
        isCreate={false}
        onSave={onSave}
        onCancel={onBack}
        loading={loading}
        error={error}
      />
    );
  };

  // Render status tab
  const renderStatus = () => (
    <Box className="capability-detail__status">
      <SettingsFormSection title="Status">
        {capability.status ? (
          <Box
            className="capability-detail__status-content"
            dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(capability.status) }}
          />
        ) : (
          <Text className="capability-detail__no-status">No status information available</Text>
        )}
      </SettingsFormSection>
    </Box>
  );

  // Render about tab
  const renderAbout = () => (
    <Box className="capability-detail__about">
      <SettingsFormSection title="About">
        {capabilityType?.about ? (
          <Box
            className="capability-detail__about-content"
            dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(capabilityType.about) }}
          />
        ) : (
          <Text className="capability-detail__no-about">No information available</Text>
        )}
      </SettingsFormSection>
    </Box>
  );

  return (
    <Box className="capability-detail">
      {/* Actions */}
      <Flex justify="between" align="center" className="capability-detail__actions">
        <Flex gap="2">
          {canUpdate && !capability.enabled && (
            <SettingsButton
              variant="secondary"
              onClick={onEnable}
              disabled={loading}
              icon={Power}
            >
              Enable
            </SettingsButton>
          )}
          {canUpdate && capability.enabled && (
            <SettingsButton
              variant="secondary"
              onClick={handleDisable}
              disabled={loading}
              icon={PowerOff}
            >
              Disable
            </SettingsButton>
          )}
        </Flex>
        {canDelete && !capability.isSystem && (
          <SettingsButton
            variant="danger"
            onClick={handleDelete}
            disabled={loading}
            icon={Trash2}
          >
            Delete
          </SettingsButton>
        )}
      </Flex>

      {/* Tabs */}
      <Tabs.Root value={activeTab} onValueChange={setActiveTab} className="capability-detail__tabs">
        <Tabs.List>
          <Tabs.Trigger value="summary">
            <Info size={16} />
            Summary
          </Tabs.Trigger>
          <Tabs.Trigger value="settings">
            <Settings size={16} />
            Settings
          </Tabs.Trigger>
          <Tabs.Trigger value="status">
            <FileText size={16} />
            Status
          </Tabs.Trigger>
          <Tabs.Trigger value="about">
            <Info size={16} />
            About
          </Tabs.Trigger>
        </Tabs.List>

        <Box className="capability-detail__tab-content">
          <Tabs.Content value="summary">{renderSummary()}</Tabs.Content>
          <Tabs.Content value="settings">{renderSettings()}</Tabs.Content>
          <Tabs.Content value="status">{renderStatus()}</Tabs.Content>
          <Tabs.Content value="about">{renderAbout()}</Tabs.Content>
        </Box>
      </Tabs.Root>

      {/* Delete Confirmation Dialog */}
      {deleteConfirmOpen && (
        <Box className="capability-detail__dialog-overlay">
          <Box className="capability-detail__dialog">
            <Text weight="medium" size="4">Confirm Delete</Text>
            <Text size="2">{capability.deleteWarningMessage}</Text>
            <Flex gap="2" justify="end" mt="4">
              <SettingsButton variant="secondary" onClick={() => setDeleteConfirmOpen(false)}>
                Cancel
              </SettingsButton>
              <SettingsButton
                variant="danger"
                onClick={() => {
                  setDeleteConfirmOpen(false);
                  onDelete();
                }}
              >
                Delete
              </SettingsButton>
            </Flex>
          </Box>
        </Box>
      )}

      {/* Disable Confirmation Dialog */}
      {disableConfirmOpen && (
        <Box className="capability-detail__dialog-overlay">
          <Box className="capability-detail__dialog">
            <Text weight="medium" size="4">Confirm Disable</Text>
            <Text size="2">{capability.disableWarningMessage}</Text>
            <Flex gap="2" justify="end" mt="4">
              <SettingsButton variant="secondary" onClick={() => setDisableConfirmOpen(false)}>
                Cancel
              </SettingsButton>
              <SettingsButton
                variant="primary"
                onClick={() => {
                  setDisableConfirmOpen(false);
                  onDisable();
                }}
              >
                Disable
              </SettingsButton>
            </Flex>
          </Box>
        </Box>
      )}
    </Box>
  );
}

export default CapabilityDetail;

