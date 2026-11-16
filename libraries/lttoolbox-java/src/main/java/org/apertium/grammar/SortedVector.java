package org.apertium.grammar;

import java.util.*;

public class SortedVector<T> {

    private final List<T> elements;
    private final Comparator<T> comp;

    // Resultado para emular std::pair<iterator, bool> de insert()
        public record InsertResult(int index, boolean inserted) {}

    // Constructor por defecto usando orden natural (T debe ser Comparable)
    @SuppressWarnings("unchecked")
    public SortedVector() {
        this.elements = new ArrayList<>();
        this.comp = (a, b) -> ((Comparable<T>) a).compareTo(b);
    }

    // Constructor aceptando un comparador personalizado (Equivalente al template Comp)
    public SortedVector(Comparator<T> comp) {
        this.elements = new ArrayList<>();
        this.comp = comp;
    }

    // --- MÉTODOS DE INSERCIÓN ---

    // Equivalente a std::pair<iterator, bool> insert(T t)
    public InsertResult insert(T t) {
        if (elements.isEmpty()) {
            elements.add(t);
            return new InsertResult(0, true);
        }

        int index = lowerBound(t);

        // Si llegó al final, es mayor que todos los existentes
        if (index == elements.size()) {
            elements.add(t);
            return new InsertResult(index, true);
        }

        // Si comp(it, t) == 0 (es decir, no es ni menor ni mayor), ya existe. No se inserta.
        if (comp.compare(elements.get(index), t) == 0) {
            return new InsertResult(index, false);
        }

        // De lo contrario, se inserta desplazando el resto
        elements.add(index, t);
        return new InsertResult(index, true);
    }

    // Equivalente a void insert(It b, It e) / Inserción de un bloque de elementos
    public void insertAll(Collection<? extends T> inputElements) {
        if (inputElements.isEmpty()) return;
        if (inputElements.size() == 1) {
            insert(inputElements.iterator().next());
            return;
        }

        List<T> incoming = new ArrayList<>(inputElements);

        // Equivalente a detail::is_sorted
        if (!isSorted(incoming)) {
            incoming.sort(comp);
        }

        // Simula la lógica de std::merge creando una lista unificada ordenada
        List<T> merged = new ArrayList<>(this.elements.size() + incoming.size());
        int i = 0, j = 0;

        while (i < this.elements.size() && j < incoming.size()) {
            T e1 = this.elements.get(i);
            T e2 = incoming.get(j);
            if (comp.compare(e1, e2) <= 0) {
                merged.add(e1);
                i++;
            } else {
                merged.add(e2);
                j++;
            }
        }
        while (i < this.elements.size()) merged.add(this.elements.get(i++));
        while (j < incoming.size()) merged.add(incoming.get(j++));

        // Equivalente a std::unique (Eliminación de duplicados consecutivos)
        this.elements.clear();
        if (!merged.isEmpty()) {
            T lastAdded = merged.getFirst();
            this.elements.add(lastAdded);
            for (int k = 1; k < merged.size(); k++) {
                T current = merged.get(k);
                if (comp.compare(current, lastAdded) != 0) {
                    this.elements.add(current);
                    lastAdded = current;
                }
            }
        }
    }

    public void pushBack(T t) {
        insert(t);
    }

    // --- MÉTODOS DE ELIMINACIÓN ---

    public boolean erase(T t) {
        if (elements.isEmpty()) return false;

        // Optimización de límites rápidos originales de CG3
        if (comp.compare(elements.getLast(), t) < 0) return false;
        if (comp.compare(t, elements.getFirst()) < 0) return false;

        int index = lowerBound(t);
        if (index < elements.size() && comp.compare(elements.get(index), t) == 0) {
            elements.remove(index);
            return true;
        }
        return false;
    }

    public void eraseAt(int index) {
        elements.remove(index);
    }

    public void eraseAll(Collection<? extends T> inputElements) {
        for (T t : inputElements) {
            erase(t);
        }
    }

    // --- MÉTODOS DE BÚSQUEDA Y LÍMITES ---

    // Devuelve el índice si existe, o el tamaño de la lista si no se encuentra
    public int find(T t) {
        if (elements.isEmpty()) return 0;
        if (comp.compare(elements.getLast(), t) < 0) return elements.size();
        if (comp.compare(t, elements.getFirst()) < 0) return elements.size();

        int index = lowerBound(t);
        if (index < elements.size() && comp.compare(elements.get(index), t) == 0) {
            return index;
        }
        return elements.size();
    }

    public boolean contains(T t) {
        return find(t) != elements.size();
    }

    public int count(T t) {
        return contains(t) ? 1 : 0;
    }

    // Equivalente exacto a std::lower_bound usando Collections.binarySearch
    public int lowerBound(T t) {
        int res = Collections.binarySearch(elements, t, comp);
        if (res >= 0) {
            // binarySearch puede retornar cualquier elemento duplicado aleatorio.
            // Caminamos hacia atrás para encontrar la PRIMERA ocurrencia (comportamiento lower_bound).
            while (res > 0 && comp.compare(elements.get(res - 1), t) == 0) {
                res--;
            }
            return res;
        } else {
            // Si no existe, devuelve (-(punto de inserción) - 1). Lo decodificamos:
            return -res - 1;
        }
    }

    // Equivalente exacto a std::upper_bound (Primer elemento estrictamente mayor que t)
    public int upperBound(T t) {
        int index = lowerBound(t);
        while (index < elements.size() && comp.compare(elements.get(index), t) == 0) {
            index++;
        }
        return index;
    }

    // --- ACCESO A DATOS ---

    public T front() { return elements.getFirst(); }
    public T back() { return elements.getLast(); }
    public T at(int index) { return elements.get(index); }
    public List<T> get() { return elements; }
    public int size() { return elements.size(); }
    public boolean empty() { return elements.isEmpty(); }
    public void clear() { elements.clear(); }

    public void swap(SortedVector<T> other) {
        // En Java se intercambian las referencias internas de los ArrayLists
        List<T> temp = new ArrayList<>(this.elements);
        this.elements.clear();
        this.elements.addAll(other.elements);
        other.elements.clear();
        other.elements.addAll(temp);
    }

    // --- MÉTODOS INTERNOS AUXILIARES ---

    private boolean isSorted(List<T> list) {
        if (list.size() <= 1) return true;
        for (int i = 1; i < list.size(); i++) {
            if (comp.compare(list.get(i), list.get(i - 1)) < 0) {
                return false;
            }
        }
        return true;
    }
}
