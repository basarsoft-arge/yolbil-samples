package com.akylas.yolbiltest.ui.main;

import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.akylas.yolbiltest.R;

/**
 * Basit bir yardımcı sınıf: NavigationInfoCard üzerindeki view referanslarını
 * merkezi bir yerden güncelleyebilmek için kullanılır.
 */
public class NavigationInfoCardView {

    private final ImageView directionIconView;
    private final TextView manifestTextView;
    private final TextView directionTextView;
    private final TextView distanceTextView;
    private final View nextCommandContainer;
    private final TextView nextCommandLabelView;
    private final ImageView nextCommandIconView;
    private final TextView totalDistanceTextView;
    private final TextView totalTimeTextView;
    private final LinearLayout speedRow;
    private final ImageView speedIconView;
    private final TextView currentSpeedTextView;
    private final TextView speedLimitTextView;

    public NavigationInfoCardView(View rootView) {
        // Kart içindeki manifest/direction bileşenleri
        View cardView = rootView.findViewById(R.id.navigationInfoCard);
        directionIconView = cardView.findViewById(R.id.navigationDirectionIcon);
        manifestTextView = cardView.findViewById(R.id.navigationManifestText);
        directionTextView = cardView.findViewById(R.id.navigationDirectionText);
        distanceTextView = cardView.findViewById(R.id.navigationDistanceText);
        nextCommandContainer = cardView.findViewById(R.id.nextCommandContainer);
        nextCommandLabelView = cardView.findViewById(R.id.nextCommandLabel);
        nextCommandIconView = cardView.findViewById(R.id.nextCommandIcon);
        totalDistanceTextView = cardView.findViewById(R.id.navigationTotalDistanceText);
        totalTimeTextView = cardView.findViewById(R.id.navigationTotalTimeText);

        // Kart dışında yer alan hız kutucuğu
        speedRow = rootView.findViewById(R.id.navigationSpeedRow);
        speedIconView = rootView.findViewById(R.id.navigationSpeedIcon);
        currentSpeedTextView = rootView.findViewById(R.id.navigationCurrentSpeedText);
        speedLimitTextView = rootView.findViewById(R.id.navigationSpeedLimitText);
    }

    public void updatePrimaryInfo(String manifestText, String directionText, String distanceText, @DrawableRes int iconRes) {
        manifestTextView.setText(manifestText);
        directionTextView.setText(directionText);
        distanceTextView.setText(distanceText);
        directionIconView.setImageResource(iconRes);
    }

    public void updateSummaryInfo(String totalDistanceText, String totalTimeText) {
        totalDistanceTextView.setText(totalDistanceText);
        totalTimeTextView.setText(totalTimeText);
    }

    public void updateSpeedInfo(int currentSpeed, int speedLimit, boolean isSpeeding) {
        if (speedRow == null) {
            return;
        }
        Resources res = speedRow.getResources();
        String currentText = currentSpeed > 0
                ? res.getString(R.string.navigation_speed_value, currentSpeed)
                : res.getString(R.string.navigation_current_speed_placeholder);
        String limitText = speedLimit > 0
                ? res.getString(R.string.navigation_speed_limit_value, speedLimit)
                : res.getString(R.string.navigation_speed_limit_placeholder);

        currentSpeedTextView.setText(currentText);
        speedLimitTextView.setText(limitText);

        int bgRes = isSpeeding ? R.drawable.bg_speed_info_warning : R.drawable.bg_speed_info_safe;
        int textColor = ContextCompat.getColor(speedRow.getContext(),
                isSpeeding ? R.color.speed_warning_text : R.color.speed_safe_text);

        speedRow.setBackgroundResource(bgRes);
        currentSpeedTextView.setTextColor(textColor);
        speedLimitTextView.setTextColor(textColor);
        speedIconView.setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
    }

    public void showNextCommand(@DrawableRes int iconRes, CharSequence labelText) {
        nextCommandContainer.setVisibility(View.VISIBLE);
        nextCommandIconView.setImageResource(iconRes);
        nextCommandLabelView.setText(labelText);
    }

    public void hideNextCommand() {
        nextCommandContainer.setVisibility(View.GONE);
    }

    public void reset() {
        Resources res = directionIconView.getResources();
        updatePrimaryInfo(
                res.getString(R.string.navigation_card_default_manifest),
                res.getString(R.string.keep_going_straight),
                "-",
                R.drawable.go_straight
        );
        updateSummaryInfo(
                res.getString(R.string.navigation_total_distance_placeholder),
                res.getString(R.string.navigation_total_time_placeholder)
        );
        updateSpeedInfo(0, 0, false);
        hideNextCommand();
    }
}
