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
import {render, screen} from '@testing-library/react';
import {useActor} from '@xstate/react';
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import FileBlobStoreWarning from './FileBlobStoreWarning';

jest.mock('@xstate/react');
jest.mock('@sonatype/nexus-ui-plugin', () => ({
  ExtJS: {
    state: jest.fn(),
  },
}));

describe('FileBlobStoreWarning', () => {
  const mockService = {};

  beforeEach(() => {
    jest.clearAllMocks();
  });

  function setupMocks({path, isClustered, workDirectory}) {
    useActor.mockReturnValue([
      {
        context: {
          data: {path},
        },
      },
    ]);

    ExtJS.state.mockReturnValue({
      getValue: jest.fn().mockReturnValue({
        isClustered,
        workDirectory,
      }),
    });
  }

  describe('when not clustered', () => {
    it('does not render warning for any path', () => {
      setupMocks({
        path: '/nexus-data/blobs',
        isClustered: false,
        workDirectory: '/nexus-data',
      });

      const {container} = render(<FileBlobStoreWarning service={mockService} />);
      expect(container.firstChild).toBeNull();
    });
  });

  describe('when clustered', () => {
    describe('relative paths', () => {
      it('shows warning for relative Unix path', () => {
        setupMocks({
          path: 'relative/path',
          isClustered: true,
          workDirectory: '/nexus-data',
        });

        render(<FileBlobStoreWarning service={mockService} />);
        expect(screen.getByText('High Availability Path Warning')).toBeInTheDocument();
      });

      it('shows warning for relative Windows path', () => {
        setupMocks({
          path: 'relative\\path',
          isClustered: true,
          workDirectory: 'C:\\nexus-data',
        });

        render(<FileBlobStoreWarning service={mockService} />);
        expect(screen.getByText('High Availability Path Warning')).toBeInTheDocument();
      });

      it('does not show warning for Unix absolute path outside work directory', () => {
        setupMocks({
          path: '/mnt/shared-storage/blobs',
          isClustered: true,
          workDirectory: '/nexus-data',
        });

        const {container} = render(<FileBlobStoreWarning service={mockService} />);
        expect(container.firstChild).toBeNull();
      });

      it('does not show warning for Windows absolute path outside work directory', () => {
        setupMocks({
          path: 'D:\\shared-storage\\blobs',
          isClustered: true,
          workDirectory: 'C:\\nexus-data',
        });

        const {container} = render(<FileBlobStoreWarning service={mockService} />);
        expect(container.firstChild).toBeNull();
      });
    });

    describe('paths under work directory', () => {
      it('shows warning for path directly under work directory', () => {
        setupMocks({
          path: '/nexus-data/blobs',
          isClustered: true,
          workDirectory: '/nexus-data',
        });

        render(<FileBlobStoreWarning service={mockService} />);
        expect(screen.getByText('High Availability Path Warning')).toBeInTheDocument();
      });

      it('shows warning for nested path under work directory', () => {
        setupMocks({
          path: '/nexus-data/blobs/default/content',
          isClustered: true,
          workDirectory: '/nexus-data',
        });

        render(<FileBlobStoreWarning service={mockService} />);
        expect(screen.getByText('High Availability Path Warning')).toBeInTheDocument();
      });

      it('does not show warning for sibling directory with prefix name', () => {
        setupMocks({
          path: '/nexus-data-backup/blobs',
          isClustered: true,
          workDirectory: '/nexus-data',
        });

        const {container} = render(<FileBlobStoreWarning service={mockService} />);
        expect(container.firstChild).toBeNull();
      });

      it('does not show warning when work directory name is prefix of path component', () => {
        setupMocks({
          path: '/nexus-data/information/blobs',
          isClustered: true,
          workDirectory: '/nexus-data/info',
        });

        const {container} = render(<FileBlobStoreWarning service={mockService} />);
        expect(container.firstChild).toBeNull();
      });

      it('shows warning when path is exactly under work directory subdirectory', () => {
        setupMocks({
          path: '/nexus-data/info/blobs',
          isClustered: true,
          workDirectory: '/nexus-data/info',
        });

        render(<FileBlobStoreWarning service={mockService} />);
        expect(screen.getByText('High Availability Path Warning')).toBeInTheDocument();
      });
    });

    describe('Windows paths', () => {
      it('shows warning for Windows path under work directory', () => {
        setupMocks({
          path: 'C:\\nexus-data\\blobs',
          isClustered: true,
          workDirectory: 'C:\\nexus-data',
        });

        render(<FileBlobStoreWarning service={mockService} />);
        expect(screen.getByText('High Availability Path Warning')).toBeInTheDocument();
      });

      it('does not show warning for Windows sibling directory', () => {
        setupMocks({
          path: 'C:\\nexus-data-backup\\blobs',
          isClustered: true,
          workDirectory: 'C:\\nexus-data',
        });

        const {container} = render(<FileBlobStoreWarning service={mockService} />);
        expect(container.firstChild).toBeNull();
      });

      it('normalizes mixed slashes in Windows paths', () => {
        setupMocks({
          path: 'C:/nexus-data/blobs',
          isClustered: true,
          workDirectory: 'C:\\nexus-data',
        });

        render(<FileBlobStoreWarning service={mockService} />);
        expect(screen.getByText('High Availability Path Warning')).toBeInTheDocument();
      });
    });

    describe('edge cases', () => {
      it('does not show warning for empty path', () => {
        setupMocks({
          path: '',
          isClustered: true,
          workDirectory: '/nexus-data',
        });

        const {container} = render(<FileBlobStoreWarning service={mockService} />);
        expect(container.firstChild).toBeNull();
      });

      it('does not show warning for null path', () => {
        setupMocks({
          path: null,
          isClustered: true,
          workDirectory: '/nexus-data',
        });

        const {container} = render(<FileBlobStoreWarning service={mockService} />);
        expect(container.firstChild).toBeNull();
      });

      it('does not show warning for undefined path', () => {
        setupMocks({
          path: undefined,
          isClustered: true,
          workDirectory: '/nexus-data',
        });

        const {container} = render(<FileBlobStoreWarning service={mockService} />);
        expect(container.firstChild).toBeNull();
      });

      it('does not show warning when work directory is empty', () => {
        setupMocks({
          path: '/nexus-data/blobs',
          isClustered: true,
          workDirectory: '',
        });

        const {container} = render(<FileBlobStoreWarning service={mockService} />);
        expect(container.firstChild).toBeNull();
      });

      it('handles work directory without trailing slash', () => {
        setupMocks({
          path: '/nexus-data/blobs',
          isClustered: true,
          workDirectory: '/nexus-data',
        });

        render(<FileBlobStoreWarning service={mockService} />);
        expect(screen.getByText('High Availability Path Warning')).toBeInTheDocument();
      });

      it('handles work directory with trailing slash', () => {
        setupMocks({
          path: '/nexus-data/blobs',
          isClustered: true,
          workDirectory: '/nexus-data/',
        });

        render(<FileBlobStoreWarning service={mockService} />);
        expect(screen.getByText('High Availability Path Warning')).toBeInTheDocument();
      });
    });

    describe('warning content', () => {
      it('displays the correct warning message', () => {
        setupMocks({
          path: 'relative/path',
          isClustered: true,
          workDirectory: '/nexus-data',
        });

        render(<FileBlobStoreWarning service={mockService} />);

        expect(screen.getByText('High Availability Path Warning')).toBeInTheDocument();
        expect(
          screen.getByText(
            /Using a relative path or a path under the working directory.*in HA mode can cause severe performance issues/
          )
        ).toBeInTheDocument();
      });
    });
  });
});
