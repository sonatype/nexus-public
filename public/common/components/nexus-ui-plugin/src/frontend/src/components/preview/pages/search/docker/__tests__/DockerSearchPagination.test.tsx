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
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import { DockerSearchPagination } from '../DockerSearchPagination';

describe('DockerSearchPagination', () => {
  it('returns null when no more results and all loaded', () => {
    const { container } = render(
      <DockerSearchPagination
        hasMore={false}
        loading={false}
        loadedCount={10}
        totalCount={10}
        onLoadMore={jest.fn()}
      />
    );

    expect(container.firstChild).toBeNull();
  });

  it('shows count info when loadedCount < totalCount (even without hasMore)', () => {
    render(
      <DockerSearchPagination
        hasMore={false}
        loading={false}
        loadedCount={5}
        totalCount={10}
        onLoadMore={jest.fn()}
      />
    );

    expect(screen.getByText(/showing 5 of 10 results/i)).toBeInTheDocument();
  });

  it('shows Load More button when hasMore is true', () => {
    render(
      <DockerSearchPagination
        hasMore={true}
        loading={false}
        loadedCount={10}
        totalCount={50}
        onLoadMore={jest.fn()}
      />
    );

    expect(screen.getByRole('button', { name: /load more/i })).toBeInTheDocument();
  });

  it('disables Load More button when loading', () => {
    render(
      <DockerSearchPagination
        hasMore={true}
        loading={true}
        loadedCount={10}
        totalCount={50}
        onLoadMore={jest.fn()}
      />
    );

    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('calls onLoadMore when Load More button clicked', async () => {
    const onLoadMore = jest.fn();
    render(
      <DockerSearchPagination
        hasMore={true}
        loading={false}
        loadedCount={10}
        totalCount={50}
        onLoadMore={onLoadMore}
      />
    );

    await userEvent.click(screen.getByRole('button', { name: /load more/i }));

    expect(onLoadMore).toHaveBeenCalledTimes(1);
  });
});
