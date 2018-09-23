package io.github.thred.rwperf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import io.github.thred.rwperf.util.Arguments;
import io.github.thred.rwperf.util.InputHandler;

public class RWPerf
{

    private static final String DEFAULT_SIZE = "64mb";
    private static final int DEFAULT_PROCESSES = 1;
    private static final int DEFAULT_THREADS = 1;
    private static final long DEFAULT_SEED = 1;
    private static final int MAX_FILES = 1024 * 8;
    private static final int MAX_PROCESSES = 16;
    private static final int MAX_THREADS = 16;
    private static final long MIN_SIZE = 1;

    public static void main(String... args) throws InterruptedException
    {
        Arguments arguments = new Arguments(args);

        if (arguments.consumeFlag("-?") || arguments.consumeFlag("--help"))
        {
            showHelp();
        }

        long size = parseSize(arguments
            .consume("-s", String.class)
            .orElse(arguments.consume("--size", String.class).orElse(DEFAULT_SIZE)));
        long maxSize =
            parseSize(arguments.consume("--max-size", String.class).orElse(String.valueOf(Math.max(size / 100, 1))));
        long minSize = parseSize(arguments.consume("--min-size", String.class).orElse("1kb"));

        if (size < 0)
        {
            System.err.println("Invalid size: " + formatSize(size));
            System.exit(-1);
        }

        if (minSize < MIN_SIZE)
        {
            System.err.println("Min-size too small: " + formatSize(minSize) + "<" + formatSize(MIN_SIZE));
            System.exit(-1);
        }

        if (maxSize < minSize)
        {
            System.err
                .println("Max-size must be greater or equals to min-size: "
                    + formatSize(minSize)
                    + ">"
                    + formatSize(maxSize));
            System.exit(-1);
        }

        long averageSize = (minSize + maxSize) / 2;

        if (size / averageSize > MAX_FILES)
        {
            System.err
                .println("Request will result in too many files: "
                    + formatSize(size)
                    + "/"
                    + formatSize(averageSize)
                    + "="
                    + (size / averageSize));
            System.exit(-1);
        }

        int processes = arguments
            .consume("-p", Integer.class)
            .orElse(arguments.consume("--processes", Integer.class).orElse(DEFAULT_PROCESSES));

        if (processes < 1)
        {
            System.err.println("Invalid number of processes: " + processes);
            System.exit(-1);
        }

        if (processes > MAX_PROCESSES)
        {
            System.err.println("Too many processes: " + processes + ">" + MAX_PROCESSES);
            System.exit(-1);
        }

        int threads = arguments
            .consume("-t", Integer.class)
            .orElse(arguments.consume("--threads", Integer.class).orElse(DEFAULT_THREADS));

        if (threads < 1)
        {
            System.err.println("Invalid number of threads: " + threads);
            System.exit(-1);
        }

        if (threads > MAX_THREADS)
        {
            System.err.println("Too many threads: " + threads + ">" + MAX_THREADS);
            System.exit(-1);
        }

        long seed = arguments.consume("--seed", Long.class).orElse(DEFAULT_SEED);
        boolean keep = arguments.consumeFlag("--keep");
        boolean subprocess = arguments.consumeFlag("--subprocess");

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

        ResultConsumer consumer;

        if (subprocess)
        {
            consumer = new ConsoleResultConsumer();
        }
        else
        {
            consumer = new AverageResultConsumer(size, minSize, maxSize, processes, threads, seed, keep, paths);
        }

        if (processes == 1)
        {
            Service service = new Service(consumer, paths, threads);

            service.prepare(size, minSize, maxSize, seed, !keep);
            service.shutdown();
        }
        else
        {
            System.out.println("Starting " + processes + " processes...");
            System.out.println();

            Pool pool = new Pool(processes);
            Random random = new Random(seed);

            for (int i = 0; i < processes; i++)
            {
                int index = i + 1;

                pool
                    .execute(() -> process(index, consumer, size / processes, minSize, maxSize, threads,
                        random.nextLong(), keep, paths));
            }

            pool.shutdown();

            System.out.println();
        }

        System.out.println(consumer);
    }

    private static void process(int index, ResultConsumer consumer, long size, long minSize, long maxSize, int threads,
        long seed, boolean keep, List<File> paths)
    {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = RWPerf.class.getCanonicalName();

        List<String> commands = new ArrayList<>();

        commands.add(javaBin);
        commands.add("-cp");
        commands.add(classpath);
        commands.add(className);
        commands.add("--size");
        commands.add(String.valueOf(size));
        commands.add("--max-size");
        commands.add(String.valueOf(maxSize));
        commands.add("--min-size");
        commands.add(String.valueOf(minSize));
        commands.add("--processes");
        commands.add("1");
        commands.add("--threads");
        commands.add(String.valueOf(threads));
        commands.add("--seed");
        commands.add(String.valueOf(seed));

        if (keep)
        {
            commands.add("--keep");
        }

        commands.add("--subprocess");

        paths.stream().map(File::getAbsolutePath).forEach(commands::add);

        ProcessBuilder builder = new ProcessBuilder(commands);

        Process process;

        try
        {
            process = builder.start();
        }
        catch (IOException e)
        {
            e.printStackTrace(System.err);
            return;
        }

        InputHandler.handle(process.getErrorStream(), System.err::println);
        InputHandler.handle(process.getInputStream(), consumer::consume);

        try
        {
            process.waitFor();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace(System.err);
        }

        System.out.println("Process " + index + " finished with result code " + process.exitValue());
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
        System.out.println("-s, --size [int]       Number of bytes to write. Supports kb, mb, gb.");
        System.out.println("                       Default is " + DEFAULT_SIZE + ".");
        System.out.println("--max-size [int]       Max size of one file. Default is size/100.");
        System.out.println("--min-size [int]       Max size of one file. Default is 1kb.");
        System.out.println("-p, --processes [int]  The number of OS processes to use. Default is 1.");
        System.out.println("-t, --threads [int]    The number of threads per process. Default is 1.");
        System.out.println("--seed [int]           The seed for the random generator.");
        System.out.println("--keep                 Do not delete the created files on exit.");
    }

    private static long parseSize(String s)
    {
        int mult = 1;

        if (s.endsWith("gb"))
        {
            mult = 1024 * 1024 * 1024;
            s = s.substring(0, s.length() - 2);
        }
        else if (s.endsWith("mb"))
        {
            mult = 1024 * 1024;
            s = s.substring(0, s.length() - 2);
        }
        else if (s.endsWith("kb"))
        {
            mult = 1024;
            s = s.substring(0, s.length() - 2);
        }
        else if (s.endsWith("b"))
        {
            mult = 1;
            s = s.substring(0, s.length() - 1);
        }

        try
        {
            return Long.parseLong(s) * mult;
        }
        catch (NumberFormatException e)
        {
            System.err.println("Failed to parse size argument: " + s);
            System.exit(-1);
        }

        return -1;
    }

    public static String formatSize(long size)
    {
        if (size >= 1024 * 1024 * 1024)
        {
            return String.format("%,.1f gb", size / (double) (1024 * 1024 * 1024));
        }

        if (size >= 1024 * 1024)
        {
            return String.format("%,.1f mb", size / (double) (1024 * 1024));
        }

        if (size >= 1024)
        {
            return String.format("%,.1f kb", size / 1024d);
        }

        return String.format("%,d b", size);
    }
}
