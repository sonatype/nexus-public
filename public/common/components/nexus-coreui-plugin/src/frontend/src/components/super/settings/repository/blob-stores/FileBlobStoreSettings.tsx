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
import { Folder, AlertTriangle } from 'lucide-react';
import {
  SettingsFormSection,
  SettingsTextInput,
  SettingsAlert
} from '../../../shared/form';
import type { BlobStoreFormData } from './types';
import './FileBlobStoreSettings.scss';

interface FileBlobStoreSettingsProps {
  data: BlobStoreFormData;
  onChange: (path: string, value: unknown) => void;
  disabled?: boolean;
  isEdit?: boolean;
}

const STRINGS = {
  TITLE: 'File Storage Configuration',
  DESCRIPTION: 'Configure the local filesystem path for blob storage',
  PATH: {
    label: 'Path',
    helpText: 'An absolute path or a path relative to <data-directory>/blobs',
    placeholder: '/path/to/blob/storage'
  },
  WARNING: 'Changing the path will not migrate existing data. Previously stored blobs will not be available at the new location.'
};

export default function FileBlobStoreSettings({
  data,
  onChange,
  disabled = false,
  isEdit = false
}: FileBlobStoreSettingsProps) {
  return (
    <div className="file-blob-store-settings">
      <SettingsFormSection
        title={STRINGS.TITLE}
        description={STRINGS.DESCRIPTION}
        icon={<Folder size={20} />}
      >
        {isEdit && (
          <SettingsAlert variant="warning" icon={<AlertTriangle size={16} />}>
            {STRINGS.WARNING}
          </SettingsAlert>
        )}

        <SettingsTextInput
          name="file-path"
          label={STRINGS.PATH.label}
          value={data.path || ''}
          onChange={(value) => onChange('path', value)}
          helpText={STRINGS.PATH.helpText}
          placeholder={STRINGS.PATH.placeholder}
          required
          disabled={disabled}
          monospace
        />
      </SettingsFormSection>
    </div>
  );
}

