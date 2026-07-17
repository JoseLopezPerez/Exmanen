package com.example.examen;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.examen.databinding.ActivityMainBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inicializamos la conexión a Realtime Database en el nodo "nominas"
        try {
            dbRef = FirebaseDatabase.getInstance().getReference("nominas");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error de conexión con Firebase", Toast.LENGTH_LONG).show();
        }

        // Programamos el evento del botón Calcular y Grabar
        binding.btnCalcularGrabar.setOnClickListener(v -> {
            calcularYGrabarNomina();
        });

        // Escuchador en tiempo real: Cada vez que se guarde algo en la BD, se actualizará la pantalla
        escucharUltimoRegistroBD();
    }

    private void calcularYGrabarNomina() {
        // 1. Capturar y validar que los campos no estén vacíos
        String empleado = binding.inputEmpleado.getText().toString().trim();
        String sueldoHoraStr = binding.inputSueldoHora.getText().toString().trim();
        String horasDiariasStr = binding.inputHorasDiarias.getText().toString().trim();
        String totalDiasStr = binding.inputTotalDias.getText().toString().trim();

        if (empleado.isEmpty() || sueldoHoraStr.isEmpty() || horasDiariasStr.isEmpty() || totalDiasStr.isEmpty()) {
            Toast.makeText(this, "Por favor, llene todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 2. Convertir textos a números para operar
            double sueldoHora = Double.parseDouble(sueldoHoraStr);
            int horasDiarias = Integer.parseInt(horasDiariasStr);
            int totalDias = Integer.parseInt(totalDiasStr);

            // 3. FÓRMULAS MATEMÁTICAS (Sueldo Total y descuento IESS de 9.45%)
            double sueldoTotal = sueldoHora * horasDiarias * totalDias;
            double descuentoIess = sueldoTotal * 0.0945;
            double sueldoNeto = sueldoTotal - descuentoIess;

            // 4. GUARDAR EN FIREBASE REALTIME DATABASE
            if (dbRef != null) {
                // Creamos el mapa de datos
                Map<String, Object> nomina = new HashMap<>();
                nomina.put("empleado", empleado);
                nomina.put("sueldo_total", sueldoTotal);
                nomina.put("sueldo_neto", sueldoNeto);
                nomina.put("timestamp", System.currentTimeMillis());

                // Guardamos con un ID único push()
                dbRef.push().setValue(nomina)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(MainActivity.this, "¡Sueldo Neto grabado con éxito!", Toast.LENGTH_SHORT).show();

                            // Limpiamos el formulario
                            binding.inputEmpleado.setText("");
                            binding.inputSueldoHora.setText("");
                            binding.inputHorasDiarias.setText("");
                            binding.inputTotalDias.setText("");
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(MainActivity.this, "Error al grabar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Por favor, ingrese valores numéricos válidos", Toast.LENGTH_SHORT).show();
        }
    }

    // Método para ESCUCHAR los datos guardados desde la Base de Datos y presentarlos
    private void escucharUltimoRegistroBD() {
        if (dbRef == null) return;

        // Consultamos el último registro agregado basándonos en el tiempo
        dbRef.orderByChild("timestamp").limitToLast(1).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        // Extraemos la información de la base de datos
                        String empleado = childSnapshot.child("empleado").getValue(String.class);
                        Double sueldoTotal = childSnapshot.child("sueldo_total").getValue(Double.class);
                        Double sueldoNeto = childSnapshot.child("sueldo_neto").getValue(Double.class);

                        if (empleado != null && sueldoTotal != null && sueldoNeto != null) {
                            // Seteamos la información en la pantalla (Sección "Resultados desde la BD")
                            binding.txtResultadoSueldoTotal.setText(String.format(Locale.getDefault(), "Sueldo total (%s): $%.2f", empleado, sueldoTotal));
                            binding.txtResultadoSueldoNeto.setText(String.format(Locale.getDefault(), "Sueldo Neto $: $%.2f", sueldoNeto));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error al leer desde la BD: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}