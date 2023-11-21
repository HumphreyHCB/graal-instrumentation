import java.lang.Thread;

public class TestingHumphreysCache extends Thread {
        
        public static long[] Buffer;
        public static int pointer;

        public TestingHumphreysCache() {
                Buffer = new long[3_000_000];
                pointer = 0;

        }

        public static void add(long item)
        {
                Buffer[pointer] = item;
                pointer++;

        }

        public void print(){

                for (long l : Buffer) {
                        System.out.print(" "+ l + " ");
                }
        }


}
