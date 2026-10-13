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
import { render } from '@testing-library/react';
import '@testing-library/jest-dom';
import { Database, Cloud, FolderTree } from 'lucide-react';

import {
  REPOSITORY_TYPE_CONFIG,
  RepositoryTypeIcon,
} from '../repositoryTypeIcon';

describe('repositoryTypeIcon', () => {
  describe('REPOSITORY_TYPE_CONFIG', () => {
    it('has exactly the three expected keys', () => {
      const keys = Object.keys(REPOSITORY_TYPE_CONFIG);
      expect(keys).toHaveLength(3);
      expect(keys).toContain('hosted');
      expect(keys).toContain('proxy');
      expect(keys).toContain('group');
    });

    it('has correct icon/iconColor/radixColor for hosted', () => {
      const config = REPOSITORY_TYPE_CONFIG.hosted;
      expect(config.icon).toBe(Database);
      expect(config.iconColor).toBe('var(--green-9)');
      expect(config.radixColor).toBe('green');
    });

    it('has correct icon/iconColor/radixColor for proxy', () => {
      const config = REPOSITORY_TYPE_CONFIG.proxy;
      expect(config.icon).toBe(Cloud);
      expect(config.iconColor).toBe('var(--blue-9)');
      expect(config.radixColor).toBe('blue');
    });

    it('has correct icon/iconColor/radixColor for group', () => {
      const config = REPOSITORY_TYPE_CONFIG.group;
      expect(config.icon).toBe(FolderTree);
      expect(config.iconColor).toBe('var(--purple-9)');
      expect(config.radixColor).toBe('purple');
    });
  });

  describe('RepositoryTypeIcon', () => {
    it('renders Database icon with green color for hosted type', () => {
      const { container } = render(<RepositoryTypeIcon type="hosted" />);
      const icon = container.querySelector('.lucide-database');
      expect(icon).toBeTruthy();
      expect(icon).toHaveStyle({ color: 'var(--green-9)' });
    });

    it('renders Cloud icon with blue color for proxy type', () => {
      const { container } = render(<RepositoryTypeIcon type="proxy" />);
      const icon = container.querySelector('.lucide-cloud');
      expect(icon).toBeTruthy();
      expect(icon).toHaveStyle({ color: 'var(--blue-9)' });
    });

    it('renders FolderTree icon with purple color for group type', () => {
      const { container } = render(<RepositoryTypeIcon type="group" />);
      const icon = container.querySelector('.lucide-folder-tree');
      expect(icon).toBeTruthy();
      expect(icon).toHaveStyle({ color: 'var(--purple-9)' });
    });

    it('passes size prop through to icon', () => {
      const { container } = render(<RepositoryTypeIcon type="hosted" size={24} />);
      const icon = container.querySelector('.lucide-database');
      expect(icon).toBeTruthy();
      expect(icon).toHaveAttribute('width', '24');
      expect(icon).toHaveAttribute('height', '24');
    });

    it('passes className prop through to icon', () => {
      const { container } = render(<RepositoryTypeIcon type="hosted" className="test-class" />);
      const icon = container.querySelector('.lucide-database');
      expect(icon).toBeTruthy();
      expect(icon).toHaveClass('test-class');
    });
  });
});
