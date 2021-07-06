package com.arc.proyecto.clientecar_v3.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.arc.proyecto.clientecar_v3.R;
import com.arc.proyecto.clientecar_v3.Utils.CoordenadaData;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class GraficaFragment extends Fragment {
    private ScatterChart grafica;
    private CoordenadaData[] datos;
    private List<Entry> entradas;
    private int numVecinos;
    private int grupo;
    private boolean inicial;
    private int maxX, maxY;
    public static GraficaFragment newInstance(int numVecinos, int grupo, CoordenadaData[] datos, boolean inicial, int maxX, int maxY) {

        Bundle args = new Bundle();
        args.putInt("Vecinos", numVecinos);
        args.putInt("Grupo", grupo);
        args.putSerializable("Datos",datos);
        args.putBoolean("Inicial",inicial);
        args.putInt("MaxX",maxX);
        args.putInt("MaxY",maxY);
        GraficaFragment fragment = new GraficaFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        numVecinos = getArguments().getInt("Vecinos");
        grupo = getArguments().getInt("Grupo");
        datos = (CoordenadaData[]) getArguments().getSerializable("Datos");
        inicial = getArguments().getBoolean("Inicial");
        maxX = getArguments().getInt("MaxX");
        maxY = getArguments().getInt("MaxY");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_grafica,container,false);
        grafica = view.findViewById(R.id.grafica);
        //CONFIGURACIÓN DE LA GRÁFICA
        grafica.getDescription().setEnabled(false);
        grafica.setDrawGridBackground(false);
        grafica.setDragEnabled(true);
        grafica.setPinchZoom(true);
        grafica.setKeepPositionOnRotation(true);
        grafica.setDrawBorders(true);
        grafica.animateX(2000, Easing.EaseInOutQuart);

        //CONFIGURACIÓN EJE X
        XAxis xAxis = grafica.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMaximum(maxX);
        xAxis.setAxisMinimum(0);
        xAxis.setGranularity(1f);
        //CONFIGURACIÓN EJE Y
        YAxis yAxisL = grafica.getAxisLeft();
        yAxisL.setAxisMaximum(maxY);
        yAxisL.setAxisMinimum(0);
        yAxisL.setSpaceBottom(0f);
        YAxis yAxisR = grafica.getAxisRight();
        yAxisR.setAxisMinimum(0);
        yAxisR.setAxisMaximum(maxY);
        yAxisR.setSpaceBottom(0f);
        //CONFIGURACIÓN DE LA LEYENDA
        Legend legend = grafica.getLegend();
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setDrawInside(false);

        //CONFIGURACIÓN DE LAS ENTRADAS DE LA GRÁFICA
        entradas = new ArrayList<>();
        if(inicial == false){
            for (int i=0; i < datos.length; i++){
                float x = (float) this.datos[i].getX();
                float y = (float) this.datos[i].getY();
                entradas.add(new Entry(x,y));
            }

        }
        ScatterDataSet dataSet = new ScatterDataSet(entradas, numVecinos+" Vecinos en el Grupo "+grupo);
        dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        dataSet.setHighlightEnabled(true);
        dataSet.setScatterShapeSize(20f);
        dataSet.setDrawHighlightIndicators(true);
        dataSet.setDrawValues(true);
        dataSet.setColor(ColorTemplate.MATERIAL_COLORS[1]);



        //ACTUALIZAR LA GRÁFICA CON LOS DATOS
        ScatterData scatterData = new ScatterData(dataSet);
        grafica.setData(scatterData);
        grafica.invalidate();
        return view;
    }
}
