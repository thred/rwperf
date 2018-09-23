package io.github.thred.rwperf;

import java.io.File;
import java.util.List;

public class AverageResultConsumer implements ResultConsumer
{

    private class Average
    {
        private final Operation operation;

        private long size = 0;
        private int count = 0;
        private double duration = 0;

        public Average(Operation operation)
        {
            super();

            this.operation = operation;
        }

        public void consume(Result result)
        {
            size += result.getSize();
            count++;
            duration += result.getDuration();
        }

        @Override
        public String toString()
        {
            long result = (long) (size / duration);
            String op = operation.toString() + " avg:";

            return String.format("%-16s%s/s", op, RWPerf.formatSize(result));
        }
    }

    private final Average writeAverage = new Average(Operation.Write);
    private final Average readAverage = new Average(Operation.Read);
    private final Average deleteAverage = new Average(Operation.Delete);

    private final long size;
    private final long minSize;
    private final long maxSize;
    private final int processes;
    private final int threads;
    private final long seed;
    private final boolean keep;
    private final List<File> paths;

    public AverageResultConsumer(long size, long minSize, long maxSize, int processes, int threads, long seed,
        boolean keep, List<File> paths)
    {
        super();

        this.size = size;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.processes = processes;
        this.threads = threads;
        this.seed = seed;
        this.keep = keep;
        this.paths = paths;
    }

    @Override
    public void consume(Result result)
    {
        switch (result.getOperation())
        {
            case Write:
                writeAverage.consume(result);
                break;

            case Read:
                readAverage.consume(result);
                break;

            case Delete:
                deleteAverage.consume(result);
                break;

            default:
                throw new UnsupportedOperationException("Operation not supported: " + result.getOperation());
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("RWPerf Results\n");
        builder.append("==============\n");
        builder.append("\n");
        builder.append("Total size:     ").append(RWPerf.formatSize(size)).append("\n");
        builder.append("Min file size:  ").append(RWPerf.formatSize(minSize)).append("\n");
        builder.append("Max file size:  ").append(RWPerf.formatSize(maxSize)).append("\n");
        builder.append("\n");
        builder.append("Processes:      ").append(processes).append("\n");
        builder.append("Threads:        ").append(threads).append("\n");
        builder.append("Seed:           ").append(seed).append("\n");
        builder.append("Delete files:   ").append(!keep).append("\n");
        builder.append("\n");
        builder.append("Paths:          ");

        boolean first = true;

        for (File path : paths)
        {
            if (!first)
            {
                builder.append("                ");
            }
            else
            {
                first = false;
            }

            builder.append(path.getAbsolutePath()).append("\n");
        }

        builder.append("\n");
        builder.append("Written:        ").append(writeAverage.count).append(" files\n");
        builder.append("Read:           ").append(readAverage.count).append(" files\n");
        builder.append("Deleted:        ").append(deleteAverage.count).append(" files\n");

        builder.append("\n");
        builder.append(writeAverage).append("\n");
        builder.append(readAverage).append("\n");
        builder.append(deleteAverage).append("\n");

        return builder.toString();
    }

}
