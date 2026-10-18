package com.ai.assistant;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText etInput;
    private ImageButton btnSend;
    private ChatAdapter adapter;
    private List<Message> messageList;
    private OkHttpClient client;

    // أدخل مفتاح API الخاِص بك هنا
    private static final String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerViewChat);
        etInput = findViewById(R.id.etInput);
        btnSend = findViewById(R.id.btnSend);

        client = new OkHttpClient();
        messageList = new ArrayList<>();
        
        messageList.add(new Message("مرحباً بك! أنا مساعدك الذكي الاحترافي. كيف يمكنني مساعدتك اليوم؟", Message.TYPE_AI));

        adapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        btnSend.setOnClickListener(v -> {
            String query = etInput.getText().toString().trim();
            if (!query.isEmpty()) {
                sendMessage(query);
            }
        });
    }

    private void sendMessage(String query) {
        messageList.add(new Message(query, Message.TYPE_USER));
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
        etInput.setText("");

        messageList.add(new Message("جاري التفكير...", Message.TYPE_AI));
        int loadingPos = messageList.size() - 1;
        adapter.notifyItemInserted(loadingPos);
        recyclerView.scrollToPosition(loadingPos);

        callGeminiApi(query, loadingPos);
    }

    private void callGeminiApi(String query, int loadingPos) {
        try {
            JSONObject jsonBody = new JSONObject();
            JSONArray contentsArr = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray partsArr = new JSONArray();
            JSONObject partObj = new JSONObject();

            partObj.put("text", query);
            partsArr.put(partObj);
            contentObj.put("parts", partsArr);
            contentsArr.put(contentObj);
            jsonBody.put("contents", contentsArr);

            RequestBody body = RequestBody.create(
                    jsonBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    updateAiResponse(loadingPos, "تعذر الاتصال بالخادم. تحقق من الاتصال بالإنترنت.");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String responseData = response.body().string();
                            JSONObject jsonResponse = new JSONObject(responseData);
                            JSONArray candidates = jsonResponse.getJSONArray("candidates");
                            JSONObject candidate = candidates.getJSONObject(0);
                            JSONObject content = candidate.getJSONObject("content");
                            JSONArray parts = content.getJSONArray("parts");
                            String aiReply = parts.getJSONObject(0).getString("text");

                            updateAiResponse(loadingPos, aiReply);
                        } catch (Exception e) {
                            updateAiResponse(loadingPos, "حدث خطأ أثناء معالجة الرد.");
                        }
                    } else {
                        updateAiResponse(loadingPos, "يرجى إضافة مفتاح API Key صحيح من Google AI Studio.");
                    }
                }
            });

        } catch (Exception e) {
            updateAiResponse(loadingPos, "حدث خطأ غير متوقع.");
        }
    }

    private void updateAiResponse(int position, String responseText) {
        new Handler(Looper.getMainLooper()).post(() -> {
            messageList.set(position, new Message(responseText, Message.TYPE_AI));
            adapter.notifyItemChanged(position);
            recyclerView.scrollToPosition(messageList.size() - 1);
        });
    }
}
