/***************************************************************************************
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

package com.ichi2.libanki;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.ichi2.anki.AnkiDroidApp;
import com.ichi2.anki.exception.ConfirmModSchemaException;
import com.ichi2.libanki.utils.SystemTime;
import com.ichi2.libanki.utils.Time;
import com.ichi2.utils.JSONArray;
import com.ichi2.utils.JSONException;
import com.ichi2.utils.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

@SuppressWarnings( {"PMD.AvoidThrowingRawExceptionTypes", "PMD.AvoidReassigningParameters",
        "PMD.NPathComplexity", "PMD.MethodNamingConventions", "PMD.ExcessiveMethodLength", "PMD.OneDeclarationPerLine",
        "PMD.SwitchStmtsShouldHaveDefault", "PMD.EmptyIfStmt", "PMD.SimplifyBooleanReturns", "PMD.CollapsibleIfStatements"})
public class Storage {


    /* Open a new or existing collection. Path must be unicode */
    public static Collection Collection(Context context, String path) {
        return Collection(context, path, false, false);
    }

    public static Collection Collection(Context context, String path, boolean server, boolean log) {
        return Collection(context, path, server, log, new SystemTime());
    }


    public static Collection Collection(Context context, String path, boolean server, boolean log, @NonNull Time time) {
        assert path.endsWith(".anki2");
        File dbFile = new File(path);
        boolean create = !dbFile.exists();
//        // connect
        DB db = new DB(path);
        try {
            // initialize
            int ver;
            Timber.i("new collection:%s", create);
            if (create) {
                AnkiDroidApp.getSharedPrefs(context).edit().remove(Consts.KEY_SYNC_CHINA_SESSION).apply();
                ver = _createDB(db, time );
            } else {
                ver = _upgradeSchema(db, time);
            }
            db.execute("PRAGMA temp_store = memory");
            // add db to col and do any remaining upgrades
            Collection col = new Collection(context, db, path, server, log, time );
            if (ver < Consts.SCHEMA_VERSION) {
                AnkiDroidApp.getSharedPrefs(context).edit().remove(Consts.KEY_SYNC_CHINA_SESSION).apply();
                _upgrade(col, ver);
            } else if (ver > Consts.SCHEMA_VERSION) {
                if (ver == 12 && _isVer11(db)) {
                    //判断数据库版本是否有问题
                    db.execute("UPDATE col SET ver = 11");
                } else {
                    throw new RuntimeException("This file requires a newer version of Anki.");
                }
            } else if (create) {
                // add in reverse order so basic is default
                for (int i = StdModels.stdModels.length - 1; i >= 0; i--) {
                    StdModels.stdModels[i].add(col);
                }
                col.save();
            }
            return col;
        } catch (Exception e) {
            Timber.e(e, "Error opening collection; closing database");
            db.close();
            throw e;
        }
    }


    private static boolean _isVer11(DB db) {
        //检查col
        //检查notes
        //检查graves
        //检查revlog
        //检查synclog
        //检查cards
        String[] colNames = new String[] {"id", "crt", "mod", "scm", "ver", "dty", "usn", "ls", "conf", "models", "decks", "dconf", "tags"};
        String[] notesNames = new String[] {"id", "guid", "mid", "mod", "usn", "tags", "flds", "sfld", "csum", "flags", "data"};
        String[] cardsNames = new String[] {"id", "nid", "did", "ord", "mod", "usn", "type", "queue", "due", "ivl", "factor", "reps", "lapses", "left", "odue", "odid", "flags", "data"};
        String[] gravesNames = new String[] {"usn", "oid", "type"};
        String[] synclogNames = new String[] {"id", "type", "mod"};
        String[] revlogNames = new String[] {"id", "cid", "usn", "ease", "ivl", "lastIvl", "factor", "time", "type"};

        return tableMatch(db, "col", colNames) && tableMatch(db, "notes", notesNames) && tableMatch(db, "cards", cardsNames) && tableMatch(db, "graves", gravesNames) && tableMatch(db, "synclog", synclogNames) && tableMatch(db, "revlog", revlogNames);
    }


    private static boolean tableMatch(DB db, String tableName, String[] columnNames) {
        tableName = tableName.toLowerCase();
        String sql = String.format("PRAGMA table_info('%s')", tableName);
        try (Cursor cursor = db.getDatabase().query(sql, null)) {

            if (cursor.getCount() == columnNames.length) {
                int i = 0;
                while (cursor.moveToNext()) {
                    if (!columnNames[i].equals(cursor.getString(cursor.getColumnIndex("name")))) {
                        return false;
                    }
                    i++;
                }
                return true;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return false;
    }




    private static int _upgradeSchema(DB db, @NonNull Time time) {
        Timber.i("_upgradeSchema db");
        db.execute("create table if not exists remarks (" + "    id             integer primary key,"
                + "    cid             integer not null,"+ "    content             text not null," + "    mod             integer not null" + ")");
        int ver = db.queryScalar("SELECT ver FROM col");
        if (ver == Consts.SCHEMA_VERSION) {
            return ver;
        }
        // add odid to cards, edue->odue
        if (db.queryScalar("SELECT ver FROM col") == 1) {
            db.execute("ALTER TABLE cards RENAME TO cards2");
            _addSchema(db, false, time);
            db.execute("insert into cards select id, nid, did, ord, mod, usn, type, queue, due, ivl, factor, reps, lapses, left, edue, 0, flags, data from cards2");
            db.execute("DROP TABLE cards2");
            db.execute("UPDATE col SET ver = 2");
            _updateIndices(db);
        }
        // remove did from notes
        if (db.queryScalar("SELECT ver FROM col") == 2) {
            db.execute("ALTER TABLE notes RENAME TO notes2");
            _addSchema(db, time);
            db.execute("insert into notes select id, guid, mid, mod, usn, tags, flds, sfld, csum, flags, data from notes2");
            db.execute("DROP TABLE notes2");
            db.execute("UPDATE col SET ver = 3");
            _updateIndices(db);
        }
        return ver;
    }


    private static void _upgrade(Collection col, int ver) {
        try {
            if (ver < 3) {
                // new deck properties
                for (Deck d : col.getDecks().all()) {
                    d.put("dyn", 0);
                    d.put("collapsed", false);
                    col.getDecks().save(d);
                }
            }
            if (ver < 4) {
                col.modSchemaNoCheck();
                ArrayList<Model> clozes = new ArrayList<>();
                for (Model m : col.getModels().all()) {
                    if (!m.getJSONArray("tmpls").getJSONObject(0).getString("qfmt").contains("{{cloze:")) {
                        m.put("type", Consts.MODEL_STD);
                    } else {
                        clozes.add(m);
                    }
                }
                for (Model m : clozes) {
                    try {
                        _upgradeClozeModel(col, m);
                    } catch (ConfirmModSchemaException e) {
                        // Will never be reached as we already set modSchemaNoCheck()
                        throw new RuntimeException(e);
                    }
                }
                col.getDb().execute("UPDATE col SET ver = 4");
            }
            if (ver < 5) {
                col.getDb().execute("UPDATE cards SET odue = 0 WHERE queue = 2");
                col.getDb().execute("UPDATE col SET ver = 5");
            }
            if (ver < 6) {
                col.modSchemaNoCheck();
                for (Model m : col.getModels().all()) {
                    m.put("css", new JSONObject(Models.defaultModel).getString("css"));
                    JSONArray ar = m.getJSONArray("tmpls");
                    for (int i = 0; i < ar.length(); i++) {
                        JSONObject t = ar.getJSONObject(i);
                        if (!t.has("css")) {
                            continue;
                        }
                        m.put("css",
                                m.getString("css") + "\n"
                                        + t.getString("css").replace(".card ", ".card" + t.getInt("ord") + 1));
                        t.remove("css");
                    }
                    col.getModels().save(m);
                }
                col.getDb().execute("UPDATE col SET ver = 6");
            }
            if (ver < 7) {
                col.modSchemaNoCheck();
                col.getDb().execute("UPDATE cards SET odue = 0 WHERE (type = " + Consts.CARD_TYPE_LRN + " OR queue = 2) AND NOT odid");
                col.getDb().execute("UPDATE col SET ver = 7");
            }
            if (ver < 8) {
                col.modSchemaNoCheck();
                col.getDb().execute("UPDATE cards SET due = due / 1000 WHERE due > 4294967296");
                col.getDb().execute("UPDATE col SET ver = 8");
            }
            if (ver < 9) {
                col.getDb().execute("UPDATE col SET ver = 9");
            }
            if (ver < 10) {
                col.getDb().execute("UPDATE cards SET left = left + left * 1000 WHERE queue = " + Consts.QUEUE_TYPE_LRN);
                col.getDb().execute("UPDATE col SET ver = 10");
            }
            if (ver < 11) {
                col.modSchemaNoCheck();
                for (Deck d : col.getDecks().all()) {
                    if (d.getInt("dyn") != 0) {
                        int order = d.getInt("order");
                        // failed order was removed
                        if (order >= 5) {
                            order -= 1;
                        }
                        JSONArray terms = new JSONArray(Arrays.asList(d.getString("search"),
                                d.getInt("limit"), order));
                        d.put("terms", new JSONArray());
                        d.getJSONArray("terms").put(0, terms);
                        d.remove("search");
                        d.remove("limit");
                        d.remove("order");
                        d.put("resched", true);
                        d.put("return", true);
                    } else {
                        if (!d.has("extendNew")) {
                            d.put("extendNew", 10);
                            d.put("extendRev", 50);
                        }
                    }
                    col.getDecks().save(d);
                }
                for (DeckConfig c : col.getDecks().allConf()) {
                    JSONObject r = c.getJSONObject("rev");
                    r.put("ivlFct", r.optDouble("ivlFct", 1));
                    if (r.has("ivlfct")) {
                        r.remove("ivlfct");
                    }
                    r.put("maxIvl", 36500);
                    col.getDecks().save(c);
                }
                for (Model m : col.getModels().all()) {
                    JSONArray tmpls = m.getJSONArray("tmpls");
                    for (int ti = 0; ti < tmpls.length(); ++ti) {
                        JSONObject t = tmpls.getJSONObject(ti);
                        t.put("bqfmt", "");
                        t.put("bafmt", "");
                    }
                    col.getModels().save(m);
                }
                col.getDb().execute("update col set ver = 11");
            }
//            if (ver < 12) {
//                col.getDb().execute("create table if not exists synclog (" + "    id             integer not null,"
//                        + "    type             integer not null,"+ "    mod             integer not null" + ")");
//                col.getDb().execute("update col set ver = 12");
//                _updateIndices(col.getDb());
//            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    private static void _upgradeClozeModel(Collection col, Model m) throws ConfirmModSchemaException {
        m.put("type", Consts.MODEL_CLOZE);
        // convert first template
        JSONObject t = m.getJSONArray("tmpls").getJSONObject(0);
        for (String type : new String[] {"qfmt", "afmt"}) {
            t.put(type, t.getString(type).replaceAll("\\{\\{cloze:1:(.+?)\\}\\}", "{{cloze:$1}}"));
        }
        t.put("name", "Cloze");
        // delete non-cloze cards for the model
        JSONArray tmpls = m.getJSONArray("tmpls");
        ArrayList<JSONObject> rem = new ArrayList<>();
        for (int i = 1; i < tmpls.length(); i++) {
            JSONObject ta = tmpls.getJSONObject(i);
            if (!ta.getString("afmt").contains("{{cloze:")) {
                rem.add(ta);
            }
        }
        for (JSONObject r : rem) {
            col.getModels().remTemplate(m, r);
        }
        JSONArray newTmpls = new JSONArray();
        newTmpls.put(tmpls.getJSONObject(0));
        m.put("tmpls", newTmpls);
        col.getModels()._updateTemplOrds(m);
        col.getModels().save(m);

    }


    private static int _createDB(DB db, @NonNull Time time) {
        db.execute("PRAGMA page_size = 4096");
        db.execute("PRAGMA legacy_file_format = 0");
        db.execute("VACUUM");
        _addSchema(db, time);
        _updateIndices(db);
        db.execute("ANALYZE");
        return Consts.SCHEMA_VERSION;
    }


    private static void _addSchema(DB db, @NonNull Time time) {
        _addSchema(db, true, time);
    }


    private static void _addSchema(DB db, boolean setColConf, @NonNull Time time) {
        db.execute("create table if not exists col ( " + "id              integer primary key, "
                + "crt             integer not null," + "mod             integer not null,"
                + "scm             integer not null," + "ver             integer not null,"
                + "dty             integer not null," + "usn             integer not null,"
                + "ls              integer not null," + "conf            text not null,"
                + "models          text not null," + "decks           text not null,"
                + "dconf           text not null," + "tags            text not null" + ");");
        db.execute("create table if not exists notes (" + "   id              integer primary key,   /* 0 */"
                + "  guid            text not null,   /* 1 */" + " mid             integer not null,   /* 2 */"
                + " mod             integer not null,   /* 3 */" + " usn             integer not null,   /* 4 */"
                + " tags            text not null,   /* 5 */" + " flds            text not null,   /* 6 */"
                + " sfld            integer not null,   /* 7 */" + " csum            integer not null,   /* 8 */"
                + " flags           integer not null,   /* 9 */" + " data            text not null   /* 10 */" + ");");
        db.execute("create table if not exists cards (" + "   id              integer primary key,   /* 0 */"
                + "  nid             integer not null,   /* 1 */" + "  did             integer not null,   /* 2 */"
                + "  ord             integer not null,   /* 3 */" + "  mod             integer not null,   /* 4 */"
                + " usn             integer not null,   /* 5 */" + " type            integer not null,   /* 6 */"
                + " queue           integer not null,   /* 7 */" + "    due             integer not null,   /* 8 */"
                + "   ivl             integer not null,   /* 9 */" + "  factor          integer not null,   /* 10 */"
                + " reps            integer not null,   /* 11 */" + "   lapses          integer not null,   /* 12 */"
                + "   left            integer not null,   /* 13 */" + "   odue            integer not null,   /* 14 */"
                + "   odid            integer not null,   /* 15 */" + "   flags           integer not null,   /* 16 */"
                + "   data            text not null   /* 17 */" + ");");
        db.execute("create table if not exists revlog (" + "   id              integer primary key,"
                + "   cid             integer not null," + "   usn             integer not null,"
                + "   ease            integer not null," + "   ivl             integer not null,"
                + "   lastIvl         integer not null," + "   factor          integer not null,"
                + "   time            integer not null," + "   type            integer not null" + ");");
        db.execute("create table if not exists graves (" + "    usn             integer not null,"
                + "    oid             integer not null," + "    type            integer not null" + ")");
        db.execute("create table if not exists synclog (" + "    id             integer not null,"
                + "    type             integer not null," + "    mod             integer not null" + ")");
        db.execute("create table if not exists remarks (" + "    id             integer primary key,"
                + "    cid             integer not null,"+ "    content             text not null," + "    mod             integer not null" + ")");
        db.execute("INSERT OR IGNORE INTO col VALUES(1,0,0," +
                time.intTimeMS() + "," + Consts.SCHEMA_VERSION +
                ",0,0,0,'','{}','','','{}')");

        if (setColConf) {
            _setColVars(db, time);
        }
    }


    private static void _setColVars(DB db, @NonNull Time time) {
        JSONObject g = new JSONObject(Decks.defaultDeck);
        g.put("id", 1);
        g.put("name", "Default");
        g.put("conf", 1);
        g.put("mod", time.intTime());
        JSONObject gc = new JSONObject(Decks.defaultConf);
        gc.put("id", 1);
        JSONObject ag = new JSONObject();
        ag.put("1", g);
        JSONObject agc = new JSONObject();
        agc.put("1", gc);
        ContentValues values = new ContentValues();
        values.put("conf", Collection.defaultConf);
        values.put("decks", Utils.jsonToString(ag));
        values.put("dconf", Utils.jsonToString(agc));
        db.update("col", values);
    }


    private static void _updateIndices(DB db) {
        Timber.e("update indices");
        db.execute("create index if not exists ix_notes_usn on notes (usn);");
        db.execute("create index if not exists ix_cards_usn on cards (usn);");
        db.execute("create index if not exists ix_revlog_usn on revlog (usn);");
        db.execute("create index if not exists ix_cards_nid on cards (nid);");
        db.execute("create index if not exists ix_cards_sched on cards (did, queue, due);");
        db.execute("create index if not exists ix_revlog_cid on revlog (cid);");
        db.execute("create index if not exists ix_notes_csum on notes (csum);)");
        db.execute("create index if not exists ix_synclog on synclog (id,type);)");
        db.execute("create index if not exists ix_synclog_id on synclog (id);)");
        db.execute("create index if not exists ix_remarks_cid on remarks (cid);)");
        db.execute("create index if not exists ix_cards_id on cards (id);)");
        db.execute("create index if not exists ix_notes_id on notes (id);)");
    }


    public static void addIndices(DB db) {
        _updateIndices(db);
    }



}
