package io.github.thred.rwperf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.github.thred.rwperf.util.Arguments;

public class RWPerf
{

    private static final String DEFAULT_SIZE = "64m";
    private static final int DEFAULT_PROCESSES = 1;
    private static final int DEFAULT_THREADS = 1;
    private static final long DEFAULT_SEED = 1;

    public static void main(String... args) throws InterruptedException
    {
        Arguments arguments = new Arguments(args);

        if (arguments.consumeFlag("-?") || arguments.consumeFlag("--help"))
        {
            showHelp();
        }

        String sizeString = arguments.consume("-s", String.class).orElse(
            arguments.consume("--size", String.class).orElse(DEFAULT_SIZE));

        int mult = 1;

        if (sizeString.endsWith("g"))
        {
            mult = 1024 * 1024 * 1024;
            sizeString = sizeString.substring(0, sizeString.length() - 1);
        }
        else if (sizeString.endsWith("m"))
        {
            mult = 1024 * 1024;
            sizeString = sizeString.substring(0, sizeString.length() - 1);
        }
        else if (sizeString.endsWith("k"))
        {
            mult = 1024;
            sizeString = sizeString.substring(0, sizeString.length() - 1);
        }

        long size = 0;

        try
        {
            size = Long.parseLong(sizeString) * mult;
        }
        catch (NumberFormatException e)
        {
            System.err.println("Failed to parse size argument: " + sizeString);
            System.exit(-1);
        }

        long maxSize = arguments.consume("--max-size", Long.class).orElse(Math.max(size / 16, 1));
        int processes = arguments.consume("-p", Integer.class).orElse(
            arguments.consume("--processes", Integer.class).orElse(DEFAULT_PROCESSES));
        int threads = arguments.consume("-t", Integer.class).orElse(
            arguments.consume("--threads", Integer.class).orElse(DEFAULT_THREADS));
        long seed = arguments.consume("--seed", Long.class).orElse(DEFAULT_SEED);
        boolean keep = arguments.consumeFlag("--keep");

        List<File> paths = new ArrayList<>();
        Optional<String> filename = arguments.consume(String.class);

        while (filename.isPresent())
        {
            File path = new File(filename.get());

            if (!path.isDirectory())
            {
                System.err.println(path.getAbsolutePath() + " is no directory.");
                System.exit(-1);
            }

            paths.add(path);

            filename = arguments.consume(String.class);
        }

        if (paths.isEmpty())
        {
            paths.add(new File("."));
        }

        AverageConsumer consumer = new AverageConsumer();
        Service service = new Service(consumer, paths, threads);

        service.prepare(size, maxSize, seed, !keep);
        service.shutdown();

        System.out.println(consumer);
    }

    private static void showHelp()
    {
        System.out.println("RWPerf");
        System.out.println("======");
        System.out.println("");
        System.out.println("A simple tool for testing Java read and write performance.");
        System.out.println("");
        System.out.println("Usage: rwperf [options] [path...]");
        System.out.println("");
        System.out.println("The default path is the current directory. The application will write and read");
        System.out.println("multiple files at the specified paths and print a summary of the performance.");
        System.out.println("");
        System.out.println("Options:");
        System.out.println("");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("-?, --help             Show this help.");
        System.out.println("-s, --size [int]       Number of bytes to write. Supports k, m, g.");
        System.out.println("                       Default is " + DEFAULT_SIZE + ".");
        System.out.println("--max-size [int]       Max size of one file. Default is size/16.");
        System.out.println("-p, --processes [int]  The number of OS processes to use. Default is 1.");
        System.out.println("-t, --threads [int]    The number of threads to use. Default is 1.");
        System.out.println("--seed [int]           The seed for the random generator.");
        System.out.println("--keep                 Do not delete the created files on exit.");
    }

}
