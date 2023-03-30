/****************************************************************************************
 * Copyright (c) 2014 Houssam Salem <houssam.salem.au@gmail.com>                        *
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

package com.ichi2.libanki;


import android.content.SharedPreferences;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy.*;

import androidx.annotation.IntDef;
import timber.log.Timber;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class Consts {

    // whether new cards should be mixed with reviews, or shown first or last
    public static final int NEW_CARDS_DISTRIBUTE = 0;
    public static final int NEW_CARDS_LAST = 1;
    public static final int NEW_CARDS_FIRST = 2;



    @Retention(SOURCE)
    @IntDef( {NEW_CARDS_DISTRIBUTE, NEW_CARDS_LAST, NEW_CARDS_FIRST})
    public @interface NEW_CARD_ORDER {
    }



    // new card insertion order
    public static final int NEW_CARDS_RANDOM = 0;
    public static final int NEW_CARDS_DUE = 1;



    @Retention(SOURCE)
    @IntDef( {NEW_CARDS_RANDOM, NEW_CARDS_DUE})
    public @interface NEW_CARDS_INSERTION {
    }



    // Queue types
    public static final int QUEUE_TYPE_MANUALLY_BURIED = -3;
    public static final int QUEUE_TYPE_SIBLING_BURIED = -2;
    public static final int QUEUE_TYPE_SUSPENDED = -1;
    public static final int QUEUE_TYPE_NEW = 0;
    public static final int QUEUE_TYPE_LRN = 1;
    public static final int QUEUE_TYPE_REV = 2;
    public static final int QUEUE_TYPE_DAY_LEARN_RELEARN = 3;
    public static final int QUEUE_TYPE_PREVIEW = 4;



    @Retention(SOURCE)
    @IntDef( {
            QUEUE_TYPE_MANUALLY_BURIED,
            QUEUE_TYPE_SIBLING_BURIED,
            QUEUE_TYPE_SUSPENDED,
            QUEUE_TYPE_NEW,
            QUEUE_TYPE_LRN,
            QUEUE_TYPE_REV,
            QUEUE_TYPE_DAY_LEARN_RELEARN,
            QUEUE_TYPE_PREVIEW
    })
    public @interface CARD_QUEUE {
    }



    // Card types
    public static final int CARD_TYPE_NEW = 0;
    public static final int CARD_TYPE_LRN = 1;
    public static final int CARD_TYPE_REV = 2;
    public static final int CARD_TYPE_RELEARNING = 3;



    @Retention(SOURCE)
    @IntDef( {CARD_TYPE_NEW, CARD_TYPE_LRN, CARD_TYPE_REV, CARD_TYPE_RELEARNING})
    public @interface CARD_TYPE {
    }



    // removal types
    public static final int REM_CARD = 0;
    public static final int REM_NOTE = 1;
    public static final int REM_DECK = 2;



    @Retention(SOURCE)
    @IntDef( {REM_CARD, REM_NOTE, REM_DECK})
    public @interface REM_TYPE {
    }



    // count display
    public static final int COUNT_ANSWERED = 0;
    public static final int COUNT_REMAINING = 1;

    // media log
    public static final int MEDIA_ADD = 0;
    public static final int MEDIA_REM = 1;

    // dynamic deck order
    public static final int DYN_OLDEST = 0;
    public static final int DYN_RANDOM = 1;
    public static final int DYN_SMALLINT = 2;
    public static final int DYN_BIGINT = 3;
    public static final int DYN_LAPSES = 4;
    public static final int DYN_ADDED = 5;
    public static final int DYN_DUE = 6;
    public static final int DYN_REVADDED = 7;
    public static final int DYN_DUEPRIORITY = 8;



    @Retention(SOURCE)
    @IntDef( {DYN_OLDEST, DYN_RANDOM, DYN_SMALLINT, DYN_BIGINT, DYN_LAPSES, DYN_ADDED, DYN_DUE, DYN_REVADDED, DYN_DUEPRIORITY})
    public @interface DYN_PRIORITY {
    }



    public static final int DYN_MAX_SIZE = 99999;

    // model types
    public static final int MODEL_STD = 0;
    public static final int MODEL_CLOZE = 1;



    @Retention(SOURCE)
    @IntDef( {MODEL_STD, MODEL_CLOZE})
    public @interface MODEL_TYPE {
    }



    public static final int STARTING_FACTOR = 2500;

    // deck schema & syncing vars
    public static final int SCHEMA_VERSION = 11;
    public static final int SYNC_ZIP_SIZE = (int) (2.5 * 1024 * 1024);
    public static final int SYNC_ZIP_SIZE_CHINA = (int) (50 * 1024 * 1024);
    public static final int SYNC_ZIP_COUNT = 25;
    public static final int SYNC_ZIP_COUNT_CHINA = 400;
    public static int LOGIN_SERVER = 0;
    public static int PRE_LOGIN_SERVER = 0;
    public static final int LOGIN_SERVER_NOT_LOGIN = 0;
    public static final int LOGIN_SERVER_ANKICHINA = 1;
    public static final int LOGIN_SERVER_ANKIWEB = 2;


    public static void saveAndUpdateLoginServer(int login_server) {
        PRE_LOGIN_SERVER = LOGIN_SERVER;
        LOGIN_SERVER = login_server;
    }


    public static void resumeLoginServer( ) {
        LOGIN_SERVER = PRE_LOGIN_SERVER;
    }


    public static boolean loginAnkiChina() {
        Timber.i("i am login in ankichina?%s", (LOGIN_SERVER == LOGIN_SERVER_ANKICHINA));
        return LOGIN_SERVER == LOGIN_SERVER_ANKICHINA;
    }


    public static boolean isLogin() {
        return LOGIN_SERVER != LOGIN_SERVER_NOT_LOGIN;
    }


    public static boolean loginAnkiWeb() {
        return LOGIN_SERVER == LOGIN_SERVER_ANKIWEB;
    }

    public static boolean savedAnkiWebAccount(SharedPreferences preferences){
        return !preferences.getString(Consts.KEY_SAVED_ANKI_WEB_ACCOUNT, "").isEmpty();
    }
    public static boolean savedAnkiChinaAccount(SharedPreferences preferences){
        return !preferences.getString(Consts.KEY_SAVED_ANKI_CHINA_PHONE, "").isEmpty();
    }
    public static final String KEY_MAIN_AD_TEXT = "KEY_MAIN_AD_TEXT";
    public static final String KEY_MAIN_AD_LINK = "KEY_MAIN_AD_LINK";
    public static final String KEY_AUTO_PLAY_TTS = "KEY_AUTO_PLAY_TTS";
    public static final String KEY_SELECT_ONLINE_SPEAK_ENGINE = "KEY_SELECT_ONLINE_SPEAK_ENGINE";
    public static final String KEY_SHOW_TTS_ICON = "KEY_SHOW_TTS_ICON";
    public static final String KEY_IS_VIP = "KEY_IS_VIP";
    public static final String KEY_SHOW_REMARK_TIP = "KEY_SHOW_REMARK_TIP";
    public static final String KEY_VIP_URL = "KEY_VIP_URL";
    public static final String KEY_REST_ONLINE_SPEAK_COUNT = "KEY_REST_ONLINE_SPEAK_COUNT";
    public static final String KEY_VIP_EXPIRED = "KEY_VIP_EXPIRED";
    public static final String KEY_LOCAL_LAYOUT_CONFIG = "KEY_LOCAL_LAYOUT_CONFIG";
    public static final String KEY_FLIP_CARD = "KEY_FLIP_CARD";
//    public static final String KEY_MAIN_AD_ENABLE = "KEY_MAIN_AD_ENABLE";
    public static final String KEY_ANKI_ACCOUNT_SERVER = "KEY_ANKI_ACCOUNT_SERVER";
    public static final String KEY_SELF_STUDYING_LIST = "KEY_SELF_STUDYING_LIST";
    public static final String KEY_SELF_STUDYING_LIST_INDEX = "KEY_SELF_STUDYING_LIST_INDEX";
    public static final String KEY_SYNC_CHINA_SESSION = "KEY_SYNC_CHINA_SESSION";
    public static final String KEY_LAST_STOP_TIME = "KEY_LAST_STOP_TIME";
    public static final String KEY_LAST_HINT_WHILE_NO_SPACE_TO_SYNC="KEY_LAST_HINT_WHILE_NO_SPACE_TO_SYNC";
    public static final String KEY_BE_VIP_HINT_COUNT="KEY_BE_VIP_HINT_COUNT";
    public static final String KEY_SAVED_ANKI_CHINA_PHONE = "KEY_SAVED_ANKI_CHINA_PHONE";
    public static final String KEY_SAVED_ANKI_CHINA_HKEY = "KEY_SAVED_ANKI_CHINA_HKEY";
    public static final String KEY_SAVED_ANKI_CHINA_TOKEN = "KEY_SAVED_ANKI_CHINA_TOKEN";
    public static final String KEY_SAVED_MODEL_KEY = "KEY_SAVED_MODEL_KEY";

    public static final String KEY_SAVED_ANKI_WEB_ACCOUNT = "KEY_SAVED_ANKI_WEB_ACCOUNT";
    public static final String KEY_SAVED_ANKI_WEB_HKEY = "KEY_SAVED_ANKI_WEB_HKEY";
    public static final String KEY_SAVED_ANKI_WEB_PASSWORD = "KEY_SAVED_ANKI_WEB_PASSWORD";
    public static final String SYNC_BASE = "https://sync%s.ankiweb.net/";
    public static final String SYNC_BASE_CHINA = "https://sync.ankichinas.com/";
    public static final String ANKI_CHINA_BASE = "https://api.ankichinas.com/";//正式域名
    public static final String BAIDU_AI_TOKEN_URL = "https://openapi.baidu.com/oauth/2.0/token?grant_type=client_credentials&client_id=seccGbksRb8hS4ipbzRxROhO&client_secret=xN1EnbXI91j8PSpOZ9lDU0jb3j0fGU86";//获取token
    public static final String BAIDU_AI_SPEAK_URL = "https://tsn.baidu.com/text2audio";//获取语音mp3
//    public static final String ANKI_CHINA_BASE = "https://dev-api.ankichinas.com/";//测试域名
    //    public static final String ANKI_CHINA_BASE = "https://dev-api.ankichinas.com/";//test
    public static final String API_VERSION = "api/v1/";
     //    public static final String SYNC_MEDIA_BASE = "https://sync.ankiweb.net/msync/";
    public static final String SYNC_MEDIA_BASE = "https://sync.ankichinas.com/msync/";
    public static final Integer DEFAULT_HOST_NUM = null;
    /* Note: 10 if using Rust backend, 9 if using Java. Set in BackendFactory.getInstance */
     public static int SYNC_VER = 9;
    public static final long UPLOAD_LIMIT_BANDWIDTH_BYTE = 20971520;
    public static final long DOWNLOAD_LIMIT_BANDWIDTH_BYTE = 20971520 / 8;
    //    public static final long UPLOAD_LIMIT_BANDWIDTH_BYTE = 100000;
//    public static final long DOWNLOAD_LIMIT_BANDWIDTH_BYTE = 100000;
    public static final String HELP_SITE = "http://ankisrs.net/docs/manual.html";
    public static final String URL_UPGRADE_CLOUD_SPACE = "https://www.ankichinas.com";
    public static final String URL_INSTRUCTION = "https://www.yuque.com/ankichina/lm007v";
    public static final String URL_ANKI_COURSE = "https://www.zhihu.com/question/28335314/answer/1491665578";
    public static final String URL_VOLUNTEER = "https://zhuanlan.zhihu.com/p/258620777";
    public static final String URL_VERSION = "https://www.yuque.com/ankichina/lm007v/bgw2h3";
    public static final String URL_FEEDBACK = "https://www.yuque.com/ankichina/lm007v/ukdigq";
    public static final String URL_USER_PROTOCOL = "https://other.ankichinas.cn/agreement.html";
    public static final String URL_PRIVATE = "https://other.ankichinas.cn/privacy.html";

    // Leech actions
    public static final int LEECH_SUSPEND = 0;
    public static final int LEECH_TAGONLY = 1;

    // Buttons
    public static final int BUTTON_ONE = 1;
    public static final int BUTTON_TWO = 2;
    public static final int BUTTON_THREE = 3;
    public static final int BUTTON_FOUR = 4;



    @Retention(SOURCE)
    @IntDef( {BUTTON_ONE, BUTTON_TWO, BUTTON_THREE, BUTTON_FOUR})
    public @interface BUTTON_TYPE {
    }



    // Revlog types
    // They are the same as Card Type except for CRAM. So one type may switch from one to other type
    public static final int REVLOG_LRN = 0;
    public static final int REVLOG_REV = 1;
    public static final int REVLOG_RELRN = 2;
    public static final int REVLOG_CRAM = 3;



    @Retention(SOURCE)
    @IntDef( {REVLOG_LRN, REVLOG_REV, REVLOG_RELRN, REVLOG_CRAM})
    public @interface REVLOG_TYPE {
    }

    // The labels defined in consts.py are in AnkiDroid's resources files.

    public static final long DEFAULT_DECK_ID = 1;
    /**
     * Default dconf - can't be removed
     */
    public static final long DEFAULT_DECK_CONFIG_ID = 1;

    public static final String FIELD_SEPARATOR = Character.toString('\u001f');
}
