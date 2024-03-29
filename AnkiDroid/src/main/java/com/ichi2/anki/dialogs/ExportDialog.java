
package com.ichi2.anki.dialogs;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.ichi2.anki.R;
import com.ichi2.anki.analytics.AnalyticsDialogFragment;

public class ExportDialog extends AnalyticsDialogFragment {

    public interface ExportDialogListener {

        void exportApkg(String path, Long did, boolean includeSched, boolean includeMedia,boolean exportCard,boolean exportApkg);
        void dismissAllDialogFragments();
    }


    public void setListener(ExportDialogListener listener) {
        this.listener = listener;
    }


    private ExportDialogListener listener;
    private final int INCLUDE_SCHED = 0;
    private final int INCLUDE_MEDIA = 1;
    private final int EXPORT_APKG = 2;
    private final int EXPORT_CARD = 3;
    private boolean mIncludeSched = false;
    private boolean mIncludeMedia = false;
    private boolean mExportCard = false;
    private boolean mExportApkg = true;


    /**
     * A set of dialogs which deal with importing a file
     * 
     * @param did An integer which specifies which of the sub-dialogs to show
     * @param dialogMessage An optional string which can be used to show a custom message or specify import path
     */
    public static ExportDialog newInstance(@NonNull String dialogMessage, Long did,ExportDialogListener listener) {
        ExportDialog f = new ExportDialog();
        Bundle args = new Bundle();
        args.putLong("did", did);
        args.putString("dialogMessage", dialogMessage);
        f.setArguments(args);
        f.setListener(listener);
        return f;
    }


    public static ExportDialog newInstance(@NonNull String dialogMessage,ExportDialogListener listener) {
        ExportDialog f = new ExportDialog();
        Bundle args = new Bundle();
        args.putString("dialogMessage", dialogMessage);
        f.setArguments(args);
        f.setListener(listener);
        return f;
    }


    @Override
    public MaterialDialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources res = getResources();
        final Long did = getArguments().getLong("did", -1L);
        Integer[] checked;
        final String[] items;
        if (did != -1L) {
            mIncludeSched = false;
            checked = new Integer[]{EXPORT_APKG};
            items = new String[] {res.getString(R.string.export_include_schedule),
                     res.getString(R.string.export_include_media), "Apkg格式", "Card格式"};
        } else {
            mIncludeSched = true;
            checked = new Integer[]{ INCLUDE_SCHED};
            items = new String[] {res.getString(R.string.export_include_schedule),
                    res.getString(R.string.export_include_media)};
        }


        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity())
                .title(R.string.export)
                .content(getArguments().getString("dialogMessage"))
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .cancelable(true)
                .items(items)
                .alwaysCallMultiChoiceCallback()
                .itemsCallbackMultiChoice(checked,
                        (materialDialog, integers, charSequences) -> {
                            mIncludeMedia = false;
                            mIncludeSched = false;
                            mExportApkg = false;
                            mExportCard = false;
                            for (Integer integer : integers) {
                                switch (integer) {
                                    case INCLUDE_SCHED:
                                        mIncludeSched = true;
                                        break;
                                    case INCLUDE_MEDIA:
                                        mIncludeMedia = true;
                                        break;
                                    case EXPORT_CARD:
                                        mExportCard = true;
//                                        mExportApkg = false;
                                        break;
                                    case EXPORT_APKG:
                                        mExportApkg = true;
//                                        mExportCard = false;
                                        break;
                                }
                            }
                            return true;
                        })
                .onPositive((dialog, which) -> {
                            if(mExportApkg&&mExportCard){
                                Toast.makeText(getContext(),"不能同时导出Apkg和Card格式",Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if(did != -1L&&!mExportApkg&&!mExportCard){
                                Toast.makeText(getContext(),"请至少选择一种导出格式",Toast.LENGTH_SHORT).show();
                                return;
                            }
                            listener.exportApkg(null, did != -1L ? did : null, mIncludeSched, mIncludeMedia,mExportCard,mExportApkg);
                    dismissAllDialogFragments();
                })
                .onNegative((dialog, which) -> dismissAllDialogFragments());
        return builder.show();
    }


    public void dismissAllDialogFragments() {
       listener.dismissAllDialogFragments();
    }

}
