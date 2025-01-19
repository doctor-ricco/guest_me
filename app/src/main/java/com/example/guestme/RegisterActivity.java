package com.example.guestme;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText firstNameInput = findViewById(R.id.firstNameInput);
        EditText lastNameInput = findViewById(R.id.lastNameInput);
        EditText dateOfBirthInput = findViewById(R.id.dateOfBirthInput);
        EditText emailInput = findViewById(R.id.emailInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        EditText confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        Spinner userTypeSpinner = findViewById(R.id.userTypeSpinner);
        Button registerButton = findViewById(R.id.registerButton);

        dateOfBirthInput.setOnClickListener(view -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (datePicker, selectedYear, selectedMonth, selectedDay) -> {
                String formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                dateOfBirthInput.setText(formattedDate);
            }, year, month, day);
            datePickerDialog.show();
        });

        registerButton.setOnClickListener(view -> {
            String firstName = firstNameInput.getText().toString();
            String lastName = lastNameInput.getText().toString();
            String dateOfBirth = dateOfBirthInput.getText().toString();
            String email = emailInput.getText().toString();
            String password = passwordInput.getText().toString();
            String confirmPassword = confirmPasswordInput.getText().toString();
            String userType = userTypeSpinner.getSelectedItem().toString();

            if (!firstName.isEmpty() && !lastName.isEmpty() && !dateOfBirth.isEmpty() &&
                    !email.isEmpty() && !password.isEmpty() && !confirmPassword.isEmpty()) {

                if (password.equals(confirmPassword)) {
                    if (isOver18(dateOfBirth)) {
                        registerUser(firstName, lastName, dateOfBirth, email, password, userType);
                    } else {
                        Toast.makeText(this, "Você precisa ter mais de 18 anos para se registrar.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "As senhas não coincidem!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            }
        });

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(view -> {
            // Finaliza a atividade atual e retorna à anterior
            finish();
        });

    }

    private boolean isOver18(String dateOfBirth) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        try {
            Date birthDate = sdf.parse(dateOfBirth);
            Calendar today = Calendar.getInstance();
            Calendar birthCalendar = Calendar.getInstance();
            birthCalendar.setTime(birthDate);

            int age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }

            return age >= 18;
        } catch (ParseException e) {
            Toast.makeText(this, "Data de nascimento inválida.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void registerUser(String firstName, String lastName, String dateOfBirth, String email, String password, String userType) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("firstName", firstName);
                        userData.put("lastName", lastName);
                        userData.put("dateOfBirth", dateOfBirth);
                        userData.put("email", email);
                        userData.put("type", userType);

                        FirebaseFirestore.getInstance().collection("users")
                                .document(userId)
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Registro concluído com sucesso! Bem-vindo(a), " + firstName + "!", Toast.LENGTH_LONG).show();

                                    // Redirecionar com base no tipo de usuário
                                    if ("Host".equals(userType)) {
                                        Intent intent = new Intent(RegisterActivity.this, HostActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else if ("Visitor".equals(userType)) {
                                        Intent intent = new Intent(RegisterActivity.this, VisitorActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Tipo de usuário desconhecido.", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao salvar dados: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Erro: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


}
