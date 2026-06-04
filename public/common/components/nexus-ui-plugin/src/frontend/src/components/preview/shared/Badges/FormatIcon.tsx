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
import {
  Package,
  Box as LucideBox,
  Container,
  Archive,
  FileCode,
  Folder,
  FileArchive,
  Layers,
  Cloud,
  HardDrive,
  Terminal,
  GitBranch,
  Boxes,
  Workflow,
  Server,
  Database,
  FolderSync,
  Code,
  Gem,
} from 'lucide-react';
import { FORMAT_LOGOS } from './formatConstants';

import './FormatIcon.scss';

/**
 * Fallback Lucide icons for repository formats.
 */
const FORMAT_ICONS: Record<string, any> = {
  maven2: Package,
  npm: LucideBox,
  docker: Container,
  nuget: Archive,
  pypi: FileCode,
  rubygems: Gem,
  raw: Folder,
  helm: Layers,
  go: Terminal,
  yum: FileArchive,
  apt: FileArchive,
  conan: Boxes,
  conda: Cloud,
  cocoapods: HardDrive,
  gitlfs: GitBranch,
  p2: Workflow,
  r: FileCode,
  composer: Package,
  cargo: Package,
  huggingface: Cloud,
  terraform: Layers,
  alpine: Package,
  swift: Code,
  pub: Package,
};

/**
 * Icons for repository types (hosted, proxy, group).
 */
const TYPE_ICONS: Record<string, any> = {
  proxy: Cloud,
  hosted: Database,
  group: FolderSync,
};

export interface FormatIconProps {
  format: string;
  type?: 'hosted' | 'proxy' | 'group';
  size?: number;
  className?: string;
  useBrandLogo?: boolean;
}

/**
 * FormatIcon - Renders an official brand logo or fallback icon for a repo format.
 */
export function FormatIcon({
  format,
  type,
  size = 32,
  className = '',
  useBrandLogo = true,
}: FormatIconProps): JSX.Element {
  const logoUrl = useBrandLogo ? FORMAT_LOGOS[format] : null;
  const FallbackIcon = FORMAT_ICONS[format] || Package;
  const TypeIcon = type ? TYPE_ICONS[type] : null;

  return (
    <div
      className={`format-icon-tile ${className}`}
      style={{
        width: size,
        height: size,
        display: 'inline-flex',
        position: 'relative',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: '100%', height: '100%' }}>
        {logoUrl ? (
          <img
            src={logoUrl}
            alt={format}
            className="format-icon-tile__logo"
            style={{ maxWidth: size, maxHeight: size, width: 'auto', height: 'auto', objectFit: 'contain' }}
          />
        ) : (
          <FallbackIcon size={size} className="format-icon-tile__fallback" />
        )}
      </div>

      {type && TypeIcon && (
        <div
          className={`format-icon-tile__type-badge format-icon-tile__type-badge--${type}`}
          style={{
            position: 'absolute',
            bottom: '-4px',
            right: '-4px'
          }}
        >
          <TypeIcon size={12} />
        </div>
      )}
    </div>
  );
}

export default FormatIcon;
