package io.github.thred.rwperf;

public interface ResultConsumer
{

    void consume(Result result);

    default void consume(String line)
    {
        if (line.startsWith("FINISH"))
        {
            return;
        }

        if (!line.startsWith("RESULT"))
        {
            System.out.println(line);
            return;
        }

        String[] chunks = line.split("\\|");

        consume(new Result(chunks[1].trim(), Operation.valueOf(chunks[2].trim()), Long.parseLong(chunks[3].trim()),
            chunks[4].trim(), Double.parseDouble(chunks[5].trim()), Boolean.parseBoolean(chunks[6].trim())));
    }

}
