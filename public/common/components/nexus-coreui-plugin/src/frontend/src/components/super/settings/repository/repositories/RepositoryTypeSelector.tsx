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

import React, { useState, useEffect, useMemo } from 'react';
import { Box, Flex, Text, Heading } from '@radix-ui/themes';
import { Loader2, Search, ChevronRight, Database, Server, FolderSync } from 'lucide-react';

import { SettingsButton, SettingsAlert } from '../../../shared/form';
import { useRepositoriesApi } from './useRepositoriesApi';
import { FormatIcon } from '@/components/super/settings/repository/repositories/components/FormatIcon';
import { 
  Recipe, 
  RepositoryTypeSelectorProps, 
  RepositoryType, 
  FORMAT_LABELS, 
  TYPE_LABELS 
} from './types';
import { 
  getFormatDescription, 
  getTypeDescription, 
  getRecipeDescription 
} from './repositoryTypeDescriptions';

import './RepositoryTypeSelector.scss';

/**
 * RepositoryTypeSelector - Multi-stage wizard for selecting technology and architecture.
 * 
 * Stage 1: Select Format (Technology)
 * Stage 2: Select Type (Hosted, Proxy, Group)
 */
export function RepositoryTypeSelector({
  onSelect,
  onCancel,
  mode,
  hideActions = false,
  selectedFormat: initialFormat,
  onFormatSelect,
  onSelectionChange
}: RepositoryTypeSelectorProps) {
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedFormat, setSelectedFormat] = useState<string | null>(initialFormat || null);
  const [selectedType, setSelectedType] = useState<RepositoryType | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const { fetchRecipes } = useRepositoriesApi();

  // Load recipes on mount
  useEffect(() => {
    const loadRecipes = async () => {
      setLoading(true);
      try {
        const data = await fetchRecipes();
        setRecipes(data);
      } catch (err: unknown) {
        setError(err instanceof Error ? err.message : 'Failed to load recipes');
      } finally {
        setLoading(false);
      }
    };
    loadRecipes();
  }, [fetchRecipes]);

  // Sync internal state with prop
  useEffect(() => {
    if (initialFormat !== undefined) {
      setSelectedFormat(initialFormat);
      if (!initialFormat) {
        setSelectedType(null);
      }
    }
  }, [initialFormat]);

  // Determine effective mode: prop mode or internal state
  const effectiveMode = mode || (selectedFormat ? 'type' : 'format');

  // Unique list of formats from recipes
  const availableFormats = useMemo(() => {
    const formats = new Set(recipes.map(r => r.format));
    let result = Array.from(formats).sort((a, b) => 
      (FORMAT_LABELS[a] || a).localeCompare(FORMAT_LABELS[b] || b)
    );

    if (searchTerm) {
      const q = searchTerm.toLowerCase();
      result = result.filter(f => 
        (FORMAT_LABELS[f] || f).toLowerCase().includes(q) || 
        f.toLowerCase().includes(q) ||
        getFormatDescription(f).toLowerCase().includes(q)
      );
    }
    return result;
  }, [recipes, searchTerm]);

  // Types available for the selected format
  const availableTypesForFormat = useMemo(() => {
    if (!selectedFormat) return [];
    return recipes
      .filter(r => r.format === selectedFormat)
      .map(r => r.type)
      .sort((a, b) => {
        const order = { proxy: 0, hosted: 1, group: 2 };
        return (order[a] ?? 99) - (order[b] ?? 99);
      });
  }, [selectedFormat, recipes]);

  const handleFormatClick = (format: string) => {
    setSelectedFormat(format);
    setSelectedType(null);
    if (onFormatSelect) onFormatSelect(format);
    if (onSelectionChange) {
      // Notify parent that a format is picked
      onSelectionChange(true, { format, type: null as any });
    }
  };

  const handleTypeClick = (type: RepositoryType) => {
    setSelectedType(type);
    const recipe = recipes.find(r => r.format === selectedFormat && r.type === type);
    if (recipe && onSelectionChange) {
      // Both format and type selected
      onSelectionChange(true, recipe);
    }
    // If not in external wizard control, advance now
    if (!hideActions && recipe) {
      onSelect(recipe);
    }
  };

  if (loading) {
    return (
      <Flex align="center" justify="center" p="8">
        <Loader2 className="animate-spin" />
        <Text ml="2">Loading repository recipes...</Text>
      </Flex>
    );
  }

  if (error) {
    return (
      <Box p="4">
        <SettingsAlert type="error">{error}</SettingsAlert>
      </Box>
    );
  }

  // --- STAGE 2: SELECT TYPE ---
  if (effectiveMode === 'type' && selectedFormat) {
    return (
      <Box className="repository-type-selector">
        <Box mb="6">
          <Flex align="center" gap="3" mt={mode ? '0' : '2'}>
            <FormatIcon format={selectedFormat} size={48} />
            <Box>
              <Heading size="5">{FORMAT_LABELS[selectedFormat] || selectedFormat}</Heading>
              <Text size="2" color="gray">{getFormatDescription(selectedFormat)}</Text>
            </Box>
          </Flex>
        </Box>

        <Box className="repository-type-selector__grid repository-type-selector__grid--types">
          {availableTypesForFormat.map(type => {
            const isSelected = selectedType === type;
            const Icon = type === 'proxy' ? Server : type === 'hosted' ? Database : FolderSync;
            
            return (
              <button
                type="button"
                key={type}
                className={`repository-type-selector__card ${isSelected ? 'repository-type-selector__card--selected' : ''}`}
                onClick={() => handleTypeClick(type as RepositoryType)}
              >
                <div style={{ display: 'flex', alignItems: 'start', gap: '16px' }}>
                  <div className={`repository-type-selector__type-icon repository-type-selector__type-icon--${type}`}>
                    <Icon size={24} />
                  </div>
                  <Box style={{ flex: 1 }}>
                    <Text weight="bold" size="3" mb="1" style={{ display: 'block' }}>
                      {TYPE_LABELS[type] || type}
                    </Text>
                    <Text size="2" color="gray" style={{ lineHeight: '1.4', display: 'block' }}>
                      {getTypeDescription(type)}
                    </Text>
                    <Text size="1" color="gray" mt="2" style={{ fontStyle: 'italic', display: 'block' }}>
                      {getRecipeDescription(selectedFormat, type)}
                    </Text>
                  </Box>
                </div>
              </button>
            );
          })}
        </Box>
      </Box>
    );
  }

  // --- STAGE 1: SELECT FORMAT ---
  return (
    <Box className="repository-type-selector">
      <Box className="repository-type-selector__search">
        <div className="repository-type-selector__search-wrapper">
          <Search size={16} className="repository-type-selector__search-icon" />
          <input
            type="text"
            placeholder="Search technology (e.g. Docker, npm, Maven)..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="repository-type-selector__search-input"
            autoFocus
            autoComplete="off"
          />
        </div>
        <Text size="2" color="gray">
          {availableFormats.length} formats available
        </Text>
      </Box>

      <Box className="repository-type-selector__grid">
        {availableFormats.map(format => (
          <button
            type="button"
            key={format}
            className={`repository-type-selector__format-card ${selectedFormat === format ? 'repository-type-selector__format-card--selected' : ''}`}
            onClick={() => handleFormatClick(format)}
          >
            <FormatIcon format={format} size={48} />
            <Text weight="medium" mt="2" style={{ textAlign: 'center' }}>
              {FORMAT_LABELS[format] || format}
            </Text>
          </button>
        ))}
      </Box>
    </Box>
  );
}

export default RepositoryTypeSelector;
