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
import { Box, Flex, Text, Badge, Tabs, Spinner, Heading } from '@radix-ui/themes';
import {
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
  Edit3,
} from 'lucide-react';
import { TYPE_ICONS, DEFAULT_TYPE_ICON } from './capabilityConstants';
import DOMPurify from 'dompurify';
import { SettingsButton, SettingsAlert, SettingsFormSection, SettingsTextArea } from '../../../../shared/form';
import { ConfirmDialog } from '../../../../shared';
import { ExtJS } from '../../../../../../interface/ExtJS';
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

function TypeIcon({ typeId, size = 18 }: { typeId: string; size?: number }) {
  const Icon = TYPE_ICONS[typeId] || DEFAULT_TYPE_ICON;
  return <Icon size={size} className="capability-detail__type-icon" />;
}

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
  const [confirmAction, setConfirmAction] = useState<'delete' | 'disable' | null>(null);
  // Track enabled state locally for UI synchronization between header buttons and form checkbox
  const [isEnabled, setIsEnabled] = useState(capability.enabled);

  const canUpdate = ExtJS.checkPermission('nexus:capabilities:update');
  const canDelete = ExtJS.checkPermission('nexus:capabilities:delete');

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

  const handleDelete = useCallback(() => {
    setConfirmAction('delete');
  }, []);

  const handleEnable = useCallback(async () => {
    setIsEnabled(true);
    try {
      await onEnable();
    } catch {
      setIsEnabled(false);
    }
  }, [onEnable]);

  const handleDisable = useCallback(async () => {
    if (capability.disableWarningMessage) {
      setConfirmAction('disable');
    } else {
      setIsEnabled(false);
      try {
        await onDisable();
      } catch {
        setIsEnabled(true);
      }
    }
  }, [capability.disableWarningMessage, onDisable]);

  const handleDisableConfirm = useCallback(async () => {
    setConfirmAction(null);
    setIsEnabled(false);
    try {
      await onDisable();
    } catch {
      setIsEnabled(true);
    }
  }, [onDisable]);

  const handleEnabledChange = useCallback((enabled: boolean) => {
    setIsEnabled(enabled);
  }, []);

  return (
    <Box className="capability-detail" data-testid="capability-detail">
      {/* Error display at top level */}
      {error && (
        <Box className="capability-detail__error-banner">
          <SettingsAlert type="error">{error}</SettingsAlert>
        </Box>
      )}

      {/* Capability Header */}
      <Box className="capability-detail__header">
        <Flex justify="between" align="start">
          <Flex align="center" gap="3">
            <TypeIcon typeId={capability.typeId} />
            <Box>
              <Heading size="5" weight="bold">{capability.typeName}</Heading>
              {capability.description && (
                <Text size="2" color="gray">{capability.description}</Text>
              )}
            </Box>
          </Flex>
          <Flex gap="2" align="center">
            <Badge
              color={
                capability.state === 'active' ? 'green'
                  : capability.state === 'error' ? 'red'
                  : capability.state === 'passive' ? 'orange'
                  : 'gray'
              }
              variant="soft"
              data-testid="capability-state-badge"
            >
              {getStateName(capability.state as 'active' | 'disabled' | 'error' | 'passive')}
            </Badge>
            <Badge
              color={isEnabled ? 'green' : 'gray'}
              variant="outline"
              data-testid="capability-enabled-badge"
            >
              {isEnabled ? 'Enabled' : 'Disabled'}
            </Badge>
          </Flex>
        </Flex>
        {/* Action buttons in header */}
        <Flex justify="end" gap="2" mt="4">
          {canUpdate && !isEnabled && (
            <SettingsButton
              variant="secondary"
              onClick={handleEnable}
              disabled={loading}
              icon={Power}
              data-analytics-id="nxrm-capability-toggle-enable"
            >
              Enable
            </SettingsButton>
          )}
          {canUpdate && isEnabled && (
            <SettingsButton
              variant="secondary"
              onClick={handleDisable}
              disabled={loading}
              icon={PowerOff}
              data-analytics-id="nxrm-capability-toggle-disable"
            >
              Disable
            </SettingsButton>
          )}
          {canDelete && !capability.isSystem && (
            <SettingsButton
              variant="danger"
              onClick={handleDelete}
              disabled={loading}
              icon={Trash2}
              testId="capability-delete-button"
              data-analytics-id="nxrm-capability-delete"
            >
              Delete
            </SettingsButton>
          )}
        </Flex>
      </Box>

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
          {/* Summary Tab */}
          <Tabs.Content value="summary">

            <Box className="capability-detail__summary-grid">
              <SummaryItem
                icon={<StateIcon state={capability.state} />}
                label="State"
                value={getStateName(capability.state as 'active' | 'disabled' | 'error' | 'passive')}
              />
              <SummaryItem
                icon={<TypeIcon typeId={capability.typeId} size={16} />}
                label="Type"
                value={capability.typeName}
              />
              <SummaryItem
                icon={<FileText size={16} />}
                label="Description"
                value={capability.description || '-'}
              />
              {capability.notes && (
                <SummaryItem
                  icon={<Edit3 size={16} />}
                  label="Notes"
                  value={capability.notes}
                />
              )}
              {/* Dynamic Tags */}
              {capability.tags &&
                Object.entries(capability.tags).map(([key, value]) => (
                  <SummaryItem
                    key={key}
                    label={key}
                    value={value}
                  />
                ))}
            </Box>
          </Tabs.Content>

          {/* Settings Tab */}
          <Tabs.Content value="settings">
            {loadingType ? (
              <Flex align="center" justify="center" className="capability-detail__loading">
                <Spinner size="3" />
                <Text size="2">Loading settings...</Text>
              </Flex>
            ) : !capabilityType ? (
              <Box className="capability-detail__error">
                <AlertCircle size={20} />
                <Text size="2">Unable to load capability type configuration</Text>
              </Box>
            ) : (
              <DynamicCapabilityForm
                capability={capability}
                capabilityType={capabilityType}
                isCreate={false}
                onSave={onSave}
                onCancel={onBack}
                loading={loading}
                error={error}
                isEnabled={isEnabled}
                onEnabledChange={handleEnabledChange}
              />
            )}
          </Tabs.Content>

          {/* Status Tab */}
          <Tabs.Content value="status">
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
          </Tabs.Content>

          {/* About Tab */}
          <Tabs.Content value="about">
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
          </Tabs.Content>
        </Box>
      </Tabs.Root>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={confirmAction === 'delete'}
        testId="capability-delete-dialog"
        onOpenChange={(open) => { if (!open) setConfirmAction(null); }}
        title="Delete Capability"
        message={capability.deleteWarningMessage
          ? `${capability.deleteWarningMessage}\n\nThis action can't be undone.`
          : "This will permanently remove this capability and its configuration. This action can't be undone."}
        confirmLabel="Delete"
        variant="danger"
        onConfirm={() => {
          setConfirmAction(null);
          onDelete();
        }}
      />

      {/* Disable Confirmation Dialog */}
      <ConfirmDialog
        open={confirmAction === 'disable'}
        testId="capability-disable-dialog"
        onOpenChange={(open) => { if (!open) setConfirmAction(null); }}
        title="Disable Capability"
        message={capability.disableWarningMessage || `Disable "${capability.typeName}"?`}
        confirmLabel="Disable"
        variant="warning"
        onConfirm={handleDisableConfirm}
      />
    </Box>
  );
}

function SummaryItem({
  icon,
  label,
  value,
}: {
  icon?: React.ReactNode;
  label: string;
  value: string;
}) {
  return (
    <Box className="capability-detail__summary-item">
      <Flex align="center" gap="2">
        {icon && <Box className="capability-detail__summary-icon">{icon}</Box>}
        <Text size="1" color="gray" className="capability-detail__summary-label">{label}:</Text>
      </Flex>
      <Text size="2" className="capability-detail__summary-value">{value}</Text>
    </Box>
  );
}

export default CapabilityDetail;
