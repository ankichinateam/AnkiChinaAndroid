/***************************************************************************************
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.anki;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ichi2.libanki.Card;
import com.ichi2.libanki.Deck;
import com.ichi2.libanki.DeckConfig;
import com.ichi2.libanki.Sound;
import com.ichi2.libanki.Utils;
import com.ichi2.libanki.template.Template;
import com.jaygoo.widget.OnRangeChangedListener;
import com.jaygoo.widget.RangeSeekBar;

import androidx.annotation.IdRes;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import static com.ichi2.anki.AbstractFlashcardViewer.getDeckIdForCard;
import static com.ichi2.libanki.Consts.KEY_AUTO_PLAY_TTS;
import static com.ichi2.libanki.Consts.KEY_SELECT_ONLINE_SPEAK_ENGINE;
import static com.ichi2.libanki.Consts.KEY_SHOW_TTS_ICON;


public class SpeakSettingActivity extends AnkiActivity implements View.OnClickListener {
    private TextView tx_front_language, tx_back_language;
    private RangeSeekBar sb_speak_speed;
    private SwitchCompat switch_auto_speak;
    private SwitchCompat switch_use_tts;
    private SwitchCompat switch_show_icon;

    public static int REQUEST_CODE_SPEAK_SETTING=1003;
    public static void OpenSpeakSetting(long cardID, long did, Context context) {
        Intent intent = new Intent(context, SpeakSettingActivity.class);
        intent.putExtra("cid", cardID);
        intent.putExtra("did", did);
        ((Activity)context).startActivityForResult(intent,REQUEST_CODE_SPEAK_SETTING);
    }


    @Override
    protected boolean isStatusBarTransparent() {
        return true;
    }


    @Override
    protected int getStatusBarColorAttr() {
        return R.attr.reviewStatusBarColor;
    }


    private DeckConfig mOptions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speak_setting);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle("");

            int[] attrs = new int[] {
                    R.attr.reviewStatusBarColor,
                    R.attr.primaryTextColor,
            };
            TypedArray ta = obtainStyledAttributes(attrs);
            toolbar.setBackground(ta.getDrawable(0));
            ((TextView) toolbar.findViewById(R.id.toolbar_title)).setText("朗读设置");
            ((TextView) toolbar.findViewById(R.id.toolbar_title)).setTextColor(ta.getColor(1, ContextCompat.getColor(this, R.color.black)));
            // Decide which action to take when the navigation button is tapped.
//            toolbar.setNavigationOnClickListener(v -> onNavigationPressed());
        }
        tx_front_language = findViewById(R.id.tx_front_language);
        tx_back_language = findViewById(R.id.tx_back_language);
        findViewById2(R.id.rl_front_language);
        findViewById2(R.id.rl_back_language);
        findViewById2(R.id.rl_engine);
        sb_speak_speed = findViewById(R.id.sb_speak_speed);
        switch_auto_speak = findViewById(R.id.switch_speak_auto);
        switch_use_tts = findViewById(R.id.switch_tts_first);
        switch_show_icon = findViewById(R.id.switch_speak_icon_show);
        setTitle("朗读设置");
        long did = getIntent().getLongExtra("did", -1);
        long cid = getIntent().getLongExtra("cid", -1);

        mCurrentCard = getCol().getCard(cid);
        mCurrentDeck = getCol().getDecks().get(did);
        int ord = mCurrentCard.getOrd();
        ReadText.releaseTts();
        ReadText.initializeTts(this, true, false, null);
        if (ReadText.getLanguageName(did, ord, Sound.SoundSide.QUESTION).isEmpty()) {
            ReadText.selectDefaultLanguage(getTextForTts(mCurrentCard.q(true)), getDeckIdForCard(mCurrentCard), mCurrentCard.getOrd(),
                    Sound.SoundSide.QUESTION, true);
        }
        if (ReadText.getLanguageName(did, ord, Sound.SoundSide.ANSWER).isEmpty()) {
            ReadText.selectDefaultLanguage(getTextForTts(mCurrentCard.q(true)), getDeckIdForCard(mCurrentCard), mCurrentCard.getOrd(),
                    Sound.SoundSide.ANSWER, true);
        }
        tx_front_language.setText(ReadText.getLanguageName(did, ord, Sound.SoundSide.QUESTION));
        tx_back_language.setText(ReadText.getLanguageName(did, ord, Sound.SoundSide.ANSWER));
        sb_speak_speed.setRange(50, 200);
        sb_speak_speed.setProgress(ReadText.getSpeechRate(did, ord) * 100);
        sb_speak_speed.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                ReadText.setTtsSpeed(did, ord, leftValue / 100);
            }


            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }


            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }
        });
//        DeckConfig mOptions = getCol().getDecks().confForDid(getDeckIdForCard(mCurrentCard));
        switch_auto_speak.setChecked(AnkiDroidApp.getSharedPrefs(this).getBoolean(KEY_AUTO_PLAY_TTS,false));
        switch_auto_speak.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AnkiDroidApp.getSharedPrefs(this).edit().putBoolean(KEY_AUTO_PLAY_TTS,isChecked).apply();
            if(!isChecked)switch_use_tts.setChecked(false);
//            applyAutoPlay(mOptions, isChecked);
        });

        switch_use_tts.setChecked(AnkiDroidApp.getSharedPrefs(this).getBoolean("tts", false));
        switch_use_tts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AnkiDroidApp.getSharedPrefs(this).edit().putBoolean("tts",isChecked).apply();
            if(isChecked)switch_auto_speak.setChecked(true);
        });

        switch_show_icon.setChecked(AnkiDroidApp.getSharedPrefs(this).getBoolean(KEY_SHOW_TTS_ICON, true));
        switch_show_icon.setOnCheckedChangeListener((buttonView, isChecked) -> AnkiDroidApp.getSharedPrefs(this).edit().putBoolean(KEY_SHOW_TTS_ICON,isChecked).apply());

    }


//    private void applyAutoPlay(DeckConfig mOptions, boolean autoplay) {
//        mOptions.put("autoplay", autoplay);
//        try {
//            getCol().getDecks().save(mOptions);
//        } catch (RuntimeException e) {
//            Timber.e(e, "DeckOptions - RuntimeException on saving conf");
//            AnkiDroidApp.sendExceptionReport(e, "DeckOptionsSaveConf");
//            setResult(DeckPicker.RESULT_DB_ERROR);
//        }
//    }


    public <T extends View> T findViewById2(@IdRes int id) {
        T view = findViewById(id);
        view.setOnClickListener(this);
        return view;
    }


    protected Card mCurrentCard;
    protected Deck mCurrentDeck;


    @Override
    protected void onResume() {
        super.onResume();
        ((TextView)findViewById(R.id.tx_engine)).setText(AnkiDroidApp.getSharedPrefs(this).getBoolean(KEY_SELECT_ONLINE_SPEAK_ENGINE,false)?"在线引擎":"离线引擎");
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.rl_front_language:
                ReadText.selectTts(getTextForTts(mCurrentCard.q(true)), getDeckIdForCard(mCurrentCard), mCurrentCard.getOrd(),
                        Sound.SoundSide.QUESTION, true, (name, id1) -> tx_front_language.setText(name));
                break;

            case R.id.rl_back_language:
                ReadText.selectTts(getTextForTts(mCurrentCard.getPureAnswer()), getDeckIdForCard(mCurrentCard),
                        mCurrentCard.getOrd(), Sound.SoundSide.ANSWER, true, (name, id12) -> tx_back_language.setText(name));
                break;
            case R.id.rl_engine:
                SwitchEngineActivity.OpenSwitchEngineActivity(this);
                break;
        }


    }


    private String getTextForTts(String text) {
        String clozeReplacement = this.getString(R.string.reviewer_tts_cloze_spoken_replacement);
        String clozeReplaced = text.replace(Template.CLOZE_DELETION_REPLACEMENT, clozeReplacement);
        return Utils.stripHTML(clozeReplaced);
    }


}
