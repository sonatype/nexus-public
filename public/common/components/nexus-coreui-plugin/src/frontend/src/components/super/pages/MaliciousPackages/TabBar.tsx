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
import { Badge, Button, Flex, Text } from '@radix-ui/themes';
import { FileBarChart, LayoutDashboard, Search, Shield, Trash2 } from 'lucide-react';

import { TabCounts, TabId } from './types';

interface TabBarProps {
  activeTab: TabId;
  counts: TabCounts;
  onTabChange: (tab: TabId) => void;
}

const TABS: Array<{ id: TabId; label: string; icon: typeof Search }> = [
  { id: 'overview', label: 'Overview', icon: LayoutDashboard },
  { id: 'detect', label: 'Detect', icon: Search },
  { id: 'remediate', label: 'Remediate', icon: Trash2 },
  { id: 'harden', label: 'Harden', icon: Shield },
  { id: 'report', label: 'Report', icon: FileBarChart },
];

export function TabBar({ activeTab, counts, onTabChange }: TabBarProps): React.ReactElement {
  return (
    <Flex className="tab-bar" align="center" gap="1" role="tablist" aria-label="Malicious packages sections">
      {TABS.map(({ id, label, icon: Icon }) => {
        const count = counts[id];
        const isActive = activeTab === id;
        return (
          <Button
            key={id}
            type="button"
            variant="ghost"
            className={`tab-bar__tab${isActive ? ' tab-bar__tab--active' : ''}`}
            onClick={() => onTabChange(id)}
            role="tab"
            aria-selected={isActive}
            id={`malicious-packages-tab-${id}`}
          >
            <Flex align="center" gap="2">
              <Icon size={16} aria-hidden />
              <Text as="span" size="2" weight={isActive ? 'bold' : 'regular'}>
                {label}
              </Text>
              {id !== 'overview' && (
                <Badge
                  size="1"
                  className={`tab-bar__badge${count > 0 ? ' tab-bar__badge--alert' : ''}`}
                >
                  {count}
                </Badge>
              )}
            </Flex>
          </Button>
        );
      })}
    </Flex>
  );
}
