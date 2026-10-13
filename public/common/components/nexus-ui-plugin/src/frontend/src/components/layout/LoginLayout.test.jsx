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

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { ExtJS } from "../../interface/ExtJS";
import {
  EDITION_COMMUNITY,
  EDITION_CORE,
  EDITION_ENTERPRISE,
  EDITION_ID_STATE_KEY,
  EDITION_PRO,
  EDITION_PRO_PLUS,
} from "../../interface/editionUtils";
import LoginLayout from "./LoginLayout";

const HEADER_IMG_SELECTOR = ".nxrm-login-header img";
const HEADER_LINK_SELECTOR = ".nxrm-login-header a";
const EDITION_BADGE_SELECTOR = ".nxrm-login-header__edition-badge";
const NEUTRAL_LOGO_ALT = "Sonatype Nexus Repository";

jest.mock("../../interface/ExtJS", () => ({
  ExtJS: {
    useState: jest.fn(),
    state: jest.fn(),
  },
}));

// The state values ExtJS.state().getValue(key, fallback) should return for a given render.
let stateValues = {};

function setStateValues(values) {
  stateValues = values;
}

beforeEach(() => {
  jest.clearAllMocks();
  stateValues = {};
  ExtJS.state.mockImplementation(() => ({
    getValue: jest.fn((key, fallback) =>
      Object.prototype.hasOwnProperty.call(stateValues, key) ? stateValues[key] : fallback
    ),
  }));
  // Default: run the factory just as ExtJS would once state is hydrated.
  ExtJS.useState.mockImplementation((fn) => fn());
});

function renderLayout() {
  return render(
    <LoginLayout>
      <div>Test Content</div>
    </LoginLayout>
  );
}

describe("LoginLayout", () => {
  it("renders header, main, and children", () => {
    setStateValues({ [EDITION_ID_STATE_KEY]: EDITION_PRO });

    const { container } = renderLayout();

    expect(container.querySelector(".nxrm-login-header")).toBeInTheDocument();
    expect(container.querySelector(".nxrm-login-main")).toBeInTheDocument();
    expect(screen.getByText("Test Content")).toBeInTheDocument();
  });

  describe("edition badge and neutral wordmark", () => {
    it("uses the neutral wordmark alt regardless of edition", () => {
      setStateValues({ [EDITION_ID_STATE_KEY]: EDITION_PRO });

      const { container } = renderLayout();

      expect(container.querySelector(HEADER_IMG_SELECTOR)).toHaveAttribute(
        "alt",
        NEUTRAL_LOGO_ALT
      );
    });

    it("renders the Pro+ edition badge for the pro plus edition id", () => {
      setStateValues({ [EDITION_ID_STATE_KEY]: EDITION_PRO_PLUS });

      const { container } = renderLayout();

      expect(container.querySelector(EDITION_BADGE_SELECTOR)).toHaveTextContent(
        "Pro+"
      );
    });

    it("renders the Enterprise edition badge for the enterprise edition id", () => {
      setStateValues({ [EDITION_ID_STATE_KEY]: EDITION_ENTERPRISE });

      const { container } = renderLayout();

      expect(container.querySelector(EDITION_BADGE_SELECTOR)).toHaveTextContent(
        "Enterprise"
      );
    });

    it("renders the Pro edition badge for the pro edition id", () => {
      setStateValues({ [EDITION_ID_STATE_KEY]: EDITION_PRO });

      const { container } = renderLayout();

      expect(container.querySelector(EDITION_BADGE_SELECTOR)).toHaveTextContent(
        "Pro"
      );
    });

    it("renders the Community Edition badge for the community edition id", () => {
      setStateValues({ [EDITION_ID_STATE_KEY]: EDITION_COMMUNITY });

      const { container } = renderLayout();

      expect(container.querySelector(EDITION_BADGE_SELECTOR)).toHaveTextContent(
        "Community Edition"
      );
    });

    it("renders the Core edition badge for the core edition id", () => {
      setStateValues({ [EDITION_ID_STATE_KEY]: EDITION_CORE });

      const { container } = renderLayout();

      expect(container.querySelector(EDITION_BADGE_SELECTOR)).toHaveTextContent(
        "Core"
      );
    });

    it("renders the edition as a solid green Radix Badge, not a hand-rolled span", () => {
      // Guards the review decision to use the Radix Badge instead of CSS tokens.
      setStateValues({ [EDITION_ID_STATE_KEY]: EDITION_PRO_PLUS });

      const { container } = renderLayout();
      const badge = container.querySelector(EDITION_BADGE_SELECTOR);

      expect(badge).toHaveClass("rt-Badge");
      expect(badge).toHaveClass("rt-variant-solid");
    });

    it("keeps the home link aria-label edition-free so the edition is not announced twice", () => {
      // Edition is conveyed by the badge; repeating it here would double-announce it.
      setStateValues({ [EDITION_ID_STATE_KEY]: EDITION_ENTERPRISE });

      const { container } = renderLayout();

      expect(container.querySelector(HEADER_LINK_SELECTOR)).toHaveAttribute(
        "aria-label",
        "Sonatype Nexus Repository home"
      );
    });

    it("does not render the edition badge before an edition id is published", () => {
      // Early-boot window: no editionId in state yet.
      ExtJS.useState.mockImplementation(() => undefined);

      const { container } = renderLayout();

      expect(
        container.querySelector(EDITION_BADGE_SELECTOR)
      ).not.toBeInTheDocument();
    });

    it("resolves a wordmark logo src even when editionId is missing at bootstrap", () => {
      // No editionId yet, so the layout still renders the wordmark, not an empty img.
      const { container } = renderLayout();
      const image = container.querySelector(HEADER_IMG_SELECTOR);
      expect(image).toBeInTheDocument();
      expect(image.getAttribute("src")).toBeTruthy();
    });

    it("keeps a stable, edition-free aria-label before state resolves", () => {
      // Early-boot window where ExtJS.useState has not been attached yet.
      ExtJS.useState.mockImplementation(() => undefined);

      const { container } = renderLayout();

      expect(container.querySelector(HEADER_LINK_SELECTOR)).toHaveAttribute(
        "aria-label",
        "Sonatype Nexus Repository home"
      );
    });
  });

  describe("home link", () => {
    it("defaults to /#browse/welcome when no context path is published", () => {
      setStateValues({ [EDITION_ID_STATE_KEY]: EDITION_PRO });

      const { container } = renderLayout();

      expect(container.querySelector(".nxrm-login-header a[href]")).toHaveAttribute(
        "href",
        "/#browse/welcome"
      );
    });

    it("prefixes the context path when published in state", () => {
      setStateValues({
        [EDITION_ID_STATE_KEY]: EDITION_PRO,
        "nexus-context-path": "/nexus-context",
      });

      const { container } = renderLayout();

      expect(container.querySelector(".nxrm-login-header a[href]")).toHaveAttribute(
        "href",
        "/nexus-context/#browse/welcome"
      );
    });

    it("routes to the welcome hash on logo click without a full navigation", () => {
      setStateValues({ [EDITION_ID_STATE_KEY]: EDITION_PRO });

      const { container } = renderLayout();
      fireEvent.click(container.querySelector(HEADER_LINK_SELECTOR));

      expect(window.location.hash).toBe("#browse/welcome");
    });
  });

  describe("bootstrap resilience", () => {
    it("does not throw when ExtJS.useState is temporarily unavailable", () => {
      ExtJS.useState.mockImplementation(() => undefined);

      expect(() => renderLayout()).not.toThrow();
      expect(screen.getByText("Test Content")).toBeInTheDocument();
    });
  });
});
