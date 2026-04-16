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
        // onShowListener fires via a Handler message posted at the end of show() —
        // asynchronously. A Choreographer VSYNC can land before it, rendering a frame
        // before the tweaks run and producing a visible jump. Doing everything here
        // instead is fully synchronous: show() has completed, installContent() /
        // setupButtons() have run so getButton() works, and no frame has been drawn yet.
        Dialog dialog = getDialog();
        if (dialog == null || !(dialog instanceof AlertDialog) || dialog.getWindow() == null) return;
        AlertDialog ad = (AlertDialog) dialog;
        android.view.Window win = ad.getWindow();

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int targetWidth = screenWidth - getDp(100);
        win.setLayout(targetWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

        int titleId = androidx.appcompat.R.id.alertTitle;
        android.widget.TextView titleView = ad.findViewById(titleId);
        if (titleView != null) {
            titleView.setPadding(getDp(25), getDp(20), getDp(25), 0);

            if (titleView.getParent() instanceof android.view.View) {
                android.view.View titleParent = (android.view.View) titleView.getParent();
                titleParent.setPadding(0, 0, 0, 0);

                if (titleParent.getParent() instanceof android.view.ViewGroup) {
                    android.view.ViewGroup titleGrandparent = (android.view.ViewGroup) titleParent.getParent();
                    titleGrandparent.setPadding(0, 0, 0, 0);
                    titleGrandparent.setMinimumHeight(0);
                    if (titleGrandparent.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
                        android.view.ViewGroup.MarginLayoutParams params = (android.view.ViewGroup.MarginLayoutParams) titleGrandparent.getLayoutParams();
                        params.setMargins(0, 0, 0, 0);
                        titleGrandparent.setLayoutParams(params);
                    }
                    for (int i = 0; i < titleGrandparent.getChildCount(); i++) {
                        android.view.View child = titleGrandparent.getChildAt(i);
                        if (child != titleParent) {
                            child.setVisibility(android.view.View.GONE);
                        }
                    }
                }
            }
        }

        int messageId = getResources().getIdentifier("message", "id", "android");
        android.widget.TextView messageView = ad.findViewById(messageId);
        if (messageView != null) {
            messageView.setPadding(getDp(25), getDp(5), getDp(25), getDp(20));

            if (messageView.getParent() instanceof android.view.View) {
                android.view.View messageParent = (android.view.View) messageView.getParent();
                messageParent.setPadding(0, 0, 0, 0);

                if (messageParent.getParent() instanceof android.view.View) {
                    android.view.View messageGrandparent = (android.view.View) messageParent.getParent();
                    messageGrandparent.setPadding(0, 0, 0, 0);
                }
            }
        }

        android.widget.Button refButton = ad.getButton(DialogInterface.BUTTON_POSITIVE);
        if (refButton == null) refButton = ad.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (refButton == null) return;

        android.view.ViewGroup buttonBar = (android.view.ViewGroup) refButton.getParent();
        if (buttonBar != null && buttonBar.getParent() instanceof android.view.View) {
            android.view.View buttonPanel = (android.view.View) buttonBar.getParent();
            buttonPanel.setPadding(0, 0, 0, 0);
        }
        if (buttonBar == null) return;

        java.util.List<android.widget.Button> sortedButtons = new java.util.ArrayList<>();
        for (int i = 0; i < buttonBar.getChildCount(); i++) {
            android.view.View child = buttonBar.getChildAt(i);
            if (child instanceof android.widget.Button && child.getVisibility() != android.view.View.GONE) {
                sortedButtons.add((android.widget.Button) child);
            }
        }

        if (sortedButtons.isEmpty()) return;

        android.widget.Button negativeButton = ad.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            negativeButton.setTextColor(android.graphics.Color.parseColor("#dd4447"));
        }

        int dialogInnerWidth = targetWidth;
        int barSidePadding = getDp(15);
        int barBottomPadding = getDp(15);
        int barTopPadding = 0;
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
            btn.setAllCaps(false);
            btn.measure(widthSpec, heightSpec);
            if (btn.getMeasuredWidth() > singleButtonWidth) {
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
                params.setMargins(0, 0, 0, (i < buttonCount - 1) ? buttonGap : 0);
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

                btn.measure(
                        android.view.View.MeasureSpec.makeMeasureSpec(singleButtonWidth, android.view.View.MeasureSpec.EXACTLY),
                        android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
                );
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

        // Scale + fade in, matching iOS dialog presentation feel.
        android.view.View decorView = win.getDecorView();
        decorView.setAlpha(0f);
        decorView.setScaleX(1.1f);
        decorView.setScaleY(1.1f);
        decorView.animate()
            .alpha(0.95f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(280)
            .setInterpolator(new android.view.animation.DecelerateInterpolator(2f))
            .start();
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
