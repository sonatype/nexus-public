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
import { Box, Text } from '@radix-ui/themes';
import { ExternalLink } from 'lucide-react';

import {
  SettingsFormSection,
  SettingsSelect,
} from '../../../../shared/form';

import {
  RepositoryFormData,
  RepositoryFormErrors,
  RoutingRule,
} from '../types';

import './RoutingRuleFacet.scss';

interface RoutingRuleFacetProps {
  formData: RepositoryFormData;
  onChange: (updates: Partial<RepositoryFormData>) => void;
  errors?: RepositoryFormErrors;
  routingRules: RoutingRule[];
}

/**
 * RoutingRuleFacet - Routing rule selection for proxy/group repositories
 */
export function RoutingRuleFacet({
  formData,
  onChange,
  errors,
  routingRules,
}: RoutingRuleFacetProps) {
  const handleChange = (value: string) => {
    onChange({ routingRuleId: value || null });
  };

  const ruleOptions = routingRules.map((rule) => ({
    value: rule.name,
    label: rule.name + (rule.mode ? ` (${rule.mode})` : ''),
  }));

  return (
    <SettingsFormSection title="Routing Rule">
      <Box className="routing-rule-facet">
        <Text size="2" className="routing-rule-facet__help">
          Choose a rule to restrict some requests from being served by this repository.
        </Text>

        <SettingsSelect
          name="routingRuleId"
          label="Routing Rule"
          value={formData.routingRuleId || ''}
          onChange={handleChange}
          options={[
            { value: '', label: 'None (allow all requests)' },
            ...ruleOptions,
          ]}
          helpText="Routing rule to apply to this repository"
        />
      </Box>
    </SettingsFormSection>
  );
}

export default RoutingRuleFacet;

