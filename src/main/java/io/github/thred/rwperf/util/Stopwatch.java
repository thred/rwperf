package io.github.thred.rwperf.util;

public class Stopwatch
{

    public static Stopwatch start()
    {
        return new Stopwatch();
    }

    private final long nanos;

    public Stopwatch()
    {
        super();

        nanos = System.nanoTime();
    }

    public double stop()
    {
        return (System.nanoTime() - nanos) / 1000000000d;
    }

}
