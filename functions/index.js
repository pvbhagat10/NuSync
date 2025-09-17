/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {setGlobalOptions} = require("firebase-functions");
// This is for v2 HTTP functions, not needed for callable functions in v1
// const {onRequest} = require("firebase-functions/https");
const logger = require("firebase-functions/logger");

// Import the v1 functions module for callable functions
const functions = require("firebase-functions");
const admin = require("firebase-admin");

// Initialize Firebase Admin SDK
admin.initializeApp();

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
setGlobalOptions({maxInstances: 10}); // This primarily affects v2 functions.

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

// exports.helloWorld = onRequest((request, response) => {
//   logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });

// V1 Callable function for sending admin notifications
// Note: set maxInstances at the function level for v1 functions if you need it.
exports.sendAdminNotification = functions.https.onCall(
    async (data, context) => {
      // Optional: Log the received data for debugging
      logger.info("sendAdminNotification called with data:", data);

      // For a simple internal app, you might initially skip this,
      // but add it for production.
      if (!context.auth) {
        logger.warn("Unauthenticated call to sendAdminNotification.");
        throw new functions.https.HttpsError(
            "unauthenticated",
            "The function must be called while authenticated.",
        );
      }

      const {type, detail, initiatorName} = data;

      if (!type || !detail || !initiatorName) {
        logger.error("Missing arguments for sendAdminNotification",
            {type, detail, initiatorName});
        throw new functions.https.HttpsError(
            "invalid-argument",
            "The function must be called with type,"+
            " detail, and initiatorName " +
        "arguments.",
        );
      }

      const notificationTitle = "NuSync Update";
      let notificationBody = "";

      switch (type) {
        case "FULFILLED":
          notificationBody =
              `${initiatorName} fulfilled a requirement: ${detail}`;
          break;
        case "COMMENTED":
          notificationBody = `${initiatorName} added a comment to: ${detail}`;
          break;
        case "DELETED":
          notificationBody =
              `${initiatorName} deleted a requirement: ${detail}`;
          break;
        default:
          notificationBody = `${initiatorName} performed an action: ${detail}`;
      }

      const adminTokens = [];
      try {
        const usersSnapshot = await admin.database().ref("Users").once("value");
        usersSnapshot.forEach((userSnap) => {
          const userData = userSnap.val();
          // Ensure userData exists and has role and fcmToken
          if (userData && userData.role === "Admin" && userData.fcmToken) {
            adminTokens.push(userData.fcmToken);
          }
        });
      } catch (error) {
        logger.error("Error fetching admin tokens:", error);
        throw new functions.https.HttpsError(
            "internal",
            "Failed to retrieve admin tokens.",
        );
      }

      if (adminTokens.length === 0) {
        logger.info("No admin tokens found to send notification.");
        return {success: true, message: "No admins to notify."};
      }

      const message = {
        notification: {
          title: notificationTitle,
          body: notificationBody,
        },
        data: {
          // You can send additional data here if needed by the app
          notificationType: type,
          requirementDetail: detail,
        },
        // For multiple tokens, consider using `token` for single or `tokens`
        // for an array. `sendToDevice` handles both.
      };

      try {
        // `sendToDevice` can send to an array of tokens
        const response = await admin.messaging()
            .sendToDevice(adminTokens, message);
        logger.info("Successfully sent message to admins:", response);

        // Optional: Handle failed tokens if any
        if (response.results) {
          response.results.forEach((result, index) => {
            const error = result.error;
            if (error) {
              logger.error(
                  `Failure to send to token ${adminTokens[index]}: ` +
              `${error.code} - ${error.message}`,
              );
              // Optionally remove invalid tokens from your database here
              if (error.code === "messaging/invalid-registration-token" ||
              error.code === "messaging/registration-token-not-registered") {
                // This would require a lookup, or storing token-to-user mapping
                // For now, just log.
              }
            }
          });
        }
        return {success: true, message: "Notifications sent to admins."};
      } catch (error) {
        logger.error("Error sending message to admins:", error);
        throw new functions.https.HttpsError(
            "unknown",
            "Failed to send notifications.",
            error.message,
        );
      }
    });
