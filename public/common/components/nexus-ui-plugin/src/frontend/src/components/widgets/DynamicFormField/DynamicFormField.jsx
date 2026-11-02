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
import { equals, mapObjIndexed, values } from 'ramda';
import { assign, createMachine } from 'xstate';
import { useMachine } from '@xstate/react';
import { NxFormSelect, NxTextInput, NxCheckbox, NxReadOnly, NxFormGroup } from '@sonatype/react-shared-components';
import { NxStatefulTransferList } from '@sonatype/react-shared-components';

import FormUtils from '../../../interface/FormUtils';
import ValidationUtils from '../../../interface/ValidationUtils';
import UIStrings from '../../../constants/UIStrings';
import ExtAPIUtils from '../../../interface/ExtAPIUtils';
import { NxLoadWrapper } from '@sonatype/react-shared-components';

/**
 * @deprecated Classic UI only; unreachable from the Preview UI. Left on XState v4 semantics
 * pending Classic UI deletion (NEXUS-55299) ahead of the XState v5 upgrade (NEXUS-51654).
 */
export default function DynamicFormField({ current, dynamicProps, id, initialValue, readOnly, onChange }) {
  // Note: storeApi, storeFilters, idMapping, nameMapping are legacy ExtJS fields
  // These will be removed once all ExtJS descriptors are migrated to React

  // Handle read-only fields - just display the value as text
  if (dynamicProps.readOnly || readOnly) {
    return readOnlyComponent(current, dynamicProps, id, initialValue);
  } else {
    if (dynamicProps.type === 'checkbox') {
      return editComponent(current, dynamicProps, id, initialValue, onChange)
    } else {
      return (
        <NxFormGroup
          label={dynamicProps.label}
          isRequired={dynamicProps.required}
          sublabel={dynamicProps.helpText}
        >
          {editComponent(current, dynamicProps, id, initialValue, onChange)}
        </NxFormGroup>
      );
    }
  }
}

function readOnlyComponent(current, dynamicProps, id, initialValue) {
  switch (dynamicProps.type) {
    case "string":
    case "number":
    case "url":
    case "combobox":
    case "text-area":
      return simpleReadOnlyComponent(
        id,
        dynamicProps.label || id,
        current.context.data[id] || initialValue || "",
        dynamicProps
      )
    case "checkbox":
      return simpleReadOnlyComponent(
        id,
        dynamicProps.label || id,
        current.context.data[id] || initialValue || false,
        dynamicProps
      )
    case "password":
      return simpleReadOnlyComponent(
        id,
        dynamicProps.label || id,
        "********",
        dynamicProps
      );
    case "itemselect":
      return listReadOnlyComponent(
        id,
        dynamicProps.label || id,
        (Array.isArray(current.context.data[id]) &&
        current.context.data[id].length > 0
          ? current.context.data[id]
          : null) ||
          initialValue ||
          [],
        dynamicProps
      );
    default: {
      console.warn(`form field type=${dynamicProps.type} is unknown`);
      return <div />;
    }
  }
}

function editComponent(current, dynamicProps, id, initialValue, onChange) {
  switch (dynamicProps.type) {
    case 'string': {
      const { fieldProps, className } = getFieldPropsAndClassName(id, current, initialValue, dynamicProps);
      const textInputProps = getTextInputProps(fieldProps, className, dynamicProps, 'text', onChange);

      return <NxTextInput {...textInputProps} />;
    }
    case 'itemselect': {
      return (
        <ItemSelectField
          id={id}
          current={current}
          initialValue={initialValue}
          dynamicProps={dynamicProps}
          onChange={onChange}
        />
      );
    }
    case 'combobox': {
      return (
        <ComboboxField
          id={id}
          current={current}
          initialValue={initialValue}
          dynamicProps={dynamicProps}
          onChange={onChange}
        />
      );
    }
    case 'number': {
      const { fieldProps, className } = getFieldPropsAndClassName(id, current, initialValue, dynamicProps);

      // Filter input to only allow digits
      const handleNumberChange = value => {
        const numericValue = value.replace(/[^\d]/g, '');
        onChange(fieldProps.name, numericValue);
      };

      const textInputProps = getTextInputProps(
        fieldProps,
        className,
        dynamicProps,
        'text',
        onChange,
        handleNumberChange
      );
      return <NxTextInput {...textInputProps} />;
    }
    case 'checkbox': {
      const checkboxProps = FormUtils.checkboxProps(id, current, initialValue || false);
      return (
        <NxCheckbox
          {...checkboxProps}
          disabled={dynamicProps.disabled}
          readOnly={dynamicProps.readOnly}
          onChange={value => onChange(checkboxProps.name, value)}
        >
          {dynamicProps.label}
        </NxCheckbox>
      );
    }
    case 'url': {
      const { fieldProps, className } = getFieldPropsAndClassName(id, current, initialValue, dynamicProps);

      // Validate URL using existing ValidationUtils
      const validateUrl = value => {
        if (!value) return null;
        return ValidationUtils.validateIsUrl(value);
      };

      const textInputProps = getTextInputProps(fieldProps, className, dynamicProps, 'text', onChange);
      return (
        <NxTextInput
          {...textInputProps}
          validatable={true}
          validationErrors={validateUrl(fieldProps.value) || fieldProps.validationErrors}
        />
      );
    }
    case 'password': {
      const { fieldProps, className } = getFieldPropsAndClassName(id, current, initialValue, dynamicProps);
      const textInputProps = getTextInputProps(fieldProps, className, dynamicProps, 'password', onChange);

      return <NxTextInput {...textInputProps} />;
    }
    case 'text-area': {
      const { fieldProps, className } = getFieldPropsAndClassName(id, current, initialValue, dynamicProps);
      const textInputProps = getTextInputProps(fieldProps, className, dynamicProps, 'textarea', onChange);

      return <NxTextInput {...textInputProps} />;
    }
    default: {
      console.warn(`form field type=${dynamicProps.type} is unknown`);
      return <div />;
    }
  }
}

function simpleReadOnlyComponent(id, label, value, dynamicProps = {}) {
  label = dynamicProps?.label || label;
  value = (typeof value === 'boolean' ? (value ? 'Enabled' : 'Disabled') : value) || 'NA';
  value = (value === 'true' && 'Enabled' || value === 'false' && 'Disabled') || value;

  return (
    <NxReadOnly>
      <NxReadOnly.Label id={id}>{label}</NxReadOnly.Label>
      <NxReadOnly.Data aria-labeledby={id}>{value}</NxReadOnly.Data>
    </NxReadOnly>
  );
}

function listReadOnlyComponent(id, label, values, dynamicProps = {}) {
  label = dynamicProps?.label || label;

  return (
    <NxReadOnly>
      <NxReadOnly.Label id={id}>{label}</NxReadOnly.Label>
      {Array.isArray(values) && values.length > 0 && values.map((val, index) => (
        <NxReadOnly.Data key={index} aria-labeledby={id}>{val}</NxReadOnly.Data>
      )) || <NxReadOnly.Data>NA</NxReadOnly.Data>}
    </NxReadOnly>
  );
}

// Helper function to get common field props and className
function getFieldPropsAndClassName(id, current, initialValue, dynamicProps) {
  const fieldProps = FormUtils.fieldProps(id, current, initialValue || '');
  const className = dynamicProps.attributes.long ? 'nx-text-input--long' : '';
  return { fieldProps, className };
}

// Helper function to get common NxTextInput props
function getTextInputProps(fieldProps, className, dynamicProps, type, onChange, customOnChange) {
  return {
    ...fieldProps,
    className,
    type,
    disabled: dynamicProps.disabled,
    readOnly: dynamicProps.readOnly,
    autoComplete: dynamicProps.allowAutocomplete || 'off',
    onChange: customOnChange || (value => onChange(fieldProps.name, value)),
  };
}

// Helper function to get field errors following FormUtils.fieldProps pattern
function getFieldErrors(id, current, initialValue = '') {
  const fieldProps = FormUtils.fieldProps(id, current, initialValue);
  const validationErrors = current.context.validationErrors || {};
  const saveErrors = current.context.saveErrors || {};
  const saveErrorData = current.context.saveErrorData || {};

  const saveError = saveErrors[id];
  const savedValue = saveErrorData[id];
  const currentValue = current.context.data[id] ?? initialValue;
  const validationError = validationErrors[id];

  // Error display logic matching FormUtils.fieldProps exactly
  // Use Ramda's equals for deep equality comparison (handles arrays and objects)
  let errors = null;
  const valuesMatch = equals(savedValue, currentValue);
  if (Boolean(savedValue) && saveError && valuesMatch) {
    errors = saveError;
  } else if (Boolean(validationError)) {
    errors = validationError;
  }

  return { errors, validationError, fieldProps };
}

// Helper function to convert storeFilters to ExtAPI format
function convertStoreFiltersToApiOptions(storeFilters) {
  return storeFilters
    ? {
        filter: Object.entries(storeFilters).map(([key, value]) => ({
          property: key,
          value: value,
        })),
      }
    : null;
}

// Component to handle combobox field type with support for both static options and storeApi
function ComboboxField({ id, current, initialValue, dynamicProps, onChange }) {
  const { storeApi, storeFilters, idMapping, nameMapping, attributes, required } = dynamicProps;
  const selectedValue = current.context.data[id] || initialValue || '';
  const { errors, fieldProps } = getFieldErrors(id, current, initialValue || '');

  // Validate required field
  const validateCombobox = value => {
    if (required && (!value || value === '')) {
      return UIStrings.ERROR.FIELD_REQUIRED;
    }
    return null;
  };

  const displayError = fieldProps.isPristine ? null : validateCombobox(selectedValue) || errors;

  // Priority: Use attributes.options if available, otherwise fall back to storeApi
  if (attributes?.options) {
    return (
      <>
        <NxFormSelect
          {...fieldProps}
          validatable={true}
          validationErrors={displayError}
          onChange={value => onChange(id, value)}
        >
          <option />
          {values(
            mapObjIndexed(
              (v, k) => (
                <option key={k} value={k}>
                  {v}
                </option>
              ),
              attributes.options
            )
          )}
        </NxFormSelect>
        {displayError && (
          <div className='nx-field-validation-message' role='alert'>
            {displayError}
          </div>
        )}
      </>
    );
  }

  // Fallback to storeApi if attributes.options is not provided
  if (storeApi) {
    const [action, method] = storeApi.split('.');
    const idProp = idMapping || 'id';
    const nameProp = nameMapping || 'name';

    // Convert storeFilters to ExtAPI format if provided
    const apiOptions = convertStoreFiltersToApiOptions(storeFilters);

    // Create a custom machine to load data from storeApi
    const [state] = useMachine(
      () =>
        createMachine(
          {
            id: 'ComboboxMachine',
            initial: 'fetching',
            states: {
              fetching: {
                invoke: {
                  src: 'fetch',
                  onDone: {
                    target: 'loaded',
                    actions: ['setData'],
                  },
                  onError: {
                    target: 'error',
                    actions: ['setError'],
                  },
                },
              },
              loaded: {},
              error: {},
            },
          },
          {
            actions: {
              setData: assign({
                data: (_, event) => {
                  const apiData = event.data || [];
                  // Convert array of objects to options map {id: name, id2: name2}
                  const optionsMap = {};
                  apiData.forEach(item => {
                    const itemId = item[idProp];
                    const itemName = item[nameProp];
                    if (itemId !== undefined && itemName !== undefined) {
                      optionsMap[itemId] = itemName;
                    }
                  });
                  return optionsMap;
                },
              }),
              setError: assign({
                error: (_, event) => event?.data?.message || UIStrings.ERROR.UNKNOWN,
              }),
            },
            services: {
              fetch: async () => {
                const response = await ExtAPIUtils.extAPIRequest(action, method, apiOptions);
                ExtAPIUtils.checkForError(response);
                return ExtAPIUtils.extractResult(response, []);
              },
            },
          }
        ),
      {
        context: {
          data: {},
          error: null,
        },
        devTools: false,
      }
    );

    const { data: options, error } = state.context;
    const isLoading = state.matches('fetching');

    if (error) {
      console.error('Failed to load combobox options:', error);
    }

    return (
      <>
        <NxLoadWrapper loading={isLoading} error={error}>
          <NxFormSelect
            {...fieldProps}
            validatable={true}
            validationErrors={displayError}
            onChange={value => onChange(id, value)}
          >
            <option />
            {values(
              mapObjIndexed(
                (v, k) => (
                  <option key={k} value={k}>
                    {v}
                  </option>
                ),
                options || {}
              )
            )}
          </NxFormSelect>
        </NxLoadWrapper>
        {displayError && (
          <div className='nx-field-validation-message' role='alert'>
            {displayError}
          </div>
        )}
      </>
    );
  }

  // Neither attributes.options nor storeApi provided
  console.warn('combobox field missing both attributes.options and storeApi');
  return (
    <>
      <NxFormSelect
        {...fieldProps}
        validatable={true}
        validationErrors={displayError}
        onChange={value => onChange(id, value)}
      >
        <option />
      </NxFormSelect>
      {displayError && (
        <div className='nx-field-validation-message' role='alert'>
          {displayError}
        </div>
      )}
    </>
  );
}

// Component to handle itemselect field type with support for both static options and storeApi
function ItemSelectField({ id, current, initialValue, dynamicProps, onChange }) {
  const { storeApi, storeFilters, idMapping, nameMapping, attributes } = dynamicProps;
  const selectedItems = current.context.data[id] || initialValue || [];
  const { errors, validationError, fieldProps } = getFieldErrors(id, current, initialValue || []);

  // Show errors if field is touched (not pristine) or if there's a validation error
  // Always show validation errors, even if field appears pristine (after submit, touchAllFields marks it as touched)
  // The validationErrors context is the source of truth after form submission
  const displayError = validationError ? errors : fieldProps.isPristine ? null : errors;

  // Priority: Use attributes.options if available, otherwise fall back to storeApi
  if (attributes?.options) {
    const allItems = attributes.options.map(it => ({ id: it, displayName: it }));

    return (
      <>
        <NxStatefulTransferList
          allItems={allItems}
          selectedItems={selectedItems}
          availableItemsLabel={attributes.fromTitle}
          selectedItemsLabel={attributes.toTitle}
          onChange={value => onChange(id, value)}
          allowReordering
        />
        {displayError && (
          <div className='nx-field-validation-message' role='alert'>
            {displayError}
          </div>
        )}
      </>
    );
  }

  // Fallback to storeApi if attributes.options is not provided
  if (storeApi) {
    const [action, method] = storeApi.split('.');
    const idProp = idMapping || 'id';
    const nameProp = nameMapping || 'name';

    // Convert storeFilters to ExtAPI format if provided
    const apiOptions = convertStoreFiltersToApiOptions(storeFilters);

    // Create a custom machine to load data from storeApi
    const [state] = useMachine(
      () =>
        createMachine(
          {
            id: 'ItemSelectMachine',
            initial: 'fetching',
            states: {
              fetching: {
                invoke: {
                  src: 'fetch',
                  onDone: {
                    target: 'loaded',
                    actions: ['setData'],
                  },
                  onError: {
                    target: 'error',
                    actions: ['setError'],
                  },
                },
              },
              loaded: {},
              error: {},
            },
          },
          {
            actions: {
              setData: assign({
                data: (_, event) => {
                  const apiData = event.data || [];
                  return apiData.map(item => ({
                    id: item[idProp],
                    displayName: item[nameProp],
                  }));
                },
              }),
              setError: assign({
                error: (_, event) => event?.data?.message || UIStrings.ERROR.UNKNOWN,
              }),
            },
            services: {
              fetch: async () => {
                const response = await ExtAPIUtils.extAPIRequest(action, method, apiOptions);
                ExtAPIUtils.checkForError(response);
                return ExtAPIUtils.extractResult(response, []);
              },
            },
          }
        ),
      {
        context: {
          data: [],
          error: null,
        },
        devTools: false,
      }
    );

    const { data, error } = state.context;
    const isLoading = state.matches('fetching');

    if (error) {
      console.error('Failed to load itemselect options:', error);
    }

    // Show loading state or empty state while loading
    if (isLoading) {
      return (
        <>
          <NxStatefulTransferList
            allItems={[]}
            selectedItems={selectedItems}
            availableItemsLabel={attributes?.fromTitle || 'Available'}
            selectedItemsLabel={attributes?.toTitle || 'Selected'}
            onChange={value => onChange(id, value)}
            allowReordering
          />
          {displayError && (
            <div className='nx-field-validation-message' role='alert'>
              {displayError}
            </div>
          )}
        </>
      );
    }

    return (
      <>
        <NxStatefulTransferList
          allItems={data || []}
          selectedItems={selectedItems}
          availableItemsLabel={attributes?.fromTitle || 'Available'}
          selectedItemsLabel={attributes?.toTitle || 'Selected'}
          onChange={value => onChange(id, value)}
          allowReordering
        />
        {displayError && (
          <div className='nx-field-validation-message' role='alert'>
            {displayError}
          </div>
        )}
      </>
    );
  }

  // Neither attributes.options nor storeApi provided
  console.warn('itemselect field missing both attributes.options and storeApi');
  return (
    <>
      <NxStatefulTransferList
        allItems={[]}
        selectedItems={selectedItems}
        availableItemsLabel={attributes?.fromTitle || 'Available'}
        selectedItemsLabel={attributes?.toTitle || 'Selected'}
        onChange={value => onChange(id, value)}
        allowReordering
      />
      {displayError && (
        <div className='nx-field-validation-message' role='alert'>
          {displayError}
        </div>
      )}
    </>
  );
}
