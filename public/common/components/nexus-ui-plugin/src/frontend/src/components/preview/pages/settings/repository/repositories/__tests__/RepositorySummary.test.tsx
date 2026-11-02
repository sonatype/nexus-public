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

import { RepositorySummary } from '../RepositorySummary';
import { Repository, RepositoryFormData } from '../types';

// Child cards fetch via API hooks; stub them so this test stays fast and isolated.
jest.mock('../BlobStoreStorageCard', () => ({
  BlobStoreStorageCard: () => null,
}));
jest.mock('../RepositoryMetricsCard', () => ({
  RepositoryMetricsCard: () => null,
}));

const STATUS_LABEL_ONLINE = 'Status: Online';
const STATUS_LABEL_OFFLINE = 'Status: Offline';

const BASE_REPOSITORY: Repository = {
  name: 'maven-releases',
  type: 'hosted',
  format: 'maven2',
  url: 'http://localhost:8081/repository/maven-releases',
} as Repository;

const EMPTY_FORM_DATA: RepositoryFormData = {} as RepositoryFormData;

function renderSummary(repository: Repository) {
  return render(
    <Theme>
      <RepositorySummary
        repository={repository}
        formData={EMPTY_FORM_DATA}
        onNavigateToTab={jest.fn()}
      />
    </Theme>
  );
}

describe('RepositorySummary status', () => {
  it('shouldRenderOnlineWhenStatusOnlineTrue', () => {
    renderSummary({ ...BASE_REPOSITORY, online: false, status: { online: true } });

    expect(screen.getByLabelText(STATUS_LABEL_ONLINE)).toBeInTheDocument();
    expect(screen.queryByLabelText(STATUS_LABEL_OFFLINE)).not.toBeInTheDocument();
  });

  it('shouldRenderOfflineWhenStatusOnlineFalse', () => {
    renderSummary({ ...BASE_REPOSITORY, online: true, status: { online: false } });

    expect(screen.getByLabelText(STATUS_LABEL_OFFLINE)).toBeInTheDocument();
    expect(screen.queryByLabelText(STATUS_LABEL_ONLINE)).not.toBeInTheDocument();
  });

  it('shouldFallBackToTopLevelOnlineWhenStatusAbsent', () => {
    renderSummary({ ...BASE_REPOSITORY, online: false, status: undefined as any });

    expect(screen.getByLabelText(STATUS_LABEL_OFFLINE)).toBeInTheDocument();
  });
});
