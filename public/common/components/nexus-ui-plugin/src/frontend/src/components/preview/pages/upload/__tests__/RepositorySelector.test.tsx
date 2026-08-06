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
import { render, screen, } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';

import { RepositorySelector } from '../RepositorySelector';

describe('RepositorySelector', () => {
  const mockRepositories = [
    { name: 'maven-releases', format: 'maven2', url: '' },
    { name: 'npm-hosted', format: 'npm', url: '' },
  ];
  const mockOnChange = jest.fn();

  it('renders the repository selector with options', () => {
    render(
      <Theme>
        <RepositorySelector
          repositoryName="maven-releases"
          onRepositoryChange={mockOnChange}
          availableRepositories={mockRepositories}
        />
      </Theme>
    );

    expect(screen.getByText('Target Repository')).toBeInTheDocument();
    expect(screen.getByDisplayValue('maven-releases')).toBeInTheDocument();
  });
});
