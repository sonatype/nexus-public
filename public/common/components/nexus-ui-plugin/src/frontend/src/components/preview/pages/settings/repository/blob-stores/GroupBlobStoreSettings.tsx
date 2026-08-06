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

import React from 'react';
import { Layers } from 'lucide-react';
import {
  SettingsFormSection,
  SettingsSelect,
  SettingsTransferList
} from '../../../../shared/form';
import { useGroupableBlobStores } from './useBlobStores';
import type { BlobStoreFormData } from './types';
import './GroupBlobStoreSettings.scss';

interface GroupBlobStoreSettingsProps {
  data: BlobStoreFormData;
  onChange: (path: string, value: unknown) => void;
  disabled?: boolean;
  isEdit?: boolean;
}

const STRINGS = {
  TITLE: 'Group Configuration',
  DESCRIPTION: 'Configure the blob store group settings',
  MEMBERS: {
    label: 'Members',
    helpText: 'Select the blob stores to include in this group',
    availableTitle: 'Available Blob Stores',
    selectedTitle: 'Selected Blob Stores'
  },
  FILL_POLICY: {
    label: 'Fill Policy',
    helpText: 'Determines how blobs are distributed across group members'
  }
};

const FILL_POLICY_OPTIONS = [
  { value: '', label: 'Select a fill policy...' },
  { value: 'writeToFirst', label: 'Write to First' },
  { value: 'roundRobin', label: 'Round Robin' }
];

export default function GroupBlobStoreSettings({
  data,
  onChange,
  disabled = false,
  isEdit = false
}: GroupBlobStoreSettingsProps) {
  const { blobStores, loading } = useGroupableBlobStores();

  // Get current members from data
  const members: string[] = data.members || [];
  const fillPolicy: string = data.fillPolicy || '';

  // Filter out already selected members from available list
  const availableOptions = blobStores
    .filter(store => !members.includes(store))
    .map(store => ({ value: store, label: store }));

  const selectedOptions = members.map(store => ({ value: store, label: store }));

  const handleMembersChange = (selected: string[]) => {
    onChange('members', selected);
  };

  const handleFillPolicyChange = (value: string) => {
    onChange('fillPolicy', value);
  };

  return (
    <div className="group-blob-store-settings">
      <SettingsFormSection
        title={STRINGS.TITLE}
        description={STRINGS.DESCRIPTION}
        icon={<Layers size={20} />}
      >
        <SettingsTransferList
          name="members"
          label={STRINGS.MEMBERS.label}
          helpText={STRINGS.MEMBERS.helpText}
          availableItems={availableOptions}
          selectedItems={selectedOptions}
          availableLabel={STRINGS.MEMBERS.availableTitle}
          selectedLabel={STRINGS.MEMBERS.selectedTitle}
          onChange={(items: Array<{value: string; label: string}>) => handleMembersChange(items.map(i => i.value))}
          disabled={disabled || loading}
        />

        <SettingsSelect
          name="group-fill-policy"
          label={STRINGS.FILL_POLICY.label}
          value={fillPolicy}
          onChange={handleFillPolicyChange}
          options={FILL_POLICY_OPTIONS}
          helpText={STRINGS.FILL_POLICY.helpText}
          required
          disabled={disabled}
        />
      </SettingsFormSection>
    </div>
  );
}

