package org.apertium.grammar;

import java.util.Arrays;

public class Bloomish {
    // En Java, los arreglos se inicializan automáticamente en 0,
    // pero mantenemos el tamaño fijo de 4.
    private final int[] value = new int[4];

    // Constructor por defecto
    public Bloomish() {
        clear();
    }

    // Constructor de copia (Equivalente a bloomish(const bloomish& other))
    public Bloomish(Bloomish other) {
        System.arraycopy(other.value, 0, this.value, 0, 4);
    }

    // Equivalente al operador de asignación (operator=)
    // En Java no hay sobrecarga de operadores, se usa un método de utilidad.
    public Bloomish copyFrom(Bloomish other) {
        System.arraycopy(other.value, 0, this.value, 0, 4);
        return this;
    }

    public void clear() {
        Arrays.fill(value, 0);
    }

    public void insert(int v) {
        if ((v & 4) != 0) { // En Java, los ifs de bits deben compararse explícitamente con 0
            value[3] |= v;
        } else if ((v & 2) != 0) {
            value[2] |= v;
        } else if ((v & 1) != 0) {
            value[1] |= v;
        } else {
            value[0] |= v;
        }
    }

    public boolean matches(int v) {
        if ((v & 4) != 0) {
            return (value[3] & v) == v;
        } else if ((v & 2) != 0) {
            return (value[2] & v) == v;
        } else if ((v & 1) != 0) {
            return (value[1] & v) == v;
        }
        return (value[0] & v) == v;
    }
}

