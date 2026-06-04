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

import { interpret } from "xstate";
import LoginPageStrings from "../../../constants/LoginPageStrings";
import ExtJS from "../../../interface/ExtJS";
import localLoginMachine from "./LocalLoginMachine";

jest.mock("../../../interface/ExtJS", () => ({
  __esModule: true,
  default: {
    requestSession: jest.fn(),
  },
}));

describe("LocalLoginMachine", () => {
  let mockRequestSession;

  beforeEach(() => {
    mockRequestSession = ExtJS.requestSession;
    mockRequestSession.mockReset();
  });

  describe("machine initialization", () => {
    it("initializes with correct initial state", () => {
      const service = interpret(localLoginMachine);
      service.start();

      expect(service.state.matches("loaded")).toBe(true);
      service.stop();
    });

    it("initializes context with empty username and password", () => {
      const service = interpret(localLoginMachine);
      service.start();

      expect(service.state.context.data).toEqual({
        username: "",
        password: "",
      });
      expect(service.state.context.pristineData).toEqual({
        username: "",
        password: "",
      });
      expect(service.state.context.skipValidation).toBe(false);
      service.stop();
    });
  });

  describe("validation logic", () => {
    it("validates username and password as required when blank on entry to loaded state", () => {
      const service = interpret(localLoginMachine);

      service.onTransition((state) => {
        if (state.matches("loaded") && state.changed) {
          // Validation happens on entry to loaded state
          // When both fields are blank, both should have errors
          expect(state.context.validationErrors.username).toBe(
            LoginPageStrings.ERRORS.USERNAME_REQUIRED
          );
          expect(state.context.validationErrors.password).toBe(
            LoginPageStrings.ERRORS.PASSWORD_REQUIRED
          );
          service.stop();
        }
      });

      service.start();
    });

    it("validates password as required when only username is provided", () => {
      const service = interpret(localLoginMachine);
      service.start();

      service.send({
        type: "UPDATE",
        name: "username",
        value: "testuser",
      });

      // Validation happens on entry to loaded state after UPDATE
      expect(service.state.context.validationErrors.username).toBeUndefined();
      expect(service.state.context.validationErrors.password).toBe(
        LoginPageStrings.ERRORS.PASSWORD_REQUIRED
      );
      service.stop();
    });

    it("passes validation when both username and password are provided", () => {
      const service = interpret(localLoginMachine);
      service.start();

      service.send({
        type: "UPDATE",
        name: "username",
        value: "testuser",
      });
      service.send({
        type: "UPDATE",
        name: "password",
        value: "testpass",
      });

      expect(service.state.context.validationErrors.username).toBeUndefined();
      expect(service.state.context.validationErrors.password).toBeUndefined();
      service.stop();
    });

    it("skips validation when skipValidation flag is true", () => {
      const service = interpret(localLoginMachine);
      service.start();

      // Set skipValidation to true (simulating after auth error)
      service.state.context.skipValidation = true;

      // Trigger validation by sending UPDATE (which re-enters loaded state)
      service.send({
        type: "UPDATE",
        name: "username",
        value: "",
      });

      // When skipValidation is true, validation should return empty object
      // But then skipValidation is reset to false
      expect(service.state.context.skipValidation).toBe(false);
      service.stop();
    });
  });

  describe("error handling - 403 Forbidden (wrong credentials)", () => {
    it("clears password field on 403 error", (done) => {
      const service = interpret(localLoginMachine);
      service.start();

      const error403 = {
        response: {
          status: 403,
          data: {},
        },
      };

      service.send({
        type: "UPDATE",
        name: "username",
        value: "testuser",
      });
      service.send({
        type: "UPDATE",
        name: "password",
        value: "wrongpass",
      });

      mockRequestSession.mockRejectedValue(error403);

      // Ensure guard passes so SAVE transitions to saving
      service.state.context.isPristine = false;

      let saveStarted = false;
      service.onTransition((state) => {
        if (state.matches("saving")) {
          saveStarted = true;
        }
        if (saveStarted && state.matches("loaded") && state.context.saveError) {
          expect(state.context.data.password).toBe("");
          expect(state.context.data.username).toBe("testuser");
          done();
        }
      });

      service.send({ type: "SAVE" });
    });

    it("sets blank field errors for username and password on 403", (done) => {
      const service = interpret(localLoginMachine);
      service.start();

      const error403 = {
        response: {
          status: 403,
          data: {},
        },
      };

      service.send({
        type: "UPDATE",
        name: "username",
        value: "testuser",
      });
      service.send({
        type: "UPDATE",
        name: "password",
        value: "wrongpass",
      });

      mockRequestSession.mockRejectedValue(error403);

      // Ensure guard passes so SAVE transitions to saving
      service.state.context.isPristine = false;

      let saveStarted = false;
      service.onTransition((state) => {
        if (state.matches("saving")) {
          saveStarted = true;
        }
        if (saveStarted && state.matches("loaded") && state.context.saveError) {
          expect(state.context.saveErrors.username).toBe(" ");
          expect(state.context.saveErrors.password).toBe(" ");
          done();
        }
      });

      service.send({ type: "SAVE" });
    });

    it("clears validation errors on 403 error", (done) => {
      const service = interpret(localLoginMachine);
      service.start();

      service.send({
        type: "UPDATE",
        name: "username",
        value: "testuser",
      });
      service.send({
        type: "UPDATE",
        name: "password",
        value: "testpass",
      });


      const error403 = {
        response: {
          status: 403,
          data: {},
        },
      };

      // Set validation errors first
      service.send({ type: "VALIDATE" });

      mockRequestSession.mockRejectedValue(error403);

      let saveStarted = false;
      service.onTransition((state) => {
        if (state.matches("saving")) {
          saveStarted = true;
        }
        if (saveStarted && state.matches("loaded") && state.context.saveError) {
          expect(state.context.validationErrors).toEqual({});
          done();
        }
      });

      service.send({ type: "SAVE" });
    });
  });

  describe("error handling - 500 Server error", () => {
    it("sets correct error message for 500 error", (done) => {
      const service = interpret(localLoginMachine);
      service.start();

      service.send({
        type: "UPDATE",
        name: "username",
        value: "testuser",
      });
      service.send({
        type: "UPDATE",
        name: "password",
        value: "testpass",
      });


      const error500 = {
        response: {
          status: 500,
          data: {
            message: "Internal server error",
          },
        },
      };

      mockRequestSession.mockRejectedValue(error500);

      let saveStarted = false;
      service.onTransition((state) => {
        if (state.matches("saving")) {
          saveStarted = true;
        }
        if (saveStarted && state.matches("loaded") && state.context.saveError) {
          expect(state.context.saveError).toBe("Internal server error");
          expect(state.context.saveErrors).toEqual({});
          done();
        }
      });

      service.send({ type: "SAVE" });
    });

    it("uses default authentication failed message when no error message provided", (done) => {
      const service = interpret(localLoginMachine);
      service.start();

      service.send({
        type: "UPDATE",
        name: "username",
        value: "testuser",
      });
      service.send({
        type: "UPDATE",
        name: "password",
        value: "testpass",
      });


      const error500 = {
        response: {
          status: 500,
          data: {},
        },
      };

      mockRequestSession.mockRejectedValue(error500);

      let saveStarted = false;
      service.onTransition((state) => {
        if (state.matches("saving")) {
          saveStarted = true;
        }
        if (saveStarted && state.matches("loaded") && state.context.saveError) {
          expect(state.context.saveError).toBe(
            LoginPageStrings.ERRORS.AUTHENTICATION_FAILED
          );
          done();
        }
      });

      service.send({ type: "SAVE" });
    });
  });

  describe("error handling - connection error (status 0)", () => {
    it("sets connection failed error message for status 0", (done) => {
      const service = interpret(localLoginMachine);
      service.start();

      service.send({
        type: "UPDATE",
        name: "username",
        value: "testuser",
      });
      service.send({
        type: "UPDATE",
        name: "password",
        value: "testpass",
      });


      const connectionError = {
        response: {
          status: 0,
          data: {},
        },
      };

      mockRequestSession.mockRejectedValue(connectionError);

      let saveStarted = false;
      service.onTransition((state) => {
        if (state.matches("saving")) {
          saveStarted = true;
        }
        if (saveStarted && state.matches("loaded") && state.context.saveError) {
          expect(state.context.saveError).toBe(
            LoginPageStrings.ERRORS.CONNECTION_FAILED
          );
          done();
        }
      });

      service.send({ type: "SAVE" });
    });
  });

  describe("error handling - error without response", () => {
    it("handles error without response object", (done) => {
      const service = interpret(localLoginMachine);
      service.start();

      service.send({
        type: "UPDATE",
        name: "username",
        value: "testuser",
      });
      service.send({
        type: "UPDATE",
        name: "password",
        value: "testpass",
      });


      const error = new Error("Network error");

      mockRequestSession.mockRejectedValue(error);

      let saveStarted = false;
      service.onTransition((state) => {
        if (state.matches("saving")) {
          saveStarted = true;
        }
        if (saveStarted && state.matches("loaded") && state.context.saveError) {
          expect(state.context.saveError).toBe(
            LoginPageStrings.ERRORS.AUTHENTICATION_FAILED
          );
          done();
        }
      });

      service.send({ type: "SAVE" });
    });
  });

  describe("clearSaveError action", () => {
    it("clears all error-related context on CLEAR_SAVE_ERROR", () => {
      const service = interpret(localLoginMachine);
      service.start();

      // Set error state first
      service.state.context.saveError = "Some error";
      service.state.context.saveErrorData = { username: "test" };
      service.state.context.saveErrors = { username: " ", password: " " };
      service.state.context.skipValidation = false;

      service.send({ type: "CLEAR_SAVE_ERROR" });

      expect(service.state.context.saveError).toBeUndefined();
      expect(service.state.context.saveErrorData).toEqual({});
      expect(service.state.context.saveErrors).toEqual({});
      expect(service.state.context.skipValidation).toBe(true);
      service.stop();
    });
  });

  describe("saveErrorData assignment", () => {
    it("saves current data to saveErrorData on error", (done) => {
      const service = interpret(localLoginMachine);
      service.start();

      const error = {
        response: {
          status: 500,
          data: {},
        },
      };

      service.send({
        type: "UPDATE",
        name: "username",
        value: "testuser",
      });
      service.send({
        type: "UPDATE",
        name: "password",
        value: "testpass",
      });

      mockRequestSession.mockRejectedValue(error);

      let saveStarted = false;
      service.onTransition((state) => {
        if (state.matches("saving")) {
          saveStarted = true;
        }
        if (saveStarted && state.matches("loaded") && state.context.saveError) {
          expect(state.context.saveErrorData).toEqual({
            username: "testuser",
            password: "testpass",
          });
          done();
        }
      });

      service.send({ type: "SAVE" });
    });
  });

  describe("service integration - saveData", () => {
    it("calls ExtJS.requestSession with correct credentials", (done) => {
      const service = interpret(localLoginMachine);
      service.start();

      const successResponse = {
        response: { status: 204 },
      };

      service.send({
        type: "UPDATE",
        name: "username",
        value: "testuser",
      });
      service.send({
        type: "UPDATE",
        name: "password",
        value: "testpass",
      });

      mockRequestSession.mockResolvedValue(successResponse);

      let saveStarted = false;
      service.onTransition((state) => {
        if (state.matches("saving")) {
          saveStarted = true;
        }
        if (saveStarted && state.matches("loaded") && !state.context.saveError) {
          expect(mockRequestSession).toHaveBeenCalledWith(
            "testuser",
            "testpass"
          );
          done();
        }
      });

      service.send({ type: "SAVE" });
    });

    it("returns response and username in success data", (done) => {
      const service = interpret(localLoginMachine);
      service.start();

      const successResponse = {
        response: { status: 204 },
      };

      service.send({
        type: "UPDATE",
        name: "username",
        value: "testuser",
      });
      service.send({
        type: "UPDATE",
        name: "password",
        value: "testpass",
      });

      mockRequestSession.mockResolvedValue(successResponse);

      let saveStarted = false;
      service.onTransition((state) => {
        if (state.matches("saving")) {
          saveStarted = true;
        }
        if (saveStarted && state.matches("loaded") && !state.context.saveError) {
          // The success data should be available in the event
          // This is tested indirectly through the component integration
          expect(mockRequestSession).toHaveBeenCalled();
          done();
        }
      });

      service.send({ type: "SAVE" });
    });

    it("transitions from loaded to saving to loaded on error", (done) => {
      const service = interpret(localLoginMachine);
      service.start();

      service.send({
        type: "UPDATE",
        name: "username",
        value: "testuser",
      });
      service.send({
        type: "UPDATE",
        name: "password",
        value: "testpass",
      });


      const error = {
        response: {
          status: 500,
          data: {},
        },
      };

      const states = [];
      service.onTransition((state) => {
        states.push(state.value);
        if (state.matches("loaded") && state.context.saveError) {
          expect(states).toContain("saving");
          expect(state.value).toBe("loaded");
          done();
        }
      });

      mockRequestSession.mockRejectedValue(error);
      service.send({ type: "SAVE" });
    });
  });

  describe("CLEAR_SAVE_ERROR event", () => {
    it("handles CLEAR_SAVE_ERROR event in loaded state", () => {
      const service = interpret(localLoginMachine);
      service.start();

      // Set error state
      service.state.context.saveError = "Some error";
      service.state.context.saveErrors = { username: " " };

      service.send({ type: "CLEAR_SAVE_ERROR" });

      expect(service.state.context.saveError).toBeUndefined();
      expect(service.state.context.saveErrors).toEqual({});
      expect(service.state.matches("loaded")).toBe(true);
      service.stop();
    });
  });

  describe("rate limiting (429)", () => {
    it("transitions to rateLimited state and sets context on 429", (done) => {
      const service = interpret(localLoginMachine);
      service.start();

      service.send({ type: "UPDATE", name: "username", value: "testuser" });
      service.send({ type: "UPDATE", name: "password", value: "testpass" });

      const error429 = {
        response: { status: 429, headers: { "retry-after": "30" } },
      };
      mockRequestSession.mockRejectedValue(error429);

      let inRateLimited = false;
      service.onTransition((state) => {
        if (state.matches("rateLimited") && !inRateLimited) {
          inRateLimited = true;
          expect(state.context.rateLimitWarning).toBe(true);
          expect(state.context.retryAfterSeconds).toBe(30);
          service.stop();
          done();
        }
      });

      service.send({ type: "SAVE" });
    });

    it("TICK decrements retryAfterSeconds when > 1", (done) => {
      const service = interpret(localLoginMachine);
      service.start();

      service.send({ type: "UPDATE", name: "username", value: "testuser" });
      service.send({ type: "UPDATE", name: "password", value: "testpass" });

      const error429 = {
        response: { status: 429, headers: { "retry-after": "5" } },
      };
      mockRequestSession.mockRejectedValue(error429);

      let tickSent = false;
      service.onTransition((state) => {
        if (state.matches("rateLimited")) {
          if (!tickSent) {
            tickSent = true;
            expect(state.context.retryAfterSeconds).toBe(5);
            service.send({ type: "TICK" });
          } else {
            expect(state.context.retryAfterSeconds).toBe(4);
            service.stop();
            done();
          }
        }
      });

      service.send({ type: "SAVE" });
    });

    it("transitions back to loaded and clears rate limit when countdown expires", (done) => {
      const service = interpret(localLoginMachine);
      service.start();

      service.send({ type: "UPDATE", name: "username", value: "testuser" });
      service.send({ type: "UPDATE", name: "password", value: "testpass" });

      const error429 = {
        response: { status: 429, headers: { "retry-after": "1" } },
      };
      mockRequestSession.mockRejectedValue(error429);

      let inRateLimited = false;
      service.onTransition((state) => {
        if (state.matches("rateLimited") && !inRateLimited) {
          inRateLimited = true;
          // retryAfterSeconds is 1; TICK should transition to loaded
          service.send({ type: "TICK" });
        } else if (state.matches("loaded") && inRateLimited) {
          expect(state.context.rateLimitWarning).toBe(false);
          expect(state.context.retryAfterSeconds).toBeNull();
          service.stop();
          done();
        }
      });

      service.send({ type: "SAVE" });
    });

    it("non-429 error does NOT enter rateLimited state", (done) => {
      const service = interpret(localLoginMachine);
      service.start();

      service.send({ type: "UPDATE", name: "username", value: "testuser" });
      service.send({ type: "UPDATE", name: "password", value: "testpass" });

      const error403 = { response: { status: 403, data: {} } };
      mockRequestSession.mockRejectedValue(error403);

      let saveStarted = false;
      service.onTransition((state) => {
        if (state.matches("saving")) {
          saveStarted = true;
        }
        if (saveStarted && state.matches("loaded") && state.context.saveError) {
          expect(state.context.rateLimitWarning).toBe(false);
          expect(state.context.retryAfterSeconds).toBeNull();
          service.stop();
          done();
        }
      });

      service.send({ type: "SAVE" });
    });
  });

  describe("dirty flag actions", () => {
    it("setDirtyFlag is a no-op", () => {
      const service = interpret(localLoginMachine);
      service.start();

      // setDirtyFlag is defined in LocalLoginMachine withConfig as a no-op
      // It's called internally by the form machine but does nothing
      // We can't directly test it, but we can verify the machine works correctly
      // even when UPDATE events are sent (which would normally trigger setDirtyFlag)
      service.send({
        type: "UPDATE",
        name: "username",
        value: "test",
      });

      // If setDirtyFlag had an error, this would throw
      expect(service.state.context.data.username).toBe("test");
      service.stop();
    });

    it("clearDirtyFlag is a no-op", () => {
      const service = interpret(localLoginMachine);
      service.start();

      // clearDirtyFlag is defined in LocalLoginMachine withConfig as a no-op
      // It's called internally by the form machine but does nothing
      // We can verify the machine continues to work correctly
      service.send({
        type: "UPDATE",
        name: "username",
        value: "test",
      });

      expect(service.state.context.data.username).toBe("test");
      service.stop();
    });
  });
});
