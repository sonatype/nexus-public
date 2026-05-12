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

import React, { useMemo } from 'react';
import { Box, Text, Flex } from '@radix-ui/themes';
import { Trash2, ExternalLink } from 'lucide-react';

import {
  SettingsFormSection,
  SettingsSelect,
} from '../../../../shared/form';

import {
  RepositoryFormData,
  RepositoryFormErrors,
  CleanupPolicy,
} from '../types';

import './CleanupFacet.scss';

interface CleanupFacetProps {
  formData: RepositoryFormData;
  onChange: (updates: Partial<RepositoryFormData>) => void;
  onNestedChange: <K extends keyof RepositoryFormData>(
    key: K,
    updates: Partial<RepositoryFormData[K]>
  ) => void;
  errors?: RepositoryFormErrors;
  cleanupPolicies: CleanupPolicy[];
}

/**
 * CleanupFacet - Cleanup policy selection for repositories
 */
export function CleanupFacet({
  formData,
  onChange,
  onNestedChange,
  errors,
  cleanupPolicies,
}: CleanupFacetProps) {
  const selectedPolicies = formData.cleanup?.policyNames || [];

  // Filter to only show policies matching this repository's format,
  // and exclude already-selected policies
  const repoFormat = formData.format || '';
  const availablePolicies = useMemo(() => {
    return cleanupPolicies.filter(
      (policy) =>
        policy.format === repoFormat &&
        !selectedPolicies.includes(policy.name)
    );
  }, [cleanupPolicies, selectedPolicies, repoFormat]);

  const handleAddPolicy = (policyName: string) => {
    if (!policyName) return;
    
    onNestedChange('cleanup', {
      policyNames: [...selectedPolicies, policyName],
    });
  };

  const handleRemovePolicy = (policyName: string) => {
    const newPolicies = selectedPolicies.filter((p) => p !== policyName);
    
    if (newPolicies.length === 0) {
      onChange({ cleanup: null });
    } else {
      onNestedChange('cleanup', { policyNames: newPolicies });
    }
  };

  const getPolicyDetails = (policyName: string) => {
    return cleanupPolicies.find((p) => p.name === policyName);
  };

  return (
    <SettingsFormSection title="Cleanup">
      <Box className="cleanup-facet">
        <Text size="2" className="cleanup-facet__help">
          Apply cleanup policies to automatically remove components from this repository.
          {' '}
          <a
            href="#preview/admin/repository/cleanuppolicies"
            className="cleanup-facet__link"
          >
            Manage Cleanup Policies
            <ExternalLink size={12} />
          </a>
        </Text>

        {/* Add policy dropdown */}
        <SettingsSelect
          name="cleanup-addPolicy"
          label="Cleanup Policies"
          value=""
          onChange={handleAddPolicy}
          options={[
            { value: '', label: 'Add a cleanup policy...' },
            ...availablePolicies.map((policy) => ({
              value: policy.name,
              label: policy.name,
            })),
          ]}
        />

        {/* Selected policies */}
        {selectedPolicies.length > 0 && (
          <Box className="cleanup-facet__policies">
            {selectedPolicies.map((policyName) => {
              const policy = getPolicyDetails(policyName);
              return (
                <Flex
                  key={policyName}
                  align="center"
                  justify="between"
                  className="cleanup-facet__policy"
                >
                  <Box>
                    <Text size="2" weight="medium">{policyName}</Text>
                    {policy?.notes && (
                      <Text size="1" className="cleanup-facet__policy-notes">
                        {policy.notes}
                      </Text>
                    )}
                  </Box>
                  <button
                    type="button"
                    onClick={() => handleRemovePolicy(policyName)}
                    className="cleanup-facet__remove-btn"
                    title="Remove policy"
                    aria-label="Remove policy"
                  >
                    <Trash2 size={14} />
                  </button>
                </Flex>
              );
            })}
          </Box>
        )}

        {selectedPolicies.length === 0 && availablePolicies.length === 0 && (
          <Box className="cleanup-facet__empty">
            <Text size="2">No cleanup policies available for {repoFormat || 'this format'}</Text>
          </Box>
        )}
      </Box>
    </SettingsFormSection>
  );
}

export default CleanupFacet;

