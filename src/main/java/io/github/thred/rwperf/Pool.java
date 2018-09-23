package io.github.thred.rwperf;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Pool
{

    private final ExecutorService executor;

    public Pool(int threads)
    {
        super();

        executor = Executors.newFixedThreadPool(threads);
    }

    public void execute(Runnable runnable)
    {
        executor.execute(runnable);
    }

    public void shutdown() throws InterruptedException
    {
        executor.shutdown();

        while (!executor.isTerminated())
        {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

}
