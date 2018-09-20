package io.github.thred.rwperf;

public class ConsoleConsumer implements Consumer
{

    @Override
    public void consume(Result result)
    {
        System.out.println(result);
    }

}
