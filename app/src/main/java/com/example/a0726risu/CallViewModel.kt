package com.example.a0726risu

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class CallViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences =
        application.getSharedPreferences("call_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _callList = MutableStateFlow<List<CallInfo>>(emptyList())
    val callList: StateFlow<List<CallInfo>> = _callList

    val notifications: StateFlow<List<NotificationInfo>> = NotificationRepository.notifications

    init {
        loadCallList()
    }

    fun addCallInfo(name: String, number: String, time: String, daysOfWeek: Set<Int>) {
        val newInfo = CallInfo(name = name, number = number, time = time, daysOfWeek = daysOfWeek)
        _callList.value += newInfo
        saveCallList()
    }

    fun deleteCallInfo(id: String) {
        _callList.value = _callList.value.filterNot { it.id == id }
        saveCallList()
    }

    fun getCallInfoById(id: String): CallInfo? {
        return _callList.value.find { it.id == id }
    }

    fun updateCallInfo(id: String, name: String, number: String, time: String, daysOfWeek: Set<Int>) {
        val updatedList = _callList.value.map { info ->
            if (info.id == id) {
                info.copy(name = name, number = number, time = time, daysOfWeek = daysOfWeek)
            } else {
                info
            }
        }
        _callList.value = updatedList
        saveCallList()
    }

    private fun saveCallList() {
        viewModelScope.launch(Dispatchers.IO) {
            val jsonString = gson.toJson(_callList.value)
            sharedPreferences.edit {
                putString("call_list_key", jsonString)
            }
        }
    }

    private fun loadCallList() {
        viewModelScope.launch(Dispatchers.IO) {
            val jsonString = sharedPreferences.getString("call_list_key", null)
            if (jsonString != null) {
                val type = object : TypeToken<List<CallInfo>>() {}.type
                _callList.value = gson.fromJson(jsonString, type)
            }
        }
    }
}
