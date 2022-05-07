package com.cyberlight.perfect.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.cyberlight.perfect.R;
import com.cyberlight.perfect.util.DbUtil;
import com.cyberlight.perfect.util.OnDataAddedListener;
import com.cyberlight.perfect.util.ToastUtil;

public class PlanDialogFragment extends DialogFragment {

    public static final String TAG = "PlanDialogFragment";

    private static final String TARGET_KEY = "target_key";
    private static final String PICK_PT_REQUEST_KEY = "pick_pt_request_key";

    private int mTarget;
    private TextView mStepperValueTv;

    public PlanDialogFragment() {
    }

    private OnDataAddedListener mOnDataAddedListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mOnDataAddedListener = (OnDataAddedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(requireActivity()
                    + " must implement OnDataAddedListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // 设置对话框布局
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.dialog_plan, null);

        Context context = getContext();
        // 获取fragmentManager准备用于对话框操作
        FragmentManager fragmentManager = getChildFragmentManager();
        // 初始化数据
        if (savedInstanceState != null) {
            mTarget = savedInstanceState.getInt(TARGET_KEY);
        } else {
            mTarget = 1;
        }
        mStepperValueTv = view.findViewById(R.id.dialog_plan_stepper_value_tv);
        mStepperValueTv.setText(String.valueOf(mTarget));
        mStepperValueTv.setOnClickListener(v -> {
            if (fragmentManager.findFragmentByTag(PlanTargetPickerDialogFragment.TAG) == null) {
                DialogFragment planTargetPickerDialogFragment =
                        PlanTargetPickerDialogFragment.newInstance(PICK_PT_REQUEST_KEY, mTarget);
                planTargetPickerDialogFragment.show(fragmentManager,
                        PlanTargetPickerDialogFragment.TAG);
            }
        });
        ImageView mStepperPlusIv = view.findViewById(R.id.dialog_plan_stepper_plus_iv);
        mStepperPlusIv.setOnClickListener(v -> {
            if (mTarget < 99) {
                mTarget++;
                mStepperValueTv.setText(String.valueOf(mTarget));
            }
        });
        ImageView mStepperMinusIv = view.findViewById(R.id.dialog_plan_stepper_minus_iv);
        mStepperMinusIv.setOnClickListener(v -> {
            if (mTarget > 1) {
                mTarget--;
                mStepperValueTv.setText(String.valueOf(mTarget));
            }
        });
        fragmentManager.setFragmentResultListener(PICK_PT_REQUEST_KEY,
                this, (requestKey, result) -> {
                    mTarget = result.getInt(PlanTargetPickerDialogFragment.PT_TARGET_KEY);
                    mStepperValueTv.setText(String.valueOf(mTarget));
                });
        // 设置按钮栏
        TextView dialogTitleTv = view.findViewById(R.id.dialog_action_bar_title_tv);
        dialogTitleTv.setText(R.string.plan_dialog_title);
        ImageView cancelIv = view.findViewById(R.id.dialog_action_bar_cancel_iv);
        cancelIv.setOnClickListener(v -> dismiss());
        ImageView confirmIv = view.findViewById(R.id.dialog_action_bar_confirm_iv);
        confirmIv.setOnClickListener(v -> {
            EditText mContentEt = view.findViewById(R.id.dialog_plan_content_et);
            String planContent = mContentEt.getText().toString();
            if (planContent.equals("")) {
                ToastUtil.showToast(context,
                        R.string.plan_incomplete_toast,
                        Toast.LENGTH_SHORT);
                return;
            }
            if (DbUtil.addPlan(context, planContent, mTarget)) {
                ToastUtil.showToast(context,
                        R.string.plan_success_toast,
                        Toast.LENGTH_SHORT);
                if (mOnDataAddedListener != null)
                    mOnDataAddedListener.onDataAdded(TAG);
            } else {
                ToastUtil.showToast(context,
                        R.string.plan_fail_toast,
                        Toast.LENGTH_SHORT);
            }
            dismiss();
        });
        // 设置对话框
        Dialog dialog = new Dialog(context, R.style.SlideBottomAnimDialog);
        dialog.setContentView(view);
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.BOTTOM; // 靠近底部
            lp.width = WindowManager.LayoutParams.MATCH_PARENT; // 宽度
            window.setAttributes(lp);
        }
        return dialog;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        //保存对话框状态
        outState.putInt(TARGET_KEY, mTarget);
        super.onSaveInstanceState(outState);
    }
}
