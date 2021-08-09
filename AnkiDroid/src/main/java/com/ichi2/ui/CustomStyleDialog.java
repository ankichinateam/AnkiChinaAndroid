package com.ichi2.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.NoCopySpan;
import android.text.Spannable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ichi2.anki.R;
import com.umeng.commonsdk.debug.W;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;


public class CustomStyleDialog extends Dialog {
    public CustomStyleDialog(@NonNull Context context) {
        super(context);
    }


    public CustomStyleDialog(Context context, int theme) {
        super(context, theme);
    }

    public String getEditorText(){
        return ((EditText)findViewById(R.id.editor)).getText().toString();
    }


    public List<EditText> getMultiEditor() {
        return mMultiEditor;
    }



    public void setMultiEditor(List<EditText> mMultiEditor) {
        this.mMultiEditor = mMultiEditor;
    }


    private List<EditText> mMultiEditor =new ArrayList<>();


    public TextView getSingleEditorModeHintView() {
        return mSingleEditorModeHintView;
    }

    public void setSingleEditorModeHintView(TextView mSingleEditorModeHintView) {
        this.mSingleEditorModeHintView = mSingleEditorModeHintView;
    }

    private TextView mSingleEditorModeHintView;
    public static class Builder {
        private Context context;
        private String title;
        private boolean centerTitle;
        private String message;
        private String positiveButtonText;
        private String negativeButtonText;
        private DialogInterface.OnClickListener positiveButtonClickListener;
        private DialogInterface.OnClickListener negativeButtonClickListener;


        public Builder(Context context) {
            this.context = context;
        }


        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }


        /**
         * Set the Dialog message from resource
         *
         * @param
         * @return
         */
        public Builder setMessage(int message) {
            this.message = (String) context.getText(message);
            return this;
        }


        /**
         * Set the Dialog title from resource
         *
         * @param title
         * @return
         */
        public Builder setTitle(int title) {
            this.title = (String) context.getText(title);
            return this;
        }


        /**
         * Set the Dialog title from String
         *
         * @param title
         * @return
         */

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder centerTitle( ) {
            this.centerTitle = true;
            return this;
        }
        /**
         * Set the positive button resource and it's listener
         *
         * @param positiveButtonText
         * @return
         */
        public Builder setPositiveButton(int positiveButtonText,
                                         DialogInterface.OnClickListener listener) {
            this.positiveButtonText = (String) context
                    .getText(positiveButtonText);
            this.positiveButtonClickListener = listener;
            return this;
        }


        public Builder setPositiveButton(String positiveButtonText,
                                         DialogInterface.OnClickListener listener) {
            this.positiveButtonText = positiveButtonText;
            this.positiveButtonClickListener = listener;
            return this;
        }


        public Builder setNegativeButton(int negativeButtonText,
                                         DialogInterface.OnClickListener listener) {
            this.negativeButtonText = (String) context
                    .getText(negativeButtonText);
            this.negativeButtonClickListener = listener;
            return this;
        }


        public Builder setNegativeButton(String negativeButtonText,
                                         DialogInterface.OnClickListener listener) {
            this.negativeButtonText = negativeButtonText;
            this.negativeButtonClickListener = listener;
            return this;
        }


        private String editorText;
        private String textHint;
        private MyTextWatcher singleEditorWatcher;
        public interface MyTextWatcher   {

            public void beforeTextChanged(Dialog dialog,CharSequence s, int start,
                                          int count, int after);

            public void onTextChanged(Dialog dialog,CharSequence s, int start, int before, int count);


            public void afterTextChanged(Dialog dialog,Editable s);
        }


        public Builder setEditorText(String editorText,
                                     String textHint) {
            this.editorText = editorText;
            this.textHint = textHint;
            return this;
        }

        public Builder addSingleTextChangedListener(MyTextWatcher watcher) {
            this.singleEditorWatcher = watcher;
            return this;
        }


        public Builder setSelectListModeCallback(SelectListModeCallback selectListModeCallback) {
            this.selectListModeCallback = selectListModeCallback;
            return this;
        }


        private SelectListModeCallback selectListModeCallback;



        public interface SelectListModeCallback {
            String[] getItemContent();

            String[] getItemHint();

            int getDefaultSelectedPosition();

            void onItemSelect(int position);
        }


        public List<EditText> getMultiEditor() {
            return mMultiEditor;
        }


        private List<EditText> mMultiEditor =new ArrayList<>();


        public Builder setMultiEditorModeCallback(MultiEditorModeCallback multiEditorModeCallback) {
            this.multiEditorModeCallback = multiEditorModeCallback;
            return this;
        }


        private MultiEditorModeCallback multiEditorModeCallback;
        public interface MultiEditorModeCallback {
            String[] getEditorText();

            String[] getEditorHint();

            String[] getItemHint();
        }

        private int inputType=-1;
        public Builder  setInputType(int inputType){
            this.inputType=inputType;
            return this;
        }

        private int customLayoutRes;
        public Builder  setCustomLayout(int layoutRes){
            this.customLayoutRes=layoutRes;
            return this;
        }

//        public Builder setSelectListModeWithHint(String editorHint,
//                                                 String textHint) {
//            this.editorHint = editorHint;
//            this.textHint = textHint;
//            return this;
//        }

        private EditText editText;
        private  CustomStyleDialog dialog;
        public CustomStyleDialog create() {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            // instantiate the dialog with the custom Theme
             dialog = new CustomStyleDialog(context, R.style.CommonDialogTheme);
            dialog.setContentView(customLayoutRes>0?customLayoutRes:R.layout.dialog_common_custom);
//            View dialog = inflater.inflate(R.dialog.dialog_common_custom, null);
//            dialog.addContentView(dialog, new LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            // set the dialog title
            ((TextView) dialog.findViewById(R.id.title)).setText(title);
            TextView title=dialog.findViewById(R.id.title);
            if(centerTitle){
                ViewGroup.LayoutParams layoutParams=title.getLayoutParams();
                layoutParams.width= LinearLayout.LayoutParams.MATCH_PARENT;
            }
            LinearLayout contentLayout = dialog.findViewById(R.id.ll_content);
            // set the confirm button
            if (positiveButtonText != null) {
                ((TextView) dialog.findViewById(R.id.confirm))
                        .setText(positiveButtonText);
                if (positiveButtonClickListener != null) {
                    ((TextView) dialog.findViewById(R.id.confirm))
                            .setOnClickListener((View.OnClickListener) v -> positiveButtonClickListener.onClick(dialog,
                                    DialogInterface.BUTTON_POSITIVE));
                }
            } else {
                // if no confirm button just set the visibility to GONE
                dialog.findViewById(R.id.confirm).setVisibility(
                        View.GONE);
            }
            // set the cancel button
            if (negativeButtonText != null) {
                ((TextView) dialog.findViewById(R.id.cancel))
                        .setText(negativeButtonText);
                if (negativeButtonClickListener != null) {
                    ((TextView) dialog.findViewById(R.id.cancel))
                            .setOnClickListener((View.OnClickListener) v -> negativeButtonClickListener.onClick(dialog,
                                    DialogInterface.BUTTON_NEGATIVE));
                }
            } else {
                // if no confirm button just set the visibility to GONE
                dialog.findViewById(R.id.cancel).setVisibility(
                        View.GONE);
            }
            if (editorText != null || textHint != null) {
                editText=dialog.findViewById(R.id.editor);
                editText.setText(editorText);
                if(singleEditorWatcher!=null)
                editText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        singleEditorWatcher.beforeTextChanged(dialog,s, start, count, after);
                    }


                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        singleEditorWatcher.onTextChanged(dialog,s, start, before, count);
                    }


                    @Override
                    public void afterTextChanged(Editable s) {
                        singleEditorWatcher.afterTextChanged(dialog,s);
                    }
                });
                if(inputType>-1){
                    editText.setInputType(inputType);
                }
                dialog.setSingleEditorModeHintView(((TextView) dialog.findViewById(R.id.hint)));
                ((TextView) dialog.findViewById(R.id.hint)).setText(textHint);
            }else if(message!=null){
                dialog.findViewById(R.id.ll_edit_content).setVisibility(View.GONE);
                ((TextView) dialog.findViewById(R.id.message)).setText(message);
                ((TextView) dialog.findViewById(R.id.message)).setVisibility(View.VISIBLE);
            } else if (selectListModeCallback != null) {
                dialog.findViewById(R.id.ll_edit_content).setVisibility(View.GONE);
                boolean hasHint = selectListModeCallback.getItemHint().length > 0;
                List<View> selectItems=new ArrayList<>();
                for (int i = 0; i < selectListModeCallback.getItemContent().length; i++) {
                    View item = inflater.inflate(hasHint ? R.layout.item_dialog_select_with_hint : R.layout.item_dialog_select, null);
                    ((TextView) item.findViewById(R.id.title)).setText(selectListModeCallback.getItemContent()[i]);
                    if(hasHint)
                    ((TextView) item.findViewById(R.id.name)).setText(selectListModeCallback.getItemHint()[i]);
                    int finalI = i;
                    item.findViewById(R.id.select).setVisibility(i==selectListModeCallback.getDefaultSelectedPosition()?View.VISIBLE:View.GONE);

                    item.setOnClickListener(v -> {
//                        TextView select=item.findViewById(R.id.select);
                        for(View item1 :selectItems){
                            item1.findViewById(R.id.select).setVisibility(item==item1?View.VISIBLE:View.GONE);
                        }
                        selectListModeCallback.onItemSelect(finalI);

//                        select.setVisibility(View.VISIBLE);
                    });
                    selectItems.add(item);
                    contentLayout.addView(item);
                }

            }else if(multiEditorModeCallback!=null){
                dialog.findViewById(R.id.ll_edit_content).setVisibility(View.GONE);
                for (int i = 0; i < multiEditorModeCallback.getEditorText().length; i++) {
                    View item = inflater.inflate( R.layout.item_editor  , null);
                    ((EditText) item.findViewById(R.id.editor)).setText(multiEditorModeCallback.getEditorText()[i]);
                    ((TextView) item.findViewById(R.id.edit_hint)).setText(multiEditorModeCallback.getEditorHint()[i]);
                    ((TextView) item.findViewById(R.id.hint)).setText(multiEditorModeCallback.getItemHint()[i]);
                    mMultiEditor.add(item.findViewById(R.id.editor));

                    if(i==0&&singleEditorWatcher!=null){
                        dialog.setSingleEditorModeHintView(((TextView) item.findViewById(R.id.hint)));
                        ((EditText) item.findViewById(R.id.editor)).addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                singleEditorWatcher.beforeTextChanged(dialog,s, start, count, after);
                            }


                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                singleEditorWatcher.onTextChanged(dialog,s, start, before, count);
                            }


                            @Override
                            public void afterTextChanged(Editable s) {
                                singleEditorWatcher.afterTextChanged(dialog,s);
                            }
                        });
                    }
                    contentLayout.addView(item);
                }
                dialog.setMultiEditor(mMultiEditor);

            }
            return dialog;
        }
    }


}
