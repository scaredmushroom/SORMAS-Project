package de.symeda.sormas.app.component.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

import de.symeda.sormas.app.BR;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.component.controls.ControlButton;
import de.symeda.sormas.app.component.controls.ControlButtonType;
import de.symeda.sormas.app.component.controls.ControlPropertyEditField;
import de.symeda.sormas.app.core.NotificationContext;
import de.symeda.sormas.app.databinding.DialogRootLayoutBinding;
import de.symeda.sormas.app.util.Callback;

/**
 * This should probably inherit from DialogFragment
 */
public abstract class AbstractDialog implements NotificationContext {

    public static final String TAG = AbstractDialog.class.getSimpleName();

    private FragmentActivity activity;
    private int rootLayoutId;
    private AlertDialog.Builder builder;
    private AlertDialog dialog;
    private DialogRootLayoutBinding rootBinding;
    private ViewDataBinding contentBinding;
    private ViewDataBinding buttonPanelBinding;
    private int contentLayoutResourceId;
    private int buttonPanelLayoutResourceId;
    private DialogViewConfig config;
    private boolean liveValidationDisabled;

    // Button callbacks
    private Callback positiveCallback;
    private Callback negativeCallback;
    private Callback deleteCallback;

    // Constructor

    public AbstractDialog(final FragmentActivity activity, int rootLayoutId, int contentLayoutResourceId,
                          int buttonPanelLayoutResourceId, int headingResourceId, int subHeadingResourceId) {

        this.builder = new AlertDialog.Builder(activity);
        this.activity = activity;
        this.rootLayoutId = rootLayoutId;
        this.contentLayoutResourceId = contentLayoutResourceId;
        this.buttonPanelLayoutResourceId = buttonPanelLayoutResourceId;

        Resources resources = activity.getResources();

        String heading = null;
        if (headingResourceId >= 0) {
            heading = resources.getString(headingResourceId);
        }
        String subHeading = null;
        if (subHeadingResourceId >= 0) {
            subHeading = resources.getString(subHeadingResourceId);
        }
        String positiveLabel = resources.getString(getPositiveButtonText());
        String negativeLabel = resources.getString(getNegativeButtonText());
        String deleteLabel = resources.getString(getDeleteButtonText());
        Drawable positiveIcon = resources.getDrawable(getPositiveButtonIconResourceId());
        Drawable negativeIcon = resources.getDrawable(getNegativeButtonIconResourceId());

        this.config = new DialogViewConfig(heading, subHeading, positiveLabel, negativeLabel, deleteLabel, positiveIcon, negativeIcon);
    }

    // Instance methods

    private DialogRootLayoutBinding bindRootLayout(final Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final DialogRootLayoutBinding binding = DataBindingUtil.inflate(inflater, this.rootLayoutId, null, false);
        final String layoutName = context.getResources().getResourceEntryName(this.rootLayoutId);

        // Bind required variables to layout
        bindDialog(binding, layoutName);
        bindConfig(binding, layoutName);

        // Hide notification frame on click
        binding.notificationFrame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(View.GONE);
            }
        });

        // Inflate dialog content
        binding.dialogContent.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                contentBinding = DataBindingUtil.bind(inflated);
                String layoutName = context.getResources().getResourceEntryName(contentLayoutResourceId);
                bindConfig(contentBinding, layoutName);
                setContentBinding(context, contentBinding, layoutName);
            }
        });

        ViewStub dialogContent = binding.dialogContent.getViewStub();
        dialogContent.setLayoutResource(contentLayoutResourceId);
        dialogContent.inflate();

        // Inflate dialog button panel
        binding.dialogButtonPanel.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                buttonPanelBinding = DataBindingUtil.bind(inflated);
                String layoutName = context.getResources().getResourceEntryName(buttonPanelLayoutResourceId);
                bindConfig(buttonPanelBinding, layoutName);
                bindDialog(buttonPanelBinding, layoutName);
            }
        });

        ViewStub buttonPanel = binding.dialogButtonPanel.getViewStub();
        buttonPanel.setLayoutResource(buttonPanelLayoutResourceId);
        buttonPanel.inflate();

        builder.setView(binding.getRoot());

        return binding;
    }

    public void show() {
        this.rootBinding = bindRootLayout(activity);
        setNotificationContextForPropertyFields((ViewGroup) rootBinding.getRoot());
        initializeContentView(rootBinding, buttonPanelBinding);

        ControlButton positiveButton = getPositiveButton();
        if (positiveButton != null) {
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onPositiveClick();
                }
            });
        }

        ControlButton negativeButton = getNegativeButton();
        if (negativeButton != null) {
            negativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNegativeClick();
                }
            });
        }

        ControlButton deleteButton = getDeleteButton();
        if (deleteButton != null) {
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDeleteClick();
                }
            });
        }

        dialog = builder.show();
    }

    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    private void bindDialog(final ViewDataBinding binding, String layoutName) {
        if (!binding.setVariable(BR.dialog, this)) {
            Log.e(TAG, "There is no variable 'dialog' in layout " + layoutName);
        }
    }

    private void bindConfig(final ViewDataBinding binding, String layoutName) {
        if (!binding.setVariable(BR.config, this.config)) {
            Log.e(TAG, "There is no variable 'config' in layout " + layoutName);
        }
    }

    protected void onPositiveClick() {
        if (positiveCallback != null) {
            positiveCallback.call();
        }
    }

    private void onNegativeClick() {
        if (negativeCallback != null) {
            negativeCallback.call();
        } else {
            dismiss();
        }
    }

    private void onDeleteClick() {
        if (deleteCallback != null) {
            final ConfirmationDialog confirmationDialog = new ConfirmationDialog(getActivity(),
                    R.string.heading_confirmation_dialog,
                    R.string.heading_sub_confirmation_notification_dialog_delete,
                    R.string.yes,
                    R.string.no);
            confirmationDialog.setPositiveCallback(new Callback() {
                @Override
                public void call() {
                    deleteCallback.call();
                    confirmationDialog.dismiss();
                }
            });

            confirmationDialog.show();
        }
    }

    public void setLiveValidationDisabled(boolean liveValidationDisabled) {
        if (this.liveValidationDisabled != liveValidationDisabled) {
            this.liveValidationDisabled = liveValidationDisabled;
            applyLiveValidationDisabledToChildren();
        }
    }

    private void applyLiveValidationDisabledToChildren() {
        if (contentBinding == null) return;
        ViewGroup root = (ViewGroup) contentBinding.getRoot();
        ControlPropertyEditField.applyLiveValidationDisabledToChildren(root, liveValidationDisabled);
    }

    private void setNotificationContextForPropertyFields(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ControlPropertyEditField) {
                ((ControlPropertyEditField) child).setNotificationContext(this);
            } else if (child instanceof ViewGroup) {
                setNotificationContextForPropertyFields((ViewGroup) child);
            }
        }
    }

    public boolean isPositiveButtonVisible() {
        return true;
    }

    public boolean isNegativeButtonVisible() {
        return true;
    }

    public boolean isDeleteButtonVisible() {
        return false;
    }

    public boolean isButtonPanelVisible() {
        return isPositiveButtonVisible() || isNegativeButtonVisible() || isDeleteButtonVisible();
    }

    public boolean isHeadingVisible() {
        return true;
    }

    public boolean isHeadingCentered() {
        return false;
    }

    public boolean isRounded() {
        return false;
    }

    public void setPositiveCallback(Callback positiveCallback) {
        this.positiveCallback = positiveCallback;
    }

    public void setNegativeCallback(Callback negativeCallback) {
        this.negativeCallback = negativeCallback;
    }

    public void setDeleteCallback(Callback deleteCallback) {
        this.deleteCallback = deleteCallback;
    }

    public Context getContext() {
        return this.activity;
    }

    public FragmentManager getFragmentManager() {
        return this.activity.getSupportFragmentManager();
    }

    public ControlButton getPositiveButton() {
        if (buttonPanelBinding == null) {
            return null;
        }

        View buttonPanelRootView = buttonPanelBinding.getRoot();

        if (buttonPanelRootView == null) {
            return null;
        }

        return buttonPanelRootView.findViewById(R.id.button_positive);
    }

    public ControlButton getNegativeButton() {
        if (buttonPanelBinding == null) {
            return null;
        }

        View buttonPanelRootView = buttonPanelBinding.getRoot();

        if (buttonPanelRootView == null) {
            return null;
        }

        return buttonPanelRootView.findViewById(R.id.button_negative);
    }

    public ControlButton getDeleteButton() {
        if (buttonPanelBinding == null) {
            return null;
        }

        View buttonPanelRootView = buttonPanelBinding.getRoot();

        if (buttonPanelRootView == null) {
            return null;
        }

        return buttonPanelRootView.findViewById(R.id.button_delete);
    }

    public ControlButtonType getPositiveButtonType() {
        return ControlButtonType.PRIMARY;
    }

    public ControlButtonType getNegativeButtonType() {
        return ControlButtonType.SECONDARY;
    }

    public ControlButtonType getDeleteButtonType() {
        return ControlButtonType.DANGER;
    }

    public int getPositiveButtonText() {
        return R.string.action_ok;
    }

    public int getNegativeButtonText() {
        return R.string.action_dismiss;
    }

    private int getDeleteButtonText() {
        return R.string.action_delete;
    }

    public int getPositiveButtonIconResourceId() {
        return R.drawable.ic_done_black_24dp;
    }

    public int getNegativeButtonIconResourceId() {
        return R.drawable.ic_clear_black_24dp;
    }

    public boolean isShowing() {
        return dialog.isShowing();
    }

    // Abstract methods

    protected abstract void setContentBinding(Context context, ViewDataBinding binding, String layoutName);

    protected abstract void initializeContentView(ViewDataBinding rootBinding, ViewDataBinding buttonPanelBinding);

    // Overrides

    @Override
    public View getRootView() {
        return rootBinding.getRoot();
    }

    // Getters & setters

    protected View getRoot() {
        return rootBinding.getRoot();
    }

    protected FragmentActivity getActivity() {
        return activity;
    }

    public DialogViewConfig getConfig() {
        return config;
    }

}