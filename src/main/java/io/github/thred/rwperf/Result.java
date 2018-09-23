package io.github.thred.rwperf;

public class Result
{

    private final String name;
    private final Operation operation;
    private final long size;
    private final String checksum;
    private final double duration;
    private final boolean success;

    public Result(String name, Operation operation, long size, String checksum, double duration, boolean success)
    {
        super();
        this.name = name;
        this.operation = operation;
        this.size = size;
        this.checksum = checksum;
        this.duration = duration;
        this.success = success;
    }

    public String getName()
    {
        return name;
    }

    public Operation getOperation()
    {
        return operation;
    }

    public long getSize()
    {
        return size;
    }

    public String getChecksum()
    {
        return checksum;
    }

    public double getDuration()
    {
        return duration;
    }

    public boolean isSuccess()
    {
        return success;
    }

    @Override
    public String toString()
    {
        return String
            .format("%s (%s) of %s bytes [%s] in %s s: %s", name, operation, size, checksum, duration,
                success ? "succeeded" : "failed");
    }

}
