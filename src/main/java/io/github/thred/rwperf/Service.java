package io.github.thred.rwperf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.github.thred.rwperf.util.Checksum;
import io.github.thred.rwperf.util.Stopwatch;

public class Service
{

    private static final int BUFFER_SIZE = 4096;

    private final Consumer consumer;
    private final Pool pool;
    private final List<File> paths;

    private final List<Runnable> reads = new ArrayList<>();
    private final List<Runnable> deletes = new ArrayList<>();

    public Service(Consumer consumer, List<File> paths, int threads)
    {
        super();

        this.consumer = consumer;

        pool = new Pool(threads);

        this.paths = paths;
    }

    public void prepare(long size, long maxSize, long seed, boolean delete)
    {
        Random random = new Random(seed);

        while (size > 0)
        {
            size = prepareWrite(random, size, maxSize);
        }

        for (Runnable runnable : reads)
        {
            pool.execute(runnable);
        }

        if (delete)
        {
            for (Runnable runnable : deletes)
            {
                pool.execute(runnable);
            }
        }
    }

    public void shutdown() throws InterruptedException
    {
        pool.shutdown();
    }

    private long prepareWrite(Random random, long remainingSize, long maxSize)
    {
        maxSize = Math.min(remainingSize, maxSize);

        File path = paths.get(random.nextInt(paths.size()));
        long size = Math.max(Math.min((long) Math.pow(random.nextDouble() * Math.sqrt(maxSize), 2), maxSize), 1);
        long seed = random.nextLong();
        File file = new File(path, "rwperf#" + seed + ".dat");

        pool.execute(() -> write(file, seed, (int) size));

        reads.add(() -> read(file));
        deletes.add(() -> delete(file));

        return remainingSize - size;
    }

    private void write(File file, long seed, int size)
    {
        Random random = new Random(seed);
        byte[] buffer = new byte[BUFFER_SIZE];
        Checksum checksum = new Checksum();
        Stopwatch stopwatch = Stopwatch.start();
        boolean success = true;
        int remaining = size;

        try (FileOutputStream out = new FileOutputStream(file))
        {
            while (remaining > 0)
            {
                random.nextBytes(buffer);

                int length = Math.min(remaining, BUFFER_SIZE);

                out.write(buffer, 0, length);
                checksum.update(buffer, 0, length);

                remaining -= BUFFER_SIZE;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace(System.err);
            success = false;
        }

        double duration = stopwatch.stop();
        String checksumValue = checksum.complete();

        consumer.consume(new Result(file, Operation.Write, size, checksumValue, duration, success));
    }

    private void read(File file)
    {
        byte[] buffer = new byte[BUFFER_SIZE];
        Checksum checksum = new Checksum();
        Stopwatch stopwatch = Stopwatch.start();
        boolean success = true;
        long size = 0;

        try (FileInputStream in = new FileInputStream(file))
        {
            int length;

            while ((length = in.read(buffer)) >= 0)
            {
                size += length;
                checksum.update(buffer, 0, length);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace(System.err);
            success = false;
        }

        double duration = stopwatch.stop();
        String checksumValue = checksum.complete();

        consumer.consume(new Result(file, Operation.Read, size, checksumValue, duration, success));
    }

    private void delete(File file)
    {
        Stopwatch stopwatch = Stopwatch.start();
        long size = file.length();
        boolean success = file.delete();
        double duration = stopwatch.stop();

        consumer.consume(new Result(file, Operation.Delete, size, "checksum", duration, success));
    }

}
