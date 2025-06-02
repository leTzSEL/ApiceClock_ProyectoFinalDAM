package com.apicedecor.apiceclock;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth autenticacionFirebase;
    private FirebaseFirestore firestore;
    private EditText emailInput, passwordInput, nameInput, surnameInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        autenticacionFirebase = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        nameInput = findViewById(R.id.nameInput);
        surnameInput = findViewById(R.id.surnameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);

        Button btnUserRegistrado = findViewById(R.id.UserRegisteredButton);

        btnUserRegistrado.setOnClickListener(view -> {
            String name = nameInput.getText().toString().trim();
            String surname = surnameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (!name.isEmpty() && !surname.isEmpty() && !email.isEmpty() && !password.isEmpty()) {
                autenticacionFirebase.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = autenticacionFirebase.getCurrentUser();
                        String userId = user.getUid();

                        //Guardamos en Firestore
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("name", name);
                        userMap.put("surname", surname);
                        userMap.put("email", email);

                        firestore.collection("users").document(userId).set(userMap).addOnSuccessListener(aVoid -> {
                            Toast.makeText(RegisterActivity.this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show();

                            //vuelve a MainActivity
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finish();//eliminamos la pantalla de la carga
                        }).addOnFailureListener(e -> {
                            Toast.makeText(RegisterActivity.this, "Error al guardar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

                    } else {
                        Toast.makeText(RegisterActivity.this, "Error al registrarse" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(RegisterActivity.this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show();
            }
        });
    }
}