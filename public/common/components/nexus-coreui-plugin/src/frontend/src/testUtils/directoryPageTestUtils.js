/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import { screen, within } from '@testing-library/react';
import { renderComponentRoute } from './renderUtils';

export async function runLinkVisiblityTest(
    routeName,
    heading,
    expectedMenu
) {
  const expectedLinkText = expectedMenu.text + ' ' + expectedMenu.description;

  await renderComponentRoute(routeName);

  const main = await assertPageRendered(heading);

  const list = within(main).getByRole('list');
  expect(list).toBeVisible();

  expect(within(list)
      .getByRole('link', { name: expectedLinkText })
  ).toBeVisible();
}

export async function runLinkNotVisibleTest(routeName, heading, expectedMenu) {
  const expectedLinkText = expectedMenu.text + ' ' + expectedMenu.description;

  await renderComponentRoute(routeName);

  const main = await assertPageRendered(heading);

  expect(within(main)
      .queryByRole('link', { name: expectedLinkText })
  ).not.toBeInTheDocument();
}

export async function runDirectoryPage404Test(routeName) {
  await renderComponentRoute(routeName);

  const main = await screen.getByRole('main');

  // Should show 404 page
  expect(within(main).getByRole('heading', { name: '404' })).toBeVisible();
  expect(within(main).getByRole('heading', { name: 'RESOURCE NOT FOUND' })).toBeVisible();
}

async function assertPageRendered(heading) {
  const main = await screen.getByRole('main');
  within(main).getByRole('heading', { name: heading })

  return main;
}
