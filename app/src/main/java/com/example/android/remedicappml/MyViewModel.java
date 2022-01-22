package com.example.android.remedicappml;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

class TimeTableViewModel extends ViewModel {

    private MutableLiveData<String> start_time_str = new MutableLiveData<>();

    void send_StartTime(String start_Time){
        start_time_str.setValue(start_Time);
    }

    LiveData<String> get_StartTime(){
        return start_time_str;
    }}