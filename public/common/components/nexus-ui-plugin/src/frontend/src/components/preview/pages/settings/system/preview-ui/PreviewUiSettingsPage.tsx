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

import React, { useCallback, useState } from 'react';
import { AlertDialog, Box, Card, Flex, Text, Heading, Switch, Button } from '@radix-ui/themes';
import { AlertCircle, Info } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';
import { SettingsButton } from '../../../../shared/form';
import { PageHeader, clearDirtyState } from '../../../../shared';
import { usePreviewUiSettingsForm } from './usePreviewUiSettingsForm';
import { PREVIEW_UI_SETTINGS_FORM_ID } from './previewUiSettingsFormMachine';
import PreviewUiStrings from '../../../../constants/pages/admin/system/PreviewUiStrings';

import './PreviewUiSettingsPage.scss';

const navigateTo = (path: string) => {
  window.location.hash = path;
};

const UI_STRINGS = PreviewUiStrings.PREVIEW_UI_SETTINGS;

export default function PreviewUiSettingsPage() {
  const canUpdate = ExtJS.checkPermission('nexus:settings:update');
  const isCloud = ExtJS.state?.()?.getValue?.('isCloud', false) ?? false;
  const form = usePreviewUiSettingsForm();
  const [discardDialogOpen, setDiscardDialogOpen] = useState(false);

  const isDirty = !form.isPristine;
  const isSubmitDisabled = form.isSaving || form.isPristine || form.hasValidationErrors;

  const handleSave = useCallback((e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!canUpdate || isSubmitDisabled) {
      return;
    }

    form.submit();
  }, [canUpdate, form, isSubmitDisabled]);

  const handleDiscard = useCallback(() => {
    if (!isDirty) {
      clearDirtyState(PREVIEW_UI_SETTINGS_FORM_ID);
      form.reset();
      return;
    }

    setDiscardDialogOpen(true);
  }, [form, isDirty]);

  const handleDiscardConfirm = useCallback(() => {
    setDiscardDialogOpen(false);
    clearDirtyState(PREVIEW_UI_SETTINGS_FORM_ID);
    form.reset();
  }, [form]);

  if (form.isLoading) {
    return (
      <Box className="preview-ui-settings-page preview-ui-settings-page--loading" data-testid="preview-ui-settings-form">
        <Box className="preview-ui-settings-page__header">
          <PageHeader
        title={UI_STRINGS.title}
        description={UI_STRINGS.description}
        breadcrumbs={[
          { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
          { label: UI_STRINGS.title },
        ]}
      />
        </Box>
        <Box className="preview-ui-settings-page__loading">
          <Text color="gray">{UI_STRINGS.ACTIONS.loading}</Text>
        </Box>
      </Box>
    );
  }

  return (
    <form
      className="preview-ui-settings-page"
      onSubmit={handleSave}
      noValidate
      autoComplete="off"
      data-testid="preview-ui-settings-form"
      aria-busy={form.isSaving}
    >
      <Box className="preview-ui-settings-page__header">
        <PageHeader
        title={UI_STRINGS.title}
        description={UI_STRINGS.description}
        breadcrumbs={[
          { label: 'Settings', onClick: () => navigateTo('#preview/admin/settings') },
          { label: UI_STRINGS.title },
        ]}
      />
      </Box>

      {canUpdate && (
        <Box className="preview-ui-settings-page__action-bar">
          <Flex gap="2" align="center" className="preview-ui-settings-page__action-bar-extra">
            {form.hasValidationErrors && (
              <Flex gap="1" align="center" className="preview-ui-settings-page__action-bar-error">
                <AlertCircle size={14} />
                <Text size="1" weight="medium">
                  {Object.values(form.validationErrors).find(Boolean)}
                </Text>
              </Flex>
            )}
            {form.saveError && !form.hasValidationErrors && (
              <Flex gap="1" align="center" className="preview-ui-settings-page__action-bar-error">
                <AlertCircle size={14} />
                <Text size="1" weight="medium">{String(form.saveError)}</Text>
              </Flex>
            )}
          </Flex>

          <Flex gap="3" align="center">
            {isDirty && !form.saveError && (
              <Text size="1" className="preview-ui-settings-page__unsaved">
                {UI_STRINGS.ACTIONS.unsavedChanges}
              </Text>
            )}
            <SettingsButton
              type="button"
              variant="secondary"
              onClick={handleDiscard}
              disabled={form.isSaving}
              testId="form-cancel"
            >
              {UI_STRINGS.ACTIONS.discard}
            </SettingsButton>
            <SettingsButton
              type="submit"
              variant="primary"
              disabled={isSubmitDisabled}
              loading={form.isSaving}
              testId="form-submit"
            >
              {UI_STRINGS.ACTIONS.save}
            </SettingsButton>
          </Flex>
        </Box>
      )}

      <Box className="preview-ui-settings-page__scroll">
        <Box className="preview-ui-settings-page__inner">
          <Text as="p" color="gray" className="preview-ui-settings-page__intro">
            {UI_STRINGS.description}
          </Text>

          <Flex className="preview-ui-settings-page__info-banner" p="3" gap="2" align="center">
            <Info size={16} />
            <Text size="2">{UI_STRINGS.INFO.reloadNote}</Text>
          </Flex>

          {/* NEXUS-52024: Disable Classic UI card hidden until Nexus One UI reaches Classic UI feature parity */}

          <Flex direction="column" gap="4">
            <Card>
              <Heading size="3" mb="3">{UI_STRINGS.ACCESS_CONTROL.title}</Heading>
              <Flex direction="column" gap="3">
                {!isCloud && (
                  <Flex justify="between" align="start" gap="4">
                    <Box style={{ flex: 1, minWidth: 0 }}>
                      <Text weight="bold">{UI_STRINGS.ANONYMOUS.label}</Text>
                      <Text as="p" size="2" color="gray">
                        {UI_STRINGS.ANONYMOUS.description}
                      </Text>
                    </Box>
                    <Switch
                      data-testid="preview-ui-switch-anonymous"
                      checked={form.checkbox('anonymousEnabled').checked}
                      onCheckedChange={form.checkbox('anonymousEnabled').onChange}
                      disabled={!canUpdate}
                    />
                  </Flex>
                )}
                <Flex justify="between" align="start" gap="4">
                  <Box style={{ flex: 1, minWidth: 0 }}>
                    <Text weight="bold">{UI_STRINGS.LOGGEDIN.label}</Text>
                    <Text as="p" size="2" color="gray">
                      {UI_STRINGS.LOGGEDIN.description}
                    </Text>
                  </Box>
                  <Switch
                    data-testid="preview-ui-switch-logged-in"
                    checked={form.checkbox('loggedInEnabled').checked}
                    onCheckedChange={form.checkbox('loggedInEnabled').onChange}
                    disabled={!canUpdate}
                  />
                </Flex>
              </Flex>
            </Card>

            <Card>
              <Heading size="3" mb="3">{UI_STRINGS.ROLLOUT.title}</Heading>
              <Flex direction="column" gap="3">
                <Flex justify="between" align="start" gap="4">
                  <Box style={{ flex: 1, minWidth: 0 }}>
                    <Text weight="bold">{UI_STRINGS.ROLLOUT.DEFAULT_TO_PREVIEW.label}</Text>
                    <Text as="p" size="2" color="gray">
                      {UI_STRINGS.ROLLOUT.DEFAULT_TO_PREVIEW.helpText}
                    </Text>
                  </Box>
                  <Switch
                    data-testid="preview-ui-switch-default-preview"
                    checked={form.checkbox('defaultToPreviewUi').checked}
                    onCheckedChange={form.checkbox('defaultToPreviewUi').onChange}
                    disabled={!canUpdate}
                  />
                </Flex>
                <Flex justify="between" align="start" gap="4">
                  <Box style={{ flex: 1, minWidth: 0 }}>
                    <Text weight="bold">{UI_STRINGS.ROLLOUT.DISABLE_SWITCH_FEEDBACK.label}</Text>
                    <Text as="p" size="2" color="gray">
                      {UI_STRINGS.ROLLOUT.DISABLE_SWITCH_FEEDBACK.helpText}
                    </Text>
                  </Box>
                  <Switch
                    data-testid="preview-ui-switch-disable-feedback"
                    checked={form.checkbox('disableSwitchFeedback').checked}
                    onCheckedChange={form.checkbox('disableSwitchFeedback').onChange}
                    disabled={!canUpdate}
                  />
                </Flex>
              </Flex>
            </Card>
          </Flex>
        </Box>
      </Box>

      <AlertDialog.Root open={discardDialogOpen} onOpenChange={setDiscardDialogOpen}>
        <AlertDialog.Content maxWidth="450px">
          <AlertDialog.Title>Unsaved Changes</AlertDialog.Title>
          <AlertDialog.Description size="2">
            You have unsaved changes. Are you sure you want to leave? Your changes will be lost.
          </AlertDialog.Description>
          <Flex gap="3" mt="4" justify="end">
            <AlertDialog.Cancel>
              <Button variant="soft" color="gray">Stay</Button>
            </AlertDialog.Cancel>
            <AlertDialog.Action>
              <Button variant="solid" color="red" onClick={handleDiscardConfirm}>Leave</Button>
            </AlertDialog.Action>
          </Flex>
        </AlertDialog.Content>
      </AlertDialog.Root>
    </form>
  );
}
