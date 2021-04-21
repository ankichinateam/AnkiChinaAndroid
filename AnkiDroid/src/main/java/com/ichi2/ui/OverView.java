package com.ichi2.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.ichi2.anki.R;
import com.ichi2.anki.stats.OverviewStatsBuilder;
import com.ichi2.libanki.Utils;
import com.ichi2.libanki.stats.Stats;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import timber.log.Timber;

public class OverView extends FrameLayout {


    public OverView(@NonNull Context context) {
        super(context);
        initView(context);
    }


    public OverView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }


    public OverView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public OverView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }





    private void initView(final Context context) {
        LayoutInflater.from(context).inflate(R.layout.stats_overview, this );
    }


    TextView findTextViewById(int resId) {
        return findViewById(resId);
    }


    private static final int CARDS_INDEX = 0;
    private static final int THETIME_INDEX = 1;
    private static final int FAILED_INDEX = 2;
    private static final int LRN_INDEX = 3;
    private static final int REV_INDEX = 4;
    private static final int RELRN_INDEX = 5;
    private static final int FILT_INDEX = 6;
    private static final int MCNT_INDEX = 7;
    private static final int MSUM_INDEX = 8;


    public void loadDataBuilder(OverviewStatsBuilder builder) {
        Timber.d("loadDataBuilder");
        Resources res = getResources();
        findTextViewById(R.id.tv_today).setText(res.getString(R.string.stats_today));


        final int minutes = (int) Math.round(builder.mTodayStats[THETIME_INDEX] / 60.0);
        final String span = res.getQuantityString(R.plurals.time_span_minutes, minutes, minutes);
        findTextViewById(R.id.tv_today_minute).setText(res.getQuantityString(R.plurals.stats_today_cards,
                builder.mTodayStats[CARDS_INDEX], builder.mTodayStats[CARDS_INDEX], span));

        findTextViewById(R.id.tv_today_repeat).setText(res.getString(R.string.stats_today_again_count, builder.mTodayStats[FAILED_INDEX]));
        if (builder.mTodayStats[CARDS_INDEX] > 0) {
            findTextViewById(R.id.tv_today_correct_rate).setVisibility(VISIBLE);
            findTextViewById(R.id.tv_today_correct_rate).setText(res.getString(R.string.stats_today_correct_count, builder.mTodayStats[CARDS_INDEX]-builder.mTodayStats[FAILED_INDEX],builder.mTodayStats[CARDS_INDEX],(((1 - builder.mTodayStats[FAILED_INDEX] / (float) (builder.mTodayStats[CARDS_INDEX])) * 100.0))));
        } else {
            findTextViewById(R.id.tv_today_correct_rate).setVisibility(GONE);
        }
        findTextViewById(R.id.tv_today_study).setText(res.getString(R.string.stats_today_type_breakdown, builder.mTodayStats[LRN_INDEX], builder.mTodayStats[REV_INDEX], builder.mTodayStats[RELRN_INDEX], builder.mTodayStats[FILT_INDEX]));
        if (builder.mTodayStats[MCNT_INDEX] != 0) {
            findTextViewById(R.id.tv_today_hint).setText(res.getString(R.string.stats_today_mature_cards, builder.mTodayStats[MSUM_INDEX], builder.mTodayStats[MCNT_INDEX], (builder.mTodayStats[MSUM_INDEX] / (float) (builder.mTodayStats[MCNT_INDEX]) * 100.0)));
        } else {
            findTextViewById(R.id.tv_today_hint).setText(res.getString(R.string.stats_today_no_mature_cards));
        }

        findTextViewById(R.id.description).setText(res.getString(builder.mType.descriptionId));

        OverviewStatsBuilder.OverviewStats  oStats=builder.mOverViewStats;
        // FORECAST
        findTextViewById(R.id.tv_predict).setText(res.getString(R.string.stats_forecast).toUpperCase());
        findTextViewById(R.id.tv_predict_all_content).setText(res.getString(R.string.stats_overview_forecast_total, oStats.forecastTotalReviews));
        findTextViewById(R.id.tv_predict_avg_content).setText(res.getString(R.string.stats_overview_forecast_average,  oStats.forecastAverageReviews));
        findTextViewById(R.id.tv_predict_deadline_content).setText(res.getString(R.string.stats_overview_forecast_due_tomorrow,  oStats.forecastDueTomorrow));

        // REVIEW COUNT
        findTextViewById(R.id.tv_review).setText(res.getString(R.string.stats_review_count).toUpperCase());
        findTextViewById(R.id.tv_review_day_content).setText( oStats.daysStudied+"/"+ oStats.allDays);
        findTextViewById(R.id.tv_review_day_percent).setText("("+(int) ((float)  oStats.daysStudied / (float) oStats.allDays * 100)+"%)");
        findTextViewById(R.id.tv_review_all_content).setText(res.getString(R.string.stats_overview_total_reviews, oStats.totalReviews));
        findTextViewById(R.id.tv_review_avg_content).setText(res.getString(R.string.stats_overview_reviews_per_day_studydays, oStats.reviewsPerDayOnStudyDays));
        boolean allDaysStudied = oStats.daysStudied == oStats.allDays;

        if (!allDaysStudied) {
            findTextViewById(R.id.tv_review_hint).setText(res.getString(R.string.stats_overview_reviews_per_day_all, oStats.reviewsPerDayOnAll));
            findTextViewById(R.id.tv_review_hint).setVisibility(VISIBLE);
        }else {
            findTextViewById(R.id.tv_review_hint).setVisibility(GONE);
        }

        //REVIEW TIME
        findTextViewById(R.id.tv_review_time).setText(res.getString(R.string.stats_review_time).toUpperCase());
       findTextViewById(R.id.tv_review_time_count_content).setText( oStats.daysStudied+"/"+ oStats.allDays);
       findTextViewById(R.id.tv_review_time_count_percent).setText("("+(int) ((float)  oStats.daysStudied / (float) oStats.allDays * 100)+"%)");
       findTextViewById(R.id.tv_review_time_all_content).setText(res.getString(R.string.stats_overview_total_time_in_period, Math.round(oStats.totalTime)));
       findTextViewById(R.id.tv_review_time_avg_content).setText(res.getString(R.string.stats_overview_time_per_day_studydays, oStats.timePerDayOnStudyDays));
        double cardsPerMinute = oStats.totalTime == 0 ? 0 : ((double) oStats.totalReviews) / oStats.totalTime;
        double averageAnswerTime = oStats.totalReviews == 0 ? 0 : (oStats.totalTime * 60) / ((double) oStats.totalReviews);
        if (!allDaysStudied) {
            findTextViewById(R.id.tv_review_time_hint).setText(res.getString(R.string.stats_overview_time_per_day_all, oStats.timePerDayOnAll)+","+res.getString(R.string.stats_overview_average_answer_time, averageAnswerTime, cardsPerMinute));
        }else {
            findTextViewById(R.id.tv_review_time_hint).setText( res.getString(R.string.stats_overview_average_answer_time, averageAnswerTime, cardsPerMinute));
        }
        // ADDED
        findTextViewById(R.id.tv_add).setText(res.getString(R.string.stats_added).toUpperCase());
        findTextViewById(R.id.tv_add_all_content).setText(res.getString(R.string.stats_overview_total_new_cards, oStats.totalNewCards));
        findTextViewById(R.id.tv_add_avg_content).setText(res.getString(R.string.stats_overview_new_cards_per_day, oStats.newCardsPerDay));
        // INTERVALS
        findTextViewById(R.id.tv_distance).setText(res.getString(R.string.stats_review_intervals).toUpperCase());
        findTextViewById(R.id.tv_distance_avg).setText(res.getString(R.string.stats_overview_average_interval));
        findTextViewById(R.id.tv_distance_avg_content).setText(Utils.roundedTimeSpan( getContext(), (int) Math.round(oStats.averageInterval * Stats.SECONDS_PER_DAY)));
        findTextViewById(R.id.tv_distance_max).setText(res.getString(R.string.stats_overview_longest_interval));
        findTextViewById(R.id.tv_distance_all_content).setText(Utils.roundedTimeSpan( getContext(), (int) Math.round(oStats.longestInterval * Stats.SECONDS_PER_DAY)));

        //ANSWER BUTTONS
        findTextViewById(R.id.tv_answer_button).setText(res.getString(R.string.stats_answer_buttons).toUpperCase());
        findTextViewById(R.id.tv_answer_study_content).setText(res.getString(R.string.stats_overview_answer_buttons_learn, oStats.newCardsOverview.getPercentage() ));
        findTextViewById(R.id.tv_answer_study_percent).setText("("+oStats.newCardsOverview.correct+"/"+ oStats.newCardsOverview.total+")");

        findTextViewById(R.id.tv_answer_not_familiar_content).setText(res.getString(R.string.stats_overview_answer_buttons_young, oStats.youngCardsOverview.getPercentage() ));
        findTextViewById(R.id.tv_answer_not_familiar_percent).setText("("+oStats.youngCardsOverview.correct+"/"+ oStats.youngCardsOverview.total+")");

        findTextViewById(R.id.tv_answer_familiar_content).setText(res.getString(R.string.stats_overview_answer_buttons_mature, oStats.matureCardsOverview.getPercentage() ));
        findTextViewById(R.id.tv_answer_familiar_percent).setText("("+oStats.matureCardsOverview.correct+"/"+ oStats.matureCardsOverview.total+")");

        //CARD TYPES
        findTextViewById(R.id.tv_note_type).setText(res.getString(R.string.stats_cards_types).toUpperCase());
        findTextViewById(R.id.tv_note_type_all_card ).setText(R.string.stats_overview_card_types_total_cards);
        findTextViewById(R.id.tv_note_type_all_card_content).setText(""+oStats.totalCards);

        findTextViewById(R.id.tv_note_type_all).setText(R.string.stats_overview_card_types_total_notes);
        findTextViewById(R.id.tv_note_type_all_content).setText(""+oStats.totalNotes);

        findTextViewById(R.id.tv_note_low_level).setText(R.string.stats_overview_card_types_lowest_ease);
        findTextViewById(R.id.tv_note_low_level_content).setText(oStats.lowestEase+"%");
        findTextViewById(R.id.tv_note_avg_level).setText(R.string.stats_overview_card_types_average_ease);
        findTextViewById(R.id.tv_note_avg_level_content).setText(oStats.averageEase+"%");
        findTextViewById(R.id.tv_note_high_level).setText(R.string.stats_overview_card_types_highest_ease);
        findTextViewById(R.id.tv_note_high_level_content).setText(oStats.highestEase+"%");

    }


//    private void appendOverViewStats(StringBuilder stringBuilder) {
//
//        //ANSWER BUTTONS
//        stringBuilder.append(_subtitle(res.getString(R.string.stats_answer_buttons).toUpperCase()));
//        stringBuilder.append(res.getString(R.string.stats_overview_answer_buttons_learn, oStats.newCardsOverview.getPercentage(), oStats.newCardsOverview.correct, oStats.newCardsOverview.total));
//        stringBuilder.append("<br>");
//        stringBuilder.append(res.getString(R.string.stats_overview_answer_buttons_young, oStats.youngCardsOverview.getPercentage(), oStats.youngCardsOverview.correct, oStats.youngCardsOverview.total));
//        stringBuilder.append("<br>");
//        stringBuilder.append(res.getString(R.string.stats_overview_answer_buttons_mature, oStats.matureCardsOverview.getPercentage(), oStats.matureCardsOverview.correct, oStats.matureCardsOverview.total));
//
//        //CARD TYPES
//        stringBuilder.append(_subtitle(res.getString(R.string.stats_cards_types).toUpperCase()));
//        stringBuilder.append(res.getString(R.string.stats_overview_card_types_total_cards, oStats.totalCards));
//        stringBuilder.append("<br>");
//        stringBuilder.append(res.getString(R.string.stats_overview_card_types_total_notes, oStats.totalNotes));
//        stringBuilder.append("<br>");
//        stringBuilder.append(res.getString(R.string.stats_overview_card_types_lowest_ease, oStats.lowestEase));
//        stringBuilder.append("<br>");
//        stringBuilder.append(res.getString(R.string.stats_overview_card_types_average_ease, oStats.averageEase));
//        stringBuilder.append("<br>");
//        stringBuilder.append(res.getString(R.string.stats_overview_card_types_highest_ease, oStats.highestEase));
//
//    }


//    private void appendTodaysStats(StringBuilder stringBuilder) {
//        Stats stats = new Stats(mCol, mDeckId);
//        int[] todayStats = stats.calculateTodayStats();
//        stringBuilder.append(_title(mOverView.getResources().getString(R.string.stats_today)));
//        Resources res = mOverView.getResources();
//        final int minutes = (int) Math.round(todayStats[THETIME_INDEX] / 60.0);
//        final String span = res.getQuantityString(R.plurals.time_span_minutes, minutes, minutes);
//        stringBuilder.append(res.getQuantityString(R.plurals.stats_today_cards,
//                todayStats[CARDS_INDEX], todayStats[CARDS_INDEX], span));
//        stringBuilder.append("<br>");
//        stringBuilder.append(res.getString(R.string.stats_today_again_count, todayStats[FAILED_INDEX]));
//        if (todayStats[CARDS_INDEX] > 0) {
//            stringBuilder.append(" ");
//            stringBuilder.append(res.getString(R.string.stats_today_correct_count, (((1 - todayStats[FAILED_INDEX] / (float) (todayStats[CARDS_INDEX])) * 100.0))));
//        }
//        stringBuilder.append("<br>");
//        stringBuilder.append(res.getString(R.string.stats_today_type_breakdown, todayStats[LRN_INDEX], todayStats[REV_INDEX], todayStats[RELRN_INDEX], todayStats[FILT_INDEX]));
//        stringBuilder.append("<br>");
//        if (todayStats[MCNT_INDEX] != 0) {
//            stringBuilder.append(res.getString(R.string.stats_today_mature_cards, todayStats[MSUM_INDEX], todayStats[MCNT_INDEX], (todayStats[MSUM_INDEX] / (float) (todayStats[MCNT_INDEX]) * 100.0)));
//        } else {
//            stringBuilder.append(res.getString(R.string.stats_today_no_mature_cards));
//        }
//    }


//    public static class StatisticData{
//        public String today_minute;
//        public int today_card_count;
//        public int today_repeat_count;
//        public int today_study;
//        public int today_review;
//        public int today_restudy;
//        public int today_filter;
//        public double today_correct_rate;
//
//        public String description;
//        public String forecast_total;
//        public String forecast_avg;
//        public String forecast_due_tomorrow;
//
//        public String review_count_days_studied;
//        public String review_count_days_percent;
//        public String review_count_total;
//        public String review_count_avg;
//        public String review_count_hint_count;
//
//        public String review_time_days_signed;
//        public String review_time_days_percent;
//        public String review_time_total;
//        public String review_time_avg;
//        public String review_time_hint_cost_time;
//        public String review_time_hint_avg_cost_time;
//        public String review_time_hint_avg_cost_time_per_card;
//
//        public String added_all;
//        public String added_avg;
//
//        public String intervals_avg;
//        public String intervals_max;
//
//        public String answer_learn_correct;
//        public String answer_learn_correct_percent;
//        public String answer_young_correct;
//        public String answer_young_correct_percent;
//        public String answer_mature_correct;
//        public String answer_mature_correct_percent;
//
//        public String type_all_card_count;
//        public String type_all_note_count;
//        public  String type_lowest_ease_percent;
//        public String type_lowest_average_percent;
//        public String type_lowest_highest_percent;
//    }

}
