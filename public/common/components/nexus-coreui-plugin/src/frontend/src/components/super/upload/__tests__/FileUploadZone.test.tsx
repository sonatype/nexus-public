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
import { render, screen } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { FileUploadZone } from '../FileUploadZone';

describe('FileUploadZone', () => {
  const mockAssets = [{ file: null }];
  const mockAssetFields = [];
  const mockValidationErrors = {};

  it('renders the file upload zone', () => {
    render(
      <Theme>
        <FileUploadZone
          assets={mockAssets}
          assetFields={mockAssetFields}
          validationErrors={mockValidationErrors}
          multipleUpload={false}
          onFileChange={jest.fn()}
          onAssetFieldChange={jest.fn()}
          onAddAsset={jest.fn()}
          onRemoveAsset={jest.fn()}
        />
      </Theme>
    );

    expect(screen.getByTestId('file-upload-zone')).toBeInTheDocument();
    expect(screen.getByText('Drop files here')).toBeInTheDocument();
  });
});
