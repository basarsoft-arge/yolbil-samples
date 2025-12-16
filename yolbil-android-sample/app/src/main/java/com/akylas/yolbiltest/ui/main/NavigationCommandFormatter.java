package com.akylas.yolbiltest.ui.main;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.DrawableRes;

import com.akylas.yolbiltest.R;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 *     NavigationInfoCard mantığını yeniden kullanabilmek için
 * komut → metin/icon eşleşmelerini tek bir yerde tutar.
 */
public final class NavigationCommandFormatter {

    private NavigationCommandFormatter() {
    }

    public static String formatDistance(double distanceInMeters) {
        if (distanceInMeters <= 0) {
            return "-";
        }
        if (distanceInMeters >= 1000) {
            double kmValue = distanceInMeters / 1000d;
            DecimalFormat decimalFormat = new DecimalFormat("0.0");
            return decimalFormat.format(kmValue) + " km";
        }
        int rounded = (int) (distanceInMeters / 10) * 10;
        if (rounded == 0) {
            rounded = 10;
        }
        return rounded + " m";
    }


    /** Manifest başlığı için boşsa varsayılan metne düşer. */
    public static String resolveManifestText(Context context, String manifestText) {
        if (!TextUtils.isEmpty(manifestText)) {
            return manifestText;
        }
        return context.getString(R.string.navigation_card_default_manifest);
    }

    @DrawableRes
    public static int getIconForCommand(String command) {
        String value = normalize(command);
        if (value.contains("TURN_RIGHT_SHARP")) return R.drawable.turn_right_sharp;
        if (value.contains("TURN_FAR_RIGHT")) return R.drawable.turn_far_right;
        if (value.contains("TURN_SECOND_RIGHT")) return R.drawable.turn_second_right;
        if (value.contains("TURN_THIRD_RIGHT")) return R.drawable.turn_third_right;
        if (value.contains("TURN_RIGHT_AT_THE_END_OF_ROAD")) return R.drawable.turn_right_at_the_end_of_road;
        if (value.contains("TURN_RIGHT")) return R.drawable.turn_right;

        if (value.contains("TURN_LEFT_SHARP")) return R.drawable.turn_left_sharp;
        if (value.contains("TURN_FAR_LEFT")) return R.drawable.turn_far_left;
        if (value.contains("TURN_SECOND_LEFT")) return R.drawable.turn_second_left;
        if (value.contains("TURN_THIRD_LEFT")) return R.drawable.turn_third_left;
        if (value.contains("TURN_LEFT_AT_THE_END_OF_ROAD")) return R.drawable.turn_left_at_the_end_of_road;
        if (value.contains("TURN_LEFT")) return R.drawable.turn_left;

        if (value.contains("TAKE_FIRST_EXIT_ON_ROUNDABOUT")) return R.drawable.take_first_exit_on_roundabout;
        if (value.contains("TAKE_SECOND_EXIT_ON_ROUNDABOUT")) return R.drawable.take_second_exit_on_roundabout;
        if (value.contains("TAKE_THIRD_EXIT_ON_ROUNDABOUT")) return R.drawable.take_third_exit_on_roundabout;
        if (value.contains("TAKE_FOURTH_EXIT_ON_ROUNDABOUT")) return R.drawable.take_fourth_exit_on_roundabout;
        if (value.contains("TAKE_FIFTH_EXIT_ON_ROUNDABOUT")) return R.drawable.take_fifth_exit_on_roundabout;
        if (value.contains("TAKE_SIXTH_EXIT_ON_ROUNDABOUT")) return R.drawable.take_sixth_exit_on_roundabout;

        if (value.contains("STAY_RIGHT")) return R.drawable.stay_right;
        if (value.contains("STAY_LEFT")) return R.drawable.stay_left;
        if (value.contains("CONTINUE_RIGHT")) return R.drawable.continue_right;
        if (value.contains("CONTINUE_LEFT")) return R.drawable.continue_left;
        if (value.contains("CONTINUE_MIDDLE")) return R.drawable.continue_middle;
        if (value.contains("GO_STRAIGHT")) return R.drawable.go_straight;

        if (value.contains("IN_TUNNEL")) return R.drawable.in_tunnel;
        if (value.contains("ABOUT_THE_ENTER_TUNNEL")) return R.drawable.about_the_enter_tunnel;
        if (value.contains("AFTER_TUNNEL")) return R.drawable.after_tunnel;

        if (value.contains("UTURN")) return R.drawable.uturn;

        if (value.contains("REACHED_YOUR_DESTINATION")) return R.drawable.reached_your_destination;
        if (value.contains("WILL_REACH_YOUR_DESTINATION")) return R.drawable.will_reach_your_destination;
        if (value.contains("PEDESTRIAN_ROAD")) return R.drawable.pedestrian_road;
        if (value.contains("OVERPASS")) return R.drawable.overpass;
        if (value.contains("UNDERPASS")) return R.drawable.underpass;
        if (value.contains("EXCEEDED_THE_SPEED_LIMIT")) return R.drawable.exceeded_the_speed_limit;
        if (value.contains("SERVICE_ROAD")) return R.drawable.service_road;

        return R.drawable.go_straight;
    }

    public static String getDirectionText(Context context, String command) {
        String value = normalize(command);
        if (value.contains("TURN_RIGHT_SHARP")) return context.getString(R.string.turn_right_sharp);
        if (value.contains("TURN_FAR_RIGHT")) return context.getString(R.string.turn_far_right);
        if (value.contains("TURN_SECOND_RIGHT")) return context.getString(R.string.turn_second_right);
        if (value.contains("TURN_THIRD_RIGHT")) return context.getString(R.string.turn_third_right);
        if (value.contains("TURN_RIGHT_AT_THE_END_OF_ROAD")) return context.getString(R.string.turn_right_at_the_end_of_road);
        if (value.contains("TURN_RIGHT_ONTO_ACCOMODATION")) return context.getString(R.string.turn_right_onto_accomodation);
        if (value.contains("TURN_RIGHT")) return context.getString(R.string.turn_right);

        if (value.contains("TURN_LEFT_SHARP")) return context.getString(R.string.turn_left_sharp);
        if (value.contains("TURN_FAR_LEFT")) return context.getString(R.string.turn_far_left);
        if (value.contains("TURN_SECOND_LEFT")) return context.getString(R.string.turn_second_left);
        if (value.contains("TURN_THIRD_LEFT")) return context.getString(R.string.turn_third_left);
        if (value.contains("TURN_LEFT_AT_THE_END_OF_ROAD")) return context.getString(R.string.turn_left_at_the_end_of_road);
        if (value.contains("TURN_LEFT_ONTO_ACCOMODATION")) return context.getString(R.string.turn_left_onto_accomodation);
        if (value.contains("TURN_LEFT")) return context.getString(R.string.turn_left);

        if (value.contains("TAKE_FIRST_EXIT_ON_ROUNDABOUT")) return context.getString(R.string.take_first_exit_on_roundabout);
        if (value.contains("TAKE_SECOND_EXIT_ON_ROUNDABOUT")) return context.getString(R.string.take_second_exit_on_roundabout);
        if (value.contains("TAKE_THIRD_EXIT_ON_ROUNDABOUT")) return context.getString(R.string.take_third_exit_on_roundabout);
        if (value.contains("TAKE_FOURTH_EXIT_ON_ROUNDABOUT")) return context.getString(R.string.take_fourth_exit_on_roundabout);
        if (value.contains("TAKE_FIFTH_EXIT_ON_ROUNDABOUT")) return context.getString(R.string.take_fifth_exit_on_roundabout);
        if (value.contains("TAKE_SIXTH_EXIT_ON_ROUNDABOUT")) return context.getString(R.string.take_sixth_exit_on_roundabout);

        if (value.contains("STAY_RIGHT")) return context.getString(R.string.stay_right);
        if (value.contains("STAY_LEFT")) return context.getString(R.string.stay_left);
        if (value.contains("CONTINUE_RIGHT")) return context.getString(R.string.continue_right);
        if (value.contains("CONTINUE_LEFT")) return context.getString(R.string.continue_left);
        if (value.contains("CONTINUE_MIDDLE")) return context.getString(R.string.continue_middle);
        if (value.contains("GO_STRAIGHT")) return context.getString(R.string.go_straight);

        if (value.contains("UTURN")) return context.getString(R.string.uturn);

        if (value.contains("SERVICE_ROAD")) return context.getString(R.string.service_road);
        if (value.contains("UNDERPASS")) return context.getString(R.string.underpass);
        if (value.contains("OVERPASS")) return context.getString(R.string.overpass);
        if (value.contains("PEDESTRIAN_ROAD")) return context.getString(R.string.pedestrian_road);
        if (value.contains("ABOUT_THE_ENTER_TUNNEL")) return context.getString(R.string.about_the_enter_tunnel);
        if (value.contains("IN_TUNNEL")) return context.getString(R.string.in_tunnel);
        if (value.contains("AFTER_TUNNEL")) return context.getString(R.string.after_tunnel);
        if (value.contains("EXCEEDED_THE_SPEED_LIMIT")) return context.getString(R.string.exceeded_the_speed_limit);
        if (value.contains("WILL_REACH_YOUR_DESTINATION")) return context.getString(R.string.will_reach_your_destination);
        if (value.contains("REACHED_YOUR_DESTINATION")) return context.getString(R.string.reached_your_destination);

        return context.getString(R.string.keep_going_straight);
    }

    public static String formatTotalDistance(double meters) {
        if (Double.isNaN(meters) || meters <= 0) {
            return "";
        }
        if (meters < 1000) {
            int rounded = (int) (meters / 10) * 10;
            if (rounded == 0) {
                return "";
            }
            return rounded + " m";
        }
        if (meters < 10_000) {
            double km = meters / 1000d;
            DecimalFormat df = new DecimalFormat("#.#");
            return df.format(km) + " km";
        }
        int kmInt = (int) (meters / 1000d);
        return kmInt + " km";
    }

    public static String formatTotalTime(double seconds) {
        if (Double.isNaN(seconds) || seconds <= 0) {
            return "0 dk";
        }
        if (seconds < 3600) {
            int minutes = (int) Math.ceil(seconds / 60d);
            return minutes + " dk";
        }
        int totalMinutes = (int) Math.ceil(seconds / 60d);
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return hours + " s " + minutes + " dk";
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.toUpperCase(Locale.US);
    }
}
