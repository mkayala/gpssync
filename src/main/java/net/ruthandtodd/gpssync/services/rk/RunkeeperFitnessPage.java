package net.ruthandtodd.gpssync.services.rk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
class RunkeeperFitnessPage {
    int size;
    FitnessItem[] items;
    String previous;
    String next;

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public FitnessItem[] getItems() {
        return items;
    }

    public void setItems(FitnessItem[] items) {
        this.items = items;
    }

    public String getPrevious() {
        return previous;
    }

    public void setPrevious(String previous) {
        this.previous = previous;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }
}
