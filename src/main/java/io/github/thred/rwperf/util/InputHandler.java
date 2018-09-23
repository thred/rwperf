package io.github.thred.rwperf.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public class InputHandler implements Closeable, Runnable
{

    public static InputHandler handle(InputStream in, Consumer<String> consumer)
    {
        return new InputHandler(in, consumer);
    }

    private final BufferedReader reader;
    private final Consumer<String> consumer;

    public InputHandler(InputStream in, Consumer<String> consumer)
    {
        super();

        this.consumer = consumer;

        reader = new BufferedReader(new InputStreamReader(in));

        Thread thread = new Thread(this, "InputHandler");

        thread.start();
    }

    @Override
    public void run()
    {
        String line;

        try
        {
            while ((line = reader.readLine()) != null)
            {
                consumer.accept(line);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace(System.err);
            try
            {
                close();
            }
            catch (IOException e1)
            {
                e1.printStackTrace(System.err);
            }
        }
    }

    @Override
    public void close() throws IOException
    {
        reader.close();
    }

}
