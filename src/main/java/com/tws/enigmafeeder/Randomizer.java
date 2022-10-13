package com.tws.enigmafeeder;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Randomizer {
        
    private final List<Long> numbers;

    public Randomizer(long howmany, long min, long max) {
        
        ThreadLocalRandom threadRandom = ThreadLocalRandom.current();

        this.numbers = new ArrayList<>();
        for (int i=0; i<howmany; i++) {
            numbers.add(threadRandom.nextLong(min, max+1));
        }

    }

    public List<Long> getNumbers() {        
        return numbers;
    }

}
