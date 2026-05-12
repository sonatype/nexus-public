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

import React, { useState } from 'react';
import { AlertTriangle } from 'lucide-react';
import {
  SettingsTextInput,
  SettingsAlert,
  SettingsButton
} from '../../../shared/form';
import './ConvertToGroupModal.scss';

interface ConvertToGroupModalProps {
  blobStoreName: string;
  onConfirm: (newGroupName: string) => void;
  onCancel: () => void;
  promoting?: boolean;
}

const STRINGS = {
  HEADER: 'Convert to Group Blob Store',
  LABEL: 'Rename Original Blob Store',
  SUBLABEL: 'Assign a new name to the original blob store',
  ALERT: 'You are converting to a group blob store. This action cannot be undone.',
  CONVERT_BUTTON: 'Convert',
  CANCEL_BUTTON: 'Cancel',
  PLACEHOLDER: 'Enter new name for original blob store'
};

export default function ConvertToGroupModal({
  blobStoreName,
  onConfirm,
  onCancel,
  promoting = false
}: ConvertToGroupModalProps) {
  const [newName, setNewName] = useState(`${blobStoreName}-original`);
  const [error, setError] = useState<string | null>(null);

  const handleConfirm = () => {
    if (!newName.trim()) {
      setError('Name is required');
      return;
    }

    if (!/^[a-zA-Z0-9_-]+$/.test(newName)) {
      setError('Name can only contain letters, numbers, underscores, and hyphens');
      return;
    }

    onConfirm(newName);
  };

  return (
    <div className="convert-to-group-modal__overlay">
      <div className="convert-to-group-modal">
        <h2 className="convert-to-group-modal__header">{STRINGS.HEADER}</h2>

        <SettingsAlert variant="warning" icon={<AlertTriangle size={16} />}>
          {STRINGS.ALERT}
        </SettingsAlert>

        <div className="convert-to-group-modal__content">
          <SettingsTextInput
            label={STRINGS.LABEL}
            helpText={STRINGS.SUBLABEL}
            value={newName}
            onChange={(value) => {
              setNewName(value);
              setError(null);
            }}
            placeholder={STRINGS.PLACEHOLDER}
            error={error || undefined}
            required
            disabled={promoting}
          />
        </div>

        <div className="convert-to-group-modal__actions">
          <SettingsButton
            variant="ghost"
            onClick={onCancel}
            disabled={promoting}
          >
            {STRINGS.CANCEL_BUTTON}
          </SettingsButton>
          <SettingsButton
            variant="primary"
            onClick={handleConfirm}
            disabled={promoting || !newName.trim()}
          >
            {promoting ? 'Converting...' : STRINGS.CONVERT_BUTTON}
          </SettingsButton>
        </div>
      </div>
    </div>
  );
}

