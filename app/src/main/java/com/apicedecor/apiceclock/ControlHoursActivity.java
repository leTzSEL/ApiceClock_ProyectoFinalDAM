package com.apicedecor.apiceclock;

import static android.text.format.DateUtils.formatElapsedTime;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ControlHoursActivity extends AppCompatActivity {

    private TextView tvTimer;
    private Button btnStartStop;
    private ImageButton btnSummary;
    private Handler handler = new Handler();

    private boolean isRunning = false;
    private long startTimeMillis = 0L;
    private long elapsedMillis = 0L;

    private Runnable timerRunnable;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    private static final String PREFS_NAME = "timerPrefs";
    private static final String KEY_START_TIME = "startTimeMillis";

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_control_hours);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvTimer = findViewById(R.id.tvTimer);
        btnStartStop = findViewById(R.id.btnStartStop);
        btnSummary = findViewById(R.id.btnSummary);

        mAuth = FirebaseAuth.getInstance();

        databaseRef = FirebaseDatabase.getInstance().getReference("workhours");


        btnStartStop.setOnClickListener(v -> {
            if (!isRunning) {
                startTimer();
            } else {
                stopTimer();
            }
        });

        btnSummary.setOnClickListener(v -> {
            // Navega a pantalla ResumeHoursActivity
            Intent intent = new Intent(ControlHoursActivity.this, ResumeHoursActivity.class);
            startActivity(intent);
        });
    }

    private void startTimer() {
        isRunning = true;
        btnStartStop.setText("DETENER");
        btnStartStop.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_light));

        startTimeMillis = System.currentTimeMillis();
        elapsedMillis = 0L;

        saveWorkrHour(true);
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                elapsedMillis = System.currentTimeMillis() - startTimeMillis;
                tvTimer.setText(formatElapsedTime(elapsedMillis));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timerRunnable);
    }

    private void stopTimer() {
        isRunning = false;
        btnStartStop.setText("INICIAR");
        btnStartStop.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_dark));

        handler.removeCallbacks(timerRunnable);

        saveWorkrHour(false); // Guardar fin
    }

    private String formatElapsedTime(long milis) {
        long totalSeconds = milis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    //Funciones trabajador
    private void saveWorkrHour(boolean isStart) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String userId = user.getUid();
            String email = user.getEmail();

            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String surname = documentSnapshot.getString("surname");

                            // Fecha y hora actuales
                            Date now = new Date();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                            dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));

                            SimpleDateFormat hourFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                            hourFormat.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));

                            String date = dateFormat.format(now);
                            String hour = hourFormat.format(now);

                            // ID único por usuario y día
                            String documentId = userId + "_" + date;

                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            DocumentReference workHourRef = db.collection("workHours").document(documentId);

                            if (isStart) {
                                // Guardamos datos de INICIO
                                Map<String, Object> workData = new HashMap<>();
                                workData.put("name", name);
                                workData.put("surname", surname);
                                workData.put("email", email);
                                workData.put("date", date);
                                workData.put("startTime", hour);
                                workData.put("startTimestamp", System.currentTimeMillis());

                                workHourRef.set(workData)
                                        .addOnSuccessListener(aVoid -> Log.d("ControlHoursActivity", "Datos de INICIO guardados"))
                                        .addOnFailureListener(e -> Log.e("ControlHoursActivity", "Error al guardar datos de INICIO: " + e.getMessage()));

                            } else {
                                // Guardamos datos de FIN y calculamos duración
                                long endTimestamp = System.currentTimeMillis();

                                workHourRef.get().addOnSuccessListener(documentSnapshot1 -> {
                                    if (documentSnapshot1.exists() && documentSnapshot1.contains("startTimestamp")) {
                                        long startTimestamp = documentSnapshot1.getLong("startTimestamp");

                                        // Calcular diferencia
                                        long diffMillis = endTimestamp - startTimestamp;
                                        long totalSeconds = diffMillis / 1000;
                                        long hours = totalSeconds / 3600;
                                        long minutes = (totalSeconds % 3600) / 60;

                                        // Formato final: "HH:mm"
                                        String totalHoursFormatted = String.format(Locale.getDefault(), "%02d:%02d", hours, minutes);

                                        Map<String, Object> endData = new HashMap<>();
                                        endData.put("endTime", hour);
                                        endData.put("endTimestamp", endTimestamp);
                                        endData.put("totalHours", totalHoursFormatted);

                                        workHourRef.update(endData)
                                                .addOnSuccessListener(aVoid -> Log.d("ControlHoursActivity", "Datos de FIN y totalHours guardados"))
                                                .addOnFailureListener(e -> Log.e("ControlHoursActivity", "Error al guardar datos de FIN: " + e.getMessage()));

                                    } else {
                                        Log.e("ControlHoursActivity", "No existe startTimestamp para calcular duración");
                                    }
                                });
                            }

                        } else {
                            Log.e("ControlHoursActivity", "No se encontraron datos del usuario.");
                        }
                    }).addOnFailureListener(e -> {
                        Log.e("ControlHoursActivity", "Error al obtener datos del usuario: " + e.getMessage());
                    });
        }
    }

}