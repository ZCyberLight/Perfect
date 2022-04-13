package com.cyberlight.perfect.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cyberlight.perfect.R;
import com.cyberlight.perfect.model.FocusRecord;
import com.cyberlight.perfect.util.DateTimeFormatUtil;

import java.util.List;

public class FocusRecordRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_RECORD = 0;
    private static final int TYPE_NO_RECORD = 1;
    private static final int TYPE_INDICATOR = 2;

    private final Context mContext;
    private final List<FocusRecord> mFocusRecords;


    public FocusRecordRecyclerAdapter(Context context, List<FocusRecord> focusRecords) {
        mContext = context;
        mFocusRecords = focusRecords;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_RECORD) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.rv_focus_record, parent, false);
            return new RecordViewHolder(v);
        } else if (viewType == TYPE_NO_RECORD) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.rv_no_focus_record, parent, false);
            return new NoRecordViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.rv_focus_record_indicator, parent, false);
            return new IndicatorViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof RecordViewHolder) {
            RecordViewHolder recordViewHolder = (RecordViewHolder) holder;
            FocusRecord focusRecord = mFocusRecords.get(position - 1);
            recordViewHolder.completionTimeTv.setText(
                    DateTimeFormatUtil.getNeatDateTime(focusRecord.completionTime)
            );
            recordViewHolder.focusDurationTv.setText(
                    mContext.getString(R.string.main_rv_focus_duration,
                            focusRecord.focusDuration / 60000)
            );
        }
    }

    @Override
    public int getItemCount() {
        return mFocusRecords.size() > 0 ? mFocusRecords.size() + 1 : 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (mFocusRecords.size() > 0)
            return position == 0 ? TYPE_INDICATOR : TYPE_RECORD;
        else
            return TYPE_NO_RECORD;
    }

    private static class RecordViewHolder extends RecyclerView.ViewHolder {
        private final TextView completionTimeTv;
        private final TextView focusDurationTv;

        public RecordViewHolder(View v) {
            super(v);
            completionTimeTv = v.findViewById(R.id.rv_focus_record_completion_time_tv);
            focusDurationTv = v.findViewById(R.id.rv_focus_record_focus_duration_tv);
        }
    }

    private static class NoRecordViewHolder extends RecyclerView.ViewHolder {
        public NoRecordViewHolder(View v) {
            super(v);
        }
    }

    private static class IndicatorViewHolder extends RecyclerView.ViewHolder {
        public IndicatorViewHolder(View v) {
            super(v);
        }
    }
}