package com.arc.proyecto.clientecar_v3.Utils;

import java.io.Serializable;

public class CoordenadaData implements Serializable {
    private int x;
    private int y;

    public CoordenadaData(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }


}