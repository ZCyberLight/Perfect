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
import com.cyberlight.perfect.util.DateTimeFormatUtil;
import com.cyberlight.perfect.widget.DateWheelPicker;
import com.cyberlight.perfect.widget.IntegerWheelPicker;

public class DateHourMinutePickerDialogFragment extends DialogFragment {

    public static final String TAG = "DateHourMinutePickerDialogFragment";

    // 用于FragmentResultListener传递结果、bundle初始化、
    // savedInstanceState状态恢复等
    public static final String DHM_REQUEST_KEY = "dhm_request_key";
    public static final String DHM_YEAR_KEY = "dhm_year_key";
    public static final String DHM_MONTH_KEY = "dhm_month_key";
    public static final String DHM_DAY_OF_MONTH_KEY = "dhm_day_of_month_key";
    public static final String DHM_HOUR_KEY = "dhm_hour_key";
    public static final String DHM_MINUTE_KEY = "dhm_minute_key";

    private int mSelectedYear;
    private int mSelectedMonth;
    private int mSelectedDayOfMonth;
    private int mSelectedHour;
    private int mSelectedMinute;

    public DateHourMinutePickerDialogFragment() {
    }

    public static DateHourMinutePickerDialogFragment newInstance(int year, int month, int dayOfMonth
            , int hour, int minute) {
        DateHourMinutePickerDialogFragment fragment = new DateHourMinutePickerDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(DHM_YEAR_KEY, year);
        bundle.putInt(DHM_MONTH_KEY, month);
        bundle.putInt(DHM_DAY_OF_MONTH_KEY, dayOfMonth);
        bundle.putInt(DHM_HOUR_KEY, hour);
        bundle.putInt(DHM_MINUTE_KEY, minute);
        fragment.setArguments(bundle);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // 获取并设置初始时间
        Bundle bundle = getArguments();
        if (bundle != null) {
            mSelectedYear = bundle.getInt(DHM_YEAR_KEY);
            mSelectedMonth = bundle.getInt(DHM_MONTH_KEY);
            mSelectedDayOfMonth = bundle.getInt(DHM_DAY_OF_MONTH_KEY);
            mSelectedHour = bundle.getInt(DHM_HOUR_KEY);
            mSelectedMinute = bundle.getInt(DHM_MINUTE_KEY);
        }
        // 恢复对话框状态
        if (savedInstanceState != null) {
            mSelectedYear = savedInstanceState.getInt(DHM_YEAR_KEY);
            mSelectedMonth = savedInstanceState.getInt(DHM_MONTH_KEY);
            mSelectedDayOfMonth = savedInstanceState.getInt(DHM_DAY_OF_MONTH_KEY);
            mSelectedHour = savedInstanceState.getInt(DHM_HOUR_KEY);
            mSelectedMinute = savedInstanceState.getInt(DHM_MINUTE_KEY);
        }
        // 设置对话框布局
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_dhm_picker, null);
        // 将初始时间赋给日期指示textView
        TextView mIndicatorTv =
                view.findViewById(R.id.dialog_dhm_indicator_tv);
        mIndicatorTv.setText(DateTimeFormatUtil
                .getReadableDateAndDayOfWeek(mSelectedYear, mSelectedMonth, mSelectedDayOfMonth));
        // 获取三个选择器
        DateWheelPicker mDateWp = view.findViewById(R.id.dialog_dhm_date_wp);
        IntegerWheelPicker mHourWp = view.findViewById(R.id.dialog_dhm_hour_wp);
        IntegerWheelPicker mMinuteWp = view.findViewById(R.id.dialog_dhm_minute_wp);
        // 初始化三个选择器的选中项
        mDateWp.setSelectedDate(mSelectedYear, mSelectedMonth, mSelectedDayOfMonth, false);
        mHourWp.setSelectedValue(mSelectedHour, false);
        mMinuteWp.setSelectedValue(mSelectedMinute, false);
        // 对三个选择器设置选中监听
        mDateWp.setOnDateSelectedListener((year, month, dayOfMonth) -> {
            mSelectedYear = year;
            mSelectedMonth = month;
            mSelectedDayOfMonth = dayOfMonth;
            mIndicatorTv.setText(DateTimeFormatUtil
                    .getReadableDateAndDayOfWeek(mSelectedYear, mSelectedMonth, mSelectedDayOfMonth));
        });
        mHourWp.setOnValueSelectedListener(value -> mSelectedHour = value);
        mMinuteWp.setOnValueSelectedListener(value -> mSelectedMinute = value);
        // 设置取消和确认按钮
        TextView mCancelTv = view.findViewById(R.id.dialog_dhm_cancel_tv);
        TextView mConfirmTv = view.findViewById(R.id.dialog_dhm_confirm_tv);
        mCancelTv.setOnClickListener(v -> dismiss());
        mConfirmTv.setOnClickListener(v -> {
            // 将对话框选择的时间通过setFragmentResult返回给Activity
            Bundle result = new Bundle();
            result.putInt(DHM_YEAR_KEY, mSelectedYear);
            result.putInt(DHM_MONTH_KEY, mSelectedMonth);
            result.putInt(DHM_DAY_OF_MONTH_KEY, mSelectedDayOfMonth);
            result.putInt(DHM_HOUR_KEY, mSelectedHour);
            result.putInt(DHM_MINUTE_KEY, mSelectedMinute);
            getParentFragmentManager().setFragmentResult(DHM_REQUEST_KEY, result);
            dismiss();
        });
        // 设置对话框
        Dialog dialog = new Dialog(getContext(), R.style.SlideBottomAnimDialog);
        dialog.setContentView(view);
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.gravity = Gravity.BOTTOM; // 靠近底部
            lp.y = 80;// 与底部距离
            lp.width = getResources().getDisplayMetrics().widthPixels / 8 * 7; // 宽度
            window.setAttributes(lp);
        }
        return dialog;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // 保存对话框状态
        outState.putInt(DHM_YEAR_KEY, mSelectedYear);
        outState.putInt(DHM_MONTH_KEY, mSelectedMonth);
        outState.putInt(DHM_DAY_OF_MONTH_KEY, mSelectedDayOfMonth);
        outState.putInt(DHM_HOUR_KEY, mSelectedHour);
        outState.putInt(DHM_MINUTE_KEY, mSelectedMinute);
        super.onSaveInstanceState(outState);
    }

}