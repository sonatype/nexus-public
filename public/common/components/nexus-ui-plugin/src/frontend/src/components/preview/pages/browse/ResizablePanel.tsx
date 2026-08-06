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

import React, { useState, useCallback, useEffect, useRef } from 'react';
import { Box, Flex } from '@radix-ui/themes';

export interface ResizablePanelProps {
  leftPanel: React.ReactNode;
  rightPanel: React.ReactNode;
  defaultLeftWidth?: number;
  minLeftWidth?: number;
  maxLeftWidth?: number;
  storageKey?: string;
  className?: string;
}

/**
 * ResizablePanel - GitHub/VS Code style split view with drag handle.
 *
 * Features:
 * - Drag handle to resize left/right panels
 * - Min/max width constraints
 * - Persist width to localStorage
 * - Double-click to reset to default
 * - Visual feedback on hover
 */
export function ResizablePanel({
  leftPanel,
  rightPanel,
  defaultLeftWidth = 400,
  minLeftWidth = 200,
  maxLeftWidth = 800,
  storageKey = 'browse-tree-width',
  className = '',
}: ResizablePanelProps): JSX.Element {
  const containerRef = useRef<HTMLDivElement>(null);
  const [leftWidth, setLeftWidth] = useState<number>(() => {
    // Load from localStorage if available
    if (storageKey) {
      const stored = localStorage.getItem(storageKey);
      if (stored) {
        const width = parseInt(stored, 10);
        if (!Number.isNaN(width) && width >= minLeftWidth && width <= maxLeftWidth) {
          return width;
        }
      }
    }
    return defaultLeftWidth;
  });

  const [isDragging, setIsDragging] = useState(false);

  // Save to localStorage when width changes
  useEffect(() => {
    if (storageKey) {
      localStorage.setItem(storageKey, leftWidth.toString());
    }
  }, [leftWidth, storageKey]);

  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    setIsDragging(true);
  }, []);

  const handleDoubleClick = useCallback(() => {
    setLeftWidth(defaultLeftWidth);
  }, [defaultLeftWidth]);

  useEffect(() => {
    if (!isDragging) return;

    const handleMouseMove = (e: MouseEvent) => {
      if (!containerRef.current) return;

      const containerRect = containerRef.current.getBoundingClientRect();
      const newWidth = e.clientX - containerRect.left;

      // Enforce constraints
      const constrainedWidth = Math.max(
        minLeftWidth,
        Math.min(maxLeftWidth, newWidth)
      );

      setLeftWidth(constrainedWidth);
    };

    const handleMouseUp = () => {
      setIsDragging(false);
    };

    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);

    // Change cursor globally while dragging
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };
  }, [isDragging, minLeftWidth, maxLeftWidth]);

  return (
    <Flex
      ref={containerRef}
      className={`resizable-panel ${className}`}
      style={{
        height: '100%',
        width: '100%',
        overflow: 'hidden',
        position: 'relative',
      }}
    >
      {/* Left Panel */}
      <Box
        className="resizable-panel__left"
        style={{
          width: `${leftWidth}px`,
          height: '100%',
          overflow: 'auto',
          flexShrink: 0,
        }}
      >
        {leftPanel}
      </Box>

      {/* Drag Handle */}
      <Box
        className="resizable-panel__handle"
        onMouseDown={handleMouseDown}
        onDoubleClick={handleDoubleClick}
        style={{
          width: '5px',
          height: '100%',
          cursor: 'col-resize',
          backgroundColor: 'var(--gray-5)',
          flexShrink: 0,
          position: 'relative',
          transition: 'background-color 0.2s',
        }}
        onMouseEnter={(e) => {
          e.currentTarget.style.backgroundColor = 'var(--blue-9)';
        }}
        onMouseLeave={(e) => {
          if (!isDragging) {
            e.currentTarget.style.backgroundColor = 'var(--gray-5)';
          }
        }}
        title="Drag to resize, double-click to reset"
      />

      {/* Right Panel */}
      <Box
        className="resizable-panel__right"
        style={{
          flex: 1,
          height: '100%',
          overflow: 'auto',
          minWidth: 0,
        }}
      >
        {rightPanel}
      </Box>
    </Flex>
  );
}

export default ResizablePanel;
