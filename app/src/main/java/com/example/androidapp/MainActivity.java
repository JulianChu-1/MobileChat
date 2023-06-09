package com.example.androidapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    TextView welcomeTextView;
    EditText messageEditText;
    ImageButton sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;
    MyDatabaseHelper dbHelper;
    private AlertDialog.Builder builder;
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messageList = new ArrayList<>();
        dbHelper = new MyDatabaseHelper(this);

        initApiKey();
        recyclerView = findViewById(R.id.recycler_view);
        welcomeTextView = findViewById(R.id.welcome_text);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_btn);

        //setup recycler view
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        sendButton.setOnClickListener((v)->{
            if(SettingActivity.m_signal){
                String question = messageEditText.getText().toString().trim();
                addToChat(question,Message.SENT_BY_ME);
                messageEditText.setText("");
                callAPI(question,SettingActivity.m_value);
                welcomeTextView.setVisibility(View.GONE);
                SaveToTableDialogue(question,"ME");
            }
            else {
                AlertShow();
            }
        });
    }

    private void AlertShow(){
        builder = new AlertDialog.Builder(MainActivity.this);

        builder.setTitle("提示");
        builder.setMessage("请先前往设置输入SecretKey哦");
        builder.setPositiveButton("确定", (dialog, which) -> {});
        builder.show();
    }

    private void SaveToTableDialogue(String value, String type){
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(System.currentTimeMillis());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("Message", value);
        values.put("_TYPE",type);
        values.put("_DATETIME",formatter.format(date));
        values.put("API_KEY",SettingActivity.m_value);
        values.put("BOT_NAME",SettingActivity.m_bot_name);
        db.insert("Dialogue_TABLE", null, values);
        db.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.setting:
                intent = new Intent(MainActivity.this,SettingActivity.class);
                startActivity(intent);
                return true;
            case R.id.history:
                intent = new Intent(MainActivity.this,HistoryActivity.class);
                startActivity(intent);
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void addToChat(String message,String sentBy){
        runOnUiThread(() -> {
            messageList.add(new Message(message,sentBy));
            messageAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
        });
    }

    void addResponse(String response){
        messageList.remove(messageList.size()-1);
        addToChat(response,Message.SENT_BY_BOT);
    }

    void callAPI(String question, String api_key){
        //okhttp
        messageList.add(new Message("Typing... ",Message.SENT_BY_BOT));

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model","text-davinci-003");
            jsonBody.put("prompt",question);
            jsonBody.put("max_tokens",4000);
            jsonBody.put("temperature",0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(jsonBody.toString(),JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/completions")
                .header("Authorization","Bearer " + api_key)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to load response due to "+e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()){
                    JSONObject  jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getString("text");
                        addResponse(result.trim());
                        SaveToTableDialogue(result.trim(),"BOT");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else{
                    addResponse("Failed to load response due to "+response.body().toString());
                }
            }

        });
    }

    private void initApiKey() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM API_TABLE";
        Cursor cursor_1 = db.rawQuery(query, null);
        cursor_1.moveToFirst();
        int count = cursor_1.getInt(0);
        cursor_1.close();
        if (count > 0) {
            String[] projection = {"API_KEY"};
            Cursor cursor = db.query("API_TABLE", projection, null, null, null, null, "ROWID DESC", "1");
            cursor.moveToLast();
            int columnIndex = cursor.getColumnIndex("API_KEY");
            if (columnIndex != -1) {
                String api_key = cursor.getString(columnIndex);
                SettingActivity.m_value = api_key;
                SettingActivity.m_signal = true;
            } else {
                Log.e("MyApp", "Column 'API_KEY' not found the last key");
            }
            cursor.close();
        } else {
            SettingActivity.m_value="";
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
}

