package com.apicedecor.apiceclock;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResumeHoursActivity extends AppCompatActivity {
    private RecyclerView recyclerView;

    private ImageButton btnHome;
    private ResumenAdapter resumenAdapter;
    private List<DaySummary> resumenList = new ArrayList<>();
    private Button exportButton;

    private String nombre = "";
    private String apellido = "";
    private String correo = "";
    private String mes = "";

    private FirebaseFirestore db = FirebaseFirestore.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resume_hours);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.recyclerViewResumen);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        resumenAdapter = new ResumenAdapter(resumenList);
        btnHome = findViewById(R.id.btnHome);
        exportButton = findViewById(R.id.exportButton);
        recyclerView.setAdapter(resumenAdapter);

        exportButton = findViewById(R.id.exportButton);

        // Cargamos datos para mostrar al entrar
        loadWorkHours();

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(ResumeHoursActivity.this, ControlHoursActivity.class);
            startActivity(intent);
            finish();
        });

        exportButton.setOnClickListener(v -> {
            cargarDatosMes(); // carga el mes actual automáticamente
        });
    }

    private void cargarDatosMes() {
        String emailUsuario = getEmailUsuario();
        if (emailUsuario.isEmpty()) {
            Toast.makeText(this, "Usuario no logueado", Toast.LENGTH_SHORT).show();
            return;
        }

        String mesAnoActual = getMesAnoActual(); // "MM-yyyy" ejemplo: "05-2025"
        resumenList.clear();

        db.collection("workHours")
                .whereEqualTo("email", emailUsuario)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot primerDoc = queryDocumentSnapshots.getDocuments().get(0);
                        nombre = primerDoc.getString("name");
                        apellido = primerDoc.getString("surname");
                        correo = primerDoc.getString("email");

                        Map<String, Integer> minutosPorDia = new HashMap<>();

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String fecha = doc.getString("date"); // ej. "30-05-2025"
                            String totalHours = doc.getString("totalHours"); // ej. "00:18"

                            if (esMesDeInteres(fecha, mesAnoActual)) {
                                String[] partes = totalHours.split(":");
                                int h = Integer.parseInt(partes[0]);
                                int m = Integer.parseInt(partes[1]);
                                int totalMinutos = h * 60 + m;

                                minutosPorDia.put(fecha, minutosPorDia.getOrDefault(fecha, 0) + totalMinutos);
                            }
                        }

                        resumenList.clear();
                        for (String dia : minutosPorDia.keySet()) {
                            int totalMin = minutosPorDia.get(dia);
                            int horas = totalMin / 60;
                            int minutos = totalMin % 60;
                            DaySummary resumenDia = new DaySummary();
                            resumenDia.setDate(dia);
                            resumenDia.setName(nombre);
                            resumenDia.setSurname(apellido);
                            resumenDia.setEmail(correo);
                            resumenDia.setTotalHours(String.format(Locale.getDefault(), "%02d:%02d", horas, minutos));
                            resumenList.add(resumenDia);
                        }

                        resumenList.sort((o1, o2) -> {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                                Date d1 = sdf.parse(o1.getDate());
                                Date d2 = sdf.parse(o2.getDate());
                                return d1.compareTo(d2);
                            } catch (ParseException e) {
                                return 0;
                            }
                        });

                        resumenAdapter.notifyDataSetChanged();

                        mes = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date()); // mes legible
                        createPDF(); // Aquí llamas a la función que genere el PDF con resumenList
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean esMesDeInteres(String fechaDDMMYYYY, String mesAnoDeseado) {
        String[] partes = fechaDDMMYYYY.split("-");
        if (partes.length != 3) return false;
        String mes = partes[1];
        String anio = partes[2];

        String mesAno = mes + "-" + anio;
        return mesAno.equals(mesAnoDeseado);
    }

    private String getMesAnoActual() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy", Locale.getDefault());
        Date now = new Date();
        return sdf.format(now);
    }

    private String getEmailUsuario() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getEmail() : "";
    }

    private void createPDF() {
        if (resumenList.isEmpty()) {
            Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear documento PDF
        PdfDocument pdfDocument = new PdfDocument();

        // Crear página con tamaño A4 (595x842 puntos)
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(14);

        int y = 40;

        // Escribir cabecera con datos del usuario y mes
        canvas.drawText("Resumen de Horas - " + mes, 40, y, paint);
        y += 25;
        canvas.drawText("Nombre: " + nombre + " " + apellido, 40, y, paint);
        y += 20;
        canvas.drawText("Correo: " + correo, 40, y, paint);
        y += 30;

        // Escribir tabla: Fecha - Horas trabajadas
        paint.setTextSize(12);
        canvas.drawText("Fecha", 40, y, paint);
        canvas.drawText("Horas trabajadas", 200, y, paint);
        y += 20;

        // Línea separadora
        canvas.drawLine(40, y, 550, y, paint);
        y += 20;

        // Escribir cada resumen del día
        for (DaySummary ds : resumenList) {
            if (y > 800) break; // para evitar salir de la página

            canvas.drawText(ds.getDate(), 40, y, paint);
            canvas.drawText(ds.getTotalHours(), 200, y, paint);
            y += 20;
        }

        pdfDocument.finishPage(page);

        // Guardar archivo PDF en la carpeta Documents/ApiceDecor con nombre "Resumen-MM-YYYY.pdf"
        String fileName = "Resumen-" + new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(new Date()) + ".pdf";
        File directory = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "ApiceDecor");
        if (!directory.exists()) directory.mkdirs();

        File file = new File(directory, fileName);

        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF generado en:\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // *** ABRIR AUTOMÁTICAMENTE EL PDF ***
            Uri pdfUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider", // ¡Asegúrate de tenerlo configurado!
                    file
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al crear PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        pdfDocument.close();
    }


    private void loadWorkHours() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String userEmail = user.getEmail();

        FirebaseFirestore.getInstance().collection("workHours")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    resumenList.clear();

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String date = document.getString("date");
                        String totalHours = document.getString("totalHours");

                        if (date != null && totalHours != null) {
                            resumenList.add(new DaySummary(date, totalHours));
                        }
                    }

                    resumenList.sort((o1, o2) -> o2.getDate().compareTo(o1.getDate()));

                    if (resumenList.size() > 7) {
                        resumenList = resumenList.subList(0, 7);
                    }

                    resumenAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("ResumeHoursActivity", "Error al cargar datos: " + e.getMessage()));
    }
}