package fr.tse.db.storage.data;


import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class SeriesQueueTest {


    @Test
    public void addValTest() {

        SeriesQueue<Int32> Ser = new SeriesQueue<Int32>(0L);

        LinkedList<DataPointComp> list = Ser.getSeries();


        assertEquals(0, list.size());


        Ser.addVal(10L, new Int32(1));

        list = Ser.getSeries();
        BitSet one = new BitSet();
        BitSet ten = new BitSet();

        one.set(0, true);
        ten.set(3, true);
        ten.set(1, true);
        assertEquals(1, list.size());
        assertTrue(ten.equals(list.get(0).getTimestamp()));
        assertTrue(one.equals(list.get(0).getValue()));


        Ser.addVal(11L, new Int32(1));
        list = Ser.getSeries();

        BitSet zero = new BitSet();
        zero.set(0, false);

        assertEquals(2, list.size());
        assertTrue(one.equals(list.get(1).getTimestamp()));
        assertTrue(zero.equals(list.get(1).getValue()));

    }

    @Test
    public void getValTest() {

        SeriesQueue<Int32> Ser = new SeriesQueue<Int32>(0L);
        Ser.addVal(10L, new Int32(1));
        Ser.addVal(15L, new Int32(40));

        Int32 point = Ser.getVal(78L, "Int32");
        Int32 point2 = Ser.getVal(10L, "Int32");
        Int32 point3 = Ser.getVal(15L, "Int32");

        assertEquals(null, point);
        assertEquals(new Integer(1), point2.getVal());
        assertEquals(new Integer(40), point3.getVal());

    }

    @Test
    public void removeTest() {

        SeriesQueue<Int32> Ser = new SeriesQueue<Int32>(0L);
        Ser.addVal(10L, new Int32(1));


        Ser.remove(56L);
        assertEquals(1, Ser.getSeries().size());
        Ser.remove(10L);

        assertEquals(0, Ser.getSeries().size());

    }

    @Test
    public void getAllPointsTest() {

        SeriesQueue<Int32> Ser = new SeriesQueue<Int32>(0L);
        Ser.addVal(10L, new Int32(1));


        Map<Long, Int32> map = Ser.getAllPoints("Int32");
        assertEquals(1, map.size());
        assertEquals(new Integer(1), map.get(10L).getVal());


    }
}
