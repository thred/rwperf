package io.github.thred.rwperf;

public class ConsoleResultConsumer implements ResultConsumer
{

    @Override
    public void consume(Result result)
    {
        System.out
            .printf("RESULT | %-32s | %-6s | %12s | %-28s | %.10f | %s\n", result.getName(), result.getOperation(),
                result.getSize(), result.getChecksum(), result.getDuration(), result.isSuccess());
    }

    @Override
    public String toString()
    {
        return "FINISHED";
    }

}
