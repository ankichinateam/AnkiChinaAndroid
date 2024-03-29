
package com.ichi2.anki.dialogs;

import android.content.res.Resources;
import android.os.Bundle;

import com.afollestad.materialdialogs.MaterialDialog;
import com.ichi2.anki.CollectionHelper;
import com.ichi2.anki.R;
import com.ichi2.anki.UIUtils;
import com.ichi2.anki.analytics.AnalyticsDialogFragment;
import com.ichi2.libanki.Utils;
import com.ichi2.utils.ImportUtils;

import java.io.File;
import java.net.URLDecoder;
import java.util.List;

import timber.log.Timber;

public class ImportDialog extends AsyncDialogFragment {

    public static final int DIALOG_IMPORT_HINT = 0;
    public static final int DIALOG_IMPORT_SELECT = 1;
    public static final int DIALOG_IMPORT_ADD_CONFIRM = 2;
    public static final int DIALOG_IMPORT_REPLACE_CONFIRM = 3;

    public interface ImportDialogListener {
        void showImportDialog(int id, String message);
        void showImportDialog(int id);
        void importAdd(String importPath);
        void importReplace(String importPath);
        void dismissAllDialogFragments();
    }


    public void setImportDialogListener(ImportDialogListener mImportDialogListener) {
        this.mImportDialogListener = mImportDialogListener;
    }


    ImportDialogListener mImportDialogListener;

    /**
     * A set of dialogs which deal with importing a file
     * 
     * @param dialogType An integer which specifies which of the sub-dialogs to show
     * @param dialogMessage An optional string which can be used to show a custom message
     * or specify import path
     */
    public static ImportDialog newInstance(int dialogType, String dialogMessage,ImportDialogListener listener) {
        ImportDialog f = new ImportDialog();
        Bundle args = new Bundle();
        args.putInt("dialogType", dialogType);
        args.putString("dialogMessage", dialogMessage);
        f.setArguments(args);
        f.setImportDialogListener(listener);
        return f;
    }


    @Override
    public MaterialDialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int mType = getArguments().getInt("dialogType");
        Resources res = getResources();
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        builder.cancelable(true);

        switch (mType) {
            case DIALOG_IMPORT_HINT: {
                // Instruct the user that they need to put their APKG files into the AnkiDroid directory
                return builder.title(res.getString(R.string.import_title))
                        .content(res.getString(R.string.import_hint, CollectionHelper.getCurrentAnkiDroidDirectory(getActivity())))
                        .positiveText(res.getString(R.string.dialog_ok))
                        .negativeText(res.getString(R.string.dialog_cancel))
                        .onPositive((dialog, which) -> mImportDialogListener.showImportDialog(DIALOG_IMPORT_SELECT))
                        .onNegative((dialog, which) -> dismissAllDialogFragments())
                        .show();
            }
            case DIALOG_IMPORT_SELECT: {
                // Allow user to choose from the list of available APKG files
                List<File> fileList = Utils.getImportableDecks(getActivity());
                if (fileList.size() == 0) {
                    UIUtils.showThemedToast(getActivity(),
                            getResources().getString(R.string.upgrade_import_no_file_found, "'.apkg'"), false);
                    return builder.showListener(dialog -> dialog.cancel()).show();
                } else {
                    String[] tts = new String[fileList.size()];
                    final String[] importValues = new String[fileList.size()];
                    for (int i = 0; i < tts.length; i++) {
                        tts[i] = fileList.get(i).getName();
                        importValues[i] = fileList.get(i).getAbsolutePath();
                    }
                    return builder.title(res.getString(R.string.import_select_title))
                            .items(tts)
                            .itemsCallback((materialDialog, view, i, charSequence) -> {
                                String importPath = importValues[i];
                                // If collection package, we assume the collection will be replaced
                                if (ImportUtils.isCollectionPackage(filenameFromPath(importPath))) {
                                    mImportDialogListener.showImportDialog(DIALOG_IMPORT_REPLACE_CONFIRM, importPath);
                                    // Otherwise we add the file since exported decks / shared decks can't be imported via replace anyway
                                } else {
                                    mImportDialogListener.showImportDialog(DIALOG_IMPORT_ADD_CONFIRM, importPath);
                                }
                            })
                            .show();
                }
            }
            case DIALOG_IMPORT_ADD_CONFIRM: {
                String displayFileName = convertToDisplayName(getArguments().getString("dialogMessage"));
                return builder.title(res.getString(R.string.import_title))
                        .content(res.getString(R.string.import_message_add_confirm, filenameFromPath(displayFileName)))
                        .positiveText(res.getString(R.string.import_message_add))
                        .negativeText(res.getString(R.string.dialog_cancel))
                        .onPositive((dialog, which) -> {
                            mImportDialogListener.importAdd(getArguments().getString("dialogMessage"));
                            dismissAllDialogFragments();
                        })
                        .show();
            }
            case DIALOG_IMPORT_REPLACE_CONFIRM: {
                String displayFileName = convertToDisplayName(getArguments().getString("dialogMessage"));
                return builder.title(res.getString(R.string.import_title))
                        .content(res.getString(R.string.import_message_replace_confirm, displayFileName))
                        .positiveText(res.getString(R.string.dialog_positive_replace))
                        .negativeText(res.getString(R.string.dialog_cancel))
                        .onPositive((dialog, which) -> {
                            mImportDialogListener.importReplace(getArguments().getString("dialogMessage"));
                            dismissAllDialogFragments();
                        })
                        .show();
            }
            default:
                return null;
        }
    }


    private String convertToDisplayName(String name) {
        //ImportUtils URLEncodes names, which isn't great for display.
        //NICE_TO_HAVE: Pass in the DisplayFileName closer to the source of the bad data, rather than fixing it here.
        try {
            return URLDecoder.decode(name, "UTF-8");
        } catch (Exception e) {
            Timber.w("Failed to convert filename to displayable string");
            return name;
        }
    }


    @Override
    public String getNotificationMessage() {
        return res().getString(R.string.import_interrupted);
    }

    @Override
    public String getNotificationTitle() {
        return res().getString(R.string.import_title);
    }
    
    public void dismissAllDialogFragments() {
        mImportDialogListener.dismissAllDialogFragments();        
    }

    private static String filenameFromPath (String path) {
        return path.split("/")[path.split("/").length - 1];
    }
}
