package im.shimo.react.prompt;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import android.view.WindowManager;
import android.widget.EditText;

import javax.annotation.Nullable;

public class RNPromptFragment extends DialogFragment implements DialogInterface.OnClickListener {

    /* package */ static final String ARG_TITLE = "title";
    /* package */ static final String ARG_MESSAGE = "message";
    /* package */ static final String ARG_BUTTON_POSITIVE = "button_positive";
    /* package */ static final String ARG_BUTTON_NEGATIVE = "button_negative";
    /* package */ static final String ARG_BUTTON_NEUTRAL = "button_neutral";
    /* package */ static final String ARG_ITEMS = "items";
    /* package */ static final String ARG_TYPE = "type";
    /* package */ static final String ARG_STYLE = "style";
    /* package */ static final String ARG_DEFAULT_VALUE = "defaultValue";
    /* package */ static final String ARG_PLACEHOLDER = "placeholder";
    /* package */ static final String ARG_SHOW_INPUT = "showInput";

    private EditText mInputText;

    public enum PromptTypes {
        TYPE_DEFAULT("default"),
        PLAIN_TEXT("plain-text"),
        SECURE_TEXT("secure-text"),
        NUMERIC("numeric"),
        EMAIL_ADDRESS("email-address"),
        PHONE_PAD("phone-pad");

        private final String mName;

        PromptTypes(final String name) {
            mName = name;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    private
    @Nullable
    RNPromptModule.PromptFragmentListener mListener;

    public RNPromptFragment() {
        mListener = null;
    }

    public void setListener(@Nullable RNPromptModule.PromptFragmentListener listener) {
        mListener = listener;
    }

    private int getDp(float value){
        return (int) (getResources().getDisplayMetrics().density * value);
    }

    public Dialog createDialog(Context activityContext, Bundle arguments) {
        AlertDialog.Builder builder;

        builder = new AlertDialog.Builder(activityContext, R.style.CleanDialogTheme);

        builder.setTitle(arguments.getString(ARG_TITLE));

        if (arguments.containsKey(ARG_BUTTON_POSITIVE)) {
            builder.setPositiveButton(arguments.getString(ARG_BUTTON_POSITIVE), this);
        }
        if (arguments.containsKey(ARG_BUTTON_NEGATIVE)) {
            builder.setNegativeButton(arguments.getString(ARG_BUTTON_NEGATIVE), this);
        }
        if (arguments.containsKey(ARG_BUTTON_NEUTRAL)) {
            builder.setNeutralButton(arguments.getString(ARG_BUTTON_NEUTRAL), this);
        }
        // if both message and items are set, Android will only show the message
        // and ignore the items argument entirely
        if (arguments.containsKey(ARG_MESSAGE)) {
            builder.setMessage(arguments.getString(ARG_MESSAGE));
        }

        if (arguments.containsKey(ARG_ITEMS)) {
            builder.setItems(arguments.getCharSequenceArray(ARG_ITEMS), this);
        }

        AlertDialog alertDialog = builder.create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                AlertDialog ad = (AlertDialog) dialog;

                int titleId = getResources().getIdentifier("alertTitle", "id", "android");
                android.widget.TextView titleView = ad.findViewById(titleId);
                if (titleView != null) {
                    titleView.setPadding(getDp(25), getDp(20), getDp(25), titleView.getPaddingBottom());
                }

                int messageId = getResources().getIdentifier("message", "id", "android");
                android.widget.TextView messageView = ad.findViewById(messageId);
                if (messageView != null) {
                    messageView.setPadding(getDp(25), messageView.getPaddingTop(), getDp(25), messageView.getPaddingBottom());
                }

                android.widget.Button refButton = ad.getButton(DialogInterface.BUTTON_POSITIVE);
                if (refButton == null) refButton = ad.getButton(DialogInterface.BUTTON_NEGATIVE);
                if (refButton == null) return;

                android.view.ViewGroup buttonBar = (android.view.ViewGroup) refButton.getParent();
                if (buttonBar == null) return;

                java.util.List<android.widget.Button> sortedButtons = new java.util.ArrayList<>();
                for (int i = 0; i < buttonBar.getChildCount(); i++) {
                    android.view.View child = buttonBar.getChildAt(i);
                    if (child instanceof android.widget.Button && child.getVisibility() != android.view.View.GONE) {
                        sortedButtons.add((android.widget.Button) child);
                    }
                }

                if (sortedButtons.isEmpty()) return;

                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int dialogMargin = getDp(100);
                int dialogInnerWidth = screenWidth - dialogMargin;

                int barSidePadding = getDp(15);
                int barBottomPadding = getDp(15);
                int barTopPadding = getDp(10);

                int buttonGap = getDp(8);

                buttonBar.setClipChildren(false);
                buttonBar.setClipToPadding(false);

                int buttonCount = sortedButtons.size();
                int usableWidth = dialogInnerWidth - (barSidePadding * 2);
                int totalGapSpace = (buttonCount - 1) * buttonGap;
                int singleButtonWidth = (usableWidth - totalGapSpace) / buttonCount;

                boolean needsVerticalLayout = false;
                int widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED);
                int heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED);

                for (android.widget.Button btn : sortedButtons) {
                    btn.measure(widthSpec, heightSpec);
                    int neededWidth = btn.getMeasuredWidth();
                    if (neededWidth > singleButtonWidth) {
                        needsVerticalLayout = true;
                        break;
                    }
                }

                if (needsVerticalLayout) {
                    if (buttonBar instanceof android.widget.LinearLayout) {
                        ((android.widget.LinearLayout) buttonBar).setOrientation(android.widget.LinearLayout.VERTICAL);
                        ((android.widget.LinearLayout) buttonBar).setGravity(android.view.Gravity.CENTER_HORIZONTAL);
                    }
                    buttonBar.setPadding(barSidePadding, barTopPadding, barSidePadding, barBottomPadding);

                    for (int i = 0; i < buttonCount; i++) {
                        android.widget.Button btn = sortedButtons.get(i);
                        android.widget.LinearLayout.LayoutParams params =
                                new android.widget.LinearLayout.LayoutParams(
                                        usableWidth,
                                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                        params.weight = 0;
                        int bottomMargin = (i < buttonCount - 1) ? buttonGap : 0;
                        params.setMargins(0, 0, 0, bottomMargin);
                        btn.setLayoutParams(params);
                    }
                } else {
                    if (buttonBar instanceof android.widget.LinearLayout) {
                        ((android.widget.LinearLayout) buttonBar).setOrientation(android.widget.LinearLayout.HORIZONTAL);
                    }
                    buttonBar.setPadding(barSidePadding, barTopPadding, barSidePadding, barBottomPadding);

                    int maxHeight = 0;
                    for (int i = 0; i < buttonCount; i++) {
                        android.widget.Button btn = sortedButtons.get(i);
                        android.widget.LinearLayout.LayoutParams params =
                                (android.widget.LinearLayout.LayoutParams) btn.getLayoutParams();

                        params.width = singleButtonWidth;
                        params.weight = 0;
                        int rightMargin = (i < buttonCount - 1) ? buttonGap : 0;
                        params.setMargins(0, 0, rightMargin, 0);
                        params.setMarginEnd(rightMargin);
                        btn.setLayoutParams(params);

                        int btnWidthSpec = android.view.View.MeasureSpec.makeMeasureSpec(
                                singleButtonWidth, android.view.View.MeasureSpec.EXACTLY);
                        int btnHeightSpec = android.view.View.MeasureSpec.makeMeasureSpec(
                                0, android.view.View.MeasureSpec.UNSPECIFIED);
                        btn.measure(btnWidthSpec, btnHeightSpec);
                        maxHeight = Math.max(maxHeight, btn.getMeasuredHeight());
                    }

                    for (android.widget.Button btn : sortedButtons) {
                        android.widget.LinearLayout.LayoutParams params =
                                (android.widget.LinearLayout.LayoutParams) btn.getLayoutParams();
                        params.height = maxHeight;
                        btn.setLayoutParams(params);
                        btn.setGravity(android.view.Gravity.CENTER);
                    }
                }
            }
        });

        Boolean isShowInput = arguments.getBoolean(ARG_SHOW_INPUT, false);
        if (!isShowInput) {
            return alertDialog;
        }

        final EditText input;
        input = new EditText(activityContext);
        input.setTextColor(android.graphics.Color.WHITE);
        alertDialog.setView(input, getDp(20), getDp(15), getDp(20), 0);

        mInputText = input;

        return alertDialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = this.createDialog(getActivity(), getArguments());

        if (mInputText != null) {
            if (mInputText.requestFocus()) {
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        }
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int targetWidth = screenWidth - getDp(100);

            dialog.getWindow().setLayout(targetWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mListener != null) {
            if (mInputText != null) {
                mListener.onConfirm(which, mInputText.getText().toString());    
            } else {
                mListener.onClick(dialog, which);
            }
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mListener != null) {
            mListener.onDismiss(dialog);
        }
    }
}
