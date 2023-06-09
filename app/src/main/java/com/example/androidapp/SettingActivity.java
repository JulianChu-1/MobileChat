package com.example.androidapp;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SettingActivity extends AppCompatActivity {

    private EditText editKey, editKey2;
    private MyDatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private List<String> dataList = new ArrayList<>();
    private List<String> dataList2 = new ArrayList<>();
    private PopupWindow popupWindow;
    private MyAdapter adapter_1;
    public static String m_value;
    public static boolean m_signal = false;
    public static String m_bot_name;
    private AlertDialog.Builder builder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        editKey = findViewById(R.id.edit_key);
        editKey2 = findViewById(R.id.edit_bot_name);
        dbHelper = new MyDatabaseHelper(this);
        recyclerView = findViewById(R.id.recycler_view);
        loadDataFromDatabase();
        initRecyclerView();
        initEditText(editKey,editKey2);

        Button btnSubmit = findViewById(R.id.btn_submit);
        btnSubmit.setOnClickListener(v -> {
            String value = editKey.getText().toString();
            String bot_name = editKey2.getText().toString();

            saveDataToDatabase(value,bot_name);
            m_value = value;
            m_signal = true;
            m_bot_name = bot_name;

            loadDataFromDatabase();
            recyclerView.setAdapter(adapter_1);
            recyclerView.getAdapter().notifyDataSetChanged();
            editKey.setText("");
            editKey2.setText("");

            AlertShow();//提示框
        });

        ConstraintLayout constraintLayout = findViewById(R.id.backgroundView);
        constraintLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editKey.clearFocus();
                editKey2.clearFocus();
            }
        });
    }

    private void AlertShow(){
        builder = new AlertDialog.Builder(SettingActivity.this);

        builder.setTitle("提示");
        builder.setMessage(m_bot_name + "创建成功！");
        builder.setPositiveButton("确定", (dialog, which) -> {
            Toast.makeText(SettingActivity.this, "开始和" + m_bot_name +"聊天吧！",Toast.LENGTH_SHORT).show();
            finish();
        });
        // builder.setNeutralButton("取消", null);
        builder.show();
    }

    private void initRecyclerView() {
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter_1 = new MyAdapter(dataList,item -> {
            editKey.setText(item);
            editKey2.setText(getBotNameFromDatabase(item));
            dismissPopupWindow();
        });
    }

    private void loadDataFromDatabase() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {"API_KEY","BOT_NAME"};
        Cursor cursor = db.query("API_TABLE", projection, null, null, null, null, "ROWID DESC", "5");

        dataList.clear();
        while (cursor.moveToNext()) {
            int columnIndex = cursor.getColumnIndex("API_KEY");
            int columnIndex2 = cursor.getColumnIndex("BOT_NAME");
            if (columnIndex != -1&&columnIndex2 != -1) {
                String value = cursor.getString(columnIndex);
                String bot_name = cursor.getString(columnIndex2);
                dataList.add(value);
                dataList2.add(bot_name);
            } else {
                Log.e("MyApp", "Column 'API_KEY' not found in result set");
            }
        }
        cursor.close();
    }

    // 存入数据到数据库
    private void saveDataToDatabase(String value,String bot_name) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("API_KEY", value);
        values.put("BOT_NAME",bot_name);
        db.insertWithOnConflict("API_TABLE", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }
    
    private void initEditText(EditText e,EditText e2) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM API_TABLE";
        Cursor cursor_1 = db.rawQuery(query, null);
        cursor_1.moveToFirst();
        int count = cursor_1.getInt(0);
        cursor_1.close();
        if (count > 0) {
            String[] projection = {"API_KEY","BOT_NAME"};
            Cursor cursor = db.query("API_TABLE", projection, null, null, null, null, "ROWID DESC", "1");
            cursor.moveToLast();
            int columnIndex = cursor.getColumnIndex("API_KEY");
            int columnIndex2 = cursor.getColumnIndex("BOT_NAME");
            if (columnIndex != -1&&columnIndex2 != -1) {
                String value = cursor.getString(columnIndex);
                String bot_name = cursor.getString(columnIndex2);
                e.setText(value);
                e2.setText(bot_name);
            } else {
                Log.e("MyApp", "Column 'API_KEY' not found the last key");
            }
            cursor.close();
        } else {
            // e.setText("");
            e2.setText("");
        }

        e.setFocusable(true);
        e.setFocusableInTouchMode(true);
        e.setOnClickListener(v -> showPopupWindow());
    }
    private void showPopupWindow() {
        if (popupWindow == null) {
            View contentView = LayoutInflater.from(this).inflate(R.layout.popup_window, null);
            RecyclerView recyclerView = contentView.findViewById(R.id.recycler_view_pop);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter_1);
            popupWindow = new PopupWindow(contentView, editKey.getWidth(), ViewGroup.LayoutParams.WRAP_CONTENT);
            popupWindow.setFocusable(true);
            popupWindow.setOutsideTouchable(true);
        }
        popupWindow.showAsDropDown(editKey);
    }
    // ERROR ERROR ERROR

    private void dismissPopupWindow() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("last_close_time", System.currentTimeMillis());
        editor.apply();
        Intent serviceIntent = new Intent(this, MyService.class);
        startService(serviceIntent);
    }

    protected void onResume(){
        Intent serviceIntent = new Intent(this, MyService.class);
        stopService(serviceIntent);
        super.onResume();
    }

    public String getBotNameFromDatabase(String apiKey) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String botName = null;

        String[] projection = {"BOT_NAME"};
        String selection = "API_KEY = ?";
        String[] selectionArgs = {apiKey};

        Cursor cursor = db.query("API_TABLE", projection, selection, selectionArgs, null, null, null);
        if (cursor.moveToFirst()) {
            botName = cursor.getString(cursor.getColumnIndexOrThrow("BOT_NAME"));
        }

        cursor.close();
        db.close();

        return botName;
    }
}