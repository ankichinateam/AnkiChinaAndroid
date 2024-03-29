
package com.ichi2.anki.dialogs;

import android.net.Uri;
import android.os.Bundle;
import android.os.Message;

import com.afollestad.materialdialogs.MaterialDialog;
import com.ichi2.anki.AnkiActivity;
import com.ichi2.anki.DeckPicker;
import com.ichi2.anki.DeckPickerFragment;
import com.ichi2.anki.R;
import com.ichi2.libanki.Collection;

import androidx.annotation.Nullable;

public class SyncErrorDialog extends AsyncDialogFragment {
    public static final int DIALOG_USER_NOT_LOGGED_IN_SYNC = 0;
    public static final int DIALOG_CONNECTION_ERROR = 1;
    public static final int DIALOG_SYNC_CONFLICT_RESOLUTION = 2;
    public static final int DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL = 3;
    public static final int DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE = 4;
    public static final int DIALOG_SYNC_SANITY_ERROR = 6;
    public static final int DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL = 7;
    public static final int DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_REMOTE = 8;
    public static final int DIALOG_MEDIA_SYNC_ERROR = 9;
    public static final int DIALOG_SYNC_CORRUPT_COLLECTION = 10;
	public static final int DIALOG_NO_ENOUGH_SERVER_SPACE = 11;
    public interface SyncErrorDialogListener {
        void showSyncErrorDialog(int dialogType);
        void showSyncErrorDialog(int dialogType, String message);
        void loginToSyncServer();
        void goToUpgradeSpace();
        void sync();
        void sync(String conflict);
        Collection getCol();
        void mediaCheck();
        void dismissAllDialogFragments();
    }


    public void setSyncErrorDialogListener(SyncErrorDialogListener syncErrorDialogListener) {
        this.syncErrorDialogListener = syncErrorDialogListener;
    }


    SyncErrorDialogListener syncErrorDialogListener;
    
    /**
     * A set of dialogs belonging to AnkiActivity which deal with sync problems
     * 
     * @param dialogType An integer which specifies which of the sub-dialogs to show
     * @param dialogMessage A string which can be optionally used to set the dialog message
     */
    public static SyncErrorDialog newInstance(int dialogType, String dialogMessage,SyncErrorDialogListener listener) {
        SyncErrorDialog f = new SyncErrorDialog();
        Bundle args = new Bundle();
        args.putInt("dialogType", dialogType);
        args.putString("dialogMessage", dialogMessage);
        f.setArguments(args);
        f.setSyncErrorDialogListener(listener);
        return f;
    }


    @Override
    public MaterialDialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity())
                .title(getTitle())
                .content(getMessage())
                .cancelable(true);

        switch (getArguments().getInt("dialogType")) {
            case DIALOG_USER_NOT_LOGGED_IN_SYNC: {
                // User not logged in; take them to login screen
                return builder.iconAttr(R.attr.dialogSyncErrorIcon)
                        .positiveText(res().getString(R.string.log_in))
                        .negativeText(res().getString(R.string.dialog_cancel))
                        .onPositive((dialog, which) -> {
                            if(syncErrorDialogListener!=null)syncErrorDialogListener.loginToSyncServer();
                        })
                        .show();
            }
            case DIALOG_NO_ENOUGH_SERVER_SPACE: {
                // User not logged in; take them to login screen
                return builder.iconAttr(R.attr.dialogSyncErrorIcon)
                        .positiveText(res().getString(R.string.upgrade))
                        .negativeText(res().getString(R.string.dialog_cancel))
                        .onPositive((dialog, which) -> {
                            if(syncErrorDialogListener!=null)syncErrorDialogListener.goToUpgradeSpace();
                        })
                        .show();
            }
            case DIALOG_CONNECTION_ERROR: {
                // Connection error; allow user to retry or cancel
                return builder.iconAttr(R.attr.dialogSyncErrorIcon)
                        .positiveText(res().getString(R.string.retry))
                        .negativeText(res().getString(R.string.dialog_cancel))
                        .onPositive((dialog, which) -> {
                            syncErrorDialogListener.sync();
                            dismissAllDialogFragments();
                        })
                        .onNegative((dialog, which) -> dismissAllDialogFragments())
                        .show();
            }
            case DIALOG_SYNC_CONFLICT_RESOLUTION: {
                // Sync conflict; allow user to cancel, or choose between local and remote versions
                return builder.iconAttr(R.attr.dialogSyncErrorIcon)
                        .positiveText(res().getString(R.string.sync_conflict_local))
                        .negativeText(res().getString(R.string.sync_conflict_remote))
                        .neutralText(res().getString(R.string.dialog_cancel))
                        .onPositive((dialog, which) -> {
                            if(syncErrorDialogListener!=null)syncErrorDialogListener
                                    .showSyncErrorDialog(DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL);
                        })
                        .onNegative((dialog, which) -> {
                            if(syncErrorDialogListener!=null)syncErrorDialogListener
                                    .showSyncErrorDialog(DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE);
                        })
                        .onNeutral((dialog, which) -> dismissAllDialogFragments())
                        .show();
            }
            case DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL:
            case DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL: {
                // Confirmation before pushing local collection to server after sync conflict
                return builder.positiveText(res().getString(R.string.dialog_positive_overwrite))
                        .negativeText(res().getString(R.string.dialog_cancel))
                        .onPositive((dialog, which) -> {
                            syncErrorDialogListener.sync("upload");
                            dismissAllDialogFragments();
                        })
                        .show();
            }
            case DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE:
            case DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_REMOTE: {
                // Confirmation before overwriting local collection with server collection after sync conflict
                return builder.positiveText(res().getString(R.string.dialog_positive_overwrite))
                        .negativeText(res().getString(R.string.dialog_cancel))
                        .onPositive((dialog, which) -> {
                            syncErrorDialogListener.sync("download");
                            dismissAllDialogFragments();
                        })
                        .show();
            }
            case DIALOG_SYNC_SANITY_ERROR: {
                // Sync sanity check error; allow user to cancel, or choose between local and remote versions
                return builder.positiveText(res().getString(R.string.sync_sanity_local))
                        .neutralText(res().getString(R.string.sync_sanity_remote))
                        .negativeText(res().getString(R.string.dialog_cancel))
                        .onPositive((dialog, which) ->{
                            if(syncErrorDialogListener!=null) syncErrorDialogListener
                                    .showSyncErrorDialog(DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL);
                        })
                        .onNeutral((dialog, which) -> {
                            if(syncErrorDialogListener!=null)syncErrorDialogListener
                                    .showSyncErrorDialog(DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_REMOTE);
                        })
                        .show();
            }// Confirmation before pushing local collection to server after sanity check error
// Confirmation before overwriting local collection with server collection after sanity check error
            case DIALOG_MEDIA_SYNC_ERROR: {
                return builder.positiveText(R.string.check_media)
                        .negativeText(R.string.cancel)
                        .onPositive((dialog, which) -> {
                            syncErrorDialogListener.mediaCheck();
                            dismissAllDialogFragments();
                        })
                        .show();
            }
            case DIALOG_SYNC_CORRUPT_COLLECTION: {
                return
                        builder.positiveText(R.string.dialog_ok)
                        .neutralText(R.string.sync_corrupt_collection_get_help)
                        .onNeutral((dialog, which) -> ((AnkiActivity)(requireActivity())).openUrl(Uri.parse(getString(R.string.repair_deck))))
                        .cancelable(false)
                        .show();

            }
            default:
                return null;
        }
    }


    private String getTitle() {
        switch (getArguments().getInt("dialogType")) {
            case DIALOG_USER_NOT_LOGGED_IN_SYNC:
                return res().getString(R.string.not_logged_in_title);
            case DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL:
            case DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE:
            case DIALOG_SYNC_CONFLICT_RESOLUTION:
                return res().getString(R.string.sync_conflict_title);
                case DIALOG_NO_ENOUGH_SERVER_SPACE:
                return res().getString(R.string.please_upgrade_cloud_space);
            default:
                return res().getString(R.string.sync_error);
        }
    }


    /**
     * Get the title which is shown in notification bar when dialog fragment can't be shown
     * 
     * @return tile to be shown in notification in bar
     */
    @Override
    public String getNotificationTitle() {
        switch (getArguments().getInt("dialogType")) {
            case DIALOG_USER_NOT_LOGGED_IN_SYNC:
                return res().getString(R.string.sync_error);
            default:
                return getTitle();
        }
    }

    @Nullable
    private String getMessage() {
        switch (getArguments().getInt("dialogType")) {
            case DIALOG_USER_NOT_LOGGED_IN_SYNC:
                return res().getString(R.string.login_create_account_message);
            case DIALOG_CONNECTION_ERROR:
                return res().getString(R.string.connection_error_message);
            case DIALOG_SYNC_CONFLICT_RESOLUTION:
                return res().getString(R.string.sync_conflict_message);
            case DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_LOCAL:
                return res().getString(R.string.sync_conflict_local_confirm);
            case DIALOG_SYNC_CONFLICT_CONFIRM_KEEP_REMOTE:
                return res().getString(R.string.sync_conflict_remote_confirm);
            case DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_LOCAL:
                return res().getString(R.string.sync_conflict_local_confirm);
            case DIALOG_SYNC_SANITY_ERROR_CONFIRM_KEEP_REMOTE:
                return res().getString(R.string.sync_conflict_remote_confirm);
            case DIALOG_SYNC_CORRUPT_COLLECTION: {
                String syncMessage = getArguments().getString("dialogMessage");
                String repairUrl = getString(R.string.repair_deck);
                String dialogMessage = getString(R.string.sync_corrupt_database, repairUrl);
                return DeckPicker.joinSyncMessages(dialogMessage, syncMessage);
            }

            default:
                return getArguments().getString("dialogMessage");
        }
    }


    /**
     * Get the message which is shown in notification bar when dialog fragment can't be shown
     * 
     * @return message to be shown in notification in bar
     */
    @Override
    public String getNotificationMessage() {
        switch (getArguments().getInt("dialogType")) {
            case DIALOG_USER_NOT_LOGGED_IN_SYNC:
                return res().getString(R.string.not_logged_in_title);
            default:
                return getMessage();
        }
    }

    @Override
    public Message getDialogHandlerMessage() {
        Message msg = Message.obtain();
        msg.what = DialogHandler.MSG_SHOW_SYNC_ERROR_DIALOG;
        Bundle b = new Bundle();
        b.putInt("dialogType", getArguments().getInt("dialogType"));
        b.putString("dialogMessage", getArguments().getString("dialogMessage"));
        msg.setData(b);
        return msg;
    }

    public void dismissAllDialogFragments() {
        syncErrorDialogListener.dismissAllDialogFragments();
    }
}
