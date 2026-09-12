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

import React, { useLayoutEffect, useState } from 'react';
import { AlertDialog, Button, Flex } from '@radix-ui/themes';

/**
 * Nexus One unsaved changes dialog for Preview UI router navigation.
 *
 * Registers window.showPreviewUnsavedDialog so the shared router guard
 * (createRouter.js) can prompt with a Radix dialog on preview.* routes.
 * Mounted once per app root (coreui and cloudui).
 */
let resolveUnsavedDialog = null;
let setUnsavedDialogOpen = () => {};

export default function PreviewUnsavedDialog() {
  const [open, setOpen] = useState(false);

  // useLayoutEffect so window.showPreviewUnsavedDialog exists before paint; UI-Router
  // onBefore can run in the same turn as a nav click and would otherwise fall back to
  // the classic modal.
  useLayoutEffect(() => {
    setUnsavedDialogOpen = setOpen;
    window.showPreviewUnsavedDialog = () => {
      return new Promise((resolve) => {
        resolveUnsavedDialog = resolve;
        setOpen(true);
      });
    };
    return () => {
      delete window.showPreviewUnsavedDialog;
    };
  }, []);

  const handleLeave = () => {
    if (resolveUnsavedDialog) {
      resolveUnsavedDialog(true);
      resolveUnsavedDialog = null;
    }
    // Don't call setOpen(false) - AlertDialog.Action closes automatically
  };

  const handleStay = () => {
    if (resolveUnsavedDialog) {
      resolveUnsavedDialog(false);
      resolveUnsavedDialog = null;
    }
    // Don't call setOpen(false) - AlertDialog.Cancel closes automatically
  };

  return (
    <AlertDialog.Root open={open} onOpenChange={setOpen}>
      <AlertDialog.Content maxWidth="450px">
        <AlertDialog.Title>Unsaved Changes</AlertDialog.Title>
        <AlertDialog.Description size="2">
          You have unsaved changes. Are you sure you want to leave? Your changes will be lost.
        </AlertDialog.Description>
        <Flex gap="3" mt="4" justify="end">
          <AlertDialog.Cancel>
            <Button variant="soft" color="gray" onClick={handleStay}>Stay</Button>
          </AlertDialog.Cancel>
          <AlertDialog.Action>
            <Button variant="solid" color="red" onClick={handleLeave}>Leave</Button>
          </AlertDialog.Action>
        </Flex>
      </AlertDialog.Content>
    </AlertDialog.Root>
  );
}
