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

/**
 * System Information types for Preview UI
 */

export interface SystemInfoSection {
  [key: string]: string | number | boolean | null | undefined;
}

export interface SystemInformation {
  'nexus-status'?: SystemInfoSection;
  'nexus-node'?: SystemInfoSection;
  'nexus-license'?: SystemInfoSection;
  'nexus-configuration'?: SystemInfoSection;
  'nexus-properties'?: SystemInfoSection;
  'system-time'?: SystemInfoSection;
  'system-properties'?: SystemInfoSection;
  'system-environment'?: SystemInfoSection;
  'system-runtime'?: SystemInfoSection;
  'system-network'?: SystemInfoSection;
  'system-filestores'?: SystemInfoSection;
  [key: string]: SystemInfoSection | undefined;
}

export interface HANode {
  nodeId: string;
  friendlyName?: string;
  hostname?: string;
  socketAddress?: string;
  local?: boolean;
}

export interface HASystemInformation {
  [nodeId: string]: SystemInformation;
}

export interface SystemInfoPageProps {
  className?: string;
}

export interface SystemInfoSectionProps {
  title: string;
  data: SystemInfoSection;
  /** Default open state for uncontrolled mode */
  defaultOpen?: boolean;
  /** Controlled open state (takes precedence over defaultOpen) */
  open?: boolean;
  /** Callback when section is toggled (for controlled mode) */
  onToggle?: (isOpen: boolean) => void;
  className?: string;
}

export interface NodeSelectorProps {
  nodes: HANode[];
  selectedNode: string | null;
  onNodeSelect: (nodeId: string) => void;
  className?: string;
}

/**
 * Format section title from key
 * e.g., 'nexus-status' -> 'Nexus Status'
 */
export function formatSectionTitle(key: string): string {
  return key
    .split('-')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

/**
 * Format value for display
 */
export function formatValue(value: string | number | boolean | null | undefined): string {
  if (value === null || value === undefined) {
    return '-';
  }
  if (typeof value === 'boolean') {
    return value ? 'Yes' : 'No';
  }
  return String(value);
}


