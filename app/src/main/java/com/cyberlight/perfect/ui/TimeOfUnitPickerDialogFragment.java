package com.cyberlight.perfect.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.cyberlight.perfect.R;
import com.cyberlight.perfect.widget.IntegerWheelPicker;
import com.cyberlight.perfect.widget.UnitWheelPicker;

import java.util.Locale;

public class TimeOfUnitPickerDialogFragment extends DialogFragment {
    public static final String TAG = "TimeOfUnitPickerDialogFragment";
    public static final String TU_REQUEST_KEY = "tu_request_key";
    public static final String TU_VALUE_KEY = "tu_value_key";
    public static final String TU_UNIT_KEY = "tu_unit_key";

    private int mSelectedValue;
    private int mSelectedUnit;

    //借助内存优化语法规则支持性能
    private String mLanguage;
    private String zhLanguage;

    public TimeOfUnitPickerDialogFragment() {
    }

    public static TimeOfUnitPickerDialogFragment newInstance(int initValue, int initUnit) {
        TimeOfUnitPickerDialogFragment fragment = new TimeOfUnitPickerDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(TU_VALUE_KEY, initValue);
        bundle.putInt(TU_UNIT_KEY, initUnit);
        fragment.setArguments(bundle);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //获取并设置初始时间
        Bundle bundle = getArguments();
        if (bundle != null) {
            mSelectedValue = bundle.getInt(TU_VALUE_KEY);
            mSelectedUnit = bundle.getInt(TU_UNIT_KEY);
        }
        //恢复对话框状态
        if (savedInstanceState != null) {
            mSelectedValue = savedInstanceState.getInt(TU_VALUE_KEY);
            mSelectedUnit = savedInstanceState.getInt(TU_UNIT_KEY);
        }
        //保存经常要判断的两个字符串，优化性能
        mLanguage = Locale.getDefault().getLanguage();
        zhLanguage = new Locale("zh").getLanguage();
        //设置布局
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_tu_picker, null);
        //初始化两个选择器
        IntegerWheelPicker mIntegerWheelPicker = view.findViewById(R.id.tu_value_picker);
        UnitWheelPicker mUnitWheelPicker = view.findViewById(R.id.tu_unit_picker);
        //将初始时间赋给两个选择器
        mIntegerWheelPicker.setSelectedValue(mSelectedValue, false);
        mUnitWheelPicker.setSelectedUnit(mSelectedUnit, false);
        //对两个选择器设置选中监听
        mIntegerWheelPicker.setOnValueSelectedListener(value -> {
            //只有在系统为英语并且原来选中1或即将选中1，才更新UnitPicker数据集
            if (!mLanguage.equals(zhLanguage) && (value == 1 || mSelectedValue == 1)) {
                mUnitWheelPicker.updateDataList(value);
            }
            mSelectedValue = value;
        });
        mUnitWheelPicker.setOnUnitSelectedListener(unit -> mSelectedUnit = unit);
        //把初始值告知UnitPicker，以保证复数等语法规则一开始就正确生效
        if (!mLanguage.equals(zhLanguage)) {
            mUnitWheelPicker.updateDataList(mSelectedValue);
        }
        //获取取消和确认按钮
        TextView mCancelBtnTv = view.findViewById(R.id.dialog_tu_picker_cancel_btn_tv);
        TextView mConfirmBtnTv = view.findViewById(R.id.dialog_tu_picker_confirm_btn_tv);
        mCancelBtnTv.setOnClickListener(v -> dismiss());
        mConfirmBtnTv.setOnClickListener(v -> {
            //将对话框选择的时间返回给Activity
            Bundle result = new Bundle();
            result.putInt(TU_VALUE_KEY, mSelectedValue);
            result.putInt(TU_UNIT_KEY, mSelectedUnit);
            getParentFragmentManager().setFragmentResult(TU_REQUEST_KEY, result);
            dismiss();
        });
        //设置对话框
        Dialog dialog = new Dialog(getContext(), R.style.SlideBottomAnimDialog);
        dialog.setContentView(view);
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.BOTTOM; // 紧贴底部
            lp.y = 80;// 与底部距离
            lp.width = getResources().getDisplayMetrics().widthPixels / 8 * 7; // 宽度
            window.setAttributes(lp);
        }
        return dialog;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        //保存对话框状态
        outState.putInt(TU_VALUE_KEY, mSelectedValue);
        outState.putInt(TU_UNIT_KEY, mSelectedUnit);
        super.onSaveInstanceState(outState);
    }

}