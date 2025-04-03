/*
 * Fun Flowers copyright 2025 Flexion Mobile Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flexion.funflowers;

import static com.flexion.funflowers.Constants.ITEM_ID_SEEDS;
import static com.flexion.funflowers.Constants.KEY_PLAYER_SEEDS;
import static com.flexion.funflowers.Constants.PLAYER_STARTING_SEEDS;
import static com.flexion.funflowers.Constants.SEEDS_PER_PURCHASE;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.flexionmobile.ddpx.listener.ConnectionStateListener;
import com.flexionmobile.ddpx.listener.PurchasesUpdateListener;
import com.flexionmobile.ddpx.model.BillingResult;
import com.flexionmobile.ddpx.model.BillingResults;
import com.flexionmobile.ddpx.model.ProductDetails;
import com.flexionmobile.ddpx.model.Purchase;
import com.flexionmobile.ddpx.model.params.BillingFlowParams;
import com.flexionmobile.ddpx.model.params.ConsumeParams;
import com.flexionmobile.ddpx.model.params.ProductDetailsParams;
import com.flexionmobile.ddpx.model.params.QueryPurchasesParams;
import com.flexionmobile.ddpx.service.BillingService;
import com.flexionmobile.fdk.FLX;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fun Flowers<br><br>
 * <p>
 * Example game using the Flexion billing SDK. The app uses a local simulated Flexion
 * billing server, so it should work as a stand-alone application. <br><br>
 * <p>
 * This app is a simple game where the player can buy seeds and use it to 'grow'
 * randomly generated flowers. The player starts the game with a set amount of seeds.
 * When the player grows a new flower, they consume a seed. If the player runs
 * out of seeds, they can buy more using an in-app purchase.<br><br>
 * <p>
 * It's important to note the consumption mechanics for each item:<br><br>
 * <p>
 * SEEDS: when seeds are purchased, the "seeds" item is then owned. We
 * consume it when we apply that item's effects to our app's world, which to
 * us means giving the player a fixed number of seeds. This happens immediately
 * after purchase! It's at this point (and not when the user grows a flower) that the
 * "seeds" item is CONSUMED. Consumption should always happen when your game
 * world was safely updated to apply the effect of the purchase. So, in an
 * example scenario:<br><br>
 * <p>
 * BEFORE:      the player has 5 seeds<br>
 * ON PURCHASE: the player has 5 seeds, "seeds" item is owned<br>
 * IMMEDIATELY: the player has 25 seeds, "seeds" item is consumed<br>
 * AFTER:       the player has 25 seeds, "seeds" item NOT owned any more<br><br>
 * <p>
 * Another important point to notice is that it may so happen that
 * the application crashed (or anything else happened) after the user
 * purchased the "seeds" item, but before it was consumed. That's why,
 * on startup, we check if we own the "seeds" item, and, if so,
 * we have to apply its effects to our world and consume it. This
 * is also very important!<br><br>
 */
public class FlowerActivity extends Activity implements PurchasesUpdateListener {

    /**
     * The tag used to mark log messages from this class
     */
    private static final String TAG = "FlowerActivity";

    /**
     * Current number of seeds that the player has
     */
    private long mPlayerSeeds;

    /**
     * An int value that references the resource ID for the current flower top to use
     */
    private int mCurrentFlowerTopId;

    /**
     * An int value that references the resource ID for the current flower bottom to use
     */
    private int mCurrentFlowerBottomId;

    /**
     * A boolean that records whether or not the player has grown a flower yet
     */
    private boolean mFlowerGrown;

    /**
     * The in-app-billing helper object
     */
    private BillingService billingService;

    /**
     * The map containing the cached product details
     */
    private final Map<String, ProductDetails> productDetails = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate() called");

        setContentView(R.layout.activity_flower);
        setWaitScreen(true);

        // This method initializes the Flexion SDK, it makes a network call to the Flexion platform and
        // downloads the required data to show the Flexion screens later. It could take several seconds depending on the network quality.
        // The downloaded content is cached, they are not downloaded on every start only if they were changed on the Flexion platform.
        // It should be called early in the game's lifecycle, preferably in the onCreate method of the Application class or the launcher activity.
        // This is an async call.
        FLX.init(this, (resultCode, message) -> {
            if (resultCode == FLX.InitResultCodes.INIT_SUCCESS) {
                info("Init success");
            } else if (resultCode == FLX.InitResultCodes.COMMUNICATION_ERROR) {
                complain("Could not connect to the Flexion platform");
            } else if (resultCode == FLX.InitResultCodes.FILE_PERSIST_ERROR) {
                complain("Could not save the downloaded data on the device");
            } else {
                complain("Init error: " + message + " [" + resultCode + "]");
            }

            // Hide the wait screen at the end of the call.
            // This is part of the sample app not the sdk, but it's a good UX to show the user something is happening in the background.
            // This is not necessarily needed in a game with a longer launch process while something is happening in the foreground.
            setWaitScreen(false);
        });

        // This call launches the Flexion screen flow which will show the required screens.
        // This call will block in the background until FLX.init finishes, the timeout is 5 minutes at the moment. This means that in an extremely bad
        // network environment this call could time out. Therefore it's recommended to call this in the success case of the InitResultListener in FLX.init.
        // The screens will be drawn on a separate activity.
        // Some of the screens may do a network call.
        FLX.showFlexionScreens(this, (resultCode, message) -> {
            if (resultCode == FLX.ShowScreenResultCodes.SCREEN_FLOW_SUCCESS) {
                Log.i(TAG, "Show screens success");
                displayToast("Show screens success", Toast.LENGTH_LONG);

                // This creates the billing service, it's in a disconnected state at this moment and cannot be used to do billing operations.
                // It will create the service only once. Any subsequent calls will return the cached service but will update the PurchasesUpdateListener.
                // It can be null if there was an error during creation. This can only happen in test mode if there was an error reading the additional dex file,
                // it should not be null in live mode at all.
                billingService = FLX.createBillingService(this, this);

                if (billingService != null) {

                    // This will initialize the billing service and makes it ready to use.
                    // It can only be called after the Flexion screen flow has finished, it will return an error otherwise.
                    // It's recommended to call this in the ShowScreensResultListener's success state.
                    billingService.startConnection(this, new ConnectionStateListener() {
                        @Override
                        public void onBillingServiceDisconnected() {
                            // In this version of the sdk this callback in only used in test mode and it will not be called in live mode.
                            complain("Billing service disconnected");
                        }

                        @Override
                        public void onBillingSetupFinished(BillingResult billingResult) {
                            Log.i(TAG, "Billing service init success");

                            // It's strongly recommended to call queryProductDetailsAsync and queryPurchasesAsync at this point
                            // to have the latest product details and handle unconsumed purchases.
                            // It's also recommended to cache the product details and use that instead of calling queryProductDetails too many times.
                            // Alternative stores are significantly slower in this regard than Google.
                            queryProductDetails();
                            queryPurchases();
                        }
                    });
                } else {
                    complain("Billing service is null");
                }

            } else {
                complain("Show screens error: " + message + " [" + resultCode + "]");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume() called");

        try {
            // Load game data and update the UI to reflect it
            loadData();
            updateUi();
        } catch (Exception e) {
            Log.e(TAG, "Exception occurred in FlowerActivity.onResume(). The exception stack trace was: "
                    + getStackTraceString(e));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        saveData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        saveData();
    }

    // This is the callback for the purchase flow from the PurchasesUpdateListener interface.
    // The possible error codes are all the 3xxx codes in BillingResult.ResultCode, check the integration guide for details.
    @Override
    public void onPurchasesUpdated(BillingResult billingResult, Purchase purchase) {
        switch (billingResult.getResponseCode()) {
            case BillingResults.ResultCode.PURCHASE_SUCCESS_CODE -> handleSuccess(purchase);
            case BillingResults.ResultCode.PURCHASE_USER_CANCELLED_CODE -> handleUserCancelled();
            case BillingResults.ResultCode.PURCHASE_ERROR_CODE -> handleError(billingResult.getDebugMessage());
            default -> complain("Unknown purchase status");
        }

        setWaitScreen(false);
    }

    // Validate, award and consume purchase. This purchase validation is only for the sample app, it's an extremely bad idea to use client side validation.
    // You should use your own server side validation instead.
    // Flexion does not offer a server-to-server consumption api at the moment, since not all stores support it.
    // It's not necessary but recommended to track purchase consumption and awarding on your server side and only consume purchases on client side
    // when you successfully awarded the item.
    private void handleSuccess(Purchase purchase) {
        if (validatePurchase(purchase)) {
            String itemId = purchase.getProducts().get(0);
            if (itemId.equals(ITEM_ID_SEEDS)) {
                Log.i(TAG, "Purchase is seeds. Starting seed consumption.");

                // Consume purchase
                if (billingService != null) {
                    billingService.consumeAsync(new ConsumeParams(purchase.getToken()), billingResult -> {
                        if (billingResult.getResponseCode() == BillingResults.ResultCode.CONSUME_SUCCESS_CODE) {
                            mPlayerSeeds = mPlayerSeeds + SEEDS_PER_PURCHASE;
                            saveData();
                            displayAlert("You purchased " + SEEDS_PER_PURCHASE + " seeds!\n\n"
                                    + "You now have " + mPlayerSeeds + " seeds to grow flowers with!");
                            updateUi();
                            Log.i(TAG, "Consumption successful");
                        } else {
                            complain("Consume error: " + billingResult.getDebugMessage());
                        }
                    });
                }
            } else {
                complain("Unknown item id: " + purchase.getProducts().get(0));
            }
        }

        setWaitScreen(false);
    }

    private void handleError(String debugMessage) {
        complain("Purchase error: " + debugMessage);
        setWaitScreen(false);
    }

    private void handleUserCancelled() {
        complain("Purchase cancelled by user");
        setWaitScreen(false);
    }

    private void queryProductDetails() {
        Log.i(TAG, "QueryProductDetails called");
        
        if (billingService == null || !billingService.isReady()) {
            complain("Billing service is null or not ready");
            return;
        }

        // Get the item details
        billingService.queryProductDetailsAsync(new ProductDetailsParams("inapp", List.of(ITEM_ID_SEEDS)), (billingResult, details) -> {
            if (billingResult.getResponseCode() == BillingResults.ResultCode.QUERY_PRODUCT_DETAILS_SUCCESS_CODE) {
                Log.i(TAG, "QueryProductDetails successful");
                details.forEach(pd -> productDetails.put(pd.getId(), pd));

                updateUi();
            } else {
                complain("Query product details failed: " + billingResult.getDebugMessage() + " [" + billingResult.getResponseCode() + "]");
            }
        });
    }

    private void queryPurchases() {
        Log.i(TAG, "QueryPurchases called");
        if (billingService == null || !billingService.isReady()) {
            complain("Billing service is null or not ready");
            return;
        }

        // Get unconsumed purchases
        billingService.queryPurchasesAsync(new QueryPurchasesParams("inapp"), (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingResults.ResultCode.QUERY_PURCHASES_SUCCESS_CODE) {
                Log.i(TAG, "QueryPurchases successful");
                purchases.forEach(this::handleSuccess);
            } else {
                complain("Query purchases failed: " + billingResult.getDebugMessage() + " [" + billingResult.getResponseCode() + "]");
            }
        });
    }

    // User clicked the 'grow flower' button
    public void onGrowFlowerButtonClicked(View arg0) {
        Log.i(TAG, "Grow flower button clicked");

        // Check whether the player has any available seeds
        if (mPlayerSeeds <= 0) {
            displayAlert("Oh no! You have run out of seeds! Buy some more so you can keep growing flowers!");
            return;
        }

        // Pick new flower parts to be displayed
        int[] flowerPartIds = FlowerPicker.pickFlowerParts();
        mCurrentFlowerTopId = flowerPartIds[0];
        mCurrentFlowerBottomId = flowerPartIds[1];

        // Record that the player has grown a flower
        mFlowerGrown = true;

        // Take one seed from the player
        mPlayerSeeds--;

        saveData();
        updateUi();

        // Log the player's new balance
        Log.i(TAG, "The player now has " + mPlayerSeeds + " seeds");
    }

    // User clicked the "Buy Seeds" button
    public void onBuySeedsButtonClicked(View arg0) {
        Log.i(TAG, "Buy seeds button clicked.");

        setWaitScreen(true);
        // launch the seeds purchase UI flow.
        // We will be notified of completion via onPurchaseFinishedCallback
        Log.i(TAG, "Launching purchase flow for seeds");

        if (billingService != null && billingService.isReady()) {
            billingService.launchBillingFlow(this, new BillingFlowParams(ITEM_ID_SEEDS, "inapp", ""));
        } else {
            setWaitScreen(false);
            complain("Billing service is null");
        }
    }

    private boolean validatePurchase(Purchase purchase) {
        try {
            boolean isValid = PurchaseValidator.verifyPurchaseData(purchase.getPurchaseJson(), purchase.getSignature());
            if (isValid) {
                displayToast("Successful purchase validation", Toast.LENGTH_LONG);
                return true;
            } else {
                complain("Invalid purchase");
            }
        } catch (Exception e) {
            Log.i(TAG, "Purchase validation failed", e);
            complain("Purchase validation failed: " + e.getMessage() + "\nCheck device logs for details");
        }
        return false;
    }

    // Updates the UI to reflect the model
    private void updateUi() {
        UpdateUISupport.updateUi(this);
    }

    /**
     * Enables or disables the "please wait" screen.
     */
    private void setWaitScreen(final boolean set) {
        runOnUiThread(() -> {
            findViewById(R.id.screen_main).setVisibility(set ? View.GONE : View.VISIBLE);
            findViewById(R.id.screen_wait).setVisibility(set ? View.VISIBLE : View.GONE);
        });
    }

    /**
     * Takes an error message and:<br>
     * i)  Logs it<br>
     * ii) Displays an alert dialog with the message to the user
     */
    private void complain(final String message) {
        runOnUiThread(() -> {
            Log.e(TAG, "**** Fun Flowers Error: " + message);
            displayAlert("Error: " + message);
        });
    }

    /**
     * Takes a message and:<br>
     * i)  Logs it<br>
     * ii) Displays a toast message to the user
     */
    private void info(final String message) {
        runOnUiThread(() -> {
            Log.i(TAG, "**** Fun Flowers Info: " + message);
            Toast.makeText(FlowerActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Display an alert message to the user
     *
     * @param message - The alert message to display
     */
    private void displayAlert(String message) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(FlowerActivity.this);
            builder.setMessage(message);
            builder.setNeutralButton("OK", null);
            Log.i(TAG, "Showing alert dialog: " + message);
            builder.create().show();
        });
    }

    /**
     * Display a toast message to the user
     *
     * @param message - The toast message to display
     */
    private void displayToast(String message, int duration) {
        runOnUiThread(() -> Toast.makeText(FlowerActivity.this, message, duration).show());
    }

    /**
     * Saves the player's game state.
     */
    private void saveData() {
        /*
         * Note: in a real application, we recommend you save data in a secure way to
         * prevent tampering. For simplicity in this sample, we simply store the player data
         * in SharedPreferences.
         */
        SharedPreferences.Editor sharedPrefs = getPreferences(MODE_PRIVATE).edit();
        sharedPrefs.putLong(KEY_PLAYER_SEEDS, mPlayerSeeds);
        sharedPrefs.commit();
        Log.i(TAG, "Saved player game data:\n"
                + "Player seeds: " + mPlayerSeeds);
    }

    /**
     * Loads the player's game state.
     */
    private void loadData() {
        SharedPreferences sharedPrefs = getPreferences(MODE_PRIVATE);
        mPlayerSeeds = sharedPrefs.getLong(KEY_PLAYER_SEEDS, PLAYER_STARTING_SEEDS);
        Log.i(TAG, "Loaded player game data:\n"
                + "Player seeds: " + mPlayerSeeds);
    }

    private String getStackTraceString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    boolean isFlowerGrown() {
        return mFlowerGrown;
    }

    int getCurrentFlowerTopId() {
        return mCurrentFlowerTopId;
    }

    int getCurrentFlowerBottomId() {
        return mCurrentFlowerBottomId;
    }

    long getPlayerSeeds() {
        return mPlayerSeeds;
    }

    Map<String, ProductDetails> getProductDetails() {
        return productDetails;
    }
}