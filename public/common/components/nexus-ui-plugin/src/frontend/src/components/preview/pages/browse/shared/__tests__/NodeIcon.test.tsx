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
import { NodeIcon } from '../NodeIcon';

describe('NodeIcon', () => {
  describe('renders correct icon for each type', () => {
    it('renders folder icon for folder type', () => {
      const { container } = render(<NodeIcon type="folder" />);
      const svg = container.querySelector('svg');
      expect(svg).toBeInTheDocument();
      // Lucide icons have class names indicating the icon type
      expect(svg).toHaveClass('lucide-folder');
    });

    it('renders package icon for component type', () => {
      const { container } = render(<NodeIcon type="component" />);
      const svg = container.querySelector('svg');
      expect(svg).toBeInTheDocument();
      expect(svg).toHaveClass('lucide-package');
    });

    it('renders file icon for asset type', () => {
      const { container } = render(<NodeIcon type="asset" />);
      const svg = container.querySelector('svg');
      expect(svg).toBeInTheDocument();
      expect(svg).toHaveClass('lucide-file');
    });
  });

  describe('size prop', () => {
    it('uses default size of 16', () => {
      const { container } = render(<NodeIcon type="folder" />);
      const svg = container.querySelector('svg');
      expect(svg).toHaveAttribute('width', '16');
      expect(svg).toHaveAttribute('height', '16');
    });

    it('uses custom size when provided', () => {
      const { container } = render(<NodeIcon type="folder" size={24} />);
      const svg = container.querySelector('svg');
      expect(svg).toHaveAttribute('width', '24');
      expect(svg).toHaveAttribute('height', '24');
    });
  });

  describe('className prop', () => {
    it('applies custom className', () => {
      const { container } = render(<NodeIcon type="folder" className="custom-class" />);
      const svg = container.querySelector('svg');
      expect(svg).toHaveClass('custom-class');
    });
  });
});

