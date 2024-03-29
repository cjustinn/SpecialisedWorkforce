package io.github.cjustinn.specialisedworkforce;

import java.util.TreeMap;

public class Helpers {
    private static final TreeMap<Integer, String> map = new TreeMap();

    public Helpers() {
    }

    public static final String toRoman(int num) {
        int l = (Integer)map.floorKey(num);
        return num == l ? (String)map.get(num) : (String)map.get(l) + toRoman(num - l);
    }

    static {
        map.put(1000, "M");
        map.put(900, "CM");
        map.put(500, "D");
        map.put(400, "CD");
        map.put(100, "C");
        map.put(90, "XC");
        map.put(50, "L");
        map.put(40, "XL");
        map.put(10, "X");
        map.put(9, "IX");
        map.put(5, "V");
        map.put(4, "IV");
        map.put(1, "I");
    }
}
