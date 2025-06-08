// functions/index.js

// Use v2 imports
const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {logger} = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

exports.updateAverageRating = onDocumentCreated("user_ratings/{ratingId}",
    async (event) => {
      const snapshot = event.data;
      if (!snapshot) {
        logger.log("No data associated with the event for ratingId:",
            event.params.ratingId);
        return null;
      }
      const newRating = snapshot.data();

      if (!newRating) {
        logger.log("New rating data is undefined in snapshot for ratingId:",
            event.params.ratingId);
        return null;
      }

      const ratedUserId = newRating.ratedUserId;
      const ratingValue = newRating.ratingValue;
      const ratingId = event.params.ratingId;

      if (!ratedUserId || typeof ratingValue !== "number") {
        logger.error(
            `Missing ratedUserId or invalid ratingValue ` +
            `(rating: ${ratingValue}) in new rating for ratingId ${ratingId}:`,
            newRating,
        );
        return null;
      }

      logger.log(
          `V2: New rating received for user ${ratedUserId} ` +
        `with value ${ratingValue}. Rating ID: ${ratingId}`,
      );

      const userProfileRef = db.collection("users").doc(ratedUserId);

      return db.runTransaction(async (transaction) => {
        const userProfileDoc = await transaction.get(userProfileRef);
        let currentTotalPoints = 0;
        let currentNumberOfRatings = 0;

        if (userProfileDoc.exists) {
          const userProfileData = userProfileDoc.data();
          currentTotalPoints = userProfileData.totalRatingPoints || 0;
          currentNumberOfRatings = userProfileData.numberOfRatings || 0;
        } else {
          logger.log(
              `V2: User profile for ${ratedUserId} does not exist. ` +
                `Initializing with this rating. ` +
                `(Ideally, profile exists from signup)`,
          );
        }

        const newTotalPoints = currentTotalPoints + ratingValue;
        const newNumberOfRatings = currentNumberOfRatings + 1;
        const newAverageRating =
            newNumberOfRatings > 0 ? newTotalPoints / newNumberOfRatings : 0;

        logger.log(
            `V2: Updating profile for ${ratedUserId}: ` +
            `New Total Points=${newTotalPoints}, ` +
            `New Num Ratings=${newNumberOfRatings}, ` +
            `New Avg Rating=${newAverageRating.toFixed(2)}`,
        );
        if (userProfileDoc.exists) {
          transaction.update(userProfileRef, {
            totalRatingPoints: newTotalPoints,
            numberOfRatings: newNumberOfRatings,
            averageRating: parseFloat(newAverageRating.toFixed(2)),
          });
        } else {
          transaction.set(userProfileRef, {
            userId: ratedUserId,
            displayName: "User " + ratedUserId.substring(0, 5),
            profilePictureUrl: null,
            totalRatingPoints: newTotalPoints,
            numberOfRatings: newNumberOfRatings,
            averageRating: parseFloat(newAverageRating.toFixed(2)),
          });
        }
        return null;
      })
          .then(() => {
            logger.log(`V2: Successfully updated` +
            `average rating for user ${ratedUserId}.`);
            return null;
          })
          .catch((error) => {
            logger.error(
                `V2: Error updating average rating for user ${ratedUserId}:`,
                error,
            );
            throw error;
          });
    });
