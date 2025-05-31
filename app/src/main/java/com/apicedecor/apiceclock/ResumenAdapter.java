package com.apicedecor.apiceclock;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;

import java.util.List;
import java.util.Locale;

public class ResumenAdapter extends RecyclerView.Adapter<ResumenAdapter.ViewHolder> {
    private List<DaySummary> resumenList;

    public ResumenAdapter(List<DaySummary> resumenList) {
        this.resumenList = resumenList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resumen, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DaySummary resumen = resumenList.get(position);
        holder.textFecha.setText(resumen.getDate());
        // Aseguramos que totalHours tenga el formato esperado "HH:mm"
        String totalHoras = resumen.getTotalHours();
        String[] partes = totalHoras.split(":");
        int horas = Integer.parseInt(partes[0]);
        int minutos = Integer.parseInt(partes[1]);

        holder.textHoras.setText(String.format(Locale.getDefault(), "%d:%02d", horas, minutos));
    }

    @Override
    public int getItemCount() {
        return resumenList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textFecha, textHoras;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textFecha = itemView.findViewById(R.id.textFecha);
            textHoras = itemView.findViewById(R.id.textHoras);
        }
    }
}

