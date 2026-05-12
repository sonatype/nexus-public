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
import { useCurrentStateAndParams } from '@uirouter/react';

import { UploadPage } from '../UploadPage';

// Mock the router hook
jest.mock('@uirouter/react', () => ({
  useCurrentStateAndParams: jest.fn(),
}));

// Mock sub-components
jest.mock('../UploadRepositoryListPage', () => ({
  UploadRepositoryListPage: () => <div data-testid="upload-list-page">List Page</div>,
}));
jest.mock('../UploadFormContainer', () => ({
  UploadFormContainer: () => <div data-testid="upload-form-container">Form Page</div>,
}));

describe('UploadPage Router', () => {
  const mockUseCurrentStateAndParams = useCurrentStateAndParams as jest.Mock;

  it('renders UploadRepositoryListPage when no repoName in params', () => {
    mockUseCurrentStateAndParams.mockReturnValue({ params: {} });
    render(<Theme><UploadPage /></Theme>);
    expect(screen.getByTestId('upload-list-page')).toBeInTheDocument();
  });

  it('renders UploadFormContainer when repoName is present in params', () => {
    mockUseCurrentStateAndParams.mockReturnValue({ params: { repoName: 'maven-releases' } });
    render(<Theme><UploadPage /></Theme>);
    expect(screen.getByTestId('upload-form-container')).toBeInTheDocument();
  });
});
