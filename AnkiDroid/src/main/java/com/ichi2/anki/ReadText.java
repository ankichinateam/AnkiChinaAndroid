/****************************************************************************************
 * Copyright (c) 2011 Norbert Nagold <norbert.nagold@gmail.com>                         *
 *                                                                                      *
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

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;

import android.view.WindowManager;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.snackbar.Snackbar;
import com.ichi2.compat.Compat;
import com.ichi2.compat.CompatHelper;
import com.ichi2.libanki.Sound;
import com.ichi2.ui.CustomStyleDialog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import timber.log.Timber;


public class ReadText {
    private static TextToSpeech mTts;
    private static ArrayList<Locale> availableTtsLocales = new ArrayList<>();
    private static String mTextToSpeak;
    private static WeakReference<Context> mReviewer;
    private static long mDid;
    private static int mOrd;
    private static Sound.SoundSide mQuestionAnswer;
    public static final String NO_TTS = "0";
    public static ArrayList<String[]> sTextQueue = new ArrayList<>();
    private static Compat compat = CompatHelper.getCompat();
    private static Object mTtsParams = compat.initTtsParams();


    public static Sound.SoundSide getmQuestionAnswer() {
        return mQuestionAnswer;
    }


    public static boolean isSpeaking() {
        return mTts != null && mTts.isSpeaking();
    }


    public static boolean isInit() {
        return mTts != null;
    }


    public static void speak(String text, String loc, int queueMode) {
        int result = mTts.setLanguage(localeFromStringIgnoringScriptAndExtensions(loc));
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(mReviewer.get(), mReviewer.get().getString(R.string.no_tts_available_message)
                    + " (" + loc + ")", Toast.LENGTH_LONG).show();
            Timber.e("Error loading locale " + loc);
        } else {

            if (mTts.isSpeaking() && queueMode == TextToSpeech.QUEUE_FLUSH) {
                Timber.d("tts engine appears to be busy... clearing queue");
                stopTts();
                //sTextQueue.add(new String[] { text, loc });
            }
            Timber.d("tts text '%s' to be played for locale (%s)", text, loc);

            compat.speak(mTts, mTextToSpeak, queueMode, mTtsParams, "stringId");
//            compat.speak(mTts, mFinalLoadContent, queueMode, mTtsParams, "stringId");
        }
    }


    public static String getLanguage(long did, int ord, Sound.SoundSide qa) {
        return MetaDB.getLanguage(mReviewer.get(), did, ord, qa);
    }

    public static String getAzureLanguage(long did, int ord, Sound.SoundSide qa) {
        return MetaDB.getAzureLanguages(mReviewer.get(), did, ord, qa);
    }
    public static String getAzureLanguageDisplay(long did, int ord, Sound.SoundSide qa) {
        return MetaDB.getAzureLanguageDisplay(mReviewer.get(), did, ord, qa);
    }
    public static float getSpeechRate(long did, int ord) {
        return MetaDB.getSpeech(mReviewer.get(), did, ord);
    }


    public static String getLanguageName(long did, int ord, Sound.SoundSide qa) {
        for (Locale locale : availableTtsLocales) {
            if (locale.getISO3Language().equals(MetaDB.getLanguage(mReviewer.get(), did, ord, qa))) {
                return locale.getDisplayName();
            }
        }
        return "";
    }


    public static void setTtsSpeed(long did, int ord, float speed) {
        if (mTts != null) {
            mTts.setSpeechRate(speed);
            MetaDB.storeSpeech(mReviewer.get(), did, ord, speed);
        }
    }


    public interface SelectTtsCallback {
        void onSelectTts(String name, String id);
    }


    public static void selectDefaultLanguage(String text, long did, int ord, Sound.SoundSide qa, boolean vipSpeak) {//在设置页面时需要知道最后选择了什么语言，通过callback获得
        mTextToSpeak = text;
        mQuestionAnswer = qa;
        mDid = did;
        mOrd = ord;
        // Build the language list if it's empty
        if (availableTtsLocales.isEmpty()) {
            buildAvailableLanguages(TextToSpeech.LANG_AVAILABLE);
        }
        if (availableTtsLocales.size() > 0) {
            for (int i = 0; i < availableTtsLocales.size(); i++) {//vip里的设置，默认选择中文
                String name = availableTtsLocales.get(i).getDisplayName();
                if ((name.equals("简体中文")
                        || name.equals("中文(简体)")
                        || name.equals("中文(简体中文)" )
                        || name.equals("中文(简体中文,中国)" )
                        || name.equals("中文（简体）")
                        || name.equals("中文（简体中文）")
                        || name.equals("中文")
                        || name.equals("中国")
                        || name.equals("中文(繁体中文)")
                        || name.equalsIgnoreCase("chinese"))
                        ||(name.contains("chinese")
                        || name.contains("Chinese")
                        || name.contains("中文")
                        || name.contains("中国"))) {
                    String locale = availableTtsLocales.get(i).getISO3Language();//默认选择中文
                    MetaDB.storeLanguage(mReviewer.get(), mDid, mOrd, mQuestionAnswer, locale);
                    break;
                }
            }
        }
    }


    /**
     * Ask the user what language they want.
     *
     * @param text The text to be read
     * @param did  The deck id
     * @param ord  The card template ordinal
     * @param qa   The card question or card answer
     */
    public static void selectTts(String text, long did, int ord, Sound.SoundSide qa, boolean vipSpeak, SelectTtsCallback callback) {//在设置页面时需要知道最后选择了什么语言，通过callback获得
        Timber.w("selectTts:%s", vipSpeak);
        //TODO: Consolidate with ReadText.readCardSide
        mTextToSpeak = text;
        mQuestionAnswer = qa;
        mDid = did;
        mOrd = ord;
        Resources res = mReviewer.get().getResources();
        final MaterialDialog.Builder builder = new MaterialDialog.Builder(mReviewer.get());
        // Build the language list if it's empty
        if (availableTtsLocales.isEmpty()) {
            buildAvailableLanguages(TextToSpeech.LANG_AVAILABLE);
        }
        if (availableTtsLocales.size() == 0) {
            Timber.w("ReadText.textToSpeech() no TTS languages available");
            builder.content(res.getString(R.string.no_tts_available_message))
                    .iconAttr(R.attr.dialogErrorIcon)
                    .positiveText(res.getString(R.string.dialog_ok));
        } else {
            if (vipSpeak) {
                for (int i = 0; i < availableTtsLocales.size(); i++) {//vip里的设置，默认选择中文
                    ArrayList<CharSequence> temp = new ArrayList<>();
                    temp.add(availableTtsLocales.get(i).getDisplayName());
                    String name = availableTtsLocales.get(i).getDisplayName();
                    if (name.contains("chinese")
                            || name.contains("Chinese")
                            || name.contains("中文")
                            || name.contains("中国")) {
                        String locale = availableTtsLocales.get(i).getISO3Language();//默认选择中文
                        MetaDB.storeLanguage(mReviewer.get(), mDid, mOrd, mQuestionAnswer, locale);
//                        return;
                    }
                }
            }

            ArrayList<CharSequence> dialogItems = new ArrayList<>();
            final ArrayList<String> dialogIds = new ArrayList<>();
            // Add option: "no tts"
            dialogItems.add(res.getString(R.string.tts_no_tts));
            dialogIds.add(NO_TTS);
            for (int i = 0; i < availableTtsLocales.size(); i++) {
                dialogItems.add(availableTtsLocales.get(i).getDisplayName());
                dialogIds.add(availableTtsLocales.get(i).getISO3Language());
            }
            String[] items = new String[dialogItems.size()];
            dialogItems.toArray(items);
            builder.title(res.getString(R.string.select_locale_title))
                    .items(items)
                    .itemsCallback((materialDialog, view, which, charSequence) -> {
                        String locale = dialogIds.get(which);
                        Timber.d("ReadText.selectTts() user chose locale '%s'", locale);
                        if (!locale.equals(NO_TTS) && callback == null) {
                            speak(mTextToSpeak, locale, TextToSpeech.QUEUE_FLUSH);
                        }
                        MetaDB.storeLanguage(mReviewer.get(), mDid, mOrd, mQuestionAnswer, locale);
                        if (callback != null) {
                            callback.onSelectTts(dialogItems.get(which).toString(), locale);
                        }
                    });
        }
        // Show the dialog after short delay so that user gets a chance to preview the card
        final Handler handler = new Handler();
        final int delay = 500;
        handler.postDelayed(() -> {
            try {
                builder.build().show();
            } catch (WindowManager.BadTokenException e) {
                Timber.w("Activity invalidated before TTS language dialog could display");
            }
        }, delay);
    }

    public static void selectTtsForVip(String text, long cid, long did, int ord, Sound.SoundSide qa, boolean vipSpeak) {
        Timber.w("selectTts:%s", vipSpeak);
        //TODO: Consolidate with ReadText.readCardSide
        mTextToSpeak = text;
        mQuestionAnswer = qa;
        mDid = did;
        mOrd = ord;
        Resources res = mReviewer.get().getResources();
        final MaterialDialog.Builder builder = new MaterialDialog.Builder(mReviewer.get());
        // Build the language list if it's empty
        if (availableTtsLocales.isEmpty()) {
            buildAvailableLanguages(TextToSpeech.LANG_AVAILABLE);
        }
        if (availableTtsLocales.size() == 0) {
            Timber.w("ReadText.textToSpeech() no TTS languages available");
            builder.content(res.getString(R.string.no_tts_available_message))
                    .iconAttr(R.attr.dialogErrorIcon)
                    .positiveText(res.getString(R.string.dialog_ok));
        } else {
            if (vipSpeak) {
                for (int i = 0; i < availableTtsLocales.size(); i++) {//vip里的设置，默认选择中文
                    String name = availableTtsLocales.get(i).getDisplayName();
                    if (name.contains("chinese")
                            || name.contains("Chinese")
                            || name.contains("中文")
                            || name.contains("中国")) {
                        String locale = availableTtsLocales.get(i).getISO3Language();//默认选择中文
//                        if (!locale.equals(NO_TTS)) {
//                            speak(mTextToSpeak, locale, TextToSpeech.QUEUE_FLUSH);
//                        }
                        MetaDB.storeLanguage(mReviewer.get(), mDid, mOrd, mQuestionAnswer, locale);
                        return;
                    }
                }
                CustomStyleDialog d = new CustomStyleDialog.Builder(mReviewer.get())
                        .setCustomLayout(R.layout.dialog_common_custom_next)
                        .setTitle("首次朗读，请设置语言")
                        .centerTitle()
                        .setMessage("设置语言后，朗读效果更优，还可以选择是否自动朗读。")
                        .setPositiveButton("前往设置", (dialog, which) -> {
                            dialog.dismiss();
                            SpeakSettingActivity.OpenSpeakSetting(cid, did, mReviewer.get());
                        })
                        .create();
                d.show();
            }

        }
    }


    /**
     * Read a card side using a TTS service.
     *
     * @param cardSide         Card side to be read; SoundSide.SOUNDS_QUESTION or SoundSide.SOUNDS_ANSWER.
     * @param cardSideContents Contents of the card side to be read, in HTML format. If it contains
     *                         any &lt;tts service="android"&gt; elements, only their contents is
     *                         read; otherwise, all text is read. See TtsParser for more details.
     * @param did              Index of the deck containing the card.
     * @param ord              The card template ordinal.
     */
    public static void readCardSide(Sound.SoundSide cardSide, String cardSideContents, long cid, long did, int ord, String clozeReplacement, boolean vipSpeak) {
        boolean isFirstText = true;
        for (TtsParser.LocalisedText textToRead : TtsParser.getTextsToRead(cardSideContents, clozeReplacement)) {
            Timber.i("text to read:" + textToRead.getText());
            if (!textToRead.getText().isEmpty()) {
                textToSpeech(textToRead.getText(), cid, did, ord, cardSide,
                        textToRead.getLocaleCode(),
                        isFirstText ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, vipSpeak);
                isFirstText = false;
            }
        }
    }


    /**
     * Read the given text using an appropriate TTS voice.
     * <p>
     * The voice is chosen as follows:
     * <p>
     * 1. If localeCode is a non-empty string representing a locale in the format returned
     * by Locale.toString(), and a voice matching the language of this locale (and ideally,
     * but not necessarily, also the country and variant of the locale) is available, then this
     * voice is used.
     * 2. Otherwise, if the database contains a saved language for the given 'did', 'ord' and 'qa'
     * arguments, and a TTS voice matching that language is available, then this voice is used
     * (unless the saved language is NO_TTS, in which case the text is not read at all).
     * 3. Otherwise, the user is asked to select a language from among those for which a voice is
     * available.
     *
     * @param queueMode TextToSpeech.QUEUE_ADD or TextToSpeech.QUEUE_FLUSH.
     */
    private static void textToSpeech(String text, long cid, long did, int ord, Sound.SoundSide qa, String localeCode,
                                     int queueMode, boolean vipSpeak) {
        mTextToSpeak = text;
        mQuestionAnswer = qa;
        mDid = did;
        mOrd = ord;
        Timber.d("ReadText.textToSpeech() method started for string '%s', locale '%s'", text, localeCode);

        final String originalLocaleCode = localeCode;

        if (!localeCode.isEmpty()) {
            if (!isLanguageAvailable(localeCode)) {
                localeCode = "";
            }
        }
        if (localeCode.isEmpty()) {
            // get the user's existing language preference
            localeCode = getLanguage(mDid, mOrd, mQuestionAnswer);
            Timber.d("ReadText.textToSpeech() method found language choice '%s'", localeCode);
        }

        if (localeCode.equals(NO_TTS)) {
            // user has chosen not to read the text
            return;
        }
        mTts.setSpeechRate(ReadText.getSpeechRate(mDid, mOrd));
        if (!localeCode.isEmpty() && isLanguageAvailable(localeCode)) {
            speak(mTextToSpeak, localeCode, queueMode);
            return;
        }

        // Otherwise ask the user what language they want to use
        if (!originalLocaleCode.isEmpty()) {
            // (after notifying them first that no TTS voice was found for the locale
            // they originally requested)
            Toast.makeText(mReviewer.get(), mReviewer.get().getString(R.string.no_tts_available_message)
                    + " (" + originalLocaleCode + ")", Toast.LENGTH_LONG).show();
        }
//        if (vipSpeak) {
//            selectTtsForVip(mTextToSpeak, cid, mDid, mOrd, mQuestionAnswer, true);
//        }
//        else {
            selectTts(mTextToSpeak, mDid, mOrd, mQuestionAnswer, true, null);
//        }
    }


    /**
     * Convert a string representation of a locale, in the format returned by Locale.toString(),
     * into a Locale object, disregarding any script and extensions fields (i.e. using solely the
     * language, country and variant fields).
     * <p>
     * Returns a Locale object constructed from an empty string if the input string is null, empty
     * or contains more than 3 fields separated by underscores.
     */
    private static Locale localeFromStringIgnoringScriptAndExtensions(String localeCode) {
        if (localeCode == null) {
            return new Locale("");
        }

        localeCode = stripScriptAndExtensions(localeCode);

        String[] fields = localeCode.split("_");
        switch (fields.length) {
            case 1:
                return new Locale(fields[0]);
            case 2:
                return new Locale(fields[0], fields[1]);
            case 3:
                return new Locale(fields[0], fields[1], fields[2]);
            default:
                return new Locale("");
        }
    }


    private static String stripScriptAndExtensions(String localeCode) {
        int hashPos = localeCode.indexOf('#');
        if (hashPos >= 0) {
            localeCode = localeCode.substring(0, hashPos);
        }
        return localeCode;
    }


    /**
     * Returns true if the TTS engine supports the language of the locale represented by localeCode
     * (which should be in the format returned by Locale.toString()), false otherwise.
     */
    private static boolean isLanguageAvailable(String localeCode) {
        return mTts.isLanguageAvailable(localeFromStringIgnoringScriptAndExtensions(localeCode)) >=
                TextToSpeech.LANG_AVAILABLE;
    }


    public static String initializeTts(Context context, boolean vipSpeak, boolean showToast, ReadTextListener listener) {
        // Store weak reference to Activity to prevent memory leak
        mReviewer = new WeakReference<>(context);
        // Create new TTS object and setup its onInit Listener
        mTts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // build list of available languages
                buildAvailableLanguages(TextToSpeech.LANG_AVAILABLE);
                if (availableTtsLocales.size() > 0) {
                    // notify the reviewer that TTS has been initialized
                    Timber.d("TTS initialized and available languages found");
                    if (listener != null) {
                        listener.ttsInitialized();
                    }
//                    ((AbstractFlashcardViewer) mReviewer.get()).ttsInitialized();
                } else {
                    if (showToast) {
                        Toast.makeText(mReviewer.get(), mReviewer.get().getString(R.string.no_tts_available_message), Toast.LENGTH_LONG).show();
                    }
                    Timber.w("TTS initialized but no available languages found");
                }
                mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onDone(String arg0) {
                        if (ReadText.sTextQueue.size() > 0) {
                            String[] text = ReadText.sTextQueue.remove(0);
                            ReadText.speak(text[0], text[1], TextToSpeech.QUEUE_FLUSH);
                        }
                        if (listener != null) {
                            listener.onDone();
                        }
                    }


                    @Override
                    @Deprecated
                    public void onError(String utteranceId) {
                        Timber.v("Andoid TTS failed. Check logcat for error. Indicates a problem with Android TTS engine.");

                        final Uri helpUrl = Uri.parse(mReviewer.get().getString(R.string.link_faq_tts));
                        final AnkiActivity ankiActivity = (AnkiActivity) mReviewer.get();
                        ankiActivity.mayOpenUrl(helpUrl);
                        if (showToast) {
                            UIUtils.showSnackbar(ankiActivity, R.string.no_tts_available_message, false, R.string.help,
                                    v -> openTtsHelpUrl(helpUrl), ankiActivity.findViewById(R.id.root_layout),
                                    new Snackbar.Callback());
                        }
                    }


                    @Override
                    public void onStart(String arg0) {
                        // no nothing
                    }
                });
            } else {
                if (showToast) {
                    Toast.makeText(mReviewer.get(), mReviewer.get().getString(R.string.no_tts_available_message), Toast.LENGTH_LONG).show();
                }
                Timber.w("TTS not successfully initialized");
            }
        });

        // Show toast that it's getting initialized, as it can take a while before the sound plays the first time
        if (showToast) {
            Toast.makeText(context, context.getString(R.string.initializing_tts), Toast.LENGTH_LONG).show();
        }

        return mTts.getDefaultEngine();
    }


    private static void openTtsHelpUrl(Uri helpUrl) {
        AnkiActivity activity = (AnkiActivity) mReviewer.get();
        activity.openUrl(helpUrl);
    }


    public static void buildAvailableLanguages(int lowestCode) {
        availableTtsLocales.clear();
        Locale[] systemLocales = Locale.getAvailableLocales();
        for (Locale loc : systemLocales) {
            try {
                int retCode = mTts.isLanguageAvailable(loc);
                if (retCode >= lowestCode) {
                    availableTtsLocales.add(loc);
                } else {
//                    Timber.v("ReadText.buildAvailableLanguages() :: %s  not available (error code %d)", loc.getCountry(), retCode);
                }
            } catch (IllegalArgumentException e) {
                Timber.e("Error checking if language " + loc.getDisplayName() + " available");
            }
        }
    }
//    public static void buildAvailableLanguages() {
//        availableTtsLocales.clear();
//        Locale[] systemLocales = Locale.getAvailableLocales();
//        for (Locale loc : systemLocales) {
//            try {
//                int retCode = mTts.isLanguageAvailable(loc);
//                if (retCode >= TextToSpeech.LANG_AVAILABLE) {
//                    availableTtsLocales.add(loc);
//                } else {
//                    Timber.v("ReadText.buildAvailableLanguages() :: %s  not available (error code %d)", loc.getCountry(), retCode);
//                }
//            } catch (IllegalArgumentException e) {
//                Timber.e("Error checking if language " + loc.getDisplayName() + " available");
//            }
//        }
//    }


    public static void releaseTts() {
        if (mTts != null) {
            mTts.stop();

            try{
                mTts.shutdown();
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }


    public static void stopTts() {
        if (mTts != null) {
            if (sTextQueue != null) {
                sTextQueue.clear();
            }
            mTts.stop();
        }
    }


    interface ReadTextListener {
        public void onDone();

        public void ttsInitialized();
    }


    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @Nullable
    public static String getTextToSpeak() {
        return mTextToSpeak;
    }
}
