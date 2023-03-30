/****************************************************************************************
 * Copyright (c) 2011 Kostas Spyropoulos <inigo.aldana@gmail.com>                       *
 * Copyright (c) 2014 Bruno Romero de Azevedo <brunodea@inf.ufsm.br>                    *
 * Copyright (c) 2014–15 Roland Sieker <ospalh@gmail.com>                               *
 * Copyright (c) 2015 Timothy Rae <perceptualchaos2@gmail.com>                          *
 * Copyright (c) 2016 Mark Carter <mark@marcardar.com>                                  *
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
// TODO: implement own menu? http://www.codeproject.com/Articles/173121/Android-Menus-My-Way

package com.ichi2.anki;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

import androidx.annotation.CheckResult;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.net.ConnectivityManagerCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.appcompat.app.ActionBar;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Pair;
import android.util.TypedValue;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.baidu.tts.chainofresponsibility.logger.LoggerProxy;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.SynthesizerTool;
import com.baidu.tts.client.TtsMode;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anim.ViewAnimation;
import com.ichi2.anki.cardviewer.MissingImageHandler;
import com.ichi2.anki.dialogs.TagsDialog;
import com.ichi2.anki.multimediacard.AudioView;
import com.ichi2.anki.cardviewer.CardAppearance;
import com.ichi2.anki.receiver.SdCardReceiver;
import com.ichi2.anki.reviewer.CardMarker;
import com.ichi2.anki.reviewer.CardMarker.FlagDef;
import com.ichi2.anki.reviewer.ReviewerCustomFonts;
import com.ichi2.anki.reviewer.ReviewerUi;
import com.ichi2.anki.cardviewer.TypedAnswer;
import com.ichi2.async.CollectionTask;
import com.ichi2.async.Connection;
import com.ichi2.async.TaskListener;
import com.ichi2.bd.Auth;
import com.ichi2.bd.FileSaveListener;
import com.ichi2.bd.IOfflineResourceConst;
import com.ichi2.bd.InitConfig;
import com.ichi2.bd.MySyntherizer;
import com.ichi2.bd.NonBlockSyntherizer;
import com.ichi2.bd.OfflineResource;
import com.ichi2.compat.CompatHelper;
import com.ichi2.libanki.DB;
import com.ichi2.libanki.Decks;
import com.ichi2.libanki.sched.AbstractSched;
import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Consts;
import com.ichi2.libanki.DeckConfig;
import com.ichi2.libanki.Note;
import com.ichi2.libanki.Sound;
import com.ichi2.libanki.Utils;
import com.ichi2.libanki.template.Template;
import com.ichi2.themes.HtmlColors;
import com.ichi2.themes.Themes;
import com.ichi2.ui.CustomStyleDialog;
import com.ichi2.utils.AdaptionUtil;
import com.ichi2.utils.DiffEngine;
import com.ichi2.utils.FileUtil;
import com.ichi2.utils.FunctionalInterfaces.Consumer;
import com.ichi2.utils.FunctionalInterfaces.Function;

import com.ichi2.utils.HtmlUtils;
import com.ichi2.utils.JSONArray;
import com.ichi2.utils.JSONException;
import com.ichi2.utils.JSONObject;
import com.ichi2.utils.OKHttpUtil;
import com.ichi2.utils.WebViewDebugging;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

import static com.ichi2.anki.DeckPicker.BE_VIP;
import static com.ichi2.anki.DeckPicker.REFRESH_LOGIN_STATE_AND_TURN_TO_VIP_HTML;
import static com.ichi2.anki.DeckPicker.REFRESH_VOICE_INFO;
import static com.ichi2.anki.SpeakSettingActivity.REQUEST_CODE_SPEAK_SETTING;
import static com.ichi2.anki.cardviewer.CardAppearance.calculateDynamicFontSize;
import static com.ichi2.anki.cardviewer.ViewerCommand.*;
import static com.ichi2.anki.reviewer.CardMarker.*;
import static com.ichi2.async.CollectionTask.TASK_TYPE.*;

import com.ichi2.async.TaskData;

import static com.ichi2.bd.MainHandlerConstant.PRINT;
import static com.ichi2.bd.MainHandlerConstant.UI_CHANGE_INPUT_TEXT_SELECTION;
import static com.ichi2.bd.MainHandlerConstant.UI_CHANGE_SYNTHES_TEXT_SELECTION;
import static com.ichi2.bd.MainHandlerConstant.UI_PLAY_END;
import static com.ichi2.bd.MainHandlerConstant.UI_PLAY_START;
import static com.ichi2.libanki.Consts.KEY_SELECT_ONLINE_SPEAK_ENGINE;
import static com.ichi2.libanki.Consts.KEY_SHOW_TTS_ICON;
import static com.ichi2.libanki.Sound.SoundSide;
import static com.ichi2.themes.Themes.THEME_NIGHT_BLACK;
import static com.ichi2.themes.Themes.THEME_NIGHT_DARK;

import com.github.zafarkhaja.semver.Version;
import com.jaygoo.widget.OnRangeChangedListener;
import com.jaygoo.widget.RangeSeekBar;


@SuppressWarnings( {"PMD.AvoidThrowingRawExceptionTypes", "PMD.FieldDeclarationsShouldBeAtStartOfClass"})
public abstract class AbstractFlashcardViewer extends AnkiActivity implements ReviewerUi, CommandProcessor {

    /**
     * Result codes that are returned when this activity finishes.
     */
    public static final int RESULT_DEFAULT = 50;
    public static final int RESULT_NO_MORE_CARDS = 52;

    /**
     * Available options performed by other activities.
     */
    public static final int EDIT_CURRENT_CARD = 0;
    public static final int DECK_OPTIONS = 1;
    public static final int REFRESH_TOP_BUTTONS = 5;
    public static final int REFRESH_GESTURE = 6;
    public static final int REFRESH_CONTROLLER = 7;
    public static final int EASE_1 = 1;
    public static final int EASE_2 = 2;
    public static final int EASE_3 = 3;
    public static final int EASE_4 = 4;

    /**
     * Maximum time in milliseconds to wait before accepting answer button presses.
     */
    @VisibleForTesting
    protected static final int DOUBLE_TAP_IGNORE_THRESHOLD = 200;

    /**
     * Time to wait in milliseconds before resuming fullscreen mode
     **/
    protected static final int INITIAL_HIDE_DELAY = 200;

    // Type answer patterns
    private static final Pattern sTypeAnsPat = Pattern.compile("\\[\\[type:(.+?)\\]\\]");

    /**
     * to be sent to and from the card editor
     */
    private static Card sEditorCard;

    protected static boolean sDisplayAnswer = false;

    private boolean mTtsInitialized = false;
    private boolean mReplayOnTtsInit = false;

    protected static final int MENU_DISABLED = 3;

    // js api developer contact
    private String mCardSuppliedDeveloperContact = "";
    private String mCardSuppliedApiVersion = "";

    private static final String sCurrentJsApiVersion = "0.0.1";
    private static final String sMinimumJsApiVersion = "0.0.1";

    // JS API ERROR CODE
    private static final int ankiJsErrorCodeDefault = 0;
    private static final int ankiJsErrorCodeMarkCard = 1;
    private static final int ankiJsErrorCodeFlagCard = 2;

    // JS api list enable/disable status
    private HashMap<String, Boolean> mJsApiListMap = new HashMap<String, Boolean>();

    private boolean isInFullscreen;

    /**
     * Broadcast that informs us when the sd card is about to be unmounted
     */
    private BroadcastReceiver mUnmountReceiver = null;

    /**
     * Variables to hold preferences
     */
    private CardAppearance mCardAppearance;
    private boolean mPrefShowTopbar;
    private boolean mShowTimer;
    protected boolean mPrefWhiteboard;
    private int mPrefFullscreenReview;
    private int mRelativeButtonSize;
    private boolean mDoubleScrolling;
    private boolean mScrollingButtons;
    private boolean mGesturesEnabled;
    // Android WebView
    protected boolean mSpeakText;
    protected boolean mDisableClipboard = false;

    protected boolean mOptUseGeneralTimerSettings;

    protected boolean mUseTimer;
    protected int mWaitAnswerSecond;
    protected int mWaitQuestionSecond;

    protected boolean mPrefUseTimer;

    protected boolean mOptUseTimer;
    protected int mOptWaitAnswerSecond;
    protected int mOptWaitQuestionSecond;

    protected boolean mUseInputTag;

    // Default short animation duration, provided by Android framework
    protected int mShortAnimDuration;

    // Preferences from the collection
    private boolean mShowNextReviewTime;

    // Answer card & cloze deletion variables
    private String mTypeCorrect = null;
    // The correct answer in the compare to field if answer should be given by learner. Null if no answer is expected.
    private String mTypeInput = "";  // What the learner actually typed
    private String mTypeFont = "";  // Font face of the compare to field
    private int mTypeSize = 0;  // Its font size
    private String mTypeWarning;

    private boolean mIsSelecting = false;
    private boolean mTouchStarted = false;
    private boolean mInAnswer = false;
    private boolean mAnswerSoundsAdded = false;

    private String mCardTemplate;

    /**
     * Variables to hold layout objects that we need to update or handle events for
     */
    private View mLookUpIcon;
    private WebView mCardWebView;
    private FrameLayout mCardFrame;
    private FrameLayout mTouchLayer;
    private TextView mChosenAnswer;
    protected TextView mNext1;
    protected TextView mNext2;
    protected TextView mNext3;
    protected TextView mNext4;
    protected EditText mAnswerField;
    protected TextView mEase1;
    protected TextView mEase2;
    protected TextView mEase3;
    protected TextView mEase4;

    protected LinearLayout mFlipCardLayout;
    protected LinearLayout mEaseButtonsLayout;
    protected LinearLayout mEase1Layout;
    protected LinearLayout mEase2Layout;
    protected LinearLayout mEase3Layout;
    protected LinearLayout mEase4Layout;
    protected FrameLayout mPreviewButtonsLayout;


    protected ImageView mPreviewPrevCard;
    protected ImageView mPreviewNextCard;
    protected ImageButton mRemark;
    protected TextView mPreviewToggleAnswerText;
    protected RelativeLayout mTopBarLayout;
    private Chronometer mCardTimer;
    protected Whiteboard mWhiteboard;
    @SuppressWarnings("deprecation") // Tracked separately as #5023 on github
    private android.text.ClipboardManager mClipboard;

    protected Card mCurrentCard;
    protected Card mPreCard;
    protected LinkedList<Long> mCardIDs = new LinkedList<>();
    private int mCurrentEase;

    private boolean mButtonHeightSet = false;

    private boolean mConfigurationChanged = false;
    private int mShowChosenAnswerLength = 2000;

    /**
     * A record of the last time the "show answer" or ease buttons were pressed. We keep track
     * of this time to ignore accidental button presses.
     */
    @VisibleForTesting
    protected long mLastClickTime;

    /**
     * Swipe Detection
     */
    private GestureDetectorCompat gestureDetector;
    private MyGestureDetector mGestureDetectorImpl;
    private boolean mLinkOverridesTouchGesture;

    private boolean mIsXScrolling = false;
    private boolean mIsYScrolling = false;

    /**
     * Gesture Allocation
     */
    private int mGestureSwipeUp;
    private int mGestureSwipeDown;
    private int mGestureSwipeLeft;
    private int mGestureSwipeRight;
    private int mGestureDoubleTap;
    private int mGestureTapLeft;
    private int mGestureTapRight;
    private int mGestureTapTop;
    private int mGestureTapBottom;
    private int mGestureLongclick;
    private int mGestureVolumeUp;
    private int mGestureVolumeDown;

    /**
     * Controller Allocation
     */
    protected int mControllerA;
    protected int mControllerB;
    protected int mControllerX;
    protected int mControllerY;
    protected int mControllerUp;
    protected int mControllerDown;
    protected int mControllerLeft;
    protected int mControllerRight;
    protected int mControllerLT;
    protected int mControllerRT;
    protected int mControllerLB;
    protected int mControllerRB;
    protected int mControllerMenu;
    protected int mControllerOption;
    protected int mControllerLeftPad;
    protected int mControllerRightPad;


    private String mCardContent;
    private String mBaseUrl;

    private int mFadeDuration = 300;

    protected AbstractSched mSched;

    protected Sound mSoundPlayer = new Sound();

    /**
     * Time taken o play all medias in mSoundPlayer
     */
    private long mUseTimerDynamicMS;

    /**
     * File of the temporary mic record
     **/
    protected AudioView mMicToolBar;
    protected String mTempAudioPath;

    /**
     * Last card that the WebView Renderer crashed on.
     * If we get 2 crashes on the same card, then we likely have an infinite loop and want to exit gracefully.
     */
    @Nullable
    private Long lastCrashingCardId = null;

    /**
     * Reference to the parent of the cardFrame to allow regeneration of the cardFrame in case of crash
     */
    private ViewGroup mCardFrameParent;

    /**
     * Lock to allow thread-safe regeneration of mCard
     */
    private ReadWriteLock mCardLock = new ReentrantReadWriteLock();

    /**
     * whether controls are currently blocked, and how long we expect them to be
     */
    private ReviewerUi.ControlBlock mControlBlocked = ControlBlock.SLOW;

    /**
     * Handle Mark/Flag state of cards
     */
    private CardMarker mCardMarker;

    /**
     * Handle providing help for "Image Not Found"
     */
    private static MissingImageHandler mMissingImageHandler = new MissingImageHandler();

    // ----------------------------------------------------------------------------
    // LISTENERS
    // ----------------------------------------------------------------------------

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            mSoundPlayer.stopSounds();
            mSoundPlayer.playSound((String) msg.obj, null);
        }
    };

    private final Handler longClickHandler = new Handler();
    private final Runnable longClickTestRunnable = new Runnable() {
        @Override
        public void run() {
            Timber.i("AbstractFlashcardViewer:: onEmulatedLongClick");
            // Show hint about lookup function if dictionary available
            if (!mDisableClipboard && Lookup.isAvailable()) {
                String lookupHint = getResources().getString(R.string.lookup_hint);
                UIUtils.showThemedToast(AbstractFlashcardViewer.this, lookupHint, false);
            }
            CompatHelper.getCompat().vibrate(AnkiDroidApp.getInstance().getApplicationContext(), 50);
            longClickHandler.postDelayed(startLongClickAction, 300);
        }
    };
    private final Runnable startLongClickAction = new Runnable() {
        @Override
        public void run() {
            executeCommand(mGestureLongclick);
        }
    };

    RelativeLayout.LayoutParams remarkLayoutParams;
    int remarkOriginBottomMargin;
    Dialog mRemarkDialog;
    EditText mEditContent;
    TextView mRemarkCount;


    private void showRemarkDialog() {
        Cursor cur = null;

        Map<String, Object> remark = new HashMap<>();
        Timber.i("search remarks cid  = " + mCurrentCard.getId());
        try {
            cur = CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance()).getDb()
                    .getDatabase()
                    .query(
                            "SELECT id,cid,content,mod FROM remarks WHERE cid = " + mCurrentCard.getId(), null);
            if (cur.moveToNext()) {
                remark.put("id", cur.getLong(0));
                remark.put("cid", cur.getLong(1));
                remark.put("content", cur.getString(2));
                remark.put("mod", cur.getLong(3));
            }
        } finally {
            if (cur != null && !cur.isClosed()) {
                cur.close();
            }
        }

        if (mRemarkDialog == null) {
            mRemarkDialog = new Dialog(this, R.style.DialogTheme2);

            //2、设置布局
            View view = View.inflate(this, R.layout.dialog_remark, null);
            mRemarkDialog.setContentView(view);
            mRemarkDialog.setOnDismissListener(dialogInterface -> {

            });
            Window window = mRemarkDialog.getWindow();
//            mRemarkDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            //设置弹出位置
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//            WindowManager.LayoutParams lps = window.getAttributes();
//            lps.verticalMargin = 0.1f;
//            window.setAttributes(lps);
            //设置弹出动画
//        window.setWindowAnimations(R.style.main_menu_animStyle);
//            //设置对话框大小
//            final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mEditContent = mRemarkDialog.findViewById(R.id.remark_content);
            mRemarkCount = mRemarkDialog.findViewById(R.id.txt_count);
        }
        mRemarkDialog.findViewById(R.id.remark_confirm).setOnClickListener(view1 -> {
            mRemarkDialog.dismiss();
            Timber.i("Adding Remark");
            DB db = CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance()).getDb();
            db.getDatabase().beginTransaction();
            try {
                if (remark.isEmpty()) {
                    remark.put("id", CollectionHelper.getInstance().getTimeSafe(AnkiDroidApp.getInstance()).intTimeMS());
                    remark.put("cid", mCurrentCard.getId());
                }
                remark.put("mod", CollectionHelper.getInstance().getTimeSafe(AnkiDroidApp.getInstance()).intTime());
                remark.put("content", mEditContent.getText().toString());
                Timber.i("insert or replace into remarks values:%s,%s,%s,%s", remark.get("id"), remark.get("cid"), remark.get("content"), remark.get("mod"));
                db.execute("insert or replace into remarks values (?,?,?,?)",
                        remark.get("id"), remark.get("cid"), remark.get("content"), remark.get("mod"));
                db.getDatabase().setTransactionSuccessful();
                CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance()).save();
                Toast.makeText(AnkiDroidApp.getInstance(), "助记保存成功", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(AnkiDroidApp.getInstance(), "助记保存失败,请重试", Toast.LENGTH_SHORT).show();
            } finally {
                db.getDatabase().endTransaction();
            }

        });
        mEditContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }


            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int count) {

            }


            @Override
            public void afterTextChanged(Editable editable) {
                mRemarkCount.setText(mEditContent.getText().toString().length() + "/300");
            }
        });
        mEditContent.setText((!remark.isEmpty() && remark.get("content") != null) ? (String) remark.get("content") : "");
//        mRemarkCount.setText(mEditContent+"/300");
        if (mRemarkDialog.isShowing()) {
            mRemarkDialog.dismiss();
            return;
        }
        mRemarkDialog.show();
    }


    // Handler for the "show answer" button
    private View.OnClickListener mFlipCardListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Timber.i("AbstractFlashcardViewer:: Show answer button pressed");
            // Ignore what is most likely an accidental double-tap.
            if (SystemClock.elapsedRealtime() - mLastClickTime < DOUBLE_TAP_IGNORE_THRESHOLD) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();
            mTimeoutHandler.removeCallbacks(mShowAnswerTask);
            displayCardAnswer();
        }
    };

    private View.OnClickListener mSelectEaseHandler = new View.OnClickListener() {
        @SuppressLint("NonConstantResourceId")
        @Override
        public void onClick(View view) {
            // Ignore what is most likely an accidental double-tap.
            if (SystemClock.elapsedRealtime() - mLastClickTime < DOUBLE_TAP_IGNORE_THRESHOLD) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();
            mTimeoutHandler.removeCallbacks(mShowQuestionTask);
            switch (view.getId()) {
                case R.id.flashcard_layout_ease1:
                    Timber.i("AbstractFlashcardViewer:: EASE_1 pressed");
                    answerCard(Consts.BUTTON_ONE);
                    break;
                case R.id.flashcard_layout_ease2:
                    Timber.i("AbstractFlashcardViewer:: EASE_2 pressed");
                    answerCard(Consts.BUTTON_TWO);
                    break;
                case R.id.flashcard_layout_ease3:
                    Timber.i("AbstractFlashcardViewer:: EASE_3 pressed");
                    answerCard(Consts.BUTTON_THREE);
                    break;
                case R.id.flashcard_layout_ease4:
                    Timber.i("AbstractFlashcardViewer:: EASE_4 pressed");
                    answerCard(Consts.BUTTON_FOUR);
                    break;
                default:
                    mCurrentEase = 0;
                    break;
            }
        }
    };

    private View.OnTouchListener mGestureListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (gestureDetector.onTouchEvent(event)) {
                return true;
            }
            if (!mDisableClipboard) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mTouchStarted = true;
                        longClickHandler.postDelayed(longClickTestRunnable, 800);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_MOVE:
                        if (mTouchStarted) {
                            longClickHandler.removeCallbacks(longClickTestRunnable);
                            mTouchStarted = false;
                        }
                        break;
                    default:
                        longClickHandler.removeCallbacks(longClickTestRunnable);
                        mTouchStarted = false;
                        break;
                }
            }

            if (!mGestureDetectorImpl.eventCanBeSentToWebView(event)) {
                return false;
            }
            //Gesture listener is added before mCard is set
            processCardAction(cardWebView -> {
                if (cardWebView == null) {
                    return;
                }
                cardWebView.dispatchTouchEvent(event);
            });
            return false;
        }
    };
    private Dialog mLayoutConfigDialog;
    ImageView alignLeft;
    ImageView alignRight;
    ImageView alignNormal;
    ImageView alignCenter;
    ImageView layerTop;
    ImageView layerCenter;
    ImageView layerBottom;
    ImageView bgColorWhite;
    ImageView bgColorGrey;
    ImageView bgColorBrown;
    ImageView bgColorGreen;
    ImageView bgColorBlack;
    RangeSeekBar fontSizeSeekBar;
    List<ImageView> aligns = new ArrayList<>();
    List<ImageView> layers = new ArrayList<>();
    List<ImageView> bgColors = new ArrayList<>();
    JSONObject needUploadViewSetting = null;
    private String mCurrentCSS = "";
    private String mCurrentEditingDefaultCSS = "";
    private long mCurrentCSSModelID = -1;


    protected void showLayoutDialog() {
        SharedPreferences preference = AnkiDroidApp.getSharedPrefs(this);
        if (mLayoutConfigDialog == null) {
            mLayoutConfigDialog = new Dialog(this, R.style.DialogTheme2);
            View view = View.inflate(this, R.layout.dialog_layout_config, null);
            mLayoutConfigDialog.setContentView(view);
            Window window = mLayoutConfigDialog.getWindow();
            mLayoutConfigDialog.setOnDismissListener(dialog -> {
                if (mCurrentCSS.equals(mCurrentEditingDefaultCSS)) {
                    //没修改过，直接返回
                    return;
                }
                JSONObject object = convertCss2JsonAndSave();//将当前css转化为类,添加修改时间
                if (needUploadViewSetting == null) {
                    needUploadViewSetting = new JSONObject();
                }
                needUploadViewSetting.put(String.valueOf(mCurrentCard.model().getLong("id")), object);
                Timber.i("save layout config :%s", needUploadViewSetting.toString());
                preference.edit().putString(Consts.KEY_LOCAL_LAYOUT_CONFIG, needUploadViewSetting.toString().replace("\\", "")).apply();
                getAccount().getToken(AbstractFlashcardViewer.this, new MyAccount.TokenCallback() {
                    @Override
                    public void onSuccess(String token) {
                        RequestBody formBody = new FormBody.Builder()
                                .add("models_view_settings", needUploadViewSetting.toString())
                                .build();

                        OKHttpUtil.post(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "users/conf", formBody, token, "", new OKHttpUtil.MyCallBack() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                e.printStackTrace();
                            }


                            @Override
                            public void onResponse(Call call, String token, Object arg1, Response response) {
                                if (response.isSuccessful()) {
                                    Timber.i("post view settings successfully!:%s", response.body());

                                } else {
                                    Timber.e("post view settings failed, error code %d", response.code());
                                }
                            }
                        });
                    }


                    @Override
                    public void onFail(String message) {

                    }
                });
            });
//            mRemarkDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            //设置弹出位置
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            view.findViewById(R.id.close).setOnClickListener(v -> mLayoutConfigDialog.dismiss());
            view.findViewById(R.id.reset).setOnClickListener(v -> {
                //将当前样式变回css()样式
//                mCurrentCSS = mCurrentCard.css().replace("<style>", "").replace("</style>", "");
                mCurrentCSS = ".card {}";
//                mCurrentCSS = mCurrentEditingDefaultCSS;
//                Timber.i("show updateTypeAnswerInfo");
//                JSONObject temp = convertCss2JsonAndSave();
                fontSizeSeekBar.setProgress(0);
                for (View view1 : layers) {//存在无一被选中的情况，通过预先全设置为非选中实现
                    view1.setSelected(false);
                }
                for (View view1 : bgColors) {
                    view1.setSelected(false);
                }
                for (View view1 : aligns) {
                    view1.setSelected(false);
                }

                String localViewSettingStr = preference.getString(Consts.KEY_LOCAL_LAYOUT_CONFIG, "");
                mCurrentCSSModelID = mCurrentCard.model().getLong("id");
                if (localViewSettingStr != null && !localViewSettingStr.isEmpty()) {
                    needUploadViewSetting = new JSONObject(localViewSettingStr);
                    if (needUploadViewSetting.remove(String.valueOf(mCurrentCSSModelID)) != null) {
                        Timber.i("remove saved setting :%s", mCurrentCSSModelID);
                        preference.edit().putString(Consts.KEY_LOCAL_LAYOUT_CONFIG, needUploadViewSetting.toString().replace("\\", "")).apply();
                    }
                }
                if (!mCacheContent.isEmpty()) {
                    updateCard(mCacheContent);
                }
            });

            alignLeft = view.findViewById(R.id.ic_align_left);
            alignRight = view.findViewById(R.id.ic_align_right);
            alignNormal = view.findViewById(R.id.ic_align_normal);
            alignCenter = view.findViewById(R.id.ic_align_center);


            aligns.add(alignLeft);
            aligns.add(alignRight);
            aligns.add(alignNormal);
            aligns.add(alignCenter);


            layerTop = view.findViewById(R.id.ic_layer_top);
            layerCenter = view.findViewById(R.id.ic_layer_center);
            layerBottom = view.findViewById(R.id.ic_layer_bottom);


            layers.add(layerTop);
            layers.add(layerCenter);
            layers.add(layerBottom);

            bgColorWhite = view.findViewById(R.id.ic_bg_white);
            bgColorGrey = view.findViewById(R.id.ic_bg_grey);
            bgColorBrown = view.findViewById(R.id.ic_bg_brown);
            bgColorGreen = view.findViewById(R.id.ic_bg_green);
            bgColorBlack = view.findViewById(R.id.ic_bg_black);


            bgColors.add(bgColorWhite);
            bgColors.add(bgColorGrey);
            bgColors.add(bgColorBrown);
            bgColors.add(bgColorGreen);
            bgColors.add(bgColorBlack);


            alignLeft.setOnClickListener(v -> onLayoutConfigAlignClick("left", alignLeft));
            alignRight.setOnClickListener(v -> onLayoutConfigAlignClick("right", alignRight));
            alignNormal.setOnClickListener(v -> onLayoutConfigAlignClick("justify", alignNormal));
            alignCenter.setOnClickListener(v -> onLayoutConfigAlignClick("center", alignCenter));

            layerTop.setOnClickListener(v -> onLayoutConfigLayerClick("flex-start", layerTop));
            layerCenter.setOnClickListener(v -> onLayoutConfigLayerClick("center", layerCenter));
            layerBottom.setOnClickListener(v -> onLayoutConfigLayerClick("flex-end", layerBottom));

            bgColorWhite.setOnClickListener(v -> onLayoutConfigBgClick("white", bgColorWhite));
            bgColorGrey.setOnClickListener(v -> onLayoutConfigBgClick("#F4F4F6", bgColorGrey));
            bgColorBrown.setOnClickListener(v -> onLayoutConfigBgClick("#E4DCCE", bgColorBrown));
            bgColorGreen.setOnClickListener(v -> onLayoutConfigBgClick("#BADEBE", bgColorGreen));
            bgColorBlack.setOnClickListener(v -> onLayoutConfigBgClick("black", bgColorBlack));


            fontSizeSeekBar = view.findViewById(R.id.font_size_seek_bar);
            fontSizeSeekBar.setRange(0, 80);
            fontSizeSeekBar.setOnRangeChangedListener(new OnRangeChangedListener() {
                @Override
                public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                    if (!isFromUser) {
                        return;
                    }
                    leftValue = Math.max(leftValue,12);
                    Matcher fontSizeMatcher = Pattern.compile("font-size:(.+?)px").matcher(mCurrentCSS);
                    if (fontSizeMatcher.find()) {
                        String fld1 = fontSizeMatcher.group(0);

                        mCurrentCSS = mCurrentCSS.replace(fld1, String.format(Locale.CHINA, "font-size: %.0fpx", leftValue));

                    } else {
                        mCurrentCSS = mCurrentCSS.replace("}", String.format(Locale.CHINA, "font-size: %.0fpx;}", leftValue));
                    }
                    Timber.i("set font size fld:%s", leftValue);//字体大小
                    if (!mCacheContent.isEmpty()) {
                        updateCard(mCacheContent);
                    }
//                    needUploadViewSetting.put(String.valueOf(mCurrentCard.model().getLong("id")), mCurrentCSS.replace(".card", ""));

                }


                @Override
                public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {

                }


                @Override
                public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {

                }
            });
        }
        if (mLayoutConfigDialog.isShowing()) {
            mLayoutConfigDialog.dismiss();
            return;
        }
        if (mCurrentCSS.isEmpty() || mCurrentCSSModelID != mCurrentCard.model().getLong("id")) {
            //获取preferences是否有保存的布局设置
            String localViewSettingStr = preference.getString(Consts.KEY_LOCAL_LAYOUT_CONFIG, "");
            boolean notSync = false;
            mCurrentCSSModelID = mCurrentCard.model().getLong("id");
            if (localViewSettingStr != null && !localViewSettingStr.isEmpty()) {
                Timber.i("find local view setting:%s", localViewSettingStr);
                needUploadViewSetting = new JSONObject(localViewSettingStr);
                try {
                    JSONObject currentModelSetting = needUploadViewSetting.getJSONObject(String.valueOf(mCurrentCSSModelID));
                    if (currentModelSetting != null) {
                        mCurrentCSS = convertJson2Css(currentModelSetting, true);

                    } else {
                        notSync = true;
                        //没这个记录，新建一个默认模板
                        mCurrentCSS = ".card {}";

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    notSync = true;
                    mCurrentCSS = ".card {}";
//                    needUploadViewSetting.put()
                }

            } else {
//                needUploadViewSetting = new JSONObject();
                mCurrentCSS = ".card {}";
            }
        } else {
            //将dialog改为原设布局
            convertJson2Css(convertCss2JsonAndSave(), true);
        }
        mCurrentEditingDefaultCSS = mCurrentCSS;//保存刚载入时的css，留着备用
//        if (localViewSettingStr == null || localViewSettingStr.isEmpty() || notSync) {
//            //本地没数据或当前model未保存，则从数据库model里的css里找 //不找了。
//            mCurrentCSS = mCurrentCard.css().replace("<style>", "").replace("</style>", "");
////            Timber.i("show updateTypeAnswerInfo");
//            JSONObject temp = convertCss2JsonAndSave();
//            fontSizeSeekBar.setProgress(Float.parseFloat(temp.getString("font_size")) - 8);
//            updateLayoutConfigDialog(TYPE_ALIGN, temp.getString("text_align"));
//            updateLayoutConfigDialog(TYPE_LAYER, temp.getString("layout"));
//            updateLayoutConfigDialog(TYPE_BG, temp.getString("background_color"));
//            if (needUploadViewSetting == null) {
//                needUploadViewSetting = new JSONObject();
//            }
//            if (notSync) {
//                needUploadViewSetting.put(String.valueOf(mCurrentCSSModelID), temp.toString());
//            }
//        }

        mLayoutConfigDialog.show();
    }


    private String convertJson2Css(JSONObject currentModelSetting, boolean updateUI) {
        String fontSize = "";
        String layer = "";
        String align = "";
        String bg = "";
        String content = "";
        try {
            fontSize = currentModelSetting.getString("font_size");
            if (!fontSize.isEmpty() && Float.parseFloat(fontSize) != 0) {
                content += String.format("font-size: %spx;\n", fontSize);
            }
            if (updateUI) {
                fontSizeSeekBar.setProgress(Float.parseFloat(fontSize) - 8);
            }
        } catch (Exception ignored) {
            if (updateUI) {
                fontSizeSeekBar.setProgress(0);
            }
        }
        try {
            layer = currentModelSetting.getString("layout");
            if (!layer.isEmpty()) {
                content += String.format("align-items: %s;\ndisplay: %s;\nheight: %s;\n", layer, "flex", "100vh");
            }
            if (updateUI) {
                updateLayoutConfigDialog(TYPE_LAYER, layer);
            }
        } catch (Exception ignored) {
            if (updateUI) {
                updateLayoutConfigDialog(TYPE_LAYER, "");
            }
        }
        try {
            align = currentModelSetting.getString("text_align");
            if (!align.isEmpty()) {
                content += String.format("text-align: %s;\n", align);
            }
            if (updateUI) {
                updateLayoutConfigDialog(TYPE_ALIGN, align);
            }
        } catch (Exception ignored) {
            if (updateUI) {
                updateLayoutConfigDialog(TYPE_ALIGN, "");
            }
        }
        try {
            bg = currentModelSetting.getString("background_color");
            if (!bg.isEmpty()) {
                content += String.format("background-color: %s;\n", bg);
                if (bg.equals("#000000") || bg.equals("black")) {
                    content += "filter: invert(1);\n";
                }
            }
            if (updateUI) {
                updateLayoutConfigDialog(TYPE_BG, bg);
            }
        } catch (Exception ignored) {
            if (updateUI) {
                updateLayoutConfigDialog(TYPE_BG, "");
            }
        }


        return /*content.isEmpty() ? ".card {}" : */String.format(".card {%s}", content);

    }


    private JSONObject convertCss2JsonAndSave() {
        String fontSize = "", align = "", layer = "", bg = "";
        JSONObject temp = new JSONObject();
        Matcher fontSizeMatcher = Pattern.compile("font-size:(.+?)px").matcher(mCurrentCSS);
        if (fontSizeMatcher.find()) {
            fontSize = fontSizeMatcher.group(1).trim();
            Timber.i("saved font size:%s", fontSize);

        }
        Matcher alignMatcher = Pattern.compile("text-align:(.+?);").matcher(mCurrentCSS);
        if (alignMatcher.find()) {
            align = alignMatcher.group(1).trim();
            Timber.i("saved text align :%s", align);

        }
        Matcher layerMatcher = Pattern.compile("align-items:(.+?);").matcher(mCurrentCSS);
        if (layerMatcher.find()) {
            layer = layerMatcher.group(1).trim();
            Timber.i("saved align items:%s", layer);

        }
        Matcher bgMatcher = Pattern.compile("background-color:(.+?);").matcher(mCurrentCSS);
        if (bgMatcher.find()) {
            bg = bgMatcher.group(1).trim();
            Timber.i("saved bg:%s", bg);
        }
        temp.put("font_size", fontSize.isEmpty() ? "" : String.valueOf(Math.max(12, Math.round(Double.parseDouble(fontSize)))));
        temp.put("text_align", align);
        temp.put("layout", layer);
        temp.put("background_color", bg);
        temp.put("updated_at", String.valueOf(System.currentTimeMillis() / 1000));
        return temp;
    }


    private static final int TYPE_LAYER = 0;
    private static final int TYPE_ALIGN = 1;
    private static final int TYPE_BG = 2;


    private void updateLayoutConfigDialog(int type, String value) {
        if (type == TYPE_LAYER) {
            switch (value) {
                case "flex-start":
                case "top":
                    updateViewsSelected(layerTop, layers);
                    break;
                case "flex-end":
                case "bottom":
                    updateViewsSelected(layerBottom, layers);
                    break;
                case "center":
                    updateViewsSelected(layerCenter, layers);
                    break;
                case "":
                    updateViewsSelected(null, layers);
                    break;
            }
        } else if (type == TYPE_ALIGN) {
            switch (value) {
                case "left":
                    updateViewsSelected(alignLeft, aligns);
                    break;
                case "right":
                    updateViewsSelected(alignRight, aligns);
                    break;
                case "center":
                    updateViewsSelected(alignCenter, aligns);
                    break;
                case "justify":
                    updateViewsSelected(alignNormal, aligns);
                    break;
                case "":
                    updateViewsSelected(null, aligns);
                    break;
            }
        } else if (type == TYPE_BG) {
            switch (value) {
                case "#F4F4F6":
                    updateViewsSelected(bgColorGrey, bgColors);
                    break;
                case "#E4DCCE":
                    updateViewsSelected(bgColorBrown, bgColors);
                    break;
                case "#BADEBE":
                    updateViewsSelected(bgColorGreen, bgColors);
                    break;
                case "#000000":
                case "black":
                    updateViewsSelected(bgColorBlack, bgColors);
                    break;
                case "white":
                case "#FFFFFF":
//                default:
                    updateViewsSelected(bgColorWhite, bgColors);
                    break;
                case "":
                    updateViewsSelected(null, bgColors);
                    break;
            }
        }
    }


    private void updateViewsSelected(ImageView view, List<ImageView> views) {
        for (ImageView imageView : views) {
            imageView.setSelected(view == imageView);
        }
    }


    private void onLayoutConfigBgClick(String value, ImageView clickView) {
        updateViewsSelected(clickView, bgColors);
        Matcher bgMatcher = Pattern.compile("background-color:(.+?);").matcher(mCurrentCSS);
        if (bgMatcher.find()) {
            String str = bgMatcher.group(0);
            Timber.i("show bg str:%s", str);
            mCurrentCSS = mCurrentCSS.replace(str, "background-color: " + value + ";");
        } else {
            mCurrentCSS = mCurrentCSS.replace("}", "background-color: " + value + ";}");
        }

        Matcher filter = Pattern.compile("filter: invert(.+?);").matcher(mCurrentCSS);
        if (filter.find()) {
            String str = filter.group(0);
            Timber.i("show filter str:%s", str);
            if (!value.equalsIgnoreCase("black") && !value.equals("#000000")) {
                //不是黑色要把反转去掉
                mCurrentCSS = mCurrentCSS.replace(str, "");
            }
        } else {
            if (value.equalsIgnoreCase("black") || value.equals("#000000")) {
                //黑色需要添加反转
                mCurrentCSS = mCurrentCSS.replace("}", "filter: invert(1);}");
            }
        }
        if (!mCacheContent.isEmpty()) {
            updateCard(mCacheContent);
        }
//        needUploadViewSetting.put(String.valueOf(mCurrentCard.model().getLong("id")), mCurrentCSS.replace(".card", ""));
    }


    private void onLayoutConfigLayerClick(String value, ImageView clickView) {
        updateViewsSelected(clickView, layers);
        Matcher layerMatcher = Pattern.compile("align-items:(.+?);").matcher(mCurrentCSS);
        if (layerMatcher.find()) {
            String str = layerMatcher.group(0);
            Timber.i("show layer str:%s", str);
            mCurrentCSS = mCurrentCSS.replace(str, "align-items: " + value + ";");

        } else {
            mCurrentCSS = mCurrentCSS.replace("}", "align-items: " + value + ";}");
        }
        Matcher layerMatcher2 = Pattern.compile("display:(.+?);").matcher(mCurrentCSS);
        if (!layerMatcher2.find()) {
            mCurrentCSS = mCurrentCSS.replace("}", "display: flex;}");
        }
        Matcher layerMatcher3 = Pattern.compile("height:(.+?);").matcher(mCurrentCSS);
        if (!layerMatcher3.find()) {
            mCurrentCSS = mCurrentCSS.replace("}", "height: 100vh;}");
        }
        if (!mCacheContent.isEmpty()) {
            updateCard(mCacheContent);
        }
//        needUploadViewSetting.put(String.valueOf(mCurrentCard.model().getLong("id")), mCurrentCSS.replace(".card", ""));

    }


    private void onLayoutConfigAlignClick(String value, ImageView clickView) {
        updateViewsSelected(clickView, aligns);
        Matcher alignMatcher = Pattern.compile("text-align:(.+?);").matcher(mCurrentCSS);
        if (alignMatcher.find()) {
            String str = alignMatcher.group(0);
            Timber.i("show text align str:%s", str);
            mCurrentCSS = mCurrentCSS.replace(str, "text-align: " + value + ";");
        } else {
            mCurrentCSS = mCurrentCSS.replace("}", "text-align: " + value + ";}");
        }
        if (!mCacheContent.isEmpty()) {
            updateCard(mCacheContent);
        }
//        needUploadViewSetting.put(String.valueOf(mCurrentCard.model().getLong("id")), mCurrentCSS.replace(".card", ""));

    }


    @SuppressLint("CheckResult")
        //This is intentionally package-private as it removes the need for synthetic accessors
    void processCardAction(Consumer<WebView> cardConsumer) {
        processCardFunction(cardWebView -> {
            cardConsumer.consume(cardWebView);
            return true;
        });
    }


    @CheckResult
    private <T> T processCardFunction(Function<WebView, T> cardFunction) {
        Lock readLock = mCardLock.readLock();
        try {
            readLock.lock();
            return cardFunction.apply(mCardWebView);
        } finally {
            readLock.unlock();
        }
    }


    protected final TaskListener mDismissCardHandler = new NextCardHandler() { /* superclass is sufficient */
    };


    private final TaskListener mUpdateCardHandler = new TaskListener() {
        private boolean mNoMoreCards;


        @Override
        public void onPreExecute() {
            showProgressBar();
        }


        @Override
        public void onProgressUpdate(TaskData value) {
            boolean cardChanged = false;
            if (mCurrentCard != value.getCard()) {
                /*
                 * Before updating mCurrentCard, we check whether it is changing or not. If the current card changes,
                 * then we need to display it as a new card, without showing the answer.
                 */
                sDisplayAnswer = false;
                cardChanged = true;  // Keep track of that so we can run a bit of new-card code
            }
            mCurrentCard = value.getCard();
            CollectionTask.launchCollectionTask(PRELOAD_NEXT_CARD); // Tasks should always be launched from GUI. So in
            // listener and not in background
            if (mCurrentCard == null) {
                // If the card is null means that there are no more cards scheduled for review.
                mNoMoreCards = true;
                showProgressBar();
                return;
            }
            if (mPrefWhiteboard && mWhiteboard != null) {
                mWhiteboard.clear();
            }

            if (sDisplayAnswer) {
                mSoundPlayer.resetSounds(); // load sounds from scratch, to expose any edit changes
                mAnswerSoundsAdded = false; // causes answer sounds to be reloaded
                generateQuestionSoundList(); // questions must be intentionally regenerated
                displayCardAnswer();
            } else {
                if (cardChanged) {
                    updateTypeAnswerInfo();
                }
                displayCardQuestion();
                mCurrentCard.startTimer();
                initTimer();
            }
            hideProgressBar();
        }


        @Override
        public void onPostExecute(TaskData result) {
            if (!result.getBoolean()) {
                // RuntimeException occurred on update cards
                closeReviewer(DeckPicker.RESULT_DB_ERROR, false);
                return;
            }
            if (mNoMoreCards) {
                closeReviewer(RESULT_NO_MORE_CARDS, true);
            }
        }
    };


    protected boolean hasSched() {
        return true;
    }


    protected void displayPre() {
        if (mCardIDs.size() > 0) {
            mCurrentCard = getCol().getCard(mCardIDs.pop());
            // Start reviewing next card
            updateTypeAnswerInfo();
            hideProgressBar();
            unblockControls();
            displayCardQuestion();
            refreshActionBar();
            findViewById(R.id.root_layout).requestFocus();
        }
    }


    abstract class NextCardHandler extends TaskListener {
        private boolean mNoMoreCards;


        @Override
        public void onPreExecute() {
            dealWithTimeBox();
        }


        @Override
        public void onProgressUpdate(TaskData value) {
            displayNext(value.getCard());
        }


        protected void displayNext(Card nextCard) {

            Timber.i("display next:" + hasSched() + "," + (mSched == null));
            if (hasSched() && mSched == null) {
                // TODO: proper testing for restored activity
                finishWithoutAnimation();
                return;
            }
            if (mCurrentCard != null) {
                mCardIDs.push(mCurrentCard.getId());
            }
            mCurrentCard = nextCard;
            if (mCurrentCard == null) {
                // If the card is null means that there are no more cards scheduled for review.
                mNoMoreCards = true; // other handlers use this, toggle state every time through
            } else {
                mNoMoreCards = false; // other handlers use this, toggle state every time through
                // Start reviewing next card
                updateTypeAnswerInfo();
                hideProgressBar();
                AbstractFlashcardViewer.this.unblockControls();
                AbstractFlashcardViewer.this.displayCardQuestion();
            }
        }


        private void dealWithTimeBox() {
            Resources res = getResources();
            Pair<Integer, Integer> elapsed = getCol().timeboxReached();
            if (elapsed != null) {
                int nCards = elapsed.second.intValue();
                int nMins = elapsed.first.intValue() / 60;
                String mins = res.getQuantityString(R.plurals.in_minutes, nMins, nMins);
                String timeboxMessage = res.getQuantityString(R.plurals.timebox_reached, nCards, nCards, mins);
                UIUtils.showThemedToast(AbstractFlashcardViewer.this, timeboxMessage, true);
                getCol().startTimebox();
            }
        }


        @Override
        public void onPostExecute(TaskData result) {
            postNextCardDisplay(result.getBoolean());
        }


        protected void postNextCardDisplay(boolean displaySuccess) {
            if (!displaySuccess) {
                // RuntimeException occurred on answering cards
                closeReviewer(DeckPicker.RESULT_DB_ERROR, false);
                return;
            }
            // Check for no more cards before session complete. If they are both true, no more cards will take
            // precedence when returning to study options.
            if (mNoMoreCards) {
                closeReviewer(RESULT_NO_MORE_CARDS, true);
            }
            // set the correct mark/unmark icon on action bar
            refreshActionBar();
            findViewById(R.id.root_layout).requestFocus();
        }
    }


    protected TaskListener mAnswerCardHandler(boolean quick) {
        return new NextCardHandler() {
            @Override
            public void onPreExecute() {
                super.onPreExecute();
                blockControls(quick);
            }
        };
    }


    /**
     * Extract type answer/cloze text and font/size
     */
    private void updateTypeAnswerInfo() {
        mTypeCorrect = null;
        mTypeInput = "";
        String q = mCurrentCard.q(false);
        Matcher m = sTypeAnsPat.matcher(q);

        int clozeIdx = 0;
        if (!m.find()) {
            return;
        }


        String fld = m.group(1);
        // if it's a cloze, extract data
        if (fld.startsWith("cloze:", 0)) {
            // get field and cloze position
            clozeIdx = mCurrentCard.getOrd() + 1;
            fld = fld.split(":")[1];
        }
        // loop through fields for a match
        JSONArray flds = mCurrentCard.model().getJSONArray("flds");
        for (int i = 0; i < flds.length(); i++) {
            String name = flds.getJSONObject(i).getString("name");
            if (name.equals(fld)) {
                mTypeCorrect = mCurrentCard.note().getItem(name);
                if (clozeIdx != 0) {
                    // narrow to cloze
                    mTypeCorrect = contentForCloze(mTypeCorrect, clozeIdx);
                }
                mTypeFont = flds.getJSONObject(i).getString("font");
                mTypeSize = flds.getJSONObject(i).getInt("size");
                break;
            }
        }
        if (mTypeCorrect == null) {
            if (clozeIdx != 0) {
                mTypeWarning = getResources().getString(R.string.empty_card_warning);
            } else {
                mTypeWarning = getResources().getString(R.string.unknown_type_field_warning, fld);
            }
        } else if ("".equals(mTypeCorrect)) {
            mTypeCorrect = null;
        } else {
            mTypeWarning = null;
        }
    }


    /**
     * Format question field when it contains typeAnswer or clozes. If there was an error during type text extraction, a
     * warning is displayed
     *
     * @param buf The question text
     * @return The formatted question text
     */
    private String typeAnsQuestionFilter(String buf) {
        Matcher m = sTypeAnsPat.matcher(buf);
        if (mTypeWarning != null) {
            return m.replaceFirst(mTypeWarning);
        }
        StringBuilder sb = new StringBuilder();
        if (mUseInputTag) {
            // These functions are defined in the JavaScript file assets/scripts/card.js. We get the text back in
            // shouldOverrideUrlLoading() in createWebView() in this file.
            sb.append("<center>\n<input type=\"text\" name=\"typed\" id=\"typeans\" onfocus=\"taFocus();\" " +
                    "onblur=\"taBlur(this);\" onKeyPress=\"return taKey(this, event)\" autocomplete=\"off\" ");
            // We have to watch out. For the preview we don’t know the font or font size. Skip those there. (Anki
            // desktop just doesn’t show the input tag there. Do it with standard values here instead.)
            if (mTypeFont != null && !TextUtils.isEmpty(mTypeFont) && mTypeSize > 0) {
                sb.append("style=\"font-family: '").append(mTypeFont).append("'; font-size: ")
                        .append(Integer.toString(mTypeSize)).append("px;\" ");
            }
            sb.append(">\n</center>\n");
        } else {
            sb.append("<span id=\"typeans\" class=\"typePrompt");
            if (mUseInputTag) {
                sb.append(" typeOff");
            }
            sb.append("\">........</span>");
        }
        return m.replaceAll(sb.toString());
    }


    /**
     * Fill the placeholder for the type comparison. Show the correct answer, and the comparison if appropriate.
     *
     * @param buf           The answer text
     * @param userAnswer    Text typed by the user, or empty.
     * @param correctAnswer The correct answer, taken from the note.
     * @return The formatted answer text
     */
    @VisibleForTesting
    String typeAnsAnswerFilter(String buf, String userAnswer, String correctAnswer) {
        Matcher m = sTypeAnsPat.matcher(buf);
        DiffEngine diffEngine = new DiffEngine();
        StringBuilder sb = new StringBuilder();
        sb.append("<div><code id=\"typeans\">");

        // We have to use Matcher.quoteReplacement because the inputs here might have $ or \.

        if (!TextUtils.isEmpty(userAnswer)) {
            // The user did type something.
            if (userAnswer.equals(correctAnswer)) {
                // and it was right.
                sb.append(Matcher.quoteReplacement(DiffEngine.wrapGood(correctAnswer)));
                sb.append("<span id=\"typecheckmark\">\u2714</span>"); // Heavy check mark
            } else {
                // Answer not correct.
                // Only use the complex diff code when needed, that is when we have some typed text that is not
                // exactly the same as the correct text.
                String[] diffedStrings = diffEngine.diffedHtmlStrings(correctAnswer, userAnswer);
                // We know we get back two strings.
                sb.append(Matcher.quoteReplacement(diffedStrings[0]));
                sb.append("<br><span id=\"typearrow\">&darr;</span><br>");
                sb.append(Matcher.quoteReplacement(diffedStrings[1]));
            }
        } else {
            if (!mUseInputTag) {
                sb.append(Matcher.quoteReplacement(DiffEngine.wrapMissing(correctAnswer)));
            } else {
                sb.append(Matcher.quoteReplacement(correctAnswer));
            }
        }
        sb.append("</code></div>");
        return m.replaceAll(sb.toString());
    }


    /**
     * Return the correct answer to use for {{type::cloze::NN}} fields.
     *
     * @param txt The field text with the clozes
     * @param idx The index of the cloze to use
     * @return A string with a comma-separeted list of unique cloze strings with the corret index.
     */

    private String contentForCloze(String txt, int idx) {
        Pattern re = Pattern.compile("\\{\\{c" + idx + "::(.+?)\\}\\}");
        Matcher m = re.matcher(txt);
        Set<String> matches = new LinkedHashSet<>();
        // LinkedHashSet: make entries appear only once, like Anki desktop (see also issue #2208), and keep the order
        // they appear in.
        String groupOne;
        int colonColonIndex = -1;
        while (m.find()) {
            groupOne = m.group(1);
            colonColonIndex = groupOne.indexOf("::");
            if (colonColonIndex > -1) {
                // Cut out the hint.
                groupOne = groupOne.substring(0, colonColonIndex);
            }
            matches.add(groupOne);
        }
        // Now do what the pythonic ", ".join(matches) does in a tricky way
        String prefix = "";
        StringBuilder resultBuilder = new StringBuilder();
        for (String match : matches) {
            resultBuilder.append(prefix);
            resultBuilder.append(match);
            prefix = ", ";
        }
        return resultBuilder.toString();
    }


    private Handler mTimerHandler = new Handler();

    private Runnable removeChosenAnswerText = new Runnable() {
        @Override
        public void run() {
            mChosenAnswer.setText("");
        }
    };

    protected int mPrefWaitAnswerSecond;
    protected int mPrefWaitQuestionSecond;


    protected int getAnswerButtonCount() {
        return getCol().getSched().answerButtons(mCurrentCard);
    }


    // ----------------------------------------------------------------------------
    // ANDROID METHODS
    // ----------------------------------------------------------------------------
    protected TextView mTitle;
    private SharedPreferences preferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate()");

        preferences = restorePreferences();
        mCardAppearance = CardAppearance.create(new ReviewerCustomFonts(this.getBaseContext()), preferences);
        super.onCreate(savedInstanceState);
        setContentView(getContentViewAttr(mPrefFullscreenReview));

        // Make ACTION_PROCESS_TEXT for in-app searching possible on > Android 4.0
        getDelegate().setHandleNativeActionModesEnabled(true);

        //        initNavigationDrawer(mainView);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            // enable ActionBar app icon to behave as action to toggle nav drawer
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
//            int[] attrs = new int[] {
//                    R.attr.reviewStatusBarColor,
//            };
//            TypedArray ta = obtainStyledAttributes(attrs);
//            toolbar.setBackground(ta.getDrawable(0));
            mTitle = toolbar.findViewById(R.id.toolbar_title);
            mTitle.setVisibility(View.VISIBLE);

            // Decide which action to take when the navigation button is tapped.
//            toolbar.setNavigationOnClickListener(v -> onNavigationPressed());
        }
        mShortAnimDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mFreeVipCount = preferences.getInt("speak_count", 0);
        mVip = preferences.getBoolean(Consts.KEY_IS_VIP, false);
        mFreeOnlineEngineCount = preferences.getInt(Consts.KEY_REST_ONLINE_SPEAK_COUNT, 1000);
//        mFreeVipRecordDay = preferences.getInt("speak_count_day", 0);
//        Calendar calendar = Calendar.getInstance();
//        if (mFreeVipRecordDay != calendar.get(Calendar.DAY_OF_YEAR)) {//已经不是记录里的同一天
//            mFreeVipCount = 0;
//            preferences.edit().putInt("speak_count", mFreeVipCount)
//                    .putInt("speak_count_day", calendar.get(Calendar.DAY_OF_YEAR)).apply();
//        }
        mainHandler = new Handler();

    }


    protected int getContentViewAttr(int fullscreenMode) {
        return R.layout.reviewer;
    }


    protected boolean isFullscreen() {
        isInFullscreen = !getSupportActionBar().isShowing();
        return isInFullscreen;
    }


    @Override
    public void onConfigurationChanged(Configuration config) {
        // called when screen rotated, etc, since recreating the Webview is too expensive
        super.onConfigurationChanged(config);
        refreshActionBar();
    }


    protected abstract void setTitle();


    // Finish initializing the activity after the collection has been correctly loaded
    @Override
    protected void onCollectionLoaded(Collection col) {
        super.onCollectionLoaded(col);
        mSched = col.getSched();
        mBaseUrl = Utils.getBaseUrl(col.getMedia().dir());

        registerExternalStorageListener();

        restoreCollectionPreferences();

        initLayout();

        setTitle();

        if (!mDisableClipboard) {
            clipboardSetText("");
        }

        // Load the template for the card
        try {
            mCardTemplate = Utils.convertStreamToString(getAssets().open("card_template.html"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize text-to-speech. This is an asynchronous operation.
//        if (mSpeakText) {
//            ReadText.initializeTts(this, true,false,new ReadTextListener());
//        }

        // Initialize dictionary lookup feature
        Lookup.initialize(this);

        updateActionBar();
        supportInvalidateOptionsMenu();
    }


    // Saves deck each time Reviewer activity loses focus
    @Override
    protected void onPause() {
        super.onPause();
        Timber.d("onPause()");

        mTimeoutHandler.removeCallbacks(mShowAnswerTask);
        mTimeoutHandler.removeCallbacks(mShowQuestionTask);
        longClickHandler.removeCallbacks(longClickTestRunnable);
        longClickHandler.removeCallbacks(startLongClickAction);

        pauseTimer();
        mSoundPlayer.stopSounds();
        stopOnlineSpeaking();
        // Prevent loss of data in Cookies
        CompatHelper.getCompat().flushWebViewCookies();
    }


    private void stopOnlineSpeaking() {
        if (synthesizer != null && synthesizer.isInitied()) {
            synthesizer.stop();
            mOnlineSpeaking = false;
            speakingHandler.obtainMessage(10086).sendToTarget();
        }
        Timber.e("stop online speaking!");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Tells the scheduler there is no more current cards. 0 is
        // not a valid id.
        activityStop = true;
        if (mSched != null) {
            mSched.discardCurrentCard();
        }
        Timber.d("onDestroy()");
        speakingHandler.removeCallbacksAndMessages(null);
        if (mTtsInitialized) {
            ReadText.releaseTts();
        }
        if (synthesizer != null && synthesizer.isInitied()) {
            synthesizer.release();
        }

        if (mUnmountReceiver != null) {
            unregisterReceiver(mUnmountReceiver);
        }
        // WebView.destroy() should be called after the end of use
        // http://developer.android.com/reference/android/webkit/WebView.html#destroy()
        if (mCardFrame != null) {
            mCardFrame.removeAllViews();
        }

        destroyWebView(mCardWebView); //OK to do without a lock
    }


    @Override
    public void onBackPressed() {
//        if (isDrawerOpen()) {
//            super.onBackPressed();
//        } else
        {
            Timber.i("Back key pressed");
            closeReviewer(RESULT_DEFAULT, false);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (processCardFunction(cardWebView -> processHardwareButtonScroll(keyCode, cardWebView))) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }


    private boolean processHardwareButtonScroll(int keyCode, WebView card) {
        if (keyCode == KeyEvent.KEYCODE_PAGE_UP) {
            card.pageUp(false);
            if (mDoubleScrolling) {
                card.pageUp(false);
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_PAGE_DOWN) {
            card.pageDown(false);
            if (mDoubleScrolling) {
                card.pageDown(false);
            }
            return true;
        }
        if (mScrollingButtons && keyCode == KeyEvent.KEYCODE_PICTSYMBOLS) {
            card.pageUp(false);
            if (mDoubleScrolling) {
                card.pageUp(false);
            }
            return true;
        }
        if (mScrollingButtons && keyCode == KeyEvent.KEYCODE_SWITCH_CHARSET) {
            card.pageDown(false);
            if (mDoubleScrolling) {
                card.pageDown(false);
            }
            return true;
        }
        return false;
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (answerFieldIsFocused()) {
            return super.onKeyUp(keyCode, event);
        }
        if (!sDisplayAnswer) {
            if (keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                displayCardAnswer();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }


    protected boolean answerFieldIsFocused() {
        return mAnswerField != null && mAnswerField.isFocused();
    }


    @SuppressWarnings("deprecation") // Tracked separately as #5023 on github
    protected boolean clipboardHasText() {
        return mClipboard != null && mClipboard.hasText();
    }


    @SuppressWarnings("deprecation") // Tracked separately as #5023 on github
    private void clipboardSetText(CharSequence text) {
        if (mClipboard != null) {
            try {
                mClipboard.setText(text);
            } catch (Exception e) {
                // https://code.google.com/p/ankidroid/issues/detail?id=1746
                // https://code.google.com/p/ankidroid/issues/detail?id=1820
                // Some devices or external applications make the clipboard throw exceptions. If this happens, we
                // must disable it or AnkiDroid will crash if it tries to use it.
                Timber.e("Clipboard error. Disabling text selection setting.");
                mDisableClipboard = true;
            }
        }
    }


    /**
     * Returns the text stored in the clipboard or the empty string if the clipboard is empty or contains something that
     * cannot be convered to text.
     *
     * @return the text in clipboard or the empty string.
     */
    @SuppressWarnings("deprecation") // Tracked separately as #5023 on github
    private CharSequence clipboardGetText() {
        CharSequence text = mClipboard != null ? mClipboard.getText() : null;
        return text != null ? text : "";
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Timber.i("onActivityResult:" + requestCode);
        if (requestCode == BE_VIP || requestCode == REFRESH_LOGIN_STATE_AND_TURN_TO_VIP_HTML) {
            mRefreshVipStateOnResume = true;
            mTurnToVipHtml = requestCode == REFRESH_LOGIN_STATE_AND_TURN_TO_VIP_HTML;
        } else if (requestCode == REFRESH_VOICE_INFO) {

            mRefreshVoiceInfoStateOnResume = true;
        } else if (resultCode == DeckPicker.RESULT_DB_ERROR) {
            closeReviewer(DeckPicker.RESULT_DB_ERROR, false);
        } else if (resultCode == DeckPicker.RESULT_MEDIA_EJECTED) {
            finishNoStorageAvailable();
        } else if (requestCode == REFRESH_TOP_BUTTONS) {
            restorePreferences();
            supportInvalidateOptionsMenu();
            invalidateOptionsMenu();

        } else if (requestCode == REQUEST_CODE_SPEAK_SETTING) {
            restorePreferences();
            mReInitBDVoice = true;
        } else if (requestCode == REFRESH_GESTURE) {
            restorePreferences();
        } else if (requestCode == REFRESH_CONTROLLER) {
            restorePreferences();
        }

        /* Reset the schedule and reload the latest card off the top of the stack if required.
           The card could have been rescheduled, the deck could have changed, or a change of
           note type could have lead to the card being deleted */
        if (data != null && data.hasExtra("reloadRequired")) {
            performReload();
        }

        if (requestCode == EDIT_CURRENT_CARD) {
            if (resultCode == RESULT_OK) {
                // content of note was changed so update the note and current card
                Timber.i("AbstractFlashcardViewer:: Saving card...");
                CollectionTask.launchCollectionTask(UPDATE_NOTE, mUpdateCardHandler,
                        new TaskData(sEditorCard, true));
                onEditedNoteChanged();
            } else if (resultCode == RESULT_CANCELED && !(data != null && data.hasExtra("reloadRequired"))) {
                // nothing was changed by the note editor so just redraw the card
                redrawCard();
            }
        } else if (requestCode == DECK_OPTIONS && resultCode == RESULT_OK) {
            performReload();
        }
        if (!mDisableClipboard) {
            clipboardSetText("");
        }
    }


    protected void onEditedNoteChanged() {

    }


    /**
     * An action which may invalidate the current list of cards has been performed
     */
    protected abstract void performReload();


    // ----------------------------------------------------------------------------
    // CUSTOM METHODS
    // ----------------------------------------------------------------------------


    // Get the did of the parent deck (ignoring any subdecks)
    protected long getParentDid() {
        long deckID = getCol().getDecks().selected();
        return deckID;
    }


    private void redrawCard() {
        //#3654 We can call this from ActivityResult, which could mean that the card content hasn't yet been set
        //if the activity was destroyed. In this case, just wait until onCollectionLoaded callback succeeds.
        if (hasLoadedCardContent()) {
            fillFlashcard();
        } else {
            Timber.i("Skipping card redraw - card still initialising.");
        }
    }


    /**
     * Whether the callback to onCollectionLoaded has loaded card content
     */
    private boolean hasLoadedCardContent() {
        return mCardContent != null;
    }


    public GestureDetectorCompat getGestureDetector() {
        return gestureDetector;
    }


    /**
     * Show/dismiss dialog when sd card is ejected/remounted (collection is saved by SdCardReceiver)
     */
    private void registerExternalStorageListener() {
        if (mUnmountReceiver == null) {
            mUnmountReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(SdCardReceiver.MEDIA_EJECT)) {
                        finishWithoutAnimation();
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(SdCardReceiver.MEDIA_EJECT);
            registerReceiver(mUnmountReceiver, iFilter);
        }
    }


    private void pauseTimer() {
        if (mCurrentCard != null) {
            mCurrentCard.stopTimer();
        }
        // We also stop the UI timer so it doesn't trigger the tick listener while paused. Letting
        // it run would trigger the time limit condition (red, stopped timer) in the background.
        if (mCardTimer != null) {
            mCardTimer.stop();
        }
    }


    private void resumeTimer() {
        if (mCurrentCard != null) {
            // Resume the card timer first. It internally accounts for the time gap between
            // suspend and resume.
            mCurrentCard.resumeTimer();
            // Then update and resume the UI timer. Set the base time as if the timer had started
            // timeTaken() seconds ago.
            mCardTimer.setBase(SystemClock.elapsedRealtime() - mCurrentCard.timeTaken());
            // Don't start the timer if we have already reached the time limit or it will tick over
            if ((SystemClock.elapsedRealtime() - mCardTimer.getBase()) < mCurrentCard.timeLimit()) {
                mCardTimer.start();
            }
        }
    }


    protected void undo() {
        if (isUndoAvailable()) {
            CollectionTask.launchCollectionTask(UNDO, mAnswerCardHandler(false));
        }
    }


    private void finishNoStorageAvailable() {
        AbstractFlashcardViewer.this.setResult(DeckPicker.RESULT_MEDIA_EJECTED);
        finishWithoutAnimation();
    }


    protected boolean editCard() {
        if (mCurrentCard == null) {
            // This should never occurs. It means the review button was pressed while there is no more card in the reviewer.
            return true;
        }
        Intent editCard = new Intent(AbstractFlashcardViewer.this, NoteEditor.class);
        editCard.putExtra(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_REVIEWER);
        sEditorCard = mCurrentCard;
        startActivityForResultWithAnimation(editCard, EDIT_CURRENT_CARD, ActivityTransitionAnimation.LEFT);
        return true;
    }


    protected void generateQuestionSoundList() {
        mSoundPlayer.addSounds(mBaseUrl, mCurrentCard.qSimple(), SoundSide.QUESTION);
    }


    protected void lookUpOrSelectText() {
        if (clipboardHasText()) {
            Timber.d("Clipboard has text = " + clipboardHasText());
            lookUp();
        } else {
            selectAndCopyText();
        }
    }


    private boolean lookUp() {
        mLookUpIcon.setVisibility(View.GONE);
        mIsSelecting = false;
        if (Lookup.lookUp(clipboardGetText().toString())) {
            clipboardSetText("");
        }
        return true;
    }


    private void showLookupButtonIfNeeded() {
        if (!mDisableClipboard && mClipboard != null) {
            if (clipboardGetText().length() != 0 && Lookup.isAvailable() && mLookUpIcon.getVisibility() != View.VISIBLE) {
                mLookUpIcon.setVisibility(View.VISIBLE);
                enableViewAnimation(mLookUpIcon, ViewAnimation.fade(ViewAnimation.FADE_IN, mFadeDuration, 0));
            } else if (mLookUpIcon.getVisibility() == View.VISIBLE) {
                mLookUpIcon.setVisibility(View.GONE);
                enableViewAnimation(mLookUpIcon, ViewAnimation.fade(ViewAnimation.FADE_OUT, mFadeDuration, 0));
            }
        }
    }


    private void hideLookupButton() {
        if (!mDisableClipboard && mLookUpIcon.getVisibility() != View.GONE) {
            mLookUpIcon.setVisibility(View.GONE);
            enableViewAnimation(mLookUpIcon, ViewAnimation.fade(ViewAnimation.FADE_OUT, mFadeDuration, 0));
            clipboardSetText("");
        }
    }


    protected void showDeleteNoteDialog() {
        Resources res = getResources();
        new MaterialDialog.Builder(this)
                .title(res.getString(R.string.delete_card_title))
                .iconAttr(R.attr.dialogErrorIcon)
                .content(res.getString(R.string.delete_note_message,
                        Utils.stripHTML(mCurrentCard.q(true))))
                .positiveText(res.getString(R.string.dialog_positive_delete))
                .negativeText(res.getString(R.string.dialog_cancel))
                .onPositive((dialog, which) -> {
                    Timber.i("AbstractFlashcardViewer:: OK button pressed to delete note %d", mCurrentCard.getNid());
                    mSoundPlayer.stopSounds();
                    stopOnlineSpeaking();
                    dismiss(Collection.DismissType.DELETE_NOTE);
                })
                .build().show();
    }


    private int getRecommendedEase(boolean easy) {
        try {
            switch (getAnswerButtonCount()) {
                case 2:
                    return EASE_2;
                case 3:
                    return easy ? EASE_3 : EASE_2;
                case 4:
                    return easy ? EASE_4 : EASE_3;
                default:
                    return 0;
            }
        } catch (RuntimeException e) {
            AnkiDroidApp.sendExceptionReport(e, "AbstractReviewer-getRecommendedEase");
            closeReviewer(DeckPicker.RESULT_DB_ERROR, true);
            return 0;
        }
    }


    protected void answerCard(@Consts.BUTTON_TYPE int ease) {
        if (mInAnswer) {
            return;
        }
        mIsSelecting = false;
        hideLookupButton();
        int buttonNumber = getCol().getSched().answerButtons(mCurrentCard);
        // Detect invalid ease for current card (e.g. by using keyboard shortcut or gesture).
        if (buttonNumber < ease) {
            return;
        }
        // Set the dots appearing below the toolbar
        switch (ease) {
            case EASE_1:
                mChosenAnswer.setText("\u2022");
                mChosenAnswer.setTextColor(ContextCompat.getColor(this, R.color.material_red_500));
                break;
            case EASE_2:
                mChosenAnswer.setText("\u2022\u2022");
                mChosenAnswer.setTextColor(ContextCompat.getColor(this, buttonNumber == Consts.BUTTON_FOUR ?
                        R.color.material_blue_grey_600 :
                        R.color.material_green_500));
                break;
            case EASE_3:
                mChosenAnswer.setText("\u2022\u2022\u2022");
                mChosenAnswer.setTextColor(ContextCompat.getColor(this, buttonNumber == Consts.BUTTON_FOUR ?
                        R.color.material_green_500 :
                        R.color.material_light_blue_500));
                break;
            case EASE_4:
                mChosenAnswer.setText("\u2022\u2022\u2022\u2022");
                mChosenAnswer.setTextColor(ContextCompat.getColor(this, R.color.material_light_blue_500));
                break;
            default:
                Timber.w("Unknown easy type %s", ease);
                break;
        }

        // remove chosen answer hint after a while
        mTimerHandler.removeCallbacks(removeChosenAnswerText);
        mTimerHandler.postDelayed(removeChosenAnswerText, mShowChosenAnswerLength);
        mSoundPlayer.stopSounds();
        stopOnlineSpeaking();
        mCurrentEase = ease;

        CollectionTask.launchCollectionTask(ANSWER_CARD, mAnswerCardHandler(true),
                new TaskData(mCurrentCard, mCurrentEase));
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            // assign correct gesture code
            int gesture = COMMAND_NOTHING;

            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    gesture = mGestureVolumeUp;
                    break;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    gesture = mGestureVolumeDown;
                    break;
            }

            // Execute gesture's command, but only consume event if action is assigned. We want the volume buttons to work normally otherwise.
            if (gesture != COMMAND_NOTHING) {
                executeCommand(gesture);
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }


    private View.OnTouchListener getRemarkTouchListener() {
        return new View.OnTouchListener() {
            private long time = 0;
            private float eventDownY = 0;
            private float maxMargin = Resources.getSystem().getDisplayMetrics().heightPixels;


            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        if (Math.abs(event.getRawY() - eventDownY) > 50) {
                            remarkOriginBottomMargin = remarkLayoutParams.bottomMargin;
                            Timber.i("保存 remarkOriginBottomMargin = " + remarkOriginBottomMargin);
                            AnkiDroidApp.getSharedPrefs(getBaseContext()).edit().putFloat("btn_remark_bottom_margin", remarkOriginBottomMargin).apply();
                        } else {
                            if (System.currentTimeMillis() - time < 500) {
                                showRemarkDialog();
                            }
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (System.currentTimeMillis() - time < 200) {
                            break;
                        }
                        float moveY = event.getRawY() - eventDownY;
                        Timber.i("moveY = " + moveY);
                        remarkLayoutParams.bottomMargin = (int) Math.min(maxMargin * 0.7, Math.max(0, remarkOriginBottomMargin - moveY));
                        mRemark.setLayoutParams(remarkLayoutParams);
                        break;
                    case MotionEvent.ACTION_DOWN:
                        time = System.currentTimeMillis();
                        eventDownY = event.getRawY();
                        break;
                }
                return true;
            }
        };
    }


    // Set the content view to the one provided and initialize accessors.
    @SuppressLint("SuspiciousIndentation")
    @SuppressWarnings("deprecation") // Tracked separately as #5023 on github for clipboard
    protected void initLayout() {
        FrameLayout mCardContainer = (FrameLayout) findViewById(R.id.flashcard_frame);

        mTopBarLayout = (RelativeLayout) findViewById(R.id.top_bar);

        ImageView mark = mTopBarLayout.findViewById(R.id.mark_icon);
        ImageView flag = mTopBarLayout.findViewById(R.id.flag_icon);
        mCardMarker = new CardMarker(mark, flag);

        mCardFrame = (FrameLayout) findViewById(R.id.flashcard);
        mCardFrameParent = (ViewGroup) mCardFrame.getParent();
        mTouchLayer = (FrameLayout) findViewById(R.id.touch_layer);
        mTouchLayer.setOnTouchListener(mGestureListener);
        if (!mDisableClipboard) {
            mClipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        }
        mCardFrame.removeAllViews();

        // Initialize swipe
        mGestureDetectorImpl = mLinkOverridesTouchGesture ? new LinkDetectingGestureDetector() : new MyGestureDetector();
        gestureDetector = new GestureDetectorCompat(this, mGestureDetectorImpl);

        mEaseButtonsLayout = findViewById(R.id.ease_buttons);
        mRemark = findViewById(R.id.btn_remark);
        if (mRemark != null) {
            mRemark.setOnTouchListener(getRemarkTouchListener());
            remarkLayoutParams = (RelativeLayout.LayoutParams) mRemark.getLayoutParams();
            float bottomMargin = AnkiDroidApp.getSharedPrefs(getBaseContext()).getFloat("btn_remark_bottom_margin", 0);
            Timber.i("bottomMargin = " + bottomMargin);
            if (bottomMargin > 0) {
                remarkLayoutParams.bottomMargin += bottomMargin;
                mRemark.setLayoutParams(remarkLayoutParams);
            }
            remarkOriginBottomMargin = remarkLayoutParams.bottomMargin;
            if (!AnkiDroidApp.getSharedPrefs(getBaseContext()).getBoolean("enable_remark", true)) {
                mRemark.setVisibility(View.GONE);
            }
        }
        mEase1 = findViewById(R.id.ease1);
        mEase1Layout = findViewById(R.id.flashcard_layout_ease1);
        mEase1Layout.setOnClickListener(mSelectEaseHandler);

        mEase2 = findViewById(R.id.ease2);
        mEase2Layout = findViewById(R.id.flashcard_layout_ease2);
        mEase2Layout.setOnClickListener(mSelectEaseHandler);

        mEase3 = findViewById(R.id.ease3);
        mEase3Layout = findViewById(R.id.flashcard_layout_ease3);
        mEase3Layout.setOnClickListener(mSelectEaseHandler);

        mEase4 = findViewById(R.id.ease4);
        mEase4Layout = findViewById(R.id.flashcard_layout_ease4);
        mEase4Layout.setOnClickListener(mSelectEaseHandler);

        mNext1 = findViewById(R.id.nextTime1);
        mNext2 = findViewById(R.id.nextTime2);
        mNext3 = findViewById(R.id.nextTime3);
        mNext4 = findViewById(R.id.nextTime4);

        if (!mShowNextReviewTime) {
            mNext1.setVisibility(View.GONE);
            mNext2.setVisibility(View.GONE);
            mNext3.setVisibility(View.GONE);
            mNext4.setVisibility(View.GONE);
        }

        Button mFlipCard = (Button) findViewById(R.id.flip_card);
        mFlipCardLayout = (LinearLayout) findViewById(R.id.flashcard_layout_flip);
        mFlipCardLayout.setOnClickListener(mFlipCardListener);

//        if (Build.VERSION.SDK_INT >= 21 && animationEnabled()) {
//            mFlipCard.setBackgroundResource(Themes.getResFromAttr(this, R.attr.hardButtonRippleRef));
//        }

        if (!mButtonHeightSet && mRelativeButtonSize != 100) {
            ViewGroup.LayoutParams params = mFlipCardLayout.getLayoutParams();
            params.height = params.height * mRelativeButtonSize / 100;
            params = mEase1Layout.getLayoutParams();
            params.height = params.height * mRelativeButtonSize / 100;
            params = mEase2Layout.getLayoutParams();
            params.height = params.height * mRelativeButtonSize / 100;
            params = mEase3Layout.getLayoutParams();
            params.height = params.height * mRelativeButtonSize / 100;
            params = mEase4Layout.getLayoutParams();
            params.height = params.height * mRelativeButtonSize / 100;
            mButtonHeightSet = true;
        }

        mPreviewButtonsLayout = findViewById(R.id.preview_buttons_layout);

        mPreviewPrevCard = findViewById(R.id.preview_previous_flashcard);
        mPreviewNextCard = findViewById(R.id.preview_next_flashcard);
        mPreviewToggleAnswerText = findViewById(R.id.preview_flip_flashcard);

        mCardTimer = (Chronometer) findViewById(R.id.card_time);

        mChosenAnswer = (TextView) findViewById(R.id.choosen_answer);

        mAnswerField = (EditText) findViewById(R.id.answer_field);


        mLookUpIcon = findViewById(R.id.lookup_button);
        mLookUpIcon.setVisibility(View.GONE);
        mLookUpIcon.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Timber.i("AbstractFlashcardViewer:: Lookup button pressed");
                if (clipboardHasText()) {
                    lookUp();
                }
            }

        });


        initControls();

        // Position answer buttons
        String answerButtonsPosition = AnkiDroidApp.getSharedPrefs(this).getString(
                getString(R.string.answer_buttons_position_preference),
                "bottom"
        );
        LinearLayout answerArea = (LinearLayout) findViewById(R.id.bottom_area_layout);
        RelativeLayout.LayoutParams answerAreaParams = (RelativeLayout.LayoutParams) answerArea.getLayoutParams();
        RelativeLayout.LayoutParams cardContainerParams = (RelativeLayout.LayoutParams) mCardContainer.getLayoutParams();

        switch (answerButtonsPosition) {
            case "top":
                cardContainerParams.addRule(RelativeLayout.BELOW, R.id.bottom_area_layout);
                answerAreaParams.addRule(RelativeLayout.BELOW, R.id.mic_tool_bar_layer);
                answerArea.removeView(mAnswerField);
                answerArea.addView(mAnswerField, 1);
                break;
            case "bottom":
                cardContainerParams.addRule(RelativeLayout.ABOVE, R.id.bottom_area_layout);
                cardContainerParams.addRule(RelativeLayout.BELOW, R.id.mic_tool_bar_layer);
                answerAreaParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                break;
            default:
                Timber.w("Unknown answerButtonsPosition: %s", answerButtonsPosition);
                break;
        }
        answerArea.setLayoutParams(answerAreaParams);
        mCardContainer.setLayoutParams(cardContainerParams);
    }


    @SuppressLint("SetJavaScriptEnabled") // they request we review carefully because of XSS security, we have
    private WebView createWebView() {
        WebView webView = new MyWebView(this);
        webView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setSupportZoom(true);
        // Start at the most zoomed-out level
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new AnkiDroidWebChromeClient());
        webView.getSettings().setAllowFileAccess(true);

        // Problems with focus and input tags is the reason we keep the old type answer mechanism for old Androids.
        webView.setFocusableInTouchMode(mUseInputTag);
        webView.setScrollbarFadingEnabled(true);

        Timber.d("Focusable = %s, Focusable in touch mode = %s", webView.isFocusable(), webView.isFocusableInTouchMode());

        webView.setWebViewClient(new CardViewerWebClient());
        // Set transparent color to prevent flashing white when night mode enabled
        webView.setBackgroundColor(Color.argb(1, 0, 0, 0));

        // Javascript interface for calling AnkiDroid functions in webview, see card.js
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(javaScriptFunction(), "AnkiDroidJS");

//        webView.setOnTouchListener((v, event) -> {
//            if(event.getAction()==MotionEvent.ACTION_DOWN){
//
//            }
//            return false;
//        });
        return webView;
    }


    /**
     * If a card is displaying the question, flip it, otherwise answer it
     */
    private void flipOrAnswerCard(int cardOrdinal) {
        if (!sDisplayAnswer) {
            displayCardAnswer();
            return;
        }
        performClickWithVisualFeedback(cardOrdinal);
    }


    private boolean webViewRendererLastCrashedOnCard(long cardId) {
        return lastCrashingCardId != null && lastCrashingCardId == cardId;
    }


    private boolean canRecoverFromWebViewRendererCrash() {
        // DEFECT
        // If we don't have a card to render, we're in a bad state. The class doesn't currently track state
        // well enough to be able to know exactly where we are in the initialisation pipeline.
        // so it's best to mark the crash as non-recoverable.
        // We should fix this, but it's very unlikely that we'll ever get here. Logs will tell

        // Revisit webViewCrashedOnCard() if changing this. Logic currently assumes we have a card.
        return mCurrentCard != null;
    }


    //#5780 - Users could OOM the WebView Renderer. This triggers the same symptoms
    @VisibleForTesting()
    @SuppressWarnings("unused")
    public void crashWebViewRenderer() {
        loadUrlInViewer("chrome://crash");
    }


    /**
     * Used to set the "javascript:" URIs for IPC
     */
    private void loadUrlInViewer(final String url) {
        processCardAction(cardWebView -> cardWebView.loadUrl(url));
    }


    private <T extends View> T inflateNewView(@IdRes int id) {
        int layoutId = getContentViewAttr(mPrefFullscreenReview);
        ViewGroup content = (ViewGroup) LayoutInflater.from(AbstractFlashcardViewer.this).inflate(layoutId, null, false);
        T ret = content.findViewById(id);
        ((ViewGroup) ret.getParent()).removeView(ret); //detach the view from its parent
        content.removeAllViews();
        return ret;
    }


    private void destroyWebView(WebView webView) {
        try {
            if (webView != null) {
                webView.stopLoading();
                webView.setWebChromeClient(null);
                webView.setWebViewClient(null);
                webView.destroy();
                if (fetchRenderHandler != null) {
                    fetchRenderHandler.removeCallbacksAndMessages(null);
                }
            }
        } catch (NullPointerException npe) {
            Timber.e(npe, "WebView became null on destruction");
        }
    }


    protected boolean shouldShowNextReviewTime() {
        return mShowNextReviewTime;
    }


    protected void displayAnswerBottomBar() {
        mFlipCardLayout.setClickable(false);
        mEaseButtonsLayout.setVisibility(View.VISIBLE);

        Runnable after = () -> mFlipCardLayout.setVisibility(View.GONE);

        // hide "Show Answer" button
//        if (animationDisabled()) {
        after.run();
//        } else {
//            mFlipCardLayout.setAlpha(1);
//            mFlipCardLayout.animate().alpha(0).setDuration(mShortAnimDuration).withEndAction(after);
//        }
    }


    protected void hideEaseButtons() {
        Runnable after = () -> {
            mEaseButtonsLayout.setVisibility(View.GONE);
            mEase1Layout.setVisibility(View.GONE);
            mEase2Layout.setVisibility(View.GONE);
            mEase3Layout.setVisibility(View.GONE);
            mEase4Layout.setVisibility(View.GONE);


            mNext1.setText("");
            mNext2.setText("");
            mNext3.setText("");
            mNext4.setText("");
        };

//        boolean easeButtonsVisible = mEaseButtonsLayout.getVisibility() == View.VISIBLE;
        mFlipCardLayout.setClickable(true);
        mFlipCardLayout.setVisibility(View.VISIBLE);

//        if (animationDisabled() || !easeButtonsVisible) {
        after.run();
//        } else {
//            mFlipCardLayout.setAlpha(0);
//            mFlipCardLayout.animate().alpha(1).setDuration(mShortAnimDuration).withEndAction(after);
//        }

        focusAnswerCompletionField();
    }


    /**
     * Focuses the appropriate field for an answer
     * And allows keyboard shortcuts to go to the default handlers.
     */
    private void focusAnswerCompletionField() {
        // This does not handle mUseInputTag (the WebView contains an input field with a typable answer).
        // In this case, the user can use touch to focus the field if necessary.
        if (typeAnswer()) {
            mAnswerField.requestFocus();
        } else {
            mFlipCardLayout.requestFocus();
        }
    }


    protected void switchTopBarVisibility(int visible) {
        if (mShowTimer) {
            mCardTimer.setVisibility(visible);
        }
        mChosenAnswer.setVisibility(visible);
    }


    protected void initControls() {
        mCardFrame.setVisibility(View.VISIBLE);
        mChosenAnswer.setVisibility(View.VISIBLE);
        mFlipCardLayout.setVisibility(View.VISIBLE);

        mAnswerField.setVisibility(typeAnswer() ? View.VISIBLE : View.GONE);
        mAnswerField.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                displayCardAnswer();
                return true;
            }
            return false;
        });
        mAnswerField.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP &&
                        (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                    displayCardAnswer();
                    return true;
                }
                return false;
            }
        });
    }


    protected SharedPreferences restorePreferences() {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());

        mUseInputTag = preferences.getBoolean("useInputTag", false);
        // On newer Androids, ignore this setting, which should be hidden in the prefs anyway.
        mDisableClipboard = "0".equals(preferences.getString("dictionary", "0"));
        // mDeckFilename = preferences.getString("deckFilename", "");
        mPrefFullscreenReview = Integer.parseInt(preferences.getString("fullscreenMode", "0"));
        mRelativeButtonSize = preferences.getInt("answerButtonSize", 100);
        mSpeakText = preferences.getBoolean("tts", false);
        mPrefUseTimer = preferences.getBoolean("timeoutAnswer", false);
        mPrefWaitAnswerSecond = preferences.getInt("timeoutAnswerSeconds", 20);
        mPrefWaitQuestionSecond = preferences.getInt("timeoutQuestionSeconds", 60);
        mScrollingButtons = preferences.getBoolean("scrolling_buttons", false);
        mDoubleScrolling = preferences.getBoolean("double_scrolling", false);
        mPrefShowTopbar = preferences.getBoolean("showTopbar", true);

        mGesturesEnabled = AnkiDroidApp.initiateGestures(preferences);
        mLinkOverridesTouchGesture = preferences.getBoolean("linkOverridesTouchGesture", false);
        if (mGesturesEnabled) {
            mGestureSwipeUp = Integer.parseInt(preferences.getString("gestureSwipeUp", "9"));
            mGestureSwipeDown = Integer.parseInt(preferences.getString("gestureSwipeDown", "0"));
            mGestureSwipeLeft = Integer.parseInt(preferences.getString("gestureSwipeLeft", "8"));
            mGestureSwipeRight = Integer.parseInt(preferences.getString("gestureSwipeRight", "17"));
            mGestureDoubleTap = Integer.parseInt(preferences.getString("gestureDoubleTap", "7"));
            mGestureTapLeft = Integer.parseInt(preferences.getString("gestureTapLeft", "3"));
            mGestureTapRight = Integer.parseInt(preferences.getString("gestureTapRight", "6"));
            mGestureTapTop = Integer.parseInt(preferences.getString("gestureTapTop", "12"));
            mGestureTapBottom = Integer.parseInt(preferences.getString("gestureTapBottom", "2"));
            mGestureLongclick = Integer.parseInt(preferences.getString("gestureLongclick", "11"));
            mGestureVolumeUp = Integer.parseInt(preferences.getString("gestureVolumeUp", "0"));
            mGestureVolumeDown = Integer.parseInt(preferences.getString("gestureVolumeDown", "0"));
        }
        mControllerA = Integer.parseInt(preferences.getString("A", "2"));
        mControllerB = Integer.parseInt(preferences.getString("B", "3"));
        mControllerX = Integer.parseInt(preferences.getString("X", "4"));
        mControllerY = Integer.parseInt(preferences.getString("Y", "5"));
        mControllerUp = Integer.parseInt(preferences.getString("up", "30"));
        mControllerDown = Integer.parseInt(preferences.getString("down", "31"));
        mControllerLeft = Integer.parseInt(preferences.getString("left", "8"));
        mControllerRight = Integer.parseInt(preferences.getString("right", "9"));
        mControllerLT = Integer.parseInt(preferences.getString("lt", "20"));
        mControllerRT = Integer.parseInt(preferences.getString("rt", "0"));
        mControllerLB = Integer.parseInt(preferences.getString("lb", "0"));
        mControllerRB = Integer.parseInt(preferences.getString("tb", "0"));
        mControllerLeftPad = Integer.parseInt(preferences.getString("leftPad", "0"));
        mControllerRightPad = Integer.parseInt(preferences.getString("rightPad", "0"));
        mControllerOption = Integer.parseInt(preferences.getString("option", "0"));
        mControllerMenu = Integer.parseInt(preferences.getString("menu", "0"));

        if (preferences.getBoolean("keepScreenOn", false)) {
            this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        return preferences;
    }


    protected void restoreCollectionPreferences() {

        // These are preferences we pull out of the collection instead of SharedPreferences
        try {
            mShowNextReviewTime = getCol().getConf().getBoolean("estTimes");

            // Dynamic don't have review options; attempt to get deck-specific auto-advance options
            // but be prepared to go with all default if it's a dynamic deck
            JSONObject revOptions = new JSONObject();
            long selectedDid = getCol().getDecks().selected();
            if (!getCol().getDecks().isDyn(selectedDid)) {
                revOptions = getCol().getDecks().confForDid(selectedDid).getJSONObject("rev");
            }

            mOptUseGeneralTimerSettings = revOptions.optBoolean("useGeneralTimeoutSettings", true);
            mOptUseTimer = revOptions.optBoolean("timeoutAnswer", false);
            mOptWaitAnswerSecond = revOptions.optInt("timeoutAnswerSeconds", 20);
            mOptWaitQuestionSecond = revOptions.optInt("timeoutQuestionSeconds", 60);
        } catch (JSONException e) {
            Timber.e(e, "Unable to restoreCollectionPreferences");
            throw new RuntimeException(e);
        } catch (NullPointerException npe) {
            // NPE on collection only happens if the Collection is broken, follow AnkiActivity example
            Intent deckPicker = new Intent(this, DeckPicker.class);
            deckPicker.putExtra("collectionLoadError", true); // don't currently do anything with this
            deckPicker.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityWithAnimation(deckPicker, ActivityTransitionAnimation.LEFT);
        }
    }


    private void setInterface() {
        if (mCurrentCard == null) {
            return;
        }
        recreateWebView();
    }


    private void recreateWebView() {
        if (mCardWebView == null) {
            mCardWebView = createWebView();
            WebViewDebugging.initializeDebugging(AnkiDroidApp.getSharedPrefs(this));
            mCardFrame.addView(mCardWebView);
            mGestureDetectorImpl.onWebViewCreated(mCardWebView);
        }
        if (mCardWebView.getVisibility() != View.VISIBLE) {
            mCardWebView.setVisibility(View.VISIBLE);
        }
    }


    private void updateForNewCard() {
        updateActionBar();

        // Clean answer field
        if (typeAnswer()) {
            mAnswerField.setText("");
        }

        if (mPrefWhiteboard && mWhiteboard != null) {
            mWhiteboard.clear();
        }
    }


    protected void updateActionBar() {
        updateDeckName();
    }


    protected void updateDeckName() {
        if (mCurrentCard == null) {
            return;
        }
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            String title = Decks.basename(getCol().getDecks().get(mCurrentCard.getDid()).getString("name"));
            actionBar.setTitle(title);
        }

        if (!mPrefShowTopbar) {
            mTopBarLayout.setVisibility(View.GONE);
        }
    }


    /*
     * Handler for the delay in auto showing question and/or answer One toggle for both question and answer, could set
     * longer delay for auto next question
     */
    protected Handler mTimeoutHandler = new Handler();

    protected Runnable mShowQuestionTask = new Runnable() {
        @Override
        public void run() {
            // Assume hitting the "Again" button when auto next question
            if (mEase1Layout.isEnabled() && mEase1Layout.getVisibility() == View.VISIBLE) {
                mEase1Layout.performClick();
            }
        }
    };

    protected Runnable mShowAnswerTask = new Runnable() {
        @Override
        public void run() {
            if (mFlipCardLayout.isEnabled() && mFlipCardLayout.getVisibility() == View.VISIBLE) {
                mFlipCardLayout.performClick();
            }
        }
    };



    class ReadTextListener implements ReadText.ReadTextListener {
        public void onDone() {
            if (!mUseTimer) {
                return;
            }
            if (ReadText.getmQuestionAnswer() == SoundSide.QUESTION) {
                long delay = mWaitAnswerSecond * 1000;
                if (delay > 0) {
                    mTimeoutHandler.postDelayed(mShowAnswerTask, delay);
                }
            } else if (ReadText.getmQuestionAnswer() == SoundSide.ANSWER) {
                long delay = mWaitQuestionSecond * 1000;
                if (delay > 0) {
                    mTimeoutHandler.postDelayed(mShowQuestionTask, delay);
                }
            }
        }


        @Override
        public void ttsInitialized() {
            AbstractFlashcardViewer.this.ttsInitialized();
        }
    }


    protected void initTimer() {
        final TypedValue typedValue = new TypedValue();
        mShowTimer = mCurrentCard.showTimer();
        if (mShowTimer && mCardTimer.getVisibility() == View.INVISIBLE) {
            mCardTimer.setVisibility(View.VISIBLE);
        } else if (!mShowTimer && mCardTimer.getVisibility() != View.INVISIBLE) {
            mCardTimer.setVisibility(View.INVISIBLE);
        }
        // Set normal timer color
        getTheme().resolveAttribute(android.R.attr.textColor, typedValue, true);
        mCardTimer.setTextColor(typedValue.data);

        mCardTimer.setBase(SystemClock.elapsedRealtime());
        mCardTimer.start();

        // Stop and highlight the timer if it reaches the time limit.
        getTheme().resolveAttribute(R.attr.maxTimerColor, typedValue, true);
        final int limit = mCurrentCard.timeLimit();
        mCardTimer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long elapsed = SystemClock.elapsedRealtime() - chronometer.getBase();
                if (elapsed >= limit) {
                    chronometer.setTextColor(typedValue.data);
                    chronometer.stop();
                }
            }
        });
    }


    protected void displayCardQuestion() {
        displayCardQuestion(false);
        // js api initialisation / reset
        jsApiInit();

    }


    protected void displayCardQuestion(boolean reload) {
        Timber.d("displayCardQuestion()");
        sDisplayAnswer = false;
        this.mMissingImageHandler.onCardSideChange();

        setInterface();

        String question;
        String displayString = "";
        if (mCurrentCard.isEmpty()) {
            displayString = getResources().getString(R.string.empty_card_warning);
        } else {
            question = mCurrentCard.q(reload);
            question = getCol().getMedia().escapeImages(question);
            question = typeAnsQuestionFilter(question);

            Timber.v("question: '%s'", question);
            // Show text entry based on if the user wants to write the answer
            if (typeAnswer()) {
                mAnswerField.setVisibility(View.VISIBLE);
            } else {
                mAnswerField.setVisibility(View.GONE);
            }

            displayString = CardAppearance.enrichWithQADiv(question, false);

//            if (mSpeakText) {
//             ReadText.setLanguageInformation(Model.getModel(DeckManager.getMainDeck(),
//             mCurrentCard.getCardModelId(), false).getId(), mCurrentCard.getCardModelId());
//            }
        }
        Timber.d("displayCardQuestion:" + displayString);

        updateCard(displayString);
        hideEaseButtons();

        // Check if it should use the general 'Timeout settings' or the ones specific to this deck
        if (mOptUseGeneralTimerSettings) {
            mUseTimer = mPrefUseTimer;
            mWaitAnswerSecond = mPrefWaitAnswerSecond;
            mWaitQuestionSecond = mPrefWaitQuestionSecond;
        } else {
            mUseTimer = mOptUseTimer;
            mWaitAnswerSecond = mOptWaitAnswerSecond;
            mWaitQuestionSecond = mOptWaitQuestionSecond;
        }

        // If the user wants to show the answer automatically
        if (mUseTimer) {
            long delay = mWaitAnswerSecond * 1000 + mUseTimerDynamicMS;
            if (delay > 0) {
                mTimeoutHandler.removeCallbacks(mShowAnswerTask);
                if (!mSpeakText) {
                    mTimeoutHandler.postDelayed(mShowAnswerTask, delay);
                }
            }
        }

        Timber.i("AbstractFlashcardViewer:: Question successfully shown for card id %d", mCurrentCard.getId());
    }


    /**
     * Clean up the correct answer text, so it can be used for the comparison with the typed text
     *
     * @param answer The content of the field the text typed by the user is compared to.
     * @return The correct answer text, with actual HTML and media references removed, and HTML entities unescaped.
     */
    protected String cleanCorrectAnswer(String answer) {
        return TypedAnswer.cleanCorrectAnswer(answer);
    }


    /**
     * Clean up the typed answer text, so it can be used for the comparison with the correct answer
     *
     * @param answer The answer text typed by the user.
     * @return The typed answer text, cleaned up.
     */
    protected String cleanTypedAnswer(String answer) {
        if (answer == null || "".equals(answer)) {
            return "";
        }
        return Utils.nfcNormalized(answer.trim());
    }


    protected void displayCardAnswer() {
        Timber.d("displayCardAnswer()");
        mMissingImageHandler.onCardSideChange();

        // prevent answering (by e.g. gestures) before card is loaded
        if (mCurrentCard == null) {
            return;
        }

        // Explicitly hide the soft keyboard. It *should* be hiding itself automatically,
        // but sometimes failed to do so (e.g. if an OnKeyListener is attached).
        if (typeAnswer()) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(mAnswerField.getWindowToken(), 0);
        }

        sDisplayAnswer = true;

        String answer = mCurrentCard.a();

        mSoundPlayer.stopSounds();
        stopOnlineSpeaking();
        answer = getCol().getMedia().escapeImages(answer);

        mAnswerField.setVisibility(View.GONE);
        // Clean up the user answer and the correct answer
        String userAnswer;
        if (mUseInputTag) {
            userAnswer = cleanTypedAnswer(mTypeInput);
        } else {
            userAnswer = cleanTypedAnswer(mAnswerField.getText().toString());
        }
        String correctAnswer = cleanCorrectAnswer(mTypeCorrect);
        Timber.d("correct answer = %s", correctAnswer);
        Timber.d("user answer = %s", userAnswer);

        answer = typeAnsAnswerFilter(answer, userAnswer, correctAnswer);

        mIsSelecting = false;
        updateCard(CardAppearance.enrichWithQADiv(answer, true));
        displayAnswerBottomBar();
        // If the user wants to show the next question automatically
        if (mUseTimer) {
            long delay = mWaitQuestionSecond * 1000 + mUseTimerDynamicMS;
            if (delay > 0) {
                mTimeoutHandler.removeCallbacks(mShowQuestionTask);
                if (!mSpeakText) {
                    mTimeoutHandler.postDelayed(mShowQuestionTask, delay);
                }
            }
        }
    }


    /**
     * Scroll the currently shown flashcard vertically
     *
     * @param dy amount to be scrolled
     */
    public void scrollCurrentCardBy(int dy) {
        processCardAction(cardWebView -> {
            if (dy != 0 && cardWebView.canScrollVertically(dy)) {
                cardWebView.scrollBy(0, dy);
            }
        });
    }


    /**
     * Tap onto the currently shown flashcard at position x and y
     *
     * @param x horizontal position of the event
     * @param y vertical position of the event
     */
    public void tapOnCurrentCard(int x, int y) {
        // assemble suitable ACTION_DOWN and ACTION_UP events and forward them to the card's handler
        MotionEvent eDown = MotionEvent.obtain(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, x, y,
                1, 1, 0, 1, 1, 0, 0);
        processCardAction(cardWebView -> cardWebView.dispatchTouchEvent(eDown));

        MotionEvent eUp = MotionEvent.obtain(eDown.getDownTime(),
                SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y,
                1, 1, 0, 1, 1, 0, 0);
        processCardAction(cardWebView -> cardWebView.dispatchTouchEvent(eUp));

    }


    /**
     * getAnswerFormat returns the answer part of this card's template as entered by user, without any parsing
     */
    public String getAnswerFormat() {
        JSONObject model = mCurrentCard.model();
        JSONObject template;
        if (model.getInt("type") == Consts.MODEL_STD) {
            template = model.getJSONArray("tmpls").getJSONObject(mCurrentCard.getOrd());
        } else {
            template = model.getJSONArray("tmpls").getJSONObject(0);
        }

        return template.getString("afmt");
    }


    private void addAnswerSounds(String answer) {
        // don't add answer sounds multiple times, such as when reshowing card after exiting editor
        // additionally, this condition reduces computation time
        if (!mAnswerSoundsAdded) {
            String answerSoundSource = removeFrontSideAudio(answer);
            mSoundPlayer.addSounds(mBaseUrl, answerSoundSource, SoundSide.ANSWER);
            mAnswerSoundsAdded = true;
        }
    }


    protected boolean isInNightMode() {
        return mCardAppearance.isNightMode();
    }


    private String mCacheContent = "";


    protected boolean statusBarColorHasChanged() {
        return true;
    }


    private void updateCard(final String newContent) {
        Timber.d("updateCard()");
        mCacheContent = newContent;
        mUseTimerDynamicMS = 0;

        // Add CSS for font color and font size
        if (mCurrentCard == null) {
            processCardAction(cardWebView -> cardWebView.getSettings().setDefaultFontSize(calculateDynamicFontSize(newContent)));
        }

        if (sDisplayAnswer) {
            addAnswerSounds(newContent);
        } else {
            // reset sounds each time first side of card is displayed, which may happen repeatedly without ever
            // leaving the card (such as when edited)
            mSoundPlayer.resetSounds();
            mAnswerSoundsAdded = false;
            mSoundPlayer.addSounds(mBaseUrl, newContent, SoundSide.QUESTION);
            if (mUseTimer && !mAnswerSoundsAdded && getConfigForCurrentCard().optBoolean("autoplay", false)) {
                addAnswerSounds(mCurrentCard.a());
            }
        }

        String content = Sound.expandSounds(mBaseUrl, newContent);

        content = CardAppearance.fixBoldStyle(content);

        Timber.v("content card = \n %s", content);

        String style = mCardAppearance.getStyle();
        Timber.v("::style:: / %s", style);

        // CSS class for card-specific styling
        String cardClass = mCardAppearance.getCardClass(mCurrentCard.getOrd() + 1, Themes.getCurrentTheme(this));
        String script = "";
        if (isInNightMode()) {
            if (!mCardAppearance.hasUserDefinedNightMode(mCurrentCard)) {
                content = HtmlColors.invertColors(content);
            }
        }
        if (Template.textContainsMathjax(content)) {
            cardClass += " mathjax-needs-to-render";
            script = " \n <script src=\"file:///android_asset/mathjax/conf.js\"> </script> \n" +
                    " <script src=\"file:///android_asset/mathjax/tex-chtml.js\"> </script> \n ";
        }


        content = CardAppearance.convertSmpToHtmlEntity(content);
        SharedPreferences prefs = AnkiDroidApp.getSharedPrefs(this);
        String localViewSettingStr = prefs.getString(Consts.KEY_LOCAL_LAYOUT_CONFIG, "");

        boolean dark = false;
        if (AnkiDroidApp.getSharedPrefs(this).getBoolean("invertedColors", false)) {
            int theme = Integer.parseInt(prefs.getString("nightTheme", "0"));
            dark = theme == THEME_NIGHT_DARK || theme == THEME_NIGHT_BLACK;
        }
        if (mCurrentCSS.isEmpty() || mCurrentCSSModelID != mCurrentCard.model().getLong("id")) {
            mCurrentCSSModelID = mCurrentCard.model().getLong("id");
            Timber.i("find new model id css %s", mCurrentCSS);
            if (localViewSettingStr != null && !localViewSettingStr.isEmpty()) {
                try {
                    JSONObject viewSetting = new JSONObject(localViewSettingStr);
                    JSONObject currentModelSetting = viewSetting.getJSONObject(String.valueOf(mCurrentCSSModelID));
                    if (currentModelSetting != null) {
                        mCurrentCSS = convertJson2Css(currentModelSetting, false);
                    } else {
                        mCurrentCSS = "";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mCurrentCSS = "";
                }
            }
        }
        Timber.i("now theme is dark:%s", dark);
        if (!dark) {
            if (!mCurrentCSS.isEmpty()) {
                Matcher bgMatcher = Pattern.compile("background-color:(.+?);").matcher(mCurrentCSS);
                Window window = getWindow();
                if (bgMatcher.find()) {
                    String fld1 = bgMatcher.group(1).trim();
                    if (shouldChangeToolbarBgLikeCss2()) {
                        findViewById(R.id.toolbar).setBackgroundColor(Color.parseColor(fld1));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            window.setStatusBarColor(Color.parseColor(fld1));
                        }
                    }
                    mCardWebView.setBackgroundColor(Color.parseColor(fld1));
                    findViewById(R.id.bottom_area_layout).setBackgroundColor(Color.parseColor(fld1));
                } else {
                    if (shouldChangeToolbarBgLikeCss2()) {
                        int[] attrs = new int[] {
                                R.attr.reviewStatusBarColor,
                        };
                        TypedArray ta = obtainStyledAttributes(attrs);
                        findViewById(R.id.toolbar).setBackground(ta.getDrawable(0));
                        ta.recycle();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            window.setStatusBarColor(Themes.getColorFromAttr(this, getStatusBarColorAttr()));
                        }
                    }
                    mCardWebView.setBackgroundColor(Color.WHITE);
                    findViewById(R.id.bottom_area_layout).setBackgroundColor(Color.TRANSPARENT);
                }
            } else {
                if (shouldChangeToolbarBgLikeCss2()) {
                    int[] attrs = new int[] {
                            R.attr.reviewStatusBarColor,
                    };

                    TypedArray ta = obtainStyledAttributes(attrs);
                    findViewById(R.id.toolbar).setBackground(ta.getDrawable(0));
                    ta.recycle();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Window window = getWindow();
                        window.setStatusBarColor(Themes.getColorFromAttr(this, getStatusBarColorAttr()));
                    }
                }
                mCardWebView.setBackgroundColor(Color.WHITE);
                findViewById(R.id.bottom_area_layout).setBackgroundColor(Color.TRANSPARENT);
            }
        }
//        if (mCurrentCSS.isEmpty()) {//保存的配置文件没有，则直接找默认的css
//            mCurrentCSS = mCurrentCard.css().replace("<style>", "").replace("</style>", "");
//        }

        mCardContent = mCardTemplate.replace("::content::", content)
                .replace("::style::", style).replace("::class::", cardClass).replace("::style2::", mCurrentCSS).replace("::script::", script).replace("::class2::", sDisplayAnswer ? "ck-back" : "ck-front");
//        Timber.d("base url = %s", mBaseUrl);
//        Timber.v("::content:: / %s", content);
//        Timber.v("::style2:: / %s", mCurrentCSS);
        if (mCardContent.contains("<!--助记-->")) {
            try {
                Cursor cur = CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance()).getDb()
                        .getDatabase()
                        .query(
                                "SELECT id,cid,content,mod FROM remarks WHERE cid = " + mCurrentCard.getId(), null);
                if (cur.moveToNext()) {
                    Timber.i("替换助记>>>>>>>");
                    mCardContent = mCardContent.replace("<!--助记-->", cur.getString(2));
                }
            } finally {

            }
        } else {
            Timber.i("没有助记");
        }


        if (AnkiDroidApp.getSharedPrefs(this).getBoolean("html_javascript_debugging", false)) {
            try {
                try (FileOutputStream f = new FileOutputStream(new File(CollectionHelper.getCurrentAnkiDroidDirectory(this),
                        "card.html"))) {
                    f.write(mCardContent.getBytes());
                }
            } catch (IOException e) {
                Timber.d(e, "failed to save card");
            }
        }
        fillFlashcard();

        if (!mConfigurationChanged) {
            playSoundsVIP(false);
        }
    }


    protected boolean shouldChangeToolbarBgLikeCss2() {
        return false;
    }


    protected boolean mRefreshVipStateOnResume = true;
    protected boolean mRefreshVoiceInfoStateOnResume = true;
    protected boolean mReInitBDVoice = false;
    protected boolean mTurnToVipHtml = false;
    private boolean hasRecord = false;
    private boolean originVipState = false;


    protected String mVipUrl;
    protected String mBuyOnlineEngineUrl;
    protected int mFreeOnlineEngineCount = -10086;
    protected boolean mVip = false;
    protected int mVipDay;
    protected int mFreeVipCount = 1;

    protected String mVipExpireAt;
    private CustomStyleDialog mBeVipDialog;
    private String mTtsEngine = "";


    @Override
    protected void onResume() {
        Timber.d("onResume()");
        super.onResume();
        activityStop = false;
        resumeTimer();
        // Set the context for the Sound manager
        mSoundPlayer.setContext(new WeakReference<Activity>(this));
        // Reset the activity title
        setTitle();
        updateActionBar();
        if (mTtsInitialized) {
            ReadText.releaseTts();
        }
        mTtsEngine = ReadText.initializeTts(this, mFreeVipCount < VIP_FREE_COUNT, false, new ReadTextListener());
        invalidateOptionsMenu();
        Timber.e("using default tts engine:%s", mTtsEngine);
        if (mRefreshVipStateOnResume) {

            mRefreshVipStateOnResume = false;
            mVip = AnkiDroidApp.getSharedPrefs(this).getBoolean(Consts.KEY_IS_VIP, false);
            mVipUrl = AnkiDroidApp.getSharedPrefs(this).getString(Consts.KEY_VIP_URL, "");
            getAccount().getToken(this, new MyAccount.TokenCallback() {
                @Override
                public void onSuccess(String token) {
                    //获取vip状态
                    OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "users/vipInfo", token, "", new OKHttpUtil.MyCallBack() {
                        @Override
                        public void onFailure(Call call, IOException e) {
//                            mVip = false;//断网不改变vip状态
                        }


                        @Override
                        public void onResponse(Call call, String token, Object arg1, Response response) {
                            if (response.isSuccessful()) {
//                            Timber.i("init vip info successfully!:%s", response.body());
                                try {
                                    final org.json.JSONObject object = new org.json.JSONObject(response.body().string());
                                    final org.json.JSONObject item = object.getJSONObject("data");
                                    mVipUrl = item.getString("vip_url");
                                    Timber.i("get vip url ：%s", mVipUrl);
                                    mVip = item.getBoolean("is_vip");
                                    if (!hasRecord) {
                                        originVipState = mVip;
                                        if (mVip) {
                                            ReadText.releaseTts();
                                            ReadText.initializeTts(AbstractFlashcardViewer.this, true, false, new ReadTextListener());
                                        }
                                    } else {
                                        if (mVip && !originVipState) {
                                            runOnUiThread(() -> {
                                                ReadText.releaseTts();
                                                ReadText.initializeTts(AbstractFlashcardViewer.this, true, false, new ReadTextListener());
                                                if (mBeVipDialog != null && mBeVipDialog.isShowing()) {
                                                    mBeVipDialog.dismiss();
                                                }
                                                Toast.makeText(AbstractFlashcardViewer.this, "你已成为超级学霸", Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    }
                                    if (!mVip && mTurnToVipHtml) {
                                        mTurnToVipHtml = false;
                                        WebViewActivity.openUrlInApp(AbstractFlashcardViewer.this, String.format(mVipUrl, token, BuildConfig.VERSION_NAME), token, BE_VIP);
                                    }
                                    mVipDay = item.getInt("vip_day");
                                    mVipExpireAt = item.getString("vip_expire_at");
                                    AnkiDroidApp.getSharedPrefs(AbstractFlashcardViewer.this).edit()
                                            .putBoolean(Consts.KEY_IS_VIP, mVip)
                                            .putString(Consts.KEY_VIP_URL, mVipUrl)
                                            .putString(Consts.KEY_VIP_EXPIRED, mVipExpireAt).apply();
                                    hasRecord = true;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Timber.e("init vip info failed, error code %d", response.code());
                            }


                        }
                    });
                    if (mRefreshVoiceInfoStateOnResume) {
                        mRefreshVoiceInfoStateOnResume = false;
                        updateOnlineVoiceInfo(token);
                    }
                }


                @Override
                public void onFail(String message) {
                    OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "users/vipInfo", "", "", new OKHttpUtil.MyCallBack() {
                        @Override
                        public void onFailure(Call call, IOException e) {

                        }


                        @Override
                        public void onResponse(Call call, String token, Object arg1, Response response) throws IOException {
                            if (response.isSuccessful()) {
//                                Timber.i("init vip info successfully!:%s", response.body());
                                try {
                                    final org.json.JSONObject object = new org.json.JSONObject(response.body().string());
                                    final org.json.JSONObject item = object.getJSONObject("data");
                                    mVipUrl = item.getString("vip_url");
                                    mVip = item.getBoolean("is_vip");
                                    mVipDay = item.getInt("vip_day");
                                    mVipExpireAt = item.getString("vip_expire_at");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Timber.e("init vip info failed, error code %d", response.code());

                            }

                        }
                    });
                    mVip = false;

                }
            });
        }
        if (mRefreshVoiceInfoStateOnResume) {
            mRefreshVoiceInfoStateOnResume = false;
            getAccount().getToken(this, new MyAccount.TokenCallback() {
                @Override
                public void onSuccess(String token) {
                    //获取在线语音引擎可用次数
                    updateOnlineVoiceInfo(token);
                }


                @Override
                public void onFail(String message) {

                }
            });
        }

        if (AnkiDroidApp.getSharedPrefs(this).getBoolean(KEY_SELECT_ONLINE_SPEAK_ENGINE, false)) {
            if (synthesizer == null) {
                initBDTTs();
            }

        }
        if (!pulledConfigFromService) {
            getAccount().getToken(this, new MyAccount.TokenCallback() {
                @Override
                public void onSuccess(String token) {
                    OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "configs/global", token, false, getServiceConfigCallback);
                }


                @Override
                public void onFail(String message) {

                }
            });

        }
//        selectNavigationItem(R.id.nav_browser);
    }


    static final int VIP_FREE_COUNT = 16;


    //1、使用谷歌引擎，走原始方法，即选择country language available
    //2、使用收费引擎，走兼容方法，即选择language available
    //3、使用收费引擎，未开启vip时有次数限制
    //测试：
    // 1、谷歌引擎是否能在未开启vip时一直正常使用
    // 2、收费引擎是否能正常触发次数限制


    private boolean useVipSpeechEngine() {
//        return mVip || mFreeVipCount <= VIP_FREE_COUNT;
        return mTtsEngine != null && !mTtsEngine.equalsIgnoreCase("com.google.android.tts");
    }


    //1、判断是否是点击按钮，如果是则播放tts
    //2、如果开启了自动朗读，则判断是否有内置语音，有内置语音，且没有开启优先tts，则读内置语音；否则使用tts朗读
    //3、如果开启了优先tts， 则判断是否有开启了自动朗读tts，有则自动朗读，否则不做动作（默认开启优先tts时会同时开启自动朗读）
    //3、如果开启了自动朗读，
    protected void playSoundsVIP(boolean doAudioReplay) {
        if (mCurrentCard == null) {
            return;
        }
        boolean replayQuestion = getConfigForCurrentCard().optBoolean("replayq", true);
        boolean autoPlay = getConfigForCurrentCard().optBoolean("autoplay", false);//自动播放，always true

        boolean autoPlayTts = AnkiDroidApp.getSharedPrefs(this).getBoolean(Consts.KEY_AUTO_PLAY_TTS, false);//自动播放tts，mSpeakText：开启tts优先

        Timber.w("play sound  :无内置语音:" + (!(sDisplayAnswer && mSoundPlayer.hasAnswer()) && !(!sDisplayAnswer && mSoundPlayer.hasQuestion())) + ",tts优先：" + mSpeakText + ",自动播放tts：" + autoPlayTts);
        if (autoPlay || doAudioReplay) {//自动播放或点击了播放按钮
            // Use TTS if TTS preference enabled and no other sound source
//            boolean useTTS =doAudioReplay||(mSpeakText && !(sDisplayAnswer && mSoundPlayer.hasAnswer()) && !(!sDisplayAnswer && mSoundPlayer.hasQuestion())) ;
            boolean useTTS = doAudioReplay || (autoPlayTts && ((!(sDisplayAnswer && mSoundPlayer.hasAnswer()) && !(!sDisplayAnswer && mSoundPlayer.hasQuestion())) || mSpeakText));
            //点击按钮或(自动朗读tts+（优先tts或没有内置语音）），则使用tts
            // We need to play the sounds from the proper side of the card
            if (!useTTS) { // Text to speech not in effect here
                if (replayQuestion && sDisplayAnswer) {
                    // only when all of the above are true will question be played with answer, to match desktop
                    mSoundPlayer.playSounds(SoundSide.QUESTION_AND_ANSWER);
                } else if (sDisplayAnswer) {
                    mSoundPlayer.playSounds(SoundSide.ANSWER);
                    if (mUseTimer) {
                        mUseTimerDynamicMS = mSoundPlayer.getSoundsLength(SoundSide.ANSWER);
                    }
                } else { // question is displayed
                    mSoundPlayer.playSounds(SoundSide.QUESTION);
                    // If the user wants to show the answer automatically
                    if (mUseTimer) {
                        mUseTimerDynamicMS = mSoundPlayer.getSoundsLength(SoundSide.QUESTION_AND_ANSWER);
                    }
                }
            } else {
                // Text to speech is in effect here
                // If the question is displayed or if the question should be replayed, read the question

//                Timber.i("play sound tts state:" + mVip + "," + useVipSpeechEngine() + "," + (mFreeVipCount >= VIP_FREE_COUNT));
                if (!mVip && useVipSpeechEngine() && mFreeVipCount >= VIP_FREE_COUNT) {//非vip使用收费引擎且超出免费次数
                    mBeVipDialog = new CustomStyleDialog.Builder(this)
                            .setCustomLayout(R.layout.dialog_common_custom_next)
                            .setTitle("功能次数已使用完！")
                            .centerTitle()
                            .setMessage("普通用户，可免费朗读15次，成为超级学霸，不限次数！")
                            .setPositiveButton("前往了解", (dialog, which) -> {
                                dialog.dismiss();
                                AnkiDroidApp.getSharedPrefs(this).edit().putBoolean("tts", false).apply();
                                AnkiDroidApp.getSharedPrefs(this).edit().putBoolean(Consts.KEY_AUTO_PLAY_TTS, false).apply();
                                openVipUrl(mVipUrl);
                            })

                            .create();
                    mBeVipDialog.setOnDismissListener(dialog -> {
                        AnkiDroidApp.getSharedPrefs(AbstractFlashcardViewer.this).edit().putBoolean("tts", false).apply();
                        AnkiDroidApp.getSharedPrefs(AbstractFlashcardViewer.this).edit().putBoolean(Consts.KEY_AUTO_PLAY_TTS, false).apply();
                    });
                    mBeVipDialog.show();
                } else {
                    mFreeVipCount++;
                    preferences.edit().putInt("speak_count", mFreeVipCount).apply();
                    if (mTtsInitialized) {
                        if (!sDisplayAnswer || (doAudioReplay && replayQuestion && !AnkiDroidApp.getSharedPrefs(this).getBoolean(KEY_SELECT_ONLINE_SPEAK_ENGINE, false))) {
                            Timber.i("ready to read question");
                            readCardText(mCurrentCard, SoundSide.QUESTION);
                        }
                        if (sDisplayAnswer) {
                            Timber.i("ready to read answer");
                            readCardText(mCurrentCard, SoundSide.ANSWER);
                        }
                    } else {
                        mReplayOnTtsInit = true;
                    }
                }
            }
        }


    }


    private Handler speakingHandler = new Handler();
    private Handler switchSpeakEngineHandler = new Handler();
    private Runnable speakingRunnable;
    private int speakingIndex = 0;

    private ImageButton mVipSpeakMenuItem;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem speak = menu.findItem(R.id.action_speak);

        if (speak != null) {
            speakingHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    if (msg.what == 10086) {
                        speakingHandler.removeCallbacksAndMessages(null);
                        ((ImageButton) speak.getActionView()).setImageResource(R.mipmap.nav_bar_aloud_normal);
                    }
                }
            };
            speak.setVisible(AnkiDroidApp.getSharedPrefs(this).getBoolean(KEY_SHOW_TTS_ICON, true));
            if (mVipSpeakMenuItem == null) {
                mVipSpeakMenuItem = new ImageButton(this);
            }
            mVipSpeakMenuItem.setBackground(null);
            speak.setActionView(mVipSpeakMenuItem);
            mVipSpeakMenuItem.setImageResource(R.mipmap.nav_bar_aloud_normal);
            speak.getActionView().setOnLongClickListener(v -> {
                SpeakSettingActivity.OpenSpeakSetting(mCurrentCard.getId(), mCurrentCard.getDid(), AbstractFlashcardViewer.this);
                return false;
            });
            speak.getActionView().setOnClickListener(v -> {
                Timber.i("speak icon pressed,ins speaking:" + mOnlineSpeaking + ",object:" + synthesizer);
                if (ReadText.isSpeaking()/*||(synthesizer!=null&&synthesizer.isInitied()&&synthesizer.)*/) {
                    ReadText.stopTts();
                } else if (mOnlineSpeaking/* && synthesizer != null*/) {
                    stopOnlineSpeaking();
                    mSoundPlayer.stopSounds();
                    mOnlineSpeaking = false;
                } else {
                    playSoundsVIP(true);
                }
            });
            speakingRunnable = new Runnable() {
                @Override
                public void run() {
                    speakingIndex++;
//                Timber.i("running speaking!:"+speakingIndex );
                    if (isSpeaking()) {
//                    Timber.i("running is speaking! " );
                        ((ImageButton) speak.getActionView()).setImageResource(speakingIndex % 2 == 0 ? R.mipmap.nav_bar_aloud_one : R.mipmap.nav_bar_aloud_two);
                        speakingHandler.postDelayed(this, 200);
                    } else {
//                    Timber.i("running is speaking end! " );
                        speakingHandler.removeCallbacksAndMessages(null);
                        ((ImageButton) speak.getActionView()).setImageResource(R.mipmap.nav_bar_aloud_normal);
                    }

                }
            };
        }

        return super.onCreateOptionsMenu(menu);
    }


    private boolean isSpeaking() {
        return ReadText.isSpeaking() || mOnlineSpeaking;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_speak_setting:
                if (mCurrentCard == null) {
                    Toast.makeText(this, "卡牌加载中", Toast.LENGTH_SHORT).show();
                } else {
                    SpeakSettingActivity.OpenSpeakSetting(mCurrentCard.getId(), mCurrentCard.getDid(), this);
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    // 主控制类，所有合成控制方法从这个类开始
    protected MySyntherizer synthesizer;
    protected String appId;

    protected String appKey;

    protected String secretKey;

    protected String sn; // 纯离线合成SDK授权码；离在线合成SDK没有此参数

    // TtsMode.MIX; 离在线融合，在线优先； TtsMode.ONLINE 纯在线； TtsMode.OFFLINE 纯离线合成，需要纯离线SDK
    protected TtsMode ttsMode = IOfflineResourceConst.DEFAULT_SDK_TTS_MODE;

    protected boolean isOnlineSDK = TtsMode.ONLINE.equals(IOfflineResourceConst.DEFAULT_SDK_TTS_MODE);

    // 离线发音选择，VOICE_FEMALE即为离线女声发音。
    // assets目录下bd_etts_common_speech_m15_mand_eng_high_am-mix_vXXXXXXX.dat为离线男声模型文件；
    // assets目录下bd_etts_common_speech_f7_mand_eng_high_am-mix_vXXXXX.dat为离线女声模型文件;
    // assets目录下bd_etts_common_speech_yyjw_mand_eng_high_am-mix_vXXXXX.dat 为度逍遥模型文件;
    // assets目录下bd_etts_common_speech_as_mand_eng_high_am_vXXXX.dat 为度丫丫模型文件;
    // 在线合成sdk下面的参数不生效
    protected String offlineVoice = OfflineResource.VOICE_MALE;
    protected Handler mainHandler;


    private void initBDTTs() {

        appId = Auth.getInstance(this).getAppId();
        appKey = Auth.getInstance(this).getAppKey();
        secretKey = Auth.getInstance(this).getSecretKey();
        sn = Auth.getInstance(this).getSn(); // 离线合成SDK必须有此参数；在线合成SDK没有此参数
        LoggerProxy.printable(true); // 日志打印在logcat中
        String tmpDir = FileUtil.createTmpDir(this);
        // 设置初始化参数
        // 此处可以改为 含有您业务逻辑的SpeechSynthesizerListener的实现类
        SpeechSynthesizerListener listener = new FileSaveListener(mainHandler, tmpDir);
        InitConfig config = getInitConfig(listener);
        synthesizer = new NonBlockSyntherizer(this, config, mainHandler); // 此处可以改为MySyntherizer 了解调用过程
        if (!isOnlineSDK) {
            Timber.i("SynthActivity" + "so version:" + SynthesizerTool.getEngineInfo());
        }
    }


    private boolean mOnlineSpeaking = false;


    protected void handle(Message msg) {
        int what = msg.what;
        switch (what) {
            case PRINT:
                Timber.i(String.valueOf(msg));
                break;
            case UI_PLAY_START:
                mOnlineSpeaking = true;
                consumeOnlineVoiceInfo(mCacheToken);
                break;
            case UI_PLAY_END:
                mOnlineSpeaking = false;
                speakingHandler.obtainMessage(10086).sendToTarget();
//                speakingHandler.removeCallbacksAndMessages(null);
                break;
            case UI_CHANGE_INPUT_TEXT_SELECTION:
            case UI_CHANGE_SYNTHES_TEXT_SELECTION:

                break;
            default:
                break;
        }
    }


    protected InitConfig getInitConfig(SpeechSynthesizerListener listener) {
        Map<String, String> params = getBDParams();
        // 添加你自己的参数
        InitConfig initConfig;
        // appId appKey secretKey 网站上您申请的应用获取。注意使用离线合成功能的话，需要应用中填写您app的包名。包名在build.gradle中获取。
        if (sn == null) {
            initConfig = new InitConfig(appId, appKey, secretKey, ttsMode, params, listener);
        } else {
            initConfig = new InitConfig(appId, appKey, secretKey, sn, ttsMode, params, listener);
        }
        // 如果您集成中出错，请将下面一段代码放在和demo中相同的位置，并复制InitConfig 和 AutoCheck到您的项目中
        // 上线时请删除AutoCheck的调用

        return initConfig;
    }


    /**
     * 合成的参数，可以初始化时填写，也可以在合成前设置。
     *
     * @return 合成参数Map
     */
    protected Map<String, String> getBDParams() {
        Map<String, String> params = new HashMap<>();
        // 以下参数均为选填
        // 设置在线发声音人： 0 普通女声（默认） 1 普通男声 3 情感男声<度逍遥> 4 情感儿童声<度丫丫>, 其它发音人见文档
        params.put(SpeechSynthesizer.PARAM_SPEAKER, "0");
        // 设置合成的音量，0-15 ，默认 5
        params.put(SpeechSynthesizer.PARAM_VOLUME, "15");
        // 设置合成的语速，0-15 ，默认 5
//        params.put(SpeechSynthesizer.PARAM_SPEED, String.valueOf(ReadText.getSpeechRate(mCurrentCard.getDid(), mCurrentCard.getOrd())*5));
        params.put(SpeechSynthesizer.PARAM_SPEED, "5");
        // 设置合成的语调，0-15 ，默认 5
        params.put(SpeechSynthesizer.PARAM_PITCH, "5");
        if (!isOnlineSDK) {
            // 在线SDK版本没有此参数。

            /*
            params.put(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);
            // 该参数设置为TtsMode.MIX生效。即纯在线模式不生效。
            // MIX_MODE_DEFAULT 默认 ，wifi状态下使用在线，非wifi离线。在线状态下，请求超时6s自动转离线
            // MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI wifi状态下使用在线，非wifi离线。在线状态下， 请求超时1.2s自动转离线
            // MIX_MODE_HIGH_SPEED_NETWORK ， 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
            // MIX_MODE_HIGH_SPEED_SYNTHESIZE, 2G 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
            // params.put(SpeechSynthesizer.PARAM_MIX_MODE_TIMEOUT, SpeechSynthesizer.PARAM_MIX_TIMEOUT_TWO_SECOND);
            // 离在线模式，强制在线优先。在线请求后超时2秒后，转为离线合成。
            */
            // 离线资源文件， 从assets目录中复制到临时目录，需要在initTTs方法前完成
            OfflineResource offlineResource = createOfflineResource(offlineVoice);
            // 声学模型文件路径 (离线引擎使用), 请确认下面两个文件存在
            params.put(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, offlineResource.getTextFilename());
            params.put(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, offlineResource.getModelFilename());
        }
        return params;
    }


    protected OfflineResource createOfflineResource(String voiceType) {
        OfflineResource offlineResource = null;
        try {
            offlineResource = new OfflineResource(this, voiceType);
        } catch (IOException e) {
            // IO 错误自行处理
            e.printStackTrace();
            Timber.i("【error】:copy files from assets failed." + e.getMessage());
        }
        return offlineResource;
    }


    /**
     * Reads the text (using TTS) for the given side of a card.
     *
     * @param card     The card to play TTS for
     * @param cardSide The side of the current card to play TTS for
     */
    private String mCacheToken = "";


    private boolean isNetworkAvailable(@NonNull Context context) {

        return Connection.isOnline();
    }


    private boolean mOfflineSpeakingForOnce = false;


    private void readCardText(final Card card, final SoundSide cardSide) {
        final String cardSideContent;
        if (!cardContentIsEmpty() && ((sDisplayAnswer && mFinalLoadAnswer.isEmpty()) || (!sDisplayAnswer && mFinalLoadQuestion.isEmpty()))) {
            mainHandler.postDelayed(() -> {
                showProgressBar();
                Toast.makeText(AbstractFlashcardViewer.this, "卡牌加载中，网络不佳", Toast.LENGTH_SHORT).show();
            }, 3000);

            nNeedSpeakFinalRenderContent = true;
            if (!fetchingRenderContent) {
                fetchWebViewRenderContent(mCardWebView);
            }
            return;
        }
        mainHandler.removeCallbacksAndMessages(null);
        hideProgressBar();
        cardSideContent = cardSide == SoundSide.QUESTION ? mFinalLoadQuestion : mFinalLoadAnswer;//在这里获取最后的内容
        Timber.i("finally i wanna say:%s,showing answer:%s", cardSideContent, sDisplayAnswer);
        //如果不是设置了离线模式且当前没网时，且非强制离线模式，则默认使用在线模式
        if (AnkiDroidApp.getSharedPrefs(this).getBoolean(KEY_SELECT_ONLINE_SPEAK_ENGINE, false) && !mOfflineSpeakingForOnce) {
            //使用在线语音引擎
            File target = new File(FileUtil.createTmpDir(this), card.getId() + "-" + cardSide + ".wav");
            Timber.i("target audio :%s", target.getAbsolutePath());
            if (target.exists()) {
                Timber.i("target audio is exists,play it now");
                mSoundPlayer.playSound(target.getAbsolutePath(), mp -> mOnlineSpeaking = false);
                mOnlineSpeaking = true;
                speakingHandler.postDelayed(speakingRunnable, 300);
                return;
            }
            if (!isNetworkAvailable(AbstractFlashcardViewer.this)) {
                CustomStyleDialog customStyleDialog = new CustomStyleDialog.Builder(AbstractFlashcardViewer.this)
                        .setCustomLayout(R.layout.dialog_common_custom_next)
                        .setTitle("在线朗读需联网！")
                        .centerTitle()
                        .setMessage("没有网络时可以点下方按钮切换成本地引擎")
                        .setPositiveButton("切换本地引擎", (dialog, which) -> {
                            dialog.dismiss();
                            AnkiDroidApp.getSharedPrefs(this).edit().putBoolean(KEY_SELECT_ONLINE_SPEAK_ENGINE, false).apply();
                        })
                        .create();
                customStyleDialog.show();
                return;
            }
            getAccount().getToken(this, new MyAccount.TokenCallback() {
                @Override
                public void onSuccess(String token) {
                    mCacheToken = token;
//                    if (mFreeOnlineEngineCount != -10086) {
//                        if (mFreeOnlineEngineCount > 0) {
//                            String voice=ReadText.getAzureLanguage(card.getDid(), card.getOrd(), cardSide);
//                            if (voice.isEmpty()) {
//                                //选择语言
//                                CustomStyleDialog d = new CustomStyleDialog.Builder(AbstractFlashcardViewer.this)
//                                        .setCustomLayout(R.layout.dialog_common_custom_next)
//                                        .setTitle("首次朗读，请设置语言")
//                                        .centerTitle()
//                                        .setMessage("设置语言后，朗读效果更优，还可以选择是否自动朗读。")
//                                        .setPositiveButton("前往设置", (dialog, which) -> {
//                                            dialog.dismiss();
//                                            SpeakSettingActivity.OpenSpeakSetting(card.getId(), card.getDid(), AbstractFlashcardViewer.this);
//                                        })  .create();
//                                d.show();
//                                return;
//                            }
                    List<Pair<String, String>> texts = splitAry(cardSideContent, 59, String.valueOf(card.getId()), cardSide);
                    Map<String, String> params = new HashMap<>();
                    params.put(SpeechSynthesizer.PARAM_SPEED, String.valueOf(ReadText.getSpeechRate(mCurrentCard.getDid(), mCurrentCard.getOrd()) * 5));
                    synthesizer.setParams(params);
                    int result = synthesizer.batchSpeak(texts);
                    checkResult(result, "speak");

//
//                        } else {
//                            CustomStyleDialog customStyleDialog = new CustomStyleDialog.Builder(AbstractFlashcardViewer.this)
//                                    .setCustomLayout(R.layout.dialog_common_custom_next)
//                                    .setTitle("在线朗读次数已用完")
//                                    .centerTitle()
//                                    .setMessage("请前往充值在线朗读次数，学霸用户可以切换离线引擎，不限朗读次数")
//                                    .setPositiveButton("前往充值", (dialog, which) -> {
//                                        dialog.dismiss();
//                                        WebViewActivity.openUrlInApp(AbstractFlashcardViewer.this, String.format(mBuyOnlineEngineUrl, token, BuildConfig.VERSION_NAME), token, REFRESH_VOICE_INFO);
//                                    }).setNegativeButton("使用离线引擎", (dialog, which) -> {
//                                        dialog.dismiss();
//                                        AnkiDroidApp.getSharedPrefs(AbstractFlashcardViewer.this).edit().putBoolean(KEY_SELECT_ONLINE_SPEAK_ENGINE, false).apply();
//                                    })
//
//                                    .create();
//
//                            customStyleDialog.show();
//                        }
//                    } else {
//                        updateOnlineVoiceInfo(token);
//                    }
                }


                @Override
                public void onFail(String message) {
                    Timber.e("need login while using online speak engine ");
                    Toast.makeText(AbstractFlashcardViewer.this, "当前未使用Anki记忆卡账号登录，无法使用在线语音引擎", Toast.LENGTH_SHORT).show();
                    Intent myAccount = new Intent(AbstractFlashcardViewer.this, MyAccount.class);
                    myAccount.putExtra("notLoggedIn", true);
                    startActivityForResultWithAnimation(myAccount, REFRESH_VOICE_INFO, ActivityTransitionAnimation.FADE);
                }
            });
            speakingHandler.postDelayed(speakingRunnable, 300);
            return;
        }
        mOfflineSpeakingForOnce = false;
        String clozeReplacement = this.getString(R.string.reviewer_tts_cloze_spoken_replacement);
        ReadText.readCardSide(cardSide, cardSideContent, card.getId(), getDeckIdForCard(card), card.getOrd(), clozeReplacement, true);
        speakingHandler.postDelayed(speakingRunnable, 300);
    }


    private static List<Pair<String, String>> splitAry(String ary, int subSize, String id, final SoundSide cardSide) {
        int count = ary.length() % subSize == 0 ? ary.length() / subSize : ary.length() / subSize + 1;
        List<Pair<String, String>> texts = new ArrayList<>();
        Timber.i("split ary:%s", ary);
        for (int i = 0; i < count; i++) {
            int start = Math.min(i * subSize, ary.length());
            int end = Math.min((i + 1) * subSize, ary.length());
            Timber.i("split ary，start：" + start + ",end:" + end);
            texts.add(new Pair<>(ary.substring(start, end), i + "-" + (count - 1) + "-" + id + "-" + cardSide));
        }
        return texts;
    }


    private void consumeOnlineVoiceInfo(String token) {
        RequestBody formBody = new FormBody.Builder()
                .build();

        OKHttpUtil.post(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "users/consumeVoice", formBody, token, "", new OKHttpUtil.MyCallBack() {
            @Override
            public void onFailure(Call call, IOException e) {

            }


            @Override
            public void onResponse(Call call, String token, Object arg1, Response response) {
                if (response.isSuccessful()) {
                    try {
                        final org.json.JSONObject object = new org.json.JSONObject(response.body().string());
                        final org.json.JSONObject item = object.getJSONObject("data");
                        mFreeOnlineEngineCount = item.getInt("total");
//                        mFreeOnlineEngineCount = 100;
                        preferences.edit().putInt(Consts.KEY_REST_ONLINE_SPEAK_COUNT, mFreeOnlineEngineCount).apply();
                        Timber.e("consume voice successfully, current total is %d", mFreeOnlineEngineCount);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Timber.e("consume voice failed, error code %d", response.code());
                }


            }
        });
    }


    private void updateOnlineVoiceInfo(String token) {
        OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "users/voiceInfo", token, "", new OKHttpUtil.MyCallBack() {
            @Override
            public void onFailure(Call call, IOException e) {

            }


            @Override
            public void onResponse(Call call, String token, Object arg1, Response response) {
                if (response.isSuccessful()) {
                    try {
                        final org.json.JSONObject object = new org.json.JSONObject(response.body().string());
                        final org.json.JSONObject item = object.getJSONObject("data");
                        Timber.e("init voice info success: %s", item.toString());
                        mBuyOnlineEngineUrl = item.getString("buy_url");
                        mFreeOnlineEngineCount = item.getInt("total");
//                        mFreeOnlineEngineCount = 100;
                        preferences.edit().putInt(Consts.KEY_REST_ONLINE_SPEAK_COUNT, mFreeOnlineEngineCount).apply();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Timber.e("init voice info failed, error code %d", response.code());
                }


            }
        });
    }


    private void checkResult(int result, String method) {
        if (result != 0) {
            Timber.e("error code :" + result + " method:" + method);
        }
    }


    private boolean nNeedSpeakFinalRenderContent;


    /**
     * Shows the dialogue for selecting TTS for the current card and cardside.
     */
//    protected void showSelectTtsDialogue() {
//        if (mTtsInitialized) {
//            if (!sDisplayAnswer) {
//                ReadText.selectTts(getTextForTts(mCurrentCard.q(true)), getDeckIdForCard(mCurrentCard), mCurrentCard.getOrd(),
//                        SoundSide.QUESTION, useVipSpeechEngine(), null);
//            } else {
//                ReadText.selectTts(getTextForTts(mCurrentCard.getPureAnswer()), getDeckIdForCard(mCurrentCard),
//                        mCurrentCard.getOrd(), SoundSide.ANSWER, useVipSpeechEngine(), null);
//            }
//        }
//    }
    private String getTextForTts(String text) {
        String clozeReplacement = this.getString(R.string.reviewer_tts_cloze_spoken_replacement);
        String clozeReplaced = text.replace(Template.CLOZE_DELETION_REPLACEMENT, clozeReplacement);
        return Utils.stripHTML(clozeReplaced);
    }


    /**
     * Returns the configuration for the current {@link Card}.
     *
     * @return The configuration for the current {@link Card}
     */
    private DeckConfig getConfigForCurrentCard() {
        return getCol().getDecks().confForDid(getDeckIdForCard(mCurrentCard));
    }


    /**
     * Returns the deck ID of the given {@link Card}.
     *
     * @param card The {@link Card} to get the deck ID
     * @return The deck ID of the {@link Card}
     */
    public static long getDeckIdForCard(final Card card) {
        // Try to get the configuration by the original deck ID (available in case of a cram deck),
        // else use the direct deck ID (in case of a 'normal' deck.
        return card.getODid() == 0 ? card.getDid() : card.getODid();
    }


    public void fillFlashcard() {
        Timber.d("fillFlashcard()");
        Timber.d("base url = %s", mBaseUrl);
        if (mCardContent == null) {
            Timber.w("fillFlashCard() called with no card content");
            return;
        }
        final String cardContent = mCardContent;
        processCardAction(cardWebView -> {
            loadContentIntoCard(cardWebView, cardContent);
            fetchWebViewRenderContent(cardWebView);
        });
        mGestureDetectorImpl.onFillFlashcard();
        if (mShowTimer && mCardTimer.getVisibility() == View.INVISIBLE) {
            switchTopBarVisibility(View.VISIBLE);
        }
        if (!sDisplayAnswer) {
            updateForNewCard();
        }
    }


    private String mFinalLoadAnswer = "";
    private String mFinalLoadQuestion = "";
//    private String mPreFinalLoadContent = "";



    public class ComJSInterface {
        @JavascriptInterface
        public void getSource(String content) {
//            Timber.i("load final :" + content);
            if (sDisplayAnswer) {
                mFinalLoadAnswer = content;
            } else {
                mFinalLoadQuestion = content;
            }

            if (!mFinalLoadQuestion.isEmpty() && mFinalLoadAnswer.contains(mFinalLoadQuestion)) {
                Timber.i("replace content:" + mFinalLoadQuestion.length());
//                mFinalLoadContent = mFinalLoadContent.replaceFirst(mPreFinalLoadContent,"");
                mFinalLoadAnswer = mFinalLoadAnswer.substring(mFinalLoadQuestion.length());
            }
//            Timber.i("current content :" + mFinalLoadAnswer);
//            Timber.i("pre content :" + mFinalLoadQuestion);
//            mFinalLoadQuestion = mFinalLoadAnswer;
            if (mCurrentCard != null && ((sDisplayAnswer && !mFinalLoadAnswer.isEmpty()) || (!sDisplayAnswer && !mFinalLoadQuestion.isEmpty())) && nNeedSpeakFinalRenderContent) {
                Timber.i("need speak final render content!");
                nNeedSpeakFinalRenderContent = false;
                fetchRenderHandler.removeCallbacksAndMessages(null);
                mainHandler.post(() -> readCardText(mCurrentCard, sDisplayAnswer ? SoundSide.ANSWER : SoundSide.QUESTION));
            }

        }
    }



    public ComJSInterface webViewRenderContentCallback;
    private Handler fetchRenderHandler;
    private boolean fetchingRenderContent;


    private void fetchWebViewRenderContent(WebView webView) {
        if (sDisplayAnswer) {
            mFinalLoadAnswer = "";
        }
        if (!sDisplayAnswer) {
            mFinalLoadQuestion = "";
        }
        if (fetchRenderHandler == null) {
            fetchRenderHandler = new Handler();
        }
        fetchingRenderContent = true;
        fetchRenderHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Timber.i("fetchWebViewRenderContent:\n question->" + mFinalLoadQuestion + "\n answer->" + mFinalLoadAnswer);
//                Timber.i("fetchWebViewRenderContent,with html content:" + mCacheContent);
//                Timber.i("fetchWebViewRenderContent,del html content:" + HtmlUtils.delHTMLTag(mCacheContent));
                if (!cardContentIsEmpty() && ((sDisplayAnswer && mFinalLoadAnswer.isEmpty()) || (!sDisplayAnswer && mFinalLoadQuestion.isEmpty()))) {
                    if (webView != null) {
                        webView.loadUrl("javascript:java_obj.getSource(document.documentElement.innerText);void(0);");
                    }
                    fetchRenderHandler.removeCallbacksAndMessages(null);
                    if (!activityStop) {
                        fetchRenderHandler.postDelayed(this, 100);
                    }
                } else {
                    fetchingRenderContent = false;
                }
            }
        }, 500);
    }


    private boolean cardContentIsEmpty() {
        return HtmlUtils.delHTMLTag(mCacheContent).isEmpty();
    }


    private boolean activityStop;


    @Override
    protected void onStop() {
        super.onStop();
        if (fetchRenderHandler != null) {
            fetchRenderHandler.removeCallbacksAndMessages(null);
        }

    }


    private void loadContentIntoCard(WebView card, String content) {
        Timber.i("show me the content before decrypt:%s", content);
        if (card != null) {
            CompatHelper.getCompat().setHTML5MediaAutoPlay(card.getSettings(), getConfigForCurrentCard().optBoolean("autoplay"));

            card.addJavascriptInterface(new ComJSInterface(), "java_obj");
            card.loadDataWithBaseURL(mBaseUrl + "__viewer__.html", content, "text/html", "utf-8", null);

        }
    }


    public static Card getEditorCard() {
        return sEditorCard;
    }


    /**
     * @return true if the AnkiDroid preference for writing answer is true and if the Anki Deck CardLayout specifies a
     * field to query
     */
    private boolean typeAnswer() {
        return !mUseInputTag && null != mTypeCorrect;
    }


    void unblockControls() {
        mControlBlocked = ControlBlock.UNBLOCKED;
        mCardFrame.setEnabled(true);
        mFlipCardLayout.setEnabled(true);

        switch (mCurrentEase) {
            case EASE_1:
                mEase1Layout.setClickable(true);
                mEase2Layout.setEnabled(true);
                mEase3Layout.setEnabled(true);
                mEase4Layout.setEnabled(true);
                break;

            case EASE_2:
                mEase1Layout.setEnabled(true);
                mEase2Layout.setClickable(true);
                mEase3Layout.setEnabled(true);
                mEase4Layout.setEnabled(true);
                break;

            case EASE_3:
                mEase1Layout.setEnabled(true);
                mEase2Layout.setEnabled(true);
                mEase3Layout.setClickable(true);
                mEase4Layout.setEnabled(true);
                break;

            case EASE_4:
                mEase1Layout.setEnabled(true);
                mEase2Layout.setEnabled(true);
                mEase3Layout.setEnabled(true);
                mEase4Layout.setClickable(true);
                break;

            default:
                mEase1Layout.setEnabled(true);
                mEase2Layout.setEnabled(true);
                mEase3Layout.setEnabled(true);
                mEase4Layout.setEnabled(true);
                break;
        }

        if (mPrefWhiteboard && mWhiteboard != null) {
            mWhiteboard.setEnabled(true);
        }

        if (typeAnswer()) {
            mAnswerField.setEnabled(true);
        }
        mTouchLayer.setVisibility(View.VISIBLE);
        mInAnswer = false;
        invalidateOptionsMenu();
    }


    @VisibleForTesting
    /** *
     * @param quick Whether we expect the control to come back quickly
     */
    protected void blockControls(boolean quick) {
        if (quick) {
            mControlBlocked = ControlBlock.QUICK;
        } else {
            mControlBlocked = ControlBlock.SLOW;
        }
        mCardFrame.setEnabled(false);
        mFlipCardLayout.setEnabled(false);
        mTouchLayer.setVisibility(View.INVISIBLE);
        mInAnswer = true;

        switch (mCurrentEase) {
            case EASE_1:
                mEase1Layout.setClickable(false);
                mEase2Layout.setEnabled(false);
                mEase3Layout.setEnabled(false);
                mEase4Layout.setEnabled(false);
                break;

            case EASE_2:
                mEase1Layout.setEnabled(false);
                mEase2Layout.setClickable(false);
                mEase3Layout.setEnabled(false);
                mEase4Layout.setEnabled(false);
                break;

            case EASE_3:
                mEase1Layout.setEnabled(false);
                mEase2Layout.setEnabled(false);
                mEase3Layout.setClickable(false);
                mEase4Layout.setEnabled(false);
                break;

            case EASE_4:
                mEase1Layout.setEnabled(false);
                mEase2Layout.setEnabled(false);
                mEase3Layout.setEnabled(false);
                mEase4Layout.setClickable(false);
                break;

            default:
                mEase1Layout.setEnabled(false);
                mEase2Layout.setEnabled(false);
                mEase3Layout.setEnabled(false);
                mEase4Layout.setEnabled(false);
                break;
        }

        if (mPrefWhiteboard && mWhiteboard != null) {
            mWhiteboard.setEnabled(false);
        }

        if (typeAnswer()) {
            mAnswerField.setEnabled(false);
        }
        invalidateOptionsMenu();
    }


    /**
     * Select Text in the webview and automatically sends the selected text to the clipboard. From
     * http://cosmez.blogspot.com/2010/04/webview-emulateshiftheld-on-android.html
     */
    @SuppressWarnings("deprecation") // Tracked separately in Github as #5024
    private void selectAndCopyText() {
        try {
            KeyEvent shiftPressEvent = new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0);
            processCardAction(shiftPressEvent::dispatch);
            shiftPressEvent.isShiftPressed();
            mIsSelecting = true;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }


    protected void executeCommandByController(int which) {
        if (!sDisplayAnswer && (which == 2 || which == 3 || which == 4 || which == 5)) {
            executeCommand(COMMAND_SHOW_ANSWER);
        } else {
//        Timber.i("show me the key code on CommandByController:"+sDisplayAnswer+","+which);
            if (which == 2 || which == 3 || which == 4 || which == 5) {
                int buttonNumber = getCol().getSched().answerButtons(mCurrentCard);
                if (buttonNumber < which - 1) {
//                    Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
                    return;
                }
                switch (which) {
                    case 2:
                        Toast.makeText(this, mEase1.getText(), Toast.LENGTH_SHORT).show();
                        break;
                    case 3:
                        Toast.makeText(this, mEase2.getText(), Toast.LENGTH_SHORT).show();
                        break;
                    case 4:
                        Toast.makeText(this, mEase3.getText(), Toast.LENGTH_SHORT).show();
                        break;
                    case 5:
                        Toast.makeText(this, mEase4.getText(), Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            executeCommand(which);
        }
    }

//    public static void ShowToast(Context context,String msg){
//        Toast toast=Toast.makeText(context,msg,Toast.LENGTH_SHORT);
//        toast.setText(msg);
//        toast.show();
//    }

//    private Toast mToast;
//
//    private void showTip(final String str) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if (mToast == null) {
//                    mToast = Toast.makeText(getApplicationContext(), "",
//                            Toast.LENGTH_LONG);
//
//                    //key parameter
//                    LinearLayout layout = (LinearLayout) mToast.getView();
//                    TextView tv = (TextView) layout.getChildAt(0);
//                    tv.setTextSize(25);
//                    //
//                }
//                //mToast.cancel();
//                mToast.setGravity(Gravity.CENTER, 0, 0);
//                mToast.setText(str);
//                mToast.show();
//            }
//        });
//    }


    public boolean executeCommand(@ViewerCommandDef int which) {
        Timber.i("show me the key code on executeCommand:" + sDisplayAnswer + "," + which + ",is block:" + isControlBlocked());
//        if (isControlBlocked() && which != COMMAND_EXIT) {
//            return false;
//        }
        switch (which) {
            case COMMAND_NOTHING:
                return true;
            case COMMAND_SHOW_ANSWER:
                if (sDisplayAnswer) {
                    return false;
                }
                displayCardAnswer();
                return true;
            case COMMAND_FLIP_OR_ANSWER_EASE1:
                flipOrAnswerCard(EASE_1);
                return true;
            case COMMAND_FLIP_OR_ANSWER_EASE2:
                flipOrAnswerCard(EASE_2);
                return true;
            case COMMAND_FLIP_OR_ANSWER_EASE3:
                flipOrAnswerCard(EASE_3);
                return true;
            case COMMAND_FLIP_OR_ANSWER_EASE4:
                flipOrAnswerCard(EASE_4);
                return true;
            case COMMAND_FLIP_OR_ANSWER_RECOMMENDED:
                flipOrAnswerCard(getRecommendedEase(false));
                return true;
            case COMMAND_FLIP_OR_ANSWER_BETTER_THAN_RECOMMENDED:
                flipOrAnswerCard(getRecommendedEase(true));
                return true;
            case COMMAND_EXIT:
                closeReviewer(RESULT_DEFAULT, false);
                return true;
            case COMMAND_UNDO:
                if (!isUndoAvailable()) {
                    return false;
                }
                undo();
                return true;
            case COMMAND_EDIT:
                editCard();
                return true;
            case COMMAND_TAG:
                showTagsDialog();
                return true;
            case COMMAND_MARK:
                onMark(mCurrentCard);
                return true;
            case COMMAND_LOOKUP:
                lookUpOrSelectText();
                return true;
            case COMMAND_BURY_CARD:
                dismiss(Collection.DismissType.BURY_CARD);
                return true;
            case COMMAND_BURY_NOTE:
                dismiss(Collection.DismissType.BURY_NOTE);
                return true;
            case COMMAND_SUSPEND_CARD:
                dismiss(Collection.DismissType.SUSPEND_CARD);
                return true;
            case COMMAND_SUSPEND_NOTE:
                dismiss(Collection.DismissType.SUSPEND_NOTE);
                return true;
            case COMMAND_DELETE:
                showDeleteNoteDialog();
                return true;
            case COMMAND_PLAY_MEDIA:
                playSoundsVIP(true);
                return true;
            case COMMAND_TOGGLE_FLAG_RED:
                toggleFlag(FLAG_RED);
                return true;
            case COMMAND_TOGGLE_FLAG_ORANGE:
                toggleFlag(FLAG_ORANGE);
                return true;
            case COMMAND_TOGGLE_FLAG_GREEN:
                toggleFlag(FLAG_GREEN);
                return true;
            case COMMAND_TOGGLE_FLAG_BLUE:
                toggleFlag(FLAG_BLUE);
                return true;
            case COMMAND_UNSET_FLAG:
                onFlag(mCurrentCard, FLAG_NONE);
                return true;
            case COMMAND_ANSWER_FIRST_BUTTON:
                return answerCardIfVisible(Consts.BUTTON_ONE);
            case COMMAND_ANSWER_SECOND_BUTTON:
                return answerCardIfVisible(Consts.BUTTON_TWO);
            case COMMAND_ANSWER_THIRD_BUTTON:
                return answerCardIfVisible(Consts.BUTTON_THREE);
            case COMMAND_ANSWER_FOURTH_BUTTON:
                return answerCardIfVisible(Consts.BUTTON_FOUR);
            case COMMAND_ANSWER_RECOMMENDED:
                return answerCardIfVisible(getRecommendedEase(false));
            case COMMAND_PAGE_UP:
                onPageUp();
                return true;
            case COMMAND_PAGE_DOWN:
                onPageDown();
                return true;
//            case COMMAND_SPEECH_PLAY:
//                playSoundsVIP(true);
//                return true;
            case COMMAND_FLIP_CARD:
                if (!sDisplayAnswer) {
                    displayCardAnswer();
                } else {
                    displayCardQuestion();
                }
                return true;

            default:
                Timber.w("Unknown command requested: %s", which);
                return false;
        }
    }


    /**
     * Displays a snackbar which does not obscure the answer buttons
     */
    protected void showSnackbar(String mainText, @StringRes int buttonText, OnClickListener onClickListener) {
        // BUG: Moving from full screen to non-full screen obscures the buttons

        Snackbar sb = UIUtils.getSnackbar(this, mainText, Snackbar.LENGTH_LONG, buttonText, onClickListener, mCardWebView, null);

        View easeButtons = findViewById(R.id.answer_options_layout);
        View previewButtons = findViewById(R.id.preview_buttons_layout);

        View upperView = previewButtons != null && previewButtons.getVisibility() != View.GONE ? previewButtons : easeButtons;

        // we need to check for View.GONE as setting the anchor does not seem to respect this property
        // (there's a gap even if the view is invisible)

        if (upperView != null && upperView.getVisibility() != View.GONE) {
            View sbView = sb.getView();
            CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) sbView.getLayoutParams();
            layoutParams.setAnchorId(upperView.getId());
            layoutParams.anchorGravity = Gravity.TOP;
            layoutParams.gravity = Gravity.TOP;
            sbView.setLayoutParams(layoutParams);
        }

        sb.show();
    }


    private void onPageUp() {
        //pageUp performs a half scroll, we want a full page
        processCardAction(cardWebView -> {
            cardWebView.pageUp(false);
            cardWebView.pageUp(false);
        });
    }


    private void onPageDown() {
        processCardAction(cardWebView -> {
            cardWebView.pageDown(false);
            cardWebView.pageDown(false);
        });
    }


    private void toggleFlag(@FlagDef int flag) {
        if (mCurrentCard.userFlag() == flag) {
            Timber.i("Toggle flag: unsetting flag");
            onFlag(mCurrentCard, FLAG_NONE);
        } else {
            Timber.i("Toggle flag: Setting flag to %d", flag);
            onFlag(mCurrentCard, flag);
        }
    }


    private boolean answerCardIfVisible(@Consts.BUTTON_TYPE int ease) {
        if (!sDisplayAnswer) {
            return false;
        }
        performClickWithVisualFeedback(ease);
        return true;
    }


    protected void performClickWithVisualFeedback(int ease) {
        // Delay could potentially be lower - testing with 20 left a visible "click"
        switch (ease) {
            case EASE_1:
                performClickWithVisualFeedback(mEase1Layout);
                break;
            case EASE_2:
                performClickWithVisualFeedback(mEase2Layout);
                break;
            case EASE_3:
                performClickWithVisualFeedback(mEase3Layout);
                break;
            case EASE_4:
                performClickWithVisualFeedback(mEase4Layout);
                break;
        }
    }


    private void performClickWithVisualFeedback(LinearLayout easeLayout) {
        easeLayout.requestFocus();
        easeLayout.postDelayed(easeLayout::performClick, 20);
    }


    @VisibleForTesting
    protected boolean isUndoAvailable() {
        return getCol().undoAvailable();
    }

    // ----------------------------------------------------------------------------
    // INNER CLASSES
    // ----------------------------------------------------------------------------



    /**
     * Provides a hook for calling "alert" from javascript. Useful for debugging your javascript.
     */
    public static final class AnkiDroidWebChromeClient extends WebChromeClient {
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Timber.i("AbstractFlashcardViewer:: onJsAlert: %s", message);
            result.confirm();
            return true;
        }
    }


    protected void closeReviewer(int result, boolean saveDeck) {
        // Stop the mic recording if still pending
        if (mMicToolBar != null) {
            mMicToolBar.notifyStopRecord();
        }
        // Remove the temporary audio file
        if (mTempAudioPath != null) {
            File tempAudioPathToDelete = new File(mTempAudioPath);
            if (tempAudioPathToDelete.exists()) {
                tempAudioPathToDelete.delete();
            }
        }

        mTimeoutHandler.removeCallbacks(mShowAnswerTask);
        mTimeoutHandler.removeCallbacks(mShowQuestionTask);
        mTimerHandler.removeCallbacks(removeChosenAnswerText);
        longClickHandler.removeCallbacks(longClickTestRunnable);
        longClickHandler.removeCallbacks(startLongClickAction);
        if (getResultIntent() == null) {
            //intent不为空，代表已经设置了result
            AbstractFlashcardViewer.this.setResult(result);
        }

        if (saveDeck) {
            UIUtils.saveCollectionInBackground();
        }
        finishWithAnimation(ActivityTransitionAnimation.RIGHT);
    }


    protected Intent getResultIntent() {
        return null;
    }


    protected void refreshActionBar() {
        supportInvalidateOptionsMenu();
    }


    /**
     * Fixing bug 720: <input> focus, thanks to pablomouzo on android issue 7189
     */
    class MyWebView extends WebView {

        public MyWebView(Context context) {
            super(context);
        }


        @Override
        public void loadDataWithBaseURL(@Nullable String baseUrl, String data, @Nullable String mimeType, @Nullable String encoding, @Nullable String historyUrl) {
            if (!AbstractFlashcardViewer.this.wasDestroyed()) {
                super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
            } else {
                Timber.w("Not loading card - Activity is in the process of being destroyed.");
            }
        }


        @Override
        protected void onScrollChanged(int horiz, int vert, int oldHoriz, int oldVert) {
            super.onScrollChanged(horiz, vert, oldHoriz, oldVert);
            if (Math.abs(horiz - oldHoriz) > Math.abs(vert - oldVert)) {
                mIsXScrolling = true;
                scrollHandler.removeCallbacks(scrollXRunnable);
                scrollHandler.postDelayed(scrollXRunnable, 300);
            } else {
                mIsYScrolling = true;
                scrollHandler.removeCallbacks(scrollYRunnable);
                scrollHandler.postDelayed(scrollYRunnable, 300);
            }
        }


        private final Handler scrollHandler = new Handler();
        private final Runnable scrollXRunnable = new Runnable() {
            @Override
            public void run() {
                mIsXScrolling = false;
            }
        };
        private final Runnable scrollYRunnable = new Runnable() {
            @Override
            public void run() {
                mIsYScrolling = false;
            }
        };

    }



    class MyGestureDetector extends SimpleOnGestureListener {
        //Android design spec for the size of the status bar.
        private final int NO_GESTURE_BORDER_DIP = 24;


        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Timber.d("onFling");

            //#5741 - A swipe from the top caused delayedHide to be triggered,
            //accepting a gesture and quickly disabling the status bar, which wasn't ideal.
            //it would be lovely to use e1.getEdgeFlags(), but alas, it doesn't work.
            if (isTouchingEdge(e1)) {
                Timber.d("ignoring edge fling");
                return false;
            }

            // Go back to immersive mode if the user had temporarily exited it (and then execute swipe gesture)
            AbstractFlashcardViewer.this.onFling();
            if (mGesturesEnabled) {
                try {
                    float dy = e2.getY() - e1.getY();
                    float dx = e2.getX() - e1.getX();

                    if (Math.abs(dx) > Math.abs(dy)) {
                        // horizontal swipe if moved further in x direction than y direction
                        if (dx > AnkiDroidApp.sSwipeMinDistance
                                && Math.abs(velocityX) > AnkiDroidApp.sSwipeThresholdVelocity
                                && !mIsXScrolling && !mIsSelecting) {
                            // right
                            executeCommand(mGestureSwipeRight);
                        } else if (dx < -AnkiDroidApp.sSwipeMinDistance
                                && Math.abs(velocityX) > AnkiDroidApp.sSwipeThresholdVelocity
                                && !mIsXScrolling && !mIsSelecting) {
                            // left
                            executeCommand(mGestureSwipeLeft);
                        }
                    } else {
                        // otherwise vertical swipe
                        if (dy > AnkiDroidApp.sSwipeMinDistance
                                && Math.abs(velocityY) > AnkiDroidApp.sSwipeThresholdVelocity
                                && !mIsYScrolling) {
                            // down
                            executeCommand(mGestureSwipeDown);
                        } else if (dy < -AnkiDroidApp.sSwipeMinDistance
                                && Math.abs(velocityY) > AnkiDroidApp.sSwipeThresholdVelocity
                                && !mIsYScrolling) {
                            // up
                            executeCommand(mGestureSwipeUp);
                        }
                    }
                } catch (Exception e) {
                    Timber.e(e, "onFling Exception");
                }
            }
            return false;
        }


        private boolean isTouchingEdge(MotionEvent e1) {
            int height = mTouchLayer.getHeight();
            int width = mTouchLayer.getWidth();
            float margin = NO_GESTURE_BORDER_DIP * getResources().getDisplayMetrics().density + 0.5f;
            return e1.getX() < margin || e1.getY() < margin || height - e1.getY() < margin || width - e1.getX() < margin;
        }


        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mGesturesEnabled) {
                executeCommand(mGestureDoubleTap);
            }
            return true;
        }


        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Timber.i("onSingleTapUp");

            if (mTouchStarted) {
                longClickHandler.removeCallbacks(longClickTestRunnable);
                mTouchStarted = false;
            }
            return false;
        }


        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Timber.i("onSingleTapConfirmed");

            // Go back to immersive mode if the user had temporarily exited it (and ignore the tap gesture)
            if (onSingleTap()) {
                return true;
            }
            return executeTouchCommand(e);
        }


        protected boolean executeTouchCommand(@NonNull MotionEvent e) {
            if (mGesturesEnabled && !mIsSelecting) {
                int height = mTouchLayer.getHeight();
                int width = mTouchLayer.getWidth();
                float posX = e.getX();
                float posY = e.getY();
                final boolean b = posY > height * (1 - posX / width);
                if (posX > posY / height * width) {
                    if (b) {
                        executeCommand(mGestureTapRight);
                    } else {
                        executeCommand(mGestureTapTop);
                    }
                } else {
                    if (b) {
                        executeCommand(mGestureTapBottom);
                    } else {
                        executeCommand(mGestureTapLeft);
                    }
                }
            }
            mIsSelecting = false;
            showLookupButtonIfNeeded();
            return false;
        }


        public void onWebViewCreated(@NonNull WebView webView) {
            //intentionally blank
        }


        public void onFillFlashcard() {
            //intentionally blank
        }


        public boolean eventCanBeSentToWebView(@NonNull MotionEvent event) {
            return true;
        }
    }


    protected boolean onSingleTap() {
        return false;
    }


    protected void onFling() {

    }


    /**
     * #6141 - blocks clicking links from executing "touch" gestures.
     * COULD_BE_BETTER: Make base class static and move this out of the CardViewer
     */
    class LinkDetectingGestureDetector extends AbstractFlashcardViewer.MyGestureDetector {
        /**
         * A list of events to process when listening to WebView touches
         */
        private HashSet<MotionEvent> mDesiredTouchEvents = new HashSet<>();
        /**
         * A list of events we sent to the WebView (to block double-processing)
         */
        private HashSet<MotionEvent> mDispatchedTouchEvents = new HashSet<>();


        @Override
        public void onFillFlashcard() {
            Timber.d("Removing pending touch events for gestures");
            mDesiredTouchEvents.clear();
            mDispatchedTouchEvents.clear();
        }


        @Override
        public boolean eventCanBeSentToWebView(@NonNull MotionEvent event) {
            //if we processed the event, we don't want to perform it again
            return !mDispatchedTouchEvents.remove(event);
        }


        @Override
        protected boolean executeTouchCommand(@NonNull MotionEvent downEvent) {
            downEvent.setAction(MotionEvent.ACTION_DOWN);
            MotionEvent upEvent = MotionEvent.obtainNoHistory(downEvent);
            upEvent.setAction(MotionEvent.ACTION_UP);

            //mark the events we want to process
            mDesiredTouchEvents.add(downEvent);
            mDesiredTouchEvents.add(upEvent);

            //mark the events to can guard against double-processing
            mDispatchedTouchEvents.add(downEvent);
            mDispatchedTouchEvents.add(upEvent);

            Timber.d("Dispatching touch events");
            processCardAction(cardWebView -> {
                cardWebView.dispatchTouchEvent(downEvent);
                cardWebView.dispatchTouchEvent(upEvent);
            });
            return false;
        }


        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onWebViewCreated(@NonNull WebView webView) {
            Timber.d("Initializing WebView touch handler");
            webView.setOnTouchListener((webViewAsView, motionEvent) -> {
                if (!mDesiredTouchEvents.remove(motionEvent)) {
                    return false;
                }

                //We need an associated up event so the WebView doesn't keep a selection
                //But we don't want to handle this as a touch event.
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    return true;
                }

                WebView card = (WebView) webViewAsView;
                HitTestResult result = card.getHitTestResult();

                if (isLinkClick(result)) {
                    Timber.v("Detected link click - ignoring gesture dispatch");
                    return true;
                }

                Timber.v("Executing continuation for click type: %d", result == null ? -178 : result.getType());
                super.executeTouchCommand(motionEvent);
                return true;
            });
        }


        private boolean isLinkClick(HitTestResult result) {
            if (result == null) {
                return false;
            }
            int type = result.getType();
            return type == HitTestResult.SRC_ANCHOR_TYPE
                    || type == HitTestResult.SRC_IMAGE_ANCHOR_TYPE;
        }
    }


    /**
     * Removes first occurrence in answerContent of any audio that is present due to use of
     * {{FrontSide}} on the answer.
     *
     * @param answerContent The content from which to remove front side audio.
     * @return The content stripped of audio due to {{FrontSide}} inclusion.
     */
    private String removeFrontSideAudio(String answerContent) {
        String answerFormat = getAnswerFormat();
        String newAnswerContent = answerContent;
        if (answerFormat.contains("{{FrontSide}}")) { // possible audio removal necessary
            String frontSideFormat = mCurrentCard._getQA(false).get("q");
            Matcher audioReferences = Sound.sSoundPattern.matcher(frontSideFormat);
            // remove the first instance of audio contained in "{{FrontSide}}"
            while (audioReferences.find()) {
                newAnswerContent = newAnswerContent.replaceFirst(Pattern.quote(audioReferences.group()), "");
            }
        }
        return newAnswerContent;
    }


    /**
     * Public method to start new video player activity
     */
    public void playVideo(String path) {
        Timber.i("Launching Video: %s", path);
        Intent videoPlayer = new Intent(this, VideoPlayer.class);
        videoPlayer.putExtra("path", path);
        startActivityWithoutAnimation(videoPlayer);
    }


    /**
     * Callback for when TTS has been initialized.
     */
    public void ttsInitialized() {
        mTtsInitialized = true;
        if (mReplayOnTtsInit) {
            playSoundsVIP(true);
        }
    }


    private void drawMark() {
        if (mCurrentCard == null) {
            return;
        }

        mCardMarker.displayMark(shouldDisplayMark());
    }


    protected boolean shouldDisplayMark() {
        return mCurrentCard.note().hasTag("marked");
    }


    protected void onMark(Card card) {
        if (card == null) {
            return;
        }
        Note note = card.note();
        if (note.hasTag("marked")) {
            note.delTag("marked");
        } else {
            note.addTag("marked");
        }
        note.flush();
        refreshActionBar();
        drawMark();
    }


    private void drawFlag() {
        if (mCurrentCard == null) {
            return;
        }
        mCardMarker.displayFlag(getFlagToDisplay());
    }


    protected @FlagDef
    int getFlagToDisplay() {
        return mCurrentCard.userFlag();
    }


    protected void onFlag(Card card, @FlagDef int flag) {
        if (card == null) {
            return;
        }
        card.setUserFlag(flag);
        card.flush();
        refreshActionBar();
        drawFlag();
        /* Following code would allow to update value of {{cardFlag}}.
           Anki does not update this value when a flag is changed, so
           currently this code would do something that anki itself
           does not do. I hope in the future Anki will correct that
           and this code may becomes useful.

        card._getQA(true); //force reload. Useful iff {{cardFlag}} occurs in the template
        if (sDisplayAnswer) {
            displayCardAnswer();
        } else {
            displayCardQuestion();
            } */
    }


    protected void dismiss(Collection.DismissType type) {
        blockControls(false);
        CollectionTask.launchCollectionTask(DISMISS, mDismissCardHandler,
                new TaskData(new Object[] {mCurrentCard, type}));
    }


    /**
     * Signals from a WebView represent actions with no parameters
     */
    @VisibleForTesting
    static class WebViewSignalParserUtils {
        /**
         * A signal which we did not know how to handle
         */
        public static final int SIGNAL_UNHANDLED = 0;
        /**
         * A known signal which should perform a noop
         */
        public static final int SIGNAL_NOOP = 1;

        public static final int TYPE_FOCUS = 2;
        /**
         * Tell the app that we no longer want to focus the WebView and should instead return keyboard focus to a
         * native answer input method.
         */
        public static final int RELINQUISH_FOCUS = 3;

        public static final int SHOW_ANSWER = 4;
        public static final int ANSWER_ORDINAL_1 = 5;
        public static final int ANSWER_ORDINAL_2 = 6;
        public static final int ANSWER_ORDINAL_3 = 7;
        public static final int ANSWER_ORDINAL_4 = 8;


        public static int getSignalFromUrl(String url) {
            switch (url) {
                case "signal:typefocus":
                    return TYPE_FOCUS;
                case "signal:relinquishFocus":
                    return RELINQUISH_FOCUS;
                case "signal:show_answer":
                    return SHOW_ANSWER;
                case "signal:answer_ease1":
                    return ANSWER_ORDINAL_1;
                case "signal:answer_ease2":
                    return ANSWER_ORDINAL_2;
                case "signal:answer_ease3":
                    return ANSWER_ORDINAL_3;
                case "signal:answer_ease4":
                    return ANSWER_ORDINAL_4;
                default:
                    break;
            }

            if (url.startsWith("signal:answer_ease")) {
                Timber.w("Unhandled signal: ease value: %s", url);
                return SIGNAL_NOOP;
            }

            return SIGNAL_UNHANDLED; //unknown, or not a signal.
        }
    }



    protected class CardViewerWebClient extends WebViewClient {
        @Override
        @TargetApi(Build.VERSION_CODES.N)
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            Timber.d("Obtained URL from card: '%s'", url);
            return filterUrl(url);
        }


        @Override
        @TargetApi(Build.VERSION_CODES.N)
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            WebResourceResponse webResourceResponse = null;
            if (!AdaptionUtil.hasWebBrowser(getBaseContext())) {
                String scheme = request.getUrl().getScheme().trim();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    String response = getResources().getString(R.string.no_outgoing_link_in_cardbrowser);
                    webResourceResponse = new WebResourceResponse("text/html", "utf-8", new ByteArrayInputStream(response.getBytes()));
                }
            }

            return webResourceResponse;
        }


        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            mMissingImageHandler.processFailure(request, AbstractFlashcardViewer.this::displayCouldNotFindImageSnackbar);
        }


        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            mMissingImageHandler.processFailure(request, AbstractFlashcardViewer.this::displayCouldNotFindImageSnackbar);
        }


        @Override
        @SuppressWarnings("deprecation") // tracked as #5017 in github
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return filterUrl(url);
        }


        // Filter any links using the custom "playsound" protocol defined in Sound.java.
        // We play sounds through these links when a user taps the sound icon.
        private boolean filterUrl(String url) {
            if (url.startsWith("playsound:")) {
                // Send a message that will be handled on the UI thread.
                Message msg = Message.obtain();
                String soundPath = url.replaceFirst("playsound:", "");
                msg.obj = soundPath;
                mHandler.sendMessage(msg);
                return true;
            }
            if (url.startsWith("file") || url.startsWith("data:")) {
                return false; // Let the webview load files, i.e. local images.
            }
            if (url.startsWith("typeblurtext:")) {
                // Store the text the javascript has send us…
                mTypeInput = decodeUrl(url.replaceFirst("typeblurtext:", ""));
                // … and show the “SHOW ANSWER” button again.
                mFlipCardLayout.setVisibility(View.VISIBLE);
                return true;
            }
            if (url.startsWith("typeentertext:")) {
                // Store the text the javascript has send us…
                mTypeInput = decodeUrl(url.replaceFirst("typeentertext:", ""));
                // … and show the answer.
                mFlipCardLayout.performClick();
                return true;
            }
            // Show options menu from WebView
            if (url.startsWith("signal:anki_show_options_menu")) {
                if (isFullscreen()) {
                    openOptionsMenu();
                } else {
                    UIUtils.showThemedToast(AbstractFlashcardViewer.this, getString(R.string.ankidroid_turn_on_fullscreen_options_menu), true);
                }
                return true;
            }

            // Show Navigation Drawer from WebView
//            if (url.startsWith("signal:anki_show_navigation_drawer")) {
//                if (isFullscreen()) {
//                    AbstractFlashcardViewer.this.onNavigationPressed();
//                } else {
//                    UIUtils.showThemedToast(AbstractFlashcardViewer.this, getString(R.string.ankidroid_turn_on_fullscreen_nav_drawer), true);
//                }
//                return true;
//            }

            // card.html reload
            if (url.startsWith("signal:reload_card_html")) {
                redrawCard();
                return true;
            }
            // mark card using javascript
            if (url.startsWith("signal:mark_current_card")) {
                if (isAnkiApiNull("markCard")) {
                    showDeveloperContact(ankiJsErrorCodeDefault);
                    return true;
                } else if (mJsApiListMap.get("markCard")) {
                    executeCommand(COMMAND_MARK);
                } else {
                    // see 02-string.xml
                    showDeveloperContact(ankiJsErrorCodeMarkCard);
                }
                return true;
            }
            // flag card (blue, green, orange, red) using javascript from AnkiDroid webview
            if (url.startsWith("signal:flag_")) {
                if (isAnkiApiNull("toggleFlag")) {
                    showDeveloperContact(ankiJsErrorCodeDefault);
                    return true;
                } else if (!mJsApiListMap.get("toggleFlag")) {
                    // see 02-string.xml
                    showDeveloperContact(ankiJsErrorCodeFlagCard);
                    return true;
                }

                String mFlag = url.replaceFirst("signal:flag_", "");
                switch (mFlag) {
                    case "none":
                        executeCommand(COMMAND_UNSET_FLAG);
                        return true;
                    case "red":
                        executeCommand(COMMAND_TOGGLE_FLAG_RED);
                        return true;
                    case "orange":
                        executeCommand(COMMAND_TOGGLE_FLAG_ORANGE);
                        return true;
                    case "green":
                        executeCommand(COMMAND_TOGGLE_FLAG_GREEN);
                        return true;
                    case "blue":
                        executeCommand(COMMAND_TOGGLE_FLAG_BLUE);
                        return true;
                    default:
                        Timber.d("No such Flag found.");
                        return true;
                }
            }

            // Show toast using JS
            if (url.startsWith("signal:anki_show_toast:")) {
                String msg = url.replaceFirst("signal:anki_show_toast:", "");
                String msgDecode = decodeUrl(msg);
                UIUtils.showThemedToast(AbstractFlashcardViewer.this, msgDecode, true);
                return true;
            }

            int signalOrdinal = WebViewSignalParserUtils.getSignalFromUrl(url);
            switch (signalOrdinal) {
                case WebViewSignalParserUtils.SIGNAL_UNHANDLED:
                    break; //continue parsing
                case WebViewSignalParserUtils.SIGNAL_NOOP:
                    return true;
                case WebViewSignalParserUtils.TYPE_FOCUS:
                    // Hide the “SHOW ANSWER” button when the input has focus. The soft keyboard takes up enough
                    // space by itself.
                    mFlipCardLayout.setVisibility(View.GONE);
                    return true;
                case WebViewSignalParserUtils.RELINQUISH_FOCUS:
                    //#5811 - The WebView could be focused via mouse. Allow components to return focus to Android.
                    focusAnswerCompletionField();
                    return true;
                /**
                 *  Call displayCardAnswer() and answerCard() from anki deck template using javascript
                 *  See card.js in assets/scripts folder
                 */
                case WebViewSignalParserUtils.SHOW_ANSWER:
                    // display answer when showAnswer() called from card.js
                    if (!sDisplayAnswer) {
                        displayCardAnswer();
                    }
                    return true;
                case WebViewSignalParserUtils.ANSWER_ORDINAL_1:
                    flipOrAnswerCard(EASE_1);
                    return true;
                case WebViewSignalParserUtils.ANSWER_ORDINAL_2:
                    flipOrAnswerCard(EASE_2);
                    return true;
                case WebViewSignalParserUtils.ANSWER_ORDINAL_3:
                    flipOrAnswerCard(EASE_3);
                    return true;
                case WebViewSignalParserUtils.ANSWER_ORDINAL_4:
                    flipOrAnswerCard(EASE_4);
                    return true;
                default:
                    //We know it was a signal, but forgot a case in the case statement.
                    //This is not the same as SIGNAL_UNHANDLED, where it isn't a known signal.
                    Timber.w("Unhandled signal case: %d", signalOrdinal);
                    return true;
            }
            Intent intent = null;
            try {
                if (url.startsWith("intent:")) {
                    intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                } else if (url.startsWith("android-app:")) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                        intent = Intent.parseUri(url, 0);
                        intent.setData(null);
                        intent.setPackage(Uri.parse(url).getHost());
                    } else {
                        intent = Intent.parseUri(url, Intent.URI_ANDROID_APP_SCHEME);
                    }
                }
                if (intent != null) {
                    if (getPackageManager().resolveActivity(intent, 0) == null) {
                        String packageName = intent.getPackage();
                        if (packageName == null) {
                            Timber.d("Not using resolved intent uri because not available: %s", intent);
                            intent = null;
                        } else {
                            Timber.d("Resolving intent uri to market uri because not available: %s", intent);
                            intent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=" + packageName));
                            if (getPackageManager().resolveActivity(intent, 0) == null) {
                                intent = null;
                            }
                        }
                    } else {
                        // https://developer.chrome.com/multidevice/android/intents says that we should remove this
                        intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    }
                }
            } catch (Throwable t) {
                Timber.w("Unable to parse intent uri: %s because: %s", url, t.getMessage());
            }
            if (intent == null) {
                Timber.d("Opening external link \"%s\" with an Intent", url);
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            } else {
                Timber.d("Opening resolved external link \"%s\" with an Intent: %s", url, intent);
            }
            try {
                startActivityWithoutAnimation(intent);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace(); // Don't crash if the intent is not handled
            }
            return true;
        }


        private String decodeUrl(String url) {
            try {
                return URLDecoder.decode(url, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Timber.e(e, "UTF-8 isn't supported as an encoding?");
            } catch (Exception e) {
                Timber.e(e, "Exception decoding: '%s'", url);
                UIUtils.showThemedToast(AbstractFlashcardViewer.this, getString(R.string.card_viewer_url_decode_error), true);
            }
            return "";
        }


        // Run any post-load events in javascript that rely on the window being completely loaded.
        @Override
        public void onPageFinished(WebView view, String url) {
            Timber.d("Java onPageFinished triggered: %s", url);
            if (Objects.equals(url, mBaseUrl + "__viewer__.html")) {
                drawFlag();
                drawMark();
                Timber.d("New URL, triggering JS onPageFinished: %s", url);
                view.loadUrl("javascript:onPageFinished();");
            }

        }


        /**
         * Fix: #5780 - WebView Renderer OOM crashes reviewer
         */
        @Override
        @TargetApi(Build.VERSION_CODES.O)
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            Timber.i("Obtaining write lock for card");
            Lock writeLock = mCardLock.writeLock();
            Timber.i("Obtained write lock for card");
            try {
                writeLock.lock();
                if (mCardWebView == null || !mCardWebView.equals(view)) {
                    //A view crashed that wasn't ours.
                    //We have nothing to handle. Returning false is a desire to crash, so return true.
                    Timber.i("Unrelated WebView Renderer terminated. Crashed: %b", detail.didCrash());
                    return true;
                }

                Timber.e("WebView Renderer process terminated. Crashed: %b", detail.didCrash());

                //Destroy the current WebView (to ensure WebView is GCed).
                //Otherwise, we get the following error:
                //"crash wasn't handled by all associated webviews, triggering application crash"
                mCardFrame.removeAllViews();
                mCardFrameParent.removeView(mCardFrame);
                //destroy after removal from the view - produces logcat warnings otherwise
                destroyWebView(mCardWebView);
                mCardWebView = null;
                //inflate a new instance of mCardFrame
                mCardFrame = inflateNewView(R.id.flashcard);
                //Even with the above, I occasionally saw the above error. Manually trigger the GC.
                //I'll keep this line unless I see another crash, which would point to another underlying issue.
                System.gc();

                //We only want to show one message per branch.

                //It's not necessarily an OOM crash, false implies a general code which is for "system terminated".
                int errorCauseId = detail.didCrash() ? R.string.webview_crash_unknown : R.string.webview_crash_oom;
                String errorCauseString = getResources().getString(errorCauseId);

                if (!canRecoverFromWebViewRendererCrash()) {
                    Timber.e("Unrecoverable WebView Render crash");
                    String errorMessage = getResources().getString(R.string.webview_crash_fatal, errorCauseString);
                    UIUtils.showThemedToast(AbstractFlashcardViewer.this, errorMessage, false);
                    finishWithoutAnimation();
                    return true;
                }

                if (webViewRendererLastCrashedOnCard(mCurrentCard.getId())) {
                    Timber.e("Web Renderer crash loop on card: %d", mCurrentCard.getId());
                    displayRenderLoopDialog(mCurrentCard, detail);
                    return true;
                }

                // If we get here, the error is non-fatal and we should re-render the WebView
                // This logic may need to be better defined. The card could have changed by the time we get here.
                lastCrashingCardId = mCurrentCard.getId();


                String nonFatalError = getResources().getString(R.string.webview_crash_nonfatal, errorCauseString);
                UIUtils.showThemedToast(AbstractFlashcardViewer.this, nonFatalError, false);

                //we need to add at index 0 so gestures still go through.
                mCardFrameParent.addView(mCardFrame, 0);

                recreateWebView();
            } finally {
                writeLock.unlock();
                Timber.d("Relinquished writeLock");
            }
            displayCardQuestion();

            //We handled the crash and can continue.
            return true;
        }


        @TargetApi(Build.VERSION_CODES.O)
        private void displayRenderLoopDialog(Card mCurrentCard, RenderProcessGoneDetail detail) {
            String cardInformation = Long.toString(mCurrentCard.getId());
            Resources res = getResources();

            String errorDetails = detail.didCrash()
                    ? res.getString(R.string.webview_crash_unknwon_detailed)
                    : res.getString(R.string.webview_crash_oom_details);
            new MaterialDialog.Builder(AbstractFlashcardViewer.this)
                    .title(res.getString(R.string.webview_crash_loop_dialog_title))
                    .content(res.getString(R.string.webview_crash_loop_dialog_content, cardInformation, errorDetails))
                    .positiveText(R.string.dialog_ok)
                    .cancelable(false)
                    .canceledOnTouchOutside(false)
                    .onPositive((materialDialog, dialogAction) -> finishWithoutAnimation())
                    .show();
        }
    }


    private void displayCouldNotFindImageSnackbar(String filename) {
        OnClickListener onClickListener = (v) -> openUrl(Uri.parse(getString(R.string.link_faq_missing_media)));
        showSnackbar(getString(R.string.card_viewer_could_not_find_image, filename), R.string.card_viewer_could_not_find_image_get_help, onClickListener);
    }


    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    protected String getTypedInputText() {
        return mTypeInput;
    }


    @SuppressLint("WebViewApiAvailability")
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    void handleUrlFromJavascript(String url) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //WebViewCompat recommended here, but I'll avoid the dependency as it's test code
            CardViewerWebClient c = ((CardViewerWebClient) this.mCardWebView.getWebViewClient());
            if (c == null) {
                throw new IllegalStateException("Couldn't obtain WebView - maybe it wasn't created yet");
            }
            c.filterUrl(url);
        } else {
            throw new IllegalStateException("Can't get WebViewClient due to Android API");
        }
    }


    // Check if value null
    private boolean isAnkiApiNull(String api) {
        return mJsApiListMap.get(api) == null;
    }


    /*
     * see 02-strings.xml
     * Show Error code when mark card or flag card unsupported
     * 1 - mark card
     * 2 - flag card
     *
     * show developer contact if js api used in card is deprecated
     */
    private void showDeveloperContact(int errorCode) {
        String errorMsg = getString(R.string.anki_js_error_code, errorCode);

        View parentLayout = findViewById(android.R.id.content);
        String snackbarMsg;
        snackbarMsg = getString(R.string.api_version_developer_contact, mCardSuppliedDeveloperContact, errorMsg);

        Snackbar snackbar = Snackbar.make(parentLayout, snackbarMsg, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        TextView snackTextView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        snackTextView.setTextColor(Color.WHITE);
        snackTextView.setMaxLines(3);

        snackbar.setActionTextColor(Color.MAGENTA)
                .setAction(getString(R.string.reviewer_invalid_api_version_visit_documentation), view -> {
                    openUrl(Uri.parse("https://github.com/ankidroid/Anki-Android/wiki"));
                });

        snackbar.show();
    }


    /**
     * Supplied api version must be equal to current api version to call mark card, toggle flag functions etc.
     */
    private boolean requireApiVersion(String apiVer, String apiDevContact) {
        try {

            if (TextUtils.isEmpty(apiDevContact)) {
                return false;
            }

            Version mVersionCurrent = Version.valueOf(sCurrentJsApiVersion);
            Version mVersionSupplied = Version.valueOf(apiVer);

            /*
             * if api major version equals to supplied major version then return true and also check for minor version and patch version
             * show toast for update and contact developer if need updates
             * otherwise return false
             */
            if (mVersionSupplied.equals(mVersionCurrent)) {
                return true;
            } else if (mVersionSupplied.lessThan(mVersionCurrent)) {
                UIUtils.showThemedToast(AbstractFlashcardViewer.this, getString(R.string.update_js_api_version, mCardSuppliedDeveloperContact), false);

                if (mVersionSupplied.greaterThanOrEqualTo(Version.valueOf(sMinimumJsApiVersion))) {
                    return true;
                } else {
                    return false;
                }
            } else {
                UIUtils.showThemedToast(AbstractFlashcardViewer.this, getString(R.string.valid_js_api_version, mCardSuppliedDeveloperContact), false);
                return false;
            }
        } catch (Exception e) {
            Timber.w(e, "requireApiVersion::exception");
        }
        return false;
    }


    @VisibleForTesting
    void loadInitialCard() {
        CollectionTask.launchCollectionTask(ANSWER_CARD, mAnswerCardHandler(false),
                new TaskData(null, 0));
    }


    public ReviewerUi.ControlBlock getControlBlocked() {
        return mControlBlocked;
    }


    public boolean isDisplayingAnswer() {
        return sDisplayAnswer;
    }


    public boolean isControlBlocked() {
        return getControlBlocked() != ControlBlock.UNBLOCKED;
    }


    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    static void setEditorCard(Card card) {
        //I don't see why we don't do this by intent.
        sEditorCard = card;
    }


    protected void showTagsDialog() {
        ArrayList<String> tags = new ArrayList<>(getCol().getTags().all());
        ArrayList<String> selTags = new ArrayList<>(mCurrentCard.note().getTags());
        TagsDialog.TagsDialogListener tagsDialogListener = (selectedTags, option) -> {
            if (!mCurrentCard.note().getTags().equals(selectedTags)) {
                String tagString = TextUtils.join(" ", selectedTags);
                Note note = mCurrentCard.note();
                note.setTagsFromStr(tagString);
                note.flush();
                // Reload current card to reflect tag changes
                displayCardQuestion(true);
            }
        };
        TagsDialog dialog = TagsDialog.newInstance(TagsDialog.TYPE_ADD_TAG, selTags, tags);
        dialog.setTagsDialogListener(tagsDialogListener);
        showDialogFragment(dialog);
    }


    // init or reset api list
    private void jsApiInit() {
        mCardSuppliedApiVersion = "";
        mCardSuppliedDeveloperContact = "";

        for (int i = 0; i < mApiList.length; i++) {
            mJsApiListMap.put(mApiList[i], false);
        }
    }


    /*
    Javascript Interface class for calling Java function from AnkiDroid WebView
   see card.js for available functions
    */
    // list of api that can be accessed
    private final String[] mApiList = {"toggleFlag", "markCard"};


    public JavaScriptFunction javaScriptFunction() {
        return new JavaScriptFunction();
    }


    public class JavaScriptFunction {

        private final Gson mGson = new Gson();


        // if supplied api version match then enable api
        private void enableJsApi() {
            for (int i = 0; i < mApiList.length; i++) {
                mJsApiListMap.put(mApiList[i], true);
            }
        }


        @JavascriptInterface
        public String init(String jsonData) {
            JSONObject data;
            String apiStatusJson = "";

            try {
                data = new JSONObject(jsonData);
                if (!(data == JSONObject.NULL)) {
                    mCardSuppliedApiVersion = data.optString("version", "");
                    mCardSuppliedDeveloperContact = data.optString("developer", "");

                    if (requireApiVersion(mCardSuppliedApiVersion, mCardSuppliedDeveloperContact)) {
                        enableJsApi();
                    }

                    apiStatusJson = mGson.toJson(mJsApiListMap);
                }

            } catch (JSONException j) {
                UIUtils.showThemedToast(AbstractFlashcardViewer.this, getString(R.string.invalid_json_data, j.getLocalizedMessage()), false);
            }
            return String.valueOf(apiStatusJson);
        }


        // This method and the one belows return "default" values when there is no count nor ETA.
        // Javascript may expect ETA and Counts to be set, this ensure it does not bug too much by providing a value of correct type
        // but with a clearly incorrect value.
        // It's overridden in the Reviewer, where those values are actually defined.
        @JavascriptInterface
        public String ankiGetNewCardCount() {
            return "-1";
        }


        @JavascriptInterface
        public String ankiGetLrnCardCount() {
            return "-1";
        }


        @JavascriptInterface
        public String ankiGetRevCardCount() {
            return "-1";
        }


        @JavascriptInterface
        public int ankiGetETA() {
            return -1;
        }


        @JavascriptInterface
        public boolean ankiGetCardMark() {
            return shouldDisplayMark();
        }


        @JavascriptInterface
        public int ankiGetCardFlag() {
            return mCurrentCard.userFlag();
        }


        @JavascriptInterface
        public String ankiGetNextTime1() {
            return (String) mNext1.getText();
        }


        @JavascriptInterface
        public String ankiGetNextTime2() {
            return (String) mNext2.getText();
        }


        @JavascriptInterface
        public String ankiGetNextTime3() {
            return (String) mNext3.getText();
        }


        @JavascriptInterface
        public String ankiGetNextTime4() {
            return (String) mNext4.getText();
        }


        @JavascriptInterface
        public int ankiGetCardReps() {
            return mCurrentCard.getReps();
        }


        @JavascriptInterface
        public int ankiGetCardInterval() {
            return mCurrentCard.getIvl();
        }


        /**
         * Returns the ease as an int (percentage * 10). Default: 2500 (250%). Minimum: 1300 (130%)
         */
        @JavascriptInterface
        public int ankiGetCardFactor() {
            return mCurrentCard.getFactor();
        }


        /**
         * Returns the last modified time as a Unix timestamp in seconds. Example: 1477384099
         */
        @JavascriptInterface
        public long ankiGetCardMod() {
            return mCurrentCard.getMod();
        }


        /**
         * Returns the ID of the card. Example: 1477380543053
         */
        @JavascriptInterface
        public long ankiGetCardId() {
            return mCurrentCard.getId();
        }


        /**
         * Returns the ID of the note which generated the card. Example: 1590418157630
         */
        @JavascriptInterface
        public long ankiGetCardNid() {
            return mCurrentCard.getNid();
        }


        @JavascriptInterface
        @Consts.CARD_TYPE
        public int ankiGetCardType() {
            return mCurrentCard.getType();
        }


        /**
         * Returns the ID of the deck which contains the card. Example: 1595967594978
         */
        @JavascriptInterface
        public long ankiGetCardDid() {
            return mCurrentCard.getDid();
        }


        @JavascriptInterface
        public int ankiGetCardLeft() {
            return mCurrentCard.getLeft();
        }


        /**
         * Returns the ID of the home deck for the card if it is filtered, or 0 if not filtered. Example: 1595967594978
         */
        @JavascriptInterface
        public long ankiGetCardODid() {
            return mCurrentCard.getODid();
        }


        @JavascriptInterface
        public long ankiGetCardODue() {
            return mCurrentCard.getODue();
        }


        @JavascriptInterface
        @Consts.CARD_QUEUE
        public int ankiGetCardQueue() {
            return mCurrentCard.getQueue();
        }


        @JavascriptInterface
        public int ankiGetCardLapses() {
            return mCurrentCard.getLapses();
        }


        @JavascriptInterface
        public long ankiGetCardDue() {
            return mCurrentCard.getDue();
        }


        @JavascriptInterface
        public boolean ankiIsInFullscreen() {
            return isFullscreen();
        }


        @JavascriptInterface
        public boolean ankiIsTopbarShown() {
            return mPrefShowTopbar;
        }


        @JavascriptInterface
        public boolean ankiIsInNightMode() {
            return isInNightMode();
        }


        @JavascriptInterface
        public boolean ankiIsActiveNetworkMetered() {
            try {
                ConnectivityManager cm = (ConnectivityManager) AnkiDroidApp.getInstance().getApplicationContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm == null) {
                    Timber.w("ConnectivityManager not found - assuming metered connection");
                    return true;
                }
                return ConnectivityManagerCompat.isActiveNetworkMetered(cm);
            } catch (Exception e) {
                Timber.w(e, "Exception obtaining metered connection - assuming metered connection");
                return true;
            }
        }
    }
}
