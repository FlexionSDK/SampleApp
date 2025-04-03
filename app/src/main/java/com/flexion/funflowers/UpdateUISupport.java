package com.flexion.funflowers;

import static com.flexion.funflowers.Constants.ITEM_ID_SEEDS;

import android.widget.ImageView;
import android.widget.TextView;

import com.flexionmobile.ddpx.model.ProductDetails;


class UpdateUISupport {

    private final FlowerActivity activity;

    static void updateUi(FlowerActivity activity) {
        final UpdateUISupport updateUISupport = new UpdateUISupport(activity);
        activity.runOnUiThread(new Runnable() {
            public void run() {
                updateUISupport.displaySeedsButton();
                updateUISupport.displayAvailableSeeds();
                updateUISupport.updateUiFlowerGrownDependent();
            }
        });
    }

    private UpdateUISupport(FlowerActivity flowerActivity) {
        this.activity = flowerActivity;
    }

    private void displayAvailableSeeds() {
        TextView playerSeedsTextView = activity.findViewById(R.id.player_seeds);
        playerSeedsTextView.setText("Seeds: " + activity.getPlayerSeeds());
    }

    private void displaySeedsButton() {
        TextView seedsButtonTextView = activity.findViewById(R.id.buy_seeds_button_textview);
        String baseText = activity.getResources().getString(R.string.buy_seeds_button_base_text);
        ProductDetails productDetails = activity.getProductDetails().get(ITEM_ID_SEEDS);
        if (productDetails != null) {
            String seedsPrice = productDetails.getPrice();
            seedsButtonTextView.setText(baseText + "\n" + seedsPrice);
        } else {
            seedsButtonTextView.setText(baseText);
        }
    }

    private void updateUiFlowerGrownDependent() {
        if (activity.isFlowerGrown()) {
            // Update the displayed flower components (currently top and bottom)
            ImageView flowerTop = activity.findViewById(R.id.flower_top);
            ImageView flowerBottom = activity.findViewById(R.id.flower_bottom);
            flowerTop.setImageResource(activity.getCurrentFlowerTopId());
            flowerBottom.setImageResource(activity.getCurrentFlowerBottomId());
        }
    }
}
