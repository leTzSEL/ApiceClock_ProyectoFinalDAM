package com.apicedecor.apiceclock;

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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        databaseRef = FirebaseDatabase.getInstance().getReference("workhours");

        startTimeMillis = prefs.getLong(KEY_START_TIME, 0L);
        if (startTimeMillis != 0L) {
            // Si había cronómetro en marcha antes, lo ponemos en marcha y calculamos elapsedMillis actual
            isRunning = true;
            btnStartStop.setText("DETENER");
            btnStartStop.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_light));

            elapsedMillis = System.currentTimeMillis() - startTimeMillis;

            startTimerRunnable();
        }

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

        // Guarda el tiempo de inicio en SharedPreferences
        prefs.edit().putLong(KEY_START_TIME, startTimeMillis).apply();

        elapsedMillis = 0L;

        saveWorkrHour(true);
        startTimerRunnable();
    }

    private void startTimerRunnable() {
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

        handler.removeCallbacks(timerRunnable);

        // Borra el tiempo guardado porque paramos el cronómetro
        prefs.edit().remove(KEY_START_TIME).apply();

        saveWorkrHour(false);

    }

    private String formatElapsedTime(long milis) {
        long totalSeconds = milis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

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
                                Map<String, Object> startInterval = new HashMap<>();
                                startInterval.put("startTime", hour);
                                startInterval.put("startTimestamp", System.currentTimeMillis());

                                // Añadir el nuevo intervalo con solo el inicio (end se completará después)
                                workHourRef.get().addOnSuccessListener(docSnap -> {
                                    if (docSnap.exists()) {
                                        // Ya existe: añadir el intervalo al array
                                        workHourRef.update("intervals", FieldValue.arrayUnion(startInterval))
                                                .addOnSuccessListener(aVoid -> Log.d("ControlHoursActivity", "Nuevo intervalo de inicio añadido"))
                                                .addOnFailureListener(e -> Log.e("ControlHoursActivity", "Error al añadir intervalo: " + e.getMessage()));
                                    } else {
                                        // No existe: crear documento con datos generales y el primer intervalo
                                        Map<String, Object> workData = new HashMap<>();
                                        workData.put("name", name);
                                        workData.put("surname", surname);
                                        workData.put("email", email);
                                        workData.put("date", date);
                                        workData.put("intervals", Collections.singletonList(startInterval));

                                        workHourRef.set(workData)
                                                .addOnSuccessListener(aVoid -> Log.d("ControlHoursActivity", "Documento creado con primer intervalo"))
                                                .addOnFailureListener(e -> Log.e("ControlHoursActivity", "Error al crear documento: " + e.getMessage()));
                                    }
                                });

                            } else {
                                // Guardamos datos de FIN y calculamos duración del intervalo
                                long endTimestamp = System.currentTimeMillis();

                                workHourRef.get().addOnSuccessListener(docSnap -> {
                                    if (docSnap.exists()) {
                                        List<Map<String, Object>> intervals = (List<Map<String, Object>>) docSnap.get("intervals");
                                        if (intervals != null && !intervals.isEmpty()) {
                                            // Buscar el último intervalo sin endTime
                                            Map<String, Object> lastInterval = intervals.get(intervals.size() - 1);
                                            if (!lastInterval.containsKey("endTime")) {
                                                long startTimestamp = (long) lastInterval.get("startTimestamp");
                                                long diffMillis = endTimestamp - startTimestamp;
                                                long totalSeconds = diffMillis / 1000;
                                                long hours = totalSeconds / 3600;
                                                long minutes = (totalSeconds % 3600) / 60;

                                                String totalHoursFormatted = String.format(Locale.getDefault(), "%02d:%02d", hours, minutes);

                                                lastInterval.put("endTime", hour);
                                                lastInterval.put("endTimestamp", endTimestamp);
                                                lastInterval.put("duration", totalHoursFormatted);

                                                // Actualizar el campo intervals en Firestore
                                                workHourRef.update("intervals", intervals)
                                                        .addOnSuccessListener(aVoid -> Log.d("ControlHoursActivity", "Intervalo actualizado con FIN"))
                                                        .addOnFailureListener(e -> Log.e("ControlHoursActivity", "Error al actualizar intervalo: " + e.getMessage()));

                                                // Calcular y actualizar el total del día
                                                long totalMillis = 0;
                                                for (Map<String, Object> interval : intervals) {
                                                    if (interval.containsKey("startTimestamp") && interval.containsKey("endTimestamp")) {
                                                        long s = (long) interval.get("startTimestamp");
                                                        long e = (long) interval.get("endTimestamp");
                                                        totalMillis += (e - s);
                                                    }
                                                }

                                                long totalSecondsDay = totalMillis / 1000;
                                                long totalHoursDay = totalSecondsDay / 3600;
                                                long totalMinutesDay = (totalSecondsDay % 3600) / 60;
                                                String totalDayFormatted = String.format(Locale.getDefault(), "%02d:%02d", totalHoursDay, totalMinutesDay);

                                                workHourRef.update("totalHours", totalDayFormatted)
                                                        .addOnSuccessListener(aVoid -> Log.d("ControlHoursActivity", "Total diario actualizado: " + totalDayFormatted))
                                                        .addOnFailureListener(e -> Log.e("ControlHoursActivity", "Error al actualizar total diario: " + e.getMessage()));
                                            }
                                        }
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