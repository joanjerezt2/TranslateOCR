package org.apertium.grammar;

import java.util.Comparator;

public class Pool<T extends Pool.Recyclable> {

    // Interfaz obligatoria para que los objetos del pool limpien sus datos internos
    public interface Recyclable {
        void clear();
    }

    // Usamos el SortedVector que implementamos en el paso anterior
    private final SortedVector<T> p;

    // Constructor por defecto usando la identidad o un comparador por referencia de memoria
    public Pool() {
        // En Java, para simular la ordenación de punteros nativos (T*), podemos comparar
        // los objetos por su identidad del sistema (System.identityHashCode)
        this.p = new SortedVector<>(Comparator.comparingInt(System::identityHashCode));
    }

    // Constructor que permite inyectar un comparador específico si tus objetos lo requieren
    public Pool(Comparator<T> comp) {
        this.p = new SortedVector<>(comp);
    }

    // Equivalente a: auto get()
    public T get() {
        T var = null;
        if (!p.empty()) {
            var = p.back();
            p.eraseAt(p.size() - 1); // Equivale a pop_back()
        }

        // En Java, el logging de depuración equivalente se haría así:
        System.err.println("OBJECT: " + (var != null ? System.identityHashCode(var) : "null") + " Pool.get()");

        return var;
    }

    // Equivalente a: void put(T* t)
    public void put(T t) {
        if (t == null) {
            return;
        }

        // System.err.println("OBJECT: " + System.identityHashCode(t) + " Pool.put()");

        // Limpia el estado interno del objeto para que quede como nuevo antes de reusarse
        t.clear();

        // Insertamos el objeto en el SortedVector
        SortedVector.InsertResult ins = p.insert(t);

        if (!ins.inserted()) {
            // Descomentar para depurar si intentas devolver el mismo objeto dos veces al pool
            throw new RuntimeException("Pool cannot insert item twice!");
        }
    }

    // El destructor ~pool() desaparece. Cuando este objeto Pool quede fuera de alcance,
    // el Garbage Collector de Java se encargará de liberar de la memoria tanto el pool
    // como todos los elementos 'Recyclable' que contenga en su interior.
}
